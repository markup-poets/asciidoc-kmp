package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.ast.*

/**
 * Default implementation of BibliographyManager.
 * Collects footnotes, assigns sequential numbers, indexes bibliography entries,
 * and resolves references.
 */
class DefaultBibliographyManager : BibliographyManager {
    
    override fun process(document: Document): BibliographyResult {
        val warnings = mutableListOf<ProcessingWarning>()
        val footnoteMap = mutableMapOf<String, Footnote>()
        val bibliographyMap = mutableMapOf<String, BibliographyEntryData>()
        val footnoteOrder = mutableListOf<String>()
        
        // First pass: collect all footnotes and bibliography entries
        collectFootnotesAndBibliography(
            document,
            footnoteMap,
            bibliographyMap,
            footnoteOrder
        )
        
        // Assign sequential numbers to footnotes based on first occurrence
        val numberedFootnotes = assignFootnoteNumbers(footnoteMap, footnoteOrder)
        
        // Second pass: validate all references
        validateReferences(
            document,
            numberedFootnotes,
            bibliographyMap,
            warnings
        )
        
        return BibliographyResult(
            document = document,
            footnotes = numberedFootnotes.values.sortedBy { it.number },
            bibliography = bibliographyMap,
            warnings = warnings
        )
    }
    
    /**
     * Collect all footnotes and bibliography entries from the document.
     */
    private fun collectFootnotesAndBibliography(
        document: Document,
        footnoteMap: MutableMap<String, Footnote>,
        bibliographyMap: MutableMap<String, BibliographyEntryData>,
        footnoteOrder: MutableList<String>
    ) {
        collectFromBlockElements(
            document.children,
            footnoteMap,
            bibliographyMap,
            footnoteOrder
        )
    }
    
    /**
     * Recursively collect footnotes and bibliography entries from block elements.
     */
    private fun collectFromBlockElements(
        elements: List<BlockElement>,
        footnoteMap: MutableMap<String, Footnote>,
        bibliographyMap: MutableMap<String, BibliographyEntryData>,
        footnoteOrder: MutableList<String>
    ) {
        for (element in elements) {
            when (element) {
                is Paragraph -> {
                    collectFromInlineElements(
                        element.content,
                        footnoteMap,
                        footnoteOrder
                    )
                }
                is Section -> {
                    collectFromBlockElements(
                        element.children,
                        footnoteMap,
                        bibliographyMap,
                        footnoteOrder
                    )
                }
                is AsciiDocList -> {
                    for (item in element.items) {
                        collectFromListItem(
                            item,
                            footnoteMap,
                            footnoteOrder
                        )
                    }
                }
                is AdmonitionBlock -> {
                    collectFromBlockElements(
                        element.content,
                        footnoteMap,
                        bibliographyMap,
                        footnoteOrder
                    )
                }
                is ConditionalDirective -> {
                    collectFromBlockElements(
                        element.content,
                        footnoteMap,
                        bibliographyMap,
                        footnoteOrder
                    )
                    collectFromBlockElements(
                        element.elseContent,
                        footnoteMap,
                        bibliographyMap,
                        footnoteOrder
                    )
                }
                is BibliographyEntry -> {
                    // Collect bibliography entry
                    bibliographyMap[element.id] = BibliographyEntryData(
                        id = element.id,
                        citation = element.citation,
                        metadata = element.metadata,
                        sourceLocation = element.sourceLocation
                    )
                }
                is Document -> {
                    collectFootnotesAndBibliography(
                        element,
                        footnoteMap,
                        bibliographyMap,
                        footnoteOrder
                    )
                }
                else -> {
                    // Other block types don't contain inline content
                }
            }
        }
    }
    
    /**
     * Collect footnotes from list items.
     */
    private fun collectFromListItem(
        item: ListItem,
        footnoteMap: MutableMap<String, Footnote>,
        footnoteOrder: MutableList<String>
    ) {
        collectFromInlineElements(item.content, footnoteMap, footnoteOrder)
        
        item.nestedList?.let { nestedList ->
            for (nestedItem in nestedList.items) {
                collectFromListItem(nestedItem, footnoteMap, footnoteOrder)
            }
        }
    }
    
    /**
     * Collect footnotes from inline elements.
     */
    private fun collectFromInlineElements(
        elements: List<InlineElement>,
        footnoteMap: MutableMap<String, Footnote>,
        footnoteOrder: MutableList<String>
    ) {
        for (element in elements) {
            when (element) {
                is FootnoteReference -> {
                    // Track first occurrence order
                    if (!footnoteMap.containsKey(element.id)) {
                        footnoteOrder.add(element.id)
                        // Create footnote with placeholder number (will be assigned later)
                        footnoteMap[element.id] = Footnote(
                            id = element.id,
                            number = 0, // Placeholder
                            content = element.content,
                            sourceLocation = element.sourceLocation
                        )
                    }
                }
                is Strong -> {
                    collectFromInlineElements(element.content, footnoteMap, footnoteOrder)
                }
                is Emphasis -> {
                    collectFromInlineElements(element.content, footnoteMap, footnoteOrder)
                }
                else -> {
                    // Other inline types don't contain nested elements
                }
            }
        }
    }
    
