package org.markup.poet.asciidoc.render

import org.markup.poet.asciidoc.ast.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for DefaultHtmlRenderer.
 * 
 * Tests the main rendering functionality including standalone and fragment modes,
 * CSS inclusion, metadata generation, and configuration validation.
 */
class DefaultHtmlRendererTest {
    
    private val blockRenderer = DefaultBlockRenderer(
        DefaultHtmlBuilder(DefaultHtmlEscaper()),
        DefaultInlineRenderer(DefaultHtmlBuilder(DefaultHtmlEscaper()))
    )
    private val inlineRenderer = DefaultInlineRenderer(DefaultHtmlBuilder(DefaultHtmlEscaper()))
    private val renderer = DefaultHtmlRenderer(blockRenderer, inlineRenderer)
    
    @Test
    fun `should render standalone document with complete HTML structure`() {
        // Arrange
        val document = Document(
            title = "Test Document",
            children = listOf(
                Paragraph(
                    content = listOf(
                        Text("Hello World", emptyMap(), SourceLocation(1, 1))
                    ),
                    attributes = emptyMap(),
                    sourceLocation = SourceLocation(1, 1)
                )
            ),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = SourceLocation(1, 1)
        )
        val config = RenderConfig(
            outputOptions = OutputOptions(standalone = true)
        )
        
        // Act
        val result = renderer.render(document, config)
        
        // Assert
        assertTrue(result.isSuccess)
        val html = result.getOrThrow()
        assertTrue(html.contains("<!DOCTYPE html>"))
        assertTrue(html.contains("<html lang=\"en\">"))
        assertTrue(html.contains("<head>"))
        assertTrue(html.contains("<title>Test Document</title>"))
        assertTrue(html.contains("<body>"))
        assertTrue(html.contains("Hello World"))
        assertTrue(html.contains("</body>"))
        assertTrue(html.contains("</html>"))
    }
    
    @Test
    fun `should render fragment without document structure`() {
        // Arrange
        val document = Document(
            title = "Test Document",
            children = listOf(
                Paragraph(
                    content = listOf(
                        Text("Hello World", emptyMap(), SourceLocation(1, 1))
                    ),
                    attributes = emptyMap(),
                    sourceLocation = SourceLocation(1, 1)
                )
            ),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = SourceLocation(1, 1)
        )
        val config = RenderConfig(
            outputOptions = OutputOptions(standalone = false)
        )
        
        // Act
        val result = renderer.render(document, config)
        
        // Assert
        assertTrue(result.isSuccess)
        val html = result.getOrThrow()
        assertTrue(html.contains("Hello World"))
        assertTrue(!html.contains("<!DOCTYPE html>"))
        assertTrue(!html.contains("<html"))
        assertTrue(!html.contains("<head>"))
        assertTrue(!html.contains("<body>"))
    }
    
    @Test
    fun `should include inline CSS when configured`() {
        // Arrange
        val document = Document(
            title = "Test",
            children = emptyList(),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = SourceLocation(1, 1)
        )
        val config = RenderConfig(
            outputOptions = OutputOptions(
                standalone = true,
                cssMode = CssMode.INLINE
            )
        )
        
        // Act
        val result = renderer.render(document, config)
        
        // Assert
        assertTrue(result.isSuccess)
        val html = result.getOrThrow()
        assertTrue(html.contains("<style>"))
        assertTrue(html.contains(".heading"))
        assertTrue(html.contains("</style>"))
    }
    
    @Test
    fun `should include external CSS link when configured`() {
        // Arrange
        val document = Document(
            title = "Test",
            children = emptyList(),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = SourceLocation(1, 1)
        )
        
        // Create a mock FileWriter to capture written content
        var writtenPath: String? = null
        var writtenContent: String? = null
        val mockFileWriter = object : FileWriter {
            override fun writeFile(path: String, content: String): Result<Unit> {
                writtenPath = path
                writtenContent = content
                return Result.success(Unit)
            }
        }
        
        val renderer = DefaultHtmlRenderer(
            blockRenderer,
            inlineRenderer,
            fileWriter = mockFileWriter
        )
        
        val config = RenderConfig(
            outputOptions = OutputOptions(
                standalone = true,
                cssMode = CssMode.EXTERNAL,
                cssPath = "styles.css"
            )
        )
        
        // Act
        val result = renderer.render(document, config)
        
        // Assert
        assertTrue(result.isSuccess)
        val html = result.getOrThrow()
        assertTrue(html.contains("<link rel=\"stylesheet\" href=\"styles.css\">"))
        assertTrue(!html.contains("<style>"))
        
        // Verify CSS file was written
        assertEquals("styles.css", writtenPath)
        assertTrue(writtenContent != null)
        assertTrue(writtenContent!!.contains(".heading")) // Should contain default CSS
    }
    
