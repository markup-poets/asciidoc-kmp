package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.ListBlock

/**
 * Generates a table of contents from document sections.
 */
interface TocGenerator {
    /**
     * Generates a table of contents for the given document.
     *
     * @param document The document to generate TOC for
     * @param config Configuration for TOC generation
     * @return Result containing the generated TOC and any errors
     */
    fun generate(document: AsgDocument, config: TocConfig): TocResult
}

/**
 * Configuration for table of contents generation.
 */
data class TocConfig(
    val maxDepth: Int = 3,
    val includeTitle: Boolean = true
)

/**
 * Result of table of contents generation.
 */
data class TocResult(
    val tocNode: ListBlock?,
    val errors: List<ProcessingError>
)
