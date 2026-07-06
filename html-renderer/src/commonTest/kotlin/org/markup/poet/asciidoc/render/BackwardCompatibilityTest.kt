package org.markup.poet.asciidoc.render

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.Header
import org.markup.poet.asciidoc.asg.InlineSpan
import org.markup.poet.asciidoc.asg.InlineText
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.LeafBlockForm
import org.markup.poet.asciidoc.asg.LeafBlockName
import org.markup.poet.asciidoc.asg.SectionBlock
import org.markup.poet.asciidoc.asg.SpanForm
import org.markup.poet.asciidoc.asg.SpanVariant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Backward compatibility tests for custom CSS styling feature.
 *
 * These tests verify that the CSS customization features do not break
 * existing functionality. They ensure that:
 * - Default RenderConfig produces same output as before
 * - Existing Theme interface works unchanged
 * - Existing API usage patterns continue to work
 */
class BackwardCompatibilityTest {

    private val blockRenderer = DefaultBlockRenderer(
        DefaultHtmlBuilder(DefaultHtmlEscaper()),
        DefaultInlineRenderer(DefaultHtmlBuilder(DefaultHtmlEscaper()))
    )
    private val inlineRenderer = DefaultInlineRenderer(DefaultHtmlBuilder(DefaultHtmlEscaper()))
    private val renderer = DefaultHtmlRenderer(blockRenderer, inlineRenderer)

    private fun document(
        title: String? = "Test",
        blocks: List<org.markup.poet.asciidoc.asg.Block> = emptyList(),
        attributes: Map<String, String> = emptyMap()
    ) = AsgDocument(
        attributes = attributes,
        header = title?.let { Header(title = listOf(InlineText(it))) },
        blocks = blocks
    )

    private fun paragraph(vararg inlines: org.markup.poet.asciidoc.asg.Inline) = LeafBlock(
        name = LeafBlockName.PARAGRAPH,
        form = LeafBlockForm.PARAGRAPH,
        inlines = inlines.toList()
    )

    /**
     * Test that default RenderConfig produces same output as before.
     */
    @Test
    fun `default RenderConfig should produce same output as before custom CSS feature`() {
        // Arrange - Create a simple document
        val document = document(
            title = "Test Document",
            blocks = listOf(
                SectionBlock(
                    title = listOf(InlineText("Introduction")),
                    level = 0,
                    blocks = listOf(paragraph(InlineText("This is a test paragraph.")))
                )
            )
        )

        // Act - Render with default config (no parameters)
        val config = RenderConfig.default()
        val result = renderer.render(document, config)

        // Assert - Verify expected structure
        assertTrue(result.isSuccess, "Rendering should succeed with default config")
        val html = result.getOrThrow()

        // Verify document structure
        assertTrue(html.contains("<!DOCTYPE html>"), "Should include DOCTYPE")
        assertTrue(html.contains("<html lang=\"en\">"), "Should include html tag with default language")
        assertTrue(html.contains("<head>"), "Should include head section")
        assertTrue(html.contains("<title>Test Document</title>"), "Should include document title")
        assertTrue(html.contains("<body>"), "Should include body section")

        // Verify default CSS is included (INLINE mode by default)
        assertTrue(html.contains("<style>"), "Should include inline CSS by default")
        assertTrue(html.contains(".heading"), "Should include default theme CSS classes")
        assertTrue(html.contains("--mp-color-primary"), "Should include CSS variables")

        // Verify content is rendered
        assertTrue(html.contains("Introduction"), "Should render heading content")
        assertTrue(html.contains("This is a test paragraph."), "Should render paragraph content")

        // Verify no custom CSS is included
        assertFalse(html.contains("/* Custom CSS */"), "Should not include custom CSS marker")
    }

    /**
     * Test that RenderConfig constructor with no cssOptions parameter works.
     */
    @Test
    fun `RenderConfig constructor without cssOptions should work with default values`() {
        // Arrange
        val document = document()

        // Act - Create config without specifying cssOptions (uses default)
        val config = RenderConfig(
            outputOptions = OutputOptions(standalone = true),
            theme = Theme.default()
        )
        val result = renderer.render(document, config)

        // Assert
        assertTrue(result.isSuccess, "Should render successfully with old-style config")
        val html = result.getOrThrow()
        assertTrue(html.contains("<style>"), "Should include default CSS")
        assertTrue(html.contains(".heading"), "Should use default theme")
    }

