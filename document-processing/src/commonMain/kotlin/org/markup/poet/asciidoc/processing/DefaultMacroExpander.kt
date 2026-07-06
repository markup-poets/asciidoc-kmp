package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.AsgNode
import org.markup.poet.asciidoc.asg.Block
import org.markup.poet.asciidoc.asg.DListBlock
import org.markup.poet.asciidoc.asg.Inline
import org.markup.poet.asciidoc.asg.InlineMacro
import org.markup.poet.asciidoc.asg.InlineSpan
import org.markup.poet.asciidoc.asg.InlineText
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.LeafBlockName
import org.markup.poet.asciidoc.asg.ListBlock
import org.markup.poet.asciidoc.asg.Location
import org.markup.poet.asciidoc.asg.ParentBlock
import org.markup.poet.asciidoc.asg.SectionBlock

/**
 * Default implementation of MacroExpander that processes [InlineMacro] invocations in the document.
 *
 * Built-in macro names (link, image, xref) are structural and left untouched;
 * every other macro must be claimed by a registered [MacroProcessor].
 */
class DefaultMacroExpander : MacroExpander {

    /** Macro names with a structural meaning that must never be treated as custom macros. */
    private val builtinMacroNames = setOf("link", "image", "xref")

    override fun expand(document: AsgDocument, config: MacroConfig): MacroResult {
        val errors = mutableListOf<ProcessingError>()

        val processedBlocks = document.blocks.map { block ->
            expandInBlock(block, document, config, errors)
        }

        return MacroResult(document.copy(blocks = processedBlocks), errors)
    }

    /**
     * Expands macros in a block, recursing into nested blocks and inline lists.
     */
    private fun expandInBlock(
        block: Block,
        document: AsgDocument,
        config: MacroConfig,
        errors: MutableList<ProcessingError>
    ): Block {
        fun recurse(blocks: List<Block>): List<Block> =
            blocks.map { expandInBlock(it, document, config, errors) }

        fun mapInlines(inlines: List<Inline>): List<Inline> =
            expandInInlineList(inlines, document, config, errors)

        return when (block) {
            is SectionBlock -> block.copy(blocks = recurse(block.blocks))
            // Verbatim blocks (listing/literal/...) never contain macros.
            is LeafBlock -> if (block.name == LeafBlockName.PARAGRAPH) {
                block.copy(inlines = mapInlines(block.inlines))
            } else {
                block
            }
            is ParentBlock -> block.copy(blocks = recurse(block.blocks))
            is ListBlock -> block.copy(items = block.items.map { item ->
                item.copy(principal = mapInlines(item.principal), blocks = recurse(item.blocks))
            })
            is DListBlock -> block.copy(items = block.items.map { item ->
                item.copy(
                    terms = item.terms.map { mapInlines(it) },
                    principal = mapInlines(item.principal),
                    blocks = recurse(item.blocks)
                )
            })
            else -> block
        }
    }

    /**
     * Expands macros in a list of inline elements.
     */
    private fun expandInInlineList(
        inlines: List<Inline>,
        document: AsgDocument,
        config: MacroConfig,
        errors: MutableList<ProcessingError>
    ): List<Inline> {
        return inlines.flatMap { inline ->
            expandInInline(inline, document, config, errors)
        }
    }

    /**
     * Expands macros in an inline element.
     * Returns a list of inline elements (may be more than one if macro generates multiple inlines).
     */
    private fun expandInInline(
        inline: Inline,
        document: AsgDocument,
        config: MacroConfig,
        errors: MutableList<ProcessingError>
    ): List<Inline> {
        return when (inline) {
            is InlineMacro -> if (inline.name in builtinMacroNames) {
                listOf(inline)
            } else {
                expandMacro(inline, document, config, errors)
            }
            is InlineSpan -> listOf(
                inline.copy(inlines = expandInInlineList(inline.inlines, document, config, errors))
            )
            // Other inline types don't contain macros
            else -> listOf(inline)
        }
    }

    /**
     * Expands a single macro invocation.
     */
    private fun expandMacro(
        macro: InlineMacro,
        document: AsgDocument,
        config: MacroConfig,
        errors: MutableList<ProcessingError>
    ): List<Inline> {
        val parameters = parametersOf(macro)

        // Find the appropriate macro processor
        val processor = config.customMacros[macro.name]

        if (processor == null) {
            // No processor found for this macro
            errors.add(
                ProcessingError(
                    message = "No processor found for macro '${macro.name}'",
                    location = macro.location,
                    errorType = ProcessingErrorType.MACRO_EXPANSION_FAILED
                )
            )
            // Return the original macro invocation as text
            return listOf(fallbackText(macro, parameters))
        }

        // Validate parameters
        val validationError = validateParameters(parameters, macro.location)
        if (validationError != null) {
            errors.add(validationError)
            return listOf(fallbackText(macro, parameters))
        }

        // Create macro context
        val context = MacroContext(
            document = document,
            location = macro.location
        )

        // Invoke the processor
        val result = try {
            processor.process(macro.name, parameters, context)
        } catch (e: Exception) {
            MacroExpansionResult.Error("Macro processor threw exception: ${e.message}")
        }

        return when (result) {
            is MacroExpansionResult.Success -> {
                // Validate the generated nodes
                val outputError = validateMacroOutput(result.nodes, macro.location)
                if (outputError != null) {
                    errors.add(outputError)
                    listOf(fallbackText(macro, parameters))
                } else {
                    // Filter to only inline elements (macros in inline context can only generate inlines)
                    result.nodes.filterIsInstance<Inline>()
                }
            }
            is MacroExpansionResult.Error -> {
                errors.add(
                    ProcessingError(
                        message = "Macro expansion failed for '${macro.name}': ${result.message}",
                        location = macro.location,
                        errorType = ProcessingErrorType.MACRO_EXPANSION_FAILED
                    )
                )
                listOf(fallbackText(macro, parameters))
            }
        }
    }

    /**
     * Parameters map exposed to [MacroProcessor]s: the target under "target",
     * positional attributes keyed by 1-based index, plus named attributes.
     */
    private fun parametersOf(macro: InlineMacro): Map<String, String> = buildMap {
        if (macro.target.isNotEmpty()) {
            put("target", macro.target)
        }
        macro.positional.forEachIndexed { index, value -> put((index + 1).toString(), value) }
        putAll(macro.named)
    }

    /**
     * Plain-text stand-in for a macro that could not be expanded.
     */
    private fun fallbackText(macro: InlineMacro, parameters: Map<String, String>): InlineText =
        InlineText(
            value = "${macro.name}[${formatParameters(parameters)}]",
            location = macro.location
        )

    /**
     * Validates macro parameters.
     */
    private fun validateParameters(
        parameters: Map<String, String>,
        location: Location?
    ): ProcessingError? {
        // Check for invalid parameter names (empty keys)
        if (parameters.keys.any { it.isEmpty() }) {
            return ProcessingError(
                message = "Macro parameters cannot have empty keys",
                location = location,
                errorType = ProcessingErrorType.MACRO_INVALID_PARAMETERS
            )
        }
        return null
    }

    /**
     * Validates macro output nodes.
     */
    private fun validateMacroOutput(
        nodes: List<AsgNode>,
        location: Location?
    ): ProcessingError? {
        // Empty output is valid; more sophisticated validation could be added here
        return null
    }

    /**
     * Formats parameters for display in error messages.
     */
    private fun formatParameters(parameters: Map<String, String>): String {
        return parameters.entries.joinToString(", ") { (key, value) ->
            "$key=$value"
        }
    }
}
