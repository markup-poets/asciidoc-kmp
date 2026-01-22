package org.markup.poet.antora.assembler

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AssemblerResultTest {
    
    @Test
    fun `should create successful result`() {
        val result = AssemblerResult(
            success = true,
            outputPath = "output/assembled.adoc",
            errors = emptyList(),
            warnings = emptyList(),
            includedFiles = setOf("file1.adoc", "file2.adoc")
        )
        
        assertTrue(result.success)
        assertEquals("output/assembled.adoc", result.outputPath)
        assertTrue(result.errors.isEmpty())
        assertTrue(result.warnings.isEmpty())
        assertEquals(2, result.includedFiles.size)
        assertTrue(result.includedFiles.contains("file1.adoc"))
        assertTrue(result.includedFiles.contains("file2.adoc"))
    }
    
    @Test
    fun `should create failed result with errors`() {
        val error = AssemblerError(
            message = "File not found",
            filePath = "missing.adoc",
            lineNumber = 10,
            errorType = AssemblerErrorType.INCLUDE_NOT_FOUND
        )
        
        val result = AssemblerResult(
            success = false,
            outputPath = null,
            errors = listOf(error),
            warnings = emptyList(),
            includedFiles = emptySet()
        )
        
        assertFalse(result.success)
        assertNull(result.outputPath)
        assertEquals(1, result.errors.size)
        assertEquals("File not found", result.errors[0].message)
        assertTrue(result.warnings.isEmpty())
        assertTrue(result.includedFiles.isEmpty())
    }
    
    @Test
    fun `should create result with warnings`() {
        val warning = AssemblerWarning(
            message = "Image not found",
            filePath = "page.adoc",
            lineNumber = 25
        )
        
        val result = AssemblerResult(
            success = true,
            outputPath = "output.adoc",
            errors = emptyList(),
            warnings = listOf(warning),
            includedFiles = setOf("page.adoc")
        )
        
        assertTrue(result.success)
        assertEquals(1, result.warnings.size)
        assertEquals("Image not found", result.warnings[0].message)
        assertEquals("page.adoc", result.warnings[0].filePath)
        assertEquals(25, result.warnings[0].lineNumber)
    }
    
    @Test
    fun `should create result with multiple errors and warnings`() {
        val errors = listOf(
            AssemblerError(
                message = "Parse error",
                filePath = "file1.adoc",
                lineNumber = 5,
                errorType = AssemblerErrorType.PARSE_ERROR
            ),
            AssemblerError(
                message = "Circular dependency detected",
                filePath = "file2.adoc",
                lineNumber = null,
                errorType = AssemblerErrorType.CIRCULAR_DEPENDENCY
            )
        )
        
        val warnings = listOf(
            AssemblerWarning(
                message = "Warning 1",
                filePath = "file3.adoc",
                lineNumber = 10
            ),
            AssemblerWarning(
                message = "Warning 2",
                filePath = null,
                lineNumber = null
            )
        )
        
        val result = AssemblerResult(
            success = false,
            outputPath = null,
            errors = errors,
            warnings = warnings,
            includedFiles = setOf("file1.adoc", "file2.adoc", "file3.adoc")
        )
        
        assertFalse(result.success)
        assertEquals(2, result.errors.size)
        assertEquals(2, result.warnings.size)
        assertEquals(3, result.includedFiles.size)
    }
}
