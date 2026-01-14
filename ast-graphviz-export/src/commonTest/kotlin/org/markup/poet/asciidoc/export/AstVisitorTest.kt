package org.markup.poet.asciidoc.export

import org.markup.poet.asciidoc.ast.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AstVisitorTest {
    
    @Test
    fun `should visit simple document with paragraph`() {
        // Arrange
        val document = Document(
            title = "Test Document",
            children = listOf(
                Paragraph(
                    content = listOf(
                        Text("Hello world", sourceLocation = SourceLocation(1, 0))
                    ),
                    sourceLocation = SourceLocation(1, 0)
                )
            ),
            documentAttributes = mapOf("author" to "Test Author"),
            sourceLocation = SourceLocation(0, 0)
        )
        
        val visitor = GraphvizAstVisitor()
        
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
        val document = Document(
            title = "Test Document",
            children = listOf(
                Section(
                    level = 1,
                    title = "Section 1",
                    children = listOf(
                        Paragraph(
                            content = listOf(
                                Text("Section content", sourceLocation = SourceLocation(2, 0))
                            ),
                            sourceLocation = SourceLocation(2, 0)
                        )
                    ),
                    sourceLocation = SourceLocation(1, 0)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(0, 0)
        )
        
        val visitor = GraphvizAstVisitor()
        
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
        val document = Document(
            title = null,
            children = listOf(
                AsciiDocList(
                    type = ListType.UNORDERED,
                    items = listOf(
                        ListItem(
                            marker = "*",
                            content = listOf(
                                Text("Item 1", sourceLocation = SourceLocation(1, 0))
                            ),
                            sourceLocation = SourceLocation(1, 0)
                        ),
                        ListItem(
                            marker = "*",
                            content = listOf(
                                Text("Item 2", sourceLocation = SourceLocation(2, 0))
                            ),
                            sourceLocation = SourceLocation(2, 0)
                        )
                    ),
                    sourceLocation = SourceLocation(1, 0)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(0, 0)
        )
        
        val visitor = GraphvizAstVisitor()
        
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
        val document = Document(
            title = "Test",
            children = listOf(
                Paragraph(
                    content = listOf(
                        Text("Test", sourceLocation = SourceLocation(1, 0))
                    ),
                    sourceLocation = SourceLocation(1, 0)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(0, 0)
        )
        
        val visitor = GraphvizAstVisitor()
        
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