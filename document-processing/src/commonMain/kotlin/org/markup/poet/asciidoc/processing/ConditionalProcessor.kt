package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.asg.AsgDocument

/**
 * Interface for processing conditional content directives (ifdef, ifndef, ifeval).
 * Evaluates conditional directives and includes/excludes content based on attribute presence
 * and expression evaluation.
 */
interface ConditionalProcessor {
    /**
     * Process conditional directives in the document.
     *
     * @param document The document to process
     * @param config Configuration for conditional processing
     * @return Result containing the processed document and any errors/warnings
     */
    fun process(document: AsgDocument, config: ConditionalConfig): ConditionalResult
}

/**
 * Configuration for conditional content processing.
 */
data class ConditionalConfig(
    /**
     * Set of attributes that are defined for conditional evaluation.
     */
    val definedAttributes: Set<String>,

    /**
     * Whether to allow nested conditional directives.
     */
    val allowNestedConditionals: Boolean = true,

    /**
     * Maximum nesting depth for conditional directives.
     */
    val maxNestingDepth: Int = 10
)

/**
 * Result of conditional content processing.
 */
data class ConditionalResult(
    /**
     * The processed document with conditional content evaluated.
     */
    val document: AsgDocument,

    /**
     * Errors encountered during processing.
     */
    val errors: List<ProcessingError>,

    /**
     * Warnings encountered during processing.
     */
    val warnings: List<ProcessingWarning>,

    /**
     * Number of conditional directives that were evaluated.
     */
    val evaluatedConditionals: Int
)
