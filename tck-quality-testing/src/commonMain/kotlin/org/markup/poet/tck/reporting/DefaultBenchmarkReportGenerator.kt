package org.markup.poet.tck.reporting

import org.markup.poet.tck.benchmark.BenchmarkMetrics

/**
 * Default implementation of BenchmarkReportGenerator.
 */
class DefaultBenchmarkReportGenerator : BenchmarkReportGenerator {
    override fun generateJson(report: BenchmarkReport): String {
        val json = StringBuilder()
        json.append("{\n")
        json.append("  \"platform\": \"${report.platform}\",\n")
        json.append("  \"timestamp\": ${report.timestamp},\n")
        json.append("  \"benchmarks\": [\n")
        
        report.benchmarks.forEachIndexed { index, metrics ->
            json.append("    {\n")
            json.append("      \"operationName\": \"${metrics.operationName}\",\n")
            json.append("      \"iterations\": ${metrics.iterations},\n")
            json.append("      \"mean\": \"${metrics.mean}\",\n")
            json.append("      \"median\": \"${metrics.median}\",\n")
            json.append("      \"p95\": \"${metrics.p95}\",\n")
            json.append("      \"p99\": \"${metrics.p99}\",\n")
            json.append("      \"min\": \"${metrics.min}\",\n")
            json.append("      \"max\": \"${metrics.max}\",\n")
            json.append("      \"throughput\": ${metrics.throughput}\n")
            json.append("    }")
            
            if (index < report.benchmarks.size - 1) {
                json.append(",")
            }
            json.append("\n")
        }
        
        json.append("  ]\n")
        json.append("}")
        return json.toString()
    }
    
    override fun compareReports(current: BenchmarkReport, baseline: BenchmarkReport): String {
        val text = StringBuilder()
        text.append("Benchmark Comparison Report\n")
        text.append("===========================\n\n")
        text.append("Platform: ${current.platform}\n")
        text.append("Current Timestamp: ${current.timestamp}\n")
        text.append("Baseline Timestamp: ${baseline.timestamp}\n\n")
        
        val baselineMap = baseline.benchmarks.associateBy { it.operationName }
        val regressions = mutableListOf<Pair<String, Double>>()
        val improvements = mutableListOf<Pair<String, Double>>()
        
        for (currentMetrics in current.benchmarks) {
            val baselineMetrics = baselineMap[currentMetrics.operationName]
            if (baselineMetrics != null) {
                val percentageChange = calculatePercentageChange(
                    baselineMetrics.mean.inWholeNanoseconds,
                    currentMetrics.mean.inWholeNanoseconds
                )
                
                text.append("${currentMetrics.operationName}:\n")
                text.append("  Baseline: ${baselineMetrics.mean}\n")
                text.append("  Current:  ${currentMetrics.mean}\n")
                text.append("  Change:   ${formatPercentage(percentageChange)}\n")
                
                when {
                    percentageChange > 10.0 -> {
                        text.append("  Status:   REGRESSION\n")
                        regressions.add(currentMetrics.operationName to percentageChange)
                    }
                    percentageChange < -10.0 -> {
                        text.append("  Status:   IMPROVEMENT\n")
                        improvements.add(currentMetrics.operationName to percentageChange)
                    }
                    else -> {
                        text.append("  Status:   STABLE\n")
                    }
                }
                text.append("\n")
            }
        }
        
        if (regressions.isNotEmpty()) {
            text.append("REGRESSIONS DETECTED:\n")
            regressions.forEach { (name, change) ->
                text.append("  - $name: ${formatPercentage(change)}\n")
            }
            text.append("\n")
        }
        
        if (improvements.isNotEmpty()) {
            text.append("IMPROVEMENTS:\n")
            improvements.forEach { (name, change) ->
                text.append("  - $name: ${formatPercentage(change)}\n")
            }
        }
        
        return text.toString()
    }
    
    private fun calculatePercentageChange(baseline: Long, current: Long): Double {
        if (baseline == 0L) return 0.0
        return ((current - baseline).toDouble() / baseline.toDouble()) * 100.0
    }
    
    private fun formatPercentage(value: Double): String {
        val sign = if (value > 0) "+" else ""
        val rounded = (value * 100).toInt() / 100.0
        return "$sign$rounded%"
    }
}
