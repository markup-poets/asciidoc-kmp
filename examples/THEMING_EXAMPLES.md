# Theming System Examples

This file contains complete, runnable examples demonstrating the theming system.

## Example 1: Basic Rendering with Default Theme

```kotlin
package org.markup.poet.examples

import org.markup.poet.asciidoc.render.*
import org.markup.poet.asciidoc.ast.*

fun basicExample() {
    // Create a simple document
    val document = Document(
        title = "My Document",
        blocks = listOf(
            Heading(1, "Introduction"),
            Paragraph("This is a simple paragraph."),
            CodeBlock("println(\"Hello, World!\")", language = "kotlin")
        )
    )
    
    // Create renderer with default configuration
    val renderer = DefaultHtmlRenderer(
        blockRenderer = DefaultBlockRenderer(),
        inlineRenderer = DefaultInlineRenderer()
    )
    
    // Render with default theme
    val result = renderer.render(document)
    
    result.fold(
        onSuccess = { html ->
            println("Rendered successfully!")
            println(html)
        },
        onFailure = { error ->
            println("Error: ${error.message}")
        }
    )
}
```

## Example 2: Using Different Built-in Themes

```kotlin
package org.markup.poet.examples

import org.markup.poet.asciidoc.render.*

fun themeComparison(document: Document) {
    val renderer = DefaultHtmlRenderer(
        blockRenderer = DefaultBlockRenderer(),
        inlineRenderer = DefaultInlineRenderer()
    )
    
    // Render with each theme
    val themes = mapOf(
        "default" to DefaultTheme(),
        "dark" to DarkTheme(),
        "kotlin" to KotlinTheme(),
        "minimal" to MinimalTheme()
    )
    
    themes.forEach { (name, theme) ->
        val config = RenderConfig(theme = theme)
        val result = renderer.render(document, config)
        
        result.onSuccess { html ->
            // Save to file
            File("output-$name.html").writeText(html)
            println("Generated output-$name.html")
        }
    }
}
```

## Example 3: Custom CSS with Theme

```kotlin
package org.markup.poet.examples

import org.markup.poet.asciidoc.render.*

fun customCssExample(document: Document) {
    val customCss = """
        /* Custom styles for headings */
        .heading-1 {
            color: #DC2626;
            border-bottom: 3px solid #DC2626;
            padding-bottom: 0.5em;
            text-transform: uppercase;
            letter-spacing: 0.05em;
        }
        
        /* Custom code block styling */
        .code-block {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            border-radius: 12px;
            padding: 1.5em;
            box-shadow: 0 10px 30px rgba(0,0,0,0.3);
        }
        
        /* Custom paragraph styling */
        .paragraph {
            font-size: 1.1em;
            line-height: 1.8;
            text-align: justify;
        }
    """.trimIndent()
    
    val config = RenderConfig(
        theme = DefaultTheme(),
        cssOptions = CssOptions(
            customCssContent = customCss
        )
    )
    
    val renderer = DefaultHtmlRenderer(
        blockRenderer = DefaultBlockRenderer(),
        inlineRenderer = DefaultInlineRenderer()
    )
    
    val result = renderer.render(document, config)
    result.onSuccess { html ->
        File("output-custom.html").writeText(html)
        println("Generated output-custom.html with custom CSS")
    }
}
```

## Example 4: CSS Variable Overrides

```kotlin
package org.markup.poet.examples

import org.markup.poet.asciidoc.render.*

fun cssVariablesExample(document: Document) {
    // Brand colors
    val brandColors = mapOf(
        "--mp-color-primary" to "#DC2626",
        "--mp-color-secondary" to "#1F2328",
        "--mp-color-text" to "#0A0B0D",
        "--mp-color-background" to "#FFFFFF"
    )
    
    // Typography
    val typography = mapOf(
        "--mp-font-family" to "'Inter', -apple-system, sans-serif",
        "--mp-font-size-base" to "18px",
        "--mp-line-height-base" to "1.8"
    )
    
    // Spacing
    val spacing = mapOf(
        "--mp-spacing-unit" to "1.5em",
        "--mp-spacing-padding" to "1.5em"
    )
    
    // Combine all overrides
    val allOverrides = brandColors + typography + spacing
    
    val config = RenderConfig(
        theme = DefaultTheme(),
        cssOptions = CssOptions(
            cssVariableOverrides = allOverrides
        )
    )
    
    val renderer = DefaultHtmlRenderer(
        blockRenderer = DefaultBlockRenderer(),
        inlineRenderer = DefaultInlineRenderer()
    )
    
    val result = renderer.render(document, config)
    result.onSuccess { html ->
        File("output-branded.html").writeText(html)
        println("Generated output-branded.html with brand colors")
    }
}
```

