package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.asg.AsgDocument

/**
 * Validates document structure and reports issues.
 * Checks for section hierarchy violations, duplicate anchors, invalid references, and structural consistency.
 */
interface DocumentValidator {
    /**
     * Validates the document structure and content.
     *
     * @param document The document to validate
     * @param config Configuration controlling validation behavior
     * @return Result containing validation errors and warnings
     */
    fun validate(document: AsgDocument, config: ValidationConfig): ValidationResult
}

/**
 * Configuration for document validation.
 */
data class ValidationConfig(
    val strictness: ValidationLevel = ValidationLevel.NORMAL,
    val checkSectionHierarchy: Boolean = true,
    val checkDuplicateAnchors: Boolean = true,
    val checkInvalidReferences: Boolean = true
)

/**
 * Result of document validation.
 */
data class ValidationResult(
    val errors: List<ProcessingError>,
    val warnings: List<ProcessingWarning>,
    val isValid: Boolean
) {
    companion object {
        /**
         * Creates a validation result indicating the document is valid.
         */
        fun valid(): ValidationResult = ValidationResult(
            errors = emptyList(),
            warnings = emptyList(),
            isValid = true
        )

        /**
         * Creates a validation result from errors and warnings.
         */
        fun fromIssues(
            errors: List<ProcessingError>,
            warnings: List<ProcessingWarning>
        ): ValidationResult = ValidationResult(
            errors = errors,
            warnings = warnings,
            isValid = errors.isEmpty()
        )
    }
}
