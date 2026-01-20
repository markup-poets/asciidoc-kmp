package org.markup.poet.asciidoc.render

/**
 * Sanitizes content to prevent XSS attacks.
 * 
 * This interface provides methods for escaping HTML content and attribute values
 * to ensure safe rendering and prevent cross-site scripting vulnerabilities.
 */
interface HtmlEscaper {
    /**
     * Escapes HTML special characters in text content.
     * 
     * Converts:
     * - & to &amp;
     * - < to &lt;
     * - > to &gt;
     * 
     * @param text The text to escape
     * @return The escaped text safe for HTML content
     */
    fun escapeHtml(text: String): String
    
    /**
     * Escapes HTML special characters in attribute values.
     * 
     * Converts:
     * - & to &amp;
     * - < to &lt;
     * - > to &gt;
     * - " to &quot;
     * - ' to &#39;
     * 
     * @param text The text to escape
     * @return The escaped text safe for HTML attribute values
     */
    fun escapeAttribute(text: String): String
}

/**
 * Default implementation of HtmlEscaper that performs standard HTML entity escaping.
 * 
 * This implementation ensures all special HTML characters are properly escaped
 * to prevent XSS attacks and ensure valid HTML output.
 */
class DefaultHtmlEscaper : HtmlEscaper {
    override fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }
    
    override fun escapeAttribute(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}
