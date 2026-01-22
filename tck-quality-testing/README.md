# TCK Quality Testing Module

This module provides the Technology Compatibility Kit (TCK) and quality testing infrastructure for the Kotlin Multiplatform AsciiDoc converter library.

## Overview

The TCK provides:
- **Test Fixtures**: Reusable AsciiDoc test documents and expected outputs
- **Validation Framework**: Utilities for comparing actual outputs with expected results
- **Performance Benchmarking**: Infrastructure for measuring parsing and rendering performance
- **Memory Monitoring**: Tools for tracking memory usage across platforms
- **Test Reporting**: Generators for JUnit XML, JSON, and text reports
- **Compatibility Tests**: Base classes for cross-platform validation

## Relationship to Official AsciiDoc TCK

This is currently a **custom TCK** designed for this project's specific needs (Kotlin Multiplatform compatibility, incremental development, performance tracking). 

**Future Goal**: Integrate with and pass the [official Eclipse Foundation AsciiDoc TCK](https://gitlab.eclipse.org/eclipse/asciidoc-lang/asciidoc-tck) to achieve full AsciiDoc specification conformance and certification.

**Current Status**: 
- ✅ Custom test infrastructure operational
- ✅ Growing fixture library for incremental development
- 🔄 Official TCK integration planned for future milestone
- 🔄 Spec conformance validation in progress

**Roadmap**: See [OFFICIAL_TCK_INTEGRATION.md](OFFICIAL_TCK_INTEGRATION.md) for the detailed integration plan.

## Module Structure

```
tck-quality-testing/
├── src/
│   ├── commonMain/kotlin/org/markup/poet/tck/
│   │   ├── fixtures/          # Test fixture management
│   │   ├── validation/        # Output validation utilities
│   │   ├── benchmark/         # Performance benchmarking
│   │   ├── memory/            # Memory monitoring
│   │   └── reporting/         # Test result reporting
│   ├── commonTest/kotlin/org/markup/poet/tck/
│   │   ├── compatibility/     # Cross-platform compatibility tests
│   │   ├── conformance/       # AsciiDoc spec conformance tests
│   │   ├── error/             # Error recovery tests
│   │   └── performance/       # Performance benchmark tests
│   ├── jvmMain/kotlin/        # JVM-specific implementations
│   ├── jvmTest/kotlin/        # JVM-specific tests
│   ├── androidMain/kotlin/    # Android-specific implementations
│   ├── androidHostTest/kotlin/# Android-specific tests
│   ├── iosMain/kotlin/        # iOS-specific implementations
│   ├── iosTest/kotlin/        # iOS-specific tests
│   ├── linuxX64Main/kotlin/   # Linux-specific implementations
│   └── linuxX64Test/kotlin/   # Linux-specific tests
└── fixtures/                   # Test fixture files (JSON)
    ├── blocks/
    ├── inline/
    ├── malformed/
    └── conformance/
```

## Core Components

### Test Fixtures

Test fixtures are reusable AsciiDoc documents with expected outputs:

```kotlin
import org.markup.poet.tck.fixtures.*

val loader = ResourceFixtureLoader()
val fixture = loader.loadFixture("block-paragraph-simple")

println(fixture.input)          // AsciiDoc input
println(fixture.expectedOutput) // Expected HTML output
```

### Validation Framework

Compare actual outputs with expected results:

```kotlin
import org.markup.poet.tck.validation.*

val validator = DefaultOutputValidator()
val result = validator.validate(expected, actual)

when (result) {
    is ValidationResult.Success -> println("Test passed!")
    is ValidationResult.Failure -> {
        println("Test failed: ${result.message}")
        println("Diff:\n${result.diff}")
    }
}
```

### Performance Benchmarking

Measure operation performance:

```kotlin
import org.markup.poet.tck.benchmark.*

val runner = DefaultBenchmarkRunner()
val metrics = runner.runBenchmark("parse_document") {
    parser.parse(document)
}

println("Mean: ${metrics.mean}")
println("P95: ${metrics.p95}")
println("Throughput: ${metrics.throughput} ops/sec")
```

### Memory Monitoring

Track memory usage:

```kotlin
import org.markup.poet.tck.memory.*

val monitor = PlatformMemoryMonitor()
val metrics = monitor.monitor("render_document") {
    renderer.render(ast)
}

println("Allocated: ${metrics.allocated} bytes")
println("Peak: ${metrics.peak} bytes")
println("Leak detected: ${metrics.leakDetected}")
```

### Compatibility Testing

Base class for cross-platform tests:

```kotlin
import org.markup.poet.tck.compatibility.*

class MyCompatibilityTest : CompatibilityTest() {
    @Test
    fun `should parse paragraphs consistently`() {
        runCompatibilityTest(
            fixtureId = "block-paragraph-simple",
            parser = { input -> myParser.parse(input) },
            renderer = { ast -> myRenderer.render(ast) }
        )
    }
    
    @Test
    fun `should support bold formatting`() {
        pending("Bold formatting not yet implemented")
    }
}
```

## Running Tests

```bash
# Run all TCK tests
./gradlew :tck-quality-testing:test

# Run platform-specific tests
./gradlew :tck-quality-testing:jvmTest
./gradlew :tck-quality-testing:iosX64Test
./gradlew :tck-quality-testing:linuxX64Test

# Build the module
./gradlew :tck-quality-testing:build
```

## Adding Test Fixtures

1. Create a JSON file in the appropriate `fixtures/` subdirectory
2. Follow the fixture format:

```json
{
  "id": "block-paragraph-simple",
  "category": "BLOCK_PARAGRAPH",
  "description": "Simple paragraph with plain text",
  "input": "This is a simple paragraph.",
  "expectedOutput": "<p>This is a simple paragraph.</p>",
  "metadata": {
    "spec_reference": "AsciiDoc Language Documentation - Paragraphs",
    "difficulty": "basic"
  }
}
```

3. Load the fixture in your tests using `FixtureLoader`

## Platform-Specific Implementations

### Memory Monitoring

- **JVM/Android**: Uses `Runtime.getRuntime()` for memory tracking
- **iOS/Linux**: Stub implementations (returns placeholder values)

Platform-specific implementations can be enhanced as needed.

## Dependencies

This module depends on:
- `asciidoc-parser` - For AST node types
- `html-renderer` - For rendering functionality
- `document-processing` - For document processing
- `ast-graphviz-export` - For AST visualization
- `kotlin-test` - For testing framework
- `kotest` - For property-based testing

## Design Philosophy

The TCK is designed to:
1. **Grow incrementally**: Tests can be marked as pending and enabled as features are implemented
2. **Ensure consistency**: Same tests run on all platforms to validate consistent behavior
3. **Provide reusability**: Fixtures and utilities are shared across test suites
4. **Enable CI/CD**: All tests execute via Gradle and produce standard reports

## Future Enhancements

### Official TCK Integration (Planned)

**Goal**: Integrate with the official Eclipse Foundation AsciiDoc TCK to achieve specification conformance certification.

**Planned Work**:
- Fetch and sync official TCK test cases from [Eclipse AsciiDoc TCK repository](https://gitlab.eclipse.org/eclipse/asciidoc-lang/asciidoc-tck)
- Support official TCK test format alongside custom fixtures
- Map official test cases to KMP test infrastructure
- Generate conformance reports showing pass/fail status against official tests
- Document any spec ambiguities or interpretation differences
- Achieve official AsciiDoc processor certification

**Benefits**:
- Guaranteed spec compliance
- Interoperability with other AsciiDoc processors
- Community trust and adoption
- Automatic updates when spec evolves

### Other Enhancements

- Advanced diff generation with syntax highlighting
- Performance regression visualization
- Automated fixture generation from AsciiDoc spec
- Coverage integration with Kover
- Fuzzing support for edge case discovery
- Mutation testing for test quality validation