    @Test
    fun `should not include CSS when mode is NONE`() {
        // Arrange
        val document = Document(
            title = "Test",
            children = emptyList(),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = SourceLocation(1, 1)
        )
        val config = RenderConfig(
            outputOptions = OutputOptions(
                standalone = true,
                cssMode = CssMode.NONE
            )
        )
        
        // Act
        val result = renderer.render(document, config)
        
        // Assert
        assertTrue(result.isSuccess)
        val html = result.getOrThrow()
        assertTrue(!html.contains("<style>"))
        assertTrue(!html.contains("<link rel=\"stylesheet\""))
    }
    
    @Test
    fun `should include metadata tags when enabled`() {
        // Arrange
        val document = Document(
            title = "Test",
            children = emptyList(),
            documentAttributes = mapOf(
                "author" to "John Doe",
                "description" to "A test document",
                "keywords" to "test, document, example"
            ),
            attributes = emptyMap(),
            sourceLocation = SourceLocation(1, 1)
        )
        val config = RenderConfig(
            outputOptions = OutputOptions(
                standalone = true,
                includeMetadata = true
            )
        )
        
        // Act
        val result = renderer.render(document, config)
        
        // Assert
        assertTrue(result.isSuccess)
        val html = result.getOrThrow()
        assertTrue(html.contains("<meta name=\"author\" content=\"John Doe\">"))
        assertTrue(html.contains("<meta name=\"description\" content=\"A test document\">"))
        assertTrue(html.contains("<meta name=\"keywords\" content=\"test, document, example\">"))
    }
    
    @Test
    fun `should not include metadata tags when disabled`() {
        // Arrange
        val document = Document(
            title = "Test",
            children = emptyList(),
            documentAttributes = mapOf(
                "author" to "John Doe",
                "description" to "A test document"
            ),
            attributes = emptyMap(),
            sourceLocation = SourceLocation(1, 1)
        )
        val config = RenderConfig(
            outputOptions = OutputOptions(
                standalone = true,
                includeMetadata = false
            )
        )
        
        // Act
        val result = renderer.render(document, config)
        
        // Assert
        assertTrue(result.isSuccess)
        val html = result.getOrThrow()
        assertTrue(!html.contains("<meta name=\"author\""))
        assertTrue(!html.contains("<meta name=\"description\""))
    }
    
    @Test
    fun `should use custom document title when provided`() {
        // Arrange
        val document = Document(
            title = "Original Title",
            children = emptyList(),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = SourceLocation(1, 1)
        )
        val config = RenderConfig(
            outputOptions = OutputOptions(
                standalone = true,
                documentTitle = "Custom Title"
            )
        )
        
        // Act
        val result = renderer.render(document, config)
        
        // Assert
        assertTrue(result.isSuccess)
        val html = result.getOrThrow()
        assertTrue(html.contains("<title>Custom Title</title>"))
        assertTrue(!html.contains("<title>Original Title</title>"))
    }
    
    @Test
    fun `should use document title when custom title not provided`() {
        // Arrange
        val document = Document(
            title = "Document Title",
            children = emptyList(),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = SourceLocation(1, 1)
        )
        val config = RenderConfig(
            outputOptions = OutputOptions(standalone = true)
        )
        
        // Act
        val result = renderer.render(document, config)
        
        // Assert
        assertTrue(result.isSuccess)
        val html = result.getOrThrow()
        assertTrue(html.contains("<title>Document Title</title>"))
    }
    
    @Test
    fun `should use default title when no title provided`() {
        // Arrange
        val document = Document(
            title = null,
            children = emptyList(),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = SourceLocation(1, 1)
        )
        val config = RenderConfig(
            outputOptions = OutputOptions(standalone = true)
        )
        
        // Act
        val result = renderer.render(document, config)
        
        // Assert
        assertTrue(result.isSuccess)
        val html = result.getOrThrow()
        assertTrue(html.contains("<title>Document</title>"))
    }
    
    @Test
    fun `should include custom language attribute`() {
        // Arrange
        val document = Document(
            title = "Test",
            children = emptyList(),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = SourceLocation(1, 1)
        )
        val config = RenderConfig(
            outputOptions = OutputOptions(
                standalone = true,
                language = "fr"
            )
        )
        
        // Act
        val result = renderer.render(document, config)
        
        // Assert
        assertTrue(result.isSuccess)
        val html = result.getOrThrow()
        assertTrue(html.contains("<html lang=\"fr\">"))
    }
    
