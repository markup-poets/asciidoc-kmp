package org.markup.poet.asciidoc.export

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.Header
import org.markup.poet.asciidoc.asg.InlineText
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.LeafBlockForm
import org.markup.poet.asciidoc.asg.LeafBlockName
import org.markup.poet.asciidoc.asg.ListBlock
import org.markup.poet.asciidoc.asg.ListItem
import org.markup.poet.asciidoc.asg.ListVariant
import org.markup.poet.asciidoc.asg.SectionBlock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AsgVisitorTest {

    private fun paragraph(text: String) = LeafBlock(
        name = LeafBlockName.PARAGRAPH,
        form = LeafBlockForm.PARAGRAPH,
        inlines = listOf(InlineText(text))
    )

    @Test
    fun `should visit simple document with paragraph`() {
        // Arrange
        val document = AsgDocument(
            header = Header(title = listOf(InlineText("Test Document"))),
            blocks = listOf(paragraph("Hello world")),
            attributes = mapOf("author" to "Test Author")
        )

        val visitor = GraphvizAsgVisitor()

        // Act
        val result = visitor.visit(document)
        val graphData = visitor.getCollectedData(document)

        // Assert
        assertTrue(result is VisitResult.Success)
        assertEquals(3, graphData.nodes.size) // Document + Paragraph + Text
        assertEquals(2, graphData.edges.size) // Document->Paragraph, Paragraph->Text
        assertEquals("Test Document", graphData.metadata.title)
        assertEquals(3, graphData.metadata.nodeCount)
    }

    @Test
    fun `should handle nested sections`() {
        // Arrange
        val document = AsgDocument(
            header = Header(title = listOf(InlineText("Test Document"))),
            blocks = listOf(
                SectionBlock(
                    title = listOf(InlineText("Section 1")),
                    level = 1,
                    blocks = listOf(paragraph("Section content"))
                )
            )
        )

        val visitor = GraphvizAsgVisitor()

        // Act
        val result = visitor.visit(document)
        val graphData = visitor.getCollectedData(document)

        // Assert
        assertTrue(result is VisitResult.Success)
        assertEquals(4, graphData.nodes.size) // Document + Section + Paragraph + Text
        assertEquals(3, graphData.edges.size) // Document->Section, Section->Paragraph, Paragraph->Text
    }

    @Test
    fun `should handle list structures`() {
        // Arrange
        val document = AsgDocument(
            blocks = listOf(
                ListBlock(
                    variant = ListVariant.UNORDERED,
                    marker = "*",
                    items = listOf(
                        ListItem(marker = "*", principal = listOf(InlineText("Item 1"))),
                        ListItem(marker = "*", principal = listOf(InlineText("Item 2")))
                    )
                )
            )
        )

        val visitor = GraphvizAsgVisitor()

        // Act
        val result = visitor.visit(document)
        val graphData = visitor.getCollectedData(document)

        // Assert
        assertTrue(result is VisitResult.Success)
        // Document + List + 2 ListItems + 2 Text nodes = 6 nodes total
        assertEquals(6, graphData.nodes.size)
        // Document->List, List->Item1, List->Item2, Item1->Text1, Item2->Text2 = 5 edges total
        assertEquals(5, graphData.edges.size)
    }

    @Test
    fun `should reset visitor state correctly`() {
        // Arrange
        val document = AsgDocument(
            header = Header(title = listOf(InlineText("Test"))),
            blocks = listOf(paragraph("Test"))
        )

        val visitor = GraphvizAsgVisitor()

        // Act - First visit
        visitor.visit(document)
        val firstData = visitor.getCollectedData(document)

        // Reset and visit again
        visitor.reset()
        visitor.visit(document)
        val secondData = visitor.getCollectedData(document)

        // Assert
        assertEquals(firstData.nodes.size, secondData.nodes.size)
        assertEquals(firstData.edges.size, secondData.edges.size)
        // Node IDs should be the same after reset (deterministic)
        assertEquals(firstData.nodes.map { it.id }, secondData.nodes.map { it.id })
    }
}
