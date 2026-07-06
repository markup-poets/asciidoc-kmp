package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.BibliographyEntryBlock
import org.markup.poet.asciidoc.asg.InlineCitation
import org.markup.poet.asciidoc.asg.InlineFootnote
import org.markup.poet.asciidoc.asg.inlineListsOf
import org.markup.poet.asciidoc.asg.visitBlocks
import org.markup.poet.asciidoc.asg.visitInlines

/**
 * Default implementation of BibliographyManager.
 * Collects footnotes, assigns sequential numbers, indexes bibliography entries,
 * and resolves references.
 */
class DefaultBibliographyManager : BibliographyManager {

    override fun process(document: AsgDocument): BibliographyResult {
        val warnings = mutableListOf<ProcessingWarning>()
        val footnoteMap = mutableMapOf<String, Footnote>()
        val bibliographyMap = mutableMapOf<String, BibliographyEntryData>()
        val footnoteOrder = mutableListOf<String>()

        // First pass: collect all footnotes and bibliography entries
        collectFootnotesAndBibliography(document, footnoteMap, bibliographyMap, footnoteOrder)

        // Assign sequential numbers to footnotes based on first occurrence
        val numberedFootnotes = assignFootnoteNumbers(footnoteMap, footnoteOrder)

        // Second pass: validate all references
        validateReferences(document, numberedFootnotes, bibliographyMap, warnings)

        return BibliographyResult(
            document = document,
            footnotes = numberedFootnotes.values.sortedBy { it.number },
            bibliography = bibliographyMap,
            warnings = warnings
        )
    }

    /**
     * Collect all footnotes and bibliography entries from the document in document order.
     */
    private fun collectFootnotesAndBibliography(
        document: AsgDocument,
        footnoteMap: MutableMap<String, Footnote>,
        bibliographyMap: MutableMap<String, BibliographyEntryData>,
        footnoteOrder: MutableList<String>
    ) {
        visitBlocks(document.blocks) { block ->
            if (block is BibliographyEntryBlock) {
                bibliographyMap[block.id] = BibliographyEntryData(
                    id = block.id,
                    citation = block.citation,
                    metadata = block.entryMetadata,
                    location = block.location
                )
            }
            inlineListsOf(block).forEach { inlines ->
                visitInlines(inlines) { inline ->
                    if (inline is InlineFootnote && !footnoteMap.containsKey(inline.id)) {
                        // Track first occurrence order; number is assigned later
                        footnoteOrder.add(inline.id)
                        footnoteMap[inline.id] = Footnote(
                            id = inline.id,
                            number = 0, // Placeholder
                            content = inline.inlines,
                            location = inline.location
                        )
                    }
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
        document: AsgDocument,
        footnoteMap: Map<String, Footnote>,
        bibliographyMap: Map<String, BibliographyEntryData>,
        warnings: MutableList<ProcessingWarning>
    ) {
        visitBlocks(document.blocks) { block ->
            inlineListsOf(block).forEach { inlines ->
                visitInlines(inlines) { inline ->
                    when (inline) {
                        is InlineFootnote -> if (!footnoteMap.containsKey(inline.id)) {
                            warnings.add(
                                ProcessingWarning(
                                    message = "Unresolved footnote reference: ${inline.id}",
                                    location = inline.location,
                                    warningType = ProcessingWarningType.FOOTNOTE_UNRESOLVED
                                )
                            )
                        }
                        is InlineCitation -> if (!bibliographyMap.containsKey(inline.citationId)) {
                            warnings.add(
                                ProcessingWarning(
                                    message = "Unresolved bibliography reference: ${inline.citationId}",
                                    location = inline.location,
                                    warningType = ProcessingWarningType.BIBLIOGRAPHY_UNRESOLVED
                                )
                            )
                        }
                        else -> Unit
                    }
                }
            }
        }
    }
}