## Example 5: Creating a Custom Theme

```kotlin
package org.markup.poet.examples

import org.markup.poet.asciidoc.render.Theme

/**
 * Custom theme for technical documentation.
 * Features monospace fonts and a terminal-like appearance.
 */
class TechnicalTheme : Theme {
    override fun headingClasses(level: Int) = "tech-heading tech-h$level"
    override fun paragraphClasses() = "tech-paragraph"
    override fun codeBlockClasses() = "tech-code"
    override fun tableClasses() = "tech-table"
    override fun listClasses() = "tech-list"
    override fun quoteClasses() = "tech-quote"
    override fun admonitionClasses(type: String) = "tech-admonition tech-$type"
    
    override fun getCss(): String = """
        :root {
            --tech-bg: #0d1117;
            --tech-fg: #c9d1d9;
            --tech-accent: #58a6ff;
            --tech-border: #30363d;
            --tech-code-bg: #161b22;
        }
        
        body {
            background: var(--tech-bg);
            color: var(--tech-fg);
            font-family: 'SF Mono', 'Consolas', 'Monaco', monospace;
            font-size: 14px;
            line-height: 1.6;
            padding: 2em;
            max-width: 1200px;
            margin: 0 auto;
        }
        
        .tech-heading {
            font-family: 'SF Mono', monospace;
            font-weight: 700;
            color: var(--tech-accent);
            margin: 2em 0 1em;
            border-bottom: 2px solid var(--tech-border);
            padding-bottom: 0.5em;
        }
        
        .tech-h1 { font-size: 2em; }
        .tech-h2 { font-size: 1.5em; }
        .tech-h3 { font-size: 1.25em; }
        
        .tech-paragraph {
            margin: 1em 0;
            line-height: 1.8;
        }
        
        .tech-code {
            background: var(--tech-code-bg);
            border: 1px solid var(--tech-border);
            border-radius: 6px;
            padding: 1em;
            overflow-x: auto;
            font-family: 'SF Mono', monospace;
            font-size: 13px;
        }
        
        .tech-table {
            width: 100%;
            border-collapse: collapse;
            margin: 1.5em 0;
        }
        
        .tech-table th,
        .tech-table td {
            border: 1px solid var(--tech-border);
            padding: 0.75em;
            text-align: left;
        }
        
        .tech-table th {
            background: var(--tech-code-bg);
            font-weight: 700;
        }
        
        .tech-list {
            margin: 1em 0;
            padding-left: 2em;
        }
        
        .tech-quote {
            border-left: 4px solid var(--tech-accent);
            padding-left: 1em;
            margin: 1.5em 0;
            font-style: italic;
            color: var(--tech-fg);
            opacity: 0.8;
        }
        
        .tech-admonition {
            border-left: 4px solid var(--tech-accent);
            background: var(--tech-code-bg);
            padding: 1em;
            margin: 1.5em 0;
            border-radius: 4px;
        }
        
        .tech-note { border-left-color: #58a6ff; }
        .tech-tip { border-left-color: #3fb950; }
        .tech-warning { border-left-color: #d29922; }
        .tech-important { border-left-color: #f85149; }
        .tech-caution { border-left-color: #db6d28; }
        
        a {
            color: var(--tech-accent);
            text-decoration: none;
        }
        
        a:hover {
            text-decoration: underline;
        }
        
        code {
            background: var(--tech-code-bg);
            padding: 0.2em 0.4em;
            border-radius: 3px;
            font-family: 'SF Mono', monospace;
            font-size: 0.9em;
        }
    """.trimIndent()
}

// Usage
fun technicalThemeExample(document: Document) {
    val config = RenderConfig(theme = TechnicalTheme())
    val renderer = DefaultHtmlRenderer(
        blockRenderer = DefaultBlockRenderer(),
        inlineRenderer = DefaultInlineRenderer()
    )
    
    val result = renderer.render(document, config)
    result.onSuccess { html ->
        File("output-technical.html").writeText(html)
        println("Generated output-technical.html with technical theme")
    }
}
```

## Example 6: External CSS Mode

