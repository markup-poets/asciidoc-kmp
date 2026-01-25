package org.markup.poet.tck.publisher

import org.markup.poet.asciidoc.ast.Document
import org.markup.poet.asciidoc.render.DefaultHtmlRenderer
import org.markup.poet.asciidoc.render.HtmlRenderer
import org.markup.poet.asciidoc.render.RenderConfig

/**
 * Wrapper for rendering TCK results documents to HTML.
 *
 * This class provides a simplified interface for rendering TCK results by:
 * - Using pre-configured RenderConfig optimized for TCK results
 * - Wrapping the DefaultHtmlRenderer with appropriate error handling
 * - Providing clear error messages for rendering failures
 *
 * The wrapper is part of the dogfooding pipeline where we use our own HTML
 * renderer to publish TCK test results, validating that our renderer works
 * correctly on real-world content.
 *
 * ## Usage Example
 * ```kotlin
 * val renderer = TckHtmlRenderer()
 * val html = renderer.render(document).getOrElse { error ->
 *     println("Rendering failed: ${error.message}")
 *     return
 * }
 * ```
 *
 * Validates: Requirements 3.1, 3.6
 *
 * @property htmlRenderer The underlying HTML renderer (defaults to DefaultHtmlRenderer)
 * @property configFactory Factory for creating render configurations (defaults to RenderConfigFactory)
 */
class TckHtmlRenderer(
    private val htmlRenderer: HtmlRenderer = createDefaultRenderer(),
    private val configFactory: RenderConfigFactory = RenderConfigFactory
) {
    
    /**
     * Renders a Document AST to HTML with TCK results styling.
     *
     * This method:
     * 1. Creates a RenderConfig optimized for TCK results (KotlinTheme, inline CSS, etc.)
     * 2. Renders the document using the configured HTML renderer
     * 3. Handles any rendering errors and wraps them with context
     *
     * The rendering process validates that our HTML renderer can successfully
     * process the TCK results document structure, including:
     * - Document title and metadata
     * - Summary statistics tables
     * - Test results organized by category
     * - Failed test details with error messages
     * - Custom CSS styling for test status indicators
     *
     * ## Error Handling
     * Rendering errors are wrapped in a Result type. Common failure scenarios:
     * - Invalid AST structure (malformed document)
     * - Missing required document elements
     * - CSS generation failures
     * - Unexpected rendering exceptions
     *
     * Validates: Requirements 3.1, 3.6
     *
     * @param document The Document AST to render (typically generated from TCK results)
     * @param documentTitle Optional custom document title (overrides default)
     * @return Result containing the rendered HTML string, or an error if rendering failed
     */
    fun render(
        document: Document,
        documentTitle: String? = null
    ): Result<String> {
        return try {
            // Create render configuration optimized for TCK results
            val config = if (documentTitle != null) {
                configFactory.createTckResultsConfig(documentTitle)
            } else {
                configFactory.createTckResultsConfig()
            }
            
            // Render the document
            val renderResult = htmlRenderer.render(document, config)
            
            // Handle rendering result
            renderResult.fold(
                onSuccess = { html ->
                    // Validate that we got non-empty HTML
                    if (html.isBlank()) {
                        Result.failure(
                            RenderingException(
                                message = "Renderer produced empty HTML output",
                                cause = null
                            )
                        )
                    } else {
                        Result.success(html)
                    }
                },
                onFailure = { error ->
                    // Wrap rendering error with context
                    Result.failure(
                        RenderingException(
                            message = "HTML rendering failed: ${error.message}",
                            cause = error
                        )
                    )
                }
            )
        } catch (e: Exception) {
            // Catch any unexpected exceptions during rendering
            Result.failure(
                RenderingException(
                    message = "Unexpected error during HTML rendering: ${e.message}",
                    cause = e
                )
            )
        }
    }
    
    /**
     * Renders a Document AST to HTML with a custom RenderConfig.
     *
     * This method allows full control over the rendering configuration,
     * useful for testing or special rendering scenarios.
     *
     * @param document The Document AST to render
     * @param config Custom render configuration
     * @return Result containing the rendered HTML string, or an error if rendering failed
     */
    fun renderWithConfig(
        document: Document,
        config: RenderConfig
    ): Result<String> {
        return try {
            val renderResult = htmlRenderer.render(document, config)
            
            renderResult.fold(
                onSuccess = { html ->
                    if (html.isBlank()) {
                        Result.failure(
                            RenderingException(
                                message = "Renderer produced empty HTML output",
                                cause = null
                            )
                        )
                    } else {
                        Result.success(html)
                    }
                },
                onFailure = { error ->
                    Result.failure(
                        RenderingException(
                            message = "HTML rendering failed: ${error.message}",
                            cause = error
                        )
                    )
                }
            )
        } catch (e: Exception) {
            Result.failure(
                RenderingException(
                    message = "Unexpected error during HTML rendering: ${e.message}",
                    cause = e
                )
            )
        }
    }
    
    companion object {
        /**
         * Creates a default HTML renderer instance.
         *
         * This is a placeholder that will be replaced with actual renderer
         * initialization once the renderer implementation is complete.
         *
         * @return Default HTML renderer instance
         */
        private fun createDefaultRenderer(): HtmlRenderer {
            // TODO: Replace with actual DefaultHtmlRenderer initialization
            // For now, return a stub that will be replaced when renderer is implemented
            return object : HtmlRenderer {
                override fun render(document: Document, config: RenderConfig): Result<String> {
                    return Result.failure(
                        NotImplementedError("DefaultHtmlRenderer not yet fully implemented")
                    )
                }
            }
        }
    }
}

/**
 * Exception thrown when HTML rendering fails.
 *
 * This exception wraps rendering errors with additional context about the
 * failure, making it easier to diagnose issues in the publishing pipeline.
 *
 * @property message Description of the rendering failure
 * @property cause The underlying exception that caused the failure (if any)
 */
class RenderingException(
    message: String,
    cause: Throwable?
) : Exception(message, cause)
