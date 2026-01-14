package org.markup.poet.asciidoc.export

import org.markup.poet.asciidoc.ast.*
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import java.io.File
import java.nio.file.Files

/**
 * JVM-specific tests for file export functionality.
 */
class FileExportTest {
    
    @Test
    fun `should export document to file successfully`() {
        // Arrange
        val exporter = GraphvizExporter()
        val document = Document(
            title = "Test Document",
            children = emptyList(),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(line = 1, column = 0)
        )
        
        val tempDir = Files.createTempDirectory("graphviz-test").toFile()
        val outputFile = File(tempDir, "output.dot")
        
        try {
            // Act
            val result = exporter.exportToFile(document, outputFile.absolutePath)
            
            // Assert
            assertTrue(result is FileWriteResult.Success)
            assertTrue(outputFile.exists())
            
            val content = outputFile.readText()
            assertTrue(content.contains("digraph AST"))
            assertTrue(content.contains("Test Document"))
        } finally {
            // Cleanup
            outputFile.delete()
            tempDir.delete()
        }
    }
    
    @Test
    fun `should create parent directories when they don't exist`() {
        // Arrange
        val exporter = GraphvizExporter()
        val document = Document(
            title = "Test Document",
            children = emptyList(),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(line = 1, column = 0)
        )
        
        val tempDir = Files.createTempDirectory("graphviz-test").toFile()
        val nestedDir = File(tempDir, "nested/deep/path")
        val outputFile = File(nestedDir, "output.dot")
        
        try {
            // Act
            val result = exporter.exportToFile(document, outputFile.absolutePath)
            
            // Assert
            assertTrue(result is FileWriteResult.Success)
            assertTrue(nestedDir.exists())
            assertTrue(outputFile.exists())
            
            val content = outputFile.readText()
            assertTrue(content.contains("digraph AST"))
        } finally {
            // Cleanup
            outputFile.delete()
            nestedDir.deleteRecursively()
            tempDir.delete()
        }
    }
    
    @Test
    fun `should return success result with correct path`() {
        // Arrange
        val exporter = GraphvizExporter()
        val document = Document(
            title = "Test Document",
            children = emptyList(),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(line = 1, column = 0)
        )
        
        val tempDir = Files.createTempDirectory("graphviz-test").toFile()
        val outputFile = File(tempDir, "output.dot")
        
        try {
            // Act
            val result = exporter.exportToFile(document, outputFile.absolutePath)
            
            // Assert
            assertTrue(result is FileWriteResult.Success)
            assertEquals(outputFile.absolutePath, (result as FileWriteResult.Success).path)
        } finally {
            // Cleanup
            outputFile.delete()
            tempDir.delete()
        }
    }
    
    @Test
    fun `should handle invalid file paths gracefully`() {
        // Arrange
        val exporter = GraphvizExporter()
        val document = Document(
            title = "Test Document",
            children = emptyList(),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(line = 1, column = 0)
        )
        
        // Use an invalid path (trying to write to a directory that can't be created)
        val invalidPath = "/\u0000invalid/path/output.dot"
        
        // Act
        val result = exporter.exportToFile(document, invalidPath)
        
        // Assert
        assertTrue(result is FileWriteResult.Error)
        val error = result as FileWriteResult.Error
        assertEquals(invalidPath, error.path)
        assertTrue(error.message.isNotEmpty())
    }
}
