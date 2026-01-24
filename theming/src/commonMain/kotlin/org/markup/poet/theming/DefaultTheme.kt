package org.markup.poet.theming

/**
 * Default theme with minimal, clean styling.
 * 
 * This theme provides semantic class names and basic styling suitable
 * for most documents. It follows web standards and accessibility best practices.
 * 
 * The default theme is format-agnostic in its design but provides CSS output
 * for HTML rendering. The same semantic approach can be adapted for PDF or
 * other output formats.
 */
class DefaultTheme : Theme {
    override val id: String = "default"
    override val name: String = "Default"
    override val description: String = "Clean, minimal styling with semantic HTML"
    
    override fun getStyle(element: ElementType, context: StyleContext): ElementStyle {
        return when (element) {
            ElementType.HEADING -> {
                val level = context.level ?: 1
                ElementStyle.withClasses("heading", "heading-$level")
            }
            ElementType.PARAGRAPH -> ElementStyle.withClasses("paragraph")
            ElementType.CODE_BLOCK -> ElementStyle.withClasses("code-block")
            ElementType.QUOTE -> ElementStyle.withClasses("quote")
            ElementType.LIST -> ElementStyle.withClasses("list")
            ElementType.LIST_ITEM -> ElementStyle.withClasses("list-item")
            ElementType.TABLE -> ElementStyle.withClasses("table")
            ElementType.TABLE_HEADER -> ElementStyle.withClasses("table-header")
            ElementType.TABLE_ROW -> ElementStyle.withClasses("table-row")
            ElementType.TABLE_CELL -> ElementStyle.withClasses("table-cell")
            ElementType.ADMONITION -> {
                val type = context.type ?: "note"
                ElementStyle.withClasses("admonition", "admonition-$type")
            }
            ElementType.EMPHASIS -> ElementStyle.withClasses("emphasis")
            ElementType.STRONG -> ElementStyle.withClasses("strong")
            ElementType.CODE -> ElementStyle.withClasses("code")
            ElementType.LINK -> ElementStyle.withClasses("link")
            ElementType.IMAGE -> ElementStyle.withClasses("image")
            ElementType.SIDEBAR -> ElementStyle.withClasses("sidebar")
            ElementType.EXAMPLE -> ElementStyle.withClasses("example")
            ElementType.LITERAL -> ElementStyle.withClasses("literal")
            ElementType.VERSE -> ElementStyle.withClasses("verse")
            else -> ElementStyle.empty()
        }
    }
    
    override fun getStylesheet(format: String): String? {
        return when (format.lowercase()) {
            "css" -> getCssStylesheet()
            else -> null
        }
    }
    
    private fun getCssStylesheet(): String = """
        :root {
            /* Colors */
            --mp-color-primary: #007acc;
            --mp-color-text: #333;
            --mp-color-text-muted: #666;
            --mp-color-background: #fff;
            --mp-color-background-alt: #f5f5f5;
            --mp-color-border: #ddd;
            --mp-color-code-bg: #f5f5f5;
            --mp-color-note: #3498db;
            --mp-color-note-bg: #e8f4f8;
            --mp-color-tip: #2ecc71;
            --mp-color-tip-bg: #e8f8f0;
            --mp-color-warning: #f39c12;
            --mp-color-warning-bg: #fef5e7;
            --mp-color-important: #e74c3c;
            --mp-color-important-bg: #fdecea;
            --mp-color-caution: #e67e22;
            --mp-color-caution-bg: #fef0e7;
            
            /* Typography */
            --mp-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
            --mp-font-family-mono: 'SF Mono', Monaco, 'Cascadia Code', monospace;
            --mp-font-size-base: 16px;
            --mp-line-height-base: 1.6;
            
            /* Spacing */
            --mp-spacing-unit: 1em;
            --mp-spacing-half: 0.5em;
            
            /* Borders */
            --mp-border-width: 1px;
            --mp-border-radius: 4px;
        }
        
        /* Headings */
        .heading {
            margin: var(--mp-spacing-unit) 0 var(--mp-spacing-half);
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
            margin: var(--mp-spacing-half) 0;
            line-height: var(--mp-line-height-base);
        }
        
        /* Code blocks */
        .code-block {
            background: var(--mp-color-code-bg);
            border: var(--mp-border-width) solid var(--mp-color-border);
            border-radius: var(--mp-border-radius);
            padding: 1em;
            margin: var(--mp-spacing-unit) 0;
            overflow-x: auto;
            font-family: var(--mp-font-family-mono);
            font-size: 0.9em;
        }
        
        /* Tables */
        .table {
            border-collapse: collapse;
            width: 100%;
            margin: var(--mp-spacing-unit) 0;
        }
        .table th,
        .table td {
            border: var(--mp-border-width) solid var(--mp-color-border);
            padding: 0.5em;
            text-align: left;
        }
        .table th {
            background: var(--mp-color-background-alt);
            font-weight: bold;
        }
        
        /* Lists */
        .list {
            margin: var(--mp-spacing-half) 0;
            padding-left: 2em;
        }
        
        /* Quotes */
        .quote {
            border-left: 4px solid var(--mp-color-border);
            padding-left: 1em;
            margin: var(--mp-spacing-unit) 0;
            color: var(--mp-color-text-muted);
            font-style: italic;
        }
        
        /* Admonitions */
        .admonition {
            border-left: 4px solid var(--mp-color-border);
            padding: 1em;
            margin: var(--mp-spacing-unit) 0;
            background: var(--mp-color-background-alt);
        }
        .admonition-note {
            border-left-color: var(--mp-color-note);
            background: var(--mp-color-note-bg);
        }
        .admonition-tip {
            border-left-color: var(--mp-color-tip);
            background: var(--mp-color-tip-bg);
        }
        .admonition-warning {
            border-left-color: var(--mp-color-warning);
            background: var(--mp-color-warning-bg);
        }
        .admonition-important {
            border-left-color: var(--mp-color-important);
            background: var(--mp-color-important-bg);
        }
        .admonition-caution {
            border-left-color: var(--mp-color-caution);
            background: var(--mp-color-caution-bg);
        }
    """.trimIndent()
}
