package org.markup.poet.tck.execution

import org.markup.poet.tck.currentTimeMillis
import org.markup.poet.tck.getPlatformName
import org.markup.poet.tck.fixtures.TestFixture
import org.markup.poet.tck.fixtures.FixtureCategory

/**
 * Executes test fixtures and collects results.
 * 
 * The TestRunner is responsible for:
 * - Executing individual test fixtures
 * - Measuring execution time
 * - Capturing output and errors
 * - Comparing actual vs expected output
 * - Handling test failures gracefully
 * 
 * **Usage:**
 * ```kotlin
 * val runner = DefaultTestRunner(
 *     parser = { input -> parseAsciiDoc(input) },
 *     renderer = { ast -> renderToHtml(ast) },
 *     validator = DefaultOutputValidator()
 * )
 * 
 * val result = runner.runTest(fixture)
 * ```
 */
interface TestRunner {
    /**
     * Run a single test fixture.
     * 
     * @param fixture The test fixture to execute
     * @return Test execution result
     */
    fun runTest(fixture: TestFixture): TestExecutionResult
    
    /**
     * Run multiple test fixtures.
     * 
     * @param fixtures List of fixtures to execute
     * @return List of execution results
     */
    fun runTests(fixtures: List<TestFixture>): List<TestExecutionResult>
    
    /**
     * Run tests with filtering.
     * 
     * @param fixtures List of fixtures to execute
     * @param filter Filter to apply
     * @return List of execution results for filtered tests
     */
    fun runTestsFiltered(
        fixtures: List<TestFixture>,
        filter: TestFilter
    ): List<TestExecutionResult>
}

/**
 * Default implementation of TestRunner.
 * 
 * This implementation:
 * - Parses AsciiDoc input
 * - Renders to output format
 * - Validates output against expected
 * - Handles errors gracefully
 * - Measures execution time
 * 
 * **Note**: Parser and renderer are provided as lambdas to allow
 * flexibility in testing different implementations.
 */
class DefaultTestRunner(
    private val parser: (String) -> Any,
    private val renderer: (Any) -> String,
    private val validator: OutputValidator
) : TestRunner {
    
    override fun runTest(fixture: TestFixture): TestExecutionResult {
        val startTime = currentTimeMillis()
        
        return try {
            // Parse the input
            val parsed = parser(fixture.input)
            
            // Render to output
            val rendered = renderer(parsed)
            
            // Validate output if expected output is provided
            val validationResult = if (fixture.expectedOutput != null) {
                validator.validate(fixture.expectedOutput, rendered)
            } else {
                ValidationResult.Success
            }
            
            val duration = currentTimeMillis() - startTime
            
            when (validationResult) {
                is ValidationResult.Success -> TestExecutionResult(
                    fixtureId = fixture.id,
                    status = TestStatus.PASSED,
                    platform = getPlatformName(),
                    durationMs = duration,
                    category = fixture.category,
                    source = fixture.metadata["source"],
                    actualOutput = rendered,
                    expectedOutput = fixture.expectedOutput
                )
                is ValidationResult.Failure -> TestExecutionResult(
                    fixtureId = fixture.id,
                    status = TestStatus.FAILED,
                    platform = getPlatformName(),
                    durationMs = duration,
                    category = fixture.category,
                    source = fixture.metadata["source"],
                    errorMessage = validationResult.message,
                    actualOutput = rendered,
                    expectedOutput = fixture.expectedOutput,
                    diff = validationResult.diff
                )
            }
        } catch (e: PendingTestException) {
            // Test is pending implementation
            val duration = currentTimeMillis() - startTime
            TestExecutionResult(
                fixtureId = fixture.id,
                status = TestStatus.PENDING,
                platform = getPlatformName(),
                durationMs = duration,
                category = fixture.category,
                source = fixture.metadata["source"],
                errorMessage = e.message
            )
        } catch (e: Exception) {
            // Test encountered an error
            val duration = currentTimeMillis() - startTime
            TestExecutionResult(
                fixtureId = fixture.id,
                status = TestStatus.ERROR,
                platform = getPlatformName(),
                durationMs = duration,
                category = fixture.category,
                source = fixture.metadata["source"],
                errorMessage = e.message,
                stackTrace = e.stackTraceToString()
            )
        }
    }
    
    override fun runTests(fixtures: List<TestFixture>): List<TestExecutionResult> {
        val results = mutableListOf<TestExecutionResult>()
        val total = fixtures.size
        
        fixtures.forEachIndexed { index, fixture ->
            // Progress logging
            if (index % 5 == 0 || index == total - 1) {
                println("Progress: ${index + 1}/$total tests (${(index + 1) * 100 / total}%)")
            }
            
            try {
                val result = runTest(fixture)
                results.add(result)
                
                // Log slow tests
                if (result.durationMs > 1000) {
                    println("⚠️  Slow test: ${fixture.id} took ${result.durationMs}ms")
                }
            } catch (e: Exception) {
                println("❌ Error running test ${fixture.id}: ${e.message}")
                // Add error result
                results.add(TestExecutionResult(
                    fixtureId = fixture.id,
                    status = TestStatus.ERROR,
                    platform = getPlatformName(),
                    durationMs = 0,
                    category = fixture.category,
                    source = fixture.metadata["source"],
                    errorMessage = "Test execution failed: ${e.message}",
                    stackTrace = e.stackTraceToString()
                ))
            }
        }
        
        return results
    }
    
    override fun runTestsFiltered(
        fixtures: List<TestFixture>,
        filter: TestFilter
    ): List<TestExecutionResult> {
        val filtered = fixtures.filter { filter.shouldRun(it) }
        return runTests(filtered)
    }
}

/**
 * Validates test output against expected output.
 */
interface OutputValidator {
    /**
     * Validate actual output against expected output.
     * 
     * @param expected Expected output
     * @param actual Actual output
     * @return Validation result
     */
    fun validate(expected: String, actual: String): ValidationResult
}

/**
 * Result of output validation.
 */
sealed class ValidationResult {
    /**
     * Validation succeeded - output matches expected.
     */
    object Success : ValidationResult()
    
    /**
     * Validation failed - output doesn't match expected.
     * 
     * @param message Description of the mismatch
     * @param diff Optional diff between expected and actual
     */
    data class Failure(
        val message: String,
        val diff: String? = null
    ) : ValidationResult()
}

/**
 * Default output validator that performs exact string comparison.
 */
class DefaultOutputValidator : OutputValidator {
    override fun validate(expected: String, actual: String): ValidationResult {
        return if (expected.trim() == actual.trim()) {
            ValidationResult.Success
        } else {
            ValidationResult.Failure(
                message = "Output mismatch",
                diff = generateDiff(expected, actual)
            )
        }
    }
    
    private fun generateDiff(expected: String, actual: String): String {
        return buildString {
            appendLine("Expected:")
            appendLine(expected.take(200))
            if (expected.length > 200) appendLine("... (truncated)")
            appendLine("\nActual:")
            appendLine(actual.take(200))
            if (actual.length > 200) appendLine("... (truncated)")
        }
    }
}

/**
 * Exception thrown when a test is pending implementation.
 */
class PendingTestException(message: String) : Exception(message)
