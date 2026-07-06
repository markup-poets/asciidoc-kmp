package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.Block
import org.markup.poet.asciidoc.asg.IncludeBlock
import org.markup.poet.asciidoc.asg.InlineText
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.LeafBlockForm
import org.markup.poet.asciidoc.asg.LeafBlockName
import org.markup.poet.asciidoc.asg.Location
import org.markup.poet.asciidoc.asg.Position
import org.markup.poet.asciidoc.parser.AsciidocParser
import org.markup.poet.asciidoc.parser.ParseResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for IncludeResolver.
 * Tests basic include resolution, relative path resolution, file not found errors,
 * line range filtering, nested includes, circular dependency detection, and max depth enforcement.
 */
class IncludeResolverTest {

    private fun loc(line: Int) = Location(Position(line, 1), Position(line, 1))

    private fun paragraph(text: String, line: Int = 1) = LeafBlock(
        name = LeafBlockName.PARAGRAPH,
        form = LeafBlockForm.PARAGRAPH,
        inlines = listOf(InlineText(text, loc(line))),
        location = loc(line)
    )

    // Mock FileReader for testing
    private class MockFileReader(
        private val files: Map<String, String> = emptyMap()
    ) : FileReader {
        override fun readFile(path: String): FileReadResult {
            return files[path]?.let { FileReadResult.Success(it) }
                ?: FileReadResult.Error("File not found: $path")
        }
    }

    /** Mock parser building one paragraph per source line; [blocksFor] can override. */
    private open inner class MockParser : AsciidocParser {
        open fun blocksFor(source: String): List<Block> = source.lines().map { paragraph(it) }

        override fun parse(source: String): ParseResult {
            return ParseResult(
                document = AsgDocument(blocks = blocksFor(source)),
                errors = emptyList(),
                warnings = emptyList()
            )
        }
    }

    @Test
    fun `should resolve basic include directive`() {
        // Arrange
        val fileContent = "This is included content"
        val fileReader = MockFileReader(mapOf("included.adoc" to fileContent))
        val resolver = DefaultIncludeResolver(MockParser())

        val document = AsgDocument(
            blocks = listOf(IncludeBlock(path = "included.adoc", location = loc(1)))
        )

        val config = IncludeConfig(
            maxDepth = 10,
            basePath = "",
            fileReader = fileReader
        )

        // Act
        val result = resolver.resolve(document, config)

        // Assert
        assertEquals(0, result.errors.size, "Should have no errors")
        assertEquals(1, result.includedFiles.size, "Should have included one file")
        assertTrue(result.includedFiles.contains("included.adoc"), "Should track included file")
        assertEquals(1, result.document.blocks.size, "Should have one block (the included paragraph)")

        val paragraph = result.document.blocks[0] as LeafBlock
        val text = paragraph.inlines[0] as InlineText
        assertEquals(fileContent, text.value)
    }

    @Test
    fun `should resolve relative path correctly`() {
        // Arrange
        val fileContent = "Nested content"
        val fileReader = MockFileReader(mapOf("docs/nested/file.adoc" to fileContent))
        val resolver = DefaultIncludeResolver(MockParser())

        val document = AsgDocument(
            blocks = listOf(IncludeBlock(path = "nested/file.adoc", location = loc(1)))
        )

        val config = IncludeConfig(
            maxDepth = 10,
            basePath = "docs",
            fileReader = fileReader
        )

        // Act
        val result = resolver.resolve(document, config)

        // Assert
        assertEquals(0, result.errors.size, "Should have no errors")
        assertTrue(result.includedFiles.contains("docs/nested/file.adoc"), "Should resolve relative path")
    }

    @Test
    fun `should report error for non-existent file`() {
        // Arrange
        val fileReader = MockFileReader(emptyMap())
        val resolver = DefaultIncludeResolver(MockParser())

        val document = AsgDocument(
            blocks = listOf(IncludeBlock(path = "missing.adoc", location = loc(5)))
        )

        val config = IncludeConfig(
            maxDepth = 10,
            basePath = "",
            fileReader = fileReader
        )

        // Act
        val result = resolver.resolve(document, config)

        // Assert
        assertEquals(1, result.errors.size, "Should have one error")
        assertEquals(ProcessingErrorType.INCLUDE_NOT_FOUND, result.errors[0].errorType)
        assertTrue(result.errors[0].message.contains("missing.adoc"), "Error should mention the file")
        assertEquals(5, result.errors[0].location?.start?.line, "Error should have correct location")
        assertEquals(0, result.includedFiles.size, "Should not track missing file")
        assertEquals(0, result.document.blocks.size, "Should have no blocks")
    }

