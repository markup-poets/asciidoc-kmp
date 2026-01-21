package org.markup.poet.tck.benchmark

import kotlin.time.Duration
import kotlin.time.measureTime

/**
 * Default implementation of BenchmarkRunner using kotlin.time.
 */
class DefaultBenchmarkRunner : BenchmarkRunner {
    override fun runBenchmark(
        name: String,
        iterations: Int,
        warmupIterations: Int,
        operation: () -> Unit
    ): BenchmarkMetrics {
        // Warmup phase
        repeat(warmupIterations) { operation() }
        
        // Measurement phase
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
        val meanDelta = calculatePercentageChange(baseline.mean, current.mean)
        val regressionDetected = meanDelta > 10.0 // 10% threshold
        
        return BenchmarkComparison(current, baseline, meanDelta, regressionDetected)
    }
    
    private fun calculateMetrics(name: String, durations: List<Duration>): BenchmarkMetrics {
        val sorted = durations.sorted()
        val mean = durations.fold(Duration.ZERO) { acc, d -> acc + d } / durations.size
        val median = sorted[sorted.size / 2]
        val p95 = sorted[((sorted.size - 1) * 0.95).toInt()]
        val p99 = sorted[((sorted.size - 1) * 0.99).toInt()]
        val throughput = if (mean.inWholeMilliseconds > 0) {
            1000.0 / mean.inWholeMilliseconds.toDouble()
        } else {
            Double.MAX_VALUE
        }
        
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
    
    private fun calculatePercentageChange(baseline: Duration, current: Duration): Double {
        if (baseline.inWholeNanoseconds == 0L) return 0.0
        return ((current.inWholeNanoseconds - baseline.inWholeNanoseconds).toDouble() / 
                baseline.inWholeNanoseconds.toDouble()) * 100.0
    }
}
