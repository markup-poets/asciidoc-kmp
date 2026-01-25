package org.markup.poet.tck.publisher

import org.markup.poet.tck.execution.AggregatedResults
import org.markup.poet.tck.execution.TestExecutionResult
import org.markup.poet.tck.execution.TestStatus
import org.markup.poet.tck.fixtures.FixtureCategory
import kotlin.math.roundToInt

/**
 * Default implementation of [TckResultsExporter] that generates AsciiDoc documents
 * from TCK test results.
 *
 * This implementation creates a comprehensive, well-structured AsciiDoc document that includes:
 * - Document title and table of contents
 * - Summary statistics with pass rates
 * - Test results organized by category
 * - Detailed information about failed tests
 * - Metadata about the test run
 *
 * The generated AsciiDoc is designed to be parsed by our own parser and rendered
 * with the Kotlin theme, demonstrating dogfooding of our implementation.
 */
class DefaultTckResultsExporter : TckResultsExporter {
    
    override fun export(
        results: AggregatedResults,
        metadata: ExportMetadata
    ): Result<String> {
        return try {
            val document = buildString {
                appendDocumentHeader()
                appendSummarySection(results)
                appendTestResultsByCategory(results)
                appendFailedTestsSection(results)
                appendMetadataSection(metadata, results)
            }
            Result.success(document)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Append the document header with title and document attributes.
     */
    private fun StringBuilder.appendDocumentHeader() {
        appendLine("= AsciiDoc Konvert - TCK Certification Results")
        appendLine(":toc: left")
        appendLine(":toclevels: 3")
        appendLine(":icons: font")
        appendLine()
    }
    
    /**
     * Append the summary section with overall statistics.
     */
    private fun StringBuilder.appendSummarySection(results: AggregatedResults) {
        appendLine("== Summary")
        appendLine()
        
        // Summary statistics table
        appendLine("[cols=\"1,3\"]")
        appendLine("|===")
        appendLine("| Metric | Value")
        appendLine()
        appendLine("| Total Tests | ${results.totalTests}")
        appendLine("| Passed | ${results.passed} (${formatPercentage(results.passed, results.totalTests)})")
        appendLine("| Failed | ${results.failed} (${formatPercentage(results.failed, results.totalTests)})")
        
        if (results.errors > 0) {
            appendLine("| Errors | ${results.errors} (${formatPercentage(results.errors, results.totalTests)})")
        }
        
        if (results.pending > 0) {
            appendLine("| Pending | ${results.pending} (${formatPercentage(results.pending, results.totalTests)})")
        }
        
        if (results.skipped > 0) {
            appendLine("| Skipped | ${results.skipped} (${formatPercentage(results.skipped, results.totalTests)})")
        }
        
        appendLine("| Certification Status | ${determineCertificationStatus(results)}")
        appendLine("|===")
        appendLine()
        
        // Overall pass rate with admonition
        val passRate = results.passRate() * 100
        val admonitionType = when {
            passRate >= 95.0 -> "TIP"
            passRate >= 70.0 -> "NOTE"
            else -> "WARNING"
        }
        
        appendLine("$admonitionType: Overall pass rate: *${formatPercentageValue(passRate)}*")
        appendLine()
    }
    
    /**
     * Append test results organized by category.
     */
    private fun StringBuilder.appendTestResultsByCategory(results: AggregatedResults) {
        appendLine("== Test Results by Category")
        appendLine()
        
        if (results.byCategory.isEmpty()) {
            appendLine("No test results available.")
            appendLine()
            return
        }
        
        // Sort categories by name for consistent output
        val sortedCategories = results.byCategory.entries.sortedBy { it.key.name }
        
        for ((category, categoryResults) in sortedCategories) {
            appendLine("=== ${formatCategoryName(category)}")
            appendLine()
            
            // Category summary
            appendLine("*Pass Rate:* ${formatPercentageValue(categoryResults.passRate * 100)} " +
                    "(${categoryResults.passed}/${categoryResults.total} tests)")
            appendLine()
            
            // Get failed tests for this category to show details
            val categoryFailedTests = results.failedTests.filter { it.category == category }
            
            if (categoryFailedTests.isNotEmpty()) {
                appendLine("*Failed Tests in this Category:*")
                appendLine()
                appendLine("[cols=\"3,1,1\"]")
                appendLine("|===")
                appendLine("| Test Name | Status | Duration")
                appendLine()
                
                // Sort tests by fixture ID for consistent output
                for (test in categoryFailedTests.sortedBy { it.fixtureId }) {
                    val statusIndicator = getStatusIndicator(test.status)
                    val statusText = "$statusIndicator ${test.status}"
                    appendLine("| ${sanitizeForAsciidoc(test.fixtureId)} | $statusText | ${test.durationMs}ms")
                }
                
                appendLine("|===")
                appendLine()
            }
        }
    }
    
    /**
     * Append the failed tests section with detailed error information.
     */
    private fun StringBuilder.appendFailedTestsSection(results: AggregatedResults) {
        val failedAndErrorTests = results.failedTests.filter { 
            it.status == TestStatus.FAILED || it.status == TestStatus.ERROR 
        }
        
        if (failedAndErrorTests.isEmpty()) {
            return
        }
        
        appendLine("== Failed Tests")
        appendLine()
        appendLine("WARNING: The following tests failed or encountered errors:")
        appendLine()
        
        // Sort by fixture ID for consistent output
        for (test in failedAndErrorTests.sortedBy { it.fixtureId }) {
            appendLine("=== ${sanitizeForAsciidoc(test.fixtureId)}")
            appendLine()
            
            // Status and category
            appendLine("*Status:* ${getStatusIndicator(test.status)} ${test.status}")
            if (test.category != null) {
                appendLine()
                appendLine("*Category:* ${formatCategoryName(test.category)}")
            }
            appendLine()
            
            // Error message
            if (test.errorMessage != null) {
                appendLine("*Error Message:*")
                appendLine()
                appendLine("[source]")
                appendLine("----")
                appendLine(sanitizeForAsciidoc(test.errorMessage))
                appendLine("----")
                appendLine()
            }
            
            // Expected vs Actual output comparison
            if (test.expectedOutput != null && test.actualOutput != null) {
                appendLine("*Expected Output:*")
                appendLine()
                appendLine("[source,json]")
                appendLine("----")
                appendLine(sanitizeForAsciidoc(test.expectedOutput))
                appendLine("----")
                appendLine()
                
                appendLine("*Actual Output:*")
                appendLine()
                appendLine("[source,json]")
                appendLine("----")
                appendLine(sanitizeForAsciidoc(test.actualOutput))
                appendLine("----")
                appendLine()
            }
            
            // Diff if available
            if (test.diff != null) {
                appendLine("*Diff:*")
                appendLine()
                appendLine("[source,diff]")
                appendLine("----")
                appendLine(sanitizeForAsciidoc(test.diff))
                appendLine("----")
                appendLine()
            }
            
            // Stack trace for errors
            if (test.status == TestStatus.ERROR && test.stackTrace != null) {
                appendLine("*Stack Trace:*")
                appendLine()
                appendLine("[source]")
                appendLine("----")
                appendLine(sanitizeForAsciidoc(test.stackTrace))
                appendLine("----")
                appendLine()
            }
        }
    }
    
    /**
     * Append the metadata section with version and run information.
     */
    private fun StringBuilder.appendMetadataSection(metadata: ExportMetadata, results: AggregatedResults) {
        appendLine("== Metadata")
        appendLine()
        
        // Format timestamp
        val timestamp = formatTimestamp(metadata.timestamp)
        
        appendLine("* *Generated:* $timestamp")
        appendLine("* *Spec Version:* ${metadata.specVersion}")
        appendLine("* *TCK Commit:* `${metadata.tckCommitHash}`")
        appendLine("* *Library Version:* ${metadata.libraryVersion}")
        appendLine("* *Run ID:* `${metadata.runId}`")
        appendLine("* *Platforms:* ${metadata.platforms.joinToString(", ")}")
        appendLine()
        
        // Platform breakdown if available
        if (results.byPlatform.isNotEmpty()) {
            appendLine("=== Results by Platform")
            appendLine()
            appendLine("[cols=\"2,1,1,1\"]")
            appendLine("|===")
            appendLine("| Platform | Total | Passed | Pass Rate")
            appendLine()
            
            for ((platform, platformResults) in results.byPlatform.entries.sortedBy { it.key }) {
                appendLine("| $platform | ${platformResults.total} | ${platformResults.passed} | " +
                        "${formatPercentageValue(platformResults.passRate * 100)}")
            }
            
            appendLine("|===")
            appendLine()
        }
    }
    
    /**
     * Get the status indicator emoji for a test status.
     */
    private fun getStatusIndicator(status: TestStatus): String {
        return when (status) {
            TestStatus.PASSED -> "✅"
            TestStatus.FAILED -> "❌"
            TestStatus.ERROR -> "💥"
            TestStatus.PENDING -> "⏳"
            TestStatus.SKIPPED -> "⏭️"
        }
    }
    
    /**
     * Format a category name for display.
     */
    private fun formatCategoryName(category: FixtureCategory): String {
        return category.name.lowercase().split('_').joinToString(" ") { word ->
            word.replaceFirstChar { it.uppercase() }
        }
    }
    
    /**
     * Format a percentage value.
     */
    private fun formatPercentage(value: Int, total: Int): String {
        if (total == 0) return "0.0%"
        val percentage = (value.toDouble() / total) * 100
        return formatPercentageValue(percentage)
    }
    
    /**
     * Format a percentage value (already calculated as 0-100).
     * Uses manual formatting to avoid locale issues (e.g., German uses comma instead of period).
     */
    private fun formatPercentageValue(percentage: Double): String {
        val rounded = (percentage * 10).toInt() / 10.0
        return "${rounded}%"
    }
    
    /**
     * Determine the certification status based on test results.
     */
    private fun determineCertificationStatus(results: AggregatedResults): String {
        val passRate = results.passRate() * 100
        
        return when {
            passRate >= 100.0 -> "✅ Ready for Certification"
            passRate >= 95.0 -> "🟡 Near Certification (${formatPercentageValue(passRate)})"
            passRate >= 70.0 -> "🟠 In Progress (${formatPercentageValue(passRate)})"
            else -> "🔴 Blocked (${formatPercentageValue(passRate)})"
        }
    }
    
    /**
     * Format a Unix timestamp (milliseconds) to a human-readable string.
     */
    private fun formatTimestamp(timestamp: Long): String {
        // For now, just return the timestamp as a string
        // In a real implementation, we'd use platform-specific date formatting
        // This is sufficient for the exporter's purposes
        return timestamp.toString()
    }
    
    /**
     * Sanitize text for inclusion in AsciiDoc documents.
     * Escapes special characters that could break AsciiDoc formatting.
     */
    private fun sanitizeForAsciidoc(text: String): String {
        return text
            .replace("|", "\\|")  // Escape pipe characters (table delimiter)
            .take(10000)  // Limit length to prevent extremely long output
    }
}
