package org.markup.poet.tck.benchmark

/**
 * Comparison between current and baseline metrics.
 */
data class BenchmarkComparison(
    val current: BenchmarkMetrics,
    val baseline: BenchmarkMetrics,
    val meanDelta: Double, // percentage change
    val regressionDetected: Boolean
)
