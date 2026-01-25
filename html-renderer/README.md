# HTML Renderer Module

The HTML Renderer module provides a flexible, themeable system for rendering AsciiDoc AST to HTML output.

## Features

- **Pluggable Theming**: Strict separation of document structure from visual presentation
- **Built-in Themes**: DefaultTheme, DarkTheme, KotlinTheme, MinimalTheme
- **CSS Customization**: CSS variables, custom CSS files, inline CSS
- **Multiple CSS Modes**: Inline, external, or no CSS
- **Platform Support**: JVM, Android, iOS, Linux, macOS
- **Type-Safe Configuration**: Kotlin data classes for all options

## Quick Start

### Basic Usage

```kotlin
import org.markup.poet.asciidoc.render.*

val renderer = DefaultHtmlRenderer(blockRenderer, inlineRenderer)
val result = renderer.render(document)

result.onSuccess { html ->
    println(html)
}
```

### With Theme

```kotlin
val config = RenderConfig(
    theme = KotlinTheme()
)

val result = renderer.render(document, config)
```

### With Custom CSS

```kotlin
val config = RenderConfig(
    theme = DefaultTheme(),
    cssOptions = CssOptions(
        customCssPath = "custom.css",
        cssVariableOverrides = mapOf(
            "--mp-color-primary" to "#DC2626"
        )
    )
)

val result = renderer.render(document, config)
```

## Architecture

### Core Components

```
HtmlRenderer
    ├── Theme (provides CSS classes and stylesheet)
    ├── CssProvider (loads and merges CSS)
    ├── BlockRenderer (renders block elements)
    ├── InlineRenderer (renders inline elements)
    └── HtmlEscaper (security/escaping)
```

### Theme Interface

```kotlin
interface Theme {
    fun headingClasses(level: Int): String
    fun paragraphClasses(): String
    fun codeBlockClasses(): String
    fun tableClasses(): String
    fun listClasses(): String
    fun quoteClasses(): String
    fun admonitionClasses(type: String): String
    fun getCss(): String
}
```

## Built-in Themes

### DefaultTheme
Clean, minimal styling with CSS variables for easy customization.

**Best for:** General documentation, public-facing content

### DarkTheme
Dark background with light text, optimized for low-light environments.

**Best for:** Code-heavy documentation, developer tools

### KotlinTheme
Kotlin-branded theme with dark background and red accents (neuroSKai design system).

**Best for:** Kotlin-related content, technical presentations

### MinimalTheme
Bare minimum styling, ideal starting point for custom themes.

**Best for:** Complete custom styling, specific brand requirements

## Configuration

### RenderConfig

```kotlin
data class RenderConfig(
    val outputOptions: OutputOptions = OutputOptions.default(),
    val theme: Theme = Theme.default(),
    val cssOptions: CssOptions = CssOptions.default(),
    val customRenderers: Map<String, CustomRenderer> = emptyMap(),
    val attributeHandlers: Map<String, AttributeHandler> = emptyMap(),
    val documentTemplate: DocumentTemplate? = null
)
```

### OutputOptions

```kotlin
data class OutputOptions(
    val standalone: Boolean = true,           // Complete HTML vs fragment
    val cssMode: CssMode = CssMode.INLINE,    // CSS inclusion mode
    val cssPath: String? = null,              // External CSS path
    val includeMetadata: Boolean = true,      // Meta tags
    val includeToc: Boolean = false,          // Table of contents
    val documentTitle: String? = null,        // Override title
    val language: String = "en",              // HTML lang attribute
    val customAttributes: Map<String, String> = emptyMap()
)
```

### CssOptions

```kotlin
data class CssOptions(
    val customCssContent: String? = null,                    // Inline CSS
    val customCssPath: String? = null,                       // CSS file path
    val includeDefaultCss: Boolean = true,                   // Include theme CSS
    val cssVariableOverrides: Map<String, String> = emptyMap() // CSS variables
)
```

### CssMode

```kotlin
enum class CssMode {
    NONE,      // No CSS included
    INLINE,    // CSS in <style> tag
    EXTERNAL   // CSS in external file
}
```

## CSS Variables

All built-in themes use CSS variables following the `--mp-{category}-{property}` convention.

### Common Variables

**Colors:**
- `--mp-color-primary` - Primary accent color
- `--mp-color-text` - Main text color
- `--mp-color-background` - Background color
- `--mp-color-border` - Border color

**Typography:**
- `--mp-font-family` - Base font family
- `--mp-font-family-mono` - Monospace font
- `--mp-font-size-base` - Base font size

**Spacing:**
- `--mp-spacing-unit` - Base spacing unit
- `--mp-spacing-padding` - Base padding

### Override Variables

