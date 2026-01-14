package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.ast.*

/**
 * Default implementation of TocGenerator that traverses document sections
 * and builds a hierarchical table of contents.
 */
class DefaultTocGenerator : TocGenerator {
    
    override fun generate(document: Document, config: TocConfig): TocResult {
        val errors = mutableListOf<ProcessingError>()
        
        // Collect all sections from the document
        val sections = collectSections(document)
        
        if (sections.isEmpty()) {
            return TocResult(null, errors)
        }
        
        // Build hierarchical TOC structure
        val tocItems = buildTocItems(sections, config, errors)
        
        if (tocItems.isEmpty()) {
            return TocResult(null, errors)
        }
        
        // Create the TOC list
        val tocList = AsciiDocList(
            type = ListType.UNORDERED,
            items = tocItems,
            sourceLocation = SourceLocation(0, 0)
        )
        
        return TocResult(tocList, errors)
    }
    
    /**
     * Collects all sections from the document in document order.
     */
    private fun collectSections(document: Document): List<Section> {
        val sections = mutableListOf<Section>()
        
        fun traverse(elements: List<BlockElement>) {
            for (element in elements) {
                when (element) {
                    is Section -> {
                        sections.add(element)
                        traverse(element.children)
                    }
                    is Document -> traverse(element.children)
                    else -> {} // Ignore non-section elements
                }
            }
        }
        
        traverse(document.children)
        return sections
    }
    
    /**
     * Builds TOC list items from sections, respecting depth limits and filtering.
     */
    private fun buildTocItems(
        sections: List<Section>,
        config: TocConfig,
        errors: MutableList<ProcessingError>
    ): List<ListItem> {
        // Filter sections based on depth limit
        val depthFiltered = filterByDepth(sections, config.maxDepth)
        
        // Filter out untitled sections
        val filteredSections = filterUntitled(depthFiltered)
        
        if (filteredSections.isEmpty()) {
            return emptyList()
        }
        
        // Build hierarchical structure
        return buildHierarchicalToc(filteredSections)
    }
    
    /**
     * Builds a hierarchical TOC structure from a flat list of sections.
     * Creates nested lists based on section levels.
     */
    private fun buildHierarchicalToc(sections: List<Section>): List<ListItem> {
        if (sections.isEmpty()) return emptyList()
        
        val result = mutableListOf<ListItem>()
        var i = 0
        
        while (i < sections.size) {
            val section = sections[i]
            val tocItem = createTocItem(section)
            
            // Find all child sections (sections with higher level that come before next sibling)
            val childSections = mutableListOf<Section>()
            var j = i + 1
            while (j < sections.size && sections[j].level > section.level) {
                childSections.add(sections[j])
                j++
            }
            
            // If there are child sections, create a nested list
            if (childSections.isNotEmpty()) {
                val nestedItems = buildHierarchicalToc(childSections)
                val nestedList = AsciiDocList(
                    type = ListType.UNORDERED,
                    items = nestedItems,
                    sourceLocation = section.sourceLocation
                )
                result.add(tocItem.copy(nestedList = nestedList))
                i = j
            } else {
                result.add(tocItem)
                i++
            }
        }
        
        return result
    }
    
    /**
     * Creates a TOC list item for a section with a cross-reference.
     */
    private fun createTocItem(section: Section): ListItem {
        // Get or generate anchor ID for the section
        val anchorId = section.attributes["id"] ?: generateAnchorId(section)
        
        // Create cross-reference to the section
        val crossRef = CrossReference(
            targetId = anchorId,
            customText = section.title,
            sourceLocation = section.sourceLocation
        )
        
        return ListItem(
            marker = "*",
            content = listOf(crossRef),
            nestedList = null,
            sourceLocation = section.sourceLocation
        )
    }
    
    /**
     * Generates an anchor ID from a section title.
     * Converts title to lowercase, replaces spaces with hyphens, removes special characters.
     */
    private fun generateAnchorId(section: Section): String {
        return section.title
            .lowercase()
            .replace(Regex("[^a-z0-9\\s-]"), "")
            .replace(Regex("\\s+"), "-")
            .trim('-')
    }
    
    /**
     * Filters sections to only include those within the specified depth limit.
     * Depth is determined by the section level (1 = top level, 2 = second level, etc.)
     */
    private fun filterByDepth(sections: List<Section>, maxDepth: Int): List<Section> {
        return sections.filter { section ->
            section.level <= maxDepth
        }
    }
    
    /**
     * Filters out sections that have no title (empty or blank title).
     */
    private fun filterUntitled(sections: List<Section>): List<Section> {
        return sections.filter { section ->
            section.title.isNotBlank()
        }
    }
}
