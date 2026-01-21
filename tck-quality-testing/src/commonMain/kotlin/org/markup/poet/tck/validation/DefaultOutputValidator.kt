package org.markup.poet.tck.validation

/**
 * Default implementation of OutputValidator with diff generation.
 */
class DefaultOutputValidator : OutputValidator {
    override fun validate(expected: String, actual: String): ValidationResult {
        return if (expected == actual) {
            ValidationResult.Success()
        } else {
            ValidationResult.Failure(
                message = "Output mismatch",
                expected = expected,
                actual = actual,
                diff = generateDiff(expected, actual)
            )
        }
    }
    
    override fun validateIgnoringWhitespace(expected: String, actual: String): ValidationResult {
        val normalizedExpected = normalizeWhitespace(expected)
        val normalizedActual = normalizeWhitespace(actual)
        return validate(normalizedExpected, normalizedActual)
    }
    
    private fun normalizeWhitespace(text: String): String {
        return text.trim().replace(Regex("\\s+"), " ")
    }
    
    private fun generateDiff(expected: String, actual: String): String {
        val expectedLines = expected.lines()
        val actualLines = actual.lines()
        val diff = StringBuilder()
        
        val maxLines = maxOf(expectedLines.size, actualLines.size)
        
        for (i in 0 until maxLines) {
            val expectedLine = expectedLines.getOrNull(i)
            val actualLine = actualLines.getOrNull(i)
            
            when {
                expectedLine == actualLine -> {
                    diff.append("  ${expectedLine}\n")
                }
                expectedLine != null && actualLine == null -> {
                    diff.append("- ${expectedLine}\n")
                }
                expectedLine == null && actualLine != null -> {
                    diff.append("+ ${actualLine}\n")
                }
                else -> {
                    diff.append("- ${expectedLine}\n")
                    diff.append("+ ${actualLine}\n")
                }
            }
        }
        
        return diff.toString()
    }
}