    /**
     * Test that existing Theme interface works unchanged.
     */
    @Test
    fun `existing Theme interface should work without modification`() {
        // Arrange - Create a custom theme using the existing interface
        val customTheme = object : Theme {
            override fun headingClasses(level: Int) = "custom-heading custom-h$level"
            override fun paragraphClasses() = "custom-paragraph"
            override fun codeBlockClasses() = "custom-code"
            override fun tableClasses() = "custom-table"
            override fun listClasses() = "custom-list"
            override fun quoteClasses() = "custom-quote"
            override fun admonitionClasses(type: String) = "custom-admonition custom-$type"

            override fun getCss() = """
                .custom-heading { font-weight: bold; }
                .custom-paragraph { margin: 1em 0; }
            """.trimIndent()
        }

        val document = document(
            blocks = listOf(
                SectionBlock(
                    title = listOf(InlineText("Title")),
                    level = 0,
                    blocks = emptyList()
                )
            )
        )

        // Act - Use custom theme with default config
        val config = RenderConfig(theme = customTheme)
        val result = renderer.render(document, config)

        // Assert
        assertTrue(result.isSuccess, "Should render with custom theme")
        val html = result.getOrThrow()
        assertTrue(html.contains(".custom-heading"), "Should include custom theme CSS")
        assertTrue(html.contains(".custom-paragraph"), "Should include custom theme CSS")
    }

    /**
     * Test that DefaultTheme continues to work as before.
     */
    @Test
    fun `DefaultTheme should produce same CSS structure as before`() {
        // Arrange
        val theme = DefaultTheme()

        // Act
        val css = theme.getCss()

        // Assert - Verify expected CSS structure
        assertTrue(css.contains(":root {"), "Should define CSS variables in :root")
        assertTrue(css.contains("--mp-color-primary"), "Should define primary color variable")
        assertTrue(css.contains("--mp-color-text"), "Should define text color variable")
        assertTrue(css.contains("--mp-font-family"), "Should define font family variable")
        assertTrue(css.contains("--mp-spacing-unit"), "Should define spacing variable")
        assertTrue(css.contains("--mp-line-height-base"), "Should define line height variable")

        // Verify class definitions
        assertTrue(css.contains(".heading {"), "Should define heading class")
        assertTrue(css.contains(".paragraph {"), "Should define paragraph class")
        assertTrue(css.contains(".code-block {"), "Should define code block class")
        assertTrue(css.contains(".table {"), "Should define table class")
        assertTrue(css.contains(".list {"), "Should define list class")
        assertTrue(css.contains(".quote {"), "Should define quote class")
        assertTrue(css.contains(".admonition {"), "Should define admonition class")

        // Verify class methods return expected values
        assertEquals("heading heading-1", theme.headingClasses(1))
        assertEquals("heading heading-2", theme.headingClasses(2))
        assertEquals("paragraph", theme.paragraphClasses())
        assertEquals("code-block", theme.codeBlockClasses())
        assertEquals("table", theme.tableClasses())
        assertEquals("list", theme.listClasses())
        assertEquals("quote", theme.quoteClasses())
        assertEquals("admonition admonition-note", theme.admonitionClasses("note"))
    }

    /**
     * Test that OutputOptions with CSS modes work as before.
     */
    @Test
    fun `OutputOptions with CssMode NONE should produce same output as before`() {
        // Arrange
        val document = document(
            blocks = listOf(paragraph(InlineText("Content")))
        )

        // Act - Use NONE mode (no CSS)
        val config = RenderConfig(
            outputOptions = OutputOptions(
                standalone = true,
                cssMode = CssMode.NONE
            )
        )
        val result = renderer.render(document, config)

        // Assert
        assertTrue(result.isSuccess)
        val html = result.getOrThrow()
        assertFalse(html.contains("<style>"), "Should not include style tag")
        assertFalse(html.contains("<link rel=\"stylesheet\""), "Should not include link tag")
        assertTrue(html.contains("Content"), "Should still render content")
    }

