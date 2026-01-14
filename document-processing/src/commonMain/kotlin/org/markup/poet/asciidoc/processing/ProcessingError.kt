package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.ast.SourceLocation

/**
 * Represents an error that occurred during document processing.
 */
data class ProcessingError(
    val message: String,
    val location: SourceLocation,
    val errorType: ProcessingErrorType,
    val severity: ErrorSeverity = ErrorSeverity.ERROR
)

/**
 * Types of processing errors that can occur.
 */
enum class ProcessingErrorType {
    INCLUDE_NOT_FOUND,
    INCLUDE_CIRCULAR_DEPENDENCY,
    INCLUDE_MAX_DEPTH_EXCEEDED,
    ATTRIBUTE_CIRCULAR_REFERENCE,
    ATTRIBUTE_UNDEFINED,
    CROSS_REFERENCE_UNRESOLVED,
    CROSS_REFERENCE_DUPLICATE_ANCHOR,
    VALIDATION_SECTION_HIERARCHY,
    VALIDATION_DUPLICATE_ANCHOR,
    MACRO_EXPANSION_FAILED,
    MACRO_INVALID_PARAMETERS,
    CONFIGURATION_INVALID
}

/**
 * Represents a warning that occurred during document processing.
 */
data class ProcessingWarning(
    val message: String,
    val location: SourceLocation,
    val warningType: ProcessingWarningType
)

/**
 * Types of processing warnings that can occur.
 */
enum class ProcessingWarningType {
    ATTRIBUTE_UNDEFINED,
    CROSS_REFERENCE_UNRESOLVED,
    SECTION_HIERARCHY_VIOLATION,
    WHITESPACE_NORMALIZATION
}

/**
 * Severity levels for processing errors.
 */
enum class ErrorSeverity {
    WARNING,
    ERROR,
    FATAL
}
