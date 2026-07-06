package org.markup.poet.cli

import org.markup.poet.asciidoc.error.ParseError
import org.markup.poet.asciidoc.error.ParseWarning
import org.markup.poet.asciidoc.processing.ProcessingError
import org.markup.poet.asciidoc.processing.ProcessingWarning

/**
 * Utility for formatting error and warning messages consistently across CLI commands.
 * 
 * This formatter ensures all error messages follow a consistent format with:
 * - Clear error descriptions
 * - File paths when applicable
 * - Line numbers when available
 * - Grouped multiple errors
 */
object ErrorFormatter {
    
    /**
     * Format a single error with optional file path and line number.
     * 
     * @param message The error message
     * @param filePath Optional file path where the error occurred
     * @param lineNumber Optional line number where the error occurred (0 or negative means no line number)
     * @param errorType Optional error type/category
     * @return Formatted error string
     */
    fun formatError(
        message: String,
        filePath: String? = null,
        lineNumber: Int = 0,
        errorType: String? = null
    ): String = buildString {
        appendLine("Error: $message")
        if (filePath != null) {
            appendLine("  File: $filePath")
        }
        if (lineNumber > 0) {
            appendLine("  Line: $lineNumber")
        }
        if (errorType != null) {
            appendLine("  Type: $errorType")
        }
    }.trimEnd()
    
    /**
     * Format multiple errors with a summary header.
     * 
     * @param errors List of error information (message, filePath, lineNumber, errorType)
     * @param operationDescription Description of the operation that failed (e.g., "Parsing", "Processing")
     * @return Formatted error string with all errors grouped together
     */
    fun formatMultipleErrors(
        errors: List<ErrorInfo>,
        operationDescription: String = "Operation"
    ): String = buildString {
        appendLine("$operationDescription failed with ${errors.size} error(s):")
        appendLine()
        errors.forEachIndexed { index, error ->
            if (index > 0) {
                appendLine()
            }
            append(formatError(error.message, error.filePath, error.lineNumber, error.errorType))
        }
    }.trimEnd()
    
    /**
     * Format parse errors from the parser.
     * 
     * @param errors List of parse errors
     * @param inputFile The input file being parsed
     * @return Formatted error string
     */
    fun formatParseErrors(errors: List<ParseError>, inputFile: String): String {
        val errorInfos = errors.map { error ->
            ErrorInfo(
                message = error.message,
                filePath = inputFile,
                lineNumber = error.location.line,
                errorType = "Parse Error"
            )
        }
        return formatMultipleErrors(errorInfos, "Parsing")
    }
    
    /**
     * Format processing errors from the document processor.
     * 
     * @param errors List of processing errors
     * @return Formatted error string
     */
    fun formatProcessingErrors(errors: List<ProcessingError>): String {
        val errorInfos = errors.map { error ->
            ErrorInfo(
                message = error.message,
                filePath = null, // Processing errors may not have file paths
                lineNumber = error.location?.start?.line ?: 0,
                errorType = error.errorType.toString()
            )
        }
        return formatMultipleErrors(errorInfos, "Processing")
    }
    
    /**
     * Format a warning message.
     * 
     * @param message The warning message
     * @param filePath Optional file path where the warning occurred
     * @param lineNumber Optional line number where the warning occurred (0 or negative means no line number)
     * @return Formatted warning string
     */
    fun formatWarning(
        message: String,
        filePath: String? = null,
        lineNumber: Int = 0
    ): String = buildString {
        append("Warning: $message")
        if (filePath != null) {
            append("\n  File: $filePath")
        }
        if (lineNumber > 0) {
            append("\n  Line: $lineNumber")
        }
    }
    
    /**
     * Format parse warnings from the parser.
     * 
     * @param warnings List of parse warnings
     * @param inputFile The input file being parsed
     * @return List of formatted warning strings
     */
    fun formatParseWarnings(warnings: List<ParseWarning>, inputFile: String): List<String> {
        return warnings.map { warning ->
            formatWarning(warning.message, inputFile, warning.location.line)
        }
    }
    
    /**
     * Format processing warnings from the document processor.
     * 
     * @param warnings List of processing warnings
     * @return List of formatted warning strings
     */
    fun formatProcessingWarnings(warnings: List<ProcessingWarning>): List<String> {
        return warnings.map { warning ->
            formatWarning(warning.message, null, warning.location?.start?.line ?: 0)
        }
    }
}

/**
 * Data class representing error information for formatting.
 */
data class ErrorInfo(
    val message: String,
    val filePath: String? = null,
    val lineNumber: Int = 0,
    val errorType: String? = null
)
