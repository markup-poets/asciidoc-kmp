# Theming Module

Core theming abstractions for document rendering with strong separation between document data and visual presentation.

## Overview

The theming module provides a format-agnostic way to define visual styles for documents. It enables:

- **Data/Presentation Separation**: Document structure (AST) is completely independent of styling
- **Format Agnostic**: Same theme concept works for HTML, PDF, and other output formats
- **Pluggable**: Easy to create and register custom themes
- **Composable**: Themes can extend or override other themes

## Core Concepts

### Theme

A `Theme` defines how document elements should be styled. It provides:

- **Element Styles**: CSS classes, properties, or format-specific styling data
- **Stylesheets**: Complete stylesheets in various formats (CSS, PDF styles, etc.)
- **Context-Aware**: Styling can vary based on element context (nesting level, type, attributes)

### ElementType

Semantic element types that can be styled:

- **Document Structure**: DOCUMENT, SECTION
- **Block Elements**: HEADING, PARAGRAPH, CODE_BLOCK, QUOTE, LIST, TABLE
- **Inline Elements**: TEXT, EMPHASIS, STRONG, CODE, LINK
- **Special Blocks**: ADMONITION, SIDEBAR, EXAMPLE, LITERAL, VERSE
- **Media**: IMAGE, VIDEO

### StyleContext

Additional context for styling decisions:

- **level**: Nesting level (e.g., heading level 1-6)
- **type**: Element subtype (e.g., admonition type: note, warning, tip)
- **attributes**: Custom attributes from the document
- **parent**: Parent element type
- **index**: Position in a sequence

### ElementStyle

Format-agnostic styling information:

- **classes**: CSS class names or semantic identifiers
- **properties**: Key-value style properties
- **customData**: Format-specific data

## Usage

### Using the Default Theme

```kotlin
import org.markup.poet.theming.*

val theme = Theme.default()
val headingStyle = theme.getStyle(ElementType.HEADING, StyleContext.heading(1))
// Returns: ElementStyle(classes=["heading", "heading-1"])

val css = theme.getStylesheet("css")
// Returns: Complete CSS stylesheet
```

### Creating a Custom Theme

```kotlin
class MyTheme : Theme {
    override val id: String = "my-theme"
    override val name: String = "My Theme"
    
    override fun getStyle(element: ElementType, context: StyleContext): ElementStyle {
        return when (element) {
            ElementType.HEADING -> {
                val level = context.level ?: 1
                ElementStyle.withClasses("custom-heading", "level-$level")
            }
            ElementType.PARAGRAPH -> ElementStyle.withClasses("custom-para")
            else -> ElementStyle.empty()
        }
    }
    
    override fun getStylesheet(format: String): String? {
        return when (format) {
            "css" -> """
                .custom-heading { font-weight: bold; }
                .level-1 { font-size: 2em; }
            """.trimIndent()
            else -> null
        }
    }
}
```

### Theme Registry

Register and discover themes:

```kotlin
// Register a theme
ThemeRegistry.register(MyTheme())

// Get a theme by ID
val theme = ThemeRegistry.get("my-theme")

// Get theme or default
val theme = ThemeRegistry.getOrDefault("my-theme")

// List all themes
val themeIds = ThemeRegistry.listThemes()

// Get all themes
val allThemes = ThemeRegistry.getAllThemes()
```

## Architecture

### Separation of Concerns

```
┌─────────────────┐
│   Document AST  │  ← Pure data structure
└────────┬────────┘
         │
         ↓
┌─────────────────┐
│     Theme       │  ← Visual presentation
└────────┬────────┘
         │
         ↓
┌─────────────────┐
│  Output Format  │  ← HTML, PDF, etc.
└─────────────────┘
```

The document AST knows nothing about styling. The theme knows nothing about document structure. The output format combines both.

### Format Agnostic Design

The same theme can generate output for multiple formats:

```kotlin
val theme = MyTheme()

// HTML output
val css = theme.getStylesheet("css")

// PDF output (future)
val pdfStyles = theme.getStylesheet("pdf-styles")

// Other formats
val customFormat = theme.getStylesheet("my-format")
```

### Extensibility

Themes can be extended or composed:

```kotlin
class ExtendedTheme(private val base: Theme) : Theme {
    override val id = "extended-${base.id}"
    override val name = "Extended ${base.name}"
    
    override fun getStyle(element: ElementType, context: StyleContext): ElementStyle {
        // Get base style
        val baseStyle = base.getStyle(element, context)
        
        // Add custom classes
        return baseStyle.copy(
            classes = baseStyle.classes + "extended"
        )
    }
    
    override fun getStylesheet(format: String): String? {
        val baseStylesheet = base.getStylesheet(format) ?: ""
        val customStylesheet = "/* Custom styles */"
        return baseStylesheet + "\n" + customStylesheet
    }
}
```

## Default Theme

The module includes a `DefaultTheme` with:

- Clean, minimal styling
- Semantic class names
- Accessibility best practices
- CSS custom properties (variables)
- Responsive design considerations

## Testing

```kotlin
import kotlin.test.Test
import kotlin.test.assertEquals
import org.markup.poet.theming.*

class MyThemeTest {
    @Test
    fun `should return correct heading classes`() {
        val theme = MyTheme()
        val style = theme.getStyle(ElementType.HEADING, StyleContext.heading(1))
        
        assertEquals(listOf("custom-heading", "level-1"), style.classes)
    }
}
```

## Future Enhancements

- **Theme Composition**: Combine multiple themes
- **Theme Variables**: Parameterized themes with customizable colors, fonts, etc.
- **PDF Support**: Generate PDF styling rules
- **Theme Validation**: Validate theme completeness and accessibility
- **Theme Preview**: Generate preview documents for themes

## See Also

- **examples module**: Example custom themes (KotlinTheme, etc.)
- **html-renderer module**: HTML rendering with theme support
