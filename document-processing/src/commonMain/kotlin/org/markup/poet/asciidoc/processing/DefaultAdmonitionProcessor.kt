package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.ast.*

/**
 * Default implementation of AdmonitionProcessor.
 * Processes admonition blocks by identifying types, extracting content,
 * handling custom titles, and validating structure.
 */
class DefaultAdmonitionProcessor : AdmonitionProcessor {
    
    override fun process(document: Document): AdmonitionResult {
        val warnings = mutableListOf<ProcessingWarning>()
        val admonitionCounts = mutableMapOf<AdmonitionType, Int>()
        
        val processedDocument = processDocument(document, warnings, admonitionCounts)
        
        return AdmonitionResult(
            document = processedDocument,
            warnings = warnings,
            admonitionCount = admonitionCounts.toMap()
        )
    }
    
    private fun processDocument(
        document: Document,
        warnings: MutableList<ProcessingWarning>,
        counts: MutableMap<AdmonitionType, Int>
    ): Document {
        val processedChildren = processBlockElements(
            document.children,
            warnings,
            counts
        )
        
        return document.copy(children = processedChildren)
    }
    
    private fun processBlockElements(
        elements: List<BlockElement>,
        warnings: MutableList<ProcessingWarning>,
        counts: MutableMap<AdmonitionType, Int>
    ): List<BlockElement> {
        val result = mutableListOf<BlockElement>()
        
        for (element in elements) {
            when (element) {
                is Paragraph -> {
                    // Check if this paragraph is an admonition
                    val admonition = tryParseAdmonition(element, warnings, counts)
                    if (admonition != null) {
                        result.add(admonition)
                    } else {
                        result.add(element)
                    }
                }
                is Section -> {
                    val processedChildren = processBlockElements(
                        element.children,
                        warnings,
                        counts
                    )
                    result.add(element.copy(children = processedChildren))
                }
                is AsciiDocList -> {
                    val processedItems = element.items.map { item ->
                        processListItem(item, warnings, counts)
                    }
                    result.add(element.copy(items = processedItems))
                }
                is AdmonitionBlock -> {
                    // Already an admonition, just count it
                    counts[element.type] = (counts[element.type] ?: 0) + 1
                    
                    // Process nested content
                    val processedContent = processBlockElements(
                        element.content,
                        warnings,
                        counts
                    )
                    result.add(element.copy(content = processedContent))
                }
                is ConditionalDirective -> {
                    val processedContent = processBlockElements(
                        element.content,
                        warnings,
                        counts
                    )
                    val processedElseContent = processBlockElements(
                        element.elseContent,
                        warnings,
                        counts
                    )
                    result.add(element.copy(
                        content = processedContent,
                        elseContent = processedElseContent
                    ))
                }
                is Document -> {
                    result.add(processDocument(element, warnings, counts))
                }
                else -> {
                    result.add(element)
                }
            }
        }
        
        return result
    }
    
    private fun processListItem(
        item: ListItem,
        warnings: MutableList<ProcessingWarning>,
        counts: MutableMap<AdmonitionType, Int>
    ): ListItem {
        val processedNestedList = item.nestedList?.let { nestedList ->
            val processedItems = nestedList.items.map { nestedItem ->
                processListItem(nestedItem, warnings, counts)
            }
            nestedList.copy(items = processedItems)
        }
        return item.copy(nestedList = processedNestedList)
    }
    
    /**
     * Try to parse a paragraph as an admonition block.
     * Admonitions in AsciiDoc can be in the form:
     * - NOTE: content
     * - TIP: content
     * - WARNING: content
     * - CAUTION: content
     * - IMPORTANT: content
     * 
     * Or with a custom title:
     * - [NOTE]
     * - .Custom Title
     * - content
     */
    private fun tryParseAdmonition(
        paragraph: Paragraph,
        warnings: MutableList<ProcessingWarning>,
        counts: MutableMap<AdmonitionType, Int>
    ): AdmonitionBlock? {
        // Check if paragraph has admonition-related attributes
        val style = paragraph.attributes["style"]
        if (style != null) {
            val admonitionType = recognizeAdmonitionType(style)
            if (admonitionType != null) {
                // This is an admonition with block syntax
                counts[admonitionType] = (counts[admonitionType] ?: 0) + 1
                
                val title = paragraph.attributes["title"]
                
                return AdmonitionBlock(
                    type = admonitionType,
                    title = title,
                    content = listOf(paragraph.copy(attributes = paragraph.attributes - "style" - "title")),
                    attributes = paragraph.attributes.filterKeys { it != "style" && it != "title" },
                    sourceLocation = paragraph.sourceLocation
                )
            } else {
                // Invalid admonition type
                warnings.add(
                    ProcessingWarning(
                        message = "Invalid admonition type: $style",
                        location = paragraph.sourceLocation,
                        warningType = ProcessingWarningType.ADMONITION_INVALID_TYPE
                    )
                )
                return null
            }
        }
        
        // Check for inline admonition syntax (TYPE: content)
        if (paragraph.content.isNotEmpty()) {
            val firstElement = paragraph.content.first()
            if (firstElement is Text) {
                val text = firstElement.content
                val colonIndex = text.indexOf(':')
                if (colonIndex > 0) {
                    val potentialType = text.substring(0, colonIndex).trim()
                    val admonitionType = recognizeAdmonitionType(potentialType)
                    
                    if (admonitionType != null) {
                        counts[admonitionType] = (counts[admonitionType] ?: 0) + 1
                        
                        // Extract content after the colon
                        val contentAfterColon = text.substring(colonIndex + 1).trim()
                        val newContent = if (contentAfterColon.isNotEmpty()) {
                            listOf(Text(contentAfterColon, firstElement.attributes, firstElement.sourceLocation)) +
                                    paragraph.content.drop(1)
                        } else {
                            paragraph.content.drop(1)
                        }
                        
                        val contentParagraph = Paragraph(
                            content = newContent,
                            attributes = emptyMap(),
                            sourceLocation = paragraph.sourceLocation
                        )
                        
                        return AdmonitionBlock(
                            type = admonitionType,
                            title = null,
                            content = listOf(contentParagraph),
                            attributes = paragraph.attributes,
                            sourceLocation = paragraph.sourceLocation
                        )
                    }
                }
            }
        }
        
        return null
    }
    
    /**
     * Recognize an admonition type from a string.
     * Returns null if the string doesn't match any known admonition type.
     */
    private fun recognizeAdmonitionType(typeString: String): AdmonitionType? {
        return when (typeString.uppercase()) {
            "NOTE" -> AdmonitionType.NOTE
            "TIP" -> AdmonitionType.TIP
            "WARNING" -> AdmonitionType.WARNING
            "CAUTION" -> AdmonitionType.CAUTION
            "IMPORTANT" -> AdmonitionType.IMPORTANT
            else -> null
        }
    }
}
