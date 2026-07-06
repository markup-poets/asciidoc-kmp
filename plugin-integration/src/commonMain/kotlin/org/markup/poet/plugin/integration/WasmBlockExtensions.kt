package org.markup.poet.plugin.integration

import org.markup.poet.asciidoc.ast.BlockElement
import org.markup.poet.asciidoc.ast.CustomBlock
import org.markup.poet.asciidoc.ast.Document
import org.markup.poet.asciidoc.ast.PassthroughBlock
import org.markup.poet.asciidoc.ast.Section
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
 * Applies `block`-capability WASM plugins to a parsed document: every
 * [CustomBlock] whose name a loaded plugin claims is replaced by the plugin's
 * output. Unclaimed custom blocks and failed invocations are left in place
 * (they render via the listing fallback) with a warning.
 */
class WasmBlockExtensions(
    private val engine: PluginEngine,
) {
    fun apply(document: Document): PluginProcessingResult {
        val warnings = mutableListOf<String>()
        val children = processBlocks(document.children, document.documentAttributes, warnings)
        return PluginProcessingResult(document.copy(children = children), warnings)
    }

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
        if (!response.ok || response.replacement == null) {
            response.error?.let {
                warnings += "plugin '${plugin.id}' rejected [${block.name}] block at line ${block.sourceLocation.line}: $it"
            }
            return listOf(block)
        }

        val replacement = response.replacement!!
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
}
