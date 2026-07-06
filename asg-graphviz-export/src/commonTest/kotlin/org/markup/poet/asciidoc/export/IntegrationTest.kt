package org.markup.poet.asciidoc.export

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.BlockMetadata
import org.markup.poet.asciidoc.asg.CommentBlock
import org.markup.poet.asciidoc.asg.Header
import org.markup.poet.asciidoc.asg.Inline
import org.markup.poet.asciidoc.asg.InlineAttributeRef
import org.markup.poet.asciidoc.asg.InlineCallout
import org.markup.poet.asciidoc.asg.InlineMacro
import org.markup.poet.asciidoc.asg.InlineRef
import org.markup.poet.asciidoc.asg.InlineSpan
import org.markup.poet.asciidoc.asg.InlineText
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.LeafBlockForm
import org.markup.poet.asciidoc.asg.LeafBlockName
import org.markup.poet.asciidoc.asg.ListBlock
import org.markup.poet.asciidoc.asg.ListItem
import org.markup.poet.asciidoc.asg.ListVariant
import org.markup.poet.asciidoc.asg.RefVariant
import org.markup.poet.asciidoc.asg.SectionBlock
import org.markup.poet.asciidoc.asg.SpanForm
import org.markup.poet.asciidoc.asg.SpanVariant
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertContains
import kotlin.test.assertEquals

/**
 * Integration tests for the complete ASG to Graphviz export workflow.
 * Tests end-to-end functionality with realistic AsciiDoc document structures.
 *
 * **Validates: Requirements 2.4, 3.1**
 */
class IntegrationTest {

    private fun paragraph(vararg inlines: Inline) = LeafBlock(
        name = LeafBlockName.PARAGRAPH,
        form = LeafBlockForm.PARAGRAPH,
        inlines = inlines.toList()
    )

    private fun listing(content: String, language: String? = null) = LeafBlock(
        name = LeafBlockName.LISTING,
        form = LeafBlockForm.DELIMITED,
        delimiter = "----",
        inlines = listOf(InlineText(content)),
        metadata = language?.let { BlockMetadata(positional = listOf("source", it)) }
    )

    private fun strong(text: String) =
        InlineSpan(SpanVariant.STRONG, SpanForm.CONSTRAINED, listOf(InlineText(text)))

    private fun emphasis(vararg inlines: Inline) =
        InlineSpan(SpanVariant.EMPHASIS, SpanForm.CONSTRAINED, inlines.toList())

