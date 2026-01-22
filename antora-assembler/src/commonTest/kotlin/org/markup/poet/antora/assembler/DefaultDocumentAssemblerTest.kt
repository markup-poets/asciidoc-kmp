package org.markup.poet.antora.assembler

import org.markup.poet.antora.*
import org.markup.poet.asciidoc.ast.*
import org.markup.poet.asciidoc.error.ParseError
import org.markup.poet.asciidoc.error.ParseWarning
import org.markup.poet.asciidoc.parser.AsciidocParser
import org.markup.poet.asciidoc.parser.ParseResult
import kotlin.test.*

class DefaultDocumentAssemblerTest {
    
    // Mock FileSystemAccess for testing
    private class MockFileSystemAccess(
        private val files: Map<String, String> = emptyMap()
    ) : FileSystemAccess {
        val writtenFiles = mutableMapOf<String, String>()
        
        override fun exists(path: String): Boolean = files.containsKey(path)
        
        override fun isDirectory(path: String): Boolean = false
        
        override fun readFile(path: String): FileReadResult {
            return files[path]?.let { FileReadResult.Success(it) }
                ?: FileReadResult.Error("File not found: $path")
        }
        
        override fun listDirectory(path: String): List<String> = emptyList()
        
        override fun writeFile(path: String, content: String): FileWriteResult {
            writtenFiles[path] = content
            return FileWriteResult.Success
        }
    }
    
    // Mock AntoraResolver for testing
    private class MockAntoraResolver(
        private val resolutions: Map<String, String> = emptyMap()
    ) : AntoraResolver {
        override fun resolve(coordinate: ResourceCoordinate, context: ResolutionContext): ResolutionResult {
            val key = "${coordinate.type}:${coordinate.path}"
            return resolutions[key]?.let { ResolutionResult.Success(it) }
                ?: ResolutionResult.Error("Not found: $key", ResolutionErrorType.FILE_NOT_FOUND)
        }
        
        override fun resolveInclude(path: String, context: ResolutionContext): ResolutionResult {
            return resolutions[path]?.let { ResolutionResult.Success(it) }
                ?: ResolutionResult.Error("Not found: $path", ResolutionErrorType.FILE_NOT_FOUND)
        }
    }
    
    // Mock AsciidocParser for testing
    private class MockAsciidocParser : AsciidocParser {
        override fun parse(source: String): ParseResult {
            // Simple parser that creates a document with paragraphs
            val lines = source.lines()
            val children = mutableListOf<BlockElement>()
            
            for (line in lines) {
                when {
                    line.startsWith("include::") -> {
                        // Parse include directive
                        val pathEnd = line.indexOf('[')
                        if (pathEnd > 0) {
                            val path = line.substring(9, pathEnd)
                            children.add(
                                IncludeDirective(
                                    path = path,
                                    attributes = emptyMap(),
                                    lineRange = null,
                                    sourceLocation = org.markup.poet.asciidoc.ast.SourceLocation(
                                        line = children.size + 1,
                                        column = 0
                                    )
                                )
                            )
                        }
                    }
                    line.startsWith("= ") -> {
                        // Parse heading
                        children.add(
                            Section(
                                level = 1,
                                title = line.substring(2),
                                children = emptyList(),
                                attributes = emptyMap(),
                                sourceLocation = org.markup.poet.asciidoc.ast.SourceLocation(
                                    line = children.size + 1,
                                    column = 0
                                )
                            )
                        )
                    }
                    line.isNotBlank() -> {
                        // Parse as paragraph
                        children.add(
                            Paragraph(
                                content = listOf(Text(
                                    content = line,
                                    attributes = emptyMap(),
                                    sourceLocation = org.markup.poet.asciidoc.ast.SourceLocation(
                                        line = children.size + 1,
                                        column = 0
                                    )
                                )),
                                attributes = emptyMap(),
                                sourceLocation = org.markup.poet.asciidoc.ast.SourceLocation(
                                    line = children.size + 1,
                                    column = 0
                                )
                            )
                        )
                    }
                }
            }
            
            return ParseResult(
                document = Document(
                    title = null,
                    children = children,
                    documentAttributes = emptyMap(),
                    attributes = emptyMap(),
                    sourceLocation = org.markup.poet.asciidoc.ast.SourceLocation(
                        line = 1,
                        column = 0
                    )
                ),
                errors = emptyList(),
                warnings = emptyList()
            )
        }
        
        override fun parse(lines: List<String>): ParseResult = parse(lines.joinToString("\n"))
    }
    
