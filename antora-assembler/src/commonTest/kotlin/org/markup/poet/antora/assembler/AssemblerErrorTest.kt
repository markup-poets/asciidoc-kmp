package org.markup.poet.antora.assembler

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AssemblerErrorTest {
    
    @Test
    fun `should create error with all fields`() {
        val error = AssemblerError(
            message = "Include file not found",
            filePath = "docs/page.adoc",
            lineNumber = 42,
            errorType = AssemblerErrorType.INCLUDE_NOT_FOUND
        )
        
        assertEquals("Include file not found", error.message)
        assertEquals("docs/page.adoc", error.filePath)
        assertEquals(42, error.lineNumber)
        assertEquals(AssemblerErrorType.INCLUDE_NOT_FOUND, error.errorType)
    }
    
    @Test
    fun `should create error with null filePath`() {
        val error = AssemblerError(
            message = "General error",
            filePath = null,
            lineNumber = null,
            errorType = AssemblerErrorType.FILE_WRITE_ERROR
        )
        
        assertEquals("General error", error.message)
        assertNull(error.filePath)
        assertNull(error.lineNumber)
        assertEquals(AssemblerErrorType.FILE_WRITE_ERROR, error.errorType)
    }
    
    @Test
    fun `should support all error types`() {
        val errorTypes = listOf(
            AssemblerErrorType.INDEX_FILE_NOT_FOUND,
            AssemblerErrorType.PARSE_ERROR,
            AssemblerErrorType.INCLUDE_NOT_FOUND,
            AssemblerErrorType.CIRCULAR_DEPENDENCY,
            AssemblerErrorType.MAX_DEPTH_EXCEEDED,
            AssemblerErrorType.FILE_WRITE_ERROR,
            AssemblerErrorType.RESOLUTION_ERROR
        )
        
        assertEquals(7, errorTypes.size)
        
        // Verify each error type can be used
        errorTypes.forEach { errorType ->
            val error = AssemblerError(
                message = "Test error",
                filePath = null,
                lineNumber = null,
                errorType = errorType
            )
            assertEquals(errorType, error.errorType)
        }
    }
    
    @Test
    fun `should create index file not found error`() {
        val error = AssemblerError(
            message = "Index file does not exist",
            filePath = "docs/index.adoc",
            lineNumber = null,
            errorType = AssemblerErrorType.INDEX_FILE_NOT_FOUND
        )
        
        assertEquals(AssemblerErrorType.INDEX_FILE_NOT_FOUND, error.errorType)
    }
    
    @Test
    fun `should create parse error with line number`() {
        val error = AssemblerError(
            message = "Invalid AsciiDoc syntax",
            filePath = "docs/page.adoc",
            lineNumber = 15,
            errorType = AssemblerErrorType.PARSE_ERROR
        )
        
        assertEquals(AssemblerErrorType.PARSE_ERROR, error.errorType)
        assertEquals(15, error.lineNumber)
    }
    
    @Test
    fun `should create circular dependency error`() {
        val error = AssemblerError(
            message = "Circular dependency: a.adoc -> b.adoc -> a.adoc",
            filePath = "a.adoc",
            lineNumber = null,
            errorType = AssemblerErrorType.CIRCULAR_DEPENDENCY
        )
        
        assertEquals(AssemblerErrorType.CIRCULAR_DEPENDENCY, error.errorType)
    }
    
    @Test
    fun `should create max depth exceeded error`() {
        val error = AssemblerError(
            message = "Maximum include depth of 50 exceeded",
            filePath = "deeply/nested/file.adoc",
            lineNumber = 10,
            errorType = AssemblerErrorType.MAX_DEPTH_EXCEEDED
        )
        
        assertEquals(AssemblerErrorType.MAX_DEPTH_EXCEEDED, error.errorType)
    }
    
    @Test
    fun `should create resolution error`() {
        val error = AssemblerError(
            message = "Could not resolve resource coordinate",
            filePath = "page.adoc",
            lineNumber = 20,
            errorType = AssemblerErrorType.RESOLUTION_ERROR
        )
        
        assertEquals(AssemblerErrorType.RESOLUTION_ERROR, error.errorType)
    }
}
