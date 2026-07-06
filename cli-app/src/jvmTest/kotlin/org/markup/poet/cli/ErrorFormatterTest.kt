package org.markup.poet.cli

import org.markup.poet.asciidoc.error.ParseError
import org.markup.poet.asciidoc.error.ParseWarning
import org.markup.poet.asciidoc.asg.Location
import org.markup.poet.asciidoc.asg.Position
import org.markup.poet.asciidoc.ast.SourceLocation
import org.markup.poet.asciidoc.processing.ProcessingError
import org.markup.poet.asciidoc.processing.ProcessingWarning
import org.markup.poet.asciidoc.processing.ProcessingErrorType
import org.markup.poet.asciidoc.processing.ProcessingWarningType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for ErrorFormatter.
 * 
 * Tests consistent error and warning formatting across CLI commands.
 */
class ErrorFormatterTest {

    private fun loc(line: Int) = Location(Position(line, 1), Position(line, 1))

    @Test
    fun `should format error with message only`() {
        val result = ErrorFormatter.formatError("Something went wrong")
        
        assertEquals("Error: Something went wrong", result)
    }
    
    @Test
    fun `should format error with file path`() {
        val result = ErrorFormatter.formatError(
            message = "File not found",
            filePath = "/path/to/file.adoc"
        )
        
        assertTrue(result.contains("Error: File not found"))
        assertTrue(result.contains("File: /path/to/file.adoc"))
    }
    
    @Test
    fun `should format error with line number`() {
        val result = ErrorFormatter.formatError(
            message = "Invalid syntax",
            lineNumber = 42
        )
        
        assertTrue(result.contains("Error: Invalid syntax"))
        assertTrue(result.contains("Line: 42"))
    }
    
    @Test
    fun `should format error with file path and line number`() {
        val result = ErrorFormatter.formatError(
            message = "Parse error",
            filePath = "document.adoc",
            lineNumber = 15
        )
        
        assertTrue(result.contains("Error: Parse error"))
        assertTrue(result.contains("File: document.adoc"))
        assertTrue(result.contains("Line: 15"))
    }
    
    @Test
    fun `should format error with all fields`() {
        val result = ErrorFormatter.formatError(
            message = "Include not found",
            filePath = "main.adoc",
            lineNumber = 10,
            errorType = "Include Error"
        )
        
        assertTrue(result.contains("Error: Include not found"))
        assertTrue(result.contains("File: main.adoc"))
        assertTrue(result.contains("Line: 10"))
        assertTrue(result.contains("Type: Include Error"))
    }
    
    @Test
    fun `should not include line number when zero`() {
        val result = ErrorFormatter.formatError(
            message = "General error",
            filePath = "file.adoc",
            lineNumber = 0
        )
        
        assertTrue(result.contains("Error: General error"))
        assertTrue(result.contains("File: file.adoc"))
        assertTrue(!result.contains("Line:"))
    }
    
    @Test
    fun `should not include line number when negative`() {
        val result = ErrorFormatter.formatError(
            message = "General error",
            filePath = "file.adoc",
            lineNumber = -1
        )
        
        assertTrue(result.contains("Error: General error"))
        assertTrue(result.contains("File: file.adoc"))
        assertTrue(!result.contains("Line:"))
    }
    
    @Test
    fun `should format multiple errors with summary`() {
        val errors = listOf(
            ErrorInfo("First error", "file1.adoc", 10, "Type1"),
            ErrorInfo("Second error", "file2.adoc", 20, "Type2"),
            ErrorInfo("Third error", "file3.adoc", 30, "Type3")
        )
        
        val result = ErrorFormatter.formatMultipleErrors(errors, "Processing")
        
        assertTrue(result.contains("Processing failed with 3 error(s):"))
        assertTrue(result.contains("Error: First error"))
        assertTrue(result.contains("File: file1.adoc"))
        assertTrue(result.contains("Line: 10"))
        assertTrue(result.contains("Error: Second error"))
        assertTrue(result.contains("File: file2.adoc"))
        assertTrue(result.contains("Line: 20"))
        assertTrue(result.contains("Error: Third error"))
        assertTrue(result.contains("File: file3.adoc"))
        assertTrue(result.contains("Line: 30"))
    }
    
    @Test
    fun `should format single error in multiple errors format`() {
        val errors = listOf(
            ErrorInfo("Only error", "file.adoc", 5)
        )
        
        val result = ErrorFormatter.formatMultipleErrors(errors, "Validation")
        
        assertTrue(result.contains("Validation failed with 1 error(s):"))
        assertTrue(result.contains("Error: Only error"))
        assertTrue(result.contains("File: file.adoc"))
        assertTrue(result.contains("Line: 5"))
    }
    
    @Test
    fun `should format parse errors with file path`() {
        val parseErrors = listOf(
            ParseError("Unexpected token", SourceLocation(10, 5)),
            ParseError("Missing closing bracket", SourceLocation(15, 10))
        )
        
        val result = ErrorFormatter.formatParseErrors(parseErrors, "document.adoc")
        
        assertTrue(result.contains("Parsing failed with 2 error(s):"))
        assertTrue(result.contains("Error: Unexpected token"))
        assertTrue(result.contains("File: document.adoc"))
        assertTrue(result.contains("Line: 10"))
        assertTrue(result.contains("Error: Missing closing bracket"))
        assertTrue(result.contains("Line: 15"))
    }
    
