package org.markup.poet.tck.reporting

/**
 * Generates benchmark reports.
 */
interface BenchmarkReportGenerator {
    /**
     * Generate JSON benchmark report.
     */
    fun generateJson(report: BenchmarkReport): String
    
    /**
     * Compare benchmark reports and detect regressions.
     */
    fun compareReports(current: BenchmarkReport, baseline: BenchmarkReport): String
}
