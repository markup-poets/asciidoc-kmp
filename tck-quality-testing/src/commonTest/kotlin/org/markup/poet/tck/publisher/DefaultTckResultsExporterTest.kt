package org.markup.poet.tck.publisher

import org.markup.poet.tck.execution.AggregatedResults
import org.markup.poet.tck.execution.CategoryResults
import org.markup.poet.tck.execution.PlatformResults
import org.markup.poet.tck.execution.SourceResults
import org.markup.poet.tck.execution.TestExecutionResult
import org.markup.poet.tck.execution.TestStatus
import org.markup.poet.tck.fixtures.FixtureCategory
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Unit tests for [DefaultTckResultsExporter].
 *
 * Tests verify that the exporter generates valid AsciiDoc documents with all required sections
 * and properly formatted content.
 */
class DefaultTckResultsExporterTest {
    
    private val exporter = DefaultTckResultsExporter()
    
    private fun createMockMetadata(): ExportMetadata {
        return ExportMetadata(
            timestamp = 1706097000000L,
            specVersion = "1.0.0",
            tckCommitHash = "abc123def456",
            libraryVersion = "1.0.0",
            platforms = listOf("JVM", "iOS", "Linux"),
            runId = "test-run-001"
        )
    }
    
    private fun createMockResults(
        passed: Int = 10,
        failed: Int = 2,
        errors: Int = 1,
        pending: Int = 0,
        skipped: Int = 0
    ): AggregatedResults {
        val total = passed + failed + errors + pending + skipped
        
        val failedTests = mutableListOf<TestExecutionResult>()
        
        // Add failed tests
        repeat(failed) { i ->
            failedTests.add(
                TestExecutionResult(
                    fixtureId = "test/failed-$i",
                    status = TestStatus.FAILED,
                    platform = "JVM",
                    durationMs = 25,
                    category = FixtureCategory.INLINE_BOLD,
                    source = "official-tck",
                    errorMessage = "Expected emphasis node, got text node",
                    expectedOutput = """{"name": "emphasis", "content": "text"}""",
                    actualOutput = """{"name": "text", "content": "text"}"""
                )
            )
        }
        
        // Add error tests
        repeat(errors) { i ->
            failedTests.add(
                TestExecutionResult(
                    fixtureId = "test/error-$i",
                    status = TestStatus.ERROR,
                    platform = "JVM",
                    durationMs = 30,
                    category = FixtureCategory.BLOCK_PARAGRAPH,
                    source = "official-tck",
                    errorMessage = "NullPointerException: Unexpected null value",
                    stackTrace = "at org.markup.poet.Parser.parse(Parser.kt:42)\n" +
                            "at org.markup.poet.Test.run(Test.kt:15)"
                )
            )
        }
        
        return AggregatedResults(
            totalTests = total,
            passed = passed,
            failed = failed,
            skipped = skipped,
            pending = pending,
            errors = errors,
            byPlatform = mapOf(
                "JVM" to PlatformResults(
                    platform = "JVM",
                    total = total,
                    passed = passed,
                    failed = failed + errors,
                    passRate = passed.toDouble() / total
                )
            ),
            byCategory = mapOf(
                FixtureCategory.INLINE_BOLD to CategoryResults(
                    category = FixtureCategory.INLINE_BOLD,
                    total = 5,
                    passed = 4,
                    failed = 1,
                    passRate = 0.8
                ),
                FixtureCategory.BLOCK_PARAGRAPH to CategoryResults(
                    category = FixtureCategory.BLOCK_PARAGRAPH,
                    total = 8,
                    passed = 6,
                    failed = 2,
                    passRate = 0.75
                )
            ),
            bySource = mapOf(
                "official-tck" to SourceResults(
                    source = "official-tck",
                    total = total,
                    passed = passed,
                    failed = failed + errors,
                    passRate = passed.toDouble() / total
                )
            ),
            failedTests = failedTests,
            pendingTests = emptyList()
        )
    }
    
    @Test
    fun `should generate valid AsciiDoc document structure`() {
        val results = createMockResults()
        val metadata = createMockMetadata()
        
        val asciidoc = exporter.export(results, metadata).getOrThrow()
        
        // Verify document header
        assertTrue(asciidoc.contains("= AsciiDoc Konvert - TCK Certification Results"))
        assertTrue(asciidoc.contains(":toc: left"))
        assertTrue(asciidoc.contains(":toclevels: 3"))
        assertTrue(asciidoc.contains(":icons: font"))
    }
    
    @Test
    fun `should include summary section with statistics`() {
        val results = createMockResults(passed = 10, failed = 2, errors = 1)
        val metadata = createMockMetadata()
        
        val asciidoc = exporter.export(results, metadata).getOrThrow()
        
        // Verify summary section exists
        assertTrue(asciidoc.contains("== Summary"))
        
        // Verify statistics table
        assertTrue(asciidoc.contains("| Total Tests | 13"))
        assertTrue(asciidoc.contains("| Passed | 10"))
        assertTrue(asciidoc.contains("| Failed | 2"))
        assertTrue(asciidoc.contains("| Errors | 1"))
        
        // Verify pass rate
        assertTrue(asciidoc.contains("Overall pass rate:"))
    }
    
    @Test
    fun `should include test results by category`() {
        val results = createMockResults()
        val metadata = createMockMetadata()
        
        val asciidoc = exporter.export(results, metadata).getOrThrow()
        
        // Verify category sections
        assertTrue(asciidoc.contains("== Test Results by Category"))
        assertTrue(asciidoc.contains("=== Inline Bold"))
        assertTrue(asciidoc.contains("=== Block Paragraph"))
    }
    
