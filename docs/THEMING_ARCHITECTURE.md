# Theming Architecture

## Overview

The theming system provides a **pluggable, flexible architecture** for styling HTML output from AsciiDoc documents. It strictly separates **document structure** (data/AST) from **visual presentation** (CSS/styling), allowing complete customization without modifying the renderer.

## Design Principles

### 1. Separation of Concerns
- **Document Structure**: AST nodes represent semantic content only
- **Visual Presentation**: Themes provide CSS classes and stylesheets
- **Rendering Logic**: HtmlRenderer orchestrates without knowing styling details

### 2. Pluggability
- Any class implementing `Theme` interface can be used
- Themes are passed to renderer via configuration
- No hardcoded styling in renderer logic

### 3. Flexibility
- Built-in themes for common use cases
- Custom CSS can override or extend theme styles
- CSS variables enable fine-grained customization
- Multiple CSS inclusion modes (inline, external, none)

## Architecture Components

```
┌─────────────────────────────────────────────────────────────┐
│                      HtmlRenderer                            │
│  (Orchestrates rendering, uses Theme for CSS classes)       │
└────────────────┬────────────────────────────────────────────┘
                 │
                 │ uses
                 ▼
┌─────────────────────────────────────────────────────────────┐
│                    Theme Interface                           │
│  - headingClasses(level)                                     │
│  - paragraphClasses()                                        │
│  - codeBlockClasses()                                        │
│  - getCss()                                                  │
└────────────────┬────────────────────────────────────────────┘
                 │
                 │ implemented by
                 ▼
┌─────────────────────────────────────────────────────────────┐
│              Built-in Themes                                 │
│  - DefaultTheme (clean, minimal)                             │
│  - DarkTheme (dark mode)                                     │
│  - KotlinTheme (Kotlin-branded)                              │
│  - MinimalTheme (bare minimum)                               │
└─────────────────────────────────────────────────────────────┘
                 │
                 │ extended by
                 ▼
┌─────────────────────────────────────────────────────────────┐
│              Custom Themes                                   │
│  (User-defined implementations)                              │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    CssProvider                               │
│  - Loads custom CSS from files or strings                    │
│  - Merges theme CSS with custom CSS                          │
│  - Applies CSS variable overrides                            │
└─────────────────────────────────────────────────────────────┘
```

## Core Interfaces

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
    
    companion object {
        fun default(): Theme = DefaultTheme()
    }
}
```

### CssProvider Interface

```kotlin
interface CssProvider {
    fun provideCss(cssOptions: CssOptions, theme: Theme): Result<String>
}
```

## Configuration

### RenderConfig

```kotlin
data class RenderConfig(
    val outputOptions: OutputOptions = OutputOptions.default(),
    val theme: Theme = Theme.default(),
    val cssOptions: CssOptions = CssOptions.default(),
    // ... other options
)
```

### CssOptions

```kotlin
data class CssOptions(
    val customCssContent: String? = null,
    val customCssPath: String? = null,
    val includeDefaultCss: Boolean = true,
    val cssVariableOverrides: Map<String, String> = emptyMap()
)
```

### OutputOptions

```kotlin
data class OutputOptions(
    val standalone: Boolean = true,
    val cssMode: CssMode = CssMode.INLINE,
    val cssPath: String? = null,
    // ... other options
)

enum class CssMode {
    NONE,    // No CSS included
    INLINE,  // CSS in <style> tag
    EXTERNAL // CSS in external file
}
```

## Built-in Themes

### DefaultTheme
Clean, minimal styling suitable for most documents.

**Features:**
- Light background with dark text
- CSS variables for easy customization
- Good readability and contrast
- Semantic class names

**CSS Variables:**
- `--mp-color-primary`: Primary accent color
- `--mp-color-text`: Main text color
- `--mp-color-background`: Background color
- `--mp-font-family`: Base font family
- `--mp-spacing-unit`: Base spacing unit
- And many more...

### DarkTheme
Dark mode with carefully chosen colors for low-light environments.

**Features:**
- Dark background (#1e1e1e) with light text
- Reduced eye strain in low-light conditions
- Good contrast ratios for accessibility
- Suitable for code-heavy documents

### KotlinTheme
Kotlin-branded theme with neuroSKai design system colors.

**Features:**
- Dark background (#0A0B0D) with red accents (#DC2626)
- Modern typography with system fonts
- Responsive design
- Professional appearance for technical content

### MinimalTheme
Bare minimum styling for maximum customization.

**Features:**
- Minimal CSS footprint
- Basic structure only
- Ideal starting point for custom themes

## Usage Examples

See [THEMING_USAGE.md](./THEMING_USAGE.md) for detailed usage examples.

## CSS Variable System

All built-in themes use CSS variables following a consistent naming convention:

```
--mp-{category}-{property}
```

**Categories:**
- `color`: Color values
- `font`: Typography settings
- `spacing`: Margins, padding, gaps
- `border`: Border styles
- `line-height`: Line height values

**Examples:**
- `--mp-color-primary`
- `--mp-font-family`
- `--mp-spacing-unit`
- `--mp-border-radius`

This allows users to customize themes without writing custom CSS:

```kotlin
val config = RenderConfig(
    theme = DefaultTheme(),
    cssOptions = CssOptions(
        cssVariableOverrides = mapOf(
            "--mp-color-primary" to "#DC2626",
            "--mp-font-family" to "Georgia, serif"
        )
    )
)
```

## Extensibility

### Creating Custom Themes

Implement the `Theme` interface:

```kotlin
class MyCustomTheme : Theme {
    override fun headingClasses(level: Int) = "my-heading my-h$level"
    override fun paragraphClasses() = "my-paragraph"
    // ... implement other methods
    
