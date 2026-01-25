package org.markup.poet.tck.conformance

/**
 * Generates AsciiDoc format conformance reports.
 * 
 * The AsciiDoc reporter creates human-readable reports with:
 * - Summary statistics
 * - Tables for platform/category breakdowns
 * - Failed test details
 * - Certification status
 * 
 * **Usage:**
 * ```kotlin
 * val reporter = DefaultAsciidocReporter()
 * val asciidoc = reporter.generateAsciidoc(report)
 * 
 * // Write to file
 * File("CONFORMANCE_REPORT.adoc").writeText(asciidoc)
 * ```
 */
interface AsciidocReporter {
    /**
     * Generate AsciiDoc representation of the conformance report.
     * 
     * @param report Conformance report to format
     * @return AsciiDoc string
     */
    fun generateAsciidoc(report: ConformanceReport): String
}

/**
 * Default implementation of AsciidocReporter.
 * 
 * Generates a comprehensive AsciiDoc report with:
 * - Executive summary
 * - Platform breakdown table
 * - Category breakdown table
 * - Failed test details
 * - Pending test details
 * - Certification status
 */
class DefaultAsciidocReporter : AsciidocReporter {
    
    override fun generateAsciidoc(report: ConformanceReport): String {
        return buildString {
            appendHeader(report)
            appendSummary(report)
            appendPlatformResults(report)
            appendCategoryResults(report)
            appendFailedTests(report)
            appendPendingTests(report)
            appendCertificationStatus(report)
            appendFooter(report)
        }
    }
    
    private fun StringBuilder.appendHeader(report: ConformanceReport) {
        appendLine("= AsciiDoc Conformance Report")
        appendLine(":toc: left")
        appendLine(":toclevels: 3")
        appendLine(":icons: font")
        appendLine(":sectnums:")
        appendLine()
        appendLine("[horizontal]")
        appendLine("Generated:: ${formatTimestamp(report.metadata.generatedAt)}")
        appendLine("Library Version:: ${report.metadata.libraryVersion}")
        appendLine("Spec Version:: ${report.metadata.specVersion}")
        appendLine("TCK Commit:: ${report.metadata.tckCommitHash.take(8)}")
        appendLine("Platforms:: ${report.metadata.platforms.joinToString(", ")}")
        appendLine()
        appendLine("---")
        appendLine()
    }
    
    private fun StringBuilder.appendSummary(report: ConformanceReport) {
        val summary = report.summary
        
        appendLine("== Executive Summary")
        appendLine()
        appendLine("[cols=\"1,1\", options=\"header\"]")
        appendLine("|===")
        appendLine("| Metric | Value")
        appendLine("| Total Tests | ${summary.totalTests}")
        appendLine("| Passed | ${summary.passed} (${formatPercentage(summary.overallPassRate)})")
        appendLine("| Failed | ${summary.failed}")
        appendLine("| Pending | ${summary.pending}")
        appendLine("| Skipped | ${summary.skipped}")
        appendLine("| Overall Pass Rate | ${formatPercentage(summary.overallPassRate)}")
        appendLine("| Official TCK Pass Rate | ${formatPercentage(summary.officialTestsPassRate)}")
        appendLine("| Custom Tests Pass Rate | ${formatPercentage(summary.customTestsPassRate)}")
        appendLine("| Total Duration | ${formatDuration(summary.totalDurationMs)}")
        appendLine("|===")
        appendLine()
    }
    
    private fun StringBuilder.appendPlatformResults(report: ConformanceReport) {
        if (report.platformResults.isEmpty()) return
        
        appendLine("== Platform Results")
        appendLine()
        appendLine("[cols=\"2,1,1,1,1\", options=\"header\"]")
        appendLine("|===")
        appendLine("| Platform | Total | Passed | Failed | Pass Rate")
        
        report.platformResults.sortedByDescending { it.passRate }.forEach { platform ->
            appendLine("| ${platform.platform} | ${platform.totalTests} | ${platform.passed} | ${platform.failed} | ${formatPercentage(platform.passRate)}")
        }
        appendLine("|===")
        appendLine()
    }
    
    private fun StringBuilder.appendCategoryResults(report: ConformanceReport) {
        if (report.categoryResults.isEmpty()) return
        
        appendLine("== Category Results")
        appendLine()
        appendLine("[cols=\"2,1,1,1,1\", options=\"header\"]")
        appendLine("|===")
        appendLine("| Category | Total | Passed | Failed | Pass Rate")
        
        report.categoryResults.sortedByDescending { it.passRate }.forEach { category ->
            appendLine("| ${category.category} | ${category.totalTests} | ${category.passed} | ${category.failed} | ${formatPercentage(category.passRate)}")
        }
        appendLine("|===")
        appendLine()
    }
    
