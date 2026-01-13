package io.github.kotlin.asciidoc.error

import io.github.kotlin.asciidoc.ast.SourceLocation

/**
 * Represents a parsing error with location and severity information.
 */
data class ParseError(
    val message: String,
    val location: SourceLocation,
    val severity: ErrorSeverity = ErrorSeverity.ERROR
)

/**
 * Represents a parsing warning with location information.
 */
data class ParseWarning(
    val message: String,
    val location: SourceLocation
)

/**
 * Enumeration of error severity levels.
 */
enum class ErrorSeverity { 
    WARNING, 
    ERROR, 
    FATAL 
}