    override fun getCss(): String = """
        .my-heading { font-weight: bold; }
        .my-h1 { font-size: 2em; }
        .my-paragraph { margin: 1em 0; }
        /* ... more CSS */
    """.trimIndent()
}
```

### Using Custom CSS

Add custom CSS alongside theme styles:

```kotlin
val config = RenderConfig(
    theme = DefaultTheme(),
    cssOptions = CssOptions(
        customCssContent = """
            .heading-1 { color: red; }
            .code-block { background: #f0f0f0; }
        """.trimIndent()
    )
)
```

Or load from a file:

```kotlin
val config = RenderConfig(
    theme = DefaultTheme(),
    cssOptions = CssOptions(
        customCssPath = "path/to/custom.css"
    )
)
```

### Disabling Default Theme CSS

Use only custom CSS:

```kotlin
val config = RenderConfig(
    theme = DefaultTheme(), // Still provides class names
    cssOptions = CssOptions(
        customCssPath = "path/to/custom.css",
        includeDefaultCss = false // Don't include theme CSS
    )
)
```

## CSS Merge Order

When multiple CSS sources are provided, they are merged in this order:

1. **CSS Variable Overrides** (`:root` block)
2. **Theme CSS** (from `theme.getCss()`)
3. **Custom CSS** (from `cssOptions.customCssContent` or `customCssPath`)

This ensures custom CSS can override theme styles while maintaining variable definitions.

## Platform Support

The theming system works across all Kotlin Multiplatform targets:

- ✅ JVM
- ✅ Android
- ✅ iOS
- ✅ Linux
- ✅ macOS (native)

Platform-specific file I/O is handled via `expect/actual` declarations in `PlatformFileReader` and `PlatformFileWriter`.

## Backward Compatibility

The theming system maintains full backward compatibility:

- Default configuration produces identical output to previous versions
- Existing `Theme` interface unchanged
- No breaking changes to public APIs
- All new features are opt-in via configuration

## Performance Considerations

- **CSS Generation**: Themes generate CSS once per render
- **File I/O**: Custom CSS files are read once per render
- **CSS Merging**: String concatenation is efficient for typical CSS sizes
- **Memory**: CSS strings are held in memory during rendering only

For high-volume rendering, consider:
- Caching CSS content in memory
- Using `CssMode.EXTERNAL` to avoid inline CSS duplication
- Reusing `RenderConfig` instances

## Testing

The theming system includes comprehensive tests:

- **Unit Tests**: Individual component behavior
- **Integration Tests**: End-to-end rendering with themes
- **Backward Compatibility Tests**: Ensure no regressions
- **Platform Tests**: Verify cross-platform functionality

See test files in `html-renderer/src/commonTest/` for examples.

## Future Enhancements

Potential future improvements:

- Theme registry for named theme lookup
- Theme composition (combine multiple themes)
- CSS preprocessing (SASS/LESS support)
- Theme validation and linting
- Visual theme preview tool
- Theme marketplace/repository

## Related Documentation

- [THEMING_USAGE.md](./THEMING_USAGE.md) - Usage examples and recipes
- [KOTLIN_THEME.md](./KOTLIN_THEME.md) - Kotlin theme details
- [html-renderer/README.md](./html-renderer/README.md) - HTML renderer documentation
- [.kiro/specs/custom-css-styling/](../.kiro/specs/custom-css-styling/) - Original specification
