package org.markup.poet.cli

import org.markup.poet.asciidoc.processing.FileReadResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission

class JvmFileReaderTest {
    
    private val fileReader = JvmFileReader()
    
    @Test
    fun `should read existing file successfully`() {
        // Arrange
        val tempFile = createTempFile("test", ".txt")
        val expectedContent = "Hello, World!\nThis is a test file."
        tempFile.writeText(expectedContent)
        
        try {
            // Act
            val result = fileReader.readFile(tempFile.absolutePath)
            
            // Assert
            assertTrue(result is FileReadResult.Success, "Expected Success result")
            assertEquals(expectedContent, (result as FileReadResult.Success).content)
        } finally {
            tempFile.delete()
        }
    }
    
    @Test
    fun `should read empty file successfully`() {
        // Arrange
        val tempFile = createTempFile("empty", ".txt")
        tempFile.writeText("")
        
        try {
            // Act
            val result = fileReader.readFile(tempFile.absolutePath)
            
            // Assert
            assertTrue(result is FileReadResult.Success, "Expected Success result")
            assertEquals("", (result as FileReadResult.Success).content)
        } finally {
            tempFile.delete()
        }
    }
    
    @Test
    fun `should read file with special characters`() {
        // Arrange
        val tempFile = createTempFile("special", ".txt")
        val expectedContent = "Special chars: äöü ñ 中文 🎉\nNewlines\n\nTabs\t\there"
        tempFile.writeText(expectedContent)
        
        try {
            // Act
            val result = fileReader.readFile(tempFile.absolutePath)
            
            // Assert
            assertTrue(result is FileReadResult.Success, "Expected Success result")
            assertEquals(expectedContent, (result as FileReadResult.Success).content)
        } finally {
            tempFile.delete()
        }
    }
    
    @Test
    fun `should read large file successfully`() {
        // Arrange
        val tempFile = createTempFile("large", ".txt")
        val largeContent = "Line of text\n".repeat(10000)
        tempFile.writeText(largeContent)
        
        try {
            // Act
            val result = fileReader.readFile(tempFile.absolutePath)
            
            // Assert
            assertTrue(result is FileReadResult.Success, "Expected Success result")
            assertEquals(largeContent, (result as FileReadResult.Success).content)
        } finally {
            tempFile.delete()
        }
    }
    
    @Test
    fun `should return error for non-existent file`() {
        // Arrange
        val nonExistentPath = "/path/to/nonexistent/file.txt"
        
        // Act
        val result = fileReader.readFile(nonExistentPath)
        
        // Assert
        assertTrue(result is FileReadResult.Error, "Expected Error result")
        val errorMessage = (result as FileReadResult.Error).message
        assertTrue(errorMessage.contains("File not found"), "Error message should mention file not found")
        assertTrue(errorMessage.contains(nonExistentPath), "Error message should include the file path")
    }
    
    @Test
    fun `should return error for directory path`() {
        // Arrange
        val tempDir = createTempDir("testdir")
        
        try {
            // Act
            val result = fileReader.readFile(tempDir.absolutePath)
            
            // Assert
            assertTrue(result is FileReadResult.Error, "Expected Error result")
            val errorMessage = (result as FileReadResult.Error).message
            assertTrue(errorMessage.contains("not a file"), "Error message should mention it's not a file")
            assertTrue(errorMessage.contains(tempDir.absolutePath), "Error message should include the path")
        } finally {
            tempDir.delete()
        }
    }
    
    @Test
    fun `should handle relative file paths`() {
        // Arrange
        val tempFile = createTempFile("relative", ".txt")
        val expectedContent = "Relative path test"
        tempFile.writeText(expectedContent)
        
        try {
            // Get relative path from current directory
            val currentDir = File(System.getProperty("user.dir"))
            val relativePath = tempFile.relativeTo(currentDir).path
            
            // Act
            val result = fileReader.readFile(relativePath)
            
            // Assert
            assertTrue(result is FileReadResult.Success, "Expected Success result")
            assertEquals(expectedContent, (result as FileReadResult.Success).content)
        } finally {
            tempFile.delete()
        }
    }
    
    @Test
    fun `should handle absolute file paths`() {
        // Arrange
        val tempFile = createTempFile("absolute", ".txt")
        val expectedContent = "Absolute path test"
        tempFile.writeText(expectedContent)
        
        try {
            // Act
            val result = fileReader.readFile(tempFile.absolutePath)
            
            // Assert
            assertTrue(result is FileReadResult.Success, "Expected Success result")
            assertEquals(expectedContent, (result as FileReadResult.Success).content)
        } finally {
            tempFile.delete()
        }
    }
    