    /**
     * Assign sequential numbers to footnotes based on first occurrence order.
     */
    private fun assignFootnoteNumbers(
        footnoteMap: Map<String, Footnote>,
        footnoteOrder: List<String>
    ): Map<String, Footnote> {
        val numberedFootnotes = mutableMapOf<String, Footnote>()
        
        footnoteOrder.forEachIndexed { index, id ->
            val footnote = footnoteMap[id]
            if (footnote != null) {
                numberedFootnotes[id] = footnote.copy(number = index + 1)
            }
        }
        
        return numberedFootnotes
    }
    
    /**
     * Validate all footnote and bibliography references in the document.
     */
    private fun validateReferences(
        document: Document,
        footnoteMap: Map<String, Footnote>,
        bibliographyMap: Map<String, BibliographyEntryData>,
        warnings: MutableList<ProcessingWarning>
    ) {
        validateBlockElements(
            document.children,
            footnoteMap,
            bibliographyMap,
            warnings
        )
    }
    
    /**
     * Recursively validate references in block elements.
     */
    private fun validateBlockElements(
        elements: List<BlockElement>,
        footnoteMap: Map<String, Footnote>,
        bibliographyMap: Map<String, BibliographyEntryData>,
        warnings: MutableList<ProcessingWarning>
    ) {
        for (element in elements) {
            when (element) {
                is Paragraph -> {
                    validateInlineElements(
                        element.content,
                        footnoteMap,
                        bibliographyMap,
                        warnings
                    )
                }
                is Section -> {
                    validateBlockElements(
                        element.children,
                        footnoteMap,
                        bibliographyMap,
                        warnings
                    )
                }
                is AsciiDocList -> {
                    for (item in element.items) {
                        validateListItem(
                            item,
                            footnoteMap,
                            bibliographyMap,
                            warnings
                        )
                    }
                }
                is AdmonitionBlock -> {
                    validateBlockElements(
                        element.content,
                        footnoteMap,
                        bibliographyMap,
                        warnings
                    )
                }
                is ConditionalDirective -> {
                    validateBlockElements(
                        element.content,
                        footnoteMap,
                        bibliographyMap,
                        warnings
                    )
                    validateBlockElements(
                        element.elseContent,
                        footnoteMap,
                        bibliographyMap,
                        warnings
                    )
                }
                is Document -> {
                    validateReferences(
                        element,
                        footnoteMap,
                        bibliographyMap,
                        warnings
                    )
                }
                else -> {
                    // Other block types don't contain inline content
                }
            }
        }
    }
    
    /**
     * Validate references in list items.
     */
    private fun validateListItem(
        item: ListItem,
        footnoteMap: Map<String, Footnote>,
        bibliographyMap: Map<String, BibliographyEntryData>,
        warnings: MutableList<ProcessingWarning>
    ) {
        validateInlineElements(item.content, footnoteMap, bibliographyMap, warnings)
        
        item.nestedList?.let { nestedList ->
            for (nestedItem in nestedList.items) {
                validateListItem(nestedItem, footnoteMap, bibliographyMap, warnings)
            }
        }
    }
    
    /**
     * Validate footnote and bibliography references in inline elements.
     */
    private fun validateInlineElements(
        elements: List<InlineElement>,
        footnoteMap: Map<String, Footnote>,
        bibliographyMap: Map<String, BibliographyEntryData>,
        warnings: MutableList<ProcessingWarning>
    ) {
        for (element in elements) {
            when (element) {
                is FootnoteReference -> {
                    if (!footnoteMap.containsKey(element.id)) {
                        warnings.add(
                            ProcessingWarning(
                                message = "Unresolved footnote reference: ${element.id}",
                                location = element.sourceLocation,
                                warningType = ProcessingWarningType.FOOTNOTE_UNRESOLVED
                            )
                        )
                    }
                }
                is BibliographyReference -> {
                    if (!bibliographyMap.containsKey(element.citationId)) {
                        warnings.add(
                            ProcessingWarning(
                                message = "Unresolved bibliography reference: ${element.citationId}",
                                location = element.sourceLocation,
                                warningType = ProcessingWarningType.BIBLIOGRAPHY_UNRESOLVED
                            )
                        )
                    }
                }
                is Strong -> {
                    validateInlineElements(element.content, footnoteMap, bibliographyMap, warnings)
                }
                is Emphasis -> {
                    validateInlineElements(element.content, footnoteMap, bibliographyMap, warnings)
                }
                else -> {
                    // Other inline types don't contain nested elements
                }
            }
        }
    }
}
