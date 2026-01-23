package org.markup.poet.asciidoc.render

/**
 * Minimal theme with basic, clean styling.
 * 
 * This theme provides the bare minimum styling needed for readable documents.
 * It uses CSS variables for easy customization and focuses on simplicity
 * over visual complexity. Perfect for users who want a clean slate to build upon
 * or prefer minimalist document styling.
 * 
 * Key characteristics:
 * - Minimal visual styling (no backgrounds, borders, or decorative elements)
 * - Clean typography with good readability
 * - CSS variables for easy customization
 * - Lightweight CSS footprint
 * 
 * CSS Variables:
 * - --mp-color-text: Main text color
 * - --mp-color-background: Background color
 * - --mp-color-border: Border color for tables and code blocks
 * - --mp-color-muted: Muted text color for quotes
 * - --mp-font-family: Base font family
 * - --mp-font-size-base: Base font size
 * - --mp-line-height: Base line height
 * - --mp-spacing-unit: Base spacing unit
 */
class MinimalTheme : Theme {
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
            :root {
                /* Colors */
                --mp-color-text: #333;
                --mp-color-background: #fff;
                --mp-color-border: #ddd;
                --mp-color-muted: #666;
                
                /* Fonts */
                --mp-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
                --mp-font-size-base: 16px;
                
                /* Spacing */
                --mp-spacing-unit: 1em;
                
                /* Line heights */
                --mp-line-height: 1.6;
            }
            
            body {
                font-family: var(--mp-font-family);
                font-size: var(--mp-font-size-base);
                line-height: var(--mp-line-height);
                color: var(--mp-color-text);
                background: var(--mp-color-background);
            }
            
            /* Headings */
            .heading {
                margin: var(--mp-spacing-unit) 0 0.5em;
                font-weight: bold;
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
            }
            
            /* Code blocks */
            .code-block {
                border: 1px solid var(--mp-color-border);
                padding: var(--mp-spacing-unit);
                margin: var(--mp-spacing-unit) 0;
                overflow-x: auto;
                font-family: monospace;
            }
            
            /* Tables */
            .table {
                border-collapse: collapse;
                width: 100%;
                margin: var(--mp-spacing-unit) 0;
            }
            .table th,
            .table td {
                border: 1px solid var(--mp-color-border);
                padding: 0.5em;
                text-align: left;
            }
            
            /* Lists */
            .list {
                margin: 0.5em 0;
                padding-left: 2em;
            }
            
            /* Quotes */
            .quote {
                border-left: 4px solid var(--mp-color-border);
                padding-left: var(--mp-spacing-unit);
                margin: var(--mp-spacing-unit) 0;
                color: var(--mp-color-muted);
            }
            
            /* Admonitions */
            .admonition {
                border-left: 4px solid var(--mp-color-border);
                padding: var(--mp-spacing-unit);
                margin: var(--mp-spacing-unit) 0;
            }
        """.trimIndent()
    }
}
