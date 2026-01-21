package org.markup.poet.tck.reporting

import org.markup.poet.tck.benchmark.BenchmarkMetrics

/**
 * Benchmark report.
 */
data class BenchmarkReport(
    val platform: String,
    val timestamp: Long,
    val benchmarks: List<BenchmarkMetrics>
)
