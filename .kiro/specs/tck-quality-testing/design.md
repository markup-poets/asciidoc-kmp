# Design Document: TCK Quality Testing

## Overview

The Technology Compatibility Kit (TCK) provides a comprehensive testing infrastructure for the Kotlin Multiplatform AsciiDoc converter library. This design establishes reusable test fixtures, validation frameworks, performance benchmarking tools, and quality assurance mechanisms that will validate features as they are implemented across all supported platforms (JVM, Android, iOS, Linux).

The TCK is designed to grow incrementally alongside feature development, providing the testing foundation needed to ensure correctness, performance, and cross-platform consistency.

## Architecture

### High-Level Structure

```
tck-quality-testing/
├── src/
│   ├── commonMain/kotlin/org/markup/poet/tck/
│   │   ├── fixtures/          # Test fixture management
│   │   ├── validation/        # Test validation utilities
│   │   ├── benchmark/         # Performance benchmarking
│   │   ├── memory/            # Memory monitoring
│   │   └── reporting/         # Test result reporting
│   ├── commonTest/kotlin/org/markup/poet/tck/
│   │   ├── compatibility/     # Cross-platform compatibility tests
│   │   ├── conformance/       # AsciiDoc spec conformance tests
│   │   ├── error/             # Error recovery tests
│   │   └── performance/       # Performance benchmark tests
│   ├── jvmTest/kotlin/        # JVM-specific tests
│   ├── androidHostTest/kotlin/# Android-specific tests
│   ├── iosTest/kotlin/        # iOS-specific tests
│   └── linuxX64Test/kotlin/   # Linux-specific tests
└── fixtures/                   # Test fixture files (AsciiDoc documents)
    ├── blocks/
    ├── inline/
    ├── attributes/
    ├── macros/
    ├── malformed/
    └── conformance/
```

### Design Principles

1. **Incremental Enablement**: Tests can be marked as pending and enabled as features are implemented
2. **Platform Consistency**: Same tests run on all platforms to ensure consistent behavior
3. **Fixture Reusability**: Test fixtures are shared across multiple test suites
4. **Clear Reporting**: Test failures provide actionable information about what failed and where
5. **CI-Friendly**: All tests execute via Gradle and produce standard reports

## Components and Interfaces

### 1. Test Fixture Management

**Purpose**: Provide reusable AsciiDoc test documents and expected outputs.

```kotlin
package org.markup.poet.tck.fixtures

/**
 * Represents a test fixture with input AsciiDoc and expected output.
 */
data class TestFixture(
    val id: String,
    val category: FixtureCategory,
    val description: String,
    val input: String,
    val expectedOutput: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

enum class FixtureCategory {
    BLOCK_PARAGRAPH,
    BLOCK_HEADING,
    BLOCK_LIST,
    BLOCK_TABLE,
    BLOCK_CODE,
    BLOCK_QUOTE,
    INLINE_BOLD,
    INLINE_ITALIC,
    INLINE_MONOSPACE,
    INLINE_SUBSCRIPT,
    INLINE_SUPERSCRIPT,
    ATTRIBUTE,
    MACRO,
    CROSS_REFERENCE,
    INCLUDE,
    MALFORMED_BLOCK,
    MALFORMED_INLINE,
    MALFORMED_ATTRIBUTE,
    CIRCULAR_INCLUDE,
    MISSING_INCLUDE,
    CONFORMANCE
}

/**
 * Loads and manages test fixtures.
 */
interface FixtureLoader {
    /**
     * Load a specific fixture by ID.
     */
    fun loadFixture(id: String): TestFixture
    
    /**
     * Load all fixtures in a category.
     */
    fun loadFixturesByCategory(category: FixtureCategory): List<TestFixture>
    
    /**
     * Load all available fixtures.
     */
    fun loadAllFixtures(): List<TestFixture>
}

/**
 * Default implementation that loads fixtures from embedded resources.
 */
class ResourceFixtureLoader : FixtureLoader {
    override fun loadFixture(id: String): TestFixture {
        // Load from resources
        TODO("Implementation")
    }
    
    override fun loadFixturesByCategory(category: FixtureCategory): List<TestFixture> {
        // Load all fixtures in category
        TODO("Implementation")
    }
    
    override fun loadAllFixtures(): List<TestFixture> {
        // Load all fixtures
        TODO("Implementation")
    }
}
```

