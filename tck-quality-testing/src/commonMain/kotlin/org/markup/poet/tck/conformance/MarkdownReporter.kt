package org.markup.poet.tck.conformance

/**
 * Generates Markdown format conformance reports.
 * 
 * The Markdown reporter creates human-readable reports with:
 * - Summary statistics
 * - Tables for platform/category breakdowns
 * - Failed test details
 * - Certification status
 * 
 * **Usage:**
 * ```kotlin
 * val reporter = DefaultMarkdownReporter()
 * val markdown = reporter.generateMarkdown(report)
 * 
 * // Write to file
 * File("CONFORMANCE_REPORT.md").writeText(markdown)
 * ```
 */
interface MarkdownReporter {
    /**
     * Generate Markdown representation of the conformance report.
     * 
     * @param report Conformance report to format
     * @return Markdown string
     */
    fun generateMarkdown(report: ConformanceReport): String
}

/**
 * Default implementation of MarkdownReporter.
 * 
 * Generates a comprehensive Markdown report with:
 * - Executive summary
 * - Platform breakdown table
 * - Category breakdown table
 * - Failed test details
 * - Pending test details
 * - Certification status
 */
class DefaultMarkdownReporter : MarkdownReporter {
    
    override fun generateMarkdown(report: ConformanceReport): String {
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
        appendLine("# AsciiDoc Conformance Report")
        appendLine()
        appendLine("**Generated**: ${formatTimestamp(report.metadata.generatedAt)}")
        appendLine("**Library Version**: ${report.metadata.libraryVersion}")
        appendLine("**Spec Version**: ${report.metadata.specVersion}")
        appendLine("**TCK Commit**: ${report.metadata.tckCommitHash.take(8)}")
        appendLine("**Platforms**: ${report.metadata.platforms.joinToString(", ")}")
        appendLine()
        appendLine("---")
        appendLine()
    }
    
    private fun StringBuilder.appendSummary(report: ConformanceReport) {
        val summary = report.summary
        
        appendLine("## Executive Summary")
        appendLine()
        appendLine("| Metric | Value |")
        appendLine("|--------|-------|")
        appendLine("| Total Tests | ${summary.totalTests} |")
        appendLine("| Passed | ${summary.passed} (${formatPercentage(summary.overallPassRate)}) |")
        appendLine("| Failed | ${summary.failed} |")
        appendLine("| Pending | ${summary.pending} |")
        appendLine("| Skipped | ${summary.skipped} |")
        appendLine("| Overall Pass Rate | ${formatPercentage(summary.overallPassRate)} |")
        appendLine("| Official TCK Pass Rate | ${formatPercentage(summary.officialTestsPassRate)} |")
        appendLine("| Custom Tests Pass Rate | ${formatPercentage(summary.customTestsPassRate)} |")
        appendLine("| Total Duration | ${formatDuration(summary.totalDurationMs)} |")
        appendLine()
    }
    
    private fun StringBuilder.appendPlatformResults(report: ConformanceReport) {
        if (report.platformResults.isEmpty()) return
        
        appendLine("## Platform Results")
        appendLine()
        appendLine("| Platform | Total | Passed | Failed | Pass Rate |")
        appendLine("|----------|-------|--------|--------|-----------|")
        
        report.platformResults.sortedByDescending { it.passRate }.forEach { platform ->
            appendLine("| ${platform.platform} | ${platform.totalTests} | ${platform.passed} | ${platform.failed} | ${formatPercentage(platform.passRate)} |")
        }
        appendLine()
    }
    
    private fun StringBuilder.appendCategoryResults(report: ConformanceReport) {
        if (report.categoryResults.isEmpty()) return
        
        appendLine("## Category Results")
        appendLine()
        appendLine("| Category | Total | Passed | Failed | Pass Rate |")
        appendLine("|----------|-------|--------|--------|-----------|")
        
        report.categoryResults.sortedByDescending { it.passRate }.forEach { category ->
            appendLine("| ${category.category} | ${category.totalTests} | ${category.passed} | ${category.failed} | ${formatPercentage(category.passRate)} |")
        }
        appendLine()
    }
    
