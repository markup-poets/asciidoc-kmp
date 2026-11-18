package org.markup.poet.plugin.integration

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.Block
import org.markup.poet.asciidoc.asg.BlockMacro
import org.markup.poet.asciidoc.asg.BlockMacroName
import org.markup.poet.asciidoc.asg.BlockMetadata
import org.markup.poet.asciidoc.asg.ConditionalBlock
import org.markup.poet.asciidoc.asg.CustomBlockMacro
import org.markup.poet.asciidoc.asg.DListBlock
import org.markup.poet.asciidoc.asg.Inline
import org.markup.poet.asciidoc.asg.InlineFootnote
import org.markup.poet.asciidoc.asg.InlineMacro
import org.markup.poet.asciidoc.asg.InlineRaw
import org.markup.poet.asciidoc.asg.InlineRef
import org.markup.poet.asciidoc.asg.InlineSpan
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.LeafBlockName
import org.markup.poet.asciidoc.asg.ListBlock
import org.markup.poet.asciidoc.asg.Location
import org.markup.poet.asciidoc.asg.ParentBlock
import org.markup.poet.asciidoc.asg.RawBlock
import org.markup.poet.asciidoc.asg.SectionBlock
import org.markup.poet.asciidoc.asg.builtInBlockStyles
import org.markup.poet.asciidoc.asg.plainText
import org.markup.poet.asciidoc.asg.serialization.AsgDocumentJsonDeserializer
import org.markup.poet.asciidoc.parser.DefaultAsciidocParser
import org.markup.poet.plugin.api.BACKEND_HTML5
import org.markup.poet.plugin.api.PLACEMENT_HEAD
import org.markup.poet.plugin.api.PluginAsset
import org.markup.poet.plugin.api.PluginDispatch
import org.markup.poet.plugin.api.PluginHandle
import org.markup.poet.plugin.api.PluginInvocation
import org.markup.poet.plugin.api.PluginReplacement
import org.markup.poet.plugin.api.PluginResponse
import org.markup.poet.plugin.api.SourcePoint
import org.markup.poet.plugin.engine.PluginException

/**
 * Result of a plugin pass over a document.
 *
 * @param assets document-level resources the plugins' output depends on, one
 *   per [PluginAsset.key], in first-use order
 */
data class PluginProcessingResult(
    val document: AsgDocument,
    val warnings: List<String>,
    val assets: List<PluginAsset> = emptyList(),
) {
    /**
     * The markup plugins want in the document `<head>`, ready for
     * `OutputOptions.headContent`. A host rendering a fragment places it in
     * its own page.
     */
    val headContent: List<String> get() = assets.filter { it.placement == PLACEMENT_HEAD }.map { it.html }
}

/**
 * Applies WASM extension plugins to a parsed ASG document:
 *
 * - `block` capability: every [LeafBlock] whose style
 *   (`metadata.positional.first()`) a plugin claims is replaced by the plugin's
 *   output.
 * - `blockMacro` capability: every [CustomBlockMacro] or built-in
 *   [BlockMacro] (`video`, `audio`, `image`) whose name a plugin claims is
 *   replaced by the plugin's output.
 * - `inlineMacro` capability: every [InlineMacro] whose name a plugin claims is
 *   replaced.
 *
 * A plugin's claim takes precedence over the built-in handling of the same
 * name (`icon`, `stem`, `video`, the `stem` block style, ...). A plugin that
 * answers `ok` without a replacement declines that one construct, which then
 * renders the built-in way.
 *
 * Responses may carry [PluginAsset]s (a stylesheet, a script); they are
 * collected once per key into [PluginProcessingResult.assets].
 *
 * Replacement content types: `html` splices a [RawBlock]/[InlineRaw]
 * passthrough node, `asciidoc` is re-parsed and spliced, and `asg` is decoded
 * as official ASG node JSON ([AsgDocumentJsonDeserializer]) and spliced —
 * blocks in block context, inlines in inline context; a mismatch is a plugin
 * error, not a crash.
 *
 * The wire ABI (the JSON envelope in [PluginInvocation]) is unchanged from the
 * legacy AST integration: the block style / macro name maps to `name`, the raw
 * block text / macro target maps to `content`, and [BlockMetadata] maps to the
 * `attributes` map with positional attributes keyed by their 1-based index.
 *
 * Unclaimed constructs and failed invocations are left in place (rendering
 * falls back to a listing / placeholder / warning) with a warning.
 */