### 2. Test Validation Framework

**Purpose**: Provide utilities for comparing actual outputs with expected outputs.

```kotlin
package org.markup.poet.tck.validation

/**
 * Result of a validation check.
 */
sealed class ValidationResult {
    data class Success(val message: String = "Validation passed") : ValidationResult()
    data class Failure(
        val message: String,
        val expected: String,
        val actual: String,
        val diff: String? = null
    ) : ValidationResult()
}

/**
 * Validates test outputs against expected results.
 */
interface OutputValidator {
    /**
     * Compare actual output with expected output.
     */
    fun validate(expected: String, actual: String): ValidationResult
    
    /**
     * Compare actual output with expected output, ignoring whitespace differences.
     */
    fun validateIgnoringWhitespace(expected: String, actual: String): ValidationResult
}

/**
 * Default implementation with diff generation.
 */
class DefaultOutputValidator : OutputValidator {
    override fun validate(expected: String, actual: String): ValidationResult {
        return if (expected == actual) {
            ValidationResult.Success()
        } else {
            ValidationResult.Failure(
                message = "Output mismatch",
                expected = expected,
                actual = actual,
                diff = generateDiff(expected, actual)
            )
        }
    }
    
    override fun validateIgnoringWhitespace(expected: String, actual: String): ValidationResult {
        val normalizedExpected = normalizeWhitespace(expected)
        val normalizedActual = normalizeWhitespace(actual)
        return validate(normalizedExpected, normalizedActual)
    }
    
    private fun generateDiff(expected: String, actual: String): String {
        // Generate unified diff
        TODO("Implementation")
    }
    
    private fun normalizeWhitespace(text: String): String {
        return text.trim().replace(Regex("\\s+"), " ")
    }
}
```

### 3. Performance Benchmarking

**Purpose**: Measure and report performance metrics for parsing and rendering operations.

```kotlin
package org.markup.poet.tck.benchmark

import kotlin.time.Duration
import kotlin.time.measureTime

/**
 * Performance metrics for a benchmark run.
 */
data class BenchmarkMetrics(
    val operationName: String,
    val iterations: Int,
    val mean: Duration,
    val median: Duration,
    val p95: Duration,
    val p99: Duration,
    val min: Duration,
    val max: Duration,
    val throughput: Double // operations per second
)

/**
 * Runs performance benchmarks.
 */
interface BenchmarkRunner {
    /**
     * Run a benchmark with the specified number of iterations.
     */
    fun runBenchmark(
        name: String,
        iterations: Int = 100,
        warmupIterations: Int = 10,
        operation: () -> Unit
    ): BenchmarkMetrics
    
    /**
     * Run a benchmark and compare against baseline.
     */
    fun runBenchmarkWithBaseline(
        name: String,
        baseline: BenchmarkMetrics,
        iterations: Int = 100,
        operation: () -> Unit
    ): BenchmarkComparison
}

/**
 * Comparison between current and baseline metrics.
 */
data class BenchmarkComparison(
    val current: BenchmarkMetrics,
    val baseline: BenchmarkMetrics,
    val meanDelta: Double, // percentage change
    val regressionDetected: Boolean
)

/**
 * Default implementation using kotlin.time.
 */
class DefaultBenchmarkRunner : BenchmarkRunner {
    override fun runBenchmark(
        name: String,
        iterations: Int,
        warmupIterations: Int,
        operation: () -> Unit
    ): BenchmarkMetrics {
        // Warmup
        repeat(warmupIterations) { operation() }
        
        // Measure
        val durations = mutableListOf<Duration>()
        repeat(iterations) {
            val duration = measureTime { operation() }
            durations.add(duration)
        }
        
        return calculateMetrics(name, durations)
    }
    
    override fun runBenchmarkWithBaseline(
        name: String,
        baseline: BenchmarkMetrics,
        iterations: Int,
        operation: () -> Unit
    ): BenchmarkComparison {
        val current = runBenchmark(name, iterations, operation = operation)
        val meanDelta = ((current.mean - baseline.mean) / baseline.mean) * 100.0
        val regressionDetected = meanDelta > 10.0 // 10% threshold
        
        return BenchmarkComparison(current, baseline, meanDelta, regressionDetected)
    }
    
    private fun calculateMetrics(name: String, durations: List<Duration>): BenchmarkMetrics {
        val sorted = durations.sorted()
        val mean = durations.fold(Duration.ZERO) { acc, d -> acc + d } / durations.size
        val median = sorted[sorted.size / 2]
        val p95 = sorted[(sorted.size * 0.95).toInt()]
        val p99 = sorted[(sorted.size * 0.99).toInt()]
        val throughput = 1000.0 / mean.inWholeMilliseconds.toDouble()
        
        return BenchmarkMetrics(
            operationName = name,
            iterations = durations.size,
            mean = mean,
            median = median,
            p95 = p95,
            p99 = p99,
            min = sorted.first(),
            max = sorted.last(),
            throughput = throughput
        )
    }
}
```

