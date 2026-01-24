package org.markup.poet.tck.conformance

import org.markup.poet.tck.execution.*
import org.markup.poet.tck.fixtures.FixtureCategory
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class ReportersTest {
    
    private fun createMockReport(): ConformanceReport {
        val metadata = ReportMetadata(
            generatedAt = 1706140800000L,
            specVersion = "1.0.0",
            tckCommitHash = "abc123def456",
            libraryVersion = "0.1.0",
            platforms = listOf("JVM", "iOS")
        )
        
        val summary = ConformanceSummary(
            totalTests = 100,
            passed = 85,
            failed = 10,
            pending = 5,
            skipped = 0,
            overallPassRate = 0.85,
            officialTestsPassRate = 0.80,
            customTestsPassRate = 0.90,
            totalDurationMs = 5000
        )
        
        val platformResults = listOf(
            PlatformConformance("JVM", 50, 45, 5, 0.90, listOf("test-1", "test-2")),
            PlatformConformance("iOS", 50, 40, 10, 0.80, listOf("test-3", "test-4"))
        )
        
        val categoryResults = listOf(
            CategoryConformance("BLOCK_PARAGRAPH", 30, 28, 2, 0.93, "4.1"),
            CategoryConformance("INLINE_BOLD", 20, 18, 2, 0.90, "5.2")
        )
        
        val failedTests = listOf(
            FailedTestDetail(
                "test-1",
                "Paragraph test",
                "BLOCK_PARAGRAPH",
                "4.1",
                listOf("JVM"),
                "Output mismatch",
                "<p>expected</p>",
                "<p>actual</p>"
            )
        )
        
        val pendingTests = listOf(
            PendingTestDetail(
                "test-pending",
                "Table test",
                "BLOCK_TABLE",
                "6.1",
                "Tables not implemented"
            )
        )
        
        val certificationStatus = CertificationStatus(
            isReady = false,
            overallProgress = 85.0,
            blockingIssues = listOf(
                BlockingIssue(
                    IssueSeverity.HIGH,
                    "Pass rate below 95%",
                    listOf("test-1", "test-2"),
                    "Fix failing tests"
                )
            ),
            recommendations = listOf("Fix 10 failing tests", "Implement 5 pending features")
        )
        
        return ConformanceReport(
            metadata = metadata,
            summary = summary,
            platformResults = platformResults,
            categoryResults = categoryResults,
            specSectionResults = emptyList(),
            failedTests = failedTests,
            pendingTests = pendingTests,
            certificationStatus = certificationStatus
        )
    }
    
    @Test
    fun `JsonReporter should generate valid JSON`() {
        val reporter = DefaultJsonReporter()
        val report = createMockReport()
        
        val json = reporter.generateJson(report)
        
        assertNotNull(json)
        assertTrue(json.isNotEmpty())
        assertTrue(json.contains("\"totalTests\""))
        assertTrue(json.contains("\"passed\""))
        assertTrue(json.contains("\"failed\""))
        assertTrue(json.contains("\"overallPassRate\""))
    }
    
    @Test
    fun `JsonReporter should pretty print by default`() {
        val reporter = DefaultJsonReporter(prettyPrint = true)
        val report = createMockReport()
        
        val json = reporter.generateJson(report)
        
        // Pretty printed JSON should have newlines and indentation
        assertTrue(json.contains("\n"))
        assertTrue(json.contains("  "))
    }
    
    @Test
    fun `CompactJsonReporter should not pretty print`() {
        val reporter = CompactJsonReporter()
        val report = createMockReport()
        
        val json = reporter.generateJson(report)
        
        assertNotNull(json)
        assertTrue(json.isNotEmpty())
        // Compact JSON should not have excessive whitespace
        assertFalse(json.contains("\n  "))
    }
    
    @Test
    fun `MarkdownReporter should generate valid Markdown`() {
        val reporter = DefaultMarkdownReporter()
        val report = createMockReport()
        
        val markdown = reporter.generateMarkdown(report)
        
        assertNotNull(markdown)
        assertTrue(markdown.isNotEmpty())
        assertTrue(markdown.contains("# AsciiDoc Conformance Report"))
        assertTrue(markdown.contains("## Executive Summary"))
        assertTrue(markdown.contains("## Platform Results"))
        assertTrue(markdown.contains("## Category Results"))
        assertTrue(markdown.contains("## Certification Status"))
    }
    
    @Test
    fun `MarkdownReporter should include tables`() {
        val reporter = DefaultMarkdownReporter()
        val report = createMockReport()
        
        val markdown = reporter.generateMarkdown(report)
        
        // Check for table syntax
        assertTrue(markdown.contains("|"))
        assertTrue(markdown.contains("---"))
    }
    
    @Test
    fun `MarkdownReporter should include failed test details`() {
        val reporter = DefaultMarkdownReporter()
        val report = createMockReport()
        
        val markdown = reporter.generateMarkdown(report)
        
        assertTrue(markdown.contains("## Failed Tests"))
        assertTrue(markdown.contains("test-1"))
        assertTrue(markdown.contains("Output mismatch"))
    }
    
    @Test
    fun `MarkdownReporter should include pending test details`() {
        val reporter = DefaultMarkdownReporter()
        val report = createMockReport()
        
        val markdown = reporter.generateMarkdown(report)
        
        assertTrue(markdown.contains("## Pending Tests"))
        assertTrue(markdown.contains("test-pending"))
        assertTrue(markdown.contains("Tables not implemented"))
    }
    
    @Test
    fun `MarkdownReporter should include certification status`() {
        val reporter = DefaultMarkdownReporter()
        val report = createMockReport()
        
        val markdown = reporter.generateMarkdown(report)
        
        assertTrue(markdown.contains("## Certification Status"))
        assertTrue(markdown.contains("NOT READY"))
        assertTrue(markdown.contains("Blocking Issues"))
        assertTrue(markdown.contains("Recommendations"))
    }
    
    @Test
    fun `HtmlReporter should generate valid HTML`() {
        val reporter = DefaultHtmlReporter()
        val report = createMockReport()
        
        val html = reporter.generateHtml(report)
        
        assertNotNull(html)
        assertTrue(html.isNotEmpty())
        assertTrue(html.contains("<!DOCTYPE html>"))
        assertTrue(html.contains("<html"))
        assertTrue(html.contains("</html>"))
        assertTrue(html.contains("<head>"))
        assertTrue(html.contains("<body>"))
    }
    
    @Test
    fun `HtmlReporter should include CSS`() {
        val reporter = DefaultHtmlReporter()
        val report = createMockReport()
        
        val html = reporter.generateHtml(report)
        
        assertTrue(html.contains("<style>"))
        assertTrue(html.contains("</style>"))
    }
    
    @Test
    fun `HtmlReporter should include summary section`() {
        val reporter = DefaultHtmlReporter()
        val report = createMockReport()
        
        val html = reporter.generateHtml(report)
        
        assertTrue(html.contains("Executive Summary"))
        assertTrue(html.contains("Total Tests"))
        assertTrue(html.contains("Pass Rate"))
    }
    
    @Test
    fun `HtmlReporter should include platform results table`() {
        val reporter = DefaultHtmlReporter()
        val report = createMockReport()
        
        val html = reporter.generateHtml(report)
        
        assertTrue(html.contains("Platform Results"))
        assertTrue(html.contains("<table>"))
        assertTrue(html.contains("JVM"))
        assertTrue(html.contains("iOS"))
    }
    
    @Test
    fun `HtmlReporter should include certification status`() {
        val reporter = DefaultHtmlReporter()
        val report = createMockReport()
        
        val html = reporter.generateHtml(report)
        
        assertTrue(html.contains("Certification Status"))
        assertTrue(html.contains("NOT READY"))
        assertTrue(html.contains("Blocking Issues"))
    }
    
    @Test
    fun `HtmlReporter should escape HTML in test details`() {
        val reporter = DefaultHtmlReporter()
        
        // Create report with HTML characters in test details
        val report = createMockReport().copy(
            failedTests = listOf(
                FailedTestDetail(
                    "test-html",
                    "Test with <html> & \"quotes\"",
                    "BLOCK_PARAGRAPH",
                    "4.1",
                    listOf("JVM"),
                    "Error with <tags>",
                    null,
                    null
                )
            )
        )
        
        val html = reporter.generateHtml(report)
        
        // HTML should be escaped
        assertTrue(html.contains("&lt;html&gt;"))
        assertTrue(html.contains("&amp;"))
        assertTrue(html.contains("&quot;"))
    }
}
