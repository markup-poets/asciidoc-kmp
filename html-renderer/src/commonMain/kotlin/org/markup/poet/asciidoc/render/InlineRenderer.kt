package org.markup.poet.asciidoc.render

import org.markup.poet.asciidoc.ast.*

/**
 * Renders inline elements to HTML.
 * 
 * InlineRenderer handles text-level markup like emphasis, strong text, code spans,
 * links, and images. It processes nested inline elements recursively to maintain
 * proper HTML structure.
 * 
 * Implementations must ensure:
 * - All text content is properly escaped to prevent XSS attacks
 * - URLs are sanitized to prevent javascript: and data: URI schemes
 * - Images include alt text for accessibility
 * - Nested inline elements are rendered correctly
 */
interface InlineRenderer {
    /**
     * Renders an inline element to HTML.
     * 
     * @param inline The inline element to render
     * @param context The rendering context containing configuration and state
     * @return The HTML string representation of the inline element
     */
    fun render(inline: InlineElement, context: RenderContext): String
}

/**
 * Default implementation of InlineRenderer that produces semantic HTML5 output.
 * 
 * This implementation:
 * - Converts Text nodes to escaped text content
 * - Converts Strong nodes to `<strong>` elements
 * - Converts Emphasis nodes to `<em>` elements
 * - Converts Code nodes to `<code>` elements
 * - Converts Link nodes to `<a>` elements with URL sanitization
 * - Converts Image nodes to `<img>` elements with alt text
 * - Handles nested inline elements recursively
 * - Logs warnings for unknown inline element types
 * 
 * Note: The design document mentions Bold, Italic, Subscript, and Superscript,
 * but the actual AST uses Strong and Emphasis. This implementation follows the
 * actual AST structure while maintaining semantic correctness.
 * 
 * @param builder The HtmlBuilder to use for constructing HTML
 */