### 4. Memory Monitoring

**Purpose**: Track memory usage during operations.

```kotlin
package org.markup.poet.tck.memory

/**
 * Memory usage snapshot.
 */
data class MemorySnapshot(
    val timestamp: Long,
    val usedMemory: Long, // bytes
    val totalMemory: Long, // bytes
    val freeMemory: Long // bytes
)

/**
 * Memory usage metrics for an operation.
 */
data class MemoryMetrics(
    val operationName: String,
    val before: MemorySnapshot,
    val after: MemorySnapshot,
    val peak: Long, // bytes
    val allocated: Long, // bytes
    val leakDetected: Boolean
)

/**
 * Monitors memory usage during operations.
 */
interface MemoryMonitor {
    /**
     * Take a memory snapshot.
     */
    fun snapshot(): MemorySnapshot
    
    /**
     * Monitor memory usage during an operation.
     */
    fun monitor(name: String, operation: () -> Unit): MemoryMetrics
    
    /**
     * Force garbage collection (platform-specific).
     */
    fun forceGC()
}

/**
 * Platform-specific memory monitoring.
 */
expect class PlatformMemoryMonitor() : MemoryMonitor {
    override fun snapshot(): MemorySnapshot
    override fun monitor(name: String, operation: () -> Unit): MemoryMetrics
    override fun forceGC()
}
```

### 5. Test Result Reporting

**Purpose**: Generate structured reports for test results, benchmarks, and coverage.

```kotlin
package org.markup.poet.tck.reporting

/**
 * Test execution result.
 */
data class TestResult(
    val testName: String,
    val platform: String,
    val status: TestStatus,
    val duration: kotlin.time.Duration,
    val errorMessage: String? = null,
    val stackTrace: String? = null
)

enum class TestStatus {
    PASSED,
    FAILED,
    SKIPPED,
    PENDING
}

/**
 * Test suite execution summary.
 */
data class TestSummary(
    val totalTests: Int,
    val passed: Int,
    val failed: Int,
    val skipped: Int,
    val pending: Int,
    val duration: kotlin.time.Duration,
    val results: List<TestResult>
)

/**
 * Generates test reports in various formats.
 */
interface ReportGenerator {
    /**
     * Generate JUnit XML report.
     */
    fun generateJUnitXml(summary: TestSummary): String
    
    /**
     * Generate JSON report.
     */
    fun generateJson(summary: TestSummary): String
    
    /**
     * Generate human-readable text report.
     */
    fun generateText(summary: TestSummary): String
}

/**
 * Benchmark report.
 */
data class BenchmarkReport(
    val platform: String,
    val timestamp: Long,
    val benchmarks: List<BenchmarkMetrics>
)

/**
 * Generates benchmark reports.
 */
interface BenchmarkReportGenerator {
    /**
     * Generate JSON benchmark report.
     */
    fun generateJson(report: BenchmarkReport): String
    
    /**
     * Compare benchmark reports and detect regressions.
     */
    fun compareReports(current: BenchmarkReport, baseline: BenchmarkReport): String
}
```