    @Test
    fun `should include failed tests section with error details`() {
        val results = createMockResults(passed = 8, failed = 2, errors = 1)
        val metadata = createMockMetadata()
        
        val asciidoc = exporter.export(results, metadata).getOrThrow()
        
        // Verify failed tests section
        assertTrue(asciidoc.contains("== Failed Tests"))
        assertTrue(asciidoc.contains("WARNING: The following tests failed or encountered errors:"))
        
        // Verify failed test details
        assertTrue(asciidoc.contains("test/failed-0"))
        assertTrue(asciidoc.contains("Expected emphasis node, got text node"))
        assertTrue(asciidoc.contains("*Expected Output:*"))
        assertTrue(asciidoc.contains("*Actual Output:*"))
        
        // Verify error test details
        assertTrue(asciidoc.contains("test/error-0"))
        assertTrue(asciidoc.contains("NullPointerException"))
        assertTrue(asciidoc.contains("*Stack Trace:*"))
    }
    
    @Test
    fun `should include metadata section`() {
        val results = createMockResults()
        val metadata = createMockMetadata()
        
        val asciidoc = exporter.export(results, metadata).getOrThrow()
        
        // Verify metadata section
        assertTrue(asciidoc.contains("== Metadata"))
        assertTrue(asciidoc.contains("*Spec Version:* 1.0.0"))
        assertTrue(asciidoc.contains("*TCK Commit:* `abc123def456`"))
        assertTrue(asciidoc.contains("*Library Version:* 1.0.0"))
        assertTrue(asciidoc.contains("*Run ID:* `test-run-001`"))
        assertTrue(asciidoc.contains("*Platforms:* JVM, iOS, Linux"))
    }
    
    @Test
    fun `should include status indicators`() {
        val results = createMockResults(passed = 10, failed = 2, errors = 1)
        val metadata = createMockMetadata()
        
        val asciidoc = exporter.export(results, metadata).getOrThrow()
        
        // Verify status indicators are present in the failed tests section
        // Since we only show failed tests in detail, indicators appear there
        assertTrue(asciidoc.contains("❌"))  // Failed
        assertTrue(asciidoc.contains("💥"))  // Error
        // Passed indicator (✅) appears in certification status
        assertTrue(asciidoc.contains("✅") || asciidoc.contains("🟡") || asciidoc.contains("🟠") || asciidoc.contains("🔴"))
    }
    
    @Test
    fun `should handle empty results`() {
        val results = createMockResults(passed = 0, failed = 0, errors = 0)
        val metadata = createMockMetadata()
        
        val asciidoc = exporter.export(results, metadata).getOrThrow()
        
        // Should still generate valid document
        assertTrue(asciidoc.contains("= AsciiDoc Konvert - TCK Certification Results"))
        assertTrue(asciidoc.contains("== Summary"))
        assertTrue(asciidoc.contains("| Total Tests | 0"))
    }
    
    @Test
    fun `should handle all passing tests`() {
        val results = createMockResults(passed = 10, failed = 0, errors = 0)
        val metadata = createMockMetadata()
        
        val asciidoc = exporter.export(results, metadata).getOrThrow()
        
        // Should not include failed tests section when there are no failures
        assertTrue(!asciidoc.contains("== Failed Tests") || !asciidoc.contains("WARNING: The following tests failed"))
        
        // Should show 100% pass rate
        assertTrue(asciidoc.contains("| Passed | 10 (100.0%)"))
    }
    
    @Test
    fun `should handle all failing tests`() {
        val results = createMockResults(passed = 0, failed = 10, errors = 0)
        val metadata = createMockMetadata()
        
        val asciidoc = exporter.export(results, metadata).getOrThrow()
        
        // Should include failed tests section
        assertTrue(asciidoc.contains("== Failed Tests"))
        
        // Should show 0% pass rate
        assertTrue(asciidoc.contains("| Passed | 0 (0.0%)"))
    }
    
    @Test
    fun `should sanitize special characters in test names`() {
        val failedTest = TestExecutionResult(
            fixtureId = "test/with|pipe|characters",
            status = TestStatus.FAILED,
            platform = "JVM",
            durationMs = 25,
            category = FixtureCategory.INLINE_BOLD,
            errorMessage = "Error with | pipe characters"
        )
        
        val results = AggregatedResults(
            totalTests = 1,
            passed = 0,
            failed = 1,
            skipped = 0,
            pending = 0,
            errors = 0,
            byPlatform = emptyMap(),
            byCategory = emptyMap(),
            bySource = emptyMap(),
            failedTests = listOf(failedTest),
            pendingTests = emptyList()
        )
        
        val metadata = createMockMetadata()
        val asciidoc = exporter.export(results, metadata).getOrThrow()
        
        // Verify pipe characters are escaped
        assertTrue(asciidoc.contains("\\|"))
    }
    
    @Test
    fun `should include certification status`() {
        // Test different pass rates
        val highPassRate = createMockResults(passed = 100, failed = 0, errors = 0)
        val mediumPassRate = createMockResults(passed = 80, failed = 20, errors = 0)
        val lowPassRate = createMockResults(passed = 50, failed = 50, errors = 0)
        
        val metadata = createMockMetadata()
        
        val highAsciidoc = exporter.export(highPassRate, metadata).getOrThrow()
        assertTrue(highAsciidoc.contains("Certification Status"))
        assertTrue(highAsciidoc.contains("Ready for Certification") || highAsciidoc.contains("Near Certification"))
        
        val mediumAsciidoc = exporter.export(mediumPassRate, metadata).getOrThrow()
        assertTrue(mediumAsciidoc.contains("Certification Status"))
        
        val lowAsciidoc = exporter.export(lowPassRate, metadata).getOrThrow()
        assertTrue(lowAsciidoc.contains("Certification Status"))
    }
}