    /**
     * Test that OutputOptions with CssMode INLINE works as before.
     */
    @Test
    fun `OutputOptions with CssMode INLINE should produce same output as before`() {
        // Arrange
        val document = document()

        // Act
        val config = RenderConfig(
            outputOptions = OutputOptions(
                standalone = true,
                cssMode = CssMode.INLINE
            )
        )
        val result = renderer.render(document, config)

        // Assert
        assertTrue(result.isSuccess)
        val html = result.getOrThrow()
        assertTrue(html.contains("<style>"), "Should include inline style tag")
        assertTrue(html.contains(".heading"), "Should include default theme CSS")
        assertFalse(html.contains("<link rel=\"stylesheet\""), "Should not include external link")
    }

    /**
     * Test that OutputOptions with CssMode EXTERNAL works as before.
     */
    @Test
    fun `OutputOptions with CssMode EXTERNAL should produce same output as before`() {
        // Arrange
        val document = document()

        // Mock file writer
        var writtenContent: String? = null
        val mockFileWriter = object : FileWriter {
            override fun writeFile(path: String, content: String): Result<Unit> {
                writtenContent = content
                return Result.success(Unit)
            }
        }

        val renderer = DefaultHtmlRenderer(blockRenderer, inlineRenderer, fileWriter = mockFileWriter)

        // Act
        val config = RenderConfig(
            outputOptions = OutputOptions(
                standalone = true,
                cssMode = CssMode.EXTERNAL,
                cssPath = "styles.css"
            )
        )
        val result = renderer.render(document, config)

        // Assert
        assertTrue(result.isSuccess)
        val html = result.getOrThrow()
        assertTrue(html.contains("<link rel=\"stylesheet\" href=\"styles.css\">"), "Should include external link")
        assertFalse(html.contains("<style>"), "Should not include inline style tag")

        // Verify CSS file content is default theme
        assertTrue(writtenContent != null, "Should write CSS file")
        assertTrue(writtenContent!!.contains(".heading"), "Should write default theme CSS")
    }

    /**
     * Test that fragment mode works as before.
     */
    @Test
    fun `fragment mode should produce same output as before`() {
        // Arrange
        val document = document(
            blocks = listOf(paragraph(InlineText("Fragment content")))
        )

        // Act
        val config = RenderConfig(
            outputOptions = OutputOptions(standalone = false)
        )
        val result = renderer.render(document, config)

        // Assert
        assertTrue(result.isSuccess)
        val html = result.getOrThrow()
        assertTrue(html.contains("Fragment content"), "Should render content")
        assertFalse(html.contains("<!DOCTYPE html>"), "Should not include DOCTYPE")
        assertFalse(html.contains("<html"), "Should not include html tag")
        assertFalse(html.contains("<head>"), "Should not include head")
        assertFalse(html.contains("<body>"), "Should not include body tag")
    }

    /**
     * Test that metadata inclusion works as before.
     */
    @Test
    fun `metadata inclusion should work as before`() {
        // Arrange
        val document = document(
            title = "Test Document",
            attributes = mapOf(
                "author" to "Jane Doe",
                "description" to "Test description",
                "keywords" to "test, backward, compatibility"
            )
        )

        // Act
        val config = RenderConfig(
            outputOptions = OutputOptions(
                standalone = true,
                includeMetadata = true
            )
        )
        val result = renderer.render(document, config)

        // Assert
        assertTrue(result.isSuccess)
        val html = result.getOrThrow()
        assertTrue(html.contains("<meta name=\"author\" content=\"Jane Doe\">"))
        assertTrue(html.contains("<meta name=\"description\" content=\"Test description\">"))
        assertTrue(html.contains("<meta name=\"keywords\" content=\"test, backward, compatibility\">"))
    }