    @Test
    fun `should return descriptive error message for file not found`() {
        // Arrange
        val missingFile = "missing-file-12345.txt"
        
        // Act
        val result = fileReader.readFile(missingFile)
        
        // Assert
        assertTrue(result is FileReadResult.Error, "Expected Error result")
        val errorMessage = (result as FileReadResult.Error).message
        assertEquals("File not found: $missingFile", errorMessage)
    }
    
    @Test
    fun `should return descriptive error message for directory`() {
        // Arrange
        val tempDir = createTempDir("testdir")
        
        try {
            // Act
            val result = fileReader.readFile(tempDir.absolutePath)
            
            // Assert
            assertTrue(result is FileReadResult.Error, "Expected Error result")
            val errorMessage = (result as FileReadResult.Error).message
            assertEquals("Path is not a file: ${tempDir.absolutePath}", errorMessage)
        } finally {
            tempDir.delete()
        }
    }
    
    @Test
    fun `should read file with AsciiDoc content`() {
        // Arrange
        val tempFile = createTempFile("asciidoc", ".adoc")
        val asciidocContent = """
            = Document Title
            
            == Section 1
            
            This is a paragraph.
            
            include::other.adoc[]
            
            == Section 2
            
            * List item 1
            * List item 2
        """.trimIndent()
        tempFile.writeText(asciidocContent)
        
        try {
            // Act
            val result = fileReader.readFile(tempFile.absolutePath)
            
            // Assert
            assertTrue(result is FileReadResult.Success, "Expected Success result")
            assertEquals(asciidocContent, (result as FileReadResult.Success).content)
        } finally {
            tempFile.delete()
        }
    }
    
    @Test
    fun `should handle file with only whitespace`() {
        // Arrange
        val tempFile = createTempFile("whitespace", ".txt")
        val whitespaceContent = "   \n\t\n  \n"
        tempFile.writeText(whitespaceContent)
        
        try {
            // Act
            val result = fileReader.readFile(tempFile.absolutePath)
            
            // Assert
            assertTrue(result is FileReadResult.Success, "Expected Success result")
            assertEquals(whitespaceContent, (result as FileReadResult.Success).content)
        } finally {
            tempFile.delete()
        }
    }
    
    @Test
    fun `should handle file with single line no newline`() {
        // Arrange
        val tempFile = createTempFile("singleline", ".txt")
        val singleLineContent = "Single line without newline"
        tempFile.writeText(singleLineContent)
        
        try {
            // Act
            val result = fileReader.readFile(tempFile.absolutePath)
            
            // Assert
            assertTrue(result is FileReadResult.Success, "Expected Success result")
            assertEquals(singleLineContent, (result as FileReadResult.Success).content)
        } finally {
            tempFile.delete()
        }
    }
    
    @Test
    fun `should handle file with multiple consecutive newlines`() {
        // Arrange
        val tempFile = createTempFile("newlines", ".txt")
        val content = "Line 1\n\n\n\nLine 2"
        tempFile.writeText(content)
        
        try {
            // Act
            val result = fileReader.readFile(tempFile.absolutePath)
            
            // Assert
            assertTrue(result is FileReadResult.Success, "Expected Success result")
            assertEquals(content, (result as FileReadResult.Success).content)
        } finally {
            tempFile.delete()
        }
    }
    
    @Test
    fun `should handle file path with spaces`() {
        // Arrange
        val tempDir = createTempDir("test dir with spaces")
        val tempFile = File(tempDir, "file with spaces.txt")
        val expectedContent = "Content in file with spaces"
        tempFile.writeText(expectedContent)
        
        try {
            // Act
            val result = fileReader.readFile(tempFile.absolutePath)
            
            // Assert
            assertTrue(result is FileReadResult.Success, "Expected Success result")
            assertEquals(expectedContent, (result as FileReadResult.Success).content)
        } finally {
            tempFile.delete()
            tempDir.delete()
        }
    }
    
    @Test
    fun `should handle nested directory structure`() {
        // Arrange
        val tempDir = createTempDir("parent")
        val childDir = File(tempDir, "child")
        childDir.mkdir()
        val tempFile = File(childDir, "nested.txt")
        val expectedContent = "Nested file content"
        tempFile.writeText(expectedContent)
        
        try {
            // Act
            val result = fileReader.readFile(tempFile.absolutePath)
            
            // Assert
            assertTrue(result is FileReadResult.Success, "Expected Success result")
            assertEquals(expectedContent, (result as FileReadResult.Success).content)
        } finally {
            tempFile.delete()
            childDir.delete()
            tempDir.delete()
        }
    }
}
