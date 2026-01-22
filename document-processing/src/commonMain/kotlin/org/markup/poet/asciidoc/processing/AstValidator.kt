package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.ast.*

/**
 * Validates AST structure for correctness.
 * Used to validate output from custom processors.
 */
object AstValidator {
    /**
     * Validates a document's AST structure.
     * Returns a list of validation errors if any are found.
     */
    fun validateDocument(document: Document): List<String> {
        val errors = mutableListOf<String>()
        
        // Validate document attributes
        for ((key, _) in document.documentAttributes) {
            if (key.isEmpty()) {
                errors.add("Document attribute key cannot be empty")
            }
        }
        
        // Validate child blocks
        for (block in document.children) {
            errors.addAll(validateBlock(block))
        }
        
        return errors
    }
    
    /**
     * Validates a block element.
     */
    private fun validateBlock(block: BlockElement): List<String> {
        val errors = mutableListOf<String>()
        
        when (block) {
            is Section -> {
                // Validate section level
                if (block.level < 1 || block.level > 6) {
                    errors.add("Section level must be between 1 and 6, got ${block.level}")
                }
                
                // Validate title
                if (block.title.isEmpty()) {
                    errors.add("Section title cannot be empty")
                }
                
                // Validate child blocks
                for (child in block.children) {
                    errors.addAll(validateBlock(child))
                }
            }
            
            is Paragraph -> {
                // Validate inline content
                for (inline in block.content) {
                    errors.addAll(validateInline(inline))
                }
            }
            
            is AsciiDocList -> {
                // Validate list items
                for (item in block.items) {
                    errors.addAll(validateListItem(item))
                }
            }
            
            is CodeBlock -> {
                // Code blocks are generally valid as-is
                // Could add validation for language if needed
            }
            
            else -> {
                // Other block types are assumed valid
            }
        }
        
        return errors
    }
    
    /**
     * Validates a list item.
     */
    private fun validateListItem(item: ListItem): List<String> {
        val errors = mutableListOf<String>()
        
        // Validate inline content
        for (inline in item.content) {
            errors.addAll(validateInline(inline))
        }
        
        // Validate nested list if present
        item.nestedList?.let { nestedList ->
            for (nestedItem in nestedList.items) {
                errors.addAll(validateListItem(nestedItem))
            }
        }
        
        return errors
    }
    
    /**
     * Validates an inline element.
     */
    private fun validateInline(inline: InlineElement): List<String> {
        val errors = mutableListOf<String>()
        
        when (inline) {
            is Text -> {
                // Text can be empty, so no validation needed
            }
            
            is Strong -> {
                // Validate nested content
                for (content in inline.content) {
                    errors.addAll(validateInline(content))
                }
            }
            
            is Emphasis -> {
                // Validate nested content
                for (content in inline.content) {
                    errors.addAll(validateInline(content))
                }
            }
            
            is Code -> {
                if (inline.content.isEmpty()) {
                    errors.add("Code content cannot be empty")
                }
            }
            
            is Link -> {
                if (inline.url.isEmpty()) {
                    errors.add("Link URL cannot be empty")
                }
            }
            
            else -> {
                // Other inline types are assumed valid
            }
        }
        
        return errors
    }
}
