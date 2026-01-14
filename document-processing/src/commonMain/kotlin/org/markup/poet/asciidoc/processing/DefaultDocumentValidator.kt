package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.ast.*

/**
 * Default implementation of DocumentValidator.
 * Validates document structure including section hierarchy, duplicate anchors, and invalid references.
 */
class DefaultDocumentValidator : DocumentValidator {
    
    override fun validate(document: Document, config: ValidationConfig): ValidationResult {
        val errors = mutableListOf<ProcessingError>()
        val warnings = mutableListOf<ProcessingWarning>()
        
        // Validate section hierarchy
        if (config.checkSectionHierarchy) {
            validateSectionHierarchy(document, warnings)
        }
        
        // Check for duplicate anchors
        if (config.checkDuplicateAnchors) {
            checkDuplicateAnchors(document, errors)
        }
        
        // Check for invalid attribute references
        if (config.checkInvalidReferences) {
            checkInvalidAttributeReferences(document, warnings)
        }
        
        // Normalize whitespace
        // Note: Whitespace normalization is typically done during parsing or rendering,
        // but we can check for common issues here
        checkWhitespaceIssues(document, warnings)
        
        return ValidationResult.fromIssues(errors, warnings)
    }
    
    /**
     * Validates that section levels follow proper hierarchy (no skipped levels).
     * Reports violations with location information.
     */
    private fun validateSectionHierarchy(
        document: Document,
        warnings: MutableList<ProcessingWarning>
    ) {
        // Track the previous section level to detect skips
        var previousLevel: Int? = null
        
        // Traverse all sections in the document
        traverseSections(document.children, previousLevel, warnings) { level ->
            previousLevel = level
        }
    }
    
    /**
     * Recursively traverses sections to validate hierarchy.
     */
    private fun traverseSections(
        elements: List<BlockElement>,
        previousLevel: Int?,
        warnings: MutableList<ProcessingWarning>,
        updateLevel: (Int) -> Unit
    ) {
        for (element in elements) {
            when (element) {
                is Section -> {
                    // Check if this section skips levels
                    if (previousLevel != null) {
                        val levelDiff = element.level - previousLevel
                        if (levelDiff > 1) {
                            warnings.add(
                                ProcessingWarning(
                                    message = "Section level ${element.level} skips from level $previousLevel (expected level ${previousLevel + 1})",
                                    location = element.sourceLocation,
                                    warningType = ProcessingWarningType.SECTION_HIERARCHY_VIOLATION
                                )
                            )
                        }
                    }
                    
                    // Update the previous level
                    updateLevel(element.level)
                    
                    // Recursively check children with the current level as context
                    traverseSections(element.children, element.level, warnings, updateLevel)
                }
                is AsciiDocList -> {
                    // Lists don't affect section hierarchy, but may contain nested content
                    for (item in element.items) {
                        if (item.nestedList != null) {
                            traverseSections(listOf(item.nestedList as BlockElement), previousLevel, warnings, updateLevel)
                        }
                    }
                }
                else -> {
                    // Other block elements don't affect section hierarchy
                }
            }
        }
    }
    
    /**
     * Checks for duplicate anchor IDs in the document.
     * Reuses logic from cross-reference resolver.
     */
    private fun checkDuplicateAnchors(
        document: Document,
        errors: MutableList<ProcessingError>
    ) {
        val anchorOccurrences = mutableMapOf<String, MutableList<AstNode>>()
        
        // Traverse document to collect all anchors
        collectAnchors(document.children, anchorOccurrences)
        
        // Report duplicate anchors as errors
        for ((anchorId, nodes) in anchorOccurrences) {
            if (nodes.size > 1) {
                val locations = nodes.joinToString(", ") { 
                    "line ${it.sourceLocation.line}" 
                }
                errors.add(
                    ProcessingError(
                        message = "Duplicate anchor ID '$anchorId' found at: $locations",
                        location = nodes.first().sourceLocation,
                        errorType = ProcessingErrorType.VALIDATION_DUPLICATE_ANCHOR,
                        severity = ErrorSeverity.ERROR
                    )
                )
            }
        }
    }
    
    /**
     * Recursively collects all anchors from the document tree.
     */
    private fun collectAnchors(
        elements: List<BlockElement>,
        anchorOccurrences: MutableMap<String, MutableList<AstNode>>
    ) {
        for (element in elements) {
            // Check if element has an anchor ID attribute
            val anchorId = element.attributes["id"]
            if (anchorId != null) {
                anchorOccurrences.getOrPut(anchorId) { mutableListOf() }.add(element)
            }
            
            // Recursively traverse children
            when (element) {
                is Section -> collectAnchors(element.children, anchorOccurrences)
                is AsciiDocList -> {
                    for (item in element.items) {
                        // Check list items for anchors
                        val itemAnchorId = item.attributes["id"]
                        if (itemAnchorId != null) {
                            anchorOccurrences.getOrPut(itemAnchorId) { mutableListOf() }.add(item)
                        }
                        // Traverse nested lists
                        if (item.nestedList != null) {
                            collectAnchors(listOf(item.nestedList as BlockElement), anchorOccurrences)
                        }
                    }
                }
                is CalloutList -> {
                    for (item in element.items) {
                        val itemAnchorId = item.attributes["id"]
                        if (itemAnchorId != null) {
                            anchorOccurrences.getOrPut(itemAnchorId) { mutableListOf() }.add(item)
                        }
                    }
                }
                else -> {
                    // Other block elements don't have children to traverse
                }
            }
        }
    }
    
