package org.markup.poet.asciidoc.render

/**
 * Kotlin-inspired theme based on neuroSKai design system
 * Features a dark background with red accents and modern typography
 */
class KotlinTheme : Theme {
    override fun headingClasses(level: Int): String = "kotlin-heading kotlin-heading-$level"
    
    override fun paragraphClasses(): String = "kotlin-paragraph"
    
    override fun codeBlockClasses(): String = "kotlin-code-block"
    
    override fun tableClasses(): String = "kotlin-table"
    
    override fun listClasses(): String = "kotlin-list"
    
    override fun quoteClasses(): String = "kotlin-quote"
    
    override fun admonitionClasses(type: String): String = "kotlin-admonition kotlin-admonition-$type"
    
    override fun getCss(): String = """
        /* Kotlin Theme - neuroSKai Design System */
        
        :root {
            /* Dark Theme Colors */
            --background: #0A0B0D;
            --foreground: #F2F2F2;
            --card: #121418;
            --card-foreground: #F2F2F2;
            --primary: #DC2626;
            --primary-foreground: #FFFFFF;
            --primary-glow: #EF4444;
            --secondary: #1F2328;
            --secondary-foreground: #D9D9D9;
            --muted: #1A1D21;
            --muted-foreground: #6B7280;
            --accent: #DC2626;
            --accent-foreground: #FFFFFF;
            --destructive: #EF4444;
            --destructive-foreground: #F8FAFC;
            --border: #262B31;
            --input: #262B31;
            --ring: #DC2626;
            --surface: #0F1114;
            --surface-variant: #1A1D21;
            
            /* Node/Arc colors for code blocks */
            --node-dark: #3C4049;
            --node-red: #DC2626;
            
            /* Typography */
            --font-display: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
            --font-body: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
            --font-mono: "SF Mono", Monaco, "Cascadia Code", "Roboto Mono", Consolas, "Courier New", monospace;
        }
        
        /* Base Styles */
        body {
            background-color: var(--background);
            color: var(--foreground);
            font-family: var(--font-body);
            font-size: 16px;
            line-height: 1.6;
            margin: 0;
            padding: 2rem;
            max-width: 900px;
            margin-left: auto;
            margin-right: auto;
        }
        
        /* Typography */
        h1, h2, h3, h4, h5, h6 {
            font-family: var(--font-display);
            font-weight: 600;
            line-height: 1.3;
            margin-top: 2rem;
            margin-bottom: 1rem;
            color: var(--foreground);
        }
        
        h1 {
            font-size: 2.5rem;
            font-weight: 700;
            border-bottom: 2px solid var(--primary);
            padding-bottom: 0.5rem;
            margin-bottom: 1.5rem;
        }
        
        h2 {
            font-size: 2rem;
            border-bottom: 1px solid var(--border);
            padding-bottom: 0.4rem;
        }
        
        h3 {
            font-size: 1.5rem;
            color: var(--primary);
        }
        
        h4 {
            font-size: 1.25rem;
        }
        
        h5 {
            font-size: 1.1rem;
        }
        
        h6 {
            font-size: 1rem;
            color: var(--muted-foreground);
        }
        
        /* Paragraphs */
        p {
            margin: 1rem 0;
            color: var(--foreground);
        }
        
        /* Links */
        a {
            color: var(--primary);
            text-decoration: none;
            transition: color 0.2s ease;
        }
        
        a:hover {
            color: var(--primary-glow);
            text-decoration: underline;
        }
        
        /* Inline formatting */
        strong, b {
            font-weight: 600;
            color: var(--foreground);
        }
        
        em, i {
            font-style: italic;
            color: var(--secondary-foreground);
        }
        
        code {
            font-family: var(--font-mono);
            font-size: 0.9em;
            background-color: var(--muted);
            color: var(--primary-glow);
            padding: 0.2em 0.4em;
            border-radius: 4px;
            border: 1px solid var(--border);
        }
        
        /* Code blocks */
        pre {
            background-color: var(--card);
            border: 1px solid var(--border);
            border-radius: 8px;
            padding: 1.5rem;
            overflow-x: auto;
            margin: 1.5rem 0;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.3);
        }
        
        pre code {
            background-color: transparent;
            border: none;
            padding: 0;
            color: var(--foreground);
            font-size: 0.95em;
            line-height: 1.5;
        }
        
        /* Lists */
        ul, ol {
            margin: 1rem 0;
            padding-left: 2rem;
        }
        
        li {
            margin: 0.5rem 0;
            color: var(--foreground);
        }
        
        li::marker {
            color: var(--primary);
        }
        
        /* Blockquotes */
        blockquote {
            border-left: 4px solid var(--primary);
            background-color: var(--muted);
            margin: 1.5rem 0;
            padding: 1rem 1.5rem;
            border-radius: 4px;
            color: var(--secondary-foreground);
        }
        
        blockquote p {
            margin: 0.5rem 0;
        }
        
        /* Tables */
        table {
            width: 100%;
            border-collapse: collapse;
            margin: 1.5rem 0;
            background-color: var(--card);
            border-radius: 8px;
            overflow: hidden;
        }
        
        th {
            background-color: var(--surface-variant);
            color: var(--foreground);
            font-weight: 600;
            text-align: left;
            padding: 0.75rem 1rem;
            border-bottom: 2px solid var(--primary);
        }
        
        td {
            padding: 0.75rem 1rem;
            border-bottom: 1px solid var(--border);
            color: var(--foreground);
        }
        
        tr:last-child td {
            border-bottom: none;
        }
        
        tr:hover {
            background-color: var(--surface-variant);
        }
        
        /* Horizontal rule */
        hr {
            border: none;
            border-top: 2px solid var(--border);
            margin: 2rem 0;
        }
        
        /* Admonition blocks */
        .admonitionblock {
            margin: 1.5rem 0;
            padding: 1rem 1.5rem;
            border-radius: 8px;
            border-left: 4px solid var(--primary);
            background-color: var(--card);
        }
        
        .admonitionblock.note {
            border-left-color: #3B82F6;
            background-color: rgba(59, 130, 246, 0.1);
        }
        
        .admonitionblock.tip {
            border-left-color: #10B981;
            background-color: rgba(16, 185, 129, 0.1);
        }
        
        .admonitionblock.important {
            border-left-color: #F59E0B;
            background-color: rgba(245, 158, 11, 0.1);
        }
        
        .admonitionblock.warning {
            border-left-color: #EF4444;
            background-color: rgba(239, 68, 68, 0.1);
        }
        
        .admonitionblock.caution {
            border-left-color: #DC2626;
            background-color: rgba(220, 38, 38, 0.1);
        }
        
        /* Images */
        img {
            max-width: 100%;
            height: auto;
            border-radius: 8px;
            margin: 1rem 0;
        }
        
        /* Sidebar */
        .sidebarblock {
            background-color: var(--surface-variant);
            border: 1px solid var(--border);
            border-radius: 8px;
            padding: 1.5rem;
            margin: 1.5rem 0;
        }
        
        /* Example block */
        .exampleblock {
            background-color: var(--muted);
            border: 1px solid var(--border);
            border-radius: 8px;
            padding: 1.5rem;
            margin: 1.5rem 0;
        }
        
        /* Listing block */
        .listingblock {
            margin: 1.5rem 0;
        }
        
        .listingblock .title {
            color: var(--primary);
            font-weight: 600;
            margin-bottom: 0.5rem;
        }
        
        /* Literal block */
        .literalblock {
            margin: 1.5rem 0;
        }
        
        .literalblock pre {
            background-color: var(--surface);
            border: 1px solid var(--border);
        }
        
        /* Verse block */
        .verseblock {
            font-style: italic;
            color: var(--secondary-foreground);
            margin: 1.5rem 0;
            padding: 1rem 2rem;
            border-left: 3px solid var(--primary);
        }
        
        /* Quote block */
        .quoteblock {
            margin: 1.5rem 0;
            padding: 1rem 2rem;
            border-left: 4px solid var(--primary);
            background-color: var(--muted);
        }
        
        .quoteblock .attribution {
            text-align: right;
            color: var(--muted-foreground);
            font-style: italic;
            margin-top: 0.5rem;
        }
        
        /* Scrollbar styling */
        ::-webkit-scrollbar {
            width: 10px;
            height: 10px;
        }
        
        ::-webkit-scrollbar-track {
            background: var(--surface);
        }
        
        ::-webkit-scrollbar-thumb {
            background: var(--border);
            border-radius: 5px;
        }
        
        ::-webkit-scrollbar-thumb:hover {
            background: var(--primary);
        }
        
        /* Selection */
        ::selection {
            background-color: var(--primary);
            color: var(--primary-foreground);
        }
        
        /* Responsive adjustments */
        @media (max-width: 768px) {
            body {
                padding: 1rem;
                font-size: 14px;
            }
            
            h1 {
                font-size: 2rem;
            }
            
            h2 {
                font-size: 1.5rem;
            }
            
            h3 {
                font-size: 1.25rem;
            }
            
            pre {
                padding: 1rem;
            }
        }
    """.trimIndent()
}
