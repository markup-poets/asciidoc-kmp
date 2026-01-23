package org.markup.poet.asciidoc.render

/**
 * Dark theme with dark color scheme for low-light environments.
 * 
 * This theme provides a comfortable dark mode experience with carefully chosen
 * colors that reduce eye strain in low-light conditions. It uses CSS variables
 * for easy customization and maintains good contrast ratios for readability.
 * 
 * Key characteristics:
 * - Dark background with light text for reduced eye strain
 * - Carefully chosen accent colors that work well on dark backgrounds
 * - Good contrast ratios for accessibility
 * - CSS variables for easy customization
 * - Suitable for code-heavy documents and technical content
 * 
 * CSS Variables:
 * - --mp-color-text: Main text color (light gray)
 * - --mp-color-text-muted: Muted text color for secondary content
 * - --mp-color-background: Main background color (dark gray)
 * - --mp-color-background-alt: Alternative background for tables/code
 * - --mp-color-border: Border color for elements
 * - --mp-color-code-bg: Background for code blocks
 * - --mp-color-link: Link color (bright blue)
 * - --mp-color-note: Note admonition color
 * - --mp-color-tip: Tip admonition color
 * - --mp-color-warning: Warning admonition color
 * - --mp-color-important: Important admonition color
 * - --mp-color-caution: Caution admonition color
 * - --mp-font-family: Base font family
 * - --mp-font-size-base: Base font size
 * - --mp-line-height: Base line height
 * - --mp-spacing-unit: Base spacing unit
 */
class DarkTheme : Theme {
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
                --mp-color-text: #e0e0e0;
                --mp-color-text-muted: #a0a0a0;
                --mp-color-background: #1e1e1e;
                --mp-color-background-alt: #2d2d2d;
                --mp-color-background-alt2: #252525;
                --mp-color-border: #444;
                --mp-color-border-alt: #555;
                --mp-color-code-bg: #2d2d2d;
                --mp-color-link: #4fc3f7;
                --mp-color-note: #64b5f6;
                --mp-color-note-bg: #1a2332;
                --mp-color-tip: #81c784;
                --mp-color-tip-bg: #1a2e1f;
                --mp-color-warning: #ffb74d;
                --mp-color-warning-bg: #2e2419;
                --mp-color-important: #e57373;
                --mp-color-important-bg: #2e1a1a;
                --mp-color-caution: #ff8a65;
                --mp-color-caution-bg: #2e1f1a;
                
                /* Fonts */
                --mp-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
                --mp-font-family-mono: 'Consolas', 'Monaco', 'Courier New', monospace;
                --mp-font-size-base: 16px;
                --mp-font-size-heading-1: 2em;
                --mp-font-size-heading-2: 1.5em;
                --mp-font-size-heading-3: 1.25em;
                --mp-font-size-heading-4: 1.1em;
                --mp-font-size-heading-5: 1em;
                --mp-font-size-heading-6: 0.9em;
                --mp-font-size-code: 0.9em;
                
                /* Spacing */
                --mp-spacing-unit: 1em;
                --mp-spacing-half: 0.5em;
                --mp-spacing-quarter: 0.25em;
                --mp-spacing-double: 2em;
                --mp-spacing-padding: 1em;
                --mp-spacing-padding-half: 0.5em;
                
                /* Line heights */
                --mp-line-height-base: 1.6;
                --mp-line-height-heading: 1.2;
                
                /* Borders */
                --mp-border-width: 1px;
                --mp-border-width-thick: 4px;
                --mp-border-radius: 4px;
            }
            
            body {
                font-family: var(--mp-font-family);
                font-size: var(--mp-font-size-base);
                line-height: var(--mp-line-height-base);
                color: var(--mp-color-text);
                background: var(--mp-color-background);
            }
            
            /* Headings */
            .heading {
                margin: var(--mp-spacing-unit) 0 var(--mp-spacing-half);
                font-weight: bold;
                line-height: var(--mp-line-height-heading);
                color: var(--mp-color-text);
            }
            .heading-1 { font-size: var(--mp-font-size-heading-1); }
            .heading-2 { font-size: var(--mp-font-size-heading-2); }
            .heading-3 { font-size: var(--mp-font-size-heading-3); }
            .heading-4 { font-size: var(--mp-font-size-heading-4); }
            .heading-5 { font-size: var(--mp-font-size-heading-5); }
            .heading-6 { font-size: var(--mp-font-size-heading-6); }
            
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
                padding: var(--mp-spacing-padding);
                margin: var(--mp-spacing-unit) 0;
                overflow-x: auto;
                font-family: var(--mp-font-family-mono);
                font-size: var(--mp-font-size-code);
            }
            .code-block code {
                background: none;
                padding: 0;
                border: none;
                color: var(--mp-color-text);
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
                padding: var(--mp-spacing-padding-half);
                text-align: left;
            }
            .table th {
                background: var(--mp-color-background-alt);
                font-weight: bold;
            }
            .table tr:nth-child(even) {
                background: var(--mp-color-background-alt2);
            }
            
            /* Lists */
            .list {
                margin: var(--mp-spacing-half) 0;
                padding-left: var(--mp-spacing-double);
            }
            .list li {
                margin: var(--mp-spacing-quarter) 0;
            }
            
            /* Quotes */
            .quote {
                border-left: var(--mp-border-width-thick) solid var(--mp-color-border);
                padding-left: var(--mp-spacing-padding);
                margin: var(--mp-spacing-unit) 0;
                color: var(--mp-color-text-muted);
                font-style: italic;
            }
            
            /* Admonitions */
            .admonition {
                border-left: var(--mp-border-width-thick) solid var(--mp-color-border-alt);
                padding: var(--mp-spacing-padding);
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
            
            /* Links */
            a {
                color: var(--mp-color-link);
                text-decoration: none;
            }
            a:hover {
                text-decoration: underline;
            }
        """.trimIndent()
    }
}
