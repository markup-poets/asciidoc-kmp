package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.Block
import org.markup.poet.asciidoc.asg.InlineRef
import org.markup.poet.asciidoc.asg.ListBlock
import org.markup.poet.asciidoc.asg.ListItem
import org.markup.poet.asciidoc.asg.ListVariant
import org.markup.poet.asciidoc.asg.RefVariant
import org.markup.poet.asciidoc.asg.SectionBlock
import org.markup.poet.asciidoc.asg.plainText
import org.markup.poet.asciidoc.asg.visitBlocks

/**
 * Default implementation of TocGenerator that traverses document sections
 * and builds a hierarchical table of contents.
 */
class DefaultTocGenerator : TocGenerator {

    override fun generate(document: AsgDocument, config: TocConfig): TocResult {
        val errors = mutableListOf<ProcessingError>()

        // Collect all sections from the document
        val sections = collectSections(document)

        if (sections.isEmpty()) {
            return TocResult(null, errors)
        }

        // Build hierarchical TOC structure
        val tocItems = buildTocItems(sections, config)

        if (tocItems.isEmpty()) {
            return TocResult(null, errors)
        }

        // Create the TOC list
        val tocList = ListBlock(
            variant = ListVariant.UNORDERED,
            marker = "*",
            items = tocItems
        )

        return TocResult(tocList, errors)
    }

    /**
     * Collects all sections from the document in document order.
     */
    private fun collectSections(document: AsgDocument): List<SectionBlock> {
        val sections = mutableListOf<SectionBlock>()
        visitBlocks(document.blocks) { block ->
            if (block is SectionBlock) {
                sections.add(block)
            }
        }
        return sections
    }

    /**
     * Builds TOC list items from sections, respecting depth limits and filtering.
     */
    private fun buildTocItems(
        sections: List<SectionBlock>,
        config: TocConfig
    ): List<ListItem> {
        // Filter sections based on depth limit: a maxDepth of N keeps sections
        // down to ASG level N ('==' is level 1), matching asciidoctor's toclevels.
        val depthFiltered = sections.filter { it.level <= config.maxDepth }

        // Filter out untitled sections
        val filteredSections = depthFiltered.filter { plainText(it.title).isNotBlank() }

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
    private fun buildHierarchicalToc(sections: List<SectionBlock>): List<ListItem> {
        if (sections.isEmpty()) return emptyList()

        val result = mutableListOf<ListItem>()
        var i = 0

        while (i < sections.size) {
            val section = sections[i]
            val tocItem = createTocItem(section)

            // Find all child sections (sections with higher level that come before next sibling)
            val childSections = mutableListOf<SectionBlock>()
            var j = i + 1
            while (j < sections.size && sections[j].level > section.level) {
                childSections.add(sections[j])
                j++
            }

            // If there are child sections, create a nested list
            if (childSections.isNotEmpty()) {
                val nestedList = ListBlock(
                    variant = ListVariant.UNORDERED,
                    marker = "*",
                    items = buildHierarchicalToc(childSections),
                    location = section.location
                )
                result.add(tocItem.copy(blocks = listOf(nestedList)))
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
    private fun createTocItem(section: SectionBlock): ListItem {
        // Get or generate anchor ID for the section
        val anchorId = section.metadata?.id ?: generateAnchorId(section)

        // Create cross-reference to the section, reusing the title inlines as link text
        val crossRef = InlineRef(
            variant = RefVariant.XREF,
            target = anchorId,
            inlines = section.title,
            location = section.location
        )

        return ListItem(
            marker = "*",
            principal = listOf(crossRef),
            location = section.location
        )
    }

    /**
     * Generates an anchor ID from a section title.
     * Converts title to lowercase, replaces spaces with hyphens, removes special characters.
     */
    private fun generateAnchorId(section: SectionBlock): String {
        return plainText(section.title)
            .lowercase()
            .replace(Regex("[^a-z0-9\\s-]"), "")
            .replace(Regex("\\s+"), "-")
            .trim('-')
    }
}