    @Test
    fun `should filter line range correctly`() {
        // Arrange
        val fileContent = """Line 1
Line 2
Line 3
Line 4
Line 5"""
        val fileReader = MockFileReader(mapOf("file.adoc" to fileContent))
        val resolver = DefaultIncludeResolver(MockParser())

        val document = AsgDocument(
            blocks = listOf(IncludeBlock(path = "file.adoc", lineRange = 2..4, location = loc(1)))
        )

        val config = IncludeConfig(
            maxDepth = 10,
            basePath = "",
            fileReader = fileReader
        )

        // Act
        val result = resolver.resolve(document, config)

        // Assert
        assertEquals(0, result.errors.size, "Should have no errors")
        assertEquals(3, result.document.blocks.size, "Should have 3 lines (2-4)")

        val line1 = (result.document.blocks[0] as LeafBlock).inlines[0] as InlineText
        val line2 = (result.document.blocks[1] as LeafBlock).inlines[0] as InlineText
        val line3 = (result.document.blocks[2] as LeafBlock).inlines[0] as InlineText

        assertEquals("Line 2", line1.value)
        assertEquals("Line 3", line2.value)
        assertEquals("Line 4", line3.value)
    }

    @Test
    fun `should handle nested includes`() {
        // Arrange
        val level2Content = "Level 2 content"

        // Create a parser that can handle nested includes
        val parser = object : MockParser() {
            override fun blocksFor(source: String): List<Block> {
                return if (source.contains("include::level2.adoc")) {
                    listOf(IncludeBlock(path = "level2.adoc", location = loc(1)))
                } else {
                    listOf(paragraph(source))
                }
            }
        }

        val fileReader = MockFileReader(
            mapOf(
                "level1.adoc" to "include::level2.adoc[]",
                "level2.adoc" to level2Content
            )
        )

        val resolver = DefaultIncludeResolver(parser)

        val document = AsgDocument(
            blocks = listOf(IncludeBlock(path = "level1.adoc", location = loc(1)))
        )

        val config = IncludeConfig(
            maxDepth = 10,
            basePath = "",
            fileReader = fileReader
        )

        // Act
        val result = resolver.resolve(document, config)

        // Assert
        assertEquals(0, result.errors.size, "Should have no errors")
        assertEquals(2, result.includedFiles.size, "Should have included two files")
        assertTrue(result.includedFiles.contains("level1.adoc"))
        assertTrue(result.includedFiles.contains("level2.adoc"))
    }

    @Test
    fun `should detect circular dependency`() {
        // Arrange
        val parser = object : MockParser() {
            override fun blocksFor(source: String): List<Block> {
                return when {
                    source.contains("include::file2.adoc") ->
                        listOf(IncludeBlock(path = "file2.adoc", location = loc(1)))
                    source.contains("include::file1.adoc") ->
                        listOf(IncludeBlock(path = "file1.adoc", location = loc(1)))
                    else -> listOf(paragraph(source))
                }
            }
        }

        val fileReader = MockFileReader(
            mapOf(
                "file1.adoc" to "include::file2.adoc[]",
                "file2.adoc" to "include::file1.adoc[]"
            )
        )

        val resolver = DefaultIncludeResolver(parser)

        val document = AsgDocument(
            blocks = listOf(IncludeBlock(path = "file1.adoc", location = loc(1)))
        )

        val config = IncludeConfig(
            maxDepth = 10,
            basePath = "",
            fileReader = fileReader
        )

        // Act
        val result = resolver.resolve(document, config)

        // Assert
        assertTrue(result.errors.size > 0, "Should have at least one error")
        val circularError = result.errors.find { it.errorType == ProcessingErrorType.INCLUDE_CIRCULAR_DEPENDENCY }
        assertTrue(circularError != null, "Should have circular dependency error")
    }