    @Test
    fun `should assemble simple document without includes`() {
        val fileSystem = MockFileSystemAccess(
            files = mapOf(
                "index.adoc" to "= Test Document\n\nThis is a test."
            )
        )
        
        val resolver = MockAntoraResolver()
        val parser = MockAsciidocParser()
        val assembler = DefaultDocumentAssembler(parser, resolver, fileSystem)
        
        val config = AssemblerConfig(
            indexFile = "index.adoc",
            outputFile = "output.adoc",
            componentRoot = "."
        )
        
        val result = assembler.assemble(config)
        
        assertTrue(result.success)
        assertEquals("output.adoc", result.outputPath)
        assertTrue(result.errors.isEmpty())
        assertEquals(1, result.includedFiles.size)
        assertTrue(result.includedFiles.contains("index.adoc"))
        
        // Check that output was written
        assertTrue(fileSystem.writtenFiles.containsKey("output.adoc"))
        val output = fileSystem.writtenFiles["output.adoc"]!!
        assertTrue(output.contains("Test Document"))
    }
    
    @Test
    fun `should report error when index file not found`() {
        val fileSystem = MockFileSystemAccess(files = emptyMap())
        val resolver = MockAntoraResolver()
        val parser = MockAsciidocParser()
        val assembler = DefaultDocumentAssembler(parser, resolver, fileSystem)
        
        val config = AssemblerConfig(
            indexFile = "missing.adoc",
            outputFile = "output.adoc",
            componentRoot = "."
        )
        
        val result = assembler.assemble(config)
        
        assertFalse(result.success)
        assertNull(result.outputPath)
        assertEquals(1, result.errors.size)
        assertEquals(AssemblerErrorType.INDEX_FILE_NOT_FOUND, result.errors[0].errorType)
        assertTrue(result.errors[0].message.contains("missing.adoc"))
    }
    
    @Test
    fun `should assemble document with single include`() {
        val fileSystem = MockFileSystemAccess(
            files = mapOf(
                "index.adoc" to "= Main\n\ninclude::partial\$intro.adoc[]\n\nEnd.",
                "modules/ROOT/partials/intro.adoc" to "This is the intro."
            )
        )
        
        val resolver = MockAntoraResolver(
            resolutions = mapOf(
                "partial\$intro.adoc" to "modules/ROOT/partials/intro.adoc"
            )
        )
        
        val parser = MockAsciidocParser()
        val assembler = DefaultDocumentAssembler(parser, resolver, fileSystem)
        
        val config = AssemblerConfig(
            indexFile = "index.adoc",
            outputFile = "output.adoc",
            componentRoot = "."
        )
        
        val result = assembler.assemble(config)
        
        assertTrue(result.success)
        assertEquals("output.adoc", result.outputPath)
        assertTrue(result.errors.isEmpty())
        assertTrue(result.includedFiles.size >= 1)
        
        // Check that output was written
        assertTrue(fileSystem.writtenFiles.containsKey("output.adoc"))
    }
    
    @Test
    fun `should detect circular dependency`() {
        val fileSystem = MockFileSystemAccess(
            files = mapOf(
                "index.adoc" to "include::a.adoc[]",
                "a.adoc" to "include::b.adoc[]",
                "b.adoc" to "include::a.adoc[]"
            )
        )
        
        val resolver = MockAntoraResolver(
            resolutions = mapOf(
                "a.adoc" to "a.adoc",
                "b.adoc" to "b.adoc"
            )
        )
        
        val parser = MockAsciidocParser()
        val assembler = DefaultDocumentAssembler(parser, resolver, fileSystem)
        
        val config = AssemblerConfig(
            indexFile = "index.adoc",
            outputFile = "output.adoc",
            componentRoot = ".",
            failOnCircularDependencies = true
        )
        
        val result = assembler.assemble(config)
        
        assertFalse(result.success)
        assertTrue(result.errors.any { it.errorType == AssemblerErrorType.CIRCULAR_DEPENDENCY })
    }
    
    @Test
    fun `should continue on circular dependency when configured`() {
        val fileSystem = MockFileSystemAccess(
            files = mapOf(
                "index.adoc" to "= Main\n\ninclude::a.adoc[]",
                "a.adoc" to "Content A\n\ninclude::b.adoc[]",
                "b.adoc" to "Content B\n\ninclude::a.adoc[]"
            )
        )
        
        val resolver = MockAntoraResolver(
            resolutions = mapOf(
                "a.adoc" to "a.adoc",
                "b.adoc" to "b.adoc"
            )
        )
        
        val parser = MockAsciidocParser()
        val assembler = DefaultDocumentAssembler(parser, resolver, fileSystem)
        
        val config = AssemblerConfig(
            indexFile = "index.adoc",
            outputFile = "output.adoc",
            componentRoot = ".",
            failOnCircularDependencies = false
        )
        
        val result = assembler.assemble(config)
        
        // Should succeed but report circular dependency as error
        assertTrue(result.errors.any { it.errorType == AssemblerErrorType.CIRCULAR_DEPENDENCY })
    }
    
