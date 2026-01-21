package org.markup.poet.tck.reporting

import org.markup.poet.tck.benchmark.BenchmarkMetrics
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class DefaultBenchmarkReportGeneratorTest {
    private val generator = DefaultBenchmarkReportGenerator()
    
    @Test
    fun `should generate JSON benchmark report`() {
        val report = BenchmarkReport(
            platform = "jvm",
            timestamp = 1704067200000,
            benchmarks = listOf(
                BenchmarkMetrics(
                    operationName = "parse_small",
                    iterations = 100,
                    mean = 5.milliseconds,
                    median = 5.milliseconds,
                    p95 = 6.milliseconds,
                    p99 = 7.milliseconds,
                    min = 4.milliseconds,
                    max = 8.milliseconds,
                    throughput = 200.0
                )
            )
        )
        
        val json = generator.generateJson(report)
        
        assertTrue(json.contains("\"platform\": \"jvm\""))
        assertTrue(json.contains("\"timestamp\": 1704067200000"))
        assertTrue(json.contains("\"operationName\": \"parse_small\""))
        assertTrue(json.contains("\"iterations\": 100"))
        assertTrue(json.contains("\"throughput\": 200.0"))
    }
    
    @Test
    fun `should generate JSON with multiple benchmarks`() {
        val report = BenchmarkReport(
            platform = "jvm",
            timestamp = 1704067200000,
            benchmarks = listOf(
                BenchmarkMetrics(
                    "op1", 100, 5.milliseconds, 5.milliseconds,
                    6.milliseconds, 7.milliseconds, 4.milliseconds, 8.milliseconds, 200.0
                ),
                BenchmarkMetrics(
                    "op2", 100, 10.milliseconds, 10.milliseconds,
                    12.milliseconds, 14.milliseconds, 8.milliseconds, 16.milliseconds, 100.0
                )
            )
        )
        
        val json = generator.generateJson(report)
        
        assertTrue(json.contains("\"operationName\": \"op1\""))
        assertTrue(json.contains("\"operationName\": \"op2\""))
    }
    
    @Test
    fun `should compare reports and detect no regression`() {
        val baseline = BenchmarkReport(
            platform = "jvm",
            timestamp = 1704067200000,
            benchmarks = listOf(
                BenchmarkMetrics(
                    "op1", 100, 100.milliseconds, 100.milliseconds,
                    110.milliseconds, 120.milliseconds, 90.milliseconds, 130.milliseconds, 10.0
                )
            )
        )
        
        val current = BenchmarkReport(
            platform = "jvm",
            timestamp = 1704067300000,
            benchmarks = listOf(
                BenchmarkMetrics(
                    "op1", 100, 105.milliseconds, 105.milliseconds,
                    115.milliseconds, 125.milliseconds, 95.milliseconds, 135.milliseconds, 9.5
                )
            )
        )
        
        val comparison = generator.compareReports(current, baseline)
        
        assertTrue(comparison.contains("Benchmark Comparison Report"))
        assertTrue(comparison.contains("op1:"))
        assertTrue(comparison.contains("Baseline:"))
        assertTrue(comparison.contains("Current:"))
        assertTrue(comparison.contains("STABLE"))
    }
    
    @Test
    fun `should detect regression`() {
        val baseline = BenchmarkReport(
            platform = "jvm",
            timestamp = 1704067200000,
            benchmarks = listOf(
                BenchmarkMetrics(
                    "op1", 100, 100.milliseconds, 100.milliseconds,
                    110.milliseconds, 120.milliseconds, 90.milliseconds, 130.milliseconds, 10.0
                )
            )
        )
        
        val current = BenchmarkReport(
            platform = "jvm",
            timestamp = 1704067300000,
            benchmarks = listOf(
                BenchmarkMetrics(
                    "op1", 100, 150.milliseconds, 150.milliseconds,
                    160.milliseconds, 170.milliseconds, 140.milliseconds, 180.milliseconds, 6.7
                )
            )
        )
        
        val comparison = generator.compareReports(current, baseline)
        
        assertTrue(comparison.contains("REGRESSION"))
        assertTrue(comparison.contains("REGRESSIONS DETECTED:"))
    }
    
    @Test
    fun `should detect improvement`() {
        val baseline = BenchmarkReport(
            platform = "jvm",
            timestamp = 1704067200000,
            benchmarks = listOf(
                BenchmarkMetrics(
                    "op1", 100, 100.milliseconds, 100.milliseconds,
                    110.milliseconds, 120.milliseconds, 90.milliseconds, 130.milliseconds, 10.0
                )
            )
        )
        
        val current = BenchmarkReport(
            platform = "jvm",
            timestamp = 1704067300000,
            benchmarks = listOf(
                BenchmarkMetrics(
                    "op1", 100, 50.milliseconds, 50.milliseconds,
                    55.milliseconds, 60.milliseconds, 45.milliseconds, 65.milliseconds, 20.0
                )
            )
        )
        
        val comparison = generator.compareReports(current, baseline)
        
        assertTrue(comparison.contains("IMPROVEMENT"))
        assertTrue(comparison.contains("IMPROVEMENTS:"))
    }
}
