package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.ast.*

/**
 * Default implementation of CrossReferenceResolver.
 */
class DefaultCrossReferenceResolver : CrossReferenceResolver {
    
    override fun resolve(document: Document): CrossReferenceResult {
        val errors = mutableListOf<ProcessingError>()
        val warnings = mutableListOf<ProcessingWarning>()
        
        // Build anchor index
        val anchorIndex = buildAnchorIndex(document, errors)
        
        // Resolve cross-references
        val processedDocument = resolveCrossReferences(document, anchorIndex, warnings)
        
        return CrossReferenceResult(
            document = processedDocument,
            errors = errors,
            warnings = warnings,
            resolvedReferences = anchorIndex
        )
    }
    
    /**
     * Resolves all cross-references in the document by matching them to anchors.
     */
    private fun resolveCrossReferences(
        document: Document,
        anchorIndex: Map<String, AnchorTarget>,
        warnings: MutableList<ProcessingWarning>
    ): Document {
        val processedChildren = document.children.map { 
            resolveInBlock(it, anchorIndex, warnings) 
        }
        return document.copy(children = processedChildren)
    }
    
    /**
     * Resolves cross-references in a block element.
     */
    private fun resolveInBlock(
        block: BlockElement,
        anchorIndex: Map<String, AnchorTarget>,
        warnings: MutableList<ProcessingWarning>
    ): BlockElement {
        return when (block) {
            is Section -> {
                val processedChildren = block.children.map { 
                    resolveInBlock(it, anchorIndex, warnings) 
                }
                block.copy(children = processedChildren)
            }
            is Paragraph -> {
                val processedContent = block.content.map { 
                    resolveInInline(it, anchorIndex, warnings) 
                }
                block.copy(content = processedContent)
            }
            is AsciiDocList -> {
                val processedItems = block.items.map { item ->
                    val processedContent = item.content.map { 
                        resolveInInline(it, anchorIndex, warnings) 
                    }
                    val processedNestedList = item.nestedList?.let {
                        resolveInBlock(it, anchorIndex, warnings) as AsciiDocList
                    }
                    item.copy(
                        content = processedContent,
                        nestedList = processedNestedList
                    )
                }
                block.copy(items = processedItems)
            }
            is CalloutList -> {
                val processedItems = block.items.map { item ->
                    val processedContent = item.content.map { 
                        resolveInInline(it, anchorIndex, warnings) 
                    }
                    item.copy(content = processedContent)
                }
                block.copy(items = processedItems)
            }
            else -> block // Other block types don't contain inline elements
        }
    }
    
    /**
     * Resolves cross-references in an inline element.
     */
    private fun resolveInInline(
        inline: InlineElement,
        anchorIndex: Map<String, AnchorTarget>,
        warnings: MutableList<ProcessingWarning>
    ): InlineElement {
        return when (inline) {
            is CrossReference -> {
                // Check if target exists in anchor index
                val target = anchorIndex[inline.targetId]
                if (target == null) {
                    // Report warning for unresolved reference
                    warnings.add(
                        ProcessingWarning(
                            message = "Unresolved cross-reference to '${inline.targetId}'",
                            location = inline.sourceLocation,
                            warningType = ProcessingWarningType.CROSS_REFERENCE_UNRESOLVED
                        )
                    )
                }
                // Return the cross-reference as-is (resolution is tracked in the index)
                inline
            }
            is Strong -> {
                val processedContent = inline.content.map { 
                    resolveInInline(it, anchorIndex, warnings) 
                }
                inline.copy(content = processedContent)
            }
            is Emphasis -> {
                val processedContent = inline.content.map { 
                    resolveInInline(it, anchorIndex, warnings) 
                }
                inline.copy(content = processedContent)
            }
            else -> inline // Other inline types don't contain nested elements
        }
    }
    
    /**
     * Builds an index of all anchors in the document.
     * Detects duplicate anchors and reports them as errors.
     */
    private fun buildAnchorIndex(
        document: Document,
        errors: MutableList<ProcessingError>
    ): Map<String, AnchorTarget> {
        val anchorIndex = mutableMapOf<String, AnchorTarget>()
        val duplicateAnchors = mutableMapOf<String, MutableList<AstNode>>()
        
        // Traverse document to collect anchors
        traverseForAnchors(document.children, anchorIndex, duplicateAnchors)
        
        // Report duplicate anchors as errors
        for ((anchorId, nodes) in duplicateAnchors) {
            if (nodes.size > 1) {
                val locations = nodes.joinToString(", ") { 
                    "line ${it.sourceLocation.line}" 
                }
                errors.add(
                    ProcessingError(
                        message = "Duplicate anchor ID '$anchorId' found at: $locations",
                        location = nodes.first().sourceLocation,
                        errorType = ProcessingErrorType.CROSS_REFERENCE_DUPLICATE_ANCHOR,
                        severity = ErrorSeverity.ERROR
                    )
                )
            }
        }
        
        return anchorIndex
    }
    