    /**
     * Test that validation errors work as before.
     */
    @Test
    fun `validation errors should work as before`() {
        // Arrange
        val document = document()

        // Act - Invalid config: EXTERNAL mode without cssPath
        val config = RenderConfig(
            outputOptions = OutputOptions(
                standalone = true,
                cssMode = CssMode.EXTERNAL,
                cssPath = null
            )
        )
        val result = renderer.render(document, config)

        // Assert
        assertTrue(result.isFailure, "Should fail with invalid config")
        val exception = result.exceptionOrNull()
        assertTrue(exception is RenderException.InvalidConfiguration)
        assertTrue(exception.message!!.contains("cssPath"))
    }

    /**
     * Test that Theme.default() returns DefaultTheme.
     */
    @Test
    fun `Theme default factory should return DefaultTheme`() {
        // Act
        val theme = Theme.default()

        // Assert
        assertTrue(theme is DefaultTheme, "Theme.default() should return DefaultTheme")
        assertEquals("heading heading-1", theme.headingClasses(1))
        assertEquals("paragraph", theme.paragraphClasses())
    }

    /**
     * Test that CssOptions.default() provides backward compatible defaults.
     */
    @Test
    fun `CssOptions default should maintain backward compatible behavior`() {
        // Act
        val cssOptions = CssOptions.default()

        // Assert - Verify defaults maintain backward compatibility
        assertEquals(null, cssOptions.customCssContent, "No custom CSS by default")
        assertEquals(null, cssOptions.customCssPath, "No custom CSS file by default")
        assertEquals(true, cssOptions.includeDefaultCss, "Default CSS included by default")
        assertEquals("", cssOptions.builtInTheme, "Empty built-in theme by default (uses theme from RenderConfig)")
        assertEquals(emptyMap(), cssOptions.cssVariables, "No CSS variables by default")
    }

    /**
     * Test that rendering with no custom CSS produces identical output.
     */
    @Test
    fun `rendering without custom CSS should produce identical output to previous version`() {
        // Arrange
        val document = document(
            title = "Comparison Test",
            blocks = listOf(
                SectionBlock(
                    title = listOf(InlineText("Main Title")),
                    level = 0,
                    blocks = listOf(
                        paragraph(
                            InlineText("This is a "),
                            InlineSpan(
                                variant = SpanVariant.STRONG,
                                form = SpanForm.CONSTRAINED,
                                inlines = listOf(InlineText("test"))
                            ),
                            InlineText(" paragraph.")
                        )
                    )
                ),
                SectionBlock(
                    title = listOf(InlineText("Subsection")),
                    level = 1,
                    blocks = emptyList()
                )
            ),
            attributes = mapOf("author" to "Test Author")
        )

        // Act - Render with default config (no custom CSS)
        val config = RenderConfig.default()
        val result = renderer.render(document, config)

        // Assert - Verify all expected elements are present
        assertTrue(result.isSuccess)
        val html = result.getOrThrow()

        // Document structure
        assertTrue(html.contains("<!DOCTYPE html>"))
        assertTrue(html.contains("<html lang=\"en\">"))
        assertTrue(html.contains("<title>Comparison Test</title>"))
        assertTrue(html.contains("<meta name=\"author\" content=\"Test Author\">"))

        // CSS inclusion (default theme, inline mode)
        assertTrue(html.contains("<style>"))
        assertTrue(html.contains(":root {"))
        assertTrue(html.contains("--mp-color-primary"))
        assertTrue(html.contains(".heading {"))
        assertTrue(html.contains(".paragraph {"))

        // Content rendering
        assertTrue(html.contains("Main Title"))
        assertTrue(html.contains("This is a"))
        assertTrue(html.contains("<strong>test</strong>"))
        assertTrue(html.contains("paragraph."))
        assertTrue(html.contains("Subsection"))

        // CSS classes applied
        assertTrue(html.contains("class=\"heading heading-1\""))
        assertTrue(html.contains("class=\"heading heading-2\""))
        assertTrue(html.contains("class=\"paragraph\""))

        // No custom CSS markers
        assertFalse(html.contains("/* Custom CSS */"))
        assertFalse(html.contains("/* Custom styles */"))
    }
}
