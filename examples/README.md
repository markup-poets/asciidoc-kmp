# Examples Module

This module demonstrates custom theme capabilities and pluggable extensions for the theming system.

## Custom Themes

### Kotlin Theme

A dark theme with red accents inspired by Kotlin branding and the neuroSKai design system.

**Features:**
- Dark background (#0A0B0D) with high contrast text
- Red accent color (#DC2626) for emphasis
- Modern typography with system fonts
- Responsive design for mobile and desktop
- Accessibility-focused color contrast
- Smooth transitions and hover effects

**Usage:**

```kotlin
import org.markup.poet.examples.KotlinTheme
import org.markup.poet.theming.ThemeRegistry

// Register the theme
ThemeRegistry.register(KotlinTheme())

// Use the theme
val theme = ThemeRegistry.get("kotlin")
val css = theme?.getStylesheet("css")
```

## Creating Custom Themes

To create your own custom theme:

1. **Implement the Theme interface:**

```kotlin
import org.markup.poet.theming.*

class MyCustomTheme : Theme {
    override val id: String = "my-theme"
    override val name: String = "My Custom Theme"
    override val description: String = "A beautiful custom theme"
    
    override fun getStyle(element: ElementType, context: StyleContext): ElementStyle {
        return when (element) {
            ElementType.HEADING -> {
                val level = context.level ?: 1
                ElementStyle.withClasses("my-heading", "my-heading-$level")
            }
            // ... handle other element types
            else -> ElementStyle.empty()
        }
    }
    
    override fun getStylesheet(format: String): String? {
        return when (format.lowercase()) {
            "css" -> """
                /* Your custom CSS here */
                .my-heading { color: #ff0000; }
            """.trimIndent()
            else -> null
        }
    }
}
```

2. **Register your theme:**

```kotlin
ThemeRegistry.register(MyCustomTheme())
```

3. **Use your theme:**

```kotlin
val theme = ThemeRegistry.get("my-theme")
```

## Design Principles

### Data/Presentation Separation

Themes only define **how things look**, not **what they are**. The document structure (AST) remains completely independent of visual presentation.

### Format Agnostic

The same theme concept works across different output formats:
- **HTML**: CSS stylesheets
- **PDF**: PDF styling rules (future)
- **Other formats**: Extensible design

### Pluggable Extensions

Themes are easy to create, register, and swap:
- No modification of core code required
- Register themes at runtime
- Switch themes dynamically
- Compose and extend existing themes

## Example Themes Included

- **KotlinTheme**: Dark theme with Kotlin branding
- More themes coming soon!

## Contributing

To contribute a new example theme:

1. Create a new file in `src/commonMain/kotlin/org/markup/poet/examples/`
2. Implement the `Theme` interface
3. Add tests in `src/commonTest/kotlin/`
4. Update this README with usage examples
5. Submit a pull request

## License

Same as the parent project.
