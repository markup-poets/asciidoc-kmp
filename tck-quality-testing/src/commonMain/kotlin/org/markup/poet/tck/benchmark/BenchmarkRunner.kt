package org.markup.poet.tck.benchmark

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
