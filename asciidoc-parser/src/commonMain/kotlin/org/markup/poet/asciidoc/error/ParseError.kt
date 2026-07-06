package org.markup.poet.asciidoc.error

import org.markup.poet.asciidoc.ast.SourceLocation

/**
 * Represents a parsing error with location and severity information.
 */
data class ParseError(
    val message: String,
    val location: SourceLocation,
    val severity: ErrorSeverity = ErrorSeverity.ERROR
) {
    /** Convenience constructor so ASG-only consumers need no legacy AST import. */
    constructor(message: String, line: Int, column: Int, severity: ErrorSeverity = ErrorSeverity.ERROR) :
        this(message, SourceLocation(line, column), severity)
}

/**
 * Represents a parsing warning with location information.
 */
data class ParseWarning(
    val message: String,
    val location: SourceLocation
) {
    /** Convenience constructor so ASG-only consumers need no legacy AST import. */
    constructor(message: String, line: Int, column: Int) : this(message, SourceLocation(line, column))
}

/**
 * Enumeration of error severity levels.
 */
enum class ErrorSeverity { 
    WARNING, 
    ERROR, 
    FATAL 
}