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

This module includes both a **custom TCK** for project-specific needs and **Official AsciiDoc TCK Integration** for specification conformance.

### Custom TCK (Operational ✅)
- Designed for Kotlin Multiplatform compatibility
- Supports incremental development with pending tests
- Performance tracking and benchmarking
- Memory monitoring across platforms
- Growing fixture library

### Official TCK Integration (In Progress 🔄)

**Goal**: Integrate with and pass the [official Eclipse Foundation AsciiDoc TCK](https://gitlab.eclipse.org/eclipse/asciidoc-lang/asciidoc-tck) to achieve full AsciiDoc specification conformance and certification.

**Current Status** (~54% Complete):
- ✅ **Phase 1**: Research & Analysis (100%)
- 🔄 **Phase 2**: TCK Sync System (89%)
- 🔄 **Phase 3**: Official Test Format Support (71%)
- 🔄 **Phase 4**: Dual Format Support (50%)
- ✅ **Phase 5**: Test Execution System (100%)
- 🔄 **Phase 6**: Conformance Reporting (73%)
- ✅ **Phase 7**: Version Tracking & Configuration (100%)
- 🔄 **Phase 8**: Gradle Tasks & Public API (25%)
- ⏳ **Phase 9**: Integration Testing & Documentation (0%)
- ⏳ **Phase 10**: CI/CD Integration (0%)

**Key Achievements**:
- ✅ Pure Kotlin implementation (no JavaScript dependencies)
- ✅ Multiplatform support (JVM, iOS, Linux)
- ✅ 171 tests (161 unit + 10 property-based)
- ✅ Complete public API (TckIntegration)
- ✅ Sync, fixture loading, test execution, reporting all operational

**Documentation**:
- [TCK_IMPLEMENTATION_STATUS.md](TCK_IMPLEMENTATION_STATUS.md) - Detailed progress report
- [OFFICIAL_TCK_PROGRESS.md](OFFICIAL_TCK_PROGRESS.md) - Phase-by-phase status
- [PHASE_5_SUMMARY.md](PHASE_5_SUMMARY.md) - Test execution system
- [PHASE_6_SUMMARY.md](PHASE_6_SUMMARY.md) - Conformance reporting
- [PHASE_7_SUMMARY.md](PHASE_7_SUMMARY.md) - Version tracking & config
- [PHASE_8_SUMMARY.md](PHASE_8_SUMMARY.md) - Public API

**Usage Example**:
```kotlin
// Initialize TCK system
val context = TckIntegration.initialize()

// Sync official TCK repository
val syncResult = TckIntegration.sync(context)
println("Synced ${syncResult.metadata.testCount} tests")

// Run all tests
val results = TckIntegration.runTests(context)
println("Passed: ${results.passed}/${results.totalTests}")

// Generate conformance report
val report = TckIntegration.generateReport(context, results)

// Check certification readiness
val status = TckIntegration.checkCertification(context, results)
println("Certification ready: ${status.isReady}")
```

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
    renderer.render(asg)
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
            renderer = { asg -> myRenderer.render(asg) }
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
- `asciidoc-parser` - For ASG node types
- `html-renderer` - For rendering functionality
- `document-processing` - For document processing
- `asg-graphviz-export` - For ASG visualization
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
