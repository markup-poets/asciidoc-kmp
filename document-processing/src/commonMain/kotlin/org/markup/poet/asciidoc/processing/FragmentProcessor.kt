package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.asg.AsgDocument

/**
 * Interface for processing document fragments with tagged includes.
 * Handles extraction of tagged sections from included content.
 */
interface FragmentProcessor {
    /**
     * Process fragments in the document, extracting tagged sections.
     *
     * @param document The document containing fragment directives
     * @param config Configuration for fragment processing
     * @return FragmentResult containing the processed document and any errors/warnings
     */
    fun processFragments(document: AsgDocument, config: FragmentConfig): FragmentResult
}

/**
 * Configuration for fragment processing.
 */
data class FragmentConfig(
    val tagPrefix: String = "tag::",
    val tagSuffix: String = "[]",
    val allowNestedTags: Boolean = false
)

/**
 * Result of fragment processing.
 */
data class FragmentResult(
    val document: AsgDocument,
    val errors: List<ProcessingError>,
    val warnings: List<ProcessingWarning>,
    val extractedTags: Map<String, List<String>>
)