    @Test
    fun `should format processing errors`() {
        val processingErrors = listOf(
            ProcessingError("Include not found", loc(5), ProcessingErrorType.INCLUDE_NOT_FOUND),
            ProcessingError("Max depth exceeded", loc(10), ProcessingErrorType.INCLUDE_MAX_DEPTH_EXCEEDED)
        )
        
        val result = ErrorFormatter.formatProcessingErrors(processingErrors)
        
        assertTrue(result.contains("Processing failed with 2 error(s):"))
        assertTrue(result.contains("Error: Include not found"))
        assertTrue(result.contains("Line: 5"))
        assertTrue(result.contains("Type: INCLUDE_NOT_FOUND"))
        assertTrue(result.contains("Error: Max depth exceeded"))
        assertTrue(result.contains("Line: 10"))
    }
    
    @Test
    fun `should format warning with message only`() {
        val result = ErrorFormatter.formatWarning("This is a warning")
        
        assertEquals("Warning: This is a warning", result)
    }
    
    @Test
    fun `should format warning with file path`() {
        val result = ErrorFormatter.formatWarning(
            message = "Deprecated syntax",
            filePath = "old-doc.adoc"
        )
        
        assertTrue(result.contains("Warning: Deprecated syntax"))
        assertTrue(result.contains("File: old-doc.adoc"))
    }
    
    @Test
    fun `should format warning with line number`() {
        val result = ErrorFormatter.formatWarning(
            message = "Unused attribute",
            lineNumber = 25
        )
        
        assertTrue(result.contains("Warning: Unused attribute"))
        assertTrue(result.contains("Line: 25"))
    }
    
    @Test
    fun `should format warning with all fields`() {
        val result = ErrorFormatter.formatWarning(
            message = "Potential issue",
            filePath = "test.adoc",
            lineNumber = 12
        )
        
        assertTrue(result.contains("Warning: Potential issue"))
        assertTrue(result.contains("File: test.adoc"))
        assertTrue(result.contains("Line: 12"))
    }
    
    @Test
    fun `should format parse warnings`() {
        val parseWarnings = listOf(
            ParseWarning("Deprecated attribute", SourceLocation(5, 0)),
            ParseWarning("Unused macro", SourceLocation(10, 0))
        )
        
        val result = ErrorFormatter.formatParseWarnings(parseWarnings, "document.adoc")
        
        assertEquals(2, result.size)
        assertTrue(result[0].contains("Warning: Deprecated attribute"))
        assertTrue(result[0].contains("File: document.adoc"))
        assertTrue(result[0].contains("Line: 5"))
        assertTrue(result[1].contains("Warning: Unused macro"))
        assertTrue(result[1].contains("Line: 10"))
    }
    
    @Test
    fun `should format processing warnings`() {
        val processingWarnings = listOf(
            ProcessingWarning("Include depth approaching limit", loc(8), ProcessingWarningType.WHITESPACE_NORMALIZATION),
            ProcessingWarning("Circular reference detected", loc(12), ProcessingWarningType.CROSS_REFERENCE_UNRESOLVED)
        )
        
        val result = ErrorFormatter.formatProcessingWarnings(processingWarnings)
        
        assertEquals(2, result.size)
        assertTrue(result[0].contains("Warning: Include depth approaching limit"))
        assertTrue(result[0].contains("Line: 8"))
        assertTrue(result[1].contains("Warning: Circular reference detected"))
        assertTrue(result[1].contains("Line: 12"))
    }
    
    @Test
    fun `should handle empty error list`() {
        val errors = emptyList<ErrorInfo>()
        
        val result = ErrorFormatter.formatMultipleErrors(errors, "Test")
        
        assertTrue(result.contains("Test failed with 0 error(s):"))
    }
    
    @Test
    fun `should separate multiple errors with blank lines`() {
        val errors = listOf(
            ErrorInfo("Error 1", "file1.adoc", 1),
            ErrorInfo("Error 2", "file2.adoc", 2)
        )
        
        val result = ErrorFormatter.formatMultipleErrors(errors, "Test")
        
        // Check that errors are separated (there should be blank lines between them)
        val lines = result.lines()
        assertTrue(lines.size > 4) // At least header + blank + error1 + blank + error2
    }
    
    @Test
    fun `should use custom operation description`() {
        val errors = listOf(ErrorInfo("Test error"))
        
        val result = ErrorFormatter.formatMultipleErrors(errors, "Custom Operation")
        
        assertTrue(result.contains("Custom Operation failed with 1 error(s):"))
    }
    
    @Test
    fun `should handle errors without file paths`() {
        val errors = listOf(
            ErrorInfo("Generic error", lineNumber = 5)
        )
        
        val result = ErrorFormatter.formatMultipleErrors(errors)
        
        assertTrue(result.contains("Error: Generic error"))
        assertTrue(result.contains("Line: 5"))
        assertTrue(!result.contains("File:"))
    }
    
    @Test
    fun `should handle errors without line numbers`() {
        val errors = listOf(
            ErrorInfo("File error", filePath = "test.adoc")
        )
        
        val result = ErrorFormatter.formatMultipleErrors(errors)
        
        assertTrue(result.contains("Error: File error"))
        assertTrue(result.contains("File: test.adoc"))
        assertTrue(!result.contains("Line:"))
    }
}
