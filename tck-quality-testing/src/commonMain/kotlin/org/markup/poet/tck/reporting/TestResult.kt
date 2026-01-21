package org.markup.poet.tck.reporting

import kotlin.time.Duration

/**
 * Test execution result.
 */
data class TestResult(
    val testName: String,
    val platform: String,
    val status: TestStatus,
    val duration: Duration,
    val errorMessage: String? = null,
    val stackTrace: String? = null
)

enum class TestStatus {
    PASSED,
    FAILED,
    SKIPPED,
    PENDING
}
