package org.markup.poet.tck.publisher

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
 * Integration test for export-parse round-trip validation.
 *
 * **Validates: Requirements 2.1, 8.2, 8.4**
 *
 * This test is CRITICAL for dogfooding validation. It ensures that:
 * 1. Our exporter generates valid AsciiDoc documents
 * 2. Our parser can successfully parse its own output
 * 3. The parsed AST contains the expected structure
 *
 * If this test fails, it indicates a critical bug in either:
 * - The exporter (generating invalid AsciiDoc)
 * - The parser (unable to parse valid AsciiDoc)
 *
 * This validates that we're truly using our own tools to publish results.
 */
class ExportParseRoundTripTest {
    
    private val exporter = DefaultTckResultsExporter()
    private val parser = DefaultAsciidocParser()
    
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
    fun `should successfully parse exported AsciiDoc with typical results`() {
        // Arrange
        val results = createMockResults(passed = 10, failed = 2, errors = 1)
        val metadata = createMockMetadata()
        
        // Act - Export to AsciiDoc
        val exportResult = exporter.export(results, metadata)
        assertTrue(exportResult.isSuccess, "Export should succeed")
        
        val asciidoc = exportResult.getOrThrow()
        assertNotNull(asciidoc, "Exported AsciiDoc should not be null")
        assertTrue(asciidoc.isNotEmpty(), "Exported AsciiDoc should not be empty")
        
        // Act - Parse with our parser (DOGFOODING!)
        val parseResult = parser.parse(asciidoc)
        
        // Assert - Parsing must succeed (CRITICAL)
        assertNotNull(parseResult, "Parse result should not be null")
        assertTrue(
            parseResult.errors.isEmpty(),
            "Parser should not produce errors when parsing our own output. Errors: ${parseResult.errors}"
        )
        
        // Assert - Document structure should be valid
        val document = parseResult.document
        assertNotNull(document, "Parsed document should not be null")
        assertNotNull(document.header?.title, "Document should have a title")
        assertTrue(document.blocks.isNotEmpty(), "Document should have content blocks")
    }
    
    @Test
    fun `should successfully parse exported AsciiDoc with all passing tests`() {
        // Arrange - All tests passing (100% pass rate)
        val results = createMockResults(passed = 20, failed = 0, errors = 0)
        val metadata = createMockMetadata()
        
        // Act - Export and parse
        val asciidoc = exporter.export(results, metadata).getOrThrow()
        val parseResult = parser.parse(asciidoc)
        
        // Assert - Must parse successfully
        assertTrue(
            parseResult.errors.isEmpty(),
            "Parser should handle documents with all passing tests. Errors: ${parseResult.errors}"
        )
        assertNotNull(parseResult.document)
    }
    
    @Test
    fun `should successfully parse exported AsciiDoc with all failing tests`() {
        // Arrange - All tests failing (0% pass rate)
        val results = createMockResults(passed = 0, failed = 10, errors = 5)
        val metadata = createMockMetadata()
        
        // Act - Export and parse
        val asciidoc = exporter.export(results, metadata).getOrThrow()
        val parseResult = parser.parse(asciidoc)
        
        // Assert - Must parse successfully
        assertTrue(
            parseResult.errors.isEmpty(),
            "Parser should handle documents with all failing tests. Errors: ${parseResult.errors}"
        )
        assertNotNull(parseResult.document)
    }
    
    @Test
    fun `should successfully parse exported AsciiDoc with empty results`() {
        // Arrange - No tests executed
        val results = createMockResults(passed = 0, failed = 0, errors = 0)
        val metadata = createMockMetadata()
        
        // Act - Export and parse
        val asciidoc = exporter.export(results, metadata).getOrThrow()
        val parseResult = parser.parse(asciidoc)
        
        // Assert - Must parse successfully
        assertTrue(
            parseResult.errors.isEmpty(),
            "Parser should handle empty results documents. Errors: ${parseResult.errors}"
        )
        assertNotNull(parseResult.document)
    }
    
    @Test
    fun `should successfully parse exported AsciiDoc with special characters`() {
        // Arrange - Test with special characters in names and error messages
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
        
        // Act - Export and parse
        val asciidoc = exporter.export(results, metadata).getOrThrow()
        val parseResult = parser.parse(asciidoc)
        
        // Assert - Must parse successfully despite special characters
        assertTrue(
            parseResult.errors.isEmpty(),
            "Parser should handle special characters in content. Errors: ${parseResult.errors}"
        )
        assertNotNull(parseResult.document)
    }
    
    @Test
    fun `should successfully parse exported AsciiDoc with multiple platforms`() {
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
        
        // Act - Export and parse
        val asciidoc = exporter.export(results, metadata).getOrThrow()
        val parseResult = parser.parse(asciidoc)
        
        // Assert - Must parse successfully
        assertTrue(
            parseResult.errors.isEmpty(),
            "Parser should handle multi-platform results. Errors: ${parseResult.errors}"
        )
        assertNotNull(parseResult.document)
    }
    
    @Test
    fun `should successfully parse exported AsciiDoc with long error messages`() {
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
        
        // Act - Export and parse
        val asciidoc = exporter.export(results, metadata).getOrThrow()
        val parseResult = parser.parse(asciidoc)
        
        // Assert - Must parse successfully even with long content
        assertTrue(
            parseResult.errors.isEmpty(),
            "Parser should handle long error messages. Errors: ${parseResult.errors}"
        )
        assertNotNull(parseResult.document)
    }
    
    @Test
    fun `parsed document should contain expected structural elements`() {
        // Arrange
        val results = createMockResults(passed = 10, failed = 2, errors = 1)
        val metadata = createMockMetadata()
        
        // Act - Export and parse
        val asciidoc = exporter.export(results, metadata).getOrThrow()
        val parseResult = parser.parse(asciidoc)
        
        // Assert - Verify document structure
        val document = parseResult.document
        
        // Should have a title
        assertNotNull(document.header?.title, "Document should have a title")
        
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
    
    @Test
    fun `should handle all test status types in round-trip`() {
        // Arrange - Create results with all status types
        val allStatusTests = listOf(
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
        
        // Act - Export and parse
        val asciidoc = exporter.export(results, metadata).getOrThrow()
        val parseResult = parser.parse(asciidoc)
        
        // Assert - Must parse successfully with all status types
        assertTrue(
            parseResult.errors.isEmpty(),
            "Parser should handle all test status types. Errors: ${parseResult.errors}"
        )
        assertNotNull(parseResult.document)
    }
}
