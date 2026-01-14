package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.ast.*

/**
 * Default implementation of MacroExpander that processes macro invocations in the document.
 */
class DefaultMacroExpander : MacroExpander {
    
    override fun expand(document: Document, config: MacroConfig): MacroResult {
        val errors = mutableListOf<ProcessingError>()
        
        // Process the document and expand all macros
        val processedChildren = document.children.flatMap { child ->
            expandInBlock(child, document, config, errors)
        }
        
        val processedDocument = document.copy(children = processedChildren)
        
        return MacroResult(processedDocument, errors)
    }
    
    /**
     * Expands macros in a block element.
     * Returns a list of block elements (may be more than one if macro generates multiple blocks).
     */
    private fun expandInBlock(
        block: BlockElement,
        document: Document,
        config: MacroConfig,
        errors: MutableList<ProcessingError>
    ): List<BlockElement> {
        return when (block) {
            is Section -> {
                val processedChildren = block.children.flatMap { child ->
                    expandInBlock(child, document, config, errors)
                }
                listOf(block.copy(children = processedChildren))
            }
            is Paragraph -> {
                val processedContent = expandInInlineList(block.content, document, config, errors)
                listOf(block.copy(content = processedContent))
            }
            is AsciiDocList -> {
                val processedItems = block.items.map { item ->
                    expandInListItem(item, document, config, errors)
                }
                listOf(block.copy(items = processedItems))
            }
            is CalloutList -> {
                val processedItems = block.items.map { item ->
                    val processedContent = expandInInlineList(item.content, document, config, errors)
                    item.copy(content = processedContent)
                }
                listOf(block.copy(items = processedItems))
            }
            is ListItem -> {
                listOf(expandInListItem(block, document, config, errors))
            }
            // Other block types don't contain macros
            else -> listOf(block)
        }
    }
    
    /**
     * Expands macros in a list item.
     */
    private fun expandInListItem(
        item: ListItem,
        document: Document,
        config: MacroConfig,
        errors: MutableList<ProcessingError>
    ): ListItem {
        val processedContent = expandInInlineList(item.content, document, config, errors)
        val processedNestedList = item.nestedList?.let { nestedList ->
            val processedItems = nestedList.items.map { nestedItem ->
                expandInListItem(nestedItem, document, config, errors)
            }
            nestedList.copy(items = processedItems)
        }
        return item.copy(content = processedContent, nestedList = processedNestedList)
    }
    
    /**
     * Expands macros in a list of inline elements.
     */
    private fun expandInInlineList(
        inlines: List<InlineElement>,
        document: Document,
        config: MacroConfig,
        errors: MutableList<ProcessingError>
    ): List<InlineElement> {
        return inlines.flatMap { inline ->
            expandInInline(inline, document, config, errors)
        }
    }
    
    /**
     * Expands macros in an inline element.
     * Returns a list of inline elements (may be more than one if macro generates multiple inlines).
     */
    private fun expandInInline(
        inline: InlineElement,
        document: Document,
        config: MacroConfig,
        errors: MutableList<ProcessingError>
    ): List<InlineElement> {
        return when (inline) {
            is MacroInvocation -> {
                // This is a macro invocation - expand it
                expandMacro(inline, document, config, errors)
            }
            is Strong -> {
                val processedContent = expandInInlineList(inline.content, document, config, errors)
                listOf(inline.copy(content = processedContent))
            }
            is Emphasis -> {
                val processedContent = expandInInlineList(inline.content, document, config, errors)
                listOf(inline.copy(content = processedContent))
            }
            // Other inline types don't contain macros
            else -> listOf(inline)
        }
    }
    
    /**
     * Expands a single macro invocation.
     */
    private fun expandMacro(
        macro: MacroInvocation,
        document: Document,
        config: MacroConfig,
        errors: MutableList<ProcessingError>
    ): List<InlineElement> {
        // Find the appropriate macro processor
        val processor = config.customMacros[macro.macroName]
        
        if (processor == null) {
            // No processor found for this macro
            errors.add(
                ProcessingError(
                    message = "No processor found for macro '${macro.macroName}'",
                    location = macro.sourceLocation,
                    errorType = ProcessingErrorType.MACRO_EXPANSION_FAILED
                )
            )
            // Return the original macro invocation as text
            return listOf(
                Text(
                    content = "${macro.macroName}[${formatParameters(macro.parameters)}]",
                    attributes = macro.attributes,
                    sourceLocation = macro.sourceLocation
                )
            )
        }
        
        // Validate parameters
        val validationError = validateParameters(macro.parameters, macro.sourceLocation)
        if (validationError != null) {
            errors.add(validationError)
            return listOf(
                Text(
                    content = "${macro.macroName}[${formatParameters(macro.parameters)}]",
                    attributes = macro.attributes,
                    sourceLocation = macro.sourceLocation
                )
            )
        }
        
        // Create macro context
        val context = MacroContext(
            document = document,
            sourceLocation = macro.sourceLocation
        )
        
        // Invoke the processor
        val result = try {
            processor.process(macro.macroName, macro.parameters, context)
        } catch (e: Exception) {
            MacroExpansionResult.Error("Macro processor threw exception: ${e.message}")
        }
        
        return when (result) {
            is MacroExpansionResult.Success -> {
                // Validate the generated nodes
                val validationError = validateMacroOutput(result.nodes, macro.sourceLocation)
                if (validationError != null) {
                    errors.add(validationError)
                    listOf(
                        Text(
                            content = "${macro.macroName}[${formatParameters(macro.parameters)}]",
                            attributes = macro.attributes,
                            sourceLocation = macro.sourceLocation
                        )
                    )
                } else {
                    // Filter to only inline elements (macros in inline context can only generate inline elements)
                    result.nodes.filterIsInstance<InlineElement>()
                }
            }
            is MacroExpansionResult.Error -> {
                errors.add(
                    ProcessingError(
                        message = "Macro expansion failed for '${macro.macroName}': ${result.message}",
                        location = macro.sourceLocation,
                        errorType = ProcessingErrorType.MACRO_EXPANSION_FAILED
                    )
                )
                listOf(
                    Text(
                        content = "${macro.macroName}[${formatParameters(macro.parameters)}]",
                        attributes = macro.attributes,
                        sourceLocation = macro.sourceLocation
                    )
                )
            }
        }
    }
    
    /**
     * Validates macro parameters.
     */
    private fun validateParameters(
        parameters: Map<String, String>,
        location: SourceLocation
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
        nodes: List<AstNode>,
        location: SourceLocation
    ): ProcessingError? {
        // Check that all nodes are valid
        if (nodes.isEmpty()) {
            // Empty output is valid
            return null
        }
        
        // For now, just check that nodes are not null
        // More sophisticated validation could be added here
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
