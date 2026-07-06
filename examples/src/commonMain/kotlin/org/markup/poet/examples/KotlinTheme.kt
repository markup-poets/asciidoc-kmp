package org.markup.poet.examples

import org.markup.poet.theming.*

/**
 * Kotlin-inspired theme based on neuroSKai design system.
 * 
 * This theme demonstrates the pluggable extension capability of the theming system.
 * It features:
 * - Dark background with red accents
 * - Modern typography
 * - Responsive design
 * - Accessibility-focused color contrast
 * 
 * This is an example of how to create custom themes that can be registered
 * and used across different output formats.
 */
class KotlinTheme : Theme {
    override val id: String = "kotlin"
    override val name: String = "Kotlin"
    override val description: String = "Dark theme with red accents inspired by Kotlin branding"
    
    override fun getStyle(element: ElementType, context: StyleContext): ElementStyle {
        return when (element) {
            ElementType.HEADING -> {
                val level = context.level ?: 1
                ElementStyle.withClasses("kotlin-heading", "kotlin-heading-$level")
            }
            ElementType.PARAGRAPH -> ElementStyle.withClasses("kotlin-paragraph")
            ElementType.CODE_BLOCK -> ElementStyle.withClasses("kotlin-code-block")
            ElementType.QUOTE -> ElementStyle.withClasses("kotlin-quote")
            ElementType.LIST -> ElementStyle.withClasses("kotlin-list")
            ElementType.LIST_ITEM -> ElementStyle.withClasses("kotlin-list-item")
            ElementType.TABLE -> ElementStyle.withClasses("kotlin-table")
            ElementType.TABLE_HEADER -> ElementStyle.withClasses("kotlin-table-header")
            ElementType.TABLE_ROW -> ElementStyle.withClasses("kotlin-table-row")
            ElementType.TABLE_CELL -> ElementStyle.withClasses("kotlin-table-cell")
            ElementType.ADMONITION -> {
                val type = context.type ?: "note"
                ElementStyle.withClasses("kotlin-admonition", "kotlin-admonition-$type")
            }
            ElementType.EMPHASIS -> ElementStyle.withClasses("kotlin-emphasis")
            ElementType.STRONG -> ElementStyle.withClasses("kotlin-strong")
            ElementType.CODE -> ElementStyle.withClasses("kotlin-code")
            ElementType.LINK -> ElementStyle.withClasses("kotlin-link")
            ElementType.IMAGE -> ElementStyle.withClasses("kotlin-image")
            ElementType.SIDEBAR -> ElementStyle.withClasses("kotlin-sidebar")
            ElementType.EXAMPLE -> ElementStyle.withClasses("kotlin-example")
            ElementType.LITERAL -> ElementStyle.withClasses("kotlin-literal")
            ElementType.VERSE -> ElementStyle.withClasses("kotlin-verse")
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
        /* Kotlin Theme - neuroSKai Design System */
        
        :root {
            /* Dark Theme Colors */
            --kotlin-background: #0A0B0D;
            --kotlin-foreground: #F2F2F2;
            --kotlin-card: #121418;
            --kotlin-primary: #DC2626;
            --kotlin-primary-glow: #EF4444;
            --kotlin-secondary: #1F2328;
            --kotlin-muted: #1A1D21;
            --kotlin-muted-foreground: #6B7280;
            --kotlin-border: #262B31;
            --kotlin-surface: #0F1114;
            
            /* Typography */
            --kotlin-font-display: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            --kotlin-font-body: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            --kotlin-font-mono: "SF Mono", Monaco, "Cascadia Code", "Roboto Mono", Consolas, monospace;
        }
        
        /* Base Styles */
        body {
            background-color: var(--kotlin-background);
            color: var(--kotlin-foreground);
            font-family: var(--kotlin-font-body);
            font-size: 16px;
            line-height: 1.6;
            margin: 0;
            padding: 2rem;
            max-width: 900px;
            margin-left: auto;
            margin-right: auto;
        }
        
        /* Headings */
        .kotlin-heading {
            font-family: var(--kotlin-font-display);
            font-weight: 600;
            line-height: 1.3;
            margin-top: 2rem;
            margin-bottom: 1rem;
            color: var(--kotlin-foreground);
        }
        
        .kotlin-heading-1 {
            font-size: 2.5rem;
            font-weight: 700;
            border-bottom: 2px solid var(--kotlin-primary);
            padding-bottom: 0.5rem;
            margin-bottom: 1.5rem;
        }
        
        .kotlin-heading-2 {
            font-size: 2rem;
            border-bottom: 1px solid var(--kotlin-border);
            padding-bottom: 0.4rem;
        }
        
        .kotlin-heading-3 {
            font-size: 1.5rem;
            color: var(--kotlin-primary);
        }
        
        .kotlin-heading-4 {
            font-size: 1.25rem;
        }
        
        .kotlin-heading-5 {
            font-size: 1.1rem;
        }
        
        .kotlin-heading-6 {
            font-size: 1rem;
            color: var(--kotlin-muted-foreground);
        }
        
        /* Paragraphs */
        .kotlin-paragraph {
            margin: 1rem 0;
            color: var(--kotlin-foreground);
        }
        
        /* Links */
        .kotlin-link {
            color: var(--kotlin-primary);
            text-decoration: none;
            transition: color 0.2s ease;
        }
        
        .kotlin-link:hover {
            color: var(--kotlin-primary-glow);
            text-decoration: underline;
        }
        
        /* Inline formatting */
        .kotlin-strong {
            font-weight: 600;
            color: var(--kotlin-foreground);
        }
        
        .kotlin-emphasis {
            font-style: italic;
            color: var(--kotlin-foreground);
        }
        
        .kotlin-code {
            font-family: var(--kotlin-font-mono);
            font-size: 0.9em;
            background-color: var(--kotlin-muted);
            color: var(--kotlin-primary-glow);
            padding: 0.2em 0.4em;
            border-radius: 4px;
            border: 1px solid var(--kotlin-border);
        }
        
        /* Code blocks */
        .kotlin-code-block {
            background-color: var(--kotlin-card);
            border: 1px solid var(--kotlin-border);
            border-radius: 8px;
            padding: 1.5rem;
            overflow-x: auto;
            margin: 1.5rem 0;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.3);
            font-family: var(--kotlin-font-mono);
            font-size: 0.95em;
            line-height: 1.5;
            color: var(--kotlin-foreground);
        }
        
        /* Lists */
        .kotlin-list {
            margin: 1rem 0;
            padding-left: 2rem;
        }
        
        .kotlin-list-item {
            margin: 0.5rem 0;
            color: var(--kotlin-foreground);
        }
        
        .kotlin-list-item::marker {
            color: var(--kotlin-primary);
        }
        
        /* Blockquotes */
        .kotlin-quote {
            border-left: 4px solid var(--kotlin-primary);
            background-color: var(--kotlin-muted);
            margin: 1.5rem 0;
            padding: 1rem 1.5rem;
            border-radius: 4px;
            color: var(--kotlin-foreground);
        }
        
        /* Tables */
        .kotlin-table {
            width: 100%;
            border-collapse: collapse;
            margin: 1.5rem 0;
            background-color: var(--kotlin-card);
            border-radius: 8px;
            overflow: hidden;
        }
        
        .kotlin-table-header {
            background-color: var(--kotlin-surface);
            color: var(--kotlin-foreground);
            font-weight: 600;
            text-align: left;
            padding: 0.75rem 1rem;
            border-bottom: 2px solid var(--kotlin-primary);
        }
        
        .kotlin-table-cell {
            padding: 0.75rem 1rem;
            border-bottom: 1px solid var(--kotlin-border);
            color: var(--kotlin-foreground);
        }
        
        .kotlin-table-row:last-child .kotlin-table-cell {
            border-bottom: none;
        }
        
        .kotlin-table-row:hover {
            background-color: var(--kotlin-surface);
        }
        
        /* Admonitions */
        .kotlin-admonition {
            margin: 1.5rem 0;
            padding: 1rem 1.5rem;
            border-radius: 8px;
            border-left: 4px solid var(--kotlin-primary);
            background-color: var(--kotlin-card);
        }
        
        .kotlin-admonition-note {
            border-left-color: #3B82F6;
            background-color: rgba(59, 130, 246, 0.1);
        }
        
        .kotlin-admonition-tip {
            border-left-color: #10B981;
            background-color: rgba(16, 185, 129, 0.1);
        }
        
        .kotlin-admonition-important {
            border-left-color: #F59E0B;
            background-color: rgba(245, 158, 11, 0.1);
        }
        
        .kotlin-admonition-warning {
            border-left-color: #EF4444;
            background-color: rgba(239, 68, 68, 0.1);
        }
        
        .kotlin-admonition-caution {
            border-left-color: #DC2626;
            background-color: rgba(220, 38, 38, 0.1);
        }
        
        /* Images */
        .kotlin-image {
            max-width: 100%;
            height: auto;
            border-radius: 8px;
            margin: 1rem 0;
        }
        
        /* Sidebar */
        .kotlin-sidebar {
            background-color: var(--kotlin-surface);
            border: 1px solid var(--kotlin-border);
            border-radius: 8px;
            padding: 1.5rem;
            margin: 1.5rem 0;
        }
        
        /* Example block */
        .kotlin-example {
            background-color: var(--kotlin-muted);
            border: 1px solid var(--kotlin-border);
            border-radius: 8px;
            padding: 1.5rem;
            margin: 1.5rem 0;
        }
        
        /* Scrollbar styling */
        ::-webkit-scrollbar {
            width: 10px;
            height: 10px;
        }
        
        ::-webkit-scrollbar-track {
            background: var(--kotlin-surface);
        }
        
        ::-webkit-scrollbar-thumb {
            background: var(--kotlin-border);
            border-radius: 5px;
        }
        
        ::-webkit-scrollbar-thumb:hover {
            background: var(--kotlin-primary);
        }
        
        /* Selection */
        ::selection {
            background-color: var(--kotlin-primary);
            color: white;
        }
        
        /* Responsive adjustments */
        @media (max-width: 768px) {
            body {
                padding: 1rem;
                font-size: 14px;
            }
            
            .kotlin-heading-1 {
                font-size: 2rem;
            }
            
            .kotlin-heading-2 {
                font-size: 1.5rem;
            }
            
            .kotlin-heading-3 {
                font-size: 1.25rem;
            }
            
            .kotlin-code-block {
                padding: 1rem;
            }
        }
    """.trimIndent()
}