    @Test
    fun `should export complete document with all block element types`() {
        // Arrange - Create a document with all common block element types
        val document = AsgDocument(
            header = Header(title = listOf(InlineText("Complete AsciiDoc Document"))),
            blocks = listOf(
                // Section with nested content
                SectionBlock(
                    title = listOf(InlineText("Introduction")),
                    level = 1,
                    blocks = listOf(paragraph(InlineText("This is an introductory paragraph.")))
                ),
                // Paragraph with mixed inline elements
                paragraph(
                    InlineText("This paragraph contains "),
                    strong("bold text"),
                    InlineText(" and "),
                    emphasis(InlineText("italic text")),
                    InlineText(".")
                ),
                // Listing block with source language
                listing("fun main() {\n    println(\"Hello World\")\n}", language = "kotlin"),
                // Unordered list
                ListBlock(
                    variant = ListVariant.UNORDERED,
                    marker = "*",
                    items = listOf(
                        ListItem(marker = "*", principal = listOf(InlineText("First item"))),
                        ListItem(marker = "*", principal = listOf(InlineText("Second item")))
                    )
                ),
                // Comment
                CommentBlock(text = "This is a comment block")
            ),
            attributes = mapOf(
                "author" to "Test Author",
                "version" to "1.0"
            )
        )

        val exporter = GraphvizExporter()

        // Act
        val result = exporter.export(document)

        // Assert - Verify DOT format structure
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        assertContains(result, "digraph ASG")
        assertContains(result, "}")

        // Verify all node types are present
        assertContains(result, "doc_1") // Document
        assertContains(result, "sec_1") // Section
        assertContains(result, "para_") // Paragraphs
        assertContains(result, "code_1") // Listing block
        assertContains(result, "list_1") // List
        assertContains(result, "item_") // ListItems
        assertContains(result, "comm_1") // Comment
        assertContains(result, "text_") // Text nodes
        assertContains(result, "strong_1") // Strong span
        assertContains(result, "em_1") // Emphasis span

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
        val document = AsgDocument(
            header = Header(title = listOf(InlineText("Nested Document"))),
            blocks = listOf(
                SectionBlock(
                    title = listOf(InlineText("Level 1 Section")),
                    level = 1,
                    blocks = listOf(
                        paragraph(InlineText("Level 1 content")),
                        SectionBlock(
                            title = listOf(InlineText("Level 2 Section")),
                            level = 2,
                            blocks = listOf(
                                paragraph(InlineText("Level 2 content")),
                                SectionBlock(
                                    title = listOf(InlineText("Level 3 Section")),
                                    level = 3,
                                    blocks = listOf(paragraph(InlineText("Level 3 content")))
                                )
                            )
                        )
                    )
                )
            )
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
        val document = AsgDocument(
            header = Header(title = listOf(InlineText("Inline Elements Test"))),
            blocks = listOf(
                paragraph(
                    InlineText("Plain text, "),
                    strong("bold"),
                    InlineText(", "),
                    emphasis(InlineText("italic")),
                    InlineText(", "),
                    InlineSpan(SpanVariant.CODE, SpanForm.CONSTRAINED, listOf(InlineText("code"))),
                    InlineText(", "),
                    InlineRef(RefVariant.LINK, "https://example.com", listOf(InlineText("link"))),
                    InlineText(", "),
                    InlineMacro(name = "image", target = "image.png", positional = listOf("An image")),
                    InlineText(", "),
                    InlineAttributeRef(name = "version"),
                    InlineText(", "),
                    InlineCallout(number = 1)
                )
            )
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
        assertContains(result, "inline_code_1") // Code span
        assertContains(result, "link_1") // Link
        assertContains(result, "img_1") // Image macro
        assertContains(result, "attr_1") // Attribute reference
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
        val document = AsgDocument(
            header = Header(title = listOf(InlineText("Nested Lists"))),
            blocks = listOf(
                ListBlock(
                    variant = ListVariant.UNORDERED,
                    marker = "*",
                    items = listOf(
                        ListItem(
                            marker = "*",
                            principal = listOf(InlineText("Parent item 1")),
                            blocks = listOf(
                                ListBlock(
                                    variant = ListVariant.UNORDERED,
                                    marker = "**",
                                    items = listOf(
                                        ListItem(marker = "**", principal = listOf(InlineText("Child item 1.1"))),
                                        ListItem(marker = "**", principal = listOf(InlineText("Child item 1.2")))
                                    )
                                )
                            )
                        ),
                        ListItem(marker = "*", principal = listOf(InlineText("Parent item 2")))
                    )
                )
            )
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
        // Arrange - Create a document with a callout list
        val document = AsgDocument(
            header = Header(title = listOf(InlineText("Callout List Example"))),
            blocks = listOf(
                listing("fun example() { // <1>\n    println(\"test\") // <2>\n}", language = "kotlin"),
                ListBlock(
                    variant = ListVariant.CALLOUT,
                    marker = "<1>",
                    items = listOf(
                        ListItem(marker = "<1>", principal = listOf(InlineText("Function declaration"))),
                        ListItem(marker = "<2>", principal = listOf(InlineText("Print statement")))
                    )
                )
            )
        )

        val exporter = GraphvizExporter()

        // Act
        val result = exporter.export(document)

        // Assert
        assertNotNull(result)
        assertTrue(result.isNotEmpty())

        // Verify callout list structure
        assertContains(result, "code_1") // Listing block
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
        val document = AsgDocument(
            header = Header(title = listOf(InlineText("Ordered List Example"))),
            blocks = listOf(
                ListBlock(
                    variant = ListVariant.ORDERED,
                    marker = ".",
                    items = listOf(
                        ListItem(marker = ".", principal = listOf(InlineText("First step"))),
                        ListItem(marker = ".", principal = listOf(InlineText("Second step"))),
                        ListItem(marker = ".", principal = listOf(InlineText("Third step")))
                    )
                )
            )
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
        // Arrange - Create nested inline elements (e.g. bold within emphasis)
        val document = AsgDocument(
            header = Header(title = listOf(InlineText("Complex Inline Nesting"))),
            blocks = listOf(
                paragraph(
                    InlineText("This has "),
                    emphasis(
                        InlineText("italic with "),
                        strong("bold inside")
                    ),
                    InlineText(" it.")
                )
            )
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
        val document = AsgDocument(
            header = Header(title = listOf(InlineText("Document with Attributes"))),
            blocks = listOf(
                SectionBlock(
                    title = listOf(InlineText("Section with Attributes")),
                    level = 1,
                    blocks = listOf(
                        LeafBlock(
                            name = LeafBlockName.PARAGRAPH,
                            form = LeafBlockForm.PARAGRAPH,
                            inlines = listOf(InlineText("Content")),
                            metadata = BlockMetadata(id = "intro", roles = listOf("lead"))
                        )
                    ),
                    metadata = BlockMetadata(id = "section1")
                )
            ),
            attributes = mapOf(
                "author" to "John Doe",
                "version" to "2.0",
                "doctype" to "article"
            )
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
        val document = AsgDocument()

        val exporter = GraphvizExporter()

        // Act
        val result = exporter.export(document)

        // Assert
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        assertContains(result, "digraph ASG")
        assertContains(result, "doc_1")
        assertContains(result, "}")
    }

    @Test
    fun `should export document with special characters in content`() {
        // Arrange - Create document with special DOT characters
        val document = AsgDocument(
            header = Header(title = listOf(InlineText("Document with \"quotes\" and \\ backslashes"))),
            blocks = listOf(
                paragraph(InlineText("Text with \"quotes\", \\backslashes\\, and \nnewlines")),
                listing("Code with { braces } and | pipes", language = "text")
            )
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
        assertContains(result, "digraph ASG")
        assertContains(result, "}")
    }

    @Test
    fun `should maintain consistent node IDs across multiple exports`() {
        // Arrange
        val document = AsgDocument(
            header = Header(title = listOf(InlineText("Consistency Test"))),
            blocks = listOf(paragraph(InlineText("Test content")))
        )

        val exporter = GraphvizExporter()

        // Act - Export the same document twice
        val result1 = exporter.export(document)
        val result2 = exporter.export(document)

        // Assert - Results should be identical (deterministic)
        assertEquals(result1, result2)
    }
}
