package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.DiscreteHeading
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

    /** A section paired with the anchor id its rendered heading will carry. */
    private data class TocEntry(val section: SectionBlock, val anchorId: String)

    override fun generate(document: AsgDocument, config: TocConfig): TocResult {
        val errors = mutableListOf<ProcessingError>()

        // Collect all sections from the document with their anchor ids
        val entries = collectEntries(document)

        if (entries.isEmpty()) {
            return TocResult(null, errors)
        }

        // Build hierarchical TOC structure
        val tocItems = buildTocItems(entries, config)

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
     * Collects all sections in document order, pairing each with the anchor id
     * the HTML renderer will give its heading: the explicit metadata id when
     * present, otherwise an id derived from the title text. Derived ids mirror
     * the renderer's `RenderContext.generateId` exactly — same normalization,
     * same `-N` suffix on repeated titles, and discrete headings consume id
     * slots too — so TOC xref targets resolve against the rendered anchors.
     */
    private fun collectEntries(document: AsgDocument): List<TocEntry> {
        val idCounts = mutableMapOf<String, Int>()

        fun derivedId(title: String): String {
            val base = title
                .lowercase()
                .replace(Regex("[^a-z0-9]+"), "-")
                .trim('-')
                .ifEmpty { "section" }
            val count = idCounts[base] ?: 0
            idCounts[base] = count + 1
            return if (count == 0) base else "$base-$count"
        }

        val entries = mutableListOf<TocEntry>()
        visitBlocks(document.blocks) { block ->
            when (block) {
                is SectionBlock -> {
                    val anchorId = block.metadata?.id ?: derivedId(plainText(block.title))
                    entries.add(TocEntry(block, anchorId))
                }
                // The renderer generates ids for discrete headings from the same
                // counter; consume the slot so section suffixes stay aligned.
                is DiscreteHeading -> {
                    if (block.metadata?.id == null) derivedId(plainText(block.title))
                }
                else -> Unit
            }
        }
        return entries
    }

    /**
     * Builds TOC list items from sections, respecting depth limits and filtering.
     */
    private fun buildTocItems(
        entries: List<TocEntry>,
        config: TocConfig
    ): List<ListItem> {
        // Filter sections based on depth limit: a maxDepth of N keeps sections
        // down to ASG level N ('==' is level 1), matching asciidoctor's toclevels.
        val depthFiltered = entries.filter { it.section.level <= config.maxDepth }

        // Filter out untitled sections
        val filteredEntries = depthFiltered.filter { plainText(it.section.title).isNotBlank() }

        if (filteredEntries.isEmpty()) {
            return emptyList()
        }

        // Build hierarchical structure
        return buildHierarchicalToc(filteredEntries)
    }

    /**
     * Builds a hierarchical TOC structure from a flat list of sections.
     * Creates nested lists based on section levels.
     */
    private fun buildHierarchicalToc(entries: List<TocEntry>): List<ListItem> {
        if (entries.isEmpty()) return emptyList()

        val result = mutableListOf<ListItem>()
        var i = 0

        while (i < entries.size) {
            val entry = entries[i]
            val tocItem = createTocItem(entry)

            // Find all child sections (sections with higher level that come before next sibling)
            val childEntries = mutableListOf<TocEntry>()
            var j = i + 1
            while (j < entries.size && entries[j].section.level > entry.section.level) {
                childEntries.add(entries[j])
                j++
            }

            // If there are child sections, create a nested list
            if (childEntries.isNotEmpty()) {
                val nestedList = ListBlock(
                    variant = ListVariant.UNORDERED,
                    marker = "*",
                    items = buildHierarchicalToc(childEntries),
                    location = entry.section.location
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
    private fun createTocItem(entry: TocEntry): ListItem {
        val section = entry.section

        // Create cross-reference to the section, reusing the title inlines as link text
        val crossRef = InlineRef(
            variant = RefVariant.XREF,
            target = entry.anchorId,
            inlines = section.title,
            location = section.location
        )

        return ListItem(
            marker = "*",
            principal = listOf(crossRef),
            location = section.location
        )
    }
}