    /**
     * Checks for invalid attribute references in the document.
     * Collects all issues and reports them in a single validation report.
     */
    private fun checkInvalidAttributeReferences(
        document: Document,
        warnings: MutableList<ProcessingWarning>
    ) {
        val invalidReferences = mutableListOf<Pair<String, SourceLocation>>()
        
        // Collect all attribute references
        collectAttributeReferences(document.children, document.documentAttributes, invalidReferences)
        
        // Report all invalid references
        for ((key, location) in invalidReferences) {
            warnings.add(
                ProcessingWarning(
                    message = "Invalid attribute reference '{$key}' - attribute is not defined",
                    location = location,
                    warningType = ProcessingWarningType.ATTRIBUTE_UNDEFINED
                )
            )
        }
    }
    
    /**
     * Recursively collects invalid attribute references from the document tree.
     */
    private fun collectAttributeReferences(
        elements: List<BlockElement>,
        documentAttributes: Map<String, String>,
        invalidReferences: MutableList<Pair<String, SourceLocation>>
    ) {
        for (element in elements) {
            when (element) {
                is Section -> {
                    collectAttributeReferences(element.children, documentAttributes, invalidReferences)
                }
                is Paragraph -> {
                    collectAttributeReferencesFromInline(element.content, documentAttributes, invalidReferences)
                }
                is AsciiDocList -> {
                    for (item in element.items) {
                        collectAttributeReferencesFromInline(item.content, documentAttributes, invalidReferences)
                        if (item.nestedList != null) {
                            collectAttributeReferences(listOf(item.nestedList as BlockElement), documentAttributes, invalidReferences)
                        }
                    }
                }
                is CalloutList -> {
                    for (item in element.items) {
                        collectAttributeReferencesFromInline(item.content, documentAttributes, invalidReferences)
                    }
                }
                else -> {
                    // Other block elements don't contain inline content
                }
            }
        }
    }
    
    /**
     * Collects invalid attribute references from inline elements.
     */
    private fun collectAttributeReferencesFromInline(
        inlineElements: List<InlineElement>,
        documentAttributes: Map<String, String>,
        invalidReferences: MutableList<Pair<String, SourceLocation>>
    ) {
        for (inline in inlineElements) {
            when (inline) {
                is AttributeReference -> {
                    // Check if attribute is defined
                    if (!documentAttributes.containsKey(inline.key)) {
                        invalidReferences.add(Pair(inline.key, inline.sourceLocation))
                    }
                }
                is Strong -> {
                    collectAttributeReferencesFromInline(inline.content, documentAttributes, invalidReferences)
                }
                is Emphasis -> {
                    collectAttributeReferencesFromInline(inline.content, documentAttributes, invalidReferences)
                }
                else -> {
                    // Other inline elements don't contain nested elements or attribute references
                }
            }
        }
    }
    
    /**
     * Checks for common whitespace issues in the document.
     * Reports warnings for normalization opportunities.
     */
    private fun checkWhitespaceIssues(
        document: Document,
        warnings: MutableList<ProcessingWarning>
    ) {
        // Check for whitespace issues in text content
        checkWhitespaceInBlocks(document.children, warnings)
    }
    
    /**
     * Recursively checks for whitespace issues in block elements.
     */
    private fun checkWhitespaceInBlocks(
        elements: List<BlockElement>,
        warnings: MutableList<ProcessingWarning>
    ) {
        for (element in elements) {
            when (element) {
                is Section -> {
                    // Check section title for leading/trailing whitespace
                    if (element.title.startsWith(" ") || element.title.endsWith(" ")) {
                        warnings.add(
                            ProcessingWarning(
                                message = "Section title has leading or trailing whitespace: '${element.title}'",
                                location = element.sourceLocation,
                                warningType = ProcessingWarningType.WHITESPACE_NORMALIZATION
                            )
                        )
                    }
                    checkWhitespaceInBlocks(element.children, warnings)
                }
                is Paragraph -> {
                    checkWhitespaceInInline(element.content, element.sourceLocation, warnings)
                }
                is AsciiDocList -> {
                    for (item in element.items) {
                        checkWhitespaceInInline(item.content, item.sourceLocation, warnings)
                        if (item.nestedList != null) {
                            checkWhitespaceInBlocks(listOf(item.nestedList as BlockElement), warnings)
                        }
                    }
                }
                is CalloutList -> {
                    for (item in element.items) {
                        checkWhitespaceInInline(item.content, item.sourceLocation, warnings)
                    }
                }
                else -> {
                    // Other block elements don't have text content to check
                }
            }
        }
    }
    
    /**
     * Checks for whitespace issues in inline elements.
     */
    private fun checkWhitespaceInInline(
        inlineElements: List<InlineElement>,
        location: SourceLocation,
        warnings: MutableList<ProcessingWarning>
    ) {
        for (inline in inlineElements) {
            when (inline) {
                is Text -> {
                    // Check for multiple consecutive spaces
                    if (inline.content.contains("  ")) {
                        warnings.add(
                            ProcessingWarning(
                                message = "Text contains multiple consecutive spaces",
                                location = inline.sourceLocation,
                                warningType = ProcessingWarningType.WHITESPACE_NORMALIZATION
                            )
                        )
                    }
                }
                is Strong -> {
                    checkWhitespaceInInline(inline.content, inline.sourceLocation, warnings)
                }
                is Emphasis -> {
                    checkWhitespaceInInline(inline.content, inline.sourceLocation, warnings)
                }
                else -> {
                    // Other inline elements don't have text content to check
                }
            }
        }
    }
}