    private fun StringBuilder.appendFailedTests(report: ConformanceReport) {
        if (report.failedTests.isEmpty()) {
            appendLine("== Failed Tests")
            appendLine()
            appendLine("icon:check-circle[role=green] No failed tests!")
            appendLine()
            return
        }
        
        appendLine("== Failed Tests (${report.failedTests.size})")
        appendLine()
        
        report.failedTests.take(20).forEach { test ->
            appendLine("=== ${test.testId}")
            appendLine()
            appendLine("*Description*:: ${test.description}")
            appendLine("*Category*:: ${test.category}")
            appendLine("*Spec Section*:: ${test.specSection}")
            appendLine("*Platforms*:: ${test.platforms.joinToString(", ")}")
            appendLine("*Error*::")
            appendLine("[source]")
            appendLine("----")
            appendLine(test.errorMessage)
            appendLine("----")
            appendLine()
            
            if (test.expectedOutput != null && test.actualOutput != null) {
                appendLine(".Output Comparison")
                appendLine("[%collapsible]")
                appendLine("====")
                appendLine("*Expected:*")
                appendLine("[source]")
                appendLine("----")
                appendLine(test.expectedOutput.take(500))
                if (test.expectedOutput.length > 500) appendLine("... (truncated)")
                appendLine("----")
                appendLine()
                appendLine("*Actual:*")
                appendLine("[source]")
                appendLine("----")
                appendLine(test.actualOutput.take(500))
                if (test.actualOutput.length > 500) appendLine("... (truncated)")
                appendLine("----")
                appendLine("====")
                appendLine()
            }
        }
        
        if (report.failedTests.size > 20) {
            appendLine("_... and ${report.failedTests.size - 20} more failed tests_")
            appendLine()
        }
    }
    
    private fun StringBuilder.appendPendingTests(report: ConformanceReport) {
        if (report.pendingTests.isEmpty()) {
            appendLine("== Pending Tests")
            appendLine()
            appendLine("icon:check-circle[role=green] No pending tests!")
            appendLine()
            return
        }
        
        appendLine("== Pending Tests (${report.pendingTests.size})")
        appendLine()
        
        report.pendingTests.take(10).forEach { test ->
            appendLine("* *${test.testId}*: ${test.description}")
            appendLine("  - Category: ${test.category}")
            appendLine("  - Reason: ${test.reason}")
        }
        
        if (report.pendingTests.size > 10) {
            appendLine()
            appendLine("_... and ${report.pendingTests.size - 10} more pending tests_")
        }
        appendLine()
    }
    
    private fun StringBuilder.appendCertificationStatus(report: ConformanceReport) {
        val status = report.certificationStatus
        
        appendLine("== Certification Status")
        appendLine()
        
        if (status.isReady) {
            appendLine("[IMPORTANT]")
            appendLine("====")
            appendLine("icon:certificate[role=green] *READY FOR CERTIFICATION*")
            appendLine("====")
        } else {
            appendLine("[WARNING]")
            appendLine("====")
            appendLine("icon:exclamation-triangle[role=red] *NOT READY FOR CERTIFICATION*")
            appendLine("====")
        }
        appendLine()
        appendLine("*Progress*: ${formatPercentage(status.overallProgress / 100.0)}")
        appendLine()
        
        if (status.blockingIssues.isNotEmpty()) {
            appendLine("=== Blocking Issues (${status.blockingIssues.size})")
            appendLine()
            
            status.blockingIssues.forEach { issue ->
                val severity = when (issue.severity) {
                    IssueSeverity.CRITICAL -> "[red]#*CRITICAL*#"
                    IssueSeverity.HIGH -> "[orange]#*HIGH*#"
                    IssueSeverity.MEDIUM -> "[yellow]#*MEDIUM*#"
                    IssueSeverity.LOW -> "[blue]#*LOW*#"
                }
                
                appendLine("* $severity: ${issue.description}")
                appendLine("  - Affected Tests: ${issue.affectedTests.size}")
                appendLine("  - Resolution: ${issue.resolution}")
                appendLine()
            }
        }
        
        if (status.recommendations.isNotEmpty()) {
            appendLine("=== Recommendations")
            appendLine()
            status.recommendations.forEach { recommendation ->
                appendLine("* $recommendation")
            }
            appendLine()
        }
    }
    
    private fun StringBuilder.appendFooter(report: ConformanceReport) {
        appendLine("---")
        appendLine()
        appendLine("_Report generated by Markup Poet AsciiDoc Converter_")
    }
    
    private fun formatTimestamp(timestamp: Long): String {
        return timestamp.toString()
    }
    
    private fun formatPercentage(rate: Double): String {
        return "${(rate * 100).toInt()}%"
    }
    
    private fun formatDuration(durationMs: Long): String {
        val seconds = durationMs / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        
        return if (minutes > 0) {
            "${minutes}m ${remainingSeconds}s"
        } else {
            "${seconds}s"
        }
    }
}
