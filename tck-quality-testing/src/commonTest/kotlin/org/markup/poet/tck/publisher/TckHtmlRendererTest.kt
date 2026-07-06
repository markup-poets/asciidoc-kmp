package org.markup.poet.tck.publisher

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.Header
import org.markup.poet.asciidoc.asg.InlineText
import org.markup.poet.asciidoc.render.HtmlRenderer
import org.markup.poet.asciidoc.render.RenderConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for TckHtmlRenderer.
 *
 * These tests verify that the rendering wrapper correctly:
 * - Uses the configured HTML renderer
 * - Applies TCK results configuration
 * - Handles rendering errors appropriately
 * - Validates output is non-empty
 *
 * Validates: Requirements 3.1, 3.6
 */
class TckHtmlRendererTest {
    
    @Test
    fun `should render document successfully with default config`() {
        // Arrange
        val document = createMockDocument()
        val mockRenderer = MockHtmlRenderer(
            renderResult = Result.success("<html><body>Test Results</body></html>")
        )
        val renderer = TckHtmlRenderer(htmlRenderer = mockRenderer)
        
        // Act
        val result = renderer.render(document)
        
        // Assert
        assertTrue(result.isSuccess, "Rendering should succeed")
        val html = result.getOrThrow()
        assertTrue(html.contains("<html>"), "Should contain HTML")
        assertTrue(html.contains("Test Results"), "Should contain content")
    }
    
    @Test
    fun `should render document with custom title`() {
        // Arrange
        val document = createMockDocument()
        val customTitle = "Custom TCK Results - 2026-01-24"
        val mockRenderer = MockHtmlRenderer(
            renderResult = Result.success("<html><head><title>$customTitle</title></head></html>")
        )
        val renderer = TckHtmlRenderer(htmlRenderer = mockRenderer)
        
        // Act
        val result = renderer.render(document, documentTitle = customTitle)
        
        // Assert
        assertTrue(result.isSuccess, "Rendering should succeed")
        val html = result.getOrThrow()
        assertTrue(html.contains(customTitle), "Should contain custom title")
    }
    
    @Test
    fun `should use RenderConfigFactory for configuration`() {
        // Arrange
        val document = createMockDocument()
        var capturedConfig: RenderConfig? = null
        val mockRenderer = MockHtmlRenderer(
            renderResult = Result.success("<html><body>Test</body></html>"),
            onRender = { _, config -> capturedConfig = config }
        )
        val renderer = TckHtmlRenderer(htmlRenderer = mockRenderer)
        
        // Act
        renderer.render(document)
        
        // Assert
        assertTrue(capturedConfig != null, "Config should be passed to renderer")
        assertTrue(capturedConfig!!.outputOptions.standalone, "Should use standalone mode")
        assertTrue(capturedConfig!!.outputOptions.includeToc, "Should include TOC")
    }
    
    @Test
    fun `should fail when renderer produces empty HTML`() {
        // Arrange
        val document = createMockDocument()
        val mockRenderer = MockHtmlRenderer(
            renderResult = Result.success("")  // Empty HTML
        )
        val renderer = TckHtmlRenderer(htmlRenderer = mockRenderer)
        
        // Act
        val result = renderer.render(document)
        
        // Assert
        assertTrue(result.isFailure, "Should fail for empty HTML")
        val error = result.exceptionOrNull()
        assertTrue(error is RenderingException, "Should throw RenderingException")
        assertTrue(
            error?.message?.contains("empty HTML") == true,
            "Error message should mention empty HTML"
        )
    }
    
    @Test
    fun `should fail when renderer produces blank HTML`() {
        // Arrange
        val document = createMockDocument()
        val mockRenderer = MockHtmlRenderer(
            renderResult = Result.success("   \n\t  ")  // Blank HTML
        )
        val renderer = TckHtmlRenderer(htmlRenderer = mockRenderer)
        
        // Act
        val result = renderer.render(document)
        
        // Assert
        assertTrue(result.isFailure, "Should fail for blank HTML")
        val error = result.exceptionOrNull()
        assertTrue(error is RenderingException, "Should throw RenderingException")
    }
    
