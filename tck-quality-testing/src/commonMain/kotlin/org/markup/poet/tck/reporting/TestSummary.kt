package org.markup.poet.tck.reporting

import kotlin.time.Duration

/**
 * Test suite execution summary.
 */
data class TestSummary(
    val totalTests: Int,
    val passed: Int,
    val failed: Int,
    val skipped: Int,
    val pending: Int,
    val duration: Duration,
    val results: List<TestResult>
)
