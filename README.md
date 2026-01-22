# Markup Poet - AsciiDoc Converter

A lightweight, spec-compliant AsciiDoc converter library built with Kotlin Multiplatform.

## Overview

Markup Poet is a minimal AsciiDoc converter that transforms AsciiDoc markup into various output formats. Built with Kotlin Multiplatform, it runs on JVM, Android, iOS, and Linux platforms without external dependencies.

## Features

- **Platform Independent**: Runs on JVM, Android, iOS, and Linux
- **Spec Compliant**: Follows AsciiDoc Language Specification
- **Clean Architecture**: Clear separation between parsing, processing, conversion, and rendering phases
- **Extensible**: Modular design allows custom processors and converters
- **Zero Dependencies**: No external libraries required

## Architecture

The library follows a clean pipeline architecture:

1. **Parse** - Analyzes AsciiDoc source text → AST
2. **Process** - Resolves includes, attributes, and substitutions  
3. **Convert** - Transforms AST into target format (HTML, etc.)
4. **Render** - Final output or persistence

## Quick Start

```kotlin
val parser = AsciidocParser()
val ast = parser.parse(input)

val processor = AsciidocProcessorPipeline()
processor.process(ast)

val html = HtmlConverter().convert(ast)
HtmlRenderer().render(html, outputFile)
```

## Installation

Add to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("org.markup.poet:asciidoc-core:$version")
}
```

## Supported Platforms

- **JVM** (Java 11+)
- **Android** (API 24+)
- **iOS** (x64, ARM64, Simulator ARM64)
- **Linux** (x64)

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.

## Contributing

Contributions are welcome! Please read our contribution guidelines and ensure all tests pass before submitting a pull request.

## CLI Tool

A command-line tool is included to convert AsciiDoc files to Graphviz DOT format for AST visualization.

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

### Technology Compatibility Kit (TCK)

The project includes a comprehensive TCK for ensuring consistent behavior across all platforms. The TCK provides:

- **Test Fixtures**: Reusable AsciiDoc test documents with expected outputs
- **Validation Framework**: Utilities for comparing actual vs expected results
- **Compatibility Tests**: Cross-platform validation tests
- **Performance Benchmarking**: Infrastructure for measuring parsing and rendering performance
- **Memory Monitoring**: Tools for tracking memory usage across platforms

**Note**: This is currently a custom TCK designed for Kotlin Multiplatform development. The project roadmap includes integration with the [official Eclipse Foundation AsciiDoc TCK](https://gitlab.eclipse.org/eclipse/asciidoc-lang/asciidoc-tck) to achieve full specification conformance and certification. See [tck-quality-testing/OFFICIAL_TCK_INTEGRATION.md](tck-quality-testing/OFFICIAL_TCK_INTEGRATION.md) for the integration roadmap.

See [tck-quality-testing/README.md](tck-quality-testing/README.md) for detailed TCK documentation and usage examples.

### Running Tests

```bash
# All tests across all platforms
./gradlew test

# Platform-specific tests
./gradlew :library:jvmTest        # JVM only
./gradlew :library:iosX64Test     # iOS only

# TCK tests
./gradlew :tck-quality-testing:test
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
