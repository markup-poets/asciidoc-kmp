package org.markup.poet.tck.reporting

/**
 * Generates test reports in various formats.
 */
interface ReportGenerator {
    /**
     * Generate JUnit XML report.
     */
    fun generateJUnitXml(summary: TestSummary): String
    
    /**
     * Generate JSON report.
     */
    fun generateJson(summary: TestSummary): String
    
    /**
     * Generate human-readable text report.
     */
    fun generateText(summary: TestSummary): String
}
