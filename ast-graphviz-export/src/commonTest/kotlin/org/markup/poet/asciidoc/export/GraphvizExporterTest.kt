package org.markup.poet.asciidoc.export

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.Header
import org.markup.poet.asciidoc.asg.InlineText
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.LeafBlockForm
import org.markup.poet.asciidoc.asg.LeafBlockName
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
        val document = AsgDocument()

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
        val document = AsgDocument(
            header = Header(title = listOf(InlineText("Test Document")))
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
        val paragraph = LeafBlock(
            name = LeafBlockName.PARAGRAPH,
            form = LeafBlockForm.PARAGRAPH,
            inlines = listOf(InlineText("Hello world"))
        )
        val document = AsgDocument(
            header = Header(title = listOf(InlineText("Test Document"))),
            blocks = listOf(paragraph)
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
        val paragraph = LeafBlock(
            name = LeafBlockName.PARAGRAPH,
            form = LeafBlockForm.PARAGRAPH,
            inlines = listOf(InlineText("Text with \"quotes\" and \n newlines"))
        )
        val document = AsgDocument(
            header = Header(title = listOf(InlineText("Document with \"special\" characters"))),
            blocks = listOf(paragraph)
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
