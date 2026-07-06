package org.markup.poet.asciidoc.error

/**
 * Represents a parsing error with source position and severity information.
 * Line and column are 1-based, matching the ASG location convention.
 */
data class ParseError(
    val message: String,
    val line: Int,
    val column: Int = 1,
    val severity: ErrorSeverity = ErrorSeverity.ERROR
)

/**
 * Represents a parsing warning with source position information.
 * Line and column are 1-based, matching the ASG location convention.
 */
data class ParseWarning(
    val message: String,
    val line: Int,
    val column: Int = 1
)

/**
 * Enumeration of error severity levels.
 */
enum class ErrorSeverity {
    WARNING,
    ERROR,
    FATAL
}
