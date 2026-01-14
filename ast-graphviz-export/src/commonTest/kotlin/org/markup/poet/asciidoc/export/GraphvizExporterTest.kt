package org.markup.poet.asciidoc.export

import org.markup.poet.asciidoc.ast.*
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertContains

/**
 * Tests for the GraphvizExporter functionality.
 */
class GraphvizExporterTest {
    
    @Test
    fun `should export empty document to valid DOT format`() {
        // Arrange
        val exporter = GraphvizExporter()
        val document = Document(
            title = null,
            children = emptyList(),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(line = 1, column = 0)
        )
        
        // Act
        val result = exporter.export(document)
        
        // Assert
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        assertContains(result, "digraph AST")
        assertContains(result, "doc_1")
        assertContains(result, "}")
    }
    
    @Test
    fun `should export document with title to valid DOT format`() {
        // Arrange
        val exporter = GraphvizExporter()
        val document = Document(
            title = "Test Document",
            children = emptyList(),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(line = 1, column = 0)
        )
        
        // Act
        val result = exporter.export(document)
        
        // Assert
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        assertContains(result, "digraph AST")
        assertContains(result, "Test Document")
        assertContains(result, "doc_1")
    }
    
    @Test
    fun `should export document with paragraph to valid DOT format`() {
        // Arrange
        val exporter = GraphvizExporter()
        val paragraph = Paragraph(
            content = listOf(
                Text("Hello world", emptyMap(), SourceLocation(2, 0))
            ),
            sourceLocation = SourceLocation(2, 0)
        )
        val document = Document(
            title = "Test Document",
            children = listOf(paragraph),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(line = 1, column = 0)
        )
        
        // Act
        val result = exporter.export(document)
        
        // Assert
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        assertContains(result, "digraph AST")
        assertContains(result, "doc_1")
        assertContains(result, "para_1")
        assertContains(result, "text_1")
        assertContains(result, "doc_1 -> para_1")
        assertContains(result, "para_1 -> text_1")
    }
    
    @Test
    fun `should handle special characters in labels`() {
        // Arrange
        val exporter = GraphvizExporter()
        val paragraph = Paragraph(
            content = listOf(
                Text("Text with \"quotes\" and \n newlines", emptyMap(), SourceLocation(2, 0))
            ),
            sourceLocation = SourceLocation(2, 0)
        )
        val document = Document(
            title = "Document with \"special\" characters",
            children = listOf(paragraph),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(line = 1, column = 0)
        )
        
        // Act
        val result = exporter.export(document)
        
        // Assert
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        assertContains(result, "\\\"quotes\\\"")
        assertContains(result, "\\n")
        // Should not contain unescaped quotes or newlines
        assertTrue(!result.contains("\"quotes\""))
    }
}