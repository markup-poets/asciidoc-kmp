# Markup Poet - AsciiDoc Converter

[![Maven Central](https://img.shields.io/maven-central/v/org.markup-poet/asciidoc-parser)](https://central.sonatype.com/namespace/org.markup-poet)
[![Build](https://github.com/markup-poets/asciidoc-kmp/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/markup-poets/asciidoc-kmp/actions/workflows/build.yml)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Docs](https://img.shields.io/badge/docs-markup--poets.github.io-blue)](https://markup-poets.github.io/asciidoc-kmp/)

**Documentation:** https://markup-poets.github.io/asciidoc-kmp/

A lightweight AsciiDoc converter library built with Kotlin Multiplatform, which aims to be compatible with the official AsciiDoc Language Specification.

## Overview

Markup Poet is a minimal AsciiDoc converter that transforms AsciiDoc markup into various output formats. Built with Kotlin Multiplatform, it runs on JVM, Android, iOS, Linux, and in the browser (WebAssembly) without external dependencies.

## Features

- **Platform Independent**: Runs on JVM, Android, iOS, Linux, and the browser via Kotlin/Wasm
- **Specification-Oriented**: Aims to be compatible with the official AsciiDoc Language Specification, tracked via its Technology Compatibility Kit (run `./run-official-tck.sh`)
- **ASG-Native**: One document model end to end — the Abstract Semantic Graph mirroring the official AsciiDoc schema
- **Broad Syntax Coverage**: Sections, lists (incl. description and callout lists), tables, admonitions, sidebars/examples/quotes, includes, conditionals, cross-references, footnotes, and rich inline formatting
- **WASM Plugins**: Custom blocks, block macros, inline macros, and converter plugins via sandboxed, language-agnostic WebAssembly — no Ruby required (see the [plugin guide](https://markup-poets.github.io/asciidoc-kmp/asciidoc-kmp/how-to/write-a-wasm-plugin.html))
- **Pluggable Theming**: Flexible styling system with built-in themes and CSS customization
- **Zero Dependencies**: No external libraries required in the core parser

## Architecture

The library follows a clean pipeline architecture built on the ASG (Abstract Semantic Graph):

1. **Parse** - AsciiDoc source text → ASG (`asciidoc-parser`)
2. **Process** - Resolves includes, conditionals, attributes, cross-references (`document-processing`)
3. **Render** - ASG → HTML with theming (`html-renderer`), or export to Graphviz DOT / official ASG JSON

## Quick Start

### Basic Rendering

```kotlin
import org.markup.poet.asciidoc.parser.DefaultAsciidocParser
import org.markup.poet.asciidoc.render.*

val document = DefaultAsciidocParser().parse(asciidocSource).document

val builder = DefaultHtmlBuilder(DefaultHtmlEscaper())
val inlineRenderer = DefaultInlineRenderer(builder)
val renderer = DefaultHtmlRenderer(DefaultBlockRenderer(builder, inlineRenderer), inlineRenderer)

renderer.render(document).onSuccess { html ->
    println(html)
}
```

### With Custom Theme

```kotlin
val config = RenderConfig(
    theme = KotlinTheme(),
    cssOptions = CssOptions(
        cssVariables = mapOf(
            "--mp-color-primary" to "#DC2626"
        )
    )
)

val result = renderer.render(document, config)
```

See the [theming guide](https://markup-poets.github.io/asciidoc-kmp/asciidoc-kmp/how-to/customize-theming.html) for theming documentation.

## Installation

Artifacts are published to [Maven Central](https://central.sonatype.com/namespace/org.markup-poet)
under the `org.markup-poet` group. Make sure `mavenCentral()` is among your
repositories (it is by default in new Gradle projects):

```kotlin
repositories {
    mavenCentral()
}
```

Then add the modules you need — each is a Kotlin Multiplatform library, so a
single `commonMain` dependency resolves the right variant per target (JVM,
Android, iOS, Linux):

```kotlin
dependencies {
    implementation("org.markup-poet:asciidoc-parser:<version>")     // parser + ASG model (zero deps)
    implementation("org.markup-poet:document-processing:<version>") // includes, conditionals, TOC, xrefs (optional)
    implementation("org.markup-poet:html-renderer:<version>")       // HTML output with theming (optional)
}
```

Pick `<version>` from the Maven Central badge above. Further modules follow the
same coordinates pattern: `asciidoc-asg` (official ASG JSON), `asg-graphviz-export`
(DOT export), `antora-resolution`/`antora-assembler` (multi-file assembly), and
the `plugin-api`/`plugin-engine`/`plugin-integration` trio (WASM plugins).

In a Kotlin Multiplatform project, declare the dependency in your shared source set:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("org.markup-poet:asciidoc-parser:<version>")
        }
    }
}
```

## Supported Platforms

- **JVM** (Java 11+)
- **Android** (API 24+)
- **iOS** (x64, ARM64, Simulator ARM64)
- **Linux** (x64)

## Theming & Styling

The library includes a powerful, pluggable theming system that strictly separates document structure from visual presentation.

### Built-in Themes

- **DefaultTheme** - Clean, minimal styling
- **DarkTheme** - Dark mode for low-light environments
- **KotlinTheme** - Kotlin-branded with red accents
- **MinimalTheme** - Bare minimum for custom styling

### Documentation

- **[Theming architecture](https://markup-poets.github.io/asciidoc-kmp/asciidoc-kmp/explanation/theming.html)** - Architecture overview and design principles

### Quick Examples

**Use a built-in theme:**
```kotlin
val config = RenderConfig(theme = DarkTheme())
renderer.render(document, config)
```

**Customize with CSS variables:**
```kotlin
val config = RenderConfig(
    theme = DefaultTheme(),
    cssOptions = CssOptions(
        cssVariables = mapOf(
            "--mp-color-primary" to "#DC2626",
            "--mp-font-family" to "Georgia, serif"
        )
    )
)
```

**Add custom CSS:**
```kotlin
val config = RenderConfig(
    theme = DefaultTheme(),
    cssOptions = CssOptions(
        customCssPath = "path/to/custom.css"
    )
)
```

**Create custom theme:**
```kotlin
class MyTheme : Theme {
    override fun headingClasses(level: Int) = "my-heading my-h$level"
    override fun getCss() = "/* your CSS */"
    // ... implement other methods
}
```

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.

## Contributing

Contributions are welcome! Please read our contribution guidelines and ensure all tests pass before submitting a pull request.

## CLI Tool

A command-line tool is included to convert AsciiDoc files to Graphviz DOT format for ASG visualization.

### Quick Usage

```bash
# Using the wrapper script
./asciidoc2dot.sh document.adoc

# Using Gradle directly
./gradlew :cli-app:jvmRun --args="document.adoc output.dot"
```

### Visualizing Output

```bash
# Generate PNG
dot -Tpng output.dot -o output.png

# Generate SVG
dot -Tsvg output.dot -o output.svg
```

See [cli-app/README.md](cli-app/README.md) for detailed CLI documentation.

## Testing & Quality Assurance

### Fixture-replay quality suite

The project includes its own fixture-replay quality suite (`tck-quality-testing`) for ensuring consistent behavior across all platforms. It is separate from the official AsciiDoc TCK and provides:

- **Test Fixtures**: Reusable AsciiDoc test documents with expected outputs
- **Validation Framework**: Utilities for comparing actual vs expected results
- **Compatibility Tests**: Cross-platform validation tests
- **Performance Benchmarking**: Infrastructure for measuring parsing and rendering performance
- **Memory Monitoring**: Tools for tracking memory usage across platforms

### Official AsciiDoc TCK

This project aims to be compatible with the official AsciiDoc Language Specification and its [Technology Compatibility Kit (TCK)](https://gitlab.eclipse.org/eclipse/asciidoc-lang/asciidoc-tck). Compatibility is a work in progress and should not be interpreted as official certification — no official certification or compatibility program for AsciiDoc implementations exists yet.

Current TCK results, kept honest in CI on every pull request:

- **TCK version tested**: `0.1.0-25-gcdfada9` (commit `cdfada9c2768b164eadf4bc12e9f9c68e6caf68a`, pinned in `tck-quality-testing/official-tck/tck-pin.txt`)
- **Passing**: 13 of 13 published tests, via the official Node.js harness with source locations compared
- **Failing**: 0
- **Skipped or unsupported tests**: none
- **Intentionally unsupported features**: none within the scope of the tested TCK; see the [TCK status page](https://markup-poets.github.io/asciidoc-kmp/asciidoc-kmp/reference/tck-and-syntax-coverage.html) for the adapter limitation on constructs the ASG schema does not cover yet
- **Reproduce**: `./run-official-tck.sh` (requires Node 20+); `./scripts/sync-official-tck.sh` checks out the pinned TCK commit

The TCK itself is still growing alongside the specification; passing all currently published tests is a snapshot, not a completeness claim.

See [tck-quality-testing/README.md](tck-quality-testing/README.md) for the quality suite's documentation and usage examples.

### Running Tests

```bash
# All tests across all platforms
./gradlew test

# Platform-specific tests
./gradlew :asciidoc-parser:jvmTest        # JVM only
./gradlew :asciidoc-parser:linuxX64Test   # Linux native

# Official TCK run (source of truth)
./run-official-tck.sh

# Fixture-replay quality suite
./gradlew :tck-quality-testing:jvmTest
```

## Development

### Building
```bash
./gradlew build
```

### Publishing
```bash
./gradlew publishToMavenLocal     # Local testing
./gradlew publishToMavenCentral   # Maven Central
```

## Trademarks

AsciiDoc® is a trademark of the Eclipse Foundation, Inc. Markup Poet is an independent project; it is not affiliated with, endorsed by, or certified by the Eclipse Foundation or the AsciiDoc Working Group.