    @Test
    fun `should collect multiple errors`() {
        val fileSystem = MockFileSystemAccess(
            files = mapOf(
                "index.adoc" to "include::missing1.adoc[]\n\ninclude::missing2.adoc[]"
            )
        )
        
        val resolver = MockAntoraResolver(resolutions = emptyMap())
        val parser = MockAsciidocParser()
        val assembler = DefaultDocumentAssembler(parser, resolver, fileSystem)
        
        val config = AssemblerConfig(
            indexFile = "index.adoc",
            outputFile = "output.adoc",
            componentRoot = ".",
            failOnMissingIncludes = false
        )
        
        val result = assembler.assemble(config)
        
        // Should collect multiple include errors
        assertTrue(result.errors.size >= 2 || result.warnings.size >= 2)
    }
    
    @Test
    fun `should write output file successfully`() {
        val fileSystem = MockFileSystemAccess(
            files = mapOf(
                "index.adoc" to "= Test\n\nContent here."
            )
        )
        
        val resolver = MockAntoraResolver()
        val parser = MockAsciidocParser()
        val assembler = DefaultDocumentAssembler(parser, resolver, fileSystem)
        
        val config = AssemblerConfig(
            indexFile = "index.adoc",
            outputFile = "output/result.adoc",
            componentRoot = "."
        )
        
        val result = assembler.assemble(config)
        
        assertTrue(result.success)
        assertEquals("output/result.adoc", result.outputPath)
        assertTrue(fileSystem.writtenFiles.containsKey("output/result.adoc"))
        
        val output = fileSystem.writtenFiles["output/result.adoc"]!!
        assertTrue(output.isNotEmpty())
    }
    
    @Test
    fun `should build dependency graph correctly`() {
        val fileSystem = MockFileSystemAccess(
            files = mapOf(
                "index.adoc" to "include::a.adoc[]\n\ninclude::b.adoc[]",
                "a.adoc" to "Content A",
                "b.adoc" to "Content B\n\ninclude::c.adoc[]",
                "c.adoc" to "Content C"
            )
        )
        
        val resolver = MockAntoraResolver(
            resolutions = mapOf(
                "a.adoc" to "a.adoc",
                "b.adoc" to "b.adoc",
                "c.adoc" to "c.adoc"
            )
        )
        
        val parser = MockAsciidocParser()
        val assembler = DefaultDocumentAssembler(parser, resolver, fileSystem)
        
        val config = AssemblerConfig(
            indexFile = "index.adoc",
            outputFile = "output.adoc",
            componentRoot = "."
        )
        
        val result = assembler.assemble(config)
        
        assertTrue(result.success)
        // Should include index.adoc, a.adoc, b.adoc, and c.adoc
        assertTrue(result.includedFiles.size >= 3)
    }
    
    @Test
    fun `should handle parse errors in index file`() {
        // Create a parser that returns errors
        val parserWithErrors = object : AsciidocParser {
            override fun parse(source: String): ParseResult {
                return ParseResult(
                    document = Document(
                        title = null,
                        children = emptyList(),
                        documentAttributes = emptyMap(),
                        attributes = emptyMap(),
                        sourceLocation = org.markup.poet.asciidoc.ast.SourceLocation(1, 0)
                    ),
                    errors = listOf(
                        ParseError(
                            message = "Invalid syntax",
                            location = org.markup.poet.asciidoc.ast.SourceLocation(1, 0),
                            severity = org.markup.poet.asciidoc.error.ErrorSeverity.ERROR
                        )
                    ),
                    warnings = emptyList()
                )
            }
            
            override fun parse(lines: List<String>): ParseResult = parse(lines.joinToString("\n"))
        }
        
        val fileSystem = MockFileSystemAccess(
            files = mapOf("index.adoc" to "invalid content")
        )
        
        val resolver = MockAntoraResolver()
        val assembler = DefaultDocumentAssembler(parserWithErrors, resolver, fileSystem)
        
        val config = AssemblerConfig(
            indexFile = "index.adoc",
            outputFile = "output.adoc",
            componentRoot = "."
        )
        
        val result = assembler.assemble(config)
        
        assertFalse(result.success)
        assertTrue(result.errors.any { it.errorType == AssemblerErrorType.PARSE_ERROR })
    }
}
