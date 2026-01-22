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
    CONFIGURATION_INVALID,
    CONDITIONAL_UNCLOSED,
    CONDITIONAL_MAX_DEPTH_EXCEEDED,
    CONDITIONAL_INVALID_EXPRESSION,
    FRAGMENT_TAG_MALFORMED,
    FRAGMENT_TAG_UNCLOSED,
    CALLOUT_INVALID_CONTEXT
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
    WHITESPACE_NORMALIZATION,
    FRAGMENT_TAG_NOT_FOUND,
    ADMONITION_INVALID_TYPE,
    FOOTNOTE_UNRESOLVED,
    BIBLIOGRAPHY_UNRESOLVED,
    CALLOUT_MISMATCH
}

/**
 * Severity levels for processing errors.
 */
enum class ErrorSeverity {
    WARNING,
    ERROR,
    FATAL
}
