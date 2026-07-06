package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.asg.AsgDocument

/**
 * Interface for attribute substitution in AsciiDoc documents.
 * Performs attribute reference substitution throughout the document.
 */
interface AttributeSubstitutor {
    /**
     * Substitute attribute references in the document.
     *
     * @param document The document to process
     * @param config Configuration controlling substitution behavior
     * @return SubstitutionResult containing the processed document and any errors
     */
    fun substitute(document: AsgDocument, config: AttributeConfig): SubstitutionResult
}

/**
 * Configuration for attribute substitution.
 */
data class AttributeConfig(
    val defaults: Map<String, String> = emptyMap(),
    val maxRecursionDepth: Int = 10,
    val undefinedBehavior: UndefinedAttributeBehavior = UndefinedAttributeBehavior.PRESERVE
)

/**
 * Behavior when encountering undefined attributes.
 */
enum class UndefinedAttributeBehavior {
    PRESERVE,  // Keep {attr} as-is
    REMOVE,    // Remove {attr} entirely
    DEFAULT    // Use default value if provided
}

/**
 * Result of attribute substitution.
 */
data class SubstitutionResult(
    val document: AsgDocument,
    val errors: List<ProcessingError>,
    val substitutedAttributes: Set<String>
)
