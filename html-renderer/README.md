# html-renderer

Renders the AsciiDoc ASG to HTML, with pluggable theming that keeps document structure separate from visual presentation. Four built-in themes (Default, Dark, Kotlin, Minimal), CSS variables, and inline/external/no-CSS output modes.

```kotlin
val config = RenderConfig(theme = KotlinTheme())
renderer.render(document, config).onSuccess(::println)
```

Runs everywhere the parser does: JVM, Android, iOS, Linux, macOS.

**Full documentation:**

- [Rendering configuration](https://markup-poets.github.io/asciidoc-kmp/reference/rendering-api.html) — `RenderConfig`, `CssOptions`, `OutputOptions`, core interfaces, CSS merge order, error handling
- [Customize themes and CSS](https://markup-poets.github.io/asciidoc-kmp/how-to/customize-theming.html) — pick a theme, override variables, write your own
- [Theming architecture](https://markup-poets.github.io/asciidoc-kmp/explanation/theming.html) — why it is built this way

```bash
./gradlew :html-renderer:jvmTest
```