    /**
     * Recursively traverses the document tree to collect all anchors.
     */
    private fun traverseForAnchors(
        elements: List<BlockElement>,
        anchorIndex: MutableMap<String, AnchorTarget>,
        duplicateAnchors: MutableMap<String, MutableList<AstNode>>
    ) {
        for (element in elements) {
            // Check if element has an anchor ID attribute
            val anchorId = element.attributes["id"]
            if (anchorId != null) {
                // Track for duplicate detection
                duplicateAnchors.getOrPut(anchorId) { mutableListOf() }.add(element)
                
                // Add to index (first occurrence wins)
                if (!anchorIndex.containsKey(anchorId)) {
                    val generatedText = generateLinkText(element)
                    anchorIndex[anchorId] = AnchorTarget(
                        anchorId = anchorId,
                        targetNode = element,
                        generatedText = generatedText
                    )
                }
            }
            
            // Recursively traverse children
            when (element) {
                is Section -> traverseForAnchors(element.children, anchorIndex, duplicateAnchors)
                is AsciiDocList -> {
                    for (item in element.items) {
                        // Check list items for anchors
                        val itemAnchorId = item.attributes["id"]
                        if (itemAnchorId != null) {
                            duplicateAnchors.getOrPut(itemAnchorId) { mutableListOf() }.add(item)
                            if (!anchorIndex.containsKey(itemAnchorId)) {
                                val generatedText = generateLinkText(item)
                                anchorIndex[itemAnchorId] = AnchorTarget(
                                    anchorId = itemAnchorId,
                                    targetNode = item,
                                    generatedText = generatedText
                                )
                            }
                        }
                        // Traverse nested lists
                        if (item.nestedList != null) {
                            traverseForAnchors(listOf(item.nestedList as BlockElement), anchorIndex, duplicateAnchors)
                        }
                    }
                }
                is CalloutList -> {
                    for (item in element.items) {
                        val itemAnchorId = item.attributes["id"]
                        if (itemAnchorId != null) {
                            duplicateAnchors.getOrPut(itemAnchorId) { mutableListOf() }.add(item)
                            if (!anchorIndex.containsKey(itemAnchorId)) {
                                val generatedText = generateLinkText(item)
                                anchorIndex[itemAnchorId] = AnchorTarget(
                                    anchorId = itemAnchorId,
                                    targetNode = item,
                                    generatedText = generatedText
                                )
                            }
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
     * Generates appropriate link text based on the target element type.
     * Preserves custom link text when provided.
     */
    private fun generateLinkText(node: AstNode): String {
        return when (node) {
            is Section -> node.title
            is ListItem -> {
                // Extract text from inline elements
                val text = node.content.joinToString("") { extractText(it) }.trim()
                if (text.isEmpty()) "[Untitled List Item]" else text
            }
            is CalloutListItem -> "Callout ${node.number}"
            is Paragraph -> {
                // Extract text from inline elements
                val text = node.content.joinToString("") { extractText(it) }.trim()
                if (text.isEmpty()) "[Untitled Paragraph]" else text
            }
            is CodeBlock -> {
                val lang = node.language ?: "code"
                "[$lang block]"
            }
            is Comment -> "[Comment]"
            is IncludeDirective -> "[Include: ${node.path}]"
            else -> "[Untitled]"
        }
    }
    
    /**
     * Extracts plain text from inline elements.
     */
    private fun extractText(inline: InlineElement): String {
        return when (inline) {
            is Text -> inline.content
            is Strong -> inline.content.joinToString("") { extractText(it) }
            is Emphasis -> inline.content.joinToString("") { extractText(it) }
            is Code -> inline.content
            is Link -> inline.text
            is Image -> inline.altText
            is AttributeReference -> "{${inline.key}}"
            is Callout -> "<${inline.number}>"
            is CrossReference -> inline.customText ?: inline.targetId
            is MacroInvocation -> inline.macroName
            is FootnoteReference -> inline.content.joinToString("") { extractText(it) }
            is BibliographyReference -> inline.citationId
        }
    }
    
    /**
     * Gets the effective link text for a cross-reference.
     * Uses custom text if provided, otherwise generates text from target.
     */
    private fun getEffectiveLinkText(
        crossRef: CrossReference,
        anchorIndex: Map<String, AnchorTarget>
    ): String {
        // If custom text is provided, use it
        val customText = crossRef.customText
        if (customText != null) {
            return customText
        }
        
        // Otherwise, use generated text from target
        val target = anchorIndex[crossRef.targetId]
        return target?.generatedText ?: crossRef.targetId
    }
}
