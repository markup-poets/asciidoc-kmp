package org.markup.poet.asciidoc.render

import org.markup.poet.asciidoc.ast.Document

/**
 * Main entry point for rendering AsciiDoc AST to HTML.
 * 
 * This interface will be implemented in subsequent tasks.
 */
interface HtmlRenderer {
    /**
     * Renders an AsciiDoc document AST to HTML.
     * 
     * @param document The root document node
     * @param config Rendering configuration
     * @return Result containing HTML string or error
     */
    fun render(document: Document, config: RenderConfig = RenderConfig.default()): Result<String>
}

/**
 * Configuration for rendering behavior.
 * 
 * Controls how the HTML renderer processes AST nodes and generates output,
 * including output format, CSS inclusion, theming, and extensibility options.
 * 
 * @param outputOptions Controls standalone vs fragment mode and CSS inclusion
 * @param theme Theme for CSS class generation and styling
 * @param customRenderers Custom renderers for specific node types (keyed by node class simple name)
 * @param attributeHandlers Custom attribute handlers for processing node attributes
 * @param documentTemplate Custom template for document structure (standalone mode only)
 */
data class RenderConfig(
    val outputOptions: OutputOptions = OutputOptions.default(),
    val theme: Theme = Theme.default(),
    val customRenderers: Map<String, CustomRenderer> = emptyMap(),
    val attributeHandlers: Map<String, AttributeHandler> = emptyMap(),
    val documentTemplate: DocumentTemplate? = null
) {
    companion object {
        fun default() = RenderConfig()
    }
}

/**
 * Output options controlling document structure and CSS inclusion.
 * 
 * These options determine whether to generate a complete HTML document or just
 * a fragment, how to include CSS styles, and various metadata settings.
 * 
 * @param standalone If true, generates complete HTML with <html>, <head>, <body>; if false, generates only body content
 * @param cssMode Controls how CSS is included in the output (NONE, INLINE, or EXTERNAL)
 * @param cssPath Path to external CSS file (required when cssMode is EXTERNAL)
 * @param includeMetadata If true, includes meta tags for author, description, keywords
 * @param includeToc If true, generates and includes a table of contents at the beginning of the document
 * @param documentTitle Optional document title for <title> tag (overrides AST title if provided)
 * @param language Language code for the lang attribute on <html> element
 * @param customAttributes Custom HTML attributes to apply to the root element
 */
data class OutputOptions(
    val standalone: Boolean = true,
    val cssMode: CssMode = CssMode.INLINE,
    val cssPath: String? = null,
    val includeMetadata: Boolean = true,
    val includeToc: Boolean = false,
    val documentTitle: String? = null,
    val language: String = "en",
    val customAttributes: Map<String, String> = emptyMap()
) {
    companion object {
        fun default() = OutputOptions()
    }
}

/**
 * CSS inclusion mode for rendered HTML output.
 * 
 * Determines how CSS styles are included in the generated HTML:
 * - NONE: No CSS included, produces unstyled semantic HTML
 * - INLINE: CSS included in a <style> tag within the document
 * - EXTERNAL: CSS referenced via <link> tag (requires cssPath in OutputOptions)
 */
enum class CssMode {
    /** No CSS included in output */
    NONE,
    
    /** CSS included in <style> tag */
    INLINE,
    
    /** CSS referenced via <link> tag */
    EXTERNAL
}

/**
 * Default implementation of HtmlRenderer.
 * 
 * This implementation orchestrates the complete rendering process:
 * 1. Validates configuration
 * 2. Creates rendering context
 * 3. Uses AstVisitor to traverse and render the AST
 * 4. Wraps content in document structure (standalone mode) or returns fragment
 * 5. Includes metadata, CSS, and custom attributes as configured
 * 
 * The renderer produces semantic HTML5 output with proper escaping for security.
 * It supports both standalone documents (complete HTML with head/body) and
 * fragments (body content only).
 * 
 * Validates: Requirements 1.1, 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 7.1, 7.2, 7.3, 7.4, 7.6, 12.4
 * 
 * @param blockRenderer The renderer for block-level elements
 * @param inlineRenderer The renderer for inline elements
 * @param escaper The HTML escaper for security (defaults to DefaultHtmlEscaper)
 */
