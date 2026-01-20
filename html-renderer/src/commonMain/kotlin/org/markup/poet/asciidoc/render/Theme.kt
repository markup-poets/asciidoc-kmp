package org.markup.poet.asciidoc.render

/**
 * Defines CSS classes and styling for rendered HTML elements.
 * 
 * A Theme provides CSS class names for different element types and can generate
 * CSS styles to be included in the rendered HTML. This allows for customizable
 * visual presentation while maintaining semantic HTML structure.
 * 
 * Implementations should provide consistent class naming conventions and
 * corresponding CSS rules that work together to style the rendered document.
 */
interface Theme {
    /**
     * Returns CSS classes for heading elements.
     * 
     * @param level The heading level (1-6)
     * @return CSS class string to apply to the heading element
     */
    fun headingClasses(level: Int): String
    
    /**
     * Returns CSS classes for paragraph elements.
     * 
     * @return CSS class string to apply to paragraph elements
     */
    fun paragraphClasses(): String
    
    /**
     * Returns CSS classes for code block elements.
     * 
     * @return CSS class string to apply to code block (pre/code) elements
     */
    fun codeBlockClasses(): String
    
    /**
     * Returns CSS classes for table elements.
     * 
     * @return CSS class string to apply to table elements
     */
    fun tableClasses(): String
    
    /**
     * Returns CSS classes for list elements.
     * 
     * @return CSS class string to apply to list (ul/ol) elements
     */
    fun listClasses(): String
    
    /**
     * Returns CSS classes for quote/blockquote elements.
     * 
     * @return CSS class string to apply to blockquote elements
     */
    fun quoteClasses(): String
    
    /**
     * Returns CSS classes for admonition blocks.
     * 
     * Admonitions are special blocks that highlight important information
     * (e.g., NOTE, TIP, WARNING, IMPORTANT, CAUTION).
     * 
     * @param type The admonition type (e.g., "note", "warning", "tip")
     * @return CSS class string to apply to the admonition element
     */
    fun admonitionClasses(type: String): String
    
    /**
     * Returns the complete CSS stylesheet for this theme.
     * 
     * The returned CSS should include rules for all classes returned by
     * the other methods in this interface. This CSS can be included inline
     * in a <style> tag or written to an external file.
     * 
     * @return CSS stylesheet as a string
     */
    fun getCss(): String
    
    companion object {
        /**
         * Returns the default theme implementation.
         * 
         * @return A DefaultTheme instance with minimal, clean styling
         */
        fun default(): Theme = DefaultTheme()
    }
}

/**
 * Default theme implementation with minimal, clean styling.
 * 
 * This theme provides a simple, readable appearance suitable for most documents.
 * It uses semantic class names and provides basic styling for all supported
 * element types without being overly opinionated about visual design.
 * 
 * Class naming convention:
 * - Base classes describe the element type (e.g., "heading", "paragraph")
 * - Modifier classes add specificity (e.g., "heading-1", "heading-2")
 * - Admonition classes include type (e.g., "admonition-note", "admonition-warning")
 */
class DefaultTheme : Theme {
    override fun headingClasses(level: Int): String {
        return "heading heading-$level"
    }
    
    override fun paragraphClasses(): String {
        return "paragraph"
    }
    
    override fun codeBlockClasses(): String {
        return "code-block"
    }
    
    override fun tableClasses(): String {
        return "table"
    }
    
    override fun listClasses(): String {
        return "list"
    }
    
    override fun quoteClasses(): String {
        return "quote"
    }
    
    override fun admonitionClasses(type: String): String {
        return "admonition admonition-$type"
    }
    
    override fun getCss(): String {
        return """
            /* Headings */
            .heading {
                margin: 1em 0 0.5em;
                font-weight: bold;
                line-height: 1.2;
            }
            .heading-1 { font-size: 2em; }
            .heading-2 { font-size: 1.5em; }
            .heading-3 { font-size: 1.25em; }
            .heading-4 { font-size: 1.1em; }
            .heading-5 { font-size: 1em; }
            .heading-6 { font-size: 0.9em; }
            
            /* Paragraphs */
            .paragraph {
                margin: 0.5em 0;
                line-height: 1.6;
            }
            
            /* Code blocks */
            .code-block {
                background: #f5f5f5;
                border: 1px solid #ddd;
                border-radius: 4px;
                padding: 1em;
                margin: 1em 0;
                overflow-x: auto;
                font-family: monospace;
                font-size: 0.9em;
            }
            .code-block code {
                background: none;
                padding: 0;
                border: none;
            }
            
            /* Tables */
            .table {
                border-collapse: collapse;
                width: 100%;
                margin: 1em 0;
            }
            .table th,
            .table td {
                border: 1px solid #ddd;
                padding: 0.5em;
                text-align: left;
            }
            .table th {
                background: #f5f5f5;
                font-weight: bold;
            }
            .table tr:nth-child(even) {
                background: #fafafa;
            }
            
            /* Lists */
            .list {
                margin: 0.5em 0;
                padding-left: 2em;
            }
            .list li {
                margin: 0.25em 0;
            }
            
            /* Quotes */
            .quote {
                border-left: 4px solid #ddd;
                padding-left: 1em;
                margin: 1em 0;
                color: #666;
                font-style: italic;
            }
            
            /* Admonitions */
            .admonition {
                border-left: 4px solid #ccc;
                padding: 1em;
                margin: 1em 0;
                background: #f9f9f9;
            }
            .admonition-note {
                border-left-color: #3498db;
                background: #e8f4f8;
            }
            .admonition-tip {
                border-left-color: #2ecc71;
                background: #e8f8f0;
            }
            .admonition-warning {
                border-left-color: #f39c12;
                background: #fef5e7;
            }
            .admonition-important {
                border-left-color: #e74c3c;
                background: #fdecea;
            }
            .admonition-caution {
                border-left-color: #e67e22;
                background: #fef0e7;
            }
        """.trimIndent()
    }
}
