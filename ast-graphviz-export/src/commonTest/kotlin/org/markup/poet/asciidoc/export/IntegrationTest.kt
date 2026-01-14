package org.markup.poet.asciidoc.export

import org.markup.poet.asciidoc.ast.*
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertContains
import kotlin.test.assertEquals

/**
 * Integration tests for the complete AST to Graphviz export workflow.
 * Tests end-to-end functionality with realistic AsciiDoc document structures.
 * 
 * **Validates: Requirements 2.4, 3.1**
 */
class IntegrationTest {
    
    @Test
    fun `should export complete document with all block element types`() {
        // Arrange - Create a document with all block element types
        val document = Document(
            title = "Complete AsciiDoc Document",
            children = listOf(
                // Section with nested content
                Section(
                    level = 1,
                    title = "Introduction",
                    children = listOf(
                        Paragraph(
                            content = listOf(
                                Text("This is an introductory paragraph.", sourceLocation = SourceLocation(3, 0))
                            ),
                            sourceLocation = SourceLocation(3, 0)
                        )
                    ),
                    sourceLocation = SourceLocation(2, 0)
                ),
                // Paragraph with mixed inline elements
                Paragraph(
                    content = listOf(
                        Text("This paragraph contains ", sourceLocation = SourceLocation(5, 0)),
                        Strong(
                            content = listOf(Text("bold text", sourceLocation = SourceLocation(5, 25))),
                            sourceLocation = SourceLocation(5, 25)
                        ),
                        Text(" and ", sourceLocation = SourceLocation(5, 35)),
                        Emphasis(
                            content = listOf(Text("italic text", sourceLocation = SourceLocation(5, 40))),
                            sourceLocation = SourceLocation(5, 40)
                        ),
                        Text(".", sourceLocation = SourceLocation(5, 52))
                    ),
                    sourceLocation = SourceLocation(5, 0)
                ),
                // Code block
                CodeBlock(
                    language = "kotlin",
                    content = "fun main() {\n    println(\"Hello World\")\n}",
                    sourceLocation = SourceLocation(7, 0)
                ),
                // Unordered list
                AsciiDocList(
                    type = ListType.UNORDERED,
                    items = listOf(
                        ListItem(
                            marker = "*",
                            content = listOf(Text("First item", sourceLocation = SourceLocation(12, 2))),
                            sourceLocation = SourceLocation(12, 0)
                        ),
                        ListItem(
                            marker = "*",
                            content = listOf(Text("Second item", sourceLocation = SourceLocation(13, 2))),
                            sourceLocation = SourceLocation(13, 0)
                        )
                    ),
                    sourceLocation = SourceLocation(12, 0)
                ),
                // Comment
                Comment(
                    content = "This is a comment block",
                    sourceLocation = SourceLocation(15, 0)
                )
            ),
            documentAttributes = mapOf(
                "author" to "Test Author",
                "version" to "1.0"
            ),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val exporter = GraphvizExporter()
        
        // Act
        val result = exporter.export(document)
        
        // Assert - Verify DOT format structure
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        assertContains(result, "digraph AST")
        assertContains(result, "}")
        
        // Verify all node types are present
        assertContains(result, "doc_1") // Document
        assertContains(result, "sec_1") // Section
        assertContains(result, "para_") // Paragraphs
        assertContains(result, "code_1") // CodeBlock
        assertContains(result, "list_1") // List
        assertContains(result, "item_") // ListItems
        assertContains(result, "comm_1") // Comment
        assertContains(result, "text_") // Text nodes
        assertContains(result, "strong_1") // Strong
        assertContains(result, "em_1") // Emphasis
        
        // Verify edges exist
        assertContains(result, "->") // At least one edge
        
        // Verify labels contain expected content
        assertContains(result, "Complete AsciiDoc Document")
        assertContains(result, "Introduction")
        assertContains(result, "kotlin")
    }
    
    @Test
    fun `should export document with deeply nested sections`() {
        // Arrange - Create a document with multiple section levels
        val document = Document(
            title = "Nested Document",
            children = listOf(
                Section(
                    level = 1,
                    title = "Level 1 Section",
                    children = listOf(
                        Paragraph(
                            content = listOf(Text("Level 1 content", sourceLocation = SourceLocation(3, 0))),
                            sourceLocation = SourceLocation(3, 0)
                        ),
                        Section(
                            level = 2,
                            title = "Level 2 Section",
                            children = listOf(
                                Paragraph(
                                    content = listOf(Text("Level 2 content", sourceLocation = SourceLocation(6, 0))),
                                    sourceLocation = SourceLocation(6, 0)
                                ),
                                Section(
                                    level = 3,
                                    title = "Level 3 Section",
                                    children = listOf(
                                        Paragraph(
                                            content = listOf(Text("Level 3 content", sourceLocation = SourceLocation(9, 0))),
                                            sourceLocation = SourceLocation(9, 0)
                                        )
                                    ),
                                    sourceLocation = SourceLocation(8, 0)
                                )
                            ),
                            sourceLocation = SourceLocation(5, 0)
                        )
                    ),
                    sourceLocation = SourceLocation(2, 0)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val exporter = GraphvizExporter()
        
        // Act
        val result = exporter.export(document)
        
        // Assert
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        
        // Verify all section levels are present
        assertContains(result, "sec_1") // Level 1
        assertContains(result, "sec_2") // Level 2
        assertContains(result, "sec_3") // Level 3
        
        // Verify hierarchical relationships
        assertContains(result, "doc_1 -> sec_1")
        assertContains(result, "sec_1 -> sec_2")
        assertContains(result, "sec_2 -> sec_3")
        
        // Verify section titles
        assertContains(result, "Level 1 Section")
        assertContains(result, "Level 2 Section")
        assertContains(result, "Level 3 Section")
    }
    
    @Test
    fun `should export document with all inline element types`() {
        // Arrange - Create a paragraph with all inline element types
        val document = Document(
            title = "Inline Elements Test",
            children = listOf(
                Paragraph(
                    content = listOf(
                        Text("Plain text, ", sourceLocation = SourceLocation(2, 0)),
                        Strong(
                            content = listOf(Text("bold", sourceLocation = SourceLocation(2, 13))),
                            sourceLocation = SourceLocation(2, 13)
                        ),
                        Text(", ", sourceLocation = SourceLocation(2, 18)),
                        Emphasis(
                            content = listOf(Text("italic", sourceLocation = SourceLocation(2, 20))),
                            sourceLocation = SourceLocation(2, 20)
                        ),
                        Text(", ", sourceLocation = SourceLocation(2, 27)),
                        Code(
                            content = "code",
                            sourceLocation = SourceLocation(2, 29)
                        ),
                        Text(", ", sourceLocation = SourceLocation(2, 34)),
                        Link(
                            url = "https://example.com",
                            text = "link",
                            sourceLocation = SourceLocation(2, 36)
                        ),
                        Text(", ", sourceLocation = SourceLocation(2, 41)),
                        Image(
                            path = "image.png",
                            altText = "An image",
                            sourceLocation = SourceLocation(2, 43)
                        ),
                        Text(", ", sourceLocation = SourceLocation(2, 53)),
                        AttributeReference(
                            key = "version",
                            sourceLocation = SourceLocation(2, 55)
                        ),
                        Text(", ", sourceLocation = SourceLocation(2, 64)),
                        Callout(
                            number = 1,
                            sourceLocation = SourceLocation(2, 66)
                        )
                    ),
                    sourceLocation = SourceLocation(2, 0)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val exporter = GraphvizExporter()
        
        // Act
        val result = exporter.export(document)
        
        // Assert
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        
        // Verify all inline element types are present
        assertContains(result, "text_") // Text
        assertContains(result, "strong_1") // Strong
        assertContains(result, "em_1") // Emphasis
        assertContains(result, "inline_code_1") // Code
        assertContains(result, "link_1") // Link
        assertContains(result, "img_1") // Image
        assertContains(result, "attr_1") // AttributeReference
        assertContains(result, "callout_1") // Callout
        
        // Verify content is present
        assertContains(result, "Plain text")
        assertContains(result, "bold")
        assertContains(result, "italic")
        assertContains(result, "link") // Link text
        assertContains(result, "An image") // Image alt text
    }

    
    @Test
    fun `should export document with nested lists`() {
        // Arrange - Create a document with nested list structures
        val document = Document(
            title = "Nested Lists",
            children = listOf(
                AsciiDocList(
                    type = ListType.UNORDERED,
                    items = listOf(
                        ListItem(
                            marker = "*",
                            content = listOf(Text("Parent item 1", sourceLocation = SourceLocation(2, 2))),
                            nestedList = AsciiDocList(
                                type = ListType.UNORDERED,
                                items = listOf(
                                    ListItem(
                                        marker = "**",
                                        content = listOf(Text("Child item 1.1", sourceLocation = SourceLocation(3, 4))),
                                        sourceLocation = SourceLocation(3, 2)
                                    ),
                                    ListItem(
                                        marker = "**",
                                        content = listOf(Text("Child item 1.2", sourceLocation = SourceLocation(4, 4))),
                                        sourceLocation = SourceLocation(4, 2)
                                    )
                                ),
                                sourceLocation = SourceLocation(3, 2)
                            ),
                            sourceLocation = SourceLocation(2, 0)
                        ),
                        ListItem(
                            marker = "*",
                            content = listOf(Text("Parent item 2", sourceLocation = SourceLocation(5, 2))),
                            sourceLocation = SourceLocation(5, 0)
                        )
                    ),
                    sourceLocation = SourceLocation(2, 0)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val exporter = GraphvizExporter()
        
        // Act
        val result = exporter.export(document)
        
        // Assert
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        
        // Verify list structure
        assertContains(result, "list_1") // Parent list
        assertContains(result, "list_2") // Nested list
        assertContains(result, "item_1") // Parent item 1
        assertContains(result, "item_2") // Child item 1.1
        assertContains(result, "item_3") // Child item 1.2
        assertContains(result, "item_4") // Parent item 2
        
        // Verify nested relationships
        assertContains(result, "item_1 -> list_2") // Parent item contains nested list
        
        // Verify content
        assertContains(result, "Parent item 1")
        assertContains(result, "Child item 1.1")
        assertContains(result, "Child item 1.2")
        assertContains(result, "Parent item 2")
    }
    
    @Test
    fun `should export document with callout list`() {
        // Arrange - Create a document with callout list
        val document = Document(
            title = "Callout List Example",
            children = listOf(
                CodeBlock(
                    language = "kotlin",
                    content = "fun example() { // <1>\n    println(\"test\") // <2>\n}",
                    sourceLocation = SourceLocation(2, 0)
                ),
                CalloutList(
                    items = listOf(
                        CalloutListItem(
                            number = 1,
                            content = listOf(Text("Function declaration", sourceLocation = SourceLocation(7, 4))),
                            sourceLocation = SourceLocation(7, 0)
                        ),
                        CalloutListItem(
                            number = 2,
                            content = listOf(Text("Print statement", sourceLocation = SourceLocation(8, 4))),
                            sourceLocation = SourceLocation(8, 0)
                        )
                    ),
                    sourceLocation = SourceLocation(7, 0)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val exporter = GraphvizExporter()
        
        // Act
        val result = exporter.export(document)
        
        // Assert
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        
        // Verify callout list structure
        assertContains(result, "code_1") // Code block
        assertContains(result, "clist_1") // Callout list
        assertContains(result, "citem_1") // Callout item 1
        assertContains(result, "citem_2") // Callout item 2
        
        // Verify content
        assertContains(result, "kotlin")
        assertContains(result, "Function declaration")
        assertContains(result, "Print statement")
    }
    
    @Test
    fun `should export document with ordered list`() {
        // Arrange - Create a document with ordered list
        val document = Document(
            title = "Ordered List Example",
            children = listOf(
                AsciiDocList(
                    type = ListType.ORDERED,
                    items = listOf(
                        ListItem(
                            marker = "1.",
                            content = listOf(Text("First step", sourceLocation = SourceLocation(2, 3))),
                            sourceLocation = SourceLocation(2, 0)
                        ),
                        ListItem(
                            marker = "2.",
                            content = listOf(Text("Second step", sourceLocation = SourceLocation(3, 3))),
                            sourceLocation = SourceLocation(3, 0)
                        ),
                        ListItem(
                            marker = "3.",
                            content = listOf(Text("Third step", sourceLocation = SourceLocation(4, 3))),
                            sourceLocation = SourceLocation(4, 0)
                        )
                    ),
                    sourceLocation = SourceLocation(2, 0)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val exporter = GraphvizExporter()
        
        // Act
        val result = exporter.export(document)
        
        // Assert
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        
        // Verify ordered list
        assertContains(result, "list_1")
        assertContains(result, "Ordered List")
        assertContains(result, "First step")
        assertContains(result, "Second step")
        assertContains(result, "Third step")
    }
    
    @Test
    fun `should export document with complex nested inline elements`() {
        // Arrange - Create nested inline elements (e.g., bold within emphasis)
        val document = Document(
            title = "Complex Inline Nesting",
            children = listOf(
                Paragraph(
                    content = listOf(
                        Text("This has ", sourceLocation = SourceLocation(2, 0)),
                        Emphasis(
                            content = listOf(
                                Text("italic with ", sourceLocation = SourceLocation(2, 10)),
                                Strong(
                                    content = listOf(Text("bold inside", sourceLocation = SourceLocation(2, 22))),
                                    sourceLocation = SourceLocation(2, 22)
                                )
                            ),
                            sourceLocation = SourceLocation(2, 10)
                        ),
                        Text(" it.", sourceLocation = SourceLocation(2, 34))
                    ),
                    sourceLocation = SourceLocation(2, 0)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val exporter = GraphvizExporter()
        
        // Act
        val result = exporter.export(document)
        
        // Assert
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        
        // Verify nested structure
        assertContains(result, "em_1")
        assertContains(result, "strong_1")
        
        // Verify nesting relationship
        assertContains(result, "em_1 -> strong_1")
        
        // Verify content
        assertContains(result, "italic with")
        assertContains(result, "bold inside")
    }
    
    @Test
    fun `should export document with attributes and metadata`() {
        // Arrange - Create a document with various attributes
        val document = Document(
            title = "Document with Attributes",
            children = listOf(
                Section(
                    level = 1,
                    title = "Section with Attributes",
                    children = listOf(
                        Paragraph(
                            content = listOf(Text("Content", sourceLocation = SourceLocation(3, 0))),
                            attributes = mapOf("role" to "lead", "id" to "intro"),
                            sourceLocation = SourceLocation(3, 0)
                        )
                    ),
                    attributes = mapOf("id" to "section1"),
                    sourceLocation = SourceLocation(2, 0)
                )
            ),
            documentAttributes = mapOf(
                "author" to "John Doe",
                "version" to "2.0",
                "doctype" to "article"
            ),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val exporter = GraphvizExporter(ExportConfig(includeAttributes = true))
        
        // Act
        val result = exporter.export(document)
        
        // Assert
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        
        // Verify document structure
        assertContains(result, "Document with Attributes")
        assertContains(result, "Section with Attributes")
        
        // When attributes are included, they should appear in the output
        // (The exact format depends on implementation)
        assertTrue(result.contains("doc_1") && result.contains("sec_1"))
    }
    
    @Test
    fun `should handle empty document gracefully`() {
        // Arrange
        val document = Document(
            title = null,
            children = emptyList(),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val exporter = GraphvizExporter()
        
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
    fun `should export document with special characters in content`() {
        // Arrange - Create document with special DOT characters
        val document = Document(
            title = "Document with \"quotes\" and \\ backslashes",
            children = listOf(
                Paragraph(
                    content = listOf(
                        Text("Text with \"quotes\", \\backslashes\\, and \nnewlines", sourceLocation = SourceLocation(2, 0))
                    ),
                    sourceLocation = SourceLocation(2, 0)
                ),
                CodeBlock(
                    language = "text",
                    content = "Code with { braces } and | pipes",
                    sourceLocation = SourceLocation(4, 0)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val exporter = GraphvizExporter()
        
        // Act
        val result = exporter.export(document)
        
        // Assert
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        
        // Verify special characters are escaped
        // In DOT format, quotes are escaped as \" and backslashes as \\
        assertTrue(result.contains("\\\"") || result.contains("&quot;"))
        assertTrue(result.contains("\\\\"))
        
        // Verify the output is still valid DOT format
        assertContains(result, "digraph AST")
        assertContains(result, "}")
    }
    
    @Test
    fun `should maintain consistent node IDs across multiple exports`() {
        // Arrange
        val document = Document(
            title = "Consistency Test",
            children = listOf(
                Paragraph(
                    content = listOf(Text("Test content", sourceLocation = SourceLocation(2, 0))),
                    sourceLocation = SourceLocation(2, 0)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val exporter = GraphvizExporter()
        
        // Act - Export the same document twice
        val result1 = exporter.export(document)
        val result2 = exporter.export(document)
        
        // Assert - Results should be identical (deterministic)
        assertEquals(result1, result2)
    }
}
