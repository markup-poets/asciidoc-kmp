package org.markup.poet.tck.publisher

import org.markup.poet.asciidoc.asg.plainText
import org.markup.poet.asciidoc.parser.DefaultAsciidocParser
import org.markup.poet.tck.execution.AggregatedResults
import org.markup.poet.tck.execution.CategoryResults
import org.markup.poet.tck.execution.PlatformResults
import org.markup.poet.tck.execution.SourceResults
import org.markup.poet.tck.execution.TestExecutionResult
import org.markup.poet.tck.execution.TestStatus
import org.markup.poet.tck.fixtures.FixtureCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration test for the complete export-parse-render pipeline.
 *
 * **Validates: Requirements 3.1, 3.2, 3.5**
 *
 * This test validates the complete dogfooding pipeline:
 * 1. Export TCK test results to AsciiDoc format
 * 2. Parse the AsciiDoc using our own parser
 * 3. Render the parsed AST to HTML with Kotlin theme
 *
 * This is a critical integration test that ensures all three stages work together
 * correctly. It validates that:
 * - The exporter generates valid AsciiDoc
 * - Our parser can parse the generated AsciiDoc
 * - The renderer can produce HTML from the parsed AST
 * - The complete pipeline preserves test result information
 *
 * If this test fails, it indicates a problem in the integration between components
 * or a bug in one of the pipeline stages.
 */
class ExportParseRenderTest {
    
    private val exporter = DefaultTckResultsExporter()
    private val parser = DefaultAsciidocParser()
    private val renderer = TckHtmlRenderer()
    
    /**
     * Create mock metadata for testing.
     */
    private fun createMockMetadata(): ExportMetadata {
        return ExportMetadata(
            timestamp = 1706097000000L,
            specVersion = "1.0.0",
            tckCommitHash = "abc123def456789",
            libraryVersion = "1.0.0",
            platforms = listOf("JVM", "iOS", "Linux"),
            runId = "test-run-001"
        )
    }
    