### 6. Compatibility Test Suite

**Purpose**: Validate that features work consistently across all platforms.

```kotlin
package org.markup.poet.tck.compatibility

/**
 * Base class for compatibility tests.
 */
abstract class CompatibilityTest {
    protected val fixtureLoader: FixtureLoader = ResourceFixtureLoader()
    protected val validator: OutputValidator = DefaultOutputValidator()
    
    /**
     * Run a compatibility test using a fixture.
     */
    protected fun runCompatibilityTest(
        fixtureId: String,
        parser: (String) -> Any,
        renderer: (Any) -> String
    ) {
        val fixture = fixtureLoader.loadFixture(fixtureId)
        val parsed = parser(fixture.input)
        val rendered = renderer(parsed)
        
        fixture.expectedOutput?.let { expected ->
            val result = validator.validate(expected, rendered)
            when (result) {
                is ValidationResult.Success -> {
                    // Test passed
                }
                is ValidationResult.Failure -> {
                    throw AssertionError(
                        "Compatibility test failed for fixture $fixtureId:\n" +
                        result.message + "\n" +
                        "Diff:\n${result.diff}"
                    )
                }
            }
        }
    }
    
    /**
     * Mark a test as pending (not yet implemented).
     */
    protected fun pending(reason: String): Nothing {
        throw PendingTestException(reason)
    }
}

class PendingTestException(message: String) : Exception(message)
```

## Data Models

### Test Fixture File Format

Test fixtures are stored as JSON files with the following structure:

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

### Benchmark Results Format

Benchmark results are stored as JSON:

```json
{
  "platform": "jvm",
  "timestamp": 1704067200000,
  "benchmarks": [
    {
      "operationName": "parse_small_document",
      "iterations": 100,
      "mean": "5.2ms",
      "median": "5.0ms",
      "p95": "6.5ms",
      "p99": "7.8ms",
      "min": "4.5ms",
      "max": "8.2ms",
      "throughput": 192.3
    }
  ]
}
```


## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Fixture Category Completeness

*For any* fixture category defined in the system, the fixture loader SHALL successfully load at least one fixture of that category.

**Validates: Requirements 1.1, 1.2, 1.7**

### Property 2: Fixture Category Filtering

*For any* fixture category, when loading fixtures by that category, all returned fixtures SHALL belong to that category and no other.

**Validates: Requirements 1.8**

### Property 3: Fixture with Expected Output

*For any* fixture that has an expectedOutput field, the fixture loader SHALL preserve the expected output value when loading the fixture.

**Validates: Requirements 1.6**

### Property 4: Validation Utilities Correctness

*For any* two strings, when they are identical, the output validator SHALL return a Success result, and when they differ, it SHALL return a Failure result with both expected and actual values.

**Validates: Requirements 1.9**

### Property 5: Test Failure Reporting

*For any* test failure, the failure message SHALL contain the fixture ID and a description of what failed.

**Validates: Requirements 2.4**

### Property 6: Benchmark Metrics Completeness

*For any* benchmark run with N iterations, the resulting BenchmarkMetrics SHALL contain mean, median, p95, p99, min, max, and throughput values.

**Validates: Requirements 3.1, 3.2, 3.4, 3.8**

### Property 7: Benchmark JSON Serialization

*For any* BenchmarkReport, serializing it to JSON and parsing it back SHALL produce an equivalent report with all metrics preserved.

**Validates: Requirements 3.6**

### Property 8: Memory Metrics Completeness

*For any* monitored operation, the resulting MemoryMetrics SHALL contain before snapshot, after snapshot, peak memory, and allocated memory values.

**Validates: Requirements 4.1, 4.2, 4.3**

### Property 9: Error Information Structure

