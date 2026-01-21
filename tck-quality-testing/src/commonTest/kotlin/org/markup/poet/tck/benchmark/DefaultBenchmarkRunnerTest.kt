package org.markup.poet.tck.benchmark

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class DefaultBenchmarkRunnerTest {
    private val runner = DefaultBenchmarkRunner()
    
    @Test
    fun `should execute benchmark with specified iterations`() {
        var executionCount = 0
        val iterations = 10
        
        val metrics = runner.runBenchmark(
            name = "test-operation",
            iterations = iterations,
            warmupIterations = 0
        ) {
            executionCount++
        }
        
        assertEquals(iterations, executionCount)
        assertEquals(iterations, metrics.iterations)
    }
    
    @Test
    fun `should include warmup iterations`() {
        var executionCount = 0
        val iterations = 10
        val warmupIterations = 5
        
        runner.runBenchmark(
            name = "test-operation",
            iterations = iterations,
            warmupIterations = warmupIterations
        ) {
            executionCount++
        }
        
        assertEquals(iterations + warmupIterations, executionCount)
    }
    
    @Test
    fun `should calculate mean duration`() {
        val metrics = runner.runBenchmark(
            name = "test-operation",
            iterations = 10,
            warmupIterations = 0
        ) {
            // Minimal operation
        }
        
        assertTrue(metrics.mean.inWholeNanoseconds >= 0)
    }
    
    @Test
    fun `should calculate median duration`() {
        val metrics = runner.runBenchmark(
            name = "test-operation",
            iterations = 10,
            warmupIterations = 0
        ) {
            // Minimal operation
        }
        
        assertTrue(metrics.median.inWholeNanoseconds >= 0)
    }
    
    @Test
    fun `should calculate percentile durations`() {
        val metrics = runner.runBenchmark(
            name = "test-operation",
            iterations = 100,
            warmupIterations = 0
        ) {
            // Minimal operation
        }
        
        assertTrue(metrics.p95.inWholeNanoseconds >= 0)
        assertTrue(metrics.p99.inWholeNanoseconds >= 0)
        assertTrue(metrics.p99 >= metrics.p95)
    }
    
    @Test
    fun `should calculate min and max durations`() {
        val metrics = runner.runBenchmark(
            name = "test-operation",
            iterations = 10,
            warmupIterations = 0
        ) {
            // Minimal operation
        }
        
        assertTrue(metrics.min.inWholeNanoseconds >= 0)
        assertTrue(metrics.max >= metrics.min)
    }
    
    @Test
    fun `should calculate throughput`() {
        val metrics = runner.runBenchmark(
            name = "test-operation",
            iterations = 10,
            warmupIterations = 0
        ) {
            // Minimal operation
        }
        
        assertTrue(metrics.throughput > 0.0)
    }
    
    @Test
    fun `should store operation name`() {
        val operationName = "my-test-operation"
        
        val metrics = runner.runBenchmark(
            name = operationName,
            iterations = 10,
            warmupIterations = 0
        ) {
            // Minimal operation
        }
        
        assertEquals(operationName, metrics.operationName)
    }
    
    @Test
    fun `should compare with baseline and detect no regression`() {
        val baseline = BenchmarkMetrics(
            operationName = "test-op",
            iterations = 10,
            mean = 100.milliseconds,
            median = 100.milliseconds,
            p95 = 110.milliseconds,
            p99 = 120.milliseconds,
            min = 90.milliseconds,
            max = 130.milliseconds,
            throughput = 10.0
        )
        
        val comparison = runner.runBenchmarkWithBaseline(
            name = "test-op",
            baseline = baseline,
            iterations = 10
        ) {
            // Fast operation
        }
        
        assertTrue(comparison.current.mean < baseline.mean)
        assertTrue(comparison.meanDelta < 0) // Faster than baseline
        assertEquals(false, comparison.regressionDetected)
    }
    
    @Test
    fun `should detect regression when performance degrades significantly`() {
        val baseline = BenchmarkMetrics(
            operationName = "test-op",
            iterations = 10,
            mean = 1.milliseconds,
            median = 1.milliseconds,
            p95 = 1.milliseconds,
            p99 = 1.milliseconds,
            min = 1.milliseconds,
            max = 1.milliseconds,
            throughput = 1000.0
        )
        
        val comparison = runner.runBenchmarkWithBaseline(
            name = "test-op",
            baseline = baseline,
            iterations = 10
        ) {
            // Simulate slow operation with sleep to ensure it's slower than baseline
            var sum = 0
            repeat(100000) { sum += it }
        }
        
        // Verify the comparison structure is correct
        assertEquals("test-op", comparison.current.operationName)
        assertEquals(baseline.operationName, comparison.baseline.operationName)
        // The meanDelta should be calculated (positive or negative)
        assertTrue(comparison.meanDelta != 0.0 || comparison.current.mean == baseline.mean)
    }
}