class WasmExtensions(
    private val engine: PluginDispatch,
    private val backend: String = BACKEND_HTML5,
) {
    private val asgDecoder = AsgDocumentJsonDeserializer()

    /** Assets of the pass in progress, by key. */
    private val assets = LinkedHashMap<String, PluginAsset>()

    fun apply(document: AsgDocument): PluginProcessingResult {
        assets.clear()
        val warnings = mutableListOf<String>()
        val blocks = processBlocks(document.blocks, document.attributes, warnings)
        return PluginProcessingResult(document.copy(blocks = blocks), warnings, assets.values.toList())
    }

    /** Records the response's assets when it replaced something; returns the replacement or null. */
    private fun PluginResponse.acceptedReplacement(): PluginReplacement? {
        val accepted = replacement?.takeIf { ok } ?: return null
        assets.forEach { asset -> this@WasmExtensions.assets.getOrPut(asset.key) { asset } }
        return accepted
    }

    // -----------------------------------------------------------------------
    // Blocks
    // -----------------------------------------------------------------------

    private fun processBlocks(
        blocks: List<Block>,
        documentAttributes: Map<String, String>,
        warnings: MutableList<String>,
    ): List<Block> = blocks.flatMap { block ->
        when (block) {
            is LeafBlock -> processLeafBlock(block, documentAttributes, warnings)
            is CustomBlockMacro -> processBlockMacro(block, documentAttributes, warnings)
            is BlockMacro -> processBuiltInBlockMacro(block, documentAttributes, warnings)
            is SectionBlock -> listOf(
                block.copy(blocks = processBlocks(block.blocks, documentAttributes, warnings)),
            )
            is ParentBlock -> listOf(
                block.copy(blocks = processBlocks(block.blocks, documentAttributes, warnings)),
            )
            is ListBlock -> listOf(
                block.copy(items = block.items.map { item ->
                    item.copy(
                        principal = processInlines(item.principal, documentAttributes, warnings),
                        blocks = processBlocks(item.blocks, documentAttributes, warnings),
                    )
                }),
            )
            is DListBlock -> listOf(
                block.copy(items = block.items.map { item ->
                    item.copy(
                        principal = processInlines(item.principal, documentAttributes, warnings),
                        blocks = processBlocks(item.blocks, documentAttributes, warnings),
                    )
                }),
            )
            is ConditionalBlock -> listOf(
                block.copy(
                    blocks = processBlocks(block.blocks, documentAttributes, warnings),
                    elseBlocks = processBlocks(block.elseBlocks, documentAttributes, warnings),
                ),
            )
            else -> listOf(block)
        }
    }

    /**
     * A leaf block with a style is offered to the plugin claiming that style.
     * For a built-in style (`stem`, `source`, ...) a plugin is optional: when
     * none claims it, or the plugin declines, only the block's inline content
     * is processed.
     */
    private fun processLeafBlock(
        block: LeafBlock,
        documentAttributes: Map<String, String>,
        warnings: MutableList<String>,
    ): List<Block> {
        val style = block.metadata?.positional?.firstOrNull()
        if (style != null && style !in builtInBlockStyles) {
            return processCustomBlock(block, style, documentAttributes, warnings)
        }
        if (style != null && engine.forCapability("block", style) != null) {
            val replaced = processCustomBlock(block, style, documentAttributes, warnings)
            if (replaced.singleOrNull() !== block) return replaced
        }
        return listOf(
            block.copy(inlines = processInlines(block.inlines, documentAttributes, warnings)),
        )
    }

    private fun processCustomBlock(
        block: LeafBlock,
        style: String,
        documentAttributes: Map<String, String>,
        warnings: MutableList<String>,
    ): List<Block> {
        val plugin = engine.forCapability("block", style) ?: return listOf(block)

        val response = try {
            plugin.process(
                PluginInvocation(
                    extensionPoint = "block",
                    name = style,
                    attributes = block.metadata.toAttributeMap(),
                    content = plainText(block.inlines),
                    documentAttributes = documentAttributes,
                    location = block.location.toSourcePoint(),
                    backend = backend,
                ),
            )
        } catch (e: PluginException) {
            warnings += "plugin '${plugin.id}' failed on [$style] block at line ${block.location.lineNumber()}: ${e.message}"
            return listOf(block)
        }

        warnings += response.warnings.map { "plugin '${plugin.id}': $it" }
        val replacement = response.acceptedReplacement()
        if (replacement == null) {
            response.error?.let {
                warnings += "plugin '${plugin.id}' rejected [$style] block at line ${block.location.lineNumber()}: $it"
            }
            return listOf(block)
        }
        return spliceBlockReplacement(plugin, replacement, block, block.location, warnings)
    }

    /**
     * A [CustomBlockMacro] is claimable by a plugin registering a `blockMacro`
     * capability under the macro name. The envelope carries the macro target
     * both as `content` and as the `target` attribute (mirroring inline
     * macros); [BlockMetadata] maps as for the `block` capability.
     */
    private fun processBlockMacro(
        macro: CustomBlockMacro,
        documentAttributes: Map<String, String>,
        warnings: MutableList<String>,
    ): List<Block> {
        val plugin = engine.forCapability("blockMacro", macro.name) ?: return listOf(macro)

        val response = try {
            plugin.process(
                PluginInvocation(
                    extensionPoint = "blockMacro",
                    name = macro.name,
                    attributes = macro.toAttributeMap(),
                    content = macro.target ?: "",
                    documentAttributes = documentAttributes,
                    location = macro.location.toSourcePoint(),
                    backend = backend,
                ),
            )
        } catch (e: PluginException) {
            warnings += "plugin '${plugin.id}' failed on ${macro.name}:: block macro at line ${macro.location.lineNumber()}: ${e.message}"
            return listOf(macro)
        }

        warnings += response.warnings.map { "plugin '${plugin.id}': $it" }
        val replacement = response.acceptedReplacement()
        if (replacement == null) {
            response.error?.let {
                warnings += "plugin '${plugin.id}' rejected ${macro.name}:: block macro at line ${macro.location.lineNumber()}: $it"
            }
            return listOf(macro)
        }
        return spliceBlockReplacement(plugin, replacement, macro, macro.location, warnings)
    }

    /**
     * A built-in block macro (`video::`, `audio::`, `image::`) goes to the
     * plugin claiming its name as a `blockMacro`, with the same envelope as a
     * custom block macro. Unclaimed or declined, it stays for the renderer.
     */
    private fun processBuiltInBlockMacro(
        macro: BlockMacro,
        documentAttributes: Map<String, String>,
        warnings: MutableList<String>,
    ): List<Block> {
        if (macro.name == BlockMacroName.TOC) return listOf(macro)
        val name = macro.name.name.lowercase()
        val plugin = engine.forCapability("blockMacro", name) ?: return listOf(macro)

        val response = try {
            plugin.process(
                PluginInvocation(
                    extensionPoint = "blockMacro",
                    name = name,
                    attributes = buildMap {
                        macro.target?.let { put("target", it) }
                        putAll(macro.metadata.toAttributeMap())
                    },
                    content = macro.target ?: "",
                    documentAttributes = documentAttributes,
                    location = macro.location.toSourcePoint(),
                    backend = backend,
                ),
            )
        } catch (e: PluginException) {
            warnings += "plugin '${plugin.id}' failed on $name:: block macro at line ${macro.location.lineNumber()}: ${e.message}"
            return listOf(macro)
        }

        warnings += response.warnings.map { "plugin '${plugin.id}': $it" }
        val replacement = response.acceptedReplacement()
        if (replacement == null) {
            response.error?.let {
                warnings += "plugin '${plugin.id}' rejected $name:: block macro at line ${macro.location.lineNumber()}: $it"
            }
            return listOf(macro)
        }
        return spliceBlockReplacement(plugin, replacement, macro, macro.location, warnings)
    }

    /** Turns a block-context replacement into the blocks spliced in place of [original]. */
    private fun spliceBlockReplacement(
        plugin: PluginHandle,
        replacement: PluginReplacement,
        original: Block,
        location: Location?,
        warnings: MutableList<String>,
    ): List<Block> = when (replacement.contentType) {
        "html" -> listOf(
            RawBlock(format = "html", content = replacement.value, location = location),
        )
        // Re-parse and splice: the replacement flows through the normal pipeline.
        "asciidoc" -> DefaultAsciidocParser().parse(replacement.value).document.blocks
        // Official ASG node JSON, decoded and spliced verbatim.
        "asg" -> try {
            asgDecoder.deserializeBlocks(replacement.nodes?.toString() ?: replacement.value)
        } catch (e: Exception) {
            warnings += "plugin '${plugin.id}' returned invalid ASG block replacement: ${e.message}"
            listOf(original)
        }
        else -> {
            warnings += "plugin '${plugin.id}' returned unsupported contentType '${replacement.contentType}'"
            listOf(original)
        }
    }

    // -----------------------------------------------------------------------
    // Inline macros
    // -----------------------------------------------------------------------

    private fun processInlines(
        inlines: List<Inline>,
        documentAttributes: Map<String, String>,
        warnings: MutableList<String>,
    ): List<Inline> = inlines.flatMap { inline ->
        when (inline) {
            is InlineMacro -> processMacro(inline, documentAttributes, warnings)
            is InlineSpan -> listOf(
                inline.copy(inlines = processInlines(inline.inlines, documentAttributes, warnings)),
            )
            is InlineRef -> listOf(
                inline.copy(inlines = processInlines(inline.inlines, documentAttributes, warnings)),
            )
            is InlineFootnote -> listOf(
                inline.copy(inlines = processInlines(inline.inlines, documentAttributes, warnings)),
            )
            else -> listOf(inline)
        }
    }

    private fun processMacro(
        macro: InlineMacro,
        documentAttributes: Map<String, String>,
        warnings: MutableList<String>,
    ): List<Inline> {
        val plugin = engine.forCapability("inlineMacro", macro.name) ?: return listOf(macro)

        val response = try {
            plugin.process(
                PluginInvocation(
                    extensionPoint = "inlineMacro",
                    name = macro.name,
                    attributes = macro.toAttributeMap(),
                    content = macro.target,
                    documentAttributes = documentAttributes,
                    location = macro.location.toSourcePoint(),
                    backend = backend,
                ),
            )
        } catch (e: PluginException) {
            warnings += "plugin '${plugin.id}' failed on ${macro.name} macro at line ${macro.location.lineNumber()}: ${e.message}"
            return listOf(macro)
        }

        warnings += response.warnings.map { "plugin '${plugin.id}': $it" }
        val replacement = response.acceptedReplacement()
        if (replacement == null) {
            response.error?.let {
                warnings += "plugin '${plugin.id}' rejected ${macro.name} macro at line ${macro.location.lineNumber()}: $it"
            }
            return listOf(macro)
        }

        return when (replacement.contentType) {
            "html" -> listOf(
                InlineRaw(format = "html", content = replacement.value, location = macro.location),
            )
            // Re-parse as a snippet and splice the first paragraph's inlines.
            "asciidoc" -> DefaultAsciidocParser().parse(replacement.value).document.blocks
                .filterIsInstance<LeafBlock>()
                .firstOrNull { it.name == LeafBlockName.PARAGRAPH }
                ?.inlines
                ?: listOf(macro)
            // Official ASG inline node JSON, decoded and spliced verbatim.
            // Block nodes in inline context fail the decode -> plugin error path.
            "asg" -> try {
                asgDecoder.deserializeInlines(replacement.nodes?.toString() ?: replacement.value)
            } catch (e: Exception) {
                warnings += "plugin '${plugin.id}' returned invalid ASG inline replacement: ${e.message}"
                listOf(macro)
            }
            else -> {
                warnings += "plugin '${plugin.id}' returned unsupported contentType '${replacement.contentType}'"
                listOf(macro)
            }
        }
    }

    // -----------------------------------------------------------------------
    // Envelope mapping (wire-ABI compatible with the legacy AST integration)
    // -----------------------------------------------------------------------

    /**
     * Attributes map for the envelope: positional by 1-based index plus named,
     * and the block's `id`, `role` (space-separated), `options`
     * (comma-separated) and `title` (plain text) when it has them. A named
     * attribute of the same name wins.
     */
    private fun BlockMetadata?.toAttributeMap(): Map<String, String> = buildMap {
        val metadata = this@toAttributeMap ?: return@buildMap
        metadata.id?.let { put("id", it) }
        if (metadata.roles.isNotEmpty()) put("role", metadata.roles.joinToString(" "))
        if (metadata.options.isNotEmpty()) put("options", metadata.options.joinToString(","))
        metadata.title?.let { put("title", plainText(it)) }
        metadata.positional.forEachIndexed { index, value -> put((index + 1).toString(), value) }
        putAll(metadata.named)
    }

    /** Macro attributes: target plus positional by 1-based index plus named. */
    private fun InlineMacro.toAttributeMap(): Map<String, String> = buildMap {
        put("target", target)
        positional.forEachIndexed { index, value -> put((index + 1).toString(), value) }
        putAll(named)
    }

    /** Block-macro attributes: target plus the metadata mapping. */
    private fun CustomBlockMacro.toAttributeMap(): Map<String, String> = buildMap {
        target?.let { put("target", it) }
        putAll(metadata.toAttributeMap())
    }

    private fun Location?.toSourcePoint(): SourcePoint =
        SourcePoint(this?.start?.line ?: 1, this?.start?.col ?: 1)

    private fun Location?.lineNumber(): Int = this?.start?.line ?: 1
}

@Deprecated("Renamed to WasmExtensions", ReplaceWith("WasmExtensions"))
typealias WasmBlockExtensions = WasmExtensions