    @Test
    fun `should wrap renderer errors with context`() {
        // Arrange
        val document = createMockDocument()
        val originalError = Exception("Invalid AST structure")
        val mockRenderer = MockHtmlRenderer(
            renderResult = Result.failure(originalError)
        )
        val renderer = TckHtmlRenderer(htmlRenderer = mockRenderer)
        
        // Act
        val result = renderer.render(document)
        
        // Assert
        assertTrue(result.isFailure, "Should fail when renderer fails")
        val error = result.exceptionOrNull()
        assertTrue(error is RenderingException, "Should throw RenderingException")
        assertTrue(
            error?.message?.contains("HTML rendering failed") == true,
            "Error message should indicate rendering failure"
        )
        assertTrue(
            error?.message?.contains("Invalid AST structure") == true,
            "Error message should include original error message"
        )
        assertEquals(originalError, error?.cause, "Should preserve original error as cause")
    }
    
    @Test
    fun `should catch unexpected exceptions during rendering`() {
        // Arrange
        val document = createMockDocument()
        val mockRenderer = MockHtmlRenderer(
            throwException = RuntimeException("Unexpected error")
        )
        val renderer = TckHtmlRenderer(htmlRenderer = mockRenderer)
        
        // Act
        val result = renderer.render(document)
        
        // Assert
        assertTrue(result.isFailure, "Should fail when exception is thrown")
        val error = result.exceptionOrNull()
        assertTrue(error is RenderingException, "Should throw RenderingException")
        assertTrue(
            error?.message?.contains("Unexpected error during HTML rendering") == true,
            "Error message should indicate unexpected error"
        )
    }
    
    @Test
    fun `should render with custom config`() {
        // Arrange
        val document = createMockDocument()
        val customConfig = RenderConfigFactory.createMinimalConfig()
        var capturedConfig: RenderConfig? = null
        val mockRenderer = MockHtmlRenderer(
            renderResult = Result.success("<html><body>Test</body></html>"),
            onRender = { _, config -> capturedConfig = config }
        )
        val renderer = TckHtmlRenderer(htmlRenderer = mockRenderer)
        
        // Act
        val result = renderer.renderWithConfig(document, customConfig)
        
        // Assert
        assertTrue(result.isSuccess, "Rendering should succeed")
        assertEquals(customConfig, capturedConfig, "Should use provided config")
    }
    
    @Test
    fun `should validate non-empty output with custom config`() {
        // Arrange
        val document = createMockDocument()
        val customConfig = RenderConfigFactory.createMinimalConfig()
        val mockRenderer = MockHtmlRenderer(
            renderResult = Result.success("")  // Empty HTML
        )
        val renderer = TckHtmlRenderer(htmlRenderer = mockRenderer)
        
        // Act
        val result = renderer.renderWithConfig(document, customConfig)
        
        // Assert
        assertTrue(result.isFailure, "Should fail for empty HTML even with custom config")
    }
    
    @Test
    fun `should handle renderer failure with custom config`() {
        // Arrange
        val document = createMockDocument()
        val customConfig = RenderConfigFactory.createMinimalConfig()
        val originalError = Exception("Rendering error")
        val mockRenderer = MockHtmlRenderer(
            renderResult = Result.failure(originalError)
        )
        val renderer = TckHtmlRenderer(htmlRenderer = mockRenderer)
        
        // Act
        val result = renderer.renderWithConfig(document, customConfig)
        
        // Assert
        assertTrue(result.isFailure, "Should fail when renderer fails")
        val error = result.exceptionOrNull()
        assertTrue(error is RenderingException, "Should throw RenderingException")
        assertEquals(originalError, error?.cause, "Should preserve original error")
    }
    
    // Helper functions
    
    private fun createMockDocument(): AsgDocument {
        return AsgDocument(
            attributes = mapOf(
                "author" to "TCK Bot",
                "description" to "Test results"
            ),
            header = Header(title = listOf(InlineText("TCK Results"))),
            blocks = emptyList()
        )
    }
}

/**
 * Mock HTML renderer for testing.
 *
 * This mock allows us to control the rendering behavior and verify that
 * the TckHtmlRenderer correctly interacts with the underlying renderer.
 */
private class MockHtmlRenderer(
    private val renderResult: Result<String> = Result.success("<html></html>"),
    private val throwException: Exception? = null,
    private val onRender: ((AsgDocument, RenderConfig) -> Unit)? = null
) : HtmlRenderer {
    
    override fun render(document: AsgDocument, config: RenderConfig): Result<String> {
        // Call the callback if provided (for capturing config)
        onRender?.invoke(document, config)
        
        // Throw exception if configured
        throwException?.let { throw it }
        
        // Return the configured result
        return renderResult
    }
}