    private fun StringBuilder.appendFailedTests(report: ConformanceReport) {
        if (report.failedTests.isEmpty()) {
            appendLine("## Failed Tests")
            appendLine()
            appendLine("✅ No failed tests!")
            appendLine()
            return
        }
        
        appendLine("## Failed Tests (${report.failedTests.size})")
        appendLine()
        
        report.failedTests.take(20).forEach { test ->
            appendLine("### ${test.testId}")
            appendLine()
            appendLine("- **Description**: ${test.description}")
            appendLine("- **Category**: ${test.category}")
            appendLine("- **Spec Section**: ${test.specSection}")
            appendLine("- **Platforms**: ${test.platforms.joinToString(", ")}")
            appendLine("- **Error**: ${test.errorMessage}")
            appendLine()
            
            if (test.expectedOutput != null && test.actualOutput != null) {
                appendLine("<details>")
                appendLine("<summary>Output Comparison</summary>")
                appendLine()
                appendLine("**Expected:**")
                appendLine("```")
                appendLine(test.expectedOutput.take(200))
                if (test.expectedOutput.length > 200) appendLine("... (truncated)")
                appendLine("```")
                appendLine()
                appendLine("**Actual:**")
                appendLine("```")
                appendLine(test.actualOutput.take(200))
                if (test.actualOutput.length > 200) appendLine("... (truncated)")
                appendLine("```")
                appendLine("</details>")
                appendLine()
            }
        }
        
        if (report.failedTests.size > 20) {
            appendLine("*... and ${report.failedTests.size - 20} more failed tests*")
            appendLine()
        }
    }
    
    private fun StringBuilder.appendPendingTests(report: ConformanceReport) {
        if (report.pendingTests.isEmpty()) {
            appendLine("## Pending Tests")
            appendLine()
            appendLine("✅ No pending tests!")
            appendLine()
            return
        }
        
        appendLine("## Pending Tests (${report.pendingTests.size})")
        appendLine()
        
        report.pendingTests.take(10).forEach { test ->
            appendLine("- **${test.testId}**: ${test.description}")
            appendLine("  - Category: ${test.category}")
            appendLine("  - Reason: ${test.reason}")
        }
        
        if (report.pendingTests.size > 10) {
            appendLine()
            appendLine("*... and ${report.pendingTests.size - 10} more pending tests*")
        }
        appendLine()
    }
    
    private fun StringBuilder.appendCertificationStatus(report: ConformanceReport) {
        val status = report.certificationStatus
        
        appendLine("## Certification Status")
        appendLine()
        
        if (status.isReady) {
            appendLine("✅ **READY FOR CERTIFICATION**")
        } else {
            appendLine("❌ **NOT READY FOR CERTIFICATION**")
        }
        appendLine()
        appendLine("**Progress**: ${formatPercentage(status.overallProgress / 100.0)}")
        appendLine()
        
        if (status.blockingIssues.isNotEmpty()) {
            appendLine("### Blocking Issues (${status.blockingIssues.size})")
            appendLine()
            
            status.blockingIssues.forEach { issue ->
                val icon = when (issue.severity) {
                    IssueSeverity.CRITICAL -> "🔴"
                    IssueSeverity.HIGH -> "🟠"
                    IssueSeverity.MEDIUM -> "🟡"
                    IssueSeverity.LOW -> "🟢"
                }
                
                appendLine("$icon **${issue.severity}**: ${issue.description}")
                appendLine("- Affected Tests: ${issue.affectedTests.size}")
                appendLine("- Resolution: ${issue.resolution}")
                appendLine()
            }
        }
        
        if (status.recommendations.isNotEmpty()) {
            appendLine("### Recommendations")
            appendLine()
            status.recommendations.forEach { recommendation ->
                appendLine("- $recommendation")
            }
            appendLine()
        }
    }
    
    private fun StringBuilder.appendFooter(report: ConformanceReport) {
        appendLine("---")
        appendLine()
        appendLine("*Report generated by Markup Poet AsciiDoc Converter*")
    }
    
    private fun formatTimestamp(timestamp: Long): String {
        // Simple timestamp formatting (could be improved with platform-specific date formatting)
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
