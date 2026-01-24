package org.markup.poet.tck.conformance

/**
 * Generates HTML format conformance reports.
 * 
 * The HTML reporter creates interactive, styled reports with:
 * - Responsive design
 * - Color-coded results
 * - Collapsible sections
 * - Summary charts
 * 
 * **Usage:**
 * ```kotlin
 * val reporter = DefaultHtmlReporter()
 * val html = reporter.generateHtml(report)
 * 
 * // Write to file
 * File("conformance-report.html").writeText(html)
 * ```
 */
interface HtmlReporter {
    /**
     * Generate HTML representation of the conformance report.
     * 
     * @param report Conformance report to format
     * @return HTML string
     */
    fun generateHtml(report: ConformanceReport): String
}

/**
 * Default implementation of HtmlReporter.
 * 
 * Generates a self-contained HTML file with embedded CSS and minimal JavaScript.
 */
class DefaultHtmlReporter : HtmlReporter {
    
    override fun generateHtml(report: ConformanceReport): String {
        return buildString {
            appendHtmlHeader(report)
            appendHtmlBody(report)
            appendHtmlFooter()
        }
    }
    
    private fun StringBuilder.appendHtmlHeader(report: ConformanceReport) {
        appendLine("<!DOCTYPE html>")
        appendLine("<html lang=\"en\">")
        appendLine("<head>")
        appendLine("    <meta charset=\"UTF-8\">")
        appendLine("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
        appendLine("    <title>AsciiDoc Conformance Report</title>")
        appendLine("    <style>")
        appendLine(getEmbeddedCss())
        appendLine("    </style>")
        appendLine("</head>")
        appendLine("<body>")
    }
    
    private fun StringBuilder.appendHtmlBody(report: ConformanceReport) {
        appendLine("    <div class=\"container\">")
        appendLine("        <header>")
        appendLine("            <h1>AsciiDoc Conformance Report</h1>")
        appendLine("            <div class=\"metadata\">")
        appendLine("                <span><strong>Generated:</strong> ${formatTimestamp(report.metadata.generatedAt)}</span>")
        appendLine("                <span><strong>Library:</strong> ${report.metadata.libraryVersion}</span>")
        appendLine("                <span><strong>Spec:</strong> ${report.metadata.specVersion}</span>")
        appendLine("                <span><strong>TCK:</strong> ${report.metadata.tckCommitHash.take(8)}</span>")
        appendLine("            </div>")
        appendLine("        </header>")
        appendLine()
        
        appendSummarySection(report)
        appendCertificationSection(report)
        appendPlatformSection(report)
        appendCategorySection(report)
        appendFailedTestsSection(report)
        appendPendingTestsSection(report)
        
        appendLine("    </div>")
    }
    
    private fun StringBuilder.appendSummarySection(report: ConformanceReport) {
        val summary = report.summary
        val passRateClass = when {
            summary.overallPassRate >= 0.95 -> "success"
            summary.overallPassRate >= 0.80 -> "warning"
            else -> "error"
        }
        
        appendLine("        <section class=\"summary\">")
        appendLine("            <h2>Executive Summary</h2>")
        appendLine("            <div class=\"stats-grid\">")
        appendLine("                <div class=\"stat-card\">")
        appendLine("                    <div class=\"stat-value\">${summary.totalTests}</div>")
        appendLine("                    <div class=\"stat-label\">Total Tests</div>")
        appendLine("                </div>")
        appendLine("                <div class=\"stat-card $passRateClass\">")
        appendLine("                    <div class=\"stat-value\">${formatPercentage(summary.overallPassRate)}</div>")
        appendLine("                    <div class=\"stat-label\">Pass Rate</div>")
        appendLine("                </div>")
        appendLine("                <div class=\"stat-card success\">")
        appendLine("                    <div class=\"stat-value\">${summary.passed}</div>")
        appendLine("                    <div class=\"stat-label\">Passed</div>")
        appendLine("                </div>")
        appendLine("                <div class=\"stat-card error\">")
        appendLine("                    <div class=\"stat-value\">${summary.failed}</div>")
        appendLine("                    <div class=\"stat-label\">Failed</div>")
        appendLine("                </div>")
        appendLine("                <div class=\"stat-card warning\">")
        appendLine("                    <div class=\"stat-value\">${summary.pending}</div>")
        appendLine("                    <div class=\"stat-label\">Pending</div>")
        appendLine("                </div>")
        appendLine("                <div class=\"stat-card\">")
        appendLine("                    <div class=\"stat-value\">${formatDuration(summary.totalDurationMs)}</div>")
        appendLine("                    <div class=\"stat-label\">Duration</div>")
        appendLine("                </div>")
        appendLine("            </div>")
        appendLine("        </section>")
        appendLine()
    }
    
    private fun StringBuilder.appendCertificationSection(report: ConformanceReport) {
        val status = report.certificationStatus
        val statusClass = if (status.isReady) "success" else "error"
        val statusText = if (status.isReady) "READY FOR CERTIFICATION" else "NOT READY"
        
        appendLine("        <section class=\"certification\">")
        appendLine("            <h2>Certification Status</h2>")
        appendLine("            <div class=\"cert-status $statusClass\">")
        appendLine("                <div class=\"cert-badge\">$statusText</div>")
        appendLine("                <div class=\"cert-progress\">Progress: ${formatPercentage(status.overallProgress / 100.0)}</div>")
        appendLine("            </div>")
        
        if (status.blockingIssues.isNotEmpty()) {
            appendLine("            <h3>Blocking Issues</h3>")
            appendLine("            <div class=\"issues-list\">")
            status.blockingIssues.forEach { issue ->
                val severityClass = issue.severity.name.lowercase()
                appendLine("                <div class=\"issue $severityClass\">")
                appendLine("                    <div class=\"issue-header\">")
                appendLine("                        <span class=\"issue-severity\">${issue.severity}</span>")
                appendLine("                        <span class=\"issue-title\">${escapeHtml(issue.description)}</span>")
                appendLine("                    </div>")
                appendLine("                    <div class=\"issue-details\">")
                appendLine("                        <p><strong>Affected Tests:</strong> ${issue.affectedTests.size}</p>")
                appendLine("                        <p><strong>Resolution:</strong> ${escapeHtml(issue.resolution)}</p>")
                appendLine("                    </div>")
                appendLine("                </div>")
            }
            appendLine("            </div>")
        }
        
        if (status.recommendations.isNotEmpty()) {
            appendLine("            <h3>Recommendations</h3>")
            appendLine("            <ul class=\"recommendations\">")
            status.recommendations.forEach { recommendation ->
                appendLine("                <li>${escapeHtml(recommendation)}</li>")
            }
            appendLine("            </ul>")
        }
        
        appendLine("        </section>")
        appendLine()
    }
    
    private fun StringBuilder.appendPlatformSection(report: ConformanceReport) {
        if (report.platformResults.isEmpty()) return
        
        appendLine("        <section class=\"platforms\">")
        appendLine("            <h2>Platform Results</h2>")
        appendLine("            <table>")
        appendLine("                <thead>")
        appendLine("                    <tr>")
        appendLine("                        <th>Platform</th>")
        appendLine("                        <th>Total</th>")
        appendLine("                        <th>Passed</th>")
        appendLine("                        <th>Failed</th>")
        appendLine("                        <th>Pass Rate</th>")
        appendLine("                    </tr>")
        appendLine("                </thead>")
        appendLine("                <tbody>")
        
        report.platformResults.sortedByDescending { it.passRate }.forEach { platform ->
            val rowClass = when {
                platform.passRate >= 0.95 -> "success"
                platform.passRate >= 0.80 -> "warning"
                else -> "error"
            }
            appendLine("                    <tr class=\"$rowClass\">")
            appendLine("                        <td>${platform.platform}</td>")
            appendLine("                        <td>${platform.totalTests}</td>")
            appendLine("                        <td>${platform.passed}</td>")
            appendLine("                        <td>${platform.failed}</td>")
            appendLine("                        <td>${formatPercentage(platform.passRate)}</td>")
            appendLine("                    </tr>")
        }
        
        appendLine("                </tbody>")
        appendLine("            </table>")
        appendLine("        </section>")
        appendLine()
    }
    
    private fun StringBuilder.appendCategorySection(report: ConformanceReport) {
        if (report.categoryResults.isEmpty()) return
        
        appendLine("        <section class=\"categories\">")
        appendLine("            <h2>Category Results</h2>")
        appendLine("            <table>")
        appendLine("                <thead>")
        appendLine("                    <tr>")
        appendLine("                        <th>Category</th>")
        appendLine("                        <th>Total</th>")
        appendLine("                        <th>Passed</th>")
        appendLine("                        <th>Failed</th>")
        appendLine("                        <th>Pass Rate</th>")
        appendLine("                    </tr>")
        appendLine("                </thead>")
        appendLine("                <tbody>")
        
        report.categoryResults.sortedByDescending { it.passRate }.forEach { category ->
            val rowClass = when {
                category.passRate >= 0.95 -> "success"
                category.passRate >= 0.80 -> "warning"
                else -> "error"
            }
            appendLine("                    <tr class=\"$rowClass\">")
            appendLine("                        <td>${category.category}</td>")
            appendLine("                        <td>${category.totalTests}</td>")
            appendLine("                        <td>${category.passed}</td>")
            appendLine("                        <td>${category.failed}</td>")
            appendLine("                        <td>${formatPercentage(category.passRate)}</td>")
            appendLine("                    </tr>")
        }
        
        appendLine("                </tbody>")
        appendLine("            </table>")
        appendLine("        </section>")
        appendLine()
    }
    
    private fun StringBuilder.appendFailedTestsSection(report: ConformanceReport) {
        if (report.failedTests.isEmpty()) {
            appendLine("        <section class=\"failed-tests\">")
            appendLine("            <h2>Failed Tests</h2>")
            appendLine("            <p class=\"success-message\">✅ No failed tests!</p>")
            appendLine("        </section>")
            appendLine()
            return
        }
        
        appendLine("        <section class=\"failed-tests\">")
        appendLine("            <h2>Failed Tests (${report.failedTests.size})</h2>")
        
        report.failedTests.take(20).forEach { test ->
            appendLine("            <details class=\"test-detail\">")
            appendLine("                <summary>${escapeHtml(test.testId)} - ${escapeHtml(test.description)}</summary>")
            appendLine("                <div class=\"test-info\">")
            appendLine("                    <p><strong>Category:</strong> ${test.category}</p>")
            appendLine("                    <p><strong>Spec Section:</strong> ${test.specSection}</p>")
            appendLine("                    <p><strong>Platforms:</strong> ${test.platforms.joinToString(", ")}</p>")
            appendLine("                    <p><strong>Error:</strong> ${escapeHtml(test.errorMessage)}</p>")
            appendLine("                </div>")
            appendLine("            </details>")
        }
        
        if (report.failedTests.size > 20) {
            appendLine("            <p class=\"more-tests\">... and ${report.failedTests.size - 20} more failed tests</p>")
        }
        
        appendLine("        </section>")
        appendLine()
    }
    
    private fun StringBuilder.appendPendingTestsSection(report: ConformanceReport) {
        if (report.pendingTests.isEmpty()) {
            appendLine("        <section class=\"pending-tests\">")
            appendLine("            <h2>Pending Tests</h2>")
            appendLine("            <p class=\"success-message\">✅ No pending tests!</p>")
            appendLine("        </section>")
            appendLine()
            return
        }
        
        appendLine("        <section class=\"pending-tests\">")
        appendLine("            <h2>Pending Tests (${report.pendingTests.size})</h2>")
        appendLine("            <ul class=\"pending-list\">")
        
        report.pendingTests.take(10).forEach { test ->
            appendLine("                <li>")
            appendLine("                    <strong>${escapeHtml(test.testId)}</strong>: ${escapeHtml(test.description)}")
            appendLine("                    <br><small>${test.category} - ${escapeHtml(test.reason)}</small>")
            appendLine("                </li>")
        }
        
        appendLine("            </ul>")
        
        if (report.pendingTests.size > 10) {
            appendLine("            <p class=\"more-tests\">... and ${report.pendingTests.size - 10} more pending tests</p>")
        }
        
        appendLine("        </section>")
        appendLine()
    }
    
    private fun StringBuilder.appendHtmlFooter() {
        appendLine("        <footer>")
        appendLine("            <p>Report generated by Markup Poet AsciiDoc Converter</p>")
        appendLine("        </footer>")
        appendLine("    </div>")
        appendLine("</body>")
        appendLine("</html>")
    }
    
    private fun getEmbeddedCss(): String {
        return """
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif; line-height: 1.6; color: #333; background: #f5f5f5; }
        .container { max-width: 1200px; margin: 0 auto; padding: 20px; }
        header { background: white; padding: 30px; border-radius: 8px; margin-bottom: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        header h1 { color: #2c3e50; margin-bottom: 15px; }
        .metadata { display: flex; gap: 20px; flex-wrap: wrap; font-size: 14px; color: #666; }
        section { background: white; padding: 30px; border-radius: 8px; margin-bottom: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        h2 { color: #2c3e50; margin-bottom: 20px; border-bottom: 2px solid #3498db; padding-bottom: 10px; }
        h3 { color: #34495e; margin: 20px 0 10px; }
        .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 15px; }
        .stat-card { padding: 20px; border-radius: 6px; text-align: center; background: #ecf0f1; }
        .stat-value { font-size: 32px; font-weight: bold; margin-bottom: 5px; }
        .stat-label { font-size: 14px; color: #7f8c8d; }
        .stat-card.success { background: #d5f4e6; color: #27ae60; }
        .stat-card.warning { background: #fff3cd; color: #f39c12; }
        .stat-card.error { background: #f8d7da; color: #e74c3c; }
        .cert-status { padding: 20px; border-radius: 6px; text-align: center; margin-bottom: 20px; }
        .cert-status.success { background: #d5f4e6; }
        .cert-status.error { background: #f8d7da; }
        .cert-badge { font-size: 24px; font-weight: bold; margin-bottom: 10px; }
        .cert-progress { font-size: 18px; }
        .issues-list { margin-top: 15px; }
        .issue { padding: 15px; border-left: 4px solid; margin-bottom: 10px; border-radius: 4px; }
        .issue.critical { border-color: #e74c3c; background: #fadbd8; }
        .issue.high { border-color: #f39c12; background: #fef5e7; }
        .issue.medium { border-color: #f1c40f; background: #fcf3cf; }
        .issue.low { border-color: #3498db; background: #d6eaf8; }
        .issue-header { display: flex; gap: 10px; align-items: center; margin-bottom: 10px; }
        .issue-severity { font-weight: bold; padding: 2px 8px; border-radius: 3px; font-size: 12px; background: rgba(0,0,0,0.1); }
        .issue-title { flex: 1; font-weight: 500; }
        .recommendations { list-style-position: inside; }
        .recommendations li { padding: 8px 0; }
        table { width: 100%; border-collapse: collapse; }
        th, td { padding: 12px; text-align: left; border-bottom: 1px solid #ddd; }
        th { background: #f8f9fa; font-weight: 600; }
        tr.success { background: #d5f4e6; }
        tr.warning { background: #fff3cd; }
        tr.error { background: #f8d7da; }
        .test-detail { margin-bottom: 10px; border: 1px solid #ddd; border-radius: 4px; }
        .test-detail summary { padding: 12px; cursor: pointer; background: #f8f9fa; font-weight: 500; }
        .test-detail summary:hover { background: #e9ecef; }
        .test-info { padding: 15px; }
        .test-info p { margin-bottom: 8px; }
        .pending-list { list-style-position: inside; }
        .pending-list li { padding: 10px 0; border-bottom: 1px solid #eee; }
        .success-message { color: #27ae60; font-size: 18px; padding: 20px; text-align: center; }
        .more-tests { margin-top: 15px; font-style: italic; color: #666; }
        footer { text-align: center; padding: 20px; color: #7f8c8d; font-size: 14px; }
        """.trimIndent()
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
    
    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}