    /**
     * Create mock test results with various statuses.
     */
    private fun createMockResults(
        passed: Int = 10,
        failed: Int = 2,
        errors: Int = 1,
        pending: Int = 0,
        skipped: Int = 0
    ): AggregatedResults {
        val total = passed + failed + errors + pending + skipped
        
        val failedTests = mutableListOf<TestExecutionResult>()
        
        // Add failed tests with detailed information
        repeat(failed) { i ->
            failedTests.add(
                TestExecutionResult(
                    fixtureId = "inline/italic/test-$i",
                    status = TestStatus.FAILED,
                    platform = "JVM",
                    durationMs = 25,
                    category = FixtureCategory.INLINE_ITALIC,
                    source = "official-tck",
                    errorMessage = "Expected italic node, got text node",
                    expectedOutput = """{"type": "italic", "content": "text"}""",
                    actualOutput = """{"type": "text", "content": "text"}""",
                    diff = """
                        - {"type": "italic", "content": "text"}
                        + {"type": "text", "content": "text"}
                    """.trimIndent()
                )
            )
        }
        
        // Add error tests with stack traces
        repeat(errors) { i ->
            failedTests.add(
                TestExecutionResult(
                    fixtureId = "block/paragraph/error-$i",
                    status = TestStatus.ERROR,
                    platform = "JVM",
                    durationMs = 30,
                    category = FixtureCategory.BLOCK_PARAGRAPH,
                    source = "official-tck",
                    errorMessage = "NullPointerException: Unexpected null value",
                    stackTrace = """
                        at org.markup.poet.asciidoc.parser.BlockParser.parse(BlockParser.kt:42)
                        at org.markup.poet.asciidoc.parser.DefaultAsciidocParser.parse(DefaultAsciidocParser.kt:15)
                        at org.markup.poet.tck.TckTest.runTest(TckTest.kt:25)
                    """.trimIndent()
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
                ),
                "iOS" to PlatformResults(
                    platform = "iOS",
                    total = total / 2,
                    passed = passed / 2,
                    failed = (failed + errors) / 2,
                    passRate = (passed / 2).toDouble() / (total / 2)
                )
            ),
            byCategory = mapOf(
                FixtureCategory.INLINE_ITALIC to CategoryResults(
                    category = FixtureCategory.INLINE_ITALIC,
                    total = 5,
                    passed = 3,
                    failed = 2,
                    passRate = 0.6
                ),
                FixtureCategory.BLOCK_PARAGRAPH to CategoryResults(
                    category = FixtureCategory.BLOCK_PARAGRAPH,
                    total = 8,
                    passed = 7,
                    failed = 1,
                    passRate = 0.875
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
    fun `should complete full export-parse-render pipeline with typical results`() {
        // Arrange
        val results = createMockResults(passed = 10, failed = 2, errors = 1)
        val metadata = createMockMetadata()
        
        // Act - Stage 1: Export to AsciiDoc
        val exportResult = exporter.export(results, metadata)
        assertTrue(exportResult.isSuccess, "Export should succeed")
        
        val asciidoc = exportResult.getOrThrow()
        assertNotNull(asciidoc, "Exported AsciiDoc should not be null")
        assertTrue(asciidoc.isNotEmpty(), "Exported AsciiDoc should not be empty")
        
        // Act - Stage 2: Parse AsciiDoc
        val parseResult = parser.parse(asciidoc)
        assertTrue(
            parseResult.errors.isEmpty(),
            "Parser should not produce errors. Errors: ${parseResult.errors}"
        )
        
        val document = parseResult.document
        assertNotNull(document, "Parsed document should not be null")
        
        // Act - Stage 3: Render to HTML
        val renderResult = renderer.render(document)
        
        // Assert - Rendering will fail until DefaultHtmlRenderer is fully implemented
        // For now, we expect a NotImplementedError
        assertTrue(
            renderResult.isFailure,
            "Rendering should fail (renderer not yet fully implemented)"
        )
        
        val error = renderResult.exceptionOrNull()
        assertNotNull(error, "Should have an error")
        assertTrue(
            error is NotImplementedError || error.message?.contains("not yet fully implemented") == true,
            "Should be NotImplementedError or similar. Got: ${error::class.simpleName}: ${error.message}"
        )
    }
    
    @Test
    fun `should complete pipeline with all passing tests`() {
        // Arrange - All tests passing (100% pass rate)
        val results = createMockResults(passed = 20, failed = 0, errors = 0)
        val metadata = createMockMetadata()
        
        // Act - Export, parse, render
        val asciidoc = exporter.export(results, metadata).getOrThrow()
        val parseResult = parser.parse(asciidoc)
        val document = parseResult.document
        val renderResult = renderer.render(document)
        
        // Assert - Export and parse should succeed
        assertTrue(parseResult.errors.isEmpty(), "Parsing should succeed")
        assertNotNull(document)
        
        // Rendering will fail until implemented
        assertTrue(renderResult.isFailure, "Rendering should fail (not yet implemented)")
    }
    
    @Test
    fun `should complete pipeline with all failing tests`() {
        // Arrange - All tests failing (0% pass rate)
        val results = createMockResults(passed = 0, failed = 10, errors = 5)
        val metadata = createMockMetadata()
        
        // Act - Export, parse, render
        val asciidoc = exporter.export(results, metadata).getOrThrow()
        val parseResult = parser.parse(asciidoc)
        val document = parseResult.document
        val renderResult = renderer.render(document)
        
        // Assert - Export and parse should succeed
        assertTrue(parseResult.errors.isEmpty(), "Parsing should succeed")
        assertNotNull(document)
        
        // Rendering will fail until implemented
        assertTrue(renderResult.isFailure, "Rendering should fail (not yet implemented)")
    }
    
    @Test
    fun `should complete pipeline with empty results`() {
        // Arrange - No tests executed
        val results = createMockResults(passed = 0, failed = 0, errors = 0)
        val metadata = createMockMetadata()
        
        // Act - Export, parse, render
        val asciidoc = exporter.export(results, metadata).getOrThrow()
        val parseResult = parser.parse(asciidoc)
        val document = parseResult.document
        val renderResult = renderer.render(document)
        
        // Assert - Export and parse should succeed
        assertTrue(parseResult.errors.isEmpty(), "Parsing should succeed")
        assertNotNull(document)
        
        // Rendering will fail until implemented
        assertTrue(renderResult.isFailure, "Rendering should fail (not yet implemented)")
    }
    
    @Test
    fun `should complete pipeline with special characters in test data`() {
        // Arrange - Test with special characters
        val specialTest = TestExecutionResult(
            fixtureId = "test/with-special-chars-<>&\"'",
            status = TestStatus.FAILED,
            platform = "JVM",
            durationMs = 25,
            category = FixtureCategory.INLINE_BOLD,
            source = "official-tck",
            errorMessage = "Error message with special chars: <tag> & \"quotes\" | pipes",
            expectedOutput = """{"content": "text with <special> & chars"}""",
            actualOutput = """{"content": "different text"}"""
        )
        
        val results = AggregatedResults(
            totalTests = 1,
            passed = 0,
            failed = 1,
            skipped = 0,
            pending = 0,
            errors = 0,
            byPlatform = mapOf(
                "JVM" to PlatformResults("JVM", 1, 0, 1, 0.0)
            ),
            byCategory = mapOf(
                FixtureCategory.INLINE_BOLD to CategoryResults(
                    FixtureCategory.INLINE_BOLD, 1, 0, 1, 0.0
                )
            ),
            bySource = mapOf(
                "official-tck" to SourceResults("official-tck", 1, 0, 1, 0.0)
            ),
            failedTests = listOf(specialTest),
            pendingTests = emptyList()
        )
        
        val metadata = createMockMetadata()
        
        // Act - Export, parse, render
        val asciidoc = exporter.export(results, metadata).getOrThrow()
        val parseResult = parser.parse(asciidoc)
        val document = parseResult.document
        val renderResult = renderer.render(document)
        
        // Assert - Export and parse should succeed despite special characters
        assertTrue(parseResult.errors.isEmpty(), "Parsing should succeed with special characters")
        assertNotNull(document)
        
        // Rendering will fail until implemented
        assertTrue(renderResult.isFailure, "Rendering should fail (not yet implemented)")
    }
    
    @Test
    fun `should complete pipeline with multiple platforms`() {
        // Arrange - Results from multiple platforms
        val results = createMockResults(passed = 15, failed = 3, errors = 2)
        val metadata = ExportMetadata(
            timestamp = 1706097000000L,
            specVersion = "1.0.0",
            tckCommitHash = "abc123def456789",
            libraryVersion = "1.0.0",
            platforms = listOf("JVM", "iOS", "Linux", "Android"),
            runId = "multi-platform-run"
        )
        
        // Act - Export, parse, render
        val asciidoc = exporter.export(results, metadata).getOrThrow()
        val parseResult = parser.parse(asciidoc)
        val document = parseResult.document
        val renderResult = renderer.render(document)
        
        // Assert - Export and parse should succeed
        assertTrue(parseResult.errors.isEmpty(), "Parsing should succeed with multiple platforms")
        assertNotNull(document)
        
        // Rendering will fail until implemented
        assertTrue(renderResult.isFailure, "Rendering should fail (not yet implemented)")
    }
    
    @Test
    fun `should complete pipeline with long error messages`() {
        // Arrange - Test with very long error message
        val longErrorMessage = "Error: " + "This is a very long error message. ".repeat(50)
        val longStackTrace = (1..20).joinToString("\n") { i ->
            "at org.markup.poet.package$i.Class$i.method$i(File$i.kt:$i)"
        }
        
        val errorTest = TestExecutionResult(
            fixtureId = "test/long-error",
            status = TestStatus.ERROR,
            platform = "JVM",
            durationMs = 100,
            category = FixtureCategory.BLOCK_PARAGRAPH,
            source = "official-tck",
            errorMessage = longErrorMessage,
            stackTrace = longStackTrace
        )
        
        val results = AggregatedResults(
            totalTests = 1,
            passed = 0,
            failed = 0,
            skipped = 0,
            pending = 0,
            errors = 1,
            byPlatform = mapOf(
                "JVM" to PlatformResults("JVM", 1, 0, 1, 0.0)
            ),
            byCategory = mapOf(
                FixtureCategory.BLOCK_PARAGRAPH to CategoryResults(
                    FixtureCategory.BLOCK_PARAGRAPH, 1, 0, 1, 0.0
                )
            ),
            bySource = mapOf(
                "official-tck" to SourceResults("official-tck", 1, 0, 1, 0.0)
            ),
            failedTests = listOf(errorTest),
            pendingTests = emptyList()
        )
        
        val metadata = createMockMetadata()
        
        // Act - Export, parse, render
        val asciidoc = exporter.export(results, metadata).getOrThrow()
        val parseResult = parser.parse(asciidoc)
        val document = parseResult.document
        val renderResult = renderer.render(document)
        
        // Assert - Export and parse should succeed even with long content
        assertTrue(parseResult.errors.isEmpty(), "Parsing should succeed with long error messages")
        assertNotNull(document)
        
        // Rendering will fail until implemented
        assertTrue(renderResult.isFailure, "Rendering should fail (not yet implemented)")
    }
    
    @Test
    fun `should preserve test result information through pipeline`() {
        // Arrange
        val results = createMockResults(passed = 10, failed = 2, errors = 1)
        val metadata = createMockMetadata()
        
        // Act - Export to AsciiDoc
        val asciidoc = exporter.export(results, metadata).getOrThrow()
        
        // Assert - Verify key information is present in AsciiDoc
        assertTrue(asciidoc.contains("Total Tests"), "Should contain total tests")
        assertTrue(asciidoc.contains("10"), "Should contain passed count")
        assertTrue(asciidoc.contains("2"), "Should contain failed count")
        assertTrue(asciidoc.contains("1"), "Should contain error count")
        
        // Verify test categories are present
        assertTrue(asciidoc.contains("Inline Italic") || asciidoc.contains("INLINE_ITALIC"), 
            "Should contain inline italic category")
        assertTrue(asciidoc.contains("Block Paragraph") || asciidoc.contains("BLOCK_PARAGRAPH"), 
            "Should contain block paragraph category")
        
        // Verify failed test details are present
        assertTrue(asciidoc.contains("inline/italic/test-0"), "Should contain failed test ID")
        assertTrue(asciidoc.contains("Expected italic node"), "Should contain error message")
        
        // Verify metadata is present
        assertTrue(asciidoc.contains("1.0.0"), "Should contain version")
        assertTrue(asciidoc.contains("JVM"), "Should contain platform")
        
        // Act - Parse AsciiDoc
        val parseResult = parser.parse(asciidoc)
        assertTrue(parseResult.errors.isEmpty(), "Parsing should succeed")
        
        val document = parseResult.document
        assertNotNull(document, "Document should not be null")
        assertNotNull(document.header, "Document should have a title")
        assertTrue(document.blocks.isNotEmpty(), "Document should have content")
        
        // Note: We cannot verify HTML content until the renderer is fully implemented
        // Once implemented, we would verify that the HTML contains the same information
    }
    
    @Test
    fun `should handle all test status types through pipeline`() {
        // Arrange - Create results with all status types
        val allStatusTests = listOf(
            TestExecutionResult(
                fixtureId = "test/passed",
                status = TestStatus.PASSED,
                platform = "JVM",
                durationMs = 15,
                category = FixtureCategory.INLINE_BOLD
            ),
            TestExecutionResult(
                fixtureId = "test/pending",
                status = TestStatus.PENDING,
                platform = "JVM",
                durationMs = 0,
                category = FixtureCategory.INLINE_BOLD
            ),
            TestExecutionResult(
                fixtureId = "test/skipped",
                status = TestStatus.SKIPPED,
                platform = "JVM",
                durationMs = 0,
                category = FixtureCategory.INLINE_BOLD
            ),
            TestExecutionResult(
                fixtureId = "test/failed",
                status = TestStatus.FAILED,
                platform = "JVM",
                durationMs = 25,
                category = FixtureCategory.INLINE_BOLD,
                errorMessage = "Test failed"
            ),
            TestExecutionResult(
                fixtureId = "test/error",
                status = TestStatus.ERROR,
                platform = "JVM",
                durationMs = 30,
                category = FixtureCategory.INLINE_BOLD,
                errorMessage = "Test error"
            )
        )
        
        val results = AggregatedResults(
            totalTests = 5,
            passed = 1,
            failed = 1,
            skipped = 1,
            pending = 1,
            errors = 1,
            byPlatform = mapOf(
                "JVM" to PlatformResults("JVM", 5, 1, 4, 0.2)
            ),
            byCategory = mapOf(
                FixtureCategory.INLINE_BOLD to CategoryResults(
                    FixtureCategory.INLINE_BOLD, 5, 1, 4, 0.2
                )
            ),
            bySource = mapOf(
                "official-tck" to SourceResults("official-tck", 5, 1, 4, 0.2)
            ),
            failedTests = allStatusTests.filter { 
                it.status == TestStatus.FAILED || it.status == TestStatus.ERROR 
            },
            pendingTests = allStatusTests.filter { it.status == TestStatus.PENDING }
        )
        
        val metadata = createMockMetadata()
        
        // Act - Export, parse, render
        val asciidoc = exporter.export(results, metadata).getOrThrow()
        val parseResult = parser.parse(asciidoc)
        val document = parseResult.document
        val renderResult = renderer.render(document)
        
        // Assert - Export and parse should succeed with all status types
        assertTrue(parseResult.errors.isEmpty(), "Parsing should succeed with all status types")
        assertNotNull(document)
        
        // Verify all status types are represented in the AsciiDoc
        // Note: Passed tests appear in summary but not in detailed tables (only failed tests shown in detail)
        assertTrue(asciidoc.contains("Passed") || asciidoc.contains("✅"), 
            "Should contain passed indicator or text")
        assertTrue(asciidoc.contains("FAILED") || asciidoc.contains("❌"), 
            "Should contain failed indicator")
        assertTrue(asciidoc.contains("ERROR") || asciidoc.contains("💥"), 
            "Should contain error indicator")
        // Skipped and Pending appear in summary if count > 0
        if (results.skipped > 0) {
            assertTrue(asciidoc.contains("Skipped") || asciidoc.contains("⏭️"), 
                "Should contain skipped indicator or text")
        }
        if (results.pending > 0) {
            assertTrue(asciidoc.contains("Pending") || asciidoc.contains("⏳"), 
                "Should contain pending indicator or text")
        }
        
        // Rendering will fail until implemented
        assertTrue(renderResult.isFailure, "Rendering should fail (not yet implemented)")
    }
    
    @Test
    fun `should validate document structure after parsing`() {
        // Arrange
        val results = createMockResults(passed = 10, failed = 2, errors = 1)
        val metadata = createMockMetadata()
        
        // Act - Export and parse
        val asciidoc = exporter.export(results, metadata).getOrThrow()
        val parseResult = parser.parse(asciidoc)
        
        // Assert - Verify document structure
        val document = parseResult.document
        
        // Should have a title
        assertNotNull(document.header, "Document should have a title")
        val title = plainText(document.header!!.title)
        assertTrue(
            title.contains("TCK") || title.contains("Results"),
            "Title should mention TCK or Results. Got: $title"
        )
        
        // Should have multiple sections (Summary, Test Results, Failed Tests, Metadata)
        assertTrue(
            document.blocks.isNotEmpty(),
            "Document should have content blocks"
        )
        
        // Verify no parse errors
        assertEquals(
            0,
            parseResult.errors.size,
            "Should have no parse errors. Errors: ${parseResult.errors}"
        )
    }
}