class DefaultInlineRenderer(
    private val builder: HtmlBuilder
) : InlineRenderer {
    
    override fun render(inline: InlineElement, context: RenderContext): String {
        return when (inline) {
            is Text -> renderText(inline)
            is Strong -> renderStrong(inline, context)
            is Emphasis -> renderEmphasis(inline, context)
            is Code -> renderCode(inline)
            is Link -> renderLink(inline, context)
            is Image -> renderImage(inline)
            is AttributeReference -> renderAttributeReference(inline, context)
            is Callout -> renderCallout(inline)
            is CrossReference -> renderCrossReference(inline, context)
            is MacroInvocation -> renderMacroInvocation(inline, context)
            is BibliographyReference -> renderBibliographyReference(inline)
            is FootnoteReference -> renderFootnoteReference(inline)
        }
    }
    
    /**
     * Renders plain text with HTML escaping.
     * 
     * Validates: Requirements 1.3, 3.1, 4.1, 4.2, 4.3
     */
    private fun renderText(text: Text): String {
        return builder.escape(text.content)
    }
    
    /**
     * Renders strong (bold) text as `<strong>` element.
     * 
     * Validates: Requirements 1.3, 3.1
     */
    private fun renderStrong(strong: Strong, context: RenderContext): String {
        val content = renderNestedInline(strong.content, context)
        return "<strong>$content</strong>"
    }
    
    /**
     * Renders emphasized (italic) text as `<em>` element.
     * 
     * Validates: Requirements 1.3, 3.2
     */
    private fun renderEmphasis(emphasis: Emphasis, context: RenderContext): String {
        val content = renderNestedInline(emphasis.content, context)
        return "<em>$content</em>"
    }
    
    /**
     * Renders inline code as `<code>` element with escaped content.
     * 
     * Validates: Requirements 1.3, 3.3
     */
    private fun renderCode(code: Code): String {
        val escaped = builder.escape(code.content)
        return "<code>$escaped</code>"
    }
    
    /**
     * Renders a hyperlink as `<a>` element with URL sanitization.
     * 
     * Sanitizes URLs to prevent javascript: and data: URI schemes.
     * Escapes the link text to prevent XSS attacks.
     * 
     * Validates: Requirements 1.3, 3.4, 4.6
     */
    private fun renderLink(link: Link, context: RenderContext): String {
        val sanitizedUrl = sanitizeUrl(link.url)
        val escapedUrl = builder.escapeAttribute(sanitizedUrl)
        val escapedText = builder.escape(link.text)
        
        val titleAttr = link.attributes["title"]?.let { title ->
            " title=\"${builder.escapeAttribute(title)}\""
        } ?: ""
        
        return "<a href=\"$escapedUrl\"$titleAttr>$escapedText</a>"
    }
    
    /**
     * Renders an inline image as `<img>` element with alt text.
     * 
     * Always includes alt attribute for accessibility, even if empty.
     * 
     * Validates: Requirements 1.3, 3.5, 8.1
     */
    private fun renderImage(image: Image): String {
        val escapedSrc = builder.escapeAttribute(image.path)
        val escapedAlt = builder.escapeAttribute(image.altText)
        
        val titleAttr = image.attributes["title"]?.let { title ->
            " title=\"${builder.escapeAttribute(title)}\""
        } ?: ""
        
        return "<img src=\"$escapedSrc\" alt=\"$escapedAlt\"$titleAttr>"
    }
    
    /**
     * Renders an attribute reference.
     * 
     * In the rendering phase, attribute references should have been resolved
     * by the document-processing module. If we encounter one here, we render
     * it as-is with a warning.
     * 
     * Validates: Requirements 11.1
     */
    private fun renderAttributeReference(ref: AttributeReference, context: RenderContext): String {
        context.logWarning("Unresolved attribute reference: {${ref.key}}")
        return builder.escape("{${ref.key}}")
    }
    
    /**
     * Renders a callout number.
     * 
     * Callouts are rendered as `<span>` elements with a specific class for styling.
     */
    private fun renderCallout(callout: Callout): String {
        return "<span class=\"callout\">&lt;${callout.number}&gt;</span>"
    }
    
    /**
     * Renders a cross-reference as an anchor link.
     * 
     * Uses the target ID for the href attribute. If custom text is provided,
     * uses it; otherwise uses the target ID as the link text.
     * 
     * Validates: Requirements 11.2
     */
    private fun renderCrossReference(ref: CrossReference, context: RenderContext): String {
        val linkText = ref.customText ?: ref.targetId
        val escapedText = builder.escape(linkText)
        val escapedId = builder.escapeAttribute(ref.targetId)
        
        return "<a href=\"#$escapedId\">$escapedText</a>"
    }
    
    /**
     * Renders a macro invocation.
     * 
     * In the rendering phase, macros should have been expanded by the
     * document-processing module. If we encounter one here, we render
     * a placeholder with a warning.
     * 
     * Validates: Requirements 11.5
     */
    private fun renderMacroInvocation(macro: MacroInvocation, context: RenderContext): String {
        context.logWarning("Unexpanded macro: ${macro.macroName}")
        val escapedName = builder.escape("${macro.macroName}[]")
        return "<span class=\"macro-placeholder\">$escapedName</span>"
    }
    
    /**
     * Renders a list of nested inline elements recursively.
     * 
     * This method is used for inline elements that contain other inline elements,
     * such as Strong and Emphasis.
     * 
     * Validates: Requirements 1.4, 3.8
     */
    private fun renderNestedInline(content: List<InlineElement>, context: RenderContext): String {
        return content.joinToString("") { render(it, context) }
    }
    
    /**
     * Sanitizes a URL to prevent XSS attacks.
     * 
     * Blocks javascript: and data: URI schemes by replacing them with "#".
     * This prevents execution of malicious scripts through link URLs.
     * 
     * Validates: Requirements 4.6
     */
    private fun sanitizeUrl(url: String): String {
        val trimmed = url.trim()
        val lower = trimmed.lowercase()
        
        // Block dangerous URI schemes
        if (lower.startsWith("javascript:") || lower.startsWith("data:")) {
            return "#"
        }
        
        return trimmed
    }
    
    /**
     * Render a bibliography reference
     */
    private fun renderBibliographyReference(ref: BibliographyReference): String {
        return """<a href="#${ref.citationId}" class="bibliography-ref">[${ref.citationId}]</a>"""
    }
    
    /**
     * Render a footnote reference
     */
    private fun renderFootnoteReference(ref: FootnoteReference): String {
        return """<sup class="footnote-ref"><a href="#fn-${ref.id}">${ref.id}</a></sup>"""
    }
}