    @Test
    fun `should enforce max depth limit`() {
        // Arrange
        val parser = object : MockParser() {
            override fun blocksFor(source: String): List<Block> {
                return when {
                    source.contains("include::level1.adoc") ->
                        listOf(IncludeBlock(path = "level1.adoc", location = loc(1)))
                    source.contains("include::level2.adoc") ->
                        listOf(IncludeBlock(path = "level2.adoc", location = loc(1)))
                    source.contains("include::level3.adoc") ->
                        listOf(IncludeBlock(path = "level3.adoc", location = loc(1)))
                    else -> listOf(paragraph(source))
                }
            }
        }

        val fileReader = MockFileReader(
            mapOf(
                "level1.adoc" to "include::level2.adoc[]",
                "level2.adoc" to "include::level3.adoc[]",
                "level3.adoc" to "Deep content"
            )
        )

        val resolver = DefaultIncludeResolver(parser)

        val document = AsgDocument(
            blocks = listOf(IncludeBlock(path = "level1.adoc", location = loc(1)))
        )

        val config = IncludeConfig(
            maxDepth = 2,
            basePath = "",
            fileReader = fileReader
        )

        // Act
        val result = resolver.resolve(document, config)

        // Assert
        assertTrue(result.errors.size > 0, "Should have at least one error")
        val depthError = result.errors.find { it.errorType == ProcessingErrorType.INCLUDE_MAX_DEPTH_EXCEEDED }
        assertTrue(depthError != null, "Should have max depth exceeded error")
        assertTrue(depthError.message.contains("2"), "Error should mention the max depth")
    }

    @Test
    fun `should handle empty line range`() {
        // Arrange
        val fileContent = """Line 1
Line 2
Line 3"""
        val fileReader = MockFileReader(mapOf("file.adoc" to fileContent))
        val resolver = DefaultIncludeResolver(MockParser())

        val document = AsgDocument(
            blocks = listOf(
                IncludeBlock(
                    path = "file.adoc",
                    lineRange = 10..15, // Out of range
                    location = loc(1)
                )
            )
        )

        val config = IncludeConfig(
            maxDepth = 10,
            basePath = "",
            fileReader = fileReader
        )

        // Act
        val result = resolver.resolve(document, config)

        // Assert
        assertEquals(0, result.errors.size, "Should have no errors")
        // When line range is out of bounds, filterLineRange returns empty string
        // which the parser parses into a single empty paragraph
        assertTrue(result.document.blocks.size <= 1, "Should have at most one block (empty paragraph)")
        if (result.document.blocks.size == 1) {
            val paragraph = result.document.blocks[0] as LeafBlock
            val text = paragraph.inlines[0] as InlineText
            assertTrue(text.value.isEmpty(), "Content should be empty")
        }
    }

    @Test
    fun `should handle absolute paths`() {
        // Arrange
        val fileContent = "Absolute path content"
        val fileReader = MockFileReader(mapOf("/absolute/path/file.adoc" to fileContent))
        val resolver = DefaultIncludeResolver(MockParser())

        val document = AsgDocument(
            blocks = listOf(IncludeBlock(path = "/absolute/path/file.adoc", location = loc(1)))
        )

        val config = IncludeConfig(
            maxDepth = 10,
            basePath = "some/base",
            fileReader = fileReader
        )

        // Act
        val result = resolver.resolve(document, config)

        // Assert
        assertEquals(0, result.errors.size, "Should have no errors")
        assertTrue(result.includedFiles.contains("/absolute/path/file.adoc"), "Should use absolute path")
    }

    @Test
    fun `should handle multiple includes in same document`() {
        // Arrange
        val file1Content = "Content 1"
        val file2Content = "Content 2"
        val fileReader = MockFileReader(
            mapOf(
                "file1.adoc" to file1Content,
                "file2.adoc" to file2Content
            )
        )
        val resolver = DefaultIncludeResolver(MockParser())

        val document = AsgDocument(
            blocks = listOf(
                IncludeBlock(path = "file1.adoc", location = loc(1)),
                IncludeBlock(path = "file2.adoc", location = loc(2))
            )
        )

        val config = IncludeConfig(
            maxDepth = 10,
            basePath = "",
            fileReader = fileReader
        )

        // Act
        val result = resolver.resolve(document, config)

        // Assert
        assertEquals(0, result.errors.size, "Should have no errors")
        assertEquals(2, result.includedFiles.size, "Should have included two files")
        assertTrue(result.includedFiles.contains("file1.adoc"))
        assertTrue(result.includedFiles.contains("file2.adoc"))
        assertEquals(2, result.document.blocks.size, "Should have two paragraphs")
    }
}
