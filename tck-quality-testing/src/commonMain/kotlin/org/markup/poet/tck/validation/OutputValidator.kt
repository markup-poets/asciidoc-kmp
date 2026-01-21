package org.markup.poet.tck.validation

/**
 * Validates test outputs against expected results.
 */
interface OutputValidator {
    /**
     * Compare actual output with expected output.
     */
    fun validate(expected: String, actual: String): ValidationResult
    
    /**
     * Compare actual output with expected output, ignoring whitespace differences.
     */
    fun validateIgnoringWhitespace(expected: String, actual: String): ValidationResult
}