```kotlin
package org.markup.poet.examples

import org.markup.poet.asciidoc.render.*
import java.io.File

fun externalCssExample(documents: List<Document>) {
    val config = RenderConfig(
        theme = KotlinTheme(),
        outputOptions = OutputOptions(
            cssMode = CssMode.EXTERNAL,
            cssPath = "shared-styles.css"
        )
    )
    
    val renderer = DefaultHtmlRenderer(
        blockRenderer = DefaultBlockRenderer(),
        inlineRenderer = DefaultInlineRenderer()
    )
    
    // Render all documents with shared CSS
    documents.forEachIndexed { index, document ->
        val result = renderer.render(document, config)
        result.onSuccess { html ->
            File("output-$index.html").writeText(html)
            println("Generated output-$index.html")
        }
    }
    
    println("All documents share shared-styles.css")
}
```

## Example 7: Dynamic Theme Selection

```kotlin
package org.markup.poet.examples

import org.markup.poet.asciidoc.render.*

class ThemeManager {
    private val themes = mapOf(
        "default" to DefaultTheme(),
        "dark" to DarkTheme(),
        "kotlin" to KotlinTheme(),
        "minimal" to MinimalTheme(),
        "technical" to TechnicalTheme()
    )
    
    fun getTheme(name: String): Theme {
        return themes[name.lowercase()] ?: DefaultTheme()
    }
    
    fun listThemes(): List<String> {
        return themes.keys.toList()
    }
}

fun dynamicThemeExample(document: Document, themeName: String) {
    val themeManager = ThemeManager()
    
    // Get theme by name
    val theme = themeManager.getTheme(themeName)
    
    val config = RenderConfig(theme = theme)
    val renderer = DefaultHtmlRenderer(
        blockRenderer = DefaultBlockRenderer(),
        inlineRenderer = DefaultInlineRenderer()
    )
    
    val result = renderer.render(document, config)
    result.onSuccess { html ->
        File("output-$themeName.html").writeText(html)
        println("Generated output-$themeName.html")
    }
}

// Usage
fun main() {
    val document = createSampleDocument()
    val themeManager = ThemeManager()
    
    println("Available themes: ${themeManager.listThemes()}")
    
    // Render with user-selected theme
    val selectedTheme = readLine() ?: "default"
    dynamicThemeExample(document, selectedTheme)
}
```

## Example 8: Batch Processing with Different Themes

```kotlin
package org.markup.poet.examples

import org.markup.poet.asciidoc.render.*
import java.io.File

data class RenderJob(
    val document: Document,
    val outputPath: String,
    val theme: Theme,
    val cssOptions: CssOptions = CssOptions.default()
)

fun batchRenderExample(jobs: List<RenderJob>) {
    val renderer = DefaultHtmlRenderer(
        blockRenderer = DefaultBlockRenderer(),
        inlineRenderer = DefaultInlineRenderer()
    )
    
    jobs.forEach { job ->
        val config = RenderConfig(
            theme = job.theme,
            cssOptions = job.cssOptions
        )
        
        val result = renderer.render(job.document, config)
        result.fold(
            onSuccess = { html ->
                File(job.outputPath).writeText(html)
                println("✓ Generated ${job.outputPath}")
            },
            onFailure = { error ->
                println("✗ Failed ${job.outputPath}: ${error.message}")
            }
        )
    }
}

// Usage
fun main() {
    val documents = loadDocuments()
    
    val jobs = listOf(
        RenderJob(
            document = documents[0],
            outputPath = "public-docs.html",
            theme = DefaultTheme()
        ),
        RenderJob(
            document = documents[1],
            outputPath = "internal-docs.html",
            theme = DarkTheme(),
            cssOptions = CssOptions(
                cssVariableOverrides = mapOf(
                    "--mp-color-primary" to "#DC2626"
                )
            )
        ),
        RenderJob(
            document = documents[2],
            outputPath = "presentation.html",
            theme = KotlinTheme()
        )
    )
    
    batchRenderExample(jobs)
}
```

## Example 9: Theme with Custom Fonts

```kotlin
package org.markup.poet.examples

import org.markup.poet.asciidoc.render.*

fun customFontsExample(document: Document) {
    // Load custom fonts via CSS
    val customCss = """
        @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;600;700&display=swap');
        @import url('https://fonts.googleapis.com/css2?family=JetBrains+Mono&display=swap');
    """.trimIndent()
    
    val config = RenderConfig(
        theme = DefaultTheme(),
        cssOptions = CssOptions(
            customCssContent = customCss,
            cssVariableOverrides = mapOf(
                "--mp-font-family" to "'Inter', sans-serif",
                "--mp-font-family-mono" to "'JetBrains Mono', monospace"
            )
        )
    )
    
    val renderer = DefaultHtmlRenderer(
        blockRenderer = DefaultBlockRenderer(),
        inlineRenderer = DefaultInlineRenderer()
    )
    
    val result = renderer.render(document, config)
    result.onSuccess { html ->
        File("output-custom-fonts.html").writeText(html)
        println("Generated output-custom-fonts.html with custom fonts")
    }
}
```

