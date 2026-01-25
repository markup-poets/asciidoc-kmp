package org.markup.poet.tck.execution

import kotlinx.serialization.Serializable
import org.markup.poet.tck.fixtures.FixtureCategory

/**
 * Result of executing a single test.
 * 
 * Contains all information about the test execution including:
 * - Test identification
 * - Execution status
 * - Platform information
 * - Timing data
 * - Error details (if failed)
 * - Output comparison (if applicable)
 */
@Serializable
data class TestExecutionResult(
    /**
     * Unique identifier of the test fixture.
     */
    val fixtureId: String,
    
    /**
     * Execution status (PASSED, FAILED, SKIPPED, PENDING, ERROR).
     */
    val status: TestStatus,
    
    /**
     * Platform where the test was executed.
     * Examples: "JVM", "iOS", "Linux", "Android"
     */
    val platform: String,
    
    /**
     * Test execution duration in milliseconds.
     */
    val durationMs: Long,
    
    /**
     * Test category.
     */
    val category: FixtureCategory? = null,
    
    /**
     * Test source (custom or official-tck).
     */
    val source: String? = null,
    
    /**
     * Error message if test failed or errored.
     */
    val errorMessage: String? = null,
    
    /**
     * Stack trace if test errored.
     */
    val stackTrace: String? = null,
    
    /**
     * Actual output produced by the test.
     */
    val actualOutput: String? = null,
    
    /**
     * Expected output for comparison.
     */
    val expectedOutput: String? = null,
    
    /**
     * Diff between expected and actual output.
     */
    val diff: String? = null,
    
    /**
     * Additional metadata about the test execution.
     */
    val metadata: Map<String, String> = emptyMap()
) {
    /**
     * Check if the test passed.
     */
    fun isPassed(): Boolean = status == TestStatus.PASSED
    
    /**
     * Check if the test failed.
     */
    fun isFailed(): Boolean = status == TestStatus.FAILED
    
    /**
     * Check if the test was skipped.
     */
    fun isSkipped(): Boolean = status == TestStatus.SKIPPED
    
    /**
     * Check if the test is pending implementation.
     */
    fun isPending(): Boolean = status == TestStatus.PENDING
    
    /**
     * Check if the test encountered an error.
     */
    fun isError(): Boolean = status == TestStatus.ERROR
    
    /**
     * Get a human-readable summary of the result.
     */
    fun summary(): String {
        return buildString {
            append("[$status] $fixtureId")
            if (category != null) append(" ($category)")
            append(" on $platform")
            append(" (${durationMs}ms)")
            if (errorMessage != null) {
                append("\n  Error: $errorMessage")
            }
        }
    }
}

/**
 * Status of a test execution.
 */
@Serializable
enum class TestStatus {
    /**
     * Test passed successfully.
     */
    PASSED,
    
    /**
     * Test failed (output didn't match expected).
     */
    FAILED,
    
    /**
     * Test was skipped (e.g., platform-specific test on wrong platform).
     */
    SKIPPED,
    
    /**
     * Test is pending implementation (feature not yet implemented).
     */
    PENDING,
    
    /**
     * Test encountered an error during execution (e.g., exception thrown).
     */
    ERROR
}

/**
 * Aggregated results from multiple test executions.
 * 
 * Provides summary statistics and breakdowns by:
 * - Platform
 * - Category
 * - Source (custom vs official)
 */
@Serializable
data class AggregatedResults(
    /**
     * Total number of tests executed.
     */
    val totalTests: Int,
    
    /**
     * Number of tests that passed.
     */
    val passed: Int,
    
    /**
     * Number of tests that failed.
     */
    val failed: Int,
    
    /**
     * Number of tests that were skipped.
     */
    val skipped: Int,
    
    /**
     * Number of tests that are pending.
     */
    val pending: Int,
    
    /**
     * Number of tests that encountered errors.
     */
    val errors: Int,
    
    /**
     * Results broken down by platform.
     */
    val byPlatform: Map<String, PlatformResults>,
    
    /**
     * Results broken down by category.
     */
    val byCategory: Map<FixtureCategory, CategoryResults>,
    
    /**
     * Results broken down by source.
     */
    val bySource: Map<String, SourceResults>,
    
    /**
     * List of all failed tests.
     */
    val failedTests: List<TestExecutionResult>,
    
    /**
     * List of all pending tests.
     */
    val pendingTests: List<TestExecutionResult>
) {
    /**
     * Calculate overall pass rate.
     */
    fun passRate(): Double {
        return if (totalTests > 0) {
            passed.toDouble() / totalTests
        } else {
            0.0
        }
    }
    
    /**
     * Get a human-readable summary.
     */
    fun summary(): String {
        return buildString {
            appendLine("Test Results Summary:")
            appendLine("  Total: $totalTests")
            appendLine("  Passed: $passed (${(passRate() * 100).toLong() / 1.0}%)")
            appendLine("  Failed: $failed")
            appendLine("  Pending: $pending")
            appendLine("  Skipped: $skipped")
            appendLine("  Errors: $errors")
            
            if (byPlatform.isNotEmpty()) {
                appendLine("\nBy Platform:")
                byPlatform.forEach { (platform, results) ->
                    appendLine("  $platform: ${results.passed}/${results.total} (${(results.passRate * 100).toLong() / 1.0}%)")
                }
            }
            
            if (byCategory.isNotEmpty()) {
                appendLine("\nBy Category:")
                byCategory.entries.sortedByDescending { it.value.total }.take(5).forEach { (category, results) ->
                    appendLine("  $category: ${results.passed}/${results.total} (${(results.passRate * 100).toLong() / 1.0}%)")
                }
            }
        }
    }
}

/**
 * Results for a specific platform.
 */
@Serializable
data class PlatformResults(
    val platform: String,
    val total: Int,
    val passed: Int,
    val failed: Int,
    val passRate: Double
)

/**
 * Results for a specific category.
 */
@Serializable
data class CategoryResults(
    val category: FixtureCategory,
    val total: Int,
    val passed: Int,
    val failed: Int,
    val passRate: Double
)

/**
 * Results for a specific source (custom or official).
 */
@Serializable
data class SourceResults(
    val source: String,
    val total: Int,
    val passed: Int,
    val failed: Int,
    val passRate: Double
)