```kotlin
val config = RenderConfig(
    theme = DefaultTheme(),
    cssOptions = CssOptions(
        cssVariableOverrides = mapOf(
            "--mp-color-primary" to "#DC2626",
            "--mp-font-family" to "Georgia, serif",
            "--mp-spacing-unit" to "1.5em"
        )
    )
)
```

## Creating Custom Themes

### Simple Custom Theme

```kotlin
class MyTheme : Theme {
    override fun headingClasses(level: Int) = "my-heading my-h$level"
    override fun paragraphClasses() = "my-paragraph"
    override fun codeBlockClasses() = "my-code"
    override fun tableClasses() = "my-table"
    override fun listClasses() = "my-list"
    override fun quoteClasses() = "my-quote"
    override fun admonitionClasses(type: String) = "my-admonition-$type"
    
    override fun getCss(): String = """
        :root {
            --my-primary: #0066cc;
        }
        
        .my-heading {
            color: var(--my-primary);
            font-weight: bold;
        }
        
        .my-h1 { font-size: 2em; }
        .my-h2 { font-size: 1.5em; }
        
        /* ... more styles ... */
    """.trimIndent()
}
```

### Use Custom Theme

```kotlin
val config = RenderConfig(theme = MyTheme())
val result = renderer.render(document, config)
```

## Examples

### Standalone HTML Document

```kotlin
val config = RenderConfig(
    outputOptions = OutputOptions(
        standalone = true,
        cssMode = CssMode.INLINE
    ),
    theme = DefaultTheme()
)
```

### HTML Fragment

```kotlin
val config = RenderConfig(
    outputOptions = OutputOptions(
        standalone = false
    )
)
```

### External CSS for Multiple Documents

```kotlin
val config = RenderConfig(
    outputOptions = OutputOptions(
        cssMode = CssMode.EXTERNAL,
        cssPath = "shared-styles.css"
    ),
    theme = KotlinTheme()
)

documents.forEach { doc ->
    renderer.render(doc, config)
}
```

### Custom CSS Only

```kotlin
val config = RenderConfig(
    theme = DefaultTheme(), // Still provides class names
    cssOptions = CssOptions(
        customCssPath = "custom.css",
        includeDefaultCss = false // Don't include theme CSS
    )
)
```

## Error Handling

```kotlin
val result = renderer.render(document, config)

result.fold(
    onSuccess = { html ->
        File("output.html").writeText(html)
    },
    onFailure = { error ->
        when (error) {
            is RenderException.InvalidConfiguration ->
                println("Invalid config: ${error.message}")
            is RenderException.InvalidAst ->
                println("Invalid AST: ${error.message}")
            is CssException.FileNotFound ->
                println("CSS file not found: ${error.path}")
            else ->
                println("Rendering failed: ${error.message}")
        }
    }
)
```

## Testing

The module includes comprehensive tests:

- **Unit Tests**: Individual component behavior
- **Integration Tests**: End-to-end rendering
- **Backward Compatibility Tests**: No regressions
- **Platform Tests**: Cross-platform validation

Run tests:

```bash
# All tests
./gradlew :html-renderer:test

# Platform-specific
./gradlew :html-renderer:jvmTest
./gradlew :html-renderer:iosX64Test
```

## Platform Support

- ✅ JVM (Java 11+)
- ✅ Android (API 24+)
- ✅ iOS (x64, ARM64, Simulator ARM64)
- ✅ Linux (x64)
- ✅ macOS (native)

Platform-specific file I/O is handled via `expect/actual` declarations.

## Performance

- **CSS Generation**: Once per render
- **File I/O**: Cached where possible
- **Memory**: CSS strings held only during rendering

For high-volume rendering:
- Reuse `RenderConfig` instances
- Use `CssMode.EXTERNAL` for multiple documents
- Consider CSS caching for repeated renders

## Documentation

- **[THEMING_ARCHITECTURE.md](../THEMING_ARCHITECTURE.md)** - Architecture overview
- **[THEMING_USAGE.md](../THEMING_USAGE.md)** - Complete usage guide
- **[THEMING_QUICK_REFERENCE.md](../THEMING_QUICK_REFERENCE.md)** - Quick reference
- **[KOTLIN_THEME.md](../KOTLIN_THEME.md)** - Kotlin theme details
- **[examples/THEMING_EXAMPLES.md](../examples/THEMING_EXAMPLES.md)** - Runnable examples

## API Reference

See source files for detailed API documentation:

- `Theme.kt` - Theme interface
- `HtmlRenderer.kt` - Renderer and configuration
- `CssProvider.kt` - CSS loading and merging
- `DefaultTheme.kt`, `DarkTheme.kt`, `KotlinTheme.kt`, `MinimalTheme.kt` - Built-in themes

## Contributing

When adding new features:

1. Maintain backward compatibility
2. Add tests for new functionality
3. Update documentation
4. Follow existing code patterns
5. Test on all platforms

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](../LICENSE) for details.