    @Test
    fun `should include custom HTML attributes`() {
        // Arrange
        val document = Document(
            title = "Test",
            children = emptyList(),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = SourceLocation(1, 1)
        )
        val config = RenderConfig(
            outputOptions = OutputOptions(
                standalone = true,
                customAttributes = mapOf(
                    "data-version" to "1.0",
                    "data-theme" to "dark"
                )
            )
        )
        
        // Act
        val result = renderer.render(document, config)
        
        // Assert
        assertTrue(result.isSuccess)
        val html = result.getOrThrow()
        assertTrue(html.contains("data-version=\"1.0\""))
        assertTrue(html.contains("data-theme=\"dark\""))
    }
    
    @Test
    fun `should fail when EXTERNAL CSS mode without cssPath`() {
        // Arrange
        val document = Document(
            title = "Test",
            children = emptyList(),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = SourceLocation(1, 1)
        )
        val config = RenderConfig(
            outputOptions = OutputOptions(
                standalone = true,
                cssMode = CssMode.EXTERNAL,
                cssPath = null
            )
        )
        
        // Act
        val result = renderer.render(document, config)
        
        // Assert
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is RenderException.InvalidConfiguration)
        assertTrue(exception.message!!.contains("cssPath"))
    }
    
    @Test
    fun `should fail when language is blank`() {
        // Arrange
        val document = Document(
            title = "Test",
            children = emptyList(),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = SourceLocation(1, 1)
        )
        val config = RenderConfig(
            outputOptions = OutputOptions(
                standalone = true,
                language = ""
            )
        )
        
        // Act
        val result = renderer.render(document, config)
        
        // Assert
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is RenderException.InvalidConfiguration)
        assertTrue(exception.message!!.contains("language"))
    }
    
    @Test
    fun `should escape HTML special characters in metadata`() {
        // Arrange
        val document = Document(
            title = "Test <script>alert('xss')</script>",
            children = emptyList(),
            documentAttributes = mapOf(
                "author" to "John <Doe>",
                "description" to "A & B"
            ),
            attributes = emptyMap(),
            sourceLocation = SourceLocation(1, 1)
        )
        val config = RenderConfig(
            outputOptions = OutputOptions(
                standalone = true,
                includeMetadata = true
            )
        )
        
        // Act
        val result = renderer.render(document, config)
        
        // Assert
        assertTrue(result.isSuccess)
        val html = result.getOrThrow()
        assertTrue(html.contains("&lt;script&gt;"))
        assertTrue(html.contains("John &lt;Doe&gt;"))
        assertTrue(html.contains("A &amp; B"))
        assertTrue(!html.contains("<script>"))
    }
    
    @Test
    fun `should include custom CSS content when provided`() {
        // Arrange
        val document = Document(
            title = "Test",
            children = emptyList(),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = SourceLocation(1, 1)
        )
        val customCss = "body { background-color: red; }"
        val config = RenderConfig(
            outputOptions = OutputOptions(
                standalone = true,
                cssMode = CssMode.INLINE
            ),
            cssOptions = CssOptions(
                customCssContent = customCss,
                includeDefaultCss = false
            )
        )
        
        // Act
        val result = renderer.render(document, config)
        
        // Assert
        assertTrue(result.isSuccess)
        val html = result.getOrThrow()
        assertTrue(html.contains("<style>"))
        assertTrue(html.contains("body { background-color: red; }"))
        assertTrue(html.contains("</style>"))
    }
    
    @Test
    fun `should merge default CSS with custom CSS when both enabled`() {
        // Arrange
        val document = Document(
            title = "Test",
            children = emptyList(),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = SourceLocation(1, 1)
        )
        val customCss = "/* Custom styles */"
        val config = RenderConfig(
            outputOptions = OutputOptions(
                standalone = true,
                cssMode = CssMode.INLINE
            ),
            cssOptions = CssOptions(
                customCssContent = customCss,
                includeDefaultCss = true
            )
        )
        
        // Act
        val result = renderer.render(document, config)
        
        // Assert
        assertTrue(result.isSuccess)
        val html = result.getOrThrow()
        assertTrue(html.contains("<style>"))
        assertTrue(html.contains(".heading")) // Default CSS
        assertTrue(html.contains("/* Custom styles */")) // Custom CSS
        assertTrue(html.contains("</style>"))
    }
    
    @Test
    fun `should apply CSS variables when provided`() {
        // Arrange
        val document = Document(
            title = "Test",
            children = emptyList(),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = SourceLocation(1, 1)
        )
        val config = RenderConfig(
            outputOptions = OutputOptions(
                standalone = true,
                cssMode = CssMode.INLINE
            ),
            cssOptions = CssOptions(
                cssVariables = mapOf(
                    "--mp-color-primary" to "#ff0000",
                    "--mp-font-size-base" to "18px"
                )
            )
        )
        
        // Act
        val result = renderer.render(document, config)
        
        // Assert
        assertTrue(result.isSuccess)
        val html = result.getOrThrow()
        assertTrue(html.contains(":root {"))
        assertTrue(html.contains("--mp-color-primary: #ff0000;"))
        assertTrue(html.contains("--mp-font-size-base: 18px;"))
    }
    
    @Test
    fun `should use built-in theme when specified`() {
        // Arrange
        val document = Document(
            title = "Test",
            children = emptyList(),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = SourceLocation(1, 1)
        )
        val config = RenderConfig(
            outputOptions = OutputOptions(
                standalone = true,
                cssMode = CssMode.INLINE
            ),
            cssOptions = CssOptions(
                builtInTheme = "minimal"
            )
        )
        
        // Act
        val result = renderer.render(document, config)
        
        // Assert
        assertTrue(result.isSuccess)
        val html = result.getOrThrow()
        assertTrue(html.contains("<style>"))
        // Minimal theme should have simpler CSS
        assertTrue(html.contains(".heading"))
    }
    
    @Test
    fun `should handle CSS provider errors gracefully`() {
        // Arrange
        val document = Document(
            title = "Test",
            children = emptyList(),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = SourceLocation(1, 1)
        )
        // Provide a non-existent CSS file path
        val config = RenderConfig(
            outputOptions = OutputOptions(
                standalone = true,
                cssMode = CssMode.INLINE
            ),
            cssOptions = CssOptions(
                customCssPath = "/non/existent/file.css",
                includeDefaultCss = false
            )
        )
        
        // Act
        val result = renderer.render(document, config)
        
        // Assert
        // Should succeed but without custom CSS (error is logged as warning)
        assertTrue(result.isSuccess)
        val html = result.getOrThrow()
        assertTrue(html.contains("<!DOCTYPE html>"))
    }
    
    @Test
    fun `should handle file write errors gracefully in EXTERNAL mode`() {
        // Arrange
        val document = Document(
            title = "Test",
            children = emptyList(),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = SourceLocation(1, 1)
        )
        
        // Create a mock FileWriter that fails
        val mockFileWriter = object : FileWriter {
            override fun writeFile(path: String, content: String): Result<Unit> {
                return Result.failure(
                    CssException.LoadingFailure("Permission denied")
                )
            }
        }
        
        val renderer = DefaultHtmlRenderer(
            blockRenderer,
            inlineRenderer,
            fileWriter = mockFileWriter
        )
        
        val config = RenderConfig(
            outputOptions = OutputOptions(
                standalone = true,
                cssMode = CssMode.EXTERNAL,
                cssPath = "styles.css"
            )
        )
        
        // Act
        val result = renderer.render(document, config)
        
        // Assert
        // Should succeed even if file write fails (error is logged as warning)
        assertTrue(result.isSuccess)
        val html = result.getOrThrow()
        assertTrue(html.contains("<!DOCTYPE html>"))
        // Link tag should not be included if file write failed
        assertTrue(!html.contains("<link rel=\"stylesheet\""))
    }
    
    @Test
    fun `should write custom CSS to external file in EXTERNAL mode`() {
        // Arrange
        val document = Document(
            title = "Test",
            children = emptyList(),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = SourceLocation(1, 1)
        )
        
        // Create a mock FileWriter to capture written content
        var writtenPath: String? = null
        var writtenContent: String? = null
        val mockFileWriter = object : FileWriter {
            override fun writeFile(path: String, content: String): Result<Unit> {
                writtenPath = path
                writtenContent = content
                return Result.success(Unit)
            }
        }
        
        val renderer = DefaultHtmlRenderer(
            blockRenderer,
            inlineRenderer,
            fileWriter = mockFileWriter
        )
        
        val customCss = "body { background-color: blue; }"
        val config = RenderConfig(
            outputOptions = OutputOptions(
                standalone = true,
                cssMode = CssMode.EXTERNAL,
                cssPath = "custom.css"
            ),
            cssOptions = CssOptions(
                customCssContent = customCss,
                includeDefaultCss = false
            )
        )
        
        // Act
        val result = renderer.render(document, config)
        
        // Assert
        assertTrue(result.isSuccess)
        val html = result.getOrThrow()
        assertTrue(html.contains("<link rel=\"stylesheet\" href=\"custom.css\">"))
        
        // Verify custom CSS was written to file
        assertEquals("custom.css", writtenPath)
        assertTrue(writtenContent!!.contains(customCss))
        assertTrue(writtenContent.contains("/* Custom CSS */"))
    }
}