*For any* structured error information, it SHALL contain line number, column number, error type, and error message fields.

**Validates: Requirements 5.7**

### Property 10: Malformed Input Safety

*For any* malformed AsciiDoc input fixture, processing it through the error recovery handler SHALL not throw unhandled exceptions.

**Validates: Requirements 5.8**

### Property 11: JUnit XML Report Validity

*For any* TestSummary, generating a JUnit XML report SHALL produce valid XML that conforms to the JUnit XML schema.

**Validates: Requirements 9.2**

## Error Handling

### Fixture Loading Errors

**Strategy**: When a fixture file is missing or malformed, the FixtureLoader should throw a descriptive exception that includes:
- The fixture ID that was requested
- The expected file path
- The specific error (file not found, JSON parse error, etc.)

```kotlin
class FixtureLoadException(
    val fixtureId: String,
    val path: String,
    message: String,
    cause: Throwable? = null
) : Exception("Failed to load fixture '$fixtureId' from '$path': $message", cause)
```

### Benchmark Failures

**Strategy**: When a benchmark operation throws an exception, the BenchmarkRunner should:
- Catch the exception
- Record it in the benchmark results
- Continue with remaining benchmarks
- Report all failures at the end

### Memory Monitoring Limitations

**Strategy**: Memory monitoring is platform-specific and may have limitations:
- On platforms without GC control, forceGC() may be a no-op
- Memory snapshots may have platform-specific precision
- Document platform-specific limitations in the MemoryMonitor implementation

### Validation Failures

**Strategy**: When output validation fails:
- Generate a unified diff showing the differences
- Truncate very large outputs to prevent overwhelming error messages
- Provide context (fixture ID, test name, platform)

## Testing Strategy

### Dual Testing Approach

The TCK itself requires comprehensive testing to ensure the testing infrastructure is reliable:

**Unit Tests**: Verify specific functionality of TCK components
- Fixture loading for known fixture IDs
- Validation logic for matching and non-matching strings
- Benchmark metric calculations
- Memory snapshot creation
- Report generation for sample data

**Property-Based Tests**: Verify universal properties across all inputs
- Fixture category filtering works for all categories
- Benchmark metrics are complete for any number of iterations
- Memory metrics are complete for any operation
- Validation is symmetric (if A != B, then B != A)
- JSON serialization round-trips preserve data

### Test Organization

Tests for the TCK infrastructure are organized by component:

```
tck-quality-testing/src/commonTest/kotlin/org/markup/poet/tck/
├── fixtures/
│   ├── FixtureLoaderTest.kt
│   └── TestFixtureTest.kt
├── validation/
│   ├── OutputValidatorTest.kt
│   └── ValidationResultTest.kt
├── benchmark/
│   ├── BenchmarkRunnerTest.kt
│   └── BenchmarkMetricsTest.kt
├── memory/
│   └── MemoryMonitorTest.kt
└── reporting/
    ├── ReportGeneratorTest.kt
    └── BenchmarkReportGeneratorTest.kt
```

### Property-Based Testing Configuration

- **Library**: Use kotlin-test for basic assertions; consider adding a property-based testing library like Kotest or kotlinx-benchmark for more advanced scenarios
- **Iterations**: Minimum 100 iterations per property test
- **Tagging**: Each property test references its design document property

Example property test:

```kotlin
@Test
fun `property 2 - fixture category filtering`() {
    // Feature: tck-quality-testing, Property 2: Fixture Category Filtering
    val loader = ResourceFixtureLoader()
    
    // Test for each category
    FixtureCategory.values().forEach { category ->
        val fixtures = loader.loadFixturesByCategory(category)
        
        // All returned fixtures must belong to the requested category
        fixtures.forEach { fixture ->
            assertEquals(
                category,
                fixture.category,
                "Fixture ${fixture.id} has wrong category"
            )
        }
    }
}
```

### Platform-Specific Testing

Platform-specific tests validate that expect/actual implementations work correctly:

