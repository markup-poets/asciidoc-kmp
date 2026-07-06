package org.markup.poet.plugin.integration

import org.markup.poet.asciidoc.ast.AsciiDocList
import org.markup.poet.asciidoc.ast.BlockElement
import org.markup.poet.asciidoc.ast.CustomBlock
import org.markup.poet.asciidoc.ast.Document
import org.markup.poet.asciidoc.ast.Emphasis
import org.markup.poet.asciidoc.ast.InlineElement
import org.markup.poet.asciidoc.ast.MacroInvocation
import org.markup.poet.asciidoc.ast.Paragraph
import org.markup.poet.asciidoc.ast.PassthroughBlock
import org.markup.poet.asciidoc.ast.RawInline
import org.markup.poet.asciidoc.ast.Section
import org.markup.poet.asciidoc.ast.Strong
import org.markup.poet.asciidoc.parser.DefaultAsciidocParser
import org.markup.poet.plugin.api.PluginInvocation
import org.markup.poet.plugin.api.SourcePoint
import org.markup.poet.plugin.engine.PluginEngine
import org.markup.poet.plugin.engine.PluginException

/** Result of a plugin pass over a document. */
data class PluginProcessingResult(
    val document: Document,
    val warnings: List<String>,
)

/**
 * Applies WASM extension plugins to a parsed document:
 *
 * - `block` capability: every [CustomBlock] whose style a plugin claims is
 *   replaced by the plugin's output (`html` → [PassthroughBlock], `asciidoc` →
 *   re-parsed and spliced).
 * - `inlineMacro` capability: every [MacroInvocation] whose name a plugin
 *   claims is replaced (`html` → [RawInline], `asciidoc` → re-parsed inline).
 *
 * Unclaimed constructs and failed invocations are left in place (rendering
 * falls back to a listing / placeholder) with a warning.
 */
class WasmExtensions(
    private val engine: PluginEngine,
) {
    fun apply(document: Document): PluginProcessingResult {
        val warnings = mutableListOf<String>()
        val children = processBlocks(document.children, document.documentAttributes, warnings)
        return PluginProcessingResult(document.copy(children = children), warnings)
    }

    // -----------------------------------------------------------------------
    // Blocks
    // -----------------------------------------------------------------------

    private fun processBlocks(
        blocks: List<BlockElement>,
        documentAttributes: Map<String, String>,
        warnings: MutableList<String>,
    ): List<BlockElement> = blocks.flatMap { block ->
        when (block) {
            is CustomBlock -> processCustomBlock(block, documentAttributes, warnings)
            is Section -> listOf(
                block.copy(children = processBlocks(block.children, documentAttributes, warnings)),
            )
            is Paragraph -> listOf(
                block.copy(content = processInlines(block.content, documentAttributes, warnings)),
            )
            is AsciiDocList -> listOf(
                block.copy(items = block.items.map { item ->
                    item.copy(content = processInlines(item.content, documentAttributes, warnings))
                }),
            )
            else -> listOf(block)
        }
    }

    private fun processCustomBlock(
        block: CustomBlock,
        documentAttributes: Map<String, String>,
        warnings: MutableList<String>,
    ): List<BlockElement> {
        val plugin = engine.forCapability("block", block.name) ?: return listOf(block)

        val response = try {
            plugin.process(
                PluginInvocation(
                    extensionPoint = "block",
                    name = block.name,
                    attributes = block.attributes,
                    content = block.rawContent,
                    documentAttributes = documentAttributes,
                    location = SourcePoint(block.sourceLocation.line, block.sourceLocation.column),
                ),
            )
        } catch (e: PluginException) {
            warnings += "plugin '${plugin.id}' failed on [${block.name}] block at line ${block.sourceLocation.line}: ${e.message}"
            return listOf(block)
        }

        warnings += response.warnings.map { "plugin '${plugin.id}': $it" }
        val replacement = response.replacement
        if (!response.ok || replacement == null) {
            response.error?.let {
                warnings += "plugin '${plugin.id}' rejected [${block.name}] block at line ${block.sourceLocation.line}: $it"
            }
            return listOf(block)
        }

        return when (replacement.contentType) {
            "html" -> listOf(
                PassthroughBlock(
                    format = "html",
                    content = replacement.value,
                    sourceLocation = block.sourceLocation,
                ),
            )
            // Re-parse and splice: the replacement flows through the normal pipeline.
            "asciidoc" -> DefaultAsciidocParser().parse(replacement.value).document.children
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
        inlines: List<InlineElement>,
        documentAttributes: Map<String, String>,
        warnings: MutableList<String>,
    ): List<InlineElement> = inlines.flatMap { inline ->
        when (inline) {
            is MacroInvocation -> processMacro(inline, documentAttributes, warnings)
            is Strong -> listOf(
                inline.copy(content = processInlines(inline.content, documentAttributes, warnings)),
            )
            is Emphasis -> listOf(
                inline.copy(content = processInlines(inline.content, documentAttributes, warnings)),
            )
            else -> listOf(inline)
        }
    }

    private fun processMacro(
        macro: MacroInvocation,
        documentAttributes: Map<String, String>,
        warnings: MutableList<String>,
    ): List<InlineElement> {
        val plugin = engine.forCapability("inlineMacro", macro.macroName) ?: return listOf(macro)

        val response = try {
            plugin.process(
                PluginInvocation(
                    extensionPoint = "inlineMacro",
                    name = macro.macroName,
                    attributes = macro.parameters,
                    content = macro.parameters["target"] ?: "",
                    documentAttributes = documentAttributes,
                    location = SourcePoint(macro.sourceLocation.line, macro.sourceLocation.column),
                ),
            )
        } catch (e: PluginException) {
            warnings += "plugin '${plugin.id}' failed on ${macro.macroName} macro at line ${macro.sourceLocation.line}: ${e.message}"
            return listOf(macro)
        }

        warnings += response.warnings.map { "plugin '${plugin.id}': $it" }
        val replacement = response.replacement
        if (!response.ok || replacement == null) {
            response.error?.let {
                warnings += "plugin '${plugin.id}' rejected ${macro.macroName} macro at line ${macro.sourceLocation.line}: $it"
            }
            return listOf(macro)
        }

        return when (replacement.contentType) {
            "html" -> listOf(
                RawInline(format = "html", content = replacement.value, sourceLocation = macro.sourceLocation),
            )
            // Re-parse as a snippet and splice the first paragraph's inlines.
            "asciidoc" -> DefaultAsciidocParser().parse(replacement.value).document.children
                .filterIsInstance<Paragraph>()
                .firstOrNull()?.content
                ?: listOf(macro)
            else -> {
                warnings += "plugin '${plugin.id}' returned unsupported contentType '${replacement.contentType}'"
                listOf(macro)
            }
        }
    }
}

@Deprecated("Renamed to WasmExtensions", ReplaceWith("WasmExtensions"))
typealias WasmBlockExtensions = WasmExtensions
