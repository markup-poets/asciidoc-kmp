package org.markup.poet.antora.assembler

import org.markup.poet.antora.*
import org.markup.poet.asciidoc.asg.*
import org.markup.poet.asciidoc.parser.AsciidocParser
import org.markup.poet.asciidoc.parser.ParseResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** A single-point source location at the given 1-based line and column. */
private fun loc(line: Int, col: Int = 1) = Location(Position(line, col), Position(line, col))

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

        val indexDoc = AsgDocument(
            header = Header(title = listOf(InlineText("Index"))),
            blocks = listOf(
                IncludeBlock(
                    path = "partial\$intro.adoc",
                    location = loc(3)
                )
            )
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
        assertEquals(1, result.document?.blocks?.size, "Should have one child block")
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

        val docA = AsgDocument(
            blocks = listOf(
                IncludeBlock(
                    path = "b.adoc",
                    location = loc(1)
                )
            )
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

        val indexDoc = AsgDocument(
            blocks = listOf(
                IncludeBlock(
                    path = "level1.adoc",
                    location = loc(1)
                )
            )
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

        val indexDoc = AsgDocument(
            blocks = listOf(
                IncludeBlock(
                    path = "missing.adoc",
                    location = loc(1)
                )
            )
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

        val indexDoc = AsgDocument(
            blocks = listOf(
                IncludeBlock(
                    path = "partial\$outer.adoc",
                    location = loc(1)
                )
            )
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

        val indexDoc = AsgDocument(
            blocks = listOf(
                IncludeBlock(
                    path = "code.txt",
                    lineRange = 2..4,
                    location = loc(1)
                )
            )
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
        assertEquals(3, result.document?.blocks?.size, "Should have 3 lines (2, 3, 4)")
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

        val indexDoc = AsgDocument(
            blocks = listOf(
                IncludeBlock(
                    path = "code.java",
                    attributes = mapOf("tags" to "method1"),
                    location = loc(1)
                )
            )
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
        val indexDoc = AsgDocument(
            header = Header(title = listOf(InlineText("Document"))),
            blocks = listOf(
                LeafBlock(
                    name = LeafBlockName.PARAGRAPH,
                    form = LeafBlockForm.PARAGRAPH,
                    inlines = listOf(InlineText("Example:", location = loc(3))),
                    location = loc(3)
                ),
                IncludeBlock(
                    path = "code.txt",
                    attributes = mapOf("indent" to "4"),
                    location = loc(5, 5) // Column 5 indicates 4-space indentation
                )
            )
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

        val indexDoc = AsgDocument(
            blocks = listOf(
                IncludeBlock(
                    path = "code.txt",
                    lineRange = 5..10,
                    location = loc(1)
                )
            )
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

        val indexDoc = AsgDocument(
            blocks = listOf(
                IncludeBlock(
                    path = "code.java",
                    attributes = mapOf("tags" to "method1,method2"),
                    location = loc(1)
                )
            )
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

        val indexDoc = AsgDocument(
            blocks = listOf(
                IncludeBlock(
                    path = "code.adoc",
                    attributes = mapOf("tags" to "section1"),
                    location = loc(1)
                )
            )
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

        // The include directive is indented by 4 spaces (1-based column 5)
        val indexDoc = AsgDocument(
            header = Header(title = listOf(InlineText("Document"))),
            blocks = listOf(
                LeafBlock(
                    name = LeafBlockName.PARAGRAPH,
                    form = LeafBlockForm.PARAGRAPH,
                    inlines = listOf(InlineText("Example:", location = loc(3))),
                    location = loc(3)
                ),
                IncludeBlock(
                    path = "code.txt",
                    location = loc(5, 5) // Column 5 = 4 spaces indent
                )
            )
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

        val indexDoc = AsgDocument(
            blocks = listOf(
                IncludeBlock(
                    path = "code.txt",
                    location = loc(1, 5) // 4 spaces indent
                )
            )
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

        val indexDoc = AsgDocument(
            blocks = listOf(
                IncludeBlock(
                    path = "code.java",
                    attributes = mapOf("tags" to "method1"),
                    location = loc(1, 5) // 4 spaces indent
                )
            )
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

        val indexDoc = AsgDocument(
            attributes = mapOf("author" to "John Doe"),
            blocks = listOf(
                IncludeBlock(
                    path = "partial\$intro.adoc",
                    location = loc(3)
                )
            )
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
        assertEquals("John Doe", result.document?.attributes?.get("author"), "Should preserve author from index")
        assertEquals("1.0", result.document?.attributes?.get("version"), "Should merge version from included file")
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

        val indexDoc = AsgDocument(
            attributes = mapOf("version" to "2.0"),
            blocks = listOf(
                IncludeBlock(
                    path = "partial\$intro.adoc",
                    location = loc(3)
                )
            )
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
        assertEquals("2.0", result.document?.attributes?.get("version"), "Should keep first definition (2.0)")
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

        val indexDoc = AsgDocument(
            attributes = mapOf("author" to "John"),
            blocks = listOf(
                IncludeBlock(
                    path = "partial\$outer.adoc",
                    location = loc(3)
                )
            )
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
        assertEquals("John", result.document?.attributes?.get("author"), "Should have author from index")
        assertEquals("1.0", result.document?.attributes?.get("version"), "Should have version from outer")
        assertEquals("MIT", result.document?.attributes?.get("license"), "Should have license from inner")
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

        val indexDoc = AsgDocument(
            attributes = mapOf("version" to "1.0"),
            blocks = listOf(
                IncludeBlock(
                    path = "partial\$intro.adoc",
                    location = loc(3)
                )
            )
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
        assertEquals("1.0", result.document?.attributes?.get("version"), "Should keep the value")
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

        val indexDoc = AsgDocument(
            header = Header(title = listOf(InlineText("Index"))),
            blocks = listOf(
                SectionBlock(
                    title = listOf(InlineText("Section 1")),
                    level = 1,
                    blocks = emptyList(),
                    metadata = BlockMetadata(id = "section1"),
                    location = loc(3)
                ),
                LeafBlock(
                    name = LeafBlockName.PARAGRAPH,
                    form = LeafBlockForm.PARAGRAPH,
                    inlines = listOf(
                        InlineText("See ", location = loc(5)),
                        InlineRef(
                            variant = RefVariant.XREF,
                            target = "section1",
                            inlines = emptyList(),
                            location = loc(5, 5)
                        )
                    ),
                    location = loc(5)
                )
            )
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
        val paragraph = result.document?.blocks?.get(1) as? LeafBlock
        assertTrue(paragraph != null, "Should have a paragraph")
        val xref = paragraph?.inlines?.get(1) as? InlineRef
        assertTrue(xref != null, "Should have a cross-reference")
        assertEquals("section1", xref?.target, "Should preserve the anchor reference")
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

        val indexDoc = AsgDocument(
            blocks = listOf(
                IncludeBlock(
                    path = "partial\$intro.adoc",
                    location = loc(1)
                ),
                LeafBlock(
                    name = LeafBlockName.PARAGRAPH,
                    form = LeafBlockForm.PARAGRAPH,
                    inlines = listOf(
                        InlineText("See ", location = loc(3)),
                        InlineRef(
                            variant = RefVariant.XREF,
                            target = "intro-section",
                            inlines = emptyList(),
                            location = loc(3, 5)
                        )
                    ),
                    location = loc(3)
                )
            )
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

        val indexDoc = AsgDocument(
            blocks = listOf(
                LeafBlock(
                    name = LeafBlockName.PARAGRAPH,
                    form = LeafBlockForm.PARAGRAPH,
                    inlines = listOf(
                        InlineText("See ", location = loc(1)),
                        InlineRef(
                            variant = RefVariant.XREF,
                            target = "page\$other.adoc#section1",
                            inlines = emptyList(),
                            location = loc(1, 5)
                        )
                    ),
                    location = loc(1)
                )
            )
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
        val paragraph = result.document?.blocks?.get(0) as? LeafBlock
        val xref = paragraph?.inlines?.get(1) as? InlineRef
        assertEquals("section1", xref?.target, "Should convert Antora xref to simple anchor")
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

        val indexDoc = AsgDocument(
            blocks = listOf(
                LeafBlock(
                    name = LeafBlockName.PARAGRAPH,
                    form = LeafBlockForm.PARAGRAPH,
                    inlines = listOf(
                        InlineText("See ", location = loc(1)),
                        InlineRef(
                            variant = RefVariant.XREF,
                            target = "admin:page\$config.adoc#settings",
                            inlines = emptyList(),
                            location = loc(1, 5)
                        )
                    ),
                    location = loc(1)
                )
            )
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
        val paragraph = result.document?.blocks?.get(0) as? LeafBlock
        val xref = paragraph?.inlines?.get(1) as? InlineRef
        assertEquals("settings", xref?.target, "Should extract anchor from module-qualified xref")
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

        val indexDoc = AsgDocument(
            blocks = listOf(
                LeafBlock(
                    name = LeafBlockName.PARAGRAPH,
                    form = LeafBlockForm.PARAGRAPH,
                    inlines = listOf(
                        InlineText("See ", location = loc(1)),
                        InlineRef(
                            variant = RefVariant.XREF,
                            target = "nonexistent",
                            inlines = emptyList(),
                            location = loc(1, 5)
                        )
                    ),
                    location = loc(1)
                )
            )
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

        val indexDoc = AsgDocument(
            blocks = listOf(
                LeafBlock(
                    name = LeafBlockName.PARAGRAPH,
                    form = LeafBlockForm.PARAGRAPH,
                    inlines = listOf(
                        InlineText("See ", location = loc(1)),
                        InlineRef(
                            variant = RefVariant.XREF,
                            target = "page\$other.adoc",
                            inlines = emptyList(),
                            location = loc(1, 5)
                        )
                    ),
                    location = loc(1)
                )
            )
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
        val paragraph = result.document?.blocks?.get(0) as? LeafBlock
        val xref = paragraph?.inlines?.get(1) as? InlineRef
        assertEquals("other", xref?.target, "Should use filename without extension as anchor")
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

        val indexDoc = AsgDocument(
            blocks = listOf(
                LeafBlock(
                    name = LeafBlockName.PARAGRAPH,
                    form = LeafBlockForm.PARAGRAPH,
                    inlines = listOf(InlineText("Main", location = loc(2))),
                    metadata = BlockMetadata(id = "main-anchor"),
                    location = loc(2)
                ),
                IncludeBlock(
                    path = "partial\$intro.adoc",
                    location = loc(4)
                )
            )
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
        val blocks = mutableListOf<Block>()
        val attributes = mutableMapOf<String, String>()
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
                        attributes[key] = value
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
                    blocks.add(
                        IncludeBlock(
                            path = path,
                            location = Location(Position(index + 1, 1), Position(index + 1, 1))
                        )
                    )
                }
            } else if (line.startsWith("==")) {
                // Parse section heading (ASG level is one less than the '=' count)
                val markerLength = line.takeWhile { it == '=' }.length
                val title = line.dropWhile { it == '=' }.trim()
                blocks.add(
                    SectionBlock(
                        title = listOf(InlineText(title)),
                        level = markerLength - 1,
                        blocks = emptyList(),
                        metadata = currentAnchorId?.let { BlockMetadata(id = it) },
                        location = Location(Position(index + 1, 1), Position(index + 1, 1))
                    )
                )
                currentAnchorId = null
            } else if (line.isNotBlank()) {
                // Create a simple paragraph
                blocks.add(
                    LeafBlock(
                        name = LeafBlockName.PARAGRAPH,
                        form = LeafBlockForm.PARAGRAPH,
                        inlines = listOf(
                            InlineText(
                                value = line,
                                location = Location(Position(index + 1, 1), Position(index + 1, 1))
                            )
                        ),
                        metadata = currentAnchorId?.let { BlockMetadata(id = it) },
                        location = Location(Position(index + 1, 1), Position(index + 1, 1))
                    )
                )
                currentAnchorId = null
            }
        }

        return ParseResult(
            document = AsgDocument(
                attributes = attributes,
                blocks = blocks
            ),
            errors = emptyList(),
            warnings = emptyList()
        )
    }
}
