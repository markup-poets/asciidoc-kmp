# neuroSKai Theme for Kotlin Compose Multiplatform

A dark/light theme system matching the neuroSKai web design system.

## Files

- **Color.kt** - All color definitions for both dark and light themes
- **Theme.kt** - Material3 theme setup with `neuroSKaiTheme` composable
- **Type.kt** - Typography definitions using Orbitron (display) and Inter (body)
- **ExtendedColors.kt** - Additional colors not covered by Material3 (glows, gradients, sidebar)

## Usage

### Basic Setup

```kotlin
// In your App.kt or main composable
@Composable
fun App() {
    neuroSKaiTheme(
        darkTheme = true // or use isSystemInDarkTheme()
    ) {
        // Your app content
    }
}
```

### Accessing Colors

```kotlin
@Composable
fun MyComponent() {
    // Material3 colors
    val primary = MaterialTheme.colorScheme.primary
    val background = MaterialTheme.colorScheme.background
    
    // Extended colors (glows, nodes, sidebar)
    val glow = ExtendedTheme.colors.primaryGlow
    val gradient = ExtendedTheme.colors.radialGradient
}
```

### Extended Theme Provider

To use extended colors, wrap your theme:

```kotlin
@Composable
fun App() {
    val darkTheme = isSystemInDarkTheme()
    
    CompositionLocalProvider(
        LocalExtendedColors provides if (darkTheme) DarkExtendedColors else LightExtendedColors
    ) {
        neuroSKaiTheme(darkTheme = darkTheme) {
            // Your app content
        }
    }
}
```

## Font Setup

Add Orbitron and Inter fonts to your KMP resources:

```
commonMain/
  resources/
    font/
      orbitron_regular.ttf
      orbitron_medium.ttf
      orbitron_semibold.ttf
      orbitron_bold.ttf
      inter_light.ttf
      inter_regular.ttf
      inter_medium.ttf
```

Then update Type.kt to load them using your platform's font loading mechanism.

## Color Palette

| Token | Dark | Light |
|-------|------|-------|
| Primary | #DC2626 (Red) | #DC2626 |
| Background | #0A0B0D | #FAFAFA |
| Surface | #0F1114 | #FFFFFF |
| Card | #121418 | #FFFFFF |
| Border | #262B31 | #E2E8F0 |
