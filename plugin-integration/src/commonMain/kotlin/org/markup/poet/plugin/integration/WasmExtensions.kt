package org.markup.poet.plugin.integration

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.Block
import org.markup.poet.asciidoc.asg.BlockMetadata
import org.markup.poet.asciidoc.asg.ConditionalBlock
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
import org.markup.poet.asciidoc.parser.DefaultAsciidocParser
import org.markup.poet.plugin.api.PluginInvocation
import org.markup.poet.plugin.api.SourcePoint
import org.markup.poet.plugin.engine.PluginEngine
import org.markup.poet.plugin.engine.PluginException

/** Result of a plugin pass over a document. */
data class PluginProcessingResult(
    val document: AsgDocument,
    val warnings: List<String>,
)

/**
 * Applies WASM extension plugins to a parsed ASG document:
 *
 * - `block` capability: every [LeafBlock] whose non-built-in style
 *   (`metadata.positional.first()`) a plugin claims is replaced by the plugin's
 *   output (`html` -> [RawBlock], `asciidoc` -> re-parsed and spliced).
 * - `inlineMacro` capability: every [InlineMacro] whose name a plugin claims is
 *   replaced (`html` -> [InlineRaw], `asciidoc` -> re-parsed inline).
 *
 * The wire ABI (the JSON envelope in [PluginInvocation]) is unchanged from the
 * legacy AST integration: the block style / macro name maps to `name`, the raw
 * block text / macro target maps to `content`, and [BlockMetadata] maps to the
 * `attributes` map with positional attributes keyed by their 1-based index.
 *
 * Unclaimed constructs and failed invocations are left in place (rendering
 * falls back to a listing / placeholder) with a warning.
 */
class WasmExtensions(
    private val engine: PluginEngine,
) {
    fun apply(document: AsgDocument): PluginProcessingResult {
        val warnings = mutableListOf<String>()
        val blocks = processBlocks(document.blocks, document.attributes, warnings)
        return PluginProcessingResult(document.copy(blocks = blocks), warnings)
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
     * A leaf block is plugin-claimable when its style is not one of the
     * built-in styles; otherwise only its inline content is processed.
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
                ),
            )
        } catch (e: PluginException) {
            warnings += "plugin '${plugin.id}' failed on [$style] block at line ${block.location.lineNumber()}: ${e.message}"
            return listOf(block)
        }

        warnings += response.warnings.map { "plugin '${plugin.id}': $it" }
        val replacement = response.replacement
        if (!response.ok || replacement == null) {
            response.error?.let {
                warnings += "plugin '${plugin.id}' rejected [$style] block at line ${block.location.lineNumber()}: $it"
            }
            return listOf(block)
        }

        return when (replacement.contentType) {
            "html" -> listOf(
                RawBlock(
                    format = "html",
                    content = replacement.value,
                    location = block.location,
                ),
            )
            // Re-parse and splice: the replacement flows through the normal pipeline.
            "asciidoc" -> DefaultAsciidocParser().parse(replacement.value).document.blocks
            else -> {
                warnings += "plugin '${plugin.id}' returned unsupported contentType '${replacement.contentType}'"
                listOf(block)
            }
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
                ),
            )
        } catch (e: PluginException) {
            warnings += "plugin '${plugin.id}' failed on ${macro.name} macro at line ${macro.location.lineNumber()}: ${e.message}"
            return listOf(macro)
        }

        warnings += response.warnings.map { "plugin '${plugin.id}': $it" }
        val replacement = response.replacement
        if (!response.ok || replacement == null) {
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
            else -> {
                warnings += "plugin '${plugin.id}' returned unsupported contentType '${replacement.contentType}'"
                listOf(macro)
            }
        }
    }

    // -----------------------------------------------------------------------
    // Envelope mapping (wire-ABI compatible with the legacy AST integration)
    // -----------------------------------------------------------------------

    /** Attributes map for the envelope: positional by 1-based index plus named. */
    private fun BlockMetadata?.toAttributeMap(): Map<String, String> = buildMap {
        this@toAttributeMap?.positional?.forEachIndexed { index, value -> put((index + 1).toString(), value) }
        this@toAttributeMap?.named?.let { putAll(it) }
    }

    /** Macro attributes: target plus positional by 1-based index plus named. */
    private fun InlineMacro.toAttributeMap(): Map<String, String> = buildMap {
        put("target", target)
        positional.forEachIndexed { index, value -> put((index + 1).toString(), value) }
        putAll(named)
    }

    private fun Location?.toSourcePoint(): SourcePoint =
        SourcePoint(this?.start?.line ?: 1, this?.start?.col ?: 1)

    private fun Location?.lineNumber(): Int = this?.start?.line ?: 1
}

@Deprecated("Renamed to WasmExtensions", ReplaceWith("WasmExtensions"))
typealias WasmBlockExtensions = WasmExtensions