## Example 10: Responsive Theme

```kotlin
package org.markup.poet.examples

import org.markup.poet.asciidoc.render.Theme

class ResponsiveTheme : Theme {
    override fun headingClasses(level: Int) = "responsive-heading responsive-h$level"
    override fun paragraphClasses() = "responsive-paragraph"
    override fun codeBlockClasses() = "responsive-code"
    override fun tableClasses() = "responsive-table"
    override fun listClasses() = "responsive-list"
    override fun quoteClasses() = "responsive-quote"
    override fun admonitionClasses(type: String) = "responsive-admonition responsive-$type"
    
    override fun getCss(): String = """
        :root {
            --base-font-size: 16px;
            --heading-scale: 1.5;
            --spacing: 1em;
        }
        
        /* Mobile first */
        body {
            font-size: var(--base-font-size);
            padding: 1em;
            max-width: 100%;
        }
        
        .responsive-heading {
            margin: 1.5em 0 0.75em;
        }
        
        .responsive-h1 { font-size: calc(var(--base-font-size) * 1.8); }
        .responsive-h2 { font-size: calc(var(--base-font-size) * 1.5); }
        .responsive-h3 { font-size: calc(var(--base-font-size) * 1.25); }
        
        .responsive-code {
            overflow-x: auto;
            font-size: 0.9em;
        }
        
        .responsive-table {
            display: block;
            overflow-x: auto;
            width: 100%;
        }
        
        /* Tablet */
        @media (min-width: 768px) {
            :root {
                --base-font-size: 18px;
            }
            
            body {
                padding: 2em;
                max-width: 750px;
                margin: 0 auto;
            }
            
            .responsive-h1 { font-size: calc(var(--base-font-size) * 2); }
            .responsive-h2 { font-size: calc(var(--base-font-size) * 1.6); }
        }
        
        /* Desktop */
        @media (min-width: 1024px) {
            :root {
                --base-font-size: 20px;
            }
            
            body {
                padding: 3em;
                max-width: 900px;
            }
            
            .responsive-h1 { font-size: calc(var(--base-font-size) * 2.5); }
            .responsive-h2 { font-size: calc(var(--base-font-size) * 2); }
        }
        
        /* Large desktop */
        @media (min-width: 1440px) {
            body {
                max-width: 1200px;
            }
        }
        
        /* Print */
        @media print {
            body {
                font-size: 12pt;
                max-width: 100%;
            }
            
            .responsive-heading {
                page-break-after: avoid;
            }
            
            .responsive-code {
                page-break-inside: avoid;
            }
        }
    """.trimIndent()
}
```

## Running the Examples

To run these examples:

1. **Add to your project:**
   ```kotlin
   // In your main function or test
   fun main() {
       val document = createSampleDocument()
       basicExample()
       customCssExample(document)
       cssVariablesExample(document)
       // ... etc
   }
   ```

2. **Create sample document:**
   ```kotlin
   fun createSampleDocument(): Document {
       return Document(
           title = "Sample Document",
           blocks = listOf(
               Heading(1, "Introduction"),
               Paragraph("This is a sample document for testing themes."),
               Heading(2, "Code Example"),
               CodeBlock("fun main() {\n    println(\"Hello!\")\n}", language = "kotlin"),
               Heading(2, "List Example"),
               UnorderedList(
                   items = listOf(
                       ListItem("First item"),
                       ListItem("Second item"),
                       ListItem("Third item")
                   )
               )
           )
       )
   }
   ```

3. **Build and run:**
   ```bash
   ./gradlew :examples:run
   ```

## Next Steps

- Explore the [THEMING_USAGE.md](../THEMING_USAGE.md) guide for more patterns
- Read [THEMING_ARCHITECTURE.md](../THEMING_ARCHITECTURE.md) for architecture details
- Check [KOTLIN_THEME.md](../KOTLIN_THEME.md) for Kotlin theme specifics
- See test files in `html-renderer/src/commonTest/` for more examples
