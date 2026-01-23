package org.markup.poet.asciidoc.render

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import java.io.File
import java.nio.file.Files

/**
 * Unit tests for JVM PlatformFileReader implementation.
 * 
 * Tests file reading functionality including:
 * - Reading valid files
 * - Handling non-existent files
 * - Handling directories
 * - Handling empty files
 * - Handling absolute and relative paths
 * 
 * Validates: Requirements 2.1, 2.2, 2.3, 2.4
 */
class PlatformFileReaderTest {
    
    private val fileReader = PlatformFileReader()
    
    @Test
    fun `should read valid CSS file successfully`() {
        // Create a temporary CSS file
        val tempFile = Files.createTempFile("test", ".css")
        val cssContent = "body { color: red; }"
        Files.writeString(tempFile, cssContent)
        
        try {
            // Read the file
            val result = fileReader.readFile(tempFile.toString())
            
            // Verify success
            assertTrue(result.isSuccess, "Should successfully read file")
            assertEquals(cssContent, result.getOrNull())
        } finally {
            // Cleanup
            Files.deleteIfExists(tempFile)
        }
    }
    
    @Test
    fun `should read empty CSS file successfully`() {
        // Create an empty temporary file
        val tempFile = Files.createTempFile("empty", ".css")
        
        try {
            // Read the file
            val result = fileReader.readFile(tempFile.toString())
            
            // Verify success with empty content
            assertTrue(result.isSuccess, "Should successfully read empty file")
            assertEquals("", result.getOrNull())
        } finally {
            // Cleanup
            Files.deleteIfExists(tempFile)
        }
    }
    
    @Test
    fun `should handle non-existent file with descriptive error`() {
        val nonExistentPath = "/tmp/non-existent-file-${System.currentTimeMillis()}.css"
        
        // Try to read non-existent file
        val result = fileReader.readFile(nonExistentPath)
        
        // Verify failure
        assertTrue(result.isFailure, "Should fail for non-existent file")
        
        val exception = result.exceptionOrNull()
        assertTrue(exception is CssException.FileNotFound, "Should throw FileNotFound exception")
        
        val fileNotFoundException = exception as CssException.FileNotFound
        assertEquals(nonExistentPath, fileNotFoundException.path)
        assertTrue(fileNotFoundException.reason.contains("not exist") || 
                   fileNotFoundException.reason.contains("not found"),
                   "Error message should mention file not existing")
    }
    
    @Test
    fun `should handle directory path with descriptive error`() {
        // Create a temporary directory
        val tempDir = Files.createTempDirectory("test-dir")
        
        try {
            // Try to read directory as file
            val result = fileReader.readFile(tempDir.toString())
            
            // Verify failure
            assertTrue(result.isFailure, "Should fail for directory path")
            
            val exception = result.exceptionOrNull()
            assertTrue(exception is CssException.FileNotFound, "Should throw FileNotFound exception")
            
            val fileNotFoundException = exception as CssException.FileNotFound
            assertEquals(tempDir.toString(), fileNotFoundException.path)
            assertTrue(fileNotFoundException.reason.contains("not a regular file"),
                       "Error message should mention it's not a regular file")
        } finally {
            // Cleanup
            Files.deleteIfExists(tempDir)
        }
    }
    
    @Test
    fun `should read file with relative path`() {
        // Create a temporary file in current directory
        val tempFile = File("test-relative-${System.currentTimeMillis()}.css")
        val cssContent = ".test { margin: 0; }"
        tempFile.writeText(cssContent)
        
        try {
            // Read using relative path
            val result = fileReader.readFile(tempFile.name)
            
            // Verify success
            assertTrue(result.isSuccess, "Should successfully read file with relative path")
            assertEquals(cssContent, result.getOrNull())
        } finally {
            // Cleanup
            tempFile.delete()
        }
    }
    
    @Test
    fun `should read file with absolute path`() {
        // Create a temporary file with absolute path
        val tempFile = Files.createTempFile("test-absolute", ".css")
        val cssContent = ".absolute { padding: 10px; }"
        Files.writeString(tempFile, cssContent)
        
        try {
            // Read using absolute path
            val result = fileReader.readFile(tempFile.toAbsolutePath().toString())
            
            // Verify success
            assertTrue(result.isSuccess, "Should successfully read file with absolute path")
            assertEquals(cssContent, result.getOrNull())
        } finally {
            // Cleanup
            Files.deleteIfExists(tempFile)
        }
    }
    
    @Test
    fun `should read file with special characters in content`() {
        // Create a file with special characters
        val tempFile = Files.createTempFile("test-special", ".css")
        val cssContent = """
            /* Special characters: @#$%^&*() */
            .unicode { content: "→ ← ↑ ↓ ★ ♥"; }
            .quotes { content: "\"quoted\""; }
            .newlines {
                display: block;
            }
        """.trimIndent()
        Files.writeString(tempFile, cssContent)
        
        try {
            // Read the file
            val result = fileReader.readFile(tempFile.toString())
            
            // Verify success and content preservation
            assertTrue(result.isSuccess, "Should successfully read file with special characters")
            assertEquals(cssContent, result.getOrNull())
        } finally {
            // Cleanup
            Files.deleteIfExists(tempFile)
        }
    }
    
    @Test
    fun `should read large CSS file successfully`() {
        // Create a large CSS file
        val tempFile = Files.createTempFile("test-large", ".css")
        val cssContent = buildString {
            repeat(1000) { i ->
                appendLine(".class-$i { color: #${i.toString(16).padStart(6, '0')}; }")
            }
        }
        Files.writeString(tempFile, cssContent)
        
        try {
            // Read the file
            val result = fileReader.readFile(tempFile.toString())
            
            // Verify success
            assertTrue(result.isSuccess, "Should successfully read large file")
            assertEquals(cssContent, result.getOrNull())
        } finally {
            // Cleanup
            Files.deleteIfExists(tempFile)
        }
    }
}
