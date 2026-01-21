package org.markup.poet.tck.validation

/**
 * Result of a validation check.
 */
sealed class ValidationResult {
    data class Success(val message: String = "Validation passed") : ValidationResult()
    data class Failure(
        val message: String,
        val expected: String,
        val actual: String,
        val diff: String? = null
    ) : ValidationResult()
}
