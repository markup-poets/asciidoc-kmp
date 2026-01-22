package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.ast.Document
import org.markup.poet.asciidoc.ast.InlineElement
import org.markup.poet.asciidoc.ast.SourceLocation

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
    fun process(document: Document): BibliographyResult
}

/**
 * Represents a footnote with its number and content.
 */
data class Footnote(
    val id: String,
    val number: Int,
    val content: List<InlineElement>,
    val sourceLocation: SourceLocation
)

/**
 * Result of bibliography and footnote processing.
 */
data class BibliographyResult(
    /**
     * The processed document with resolved footnote and bibliography references.
     */
    val document: Document,
    
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
    val sourceLocation: SourceLocation
)