```kotlin
// commonTest - Test the interface
@Test
fun `memory monitor can take snapshots`() {
    val monitor = PlatformMemoryMonitor()
    val snapshot = monitor.snapshot()
    
    assertNotNull(snapshot)
    assertTrue(snapshot.totalMemory > 0)
}

// jvmTest - Test JVM-specific behavior
@Test
fun `jvm memory monitor uses Runtime`() {
    val monitor = PlatformMemoryMonitor()
    val snapshot = monitor.snapshot()
    
    // JVM-specific assertion
    assertEquals(
        Runtime.getRuntime().totalMemory(),
        snapshot.totalMemory
    )
}
```

### Coverage Goals

- **TCK Infrastructure**: Aim for 90%+ coverage of the TCK infrastructure code itself
- **Test Fixtures**: Ensure fixtures exist for all major AsciiDoc constructs
- **Error Paths**: Test error handling paths in fixture loading, validation, and reporting

### Continuous Integration

The TCK tests run on every commit:

```bash
# Run all TCK infrastructure tests
./gradlew :tck-quality-testing:test

# Run on specific platform
./gradlew :tck-quality-testing:jvmTest
./gradlew :tck-quality-testing:iosX64Test
```

### Test Execution Time

- **Unit tests**: Should complete in < 10 seconds
- **Property tests**: Should complete in < 30 seconds (100 iterations each)
- **Full suite**: Should complete in < 1 minute

## Implementation Notes

### Fixture File Organization

Fixtures are organized in a directory structure that mirrors the category hierarchy:

```
fixtures/
├── blocks/
│   ├── paragraph-simple.json
│   ├── heading-levels.json
│   ├── list-unordered.json
│   └── ...
├── inline/
│   ├── bold-simple.json
│   ├── italic-nested.json
│   └── ...
├── malformed/
│   ├── unclosed-block.json
│   ├── invalid-attribute.json
│   └── ...
└── conformance/
    ├── spec-example-1.json
    ├── spec-example-2.json
    └── ...
```

### Fixture Naming Convention

Fixture IDs follow the pattern: `{category}-{description}`

Examples:
- `block-paragraph-simple`
- `inline-bold-nested`
- `malformed-unclosed-block`
- `conformance-spec-section-3-2`

### Incremental Test Enablement

Tests for unimplemented features use the `pending()` function:

```kotlin
@Test
fun `should parse complex tables`() {
    pending("Table parsing not yet implemented")
    
    // Test code will be enabled once feature is implemented
    val fixture = fixtureLoader.loadFixture("block-table-complex")
    // ... test logic
}
```

### Benchmark Baseline Management

Baseline benchmark results are stored in version control:

```
benchmarks/
├── baseline-jvm.json
├── baseline-android.json
├── baseline-ios.json
└── baseline-linux.json
```

CI compares current results against baselines and fails if regressions exceed threshold.

### Platform-Specific Memory Monitoring

Memory monitoring implementations vary by platform:

**JVM/Android**:
```kotlin
actual class PlatformMemoryMonitor : MemoryMonitor {
    override fun snapshot(): MemorySnapshot {
        val runtime = Runtime.getRuntime()
        return MemorySnapshot(
            timestamp = System.currentTimeMillis(),
            usedMemory = runtime.totalMemory() - runtime.freeMemory(),
            totalMemory = runtime.totalMemory(),
            freeMemory = runtime.freeMemory()
        )
    }
    
    override fun forceGC() {
        System.gc()
    }
}
```

**iOS/Linux**: Platform-specific implementations using native APIs.

## Future Enhancements

### Phase 1 (Current)
- Basic fixture infrastructure
- Simple validation utilities
- Basic benchmark runner
- Memory monitoring foundation

### Phase 2 (Future)
- Advanced diff generation with syntax highlighting
- Performance regression visualization
- Automated fixture generation from AsciiDoc spec
- Coverage integration with Kover or JaCoCo

### Phase 3 (Future)
- Fuzzing support for discovering edge cases
- Mutation testing for test quality validation
- Distributed benchmark execution
- Real-time performance dashboards
