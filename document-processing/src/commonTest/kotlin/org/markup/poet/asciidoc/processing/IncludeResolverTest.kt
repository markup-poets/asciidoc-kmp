package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.ast.*
import org.markup.poet.asciidoc.parser.AsciidocParser
import org.markup.poet.asciidoc.parser.AsgParseResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Unit tests for IncludeResolver.
 * Tests basic include resolution, relative path resolution, file not found errors,
 * line range filtering, nested includes, circular dependency detection, and max depth enforcement.
 */
class IncludeResolverTest {
    
    // Mock FileReader for testing
    private class MockFileReader(
        private val files: Map<String, String> = emptyMap()
    ) : FileReader {
        override fun readFile(path: String): FileReadResult {
            return files[path]?.let { FileReadResult.Success(it) }
                ?: FileReadResult.Error("File not found: $path")
        }
    }
    
    // Mock parser that creates simple documents
    private class MockParser : AsciidocParser {
        override fun parseToAsg(source: String): AsgParseResult {
            val result = parse(source)
            return AsgParseResult(
                document = org.markup.poet.asciidoc.asg.AsgDocument(),
                errors = result.errors,
                warnings = result.warnings,
            )
        }

        override fun parse(input: String): org.markup.poet.asciidoc.parser.ParseResult {
            val lines = input.lines()
            val children = lines.map { line ->
                Paragraph(
                    content = listOf(Text(line, sourceLocation = SourceLocation(1))),
                    sourceLocation = SourceLocation(1)
                )
            }
            
            return org.markup.poet.asciidoc.parser.ParseResult(
                document = Document(
                    title = "Included",
                    children = children,
                    documentAttributes = emptyMap(),
                    sourceLocation = SourceLocation(0)
                ),
                errors = emptyList(),
                warnings = emptyList()
            )
        }
        
        override fun parse(lines: List<String>): org.markup.poet.asciidoc.parser.ParseResult {
            return parse(lines.joinToString("\n"))
        }
    }
    
