package org.markup.poet.tck.benchmark

import kotlin.time.Duration

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
