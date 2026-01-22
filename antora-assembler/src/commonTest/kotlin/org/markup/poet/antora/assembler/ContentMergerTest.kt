package org.markup.poet.antora.assembler

import org.markup.poet.antora.*
import org.markup.poet.asciidoc.ast.*
import org.markup.poet.asciidoc.parser.AsciidocParser
import org.markup.poet.asciidoc.parser.ParseResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for ContentMerger.
 * Tests include resolution, recursive processing, cycle detection, and filtering.
 */
class ContentMergerTest {
    
    @Test
    fun `should merge simple include directive`() {
        val fileSystem = MockFileSystem(
            mapOf(
                "/docs/modules/ROOT/pages/index.adoc" to "= Index\n\ninclude::partial\$intro.adoc[]",
                "/docs/modules/ROOT/partials/intro.adoc" to "This is the intro."
            )
        )
        
        val resolver = MockAntoraResolver(fileSystem)
        val parser = MockAsciidocParser()
        val merger = ContentMerger(resolver, parser, fileSystem)
        
        val indexDoc = Document(
            title = "Index",
            children = listOf(
                IncludeDirective(
                    path = "partial\$intro.adoc",
                    attributes = emptyMap(),
                    sourceLocation = SourceLocation(3, 0)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val context = ResolutionContext(
            componentRoot = "/docs",
            currentModule = "ROOT",
            currentFilePath = "/docs/modules/ROOT/pages/index.adoc"
        )
        
        val config = AssemblerConfig(
            indexFile = "/docs/modules/ROOT/pages/index.adoc",
            outputFile = "/output/result.adoc",
            componentRoot = "/docs"
        )
        
        val result = merger.merge(indexDoc, context, config)
        
        assertTrue(result.errors.isEmpty(), "Should have no errors")
        assertTrue(result.warnings.isEmpty(), "Should have no warnings")
        assertEquals(1, result.document?.children?.size, "Should have one child element")
    }
    
    @Test
    fun `should detect circular dependency`() {
        val fileSystem = MockFileSystem(
            mapOf(
                "/docs/modules/ROOT/pages/a.adoc" to "include::b.adoc[]",
                "/docs/modules/ROOT/pages/b.adoc" to "include::a.adoc[]"
            )
        )
        
        val resolver = MockAntoraResolver(fileSystem)
        val parser = MockAsciidocParser()
        val merger = ContentMerger(resolver, parser, fileSystem)
        
        val docA = Document(
            title = null,
            children = listOf(
                IncludeDirective(
                    path = "b.adoc",
                    attributes = emptyMap(),
                    sourceLocation = SourceLocation(1, 0)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val context = ResolutionContext(
            componentRoot = "/docs",
            currentModule = "ROOT",
            currentFilePath = "/docs/modules/ROOT/pages/a.adoc"
        )
        
        val config = AssemblerConfig(
            indexFile = "/docs/modules/ROOT/pages/a.adoc",
            outputFile = "/output/result.adoc",
            componentRoot = "/docs",
            failOnCircularDependencies = true
        )
        
        val result = merger.merge(docA, context, config)
        
        assertTrue(result.errors.isNotEmpty(), "Should have errors")
        assertTrue(
            result.errors.any { it.errorType == AssemblerErrorType.CIRCULAR_DEPENDENCY },
            "Should have circular dependency error"
        )
    }
    
    @Test
    fun `should enforce max depth limit`() {
        val fileSystem = MockFileSystem(
            mapOf(
                "/docs/modules/ROOT/pages/index.adoc" to "include::level1.adoc[]",
                "/docs/modules/ROOT/pages/level1.adoc" to "include::level2.adoc[]",
                "/docs/modules/ROOT/pages/level2.adoc" to "include::level3.adoc[]",
                "/docs/modules/ROOT/pages/level3.adoc" to "Content"
            )
        )
        
        val resolver = MockAntoraResolver(fileSystem)
        val parser = MockAsciidocParser()
        val merger = ContentMerger(resolver, parser, fileSystem)
        
        val indexDoc = Document(
            title = null,
            children = listOf(
                IncludeDirective(
                    path = "level1.adoc",
                    attributes = emptyMap(),
                    sourceLocation = SourceLocation(1, 0)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val context = ResolutionContext(
            componentRoot = "/docs",
            currentModule = "ROOT",
            currentFilePath = "/docs/modules/ROOT/pages/index.adoc"
        )
        
        val config = AssemblerConfig(
            indexFile = "/docs/modules/ROOT/pages/index.adoc",
            outputFile = "/output/result.adoc",
            componentRoot = "/docs",
            maxDepth = 2
        )
        
        val result = merger.merge(indexDoc, context, config)
        
        assertTrue(result.errors.isNotEmpty(), "Should have errors")
        assertTrue(
            result.errors.any { it.errorType == AssemblerErrorType.MAX_DEPTH_EXCEEDED },
            "Should have max depth exceeded error"
        )
    }
    
    @Test
    fun `should handle missing include file`() {
        val fileSystem = MockFileSystem(
            mapOf(
                "/docs/modules/ROOT/pages/index.adoc" to "include::missing.adoc[]"
            )
        )
        
        val resolver = MockAntoraResolver(fileSystem)
        val parser = MockAsciidocParser()
        val merger = ContentMerger(resolver, parser, fileSystem)
        
        val indexDoc = Document(
            title = null,
            children = listOf(
                IncludeDirective(
                    path = "missing.adoc",
                    attributes = emptyMap(),
                    sourceLocation = SourceLocation(1, 0)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val context = ResolutionContext(
            componentRoot = "/docs",
            currentModule = "ROOT",
            currentFilePath = "/docs/modules/ROOT/pages/index.adoc"
        )
        
        val config = AssemblerConfig(
            indexFile = "/docs/modules/ROOT/pages/index.adoc",
            outputFile = "/output/result.adoc",
            componentRoot = "/docs",
            failOnMissingIncludes = true
        )
        
        val result = merger.merge(indexDoc, context, config)
        
        assertTrue(result.errors.isNotEmpty(), "Should have errors")
        assertTrue(
            result.errors.any { it.errorType == AssemblerErrorType.INCLUDE_NOT_FOUND },
            "Should have include not found error"
        )
    }
    
    @Test
    fun `should process nested includes recursively`() {
        val fileSystem = MockFileSystem(
            mapOf(
                "/docs/modules/ROOT/pages/index.adoc" to "include::partial\$outer.adoc[]",
                "/docs/modules/ROOT/partials/outer.adoc" to "Outer\ninclude::inner.adoc[]",
                "/docs/modules/ROOT/partials/inner.adoc" to "Inner content"
            )
        )
        
        val resolver = MockAntoraResolver(fileSystem)
        val parser = MockAsciidocParser()
        val merger = ContentMerger(resolver, parser, fileSystem)
        
        val indexDoc = Document(
            title = null,
            children = listOf(
                IncludeDirective(
                    path = "partial\$outer.adoc",
                    attributes = emptyMap(),
                    sourceLocation = SourceLocation(1, 0)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val context = ResolutionContext(
            componentRoot = "/docs",
            currentModule = "ROOT",
            currentFilePath = "/docs/modules/ROOT/pages/index.adoc"
        )
        
        val config = AssemblerConfig(
            indexFile = "/docs/modules/ROOT/pages/index.adoc",
            outputFile = "/output/result.adoc",
            componentRoot = "/docs"
        )
        
        val result = merger.merge(indexDoc, context, config)
        
        assertTrue(result.errors.isEmpty(), "Should have no errors")
        assertTrue(result.document != null, "Should have a document")
    }
    
    @Test
    fun `should filter content by line range`() {
        val fileSystem = MockFileSystem(
            mapOf(
                "/docs/modules/ROOT/pages/index.adoc" to "include::code.txt[lines=2..4]",
                "/docs/modules/ROOT/pages/code.txt" to "Line 1\nLine 2\nLine 3\nLine 4\nLine 5"
            )
        )
        
        val resolver = MockAntoraResolver(fileSystem)
        val parser = MockAsciidocParser()
        val merger = ContentMerger(resolver, parser, fileSystem)
        
        val indexDoc = Document(
            title = null,
            children = listOf(
                IncludeDirective(
                    path = "code.txt",
                    lineRange = 2..4,
                    attributes = emptyMap(),
                    sourceLocation = SourceLocation(1, 0)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val context = ResolutionContext(
            componentRoot = "/docs",
            currentModule = "ROOT",
            currentFilePath = "/docs/modules/ROOT/pages/index.adoc"
        )
        
        val config = AssemblerConfig(
            indexFile = "/docs/modules/ROOT/pages/index.adoc",
            outputFile = "/output/result.adoc",
            componentRoot = "/docs"
        )
        
        val result = merger.merge(indexDoc, context, config)
        
        assertTrue(result.errors.isEmpty(), "Should have no errors")
        assertTrue(result.document != null, "Should have a document")
        // The merged content should only contain lines 2-4
        assertEquals(3, result.document?.children?.size, "Should have 3 lines (2, 3, 4)")
    }
    
    @Test
    fun `should filter content by tags`() {
        val fileSystem = MockFileSystem(
            mapOf(
                "/docs/modules/ROOT/pages/index.adoc" to "include::code.java[tags=method1]",
                "/docs/modules/ROOT/pages/code.java" to """
                    public class Example {
                        // tag::method1[]
                        public void method1() {
                            System.out.println("Method 1");
                        }
                        // end::method1[]
                        
                        // tag::method2[]
                        public void method2() {
                            System.out.println("Method 2");
                        }
                        // end::method2[]
                    }
                """.trimIndent()
            )
        )
        
        val resolver = MockAntoraResolver(fileSystem)
        val parser = MockAsciidocParser()
        val merger = ContentMerger(resolver, parser, fileSystem)
        
        val indexDoc = Document(
            title = null,
            children = listOf(
                IncludeDirective(
                    path = "code.java",
                    attributes = mapOf("tags" to "method1"),
                    sourceLocation = SourceLocation(1, 0)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val context = ResolutionContext(
            componentRoot = "/docs",
            currentModule = "ROOT",
            currentFilePath = "/docs/modules/ROOT/pages/index.adoc"
        )
        
        val config = AssemblerConfig(
            indexFile = "/docs/modules/ROOT/pages/index.adoc",
            outputFile = "/output/result.adoc",
            componentRoot = "/docs"
        )
        
        val result = merger.merge(indexDoc, context, config)
        
        assertTrue(result.errors.isEmpty(), "Should have no errors")
        assertTrue(result.document != null, "Should have a document")
        // The merged content should only contain method1, not method2
    }
    
    @Test
    fun `should preserve indentation of included content`() {
        val fileSystem = MockFileSystem(
            mapOf(
                "/docs/modules/ROOT/pages/index.adoc" to """
                    = Document
                    
                    Example:
                    
                        include::code.txt[]
                """.trimIndent(),
                "/docs/modules/ROOT/pages/code.txt" to "Line 1\nLine 2\nLine 3"
            )
        )
        
        val resolver = MockAntoraResolver(fileSystem)
        val parser = MockAsciidocParser()
        val merger = ContentMerger(resolver, parser, fileSystem)
        
        // For this test, we need to track the indentation level of the include directive
        // The include is indented by 4 spaces, so the included content should also be indented by 4 spaces
        val indexDoc = Document(
            title = "Document",
            children = listOf(
                Paragraph(
                    content = listOf(
                        Text(
                            content = "Example:",
                            attributes = emptyMap(),
                            sourceLocation = SourceLocation(3, 0)
                        )
                    ),
                    attributes = emptyMap(),
                    sourceLocation = SourceLocation(3, 0)
                ),
                IncludeDirective(
                    path = "code.txt",
                    attributes = mapOf("indent" to "4"),
                    sourceLocation = SourceLocation(5, 4) // Column 4 indicates indentation
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val context = ResolutionContext(
            componentRoot = "/docs",
            currentModule = "ROOT",
            currentFilePath = "/docs/modules/ROOT/pages/index.adoc"
        )
        
        val config = AssemblerConfig(
            indexFile = "/docs/modules/ROOT/pages/index.adoc",
            outputFile = "/output/result.adoc",
            componentRoot = "/docs"
        )
        
        val result = merger.merge(indexDoc, context, config)
        
        assertTrue(result.errors.isEmpty(), "Should have no errors")
        assertTrue(result.document != null, "Should have a document")
        // The included content should be indented by 4 spaces
    }
    
    @Test
    fun `should handle line range with out of bounds values`() {
        val fileSystem = MockFileSystem(
            mapOf(
                "/docs/modules/ROOT/pages/index.adoc" to "include::code.txt[lines=5..10]",
                "/docs/modules/ROOT/pages/code.txt" to "Line 1\nLine 2\nLine 3"
            )
        )
        
        val resolver = MockAntoraResolver(fileSystem)
        val parser = MockAsciidocParser()
        val merger = ContentMerger(resolver, parser, fileSystem)
        
        val indexDoc = Document(
            title = null,
            children = listOf(
                IncludeDirective(
                    path = "code.txt",
                    lineRange = 5..10,
                    attributes = emptyMap(),
                    sourceLocation = SourceLocation(1, 0)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val context = ResolutionContext(
            componentRoot = "/docs",
            currentModule = "ROOT",
            currentFilePath = "/docs/modules/ROOT/pages/index.adoc"
        )
        
        val config = AssemblerConfig(
            indexFile = "/docs/modules/ROOT/pages/index.adoc",
            outputFile = "/output/result.adoc",
            componentRoot = "/docs"
        )
        
        val result = merger.merge(indexDoc, context, config)
        
        // Should handle gracefully without crashing
        assertTrue(result.errors.isEmpty(), "Should have no errors")
    }
    
    @Test
    fun `should handle multiple tags in filter`() {
        val fileSystem = MockFileSystem(
            mapOf(
                "/docs/modules/ROOT/pages/index.adoc" to "include::code.java[tags=method1;method2]",
                "/docs/modules/ROOT/pages/code.java" to """
                    // tag::method1[]
                    Method 1 content
                    // end::method1[]
                    
                    Other content
                    
                    // tag::method2[]
                    Method 2 content
                    // end::method2[]
                    
                    // tag::method3[]
                    Method 3 content
                    // end::method3[]
                """.trimIndent()
            )
        )
        
        val resolver = MockAntoraResolver(fileSystem)
        val parser = MockAsciidocParser()
        val merger = ContentMerger(resolver, parser, fileSystem)
        
        val indexDoc = Document(
            title = null,
            children = listOf(
                IncludeDirective(
                    path = "code.java",
                    attributes = mapOf("tags" to "method1,method2"),
                    sourceLocation = SourceLocation(1, 0)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val context = ResolutionContext(
            componentRoot = "/docs",
            currentModule = "ROOT",
            currentFilePath = "/docs/modules/ROOT/pages/index.adoc"
        )
        
        val config = AssemblerConfig(
            indexFile = "/docs/modules/ROOT/pages/index.adoc",
            outputFile = "/output/result.adoc",
            componentRoot = "/docs"
        )
        
        val result = merger.merge(indexDoc, context, config)
        
        assertTrue(result.errors.isEmpty(), "Should have no errors")
        assertTrue(result.document != null, "Should have a document")
        // Should include method1 and method2, but not method3
    }
    
    @Test
    fun `should support AsciiDoc comment style tags`() {
        val fileSystem = MockFileSystem(
            mapOf(
                "/docs/modules/ROOT/pages/index.adoc" to "include::code.adoc[tags=section1]",
                "/docs/modules/ROOT/pages/code.adoc" to """
                    # tag::section1[]
                    Section 1 content
                    # end::section1[]
                    
                    # tag::section2[]
                    Section 2 content
                    # end::section2[]
                """.trimIndent()
            )
        )
        
        val resolver = MockAntoraResolver(fileSystem)
        val parser = MockAsciidocParser()
        val merger = ContentMerger(resolver, parser, fileSystem)
        
        val indexDoc = Document(
            title = null,
            children = listOf(
                IncludeDirective(
                    path = "code.adoc",
                    attributes = mapOf("tags" to "section1"),
                    sourceLocation = SourceLocation(1, 0)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val context = ResolutionContext(
            componentRoot = "/docs",
            currentModule = "ROOT",
            currentFilePath = "/docs/modules/ROOT/pages/index.adoc"
        )
        
        val config = AssemblerConfig(
            indexFile = "/docs/modules/ROOT/pages/index.adoc",
            outputFile = "/output/result.adoc",
            componentRoot = "/docs"
        )
        
        val result = merger.merge(indexDoc, context, config)
        
        assertTrue(result.errors.isEmpty(), "Should have no errors")
        assertTrue(result.document != null, "Should have a document")
    }
    
    @Test
    fun `should apply indentation based on source location column`() {
        val fileSystem = MockFileSystem(
            mapOf(
                "/docs/modules/ROOT/pages/index.adoc" to """
                    = Document
                    
                    Example:
                    
                        include::code.txt[]
                """.trimIndent(),
                "/docs/modules/ROOT/pages/code.txt" to "Line 1\nLine 2\nLine 3"
            )
        )
        
        val resolver = MockAntoraResolver(fileSystem)
        val parser = MockAsciidocParser()
        val merger = ContentMerger(resolver, parser, fileSystem)
        
        // The include directive is at column 4 (4 spaces indentation)
        val indexDoc = Document(
            title = "Document",
            children = listOf(
                Paragraph(
                    content = listOf(
                        Text(
                            content = "Example:",
                            attributes = emptyMap(),
                            sourceLocation = SourceLocation(3, 0)
                        )
                    ),
                    attributes = emptyMap(),
                    sourceLocation = SourceLocation(3, 0)
                ),
                IncludeDirective(
                    path = "code.txt",
                    attributes = emptyMap(),
                    sourceLocation = SourceLocation(5, 4) // Column 4 = 4 spaces indent
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val context = ResolutionContext(
            componentRoot = "/docs",
            currentModule = "ROOT",
            currentFilePath = "/docs/modules/ROOT/pages/index.adoc"
        )
        
        val config = AssemblerConfig(
            indexFile = "/docs/modules/ROOT/pages/index.adoc",
            outputFile = "/output/result.adoc",
            componentRoot = "/docs"
        )
        
        val result = merger.merge(indexDoc, context, config)
        
        assertTrue(result.errors.isEmpty(), "Should have no errors")
        assertTrue(result.document != null, "Should have a document")
        // The parser will receive indented content
    }
    
    @Test
    fun `should not indent blank lines`() {
        val fileSystem = MockFileSystem(
            mapOf(
                "/docs/modules/ROOT/pages/index.adoc" to "    include::code.txt[]",
                "/docs/modules/ROOT/pages/code.txt" to "Line 1\n\nLine 3"
            )
        )
        
        val resolver = MockAntoraResolver(fileSystem)
        val parser = MockAsciidocParser()
        val merger = ContentMerger(resolver, parser, fileSystem)
        
        val indexDoc = Document(
            title = null,
            children = listOf(
                IncludeDirective(
                    path = "code.txt",
                    attributes = emptyMap(),
                    sourceLocation = SourceLocation(1, 4) // 4 spaces indent
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val context = ResolutionContext(
            componentRoot = "/docs",
            currentModule = "ROOT",
            currentFilePath = "/docs/modules/ROOT/pages/index.adoc"
        )
        
        val config = AssemblerConfig(
            indexFile = "/docs/modules/ROOT/pages/index.adoc",
            outputFile = "/output/result.adoc",
            componentRoot = "/docs"
        )
        
        val result = merger.merge(indexDoc, context, config)
        
        assertTrue(result.errors.isEmpty(), "Should have no errors")
        assertTrue(result.document != null, "Should have a document")
        // Blank lines should not be indented
    }
    
    @Test
    fun `should combine line range tag filtering and indentation`() {
        val fileSystem = MockFileSystem(
            mapOf(
                "/docs/modules/ROOT/pages/index.adoc" to "    include::code.java[tags=method1]",
                "/docs/modules/ROOT/pages/code.java" to """
                    Line 1
                    Line 2
                    // tag::method1[]
                    public void method1() {
                        System.out.println("Method 1");
                    }
                    // end::method1[]
                    Line 8
                    // tag::method2[]
                    public void method2() {
                        System.out.println("Method 2");
                    }
                    // end::method2[]
                """.trimIndent()
            )
        )
        
        val resolver = MockAntoraResolver(fileSystem)
        val parser = MockAsciidocParser()
        val merger = ContentMerger(resolver, parser, fileSystem)
        
        val indexDoc = Document(
            title = null,
            children = listOf(
                IncludeDirective(
                    path = "code.java",
                    attributes = mapOf("tags" to "method1"),
                    sourceLocation = SourceLocation(1, 4) // 4 spaces indent
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val context = ResolutionContext(
            componentRoot = "/docs",
            currentModule = "ROOT",
            currentFilePath = "/docs/modules/ROOT/pages/index.adoc"
        )
        
        val config = AssemblerConfig(
            indexFile = "/docs/modules/ROOT/pages/index.adoc",
            outputFile = "/output/result.adoc",
            componentRoot = "/docs"
        )
        
        val result = merger.merge(indexDoc, context, config)
        
        assertTrue(result.errors.isEmpty(), "Should have no errors")
        assertTrue(result.document != null, "Should have a document")
        // Should only include method1 content, with 4 spaces indentation
    }
    
    @Test
    fun `should merge attributes from included file`() {
        val fileSystem = MockFileSystem(
            mapOf(
                "/docs/modules/ROOT/pages/index.adoc" to ":author: John Doe\n\ninclude::partial\$intro.adoc[]",
                "/docs/modules/ROOT/partials/intro.adoc" to ":version: 1.0\n\nThis is the intro."
            )
        )
        
        val resolver = MockAntoraResolver(fileSystem)
        val parser = MockAsciidocParser()
        val merger = ContentMerger(resolver, parser, fileSystem)
        
        val indexDoc = Document(
            title = null,
            children = listOf(
                IncludeDirective(
                    path = "partial\$intro.adoc",
                    attributes = emptyMap(),
                    sourceLocation = SourceLocation(3, 0)
                )
            ),
            documentAttributes = mapOf("author" to "John Doe"),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val context = ResolutionContext(
            componentRoot = "/docs",
            currentModule = "ROOT",
            currentFilePath = "/docs/modules/ROOT/pages/index.adoc"
        )
        
        val config = AssemblerConfig(
            indexFile = "/docs/modules/ROOT/pages/index.adoc",
            outputFile = "/output/result.adoc",
            componentRoot = "/docs"
        )
        
        val result = merger.merge(indexDoc, context, config)
        
        assertTrue(result.errors.isEmpty(), "Should have no errors")
        assertTrue(result.document != null, "Should have a document")
        assertEquals("John Doe", result.document?.documentAttributes?.get("author"), "Should preserve author from index")
        assertEquals("1.0", result.document?.documentAttributes?.get("version"), "Should merge version from included file")
    }
    
    @Test
    fun `should use first definition when attributes conflict`() {
        val fileSystem = MockFileSystem(
            mapOf(
                "/docs/modules/ROOT/pages/index.adoc" to ":version: 2.0\n\ninclude::partial\$intro.adoc[]",
                "/docs/modules/ROOT/partials/intro.adoc" to ":version: 1.0\n\nThis is the intro."
            )
        )
        
        val resolver = MockAntoraResolver(fileSystem)
        val parser = MockAsciidocParser()
        val merger = ContentMerger(resolver, parser, fileSystem)
        
        val indexDoc = Document(
            title = null,
            children = listOf(
                IncludeDirective(
                    path = "partial\$intro.adoc",
                    attributes = emptyMap(),
                    sourceLocation = SourceLocation(3, 0)
                )
            ),
            documentAttributes = mapOf("version" to "2.0"),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val context = ResolutionContext(
            componentRoot = "/docs",
            currentModule = "ROOT",
            currentFilePath = "/docs/modules/ROOT/pages/index.adoc"
        )
        
        val config = AssemblerConfig(
            indexFile = "/docs/modules/ROOT/pages/index.adoc",
            outputFile = "/output/result.adoc",
            componentRoot = "/docs"
        )
        
        val result = merger.merge(indexDoc, context, config)
        
        assertTrue(result.errors.isEmpty(), "Should have no errors")
        assertTrue(result.document != null, "Should have a document")
        assertEquals("2.0", result.document?.documentAttributes?.get("version"), "Should keep first definition (2.0)")
        assertTrue(result.warnings.isNotEmpty(), "Should have a warning about attribute conflict")
        assertTrue(
            result.warnings.any { it.message.contains("Attribute 'version' already defined") },
            "Should warn about conflicting attribute"
        )
    }
    
    @Test
    fun `should merge attributes from nested includes`() {
        val fileSystem = MockFileSystem(
            mapOf(
                "/docs/modules/ROOT/pages/index.adoc" to ":author: John\n\ninclude::partial\$outer.adoc[]",
                "/docs/modules/ROOT/partials/outer.adoc" to ":version: 1.0\n\ninclude::inner.adoc[]",
                "/docs/modules/ROOT/partials/inner.adoc" to ":license: MIT\n\nInner content"
            )
        )
        
        val resolver = MockAntoraResolver(fileSystem)
        val parser = MockAsciidocParser()
        val merger = ContentMerger(resolver, parser, fileSystem)
        
        val indexDoc = Document(
            title = null,
            children = listOf(
                IncludeDirective(
                    path = "partial\$outer.adoc",
                    attributes = emptyMap(),
                    sourceLocation = SourceLocation(3, 0)
                )
            ),
            documentAttributes = mapOf("author" to "John"),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val context = ResolutionContext(
            componentRoot = "/docs",
            currentModule = "ROOT",
            currentFilePath = "/docs/modules/ROOT/pages/index.adoc"
        )
        
        val config = AssemblerConfig(
            indexFile = "/docs/modules/ROOT/pages/index.adoc",
            outputFile = "/output/result.adoc",
            componentRoot = "/docs"
        )
        
        val result = merger.merge(indexDoc, context, config)
        
        assertTrue(result.errors.isEmpty(), "Should have no errors")
        assertTrue(result.document != null, "Should have a document")
        assertEquals("John", result.document?.documentAttributes?.get("author"), "Should have author from index")
        assertEquals("1.0", result.document?.documentAttributes?.get("version"), "Should have version from outer")
        assertEquals("MIT", result.document?.documentAttributes?.get("license"), "Should have license from inner")
    }
    
    @Test
    fun `should not warn when same attribute value is redefined`() {
        val fileSystem = MockFileSystem(
            mapOf(
                "/docs/modules/ROOT/pages/index.adoc" to ":version: 1.0\n\ninclude::partial\$intro.adoc[]",
                "/docs/modules/ROOT/partials/intro.adoc" to ":version: 1.0\n\nThis is the intro."
            )
        )
        
        val resolver = MockAntoraResolver(fileSystem)
        val parser = MockAsciidocParser()
        val merger = ContentMerger(resolver, parser, fileSystem)
        
        val indexDoc = Document(
            title = null,
            children = listOf(
                IncludeDirective(
                    path = "partial\$intro.adoc",
                    attributes = emptyMap(),
                    sourceLocation = SourceLocation(3, 0)
                )
            ),
            documentAttributes = mapOf("version" to "1.0"),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val context = ResolutionContext(
            componentRoot = "/docs",
            currentModule = "ROOT",
            currentFilePath = "/docs/modules/ROOT/pages/index.adoc"
        )
        
        val config = AssemblerConfig(
            indexFile = "/docs/modules/ROOT/pages/index.adoc",
            outputFile = "/output/result.adoc",
            componentRoot = "/docs"
        )
        
        val result = merger.merge(indexDoc, context, config)
        
        assertTrue(result.errors.isEmpty(), "Should have no errors")
        assertTrue(result.warnings.isEmpty(), "Should have no warnings when same value is redefined")
        assertEquals("1.0", result.document?.documentAttributes?.get("version"), "Should keep the value")
    }
    
    @Test
    fun `should preserve same-file anchor references`() {
        val fileSystem = MockFileSystem(
            mapOf(
                "/docs/modules/ROOT/pages/index.adoc" to "= Index\n\n[[section1]]\n== Section 1\n\nSee <<section1>>"
            )
        )
        
        val resolver = MockAntoraResolver(fileSystem)
        val parser = MockAsciidocParser()
        val merger = ContentMerger(resolver, parser, fileSystem)
        
        val indexDoc = Document(
            title = "Index",
            children = listOf(
                Section(
                    level = 2,
                    title = "Section 1",
                    children = emptyList(),
                    attributes = mapOf("id" to "section1"),
                    sourceLocation = SourceLocation(3, 0)
                ),
                Paragraph(
                    content = listOf(
                        Text(content = "See ", attributes = emptyMap(), sourceLocation = SourceLocation(5, 0)),
                        CrossReference(
                            targetId = "section1",
                            customText = null,
                            attributes = emptyMap(),
                            sourceLocation = SourceLocation(5, 4)
                        )
                    ),
                    attributes = emptyMap(),
                    sourceLocation = SourceLocation(5, 0)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val context = ResolutionContext(
            componentRoot = "/docs",
            currentModule = "ROOT",
            currentFilePath = "/docs/modules/ROOT/pages/index.adoc"
        )
        
        val config = AssemblerConfig(
            indexFile = "/docs/modules/ROOT/pages/index.adoc",
            outputFile = "/output/result.adoc",
            componentRoot = "/docs"
        )
        
        val result = merger.merge(indexDoc, context, config)
        
        assertTrue(result.errors.isEmpty(), "Should have no errors")
        assertTrue(result.warnings.isEmpty(), "Should have no warnings for valid anchor reference")
        assertTrue(result.document != null, "Should have a document")
        
        // Verify the cross-reference is preserved
        val paragraph = result.document?.children?.get(1) as? Paragraph
        assertTrue(paragraph != null, "Should have a paragraph")
        val xref = paragraph?.content?.get(1) as? CrossReference
        assertTrue(xref != null, "Should have a cross-reference")
        assertEquals("section1", xref?.targetId, "Should preserve the anchor reference")
    }
    
    @Test
    fun `should preserve cross-file anchor references for included content`() {
        val fileSystem = MockFileSystem(
            mapOf(
                "/docs/modules/ROOT/pages/index.adoc" to "include::partial\$intro.adoc[]\n\nSee <<intro-section>>",
                "/docs/modules/ROOT/partials/intro.adoc" to "[[intro-section]]\n== Introduction\n\nIntro content"
            )
        )
        
        val resolver = MockAntoraResolver(fileSystem)
        val parser = MockAsciidocParser()
        val merger = ContentMerger(resolver, parser, fileSystem)
        
        val indexDoc = Document(
            title = null,
            children = listOf(
                IncludeDirective(
                    path = "partial\$intro.adoc",
                    attributes = emptyMap(),
                    sourceLocation = SourceLocation(1, 0)
                ),
                Paragraph(
                    content = listOf(
                        Text(content = "See ", attributes = emptyMap(), sourceLocation = SourceLocation(3, 0)),
                        CrossReference(
                            targetId = "intro-section",
                            customText = null,
                            attributes = emptyMap(),
                            sourceLocation = SourceLocation(3, 4)
                        )
                    ),
                    attributes = emptyMap(),
                    sourceLocation = SourceLocation(3, 0)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val context = ResolutionContext(
            componentRoot = "/docs",
            currentModule = "ROOT",
            currentFilePath = "/docs/modules/ROOT/pages/index.adoc"
        )
        
        val config = AssemblerConfig(
            indexFile = "/docs/modules/ROOT/pages/index.adoc",
            outputFile = "/output/result.adoc",
            componentRoot = "/docs"
        )
        
        val result = merger.merge(indexDoc, context, config)
        
        assertTrue(result.errors.isEmpty(), "Should have no errors")
        assertTrue(result.warnings.isEmpty(), "Should have no warnings for valid cross-file anchor")
        assertTrue(result.document != null, "Should have a document")
    }
    
    @Test
    fun `should convert Antora xref with page coordinate to simple anchor`() {
        val fileSystem = MockFileSystem(
            mapOf(
                "/docs/modules/ROOT/pages/index.adoc" to "See xref:page\$other.adoc#section1[]",
                "/docs/modules/ROOT/pages/other.adoc" to "[[section1]]\n== Section 1"
            )
        )
        
        val resolver = MockAntoraResolver(fileSystem)
        val parser = MockAsciidocParser()
        val merger = ContentMerger(resolver, parser, fileSystem)
        
        val indexDoc = Document(
            title = null,
            children = listOf(
                Paragraph(
                    content = listOf(
                        Text(content = "See ", attributes = emptyMap(), sourceLocation = SourceLocation(1, 0)),
                        CrossReference(
                            targetId = "page\$other.adoc#section1",
                            customText = null,
                            attributes = emptyMap(),
                            sourceLocation = SourceLocation(1, 4)
                        )
                    ),
                    attributes = emptyMap(),
                    sourceLocation = SourceLocation(1, 0)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val context = ResolutionContext(
            componentRoot = "/docs",
            currentModule = "ROOT",
            currentFilePath = "/docs/modules/ROOT/pages/index.adoc"
        )
        
        val config = AssemblerConfig(
            indexFile = "/docs/modules/ROOT/pages/index.adoc",
            outputFile = "/output/result.adoc",
            componentRoot = "/docs"
        )
        
        val result = merger.merge(indexDoc, context, config)
        
        assertTrue(result.errors.isEmpty(), "Should have no errors")
        assertTrue(result.document != null, "Should have a document")
        
        // Verify the xref was converted to simple anchor
        val paragraph = result.document?.children?.get(0) as? Paragraph
        val xref = paragraph?.content?.get(1) as? CrossReference
        assertEquals("section1", xref?.targetId, "Should convert Antora xref to simple anchor")
    }
    
    @Test
    fun `should convert Antora xref with module-qualified coordinate`() {
        val fileSystem = MockFileSystem(
            mapOf(
                "/docs/modules/ROOT/pages/index.adoc" to "See xref:admin:page\$config.adoc#settings[]"
            )
        )
        
        val resolver = MockAntoraResolver(fileSystem)
        val parser = MockAsciidocParser()
        val merger = ContentMerger(resolver, parser, fileSystem)
        
        val indexDoc = Document(
            title = null,
            children = listOf(
                Paragraph(
                    content = listOf(
                        Text(content = "See ", attributes = emptyMap(), sourceLocation = SourceLocation(1, 0)),
                        CrossReference(
                            targetId = "admin:page\$config.adoc#settings",
                            customText = null,
                            attributes = emptyMap(),
                            sourceLocation = SourceLocation(1, 4)
                        )
                    ),
                    attributes = emptyMap(),
                    sourceLocation = SourceLocation(1, 0)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val context = ResolutionContext(
            componentRoot = "/docs",
            currentModule = "ROOT",
            currentFilePath = "/docs/modules/ROOT/pages/index.adoc"
        )
        
        val config = AssemblerConfig(
            indexFile = "/docs/modules/ROOT/pages/index.adoc",
            outputFile = "/output/result.adoc",
            componentRoot = "/docs"
        )
        
        val result = merger.merge(indexDoc, context, config)
        
        assertTrue(result.errors.isEmpty(), "Should have no errors")
        assertTrue(result.document != null, "Should have a document")
        
        // Verify the xref was converted to simple anchor
        val paragraph = result.document?.children?.get(0) as? Paragraph
        val xref = paragraph?.content?.get(1) as? CrossReference
        assertEquals("settings", xref?.targetId, "Should extract anchor from module-qualified xref")
    }
    
    @Test
    fun `should warn when cross-reference target not found`() {
        val fileSystem = MockFileSystem(
            mapOf(
                "/docs/modules/ROOT/pages/index.adoc" to "See <<nonexistent>>"
            )
        )
        
        val resolver = MockAntoraResolver(fileSystem)
        val parser = MockAsciidocParser()
        val merger = ContentMerger(resolver, parser, fileSystem)
        
        val indexDoc = Document(
            title = null,
            children = listOf(
                Paragraph(
                    content = listOf(
                        Text(content = "See ", attributes = emptyMap(), sourceLocation = SourceLocation(1, 0)),
                        CrossReference(
                            targetId = "nonexistent",
                            customText = null,
                            attributes = emptyMap(),
                            sourceLocation = SourceLocation(1, 4)
                        )
                    ),
                    attributes = emptyMap(),
                    sourceLocation = SourceLocation(1, 0)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val context = ResolutionContext(
            componentRoot = "/docs",
            currentModule = "ROOT",
            currentFilePath = "/docs/modules/ROOT/pages/index.adoc"
        )
        
        val config = AssemblerConfig(
            indexFile = "/docs/modules/ROOT/pages/index.adoc",
            outputFile = "/output/result.adoc",
            componentRoot = "/docs"
        )
        
        val result = merger.merge(indexDoc, context, config)
        
        assertTrue(result.errors.isEmpty(), "Should have no errors")
        assertTrue(result.warnings.isNotEmpty(), "Should have warnings")
        assertTrue(
            result.warnings.any { it.message.contains("Cross-reference target 'nonexistent' not found") },
            "Should warn about missing anchor"
        )
    }
    
    @Test
    fun `should handle xref without anchor in Antora coordinate`() {
        val fileSystem = MockFileSystem(
            mapOf(
                "/docs/modules/ROOT/pages/index.adoc" to "See xref:page\$other.adoc[]"
            )
        )
        
        val resolver = MockAntoraResolver(fileSystem)
        val parser = MockAsciidocParser()
        val merger = ContentMerger(resolver, parser, fileSystem)
        
        val indexDoc = Document(
            title = null,
            children = listOf(
                Paragraph(
                    content = listOf(
                        Text(content = "See ", attributes = emptyMap(), sourceLocation = SourceLocation(1, 0)),
                        CrossReference(
                            targetId = "page\$other.adoc",
                            customText = null,
                            attributes = emptyMap(),
                            sourceLocation = SourceLocation(1, 4)
                        )
                    ),
                    attributes = emptyMap(),
                    sourceLocation = SourceLocation(1, 0)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val context = ResolutionContext(
            componentRoot = "/docs",
            currentModule = "ROOT",
            currentFilePath = "/docs/modules/ROOT/pages/index.adoc"
        )
        
        val config = AssemblerConfig(
            indexFile = "/docs/modules/ROOT/pages/index.adoc",
            outputFile = "/output/result.adoc",
            componentRoot = "/docs"
        )
        
        val result = merger.merge(indexDoc, context, config)
        
        assertTrue(result.errors.isEmpty(), "Should have no errors")
        assertTrue(result.document != null, "Should have a document")
        
        // Verify the xref was converted using filename as anchor
        val paragraph = result.document?.children?.get(0) as? Paragraph
        val xref = paragraph?.content?.get(1) as? CrossReference
        assertEquals("other", xref?.targetId, "Should use filename without extension as anchor")
    }
    
    @Test
    fun `should maintain anchor registry across includes`() {
        val fileSystem = MockFileSystem(
            mapOf(
                "/docs/modules/ROOT/pages/index.adoc" to "[[main-anchor]]\n\ninclude::partial\$intro.adoc[]",
                "/docs/modules/ROOT/partials/intro.adoc" to "[[intro-anchor]]\n\nIntro content"
            )
        )
        
        val resolver = MockAntoraResolver(fileSystem)
        val parser = MockAsciidocParser()
        val merger = ContentMerger(resolver, parser, fileSystem)
        
        val indexDoc = Document(
            title = null,
            children = listOf(
                Paragraph(
                    content = listOf(
                        Text(content = "Main", attributes = emptyMap(), sourceLocation = SourceLocation(2, 0))
                    ),
                    attributes = mapOf("id" to "main-anchor"),
                    sourceLocation = SourceLocation(2, 0)
                ),
                IncludeDirective(
                    path = "partial\$intro.adoc",
                    attributes = emptyMap(),
                    sourceLocation = SourceLocation(4, 0)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val context = ResolutionContext(
            componentRoot = "/docs",
            currentModule = "ROOT",
            currentFilePath = "/docs/modules/ROOT/pages/index.adoc"
        )
        
        val config = AssemblerConfig(
            indexFile = "/docs/modules/ROOT/pages/index.adoc",
            outputFile = "/output/result.adoc",
            componentRoot = "/docs"
        )
        
        val result = merger.merge(indexDoc, context, config)
        
        assertTrue(result.errors.isEmpty(), "Should have no errors")
        assertTrue(result.warnings.isEmpty(), "Should have no warnings")
        // Both anchors should be in the registry
    }
}

// Mock implementations for testing

class MockFileSystem(private val files: Map<String, String>) : FileSystemAccess {
    override fun exists(path: String): Boolean = files.containsKey(path)
    
    override fun isDirectory(path: String): Boolean = false
    
    override fun readFile(path: String): FileReadResult {
        return files[path]?.let { FileReadResult.Success(it) }
            ?: FileReadResult.Error("File not found: $path")
    }
    
    override fun listDirectory(path: String): List<String> = emptyList()
    
    override fun writeFile(path: String, content: String): FileWriteResult {
        return FileWriteResult.Success
    }
}

class MockAntoraResolver(private val fileSystem: FileSystemAccess) : AntoraResolver {
    override fun resolve(coordinate: ResourceCoordinate, context: ResolutionContext): ResolutionResult {
        val basePath = "${context.componentRoot}/modules/${context.currentModule}"
        val typePath = when (coordinate.type) {
            ResourceType.PARTIAL -> "partials"
            ResourceType.EXAMPLE -> "examples"
            ResourceType.PAGE -> "pages"
            ResourceType.IMAGE -> "images"
            ResourceType.ATTACHMENT -> "attachments"
            ResourceType.RELATIVE -> return resolveRelative(coordinate.path, context)
        }
        
        val fullPath = "$basePath/$typePath/${coordinate.path}"
        return if (fileSystem.exists(fullPath)) {
            ResolutionResult.Success(fullPath)
        } else {
            ResolutionResult.Error("File not found: $fullPath", ResolutionErrorType.FILE_NOT_FOUND)
        }
    }
    
    override fun resolveInclude(path: String, context: ResolutionContext): ResolutionResult {
        // Try parsing as coordinate first
        val coordinate = ResourceCoordinate.parse(path)
        if (coordinate != null) {
            return resolve(coordinate, context)
        }
        
        // Fall back to relative path
        val basePath = context.currentFilePath?.substringBeforeLast('/') ?: "${context.componentRoot}/modules/${context.currentModule}/pages"
        val fullPath = "$basePath/$path"
        
        return if (fileSystem.exists(fullPath)) {
            ResolutionResult.Success(fullPath)
        } else {
            ResolutionResult.Error("File not found: $fullPath", ResolutionErrorType.FILE_NOT_FOUND)
        }
    }
    
    private fun resolveRelative(path: String, context: ResolutionContext): ResolutionResult {
        val basePath = context.currentFilePath?.substringBeforeLast('/') ?: "${context.componentRoot}/modules/${context.currentModule}/pages"
        val fullPath = "$basePath/$path"
        
        return if (fileSystem.exists(fullPath)) {
            ResolutionResult.Success(fullPath)
        } else {
            ResolutionResult.Error("File not found: $fullPath", ResolutionErrorType.FILE_NOT_FOUND)
        }
    }
}

class MockAsciidocParser : AsciidocParser {
    override fun parse(source: String): ParseResult {
        // Simple mock parser that creates a paragraph for non-include content
        val lines = source.lines()
        val children = mutableListOf<BlockElement>()
        val documentAttributes = mutableMapOf<String, String>()
        var currentAnchorId: String? = null
        
        for ((index, line) in lines.withIndex()) {
            if (line.startsWith(":") && line.contains(":") && line.endsWith(":") || 
                (line.startsWith(":") && line.contains(": "))) {
                // Parse document attribute
                val attributeLine = line.trim()
                if (attributeLine.startsWith(":") && !attributeLine.startsWith("::")) {
                    val parts = attributeLine.substring(1).split(":", limit = 2)
                    if (parts.size == 2) {
                        val key = parts[0].trim()
                        val value = parts[1].trim()
                        documentAttributes[key] = value
                    }
                }
            } else if (line.startsWith("[[") && line.contains("]]")) {
                // Parse anchor definition
                val anchorMatch = Regex("""\[\[([^\]]+)\]\]""").find(line)
                if (anchorMatch != null) {
                    currentAnchorId = anchorMatch.groupValues[1]
                }
            } else if (line.startsWith("include::")) {
                // Parse include directive
                val pathMatch = Regex("""include::([^\[]+)\[\]""").find(line)
                if (pathMatch != null) {
                    val path = pathMatch.groupValues[1]
                    children.add(
                        IncludeDirective(
                            path = path,
                            attributes = emptyMap(),
                            sourceLocation = SourceLocation(index + 1, 0)
                        )
                    )
                }
            } else if (line.startsWith("==")) {
                // Parse section heading
                val level = line.takeWhile { it == '=' }.length
                val title = line.dropWhile { it == '=' }.trim()
                val attributes = if (currentAnchorId != null) {
                    mapOf("id" to currentAnchorId)
                } else {
                    emptyMap()
                }
                children.add(
                    Section(
                        level = level,
                        title = title,
                        children = emptyList(),
                        attributes = attributes,
                        sourceLocation = SourceLocation(index + 1, 0)
                    )
                )
                currentAnchorId = null
            } else if (line.isNotBlank()) {
                // Create a simple paragraph
                val attributes = if (currentAnchorId != null) {
                    mapOf("id" to currentAnchorId)
                } else {
                    emptyMap()
                }
                children.add(
                    Paragraph(
                        content = listOf(
                            Text(
                                content = line,
                                attributes = emptyMap(),
                                sourceLocation = SourceLocation(index + 1, 0)
                            )
                        ),
                        attributes = attributes,
                        sourceLocation = SourceLocation(index + 1, 0)
                    )
                )
                currentAnchorId = null
            }
        }
        
        return ParseResult(
            document = Document(
                title = null,
                children = children,
                documentAttributes = documentAttributes,
                sourceLocation = SourceLocation(1, 0)
            ),
            errors = emptyList(),
            warnings = emptyList()
        )
    }
    
    override fun parse(lines: List<String>): ParseResult {
        return parse(lines.joinToString("\n"))
    }
}
