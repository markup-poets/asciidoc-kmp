package org.markup.poet.asciidoc.render

/**
 * Constructs HTML strings with proper structure and nesting.
 * 
 * This interface provides a DSL-style API for building HTML content with
 * automatic escaping of text content and attribute values.
 */
interface HtmlBuilder {
    /**
     * Builds HTML content using a DSL-style block.
     * 
     * Example:
     * ```
     * val html = builder.build {
     *     openTag("p", mapOf("class" to "paragraph"))
     *     text("Hello, world!")
     *     closeTag("p")
     * }
     * ```
     * 
     * @param block The DSL block that constructs the HTML
     * @return The generated HTML string
     */
    fun build(block: HtmlBuilder.() -> Unit): String
    
    /**
     * Opens an HTML tag with optional attributes.
     * 
     * Attributes with empty values are omitted from the output.
     * Attribute values are automatically escaped using escapeAttribute().
     * 
     * @param name The tag name (e.g., "p", "div", "h1")
     * @param attributes Map of attribute names to values
     */
    fun openTag(name: String, attributes: Map<String, String> = emptyMap())
    
    /**
     * Closes an HTML tag.
     * 
     * @param name The tag name to close (e.g., "p", "div", "h1")
     */
    fun closeTag(name: String)
    
    /**
     * Adds text content to the HTML.
     * 
     * Note: This method does NOT automatically escape the text.
     * Callers should use escape() or escapeAttribute() as needed.
     * 
     * @param content The text content to add
     */
    fun text(content: String)
    
    /**
     * Escapes text content for safe inclusion in HTML.
     * 
     * Converts special HTML characters to their entity equivalents:
     * - & to &amp;
     * - < to &lt;
     * - > to &gt;
     * 
     * @param text The text to escape
     * @return The escaped text
     */
    fun escape(text: String): String
    
    /**
     * Escapes text for safe inclusion in HTML attribute values.
     * 
     * Converts special HTML characters to their entity equivalents:
     * - & to &amp;
     * - < to &lt;
     * - > to &gt;
     * - " to &quot;
     * - ' to &#39;
     * 
     * @param text The text to escape
     * @return The escaped text
     */
    fun escapeAttribute(text: String): String
}

/**
 * Default implementation of HtmlBuilder that constructs HTML strings
 * with proper structure and automatic escaping.
 * 
 * This implementation uses a StringBuilder internally for efficient
 * string concatenation and integrates with HtmlEscaper for security.
 * 
 * @param escaper The HtmlEscaper to use for content and attribute escaping
 */
class DefaultHtmlBuilder(
    private val escaper: HtmlEscaper
) : HtmlBuilder {
    private val buffer = StringBuilder()
    
    override fun build(block: HtmlBuilder.() -> Unit): String {
        buffer.clear()
        block()
        return buffer.toString()
    }
    
    override fun openTag(name: String, attributes: Map<String, String>) {
        buffer.append("<").append(name)
        attributes.forEach { (key, value) ->
            if (value.isNotEmpty()) {
                buffer.append(" ").append(key).append("=\"")
                buffer.append(escapeAttribute(value))
                buffer.append("\"")
            }
        }
        buffer.append(">")
    }
    
    override fun closeTag(name: String) {
        buffer.append("</").append(name).append(">")
    }
    
    override fun text(content: String) {
        buffer.append(content)
    }
    
    override fun escape(text: String): String {
        return escaper.escapeHtml(text)
    }
    
    override fun escapeAttribute(text: String): String {
        return escaper.escapeAttribute(text)
    }
}
