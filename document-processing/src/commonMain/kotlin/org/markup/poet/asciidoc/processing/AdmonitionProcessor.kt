package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.asg.AsgDocument

/**
 * Interface for processing admonition blocks.
 * Identifies admonition types, extracts content, handles custom titles,
 * and validates admonition structure.
 */
interface AdmonitionProcessor {
    /**
     * Process admonition blocks in the document.
     *
     * @param document The document to process
     * @return Result containing the processed document and any warnings
     */
    fun process(document: AsgDocument): AdmonitionResult
}

/**
 * Result of admonition processing.
 */
data class AdmonitionResult(
    /**
     * The processed document with admonitions identified and structured.
     */
    val document: AsgDocument,

    /**
     * Warnings encountered during processing (e.g., invalid admonition types).
     */
    val warnings: List<ProcessingWarning>,

    /**
     * Count of admonitions by ASG variant ("note", "tip", "warning", "caution", "important").
     */
    val admonitionCount: Map<String, Int>
)
