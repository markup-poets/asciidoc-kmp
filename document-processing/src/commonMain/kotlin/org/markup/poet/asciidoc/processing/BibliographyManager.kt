package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.Inline
import org.markup.poet.asciidoc.asg.Location

/**
 * Interface for managing footnotes and bibliography entries.
 * Collects footnotes, assigns sequential numbers, indexes bibliography entries,
 * and resolves references.
 */
interface BibliographyManager {
    /**
     * Process footnotes and bibliography entries in the document.
     *
     * @param document The document to process
     * @return Result containing the processed document, footnotes, bibliography, and warnings
     */
    fun process(document: AsgDocument): BibliographyResult
}

/**
 * Represents a footnote with its number and content.
 */
data class Footnote(
    val id: String,
    val number: Int,
    val content: List<Inline>,
    val location: Location?
)

/**
 * Result of bibliography and footnote processing.
 */
data class BibliographyResult(
    /**
     * The processed document with resolved footnote and bibliography references.
     */
    val document: AsgDocument,

    /**
     * List of all footnotes in document order with assigned numbers.
     */
    val footnotes: List<Footnote>,

    /**
     * List of all bibliography entries indexed by ID.
     */
    val bibliography: Map<String, BibliographyEntryData>,

    /**
     * Warnings encountered during processing (e.g., unresolved references).
     */
    val warnings: List<ProcessingWarning>
)

/**
 * Data class representing a bibliography entry with its metadata.
 */
data class BibliographyEntryData(
    val id: String,
    val citation: String,
    val metadata: Map<String, String>,
    val location: Location?
)