    @Test
    fun `should resolve basic include directive`() {
        // Arrange
        val fileContent = "This is included content"
        val fileReader = MockFileReader(mapOf("included.adoc" to fileContent))
        val parser = MockParser()
        val resolver = DefaultIncludeResolver(parser)
        
        val document = Document(
            title = "Main",
            children = listOf(
                IncludeDirective(
                    path = "included.adoc",
                    sourceLocation = SourceLocation(1)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(0)
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
        assertEquals(1, result.document.children.size, "Should have one child (the included paragraph)")
        
        val paragraph = result.document.children[0] as Paragraph
        val text = paragraph.content[0] as Text
        assertEquals(fileContent, text.content)
    }
    
    @Test
    fun `should resolve relative path correctly`() {
        // Arrange
        val fileContent = "Nested content"
        val fileReader = MockFileReader(mapOf("docs/nested/file.adoc" to fileContent))
        val parser = MockParser()
        val resolver = DefaultIncludeResolver(parser)
        
        val document = Document(
            title = "Main",
            children = listOf(
                IncludeDirective(
                    path = "nested/file.adoc",
                    sourceLocation = SourceLocation(1)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(0)
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
        val parser = MockParser()
        val resolver = DefaultIncludeResolver(parser)
        
        val document = Document(
            title = "Main",
            children = listOf(
                IncludeDirective(
                    path = "missing.adoc",
                    sourceLocation = SourceLocation(5)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(0)
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
        assertEquals(5, result.errors[0].location.line, "Error should have correct location")
        assertEquals(0, result.includedFiles.size, "Should not track missing file")
        assertEquals(0, result.document.children.size, "Should have no children")
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
        val parser = MockParser()
        val resolver = DefaultIncludeResolver(parser)
        
        val document = Document(
            title = "Main",
            children = listOf(
                IncludeDirective(
                    path = "file.adoc",
                    lineRange = 2..4,
                    sourceLocation = SourceLocation(1)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(0)
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
        assertEquals(3, result.document.children.size, "Should have 3 lines (2-4)")
        
        val line1 = (result.document.children[0] as Paragraph).content[0] as Text
        val line2 = (result.document.children[1] as Paragraph).content[0] as Text
        val line3 = (result.document.children[2] as Paragraph).content[0] as Text
        
        assertEquals("Line 2", line1.content)
        assertEquals("Line 3", line2.content)
        assertEquals("Line 4", line3.content)
    }
    
    @Test
    fun `should handle nested includes`() {
        // Arrange
        val level2Content = "Level 2 content"
        val level1Content = "Level 1 content"
        
        // Create a parser that can handle nested includes
        val parser = object : AsciidocParser {
            override fun parseToAsg(source: String): AsgParseResult {
                val result = parse(source)
                return AsgParseResult(
                    document = org.markup.poet.asciidoc.asg.AsgDocument(),
                    errors = result.errors,
                    warnings = result.warnings,
                )
            }

            override fun parse(input: String): org.markup.poet.asciidoc.parser.ParseResult {
                val children = mutableListOf<BlockElement>()
                
                if (input.contains("include::level2.adoc")) {
                    children.add(
                        IncludeDirective(
                            path = "level2.adoc",
                            sourceLocation = SourceLocation(1)
                        )
                    )
                } else {
                    children.add(
                        Paragraph(
                            content = listOf(Text(input, sourceLocation = SourceLocation(1))),
                            sourceLocation = SourceLocation(1)
                        )
                    )
                }
                
                return org.markup.poet.asciidoc.parser.ParseResult(
                    document = Document(
                        title = "Included",
                        children = children,
                        documentAttributes = emptyMap(),
                        sourceLocation = SourceLocation(0)
                    ),
                    errors = emptyList(),
                    warnings = emptyList()
                )
            }
            
            override fun parse(lines: List<String>): org.markup.poet.asciidoc.parser.ParseResult {
                return parse(lines.joinToString("\n"))
            }
        }
        
        val fileReader = MockFileReader(
            mapOf(
                "level1.adoc" to "include::level2.adoc[]",
                "level2.adoc" to level2Content
            )
        )
        
        val resolver = DefaultIncludeResolver(parser)
        
        val document = Document(
            title = "Main",
            children = listOf(
                IncludeDirective(
                    path = "level1.adoc",
                    sourceLocation = SourceLocation(1)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(0)
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
        val parser = object : AsciidocParser {
            override fun parseToAsg(source: String): AsgParseResult {
                val result = parse(source)
                return AsgParseResult(
                    document = org.markup.poet.asciidoc.asg.AsgDocument(),
                    errors = result.errors,
                    warnings = result.warnings,
                )
            }

            override fun parse(input: String): org.markup.poet.asciidoc.parser.ParseResult {
                val children = when {
                    input.contains("include::file2.adoc") -> listOf(
                        IncludeDirective(
                            path = "file2.adoc",
                            sourceLocation = SourceLocation(1)
                        )
                    )
                    input.contains("include::file1.adoc") -> listOf(
                        IncludeDirective(
                            path = "file1.adoc",
                            sourceLocation = SourceLocation(1)
                        )
                    )
                    else -> listOf(
                        Paragraph(
                            content = listOf(Text(input, sourceLocation = SourceLocation(1))),
                            sourceLocation = SourceLocation(1)
                        )
                    )
                }
                
                return org.markup.poet.asciidoc.parser.ParseResult(
                    document = Document(
                        title = "Included",
                        children = children,
                        documentAttributes = emptyMap(),
                        sourceLocation = SourceLocation(0)
                    ),
                    errors = emptyList(),
                    warnings = emptyList()
                )
            }
            
            override fun parse(lines: List<String>): org.markup.poet.asciidoc.parser.ParseResult {
                return parse(lines.joinToString("\n"))
            }
        }
        
        val fileReader = MockFileReader(
            mapOf(
                "file1.adoc" to "include::file2.adoc[]",
                "file2.adoc" to "include::file1.adoc[]"
            )
        )
        
        val resolver = DefaultIncludeResolver(parser)
        
        val document = Document(
            title = "Main",
            children = listOf(
                IncludeDirective(
                    path = "file1.adoc",
                    sourceLocation = SourceLocation(1)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(0)
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
        val parser = object : AsciidocParser {
            override fun parseToAsg(source: String): AsgParseResult {
                val result = parse(source)
                return AsgParseResult(
                    document = org.markup.poet.asciidoc.asg.AsgDocument(),
                    errors = result.errors,
                    warnings = result.warnings,
                )
            }

            override fun parse(input: String): org.markup.poet.asciidoc.parser.ParseResult {
                val children = when {
                    input.contains("include::level1.adoc") -> listOf(
                        IncludeDirective(
                            path = "level1.adoc",
                            sourceLocation = SourceLocation(1)
                        )
                    )
                    input.contains("include::level2.adoc") -> listOf(
                        IncludeDirective(
                            path = "level2.adoc",
                            sourceLocation = SourceLocation(1)
                        )
                    )
                    input.contains("include::level3.adoc") -> listOf(
                        IncludeDirective(
                            path = "level3.adoc",
                            sourceLocation = SourceLocation(1)
                        )
                    )
                    else -> listOf(
                        Paragraph(
                            content = listOf(Text(input, sourceLocation = SourceLocation(1))),
                            sourceLocation = SourceLocation(1)
                        )
                    )
                }
                
                return org.markup.poet.asciidoc.parser.ParseResult(
                    document = Document(
                        title = "Included",
                        children = children,
                        documentAttributes = emptyMap(),
                        sourceLocation = SourceLocation(0)
                    ),
                    errors = emptyList(),
                    warnings = emptyList()
                )
            }
            
            override fun parse(lines: List<String>): org.markup.poet.asciidoc.parser.ParseResult {
                return parse(lines.joinToString("\n"))
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
        
        val document = Document(
            title = "Main",
            children = listOf(
                IncludeDirective(
                    path = "level1.adoc",
                    sourceLocation = SourceLocation(1)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(0)
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
        val parser = MockParser()
        val resolver = DefaultIncludeResolver(parser)
        
        val document = Document(
            title = "Main",
            children = listOf(
                IncludeDirective(
                    path = "file.adoc",
                    lineRange = 10..15, // Out of range
                    sourceLocation = SourceLocation(1)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(0)
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
        assertTrue(result.document.children.size <= 1, "Should have at most one child (empty paragraph)")
        if (result.document.children.size == 1) {
            val paragraph = result.document.children[0] as Paragraph
            val text = paragraph.content[0] as Text
            assertTrue(text.content.isEmpty(), "Content should be empty")
        }
    }
    
    @Test
    fun `should handle absolute paths`() {
        // Arrange
        val fileContent = "Absolute path content"
        val fileReader = MockFileReader(mapOf("/absolute/path/file.adoc" to fileContent))
        val parser = MockParser()
        val resolver = DefaultIncludeResolver(parser)
        
        val document = Document(
            title = "Main",
            children = listOf(
                IncludeDirective(
                    path = "/absolute/path/file.adoc",
                    sourceLocation = SourceLocation(1)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(0)
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
        val parser = MockParser()
        val resolver = DefaultIncludeResolver(parser)
        
        val document = Document(
            title = "Main",
            children = listOf(
                IncludeDirective(
                    path = "file1.adoc",
                    sourceLocation = SourceLocation(1)
                ),
                IncludeDirective(
                    path = "file2.adoc",
                    sourceLocation = SourceLocation(2)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(0)
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
        assertEquals(2, result.document.children.size, "Should have two paragraphs")
    }
}