class DefaultHtmlRenderer(
    private val blockRenderer: BlockRenderer,
    private val inlineRenderer: InlineRenderer,
    private val escaper: HtmlEscaper = DefaultHtmlEscaper()
) : HtmlRenderer {
    
    override fun render(document: Document, config: RenderConfig): Result<String> {
        // Validate configuration
        val validationError = validateConfig(config)
        if (validationError != null) {
            return Result.failure(RenderException.InvalidConfiguration(validationError.first, validationError.second))
        }
        
        return try {
            // Create rendering context
            val context = RenderContext(config)
            
            // Create visitor and render document body
            val visitor = AstVisitor(blockRenderer, inlineRenderer, context)
            val bodyHtml = visitor.visit(document)
            
            // Wrap in document structure or return fragment
            val html = if (config.outputOptions.standalone) {
                // Use custom template if provided, otherwise use default
                if (config.documentTemplate != null) {
                    config.documentTemplate.generateDocument(bodyHtml, document, config, context)
                } else {
                    wrapInDocument(bodyHtml, document, config, context)
                }
            } else {
                bodyHtml
            }
            
            Result.success(html)
        } catch (e: RenderException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(RenderException.RenderingFailure("Unexpected error during rendering: ${e.message}"))
        }
    }
    
    /**
     * Validates the rendering configuration.
     * 
     * Checks for invalid settings such as:
     * - EXTERNAL CSS mode without cssPath
     * - Invalid language codes
     * - Other configuration inconsistencies
     * 
     * @param config The configuration to validate
     * @return Pair of (setting name, reason) if invalid, null if valid
     */
    private fun validateConfig(config: RenderConfig): Pair<String, String>? {
        val options = config.outputOptions
        
        // Validate CSS configuration
        if (options.cssMode == CssMode.EXTERNAL && options.cssPath.isNullOrBlank()) {
            return Pair("cssPath", "cssPath is required when cssMode is EXTERNAL")
        }
        
        // Validate language code (basic check)
        if (options.language.isBlank()) {
            return Pair("language", "language code cannot be blank")
        }
        
        return null
    }
    
    /**
     * Wraps the rendered body content in a complete HTML document structure.
     * 
     * Generates:
     * - <!DOCTYPE html> declaration
     * - <html> element with lang attribute and custom attributes
     * - <head> section with metadata, title, and CSS
     * - <body> section with rendered content
     * 
     * Validates: Requirements 5.1, 5.3, 5.4, 5.5, 5.6, 5.7, 7.1, 7.2, 7.3, 7.4, 7.6
     * 
     * @param bodyContent The rendered body HTML
     * @param document The source document for metadata
     * @param config The rendering configuration
     * @param context The rendering context
     * @return Complete HTML document string
     */
    private fun wrapInDocument(
        bodyContent: String,
        document: Document,
        config: RenderConfig,
        context: RenderContext
    ): String {
        val options = config.outputOptions
        val builder = StringBuilder()
        
        // DOCTYPE
        builder.append("<!DOCTYPE html>\n")
        
        // HTML opening tag with lang attribute and custom attributes
        builder.append("<html lang=\"${escaper.escapeAttribute(options.language)}\"")
        options.customAttributes.forEach { (key, value) ->
            builder.append(" $key=\"${escaper.escapeAttribute(value)}\"")
        }
        builder.append(">\n")
        
        // HEAD section
        builder.append("<head>\n")
        builder.append("  <meta charset=\"UTF-8\">\n")
        builder.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
        
        // Title
        val title = options.documentTitle ?: document.title ?: "Document"
        builder.append("  <title>${escaper.escapeHtml(title)}</title>\n")
        
        // Metadata tags (if enabled)
        if (options.includeMetadata) {
            // Author
            document.getAttribute("author")?.let { author ->
                builder.append("  <meta name=\"author\" content=\"${escaper.escapeAttribute(author)}\">\n")
            }
            
            // Description
            document.getAttribute("description")?.let { description ->
                builder.append("  <meta name=\"description\" content=\"${escaper.escapeAttribute(description)}\">\n")
            }
            
            // Keywords
            document.getAttribute("keywords")?.let { keywords ->
                builder.append("  <meta name=\"keywords\" content=\"${escaper.escapeAttribute(keywords)}\">\n")
            }
        }
        
        // CSS inclusion
        when (options.cssMode) {
            CssMode.INLINE -> {
                builder.append("  <style>\n")
                builder.append(config.theme.getCss())
                builder.append("\n  </style>\n")
            }
            CssMode.EXTERNAL -> {
                options.cssPath?.let { path ->
                    builder.append("  <link rel=\"stylesheet\" href=\"${escaper.escapeAttribute(path)}\">\n")
                }
            }
            CssMode.NONE -> {
                // No CSS
            }
        }
        
        builder.append("</head>\n")
        
        // BODY section
        builder.append("<body>\n")
        
        // Include table of contents if requested
        if (options.includeToc) {
            val toc = context.generateToc(escaper)
            if (toc.isNotEmpty()) {
                builder.append(toc)
                builder.append("\n")
            }
        }
        
        builder.append(bodyContent)
        builder.append("\n</body>\n")
        
        // Close HTML
        builder.append("</html>")
        
        return builder.toString()
    }
}

/**
 * Exception types for rendering errors.
 * 
 * These exceptions provide specific error information for different failure modes
 * during the rendering process.
 */
sealed class RenderException(message: String) : Exception(message) {
    /**
     * Thrown when the AST structure is invalid or malformed.
     * 
     * @param nodeName The name of the problematic node type
     * @param reason Description of why the AST is invalid
     */
    data class InvalidAst(val nodeName: String, val reason: String) : 
        RenderException("Invalid AST structure at $nodeName: $reason")
    
    /**
     * Thrown when the rendering configuration is invalid.
     * 
     * @param setting The name of the invalid configuration setting
     * @param reason Description of why the configuration is invalid
     */
    data class InvalidConfiguration(val setting: String, val reason: String) : 
        RenderException("Invalid configuration for $setting: $reason")
    
    /**
     * Thrown when HTML validation fails.
     * 
     * @param html The generated HTML that failed validation
     * @param errors List of validation error messages
     */
    data class ValidationFailure(val html: String, val errors: List<String>) : 
        RenderException("Generated HTML failed validation: ${errors.joinToString(", ")}")
    
    /**
     * Thrown when an unexpected error occurs during rendering.
     * 
     * @param reason Description of the rendering failure
     */
    data class RenderingFailure(val reason: String) :
        RenderException("Rendering failed: $reason")
}
