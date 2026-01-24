package org.markup.poet.tck.conformance

import org.markup.poet.tck.execution.*
import org.markup.poet.tck.fixtures.FixtureCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

class ReportGeneratorTest {
    
    private fun createMockResults(): AggregatedResults {
        val results = listOf(
            TestExecutionResult("test-1", TestStatus.PASSED, "JVM", 100, FixtureCategory.BLOCK_PARAGRAPH, "custom"),
            TestExecutionResult("test-2", TestStatus.FAILED, "JVM", 150, FixtureCategory.BLOCK_PARAGRAPH, "official-tck", errorMessage = "Output mismatch"),
            TestExecutionResult("test-3", TestStatus.PASSED, "iOS", 120, FixtureCategory.BLOCK_HEADING, "custom"),
            TestExecutionResult("test-4", TestStatus.PENDING, "iOS", 80, FixtureCategory.BLOCK_TABLE, "official-tck", errorMessage = "Not implemented"),
            TestExecutionResult("test-5", TestStatus.PASSED, "Linux", 110, FixtureCategory.INLINE_BOLD, "custom")
        )
        
        val aggregator = DefaultResultAggregator()
        return aggregator.aggregate(results)
    }
    
    private fun createMockMetadata(): ReportMetadata {
        return ReportMetadata(
            generatedAt = 1706140800000L,
            specVersion = "1.0.0",
            tckCommitHash = "abc123def456",
            libraryVersion = "0.1.0",
            platforms = listOf("JVM", "iOS", "Linux")
        )
    }
    
    @Test
    fun `should generate complete conformance report`() {
        val checker = DefaultCertificationChecker()
        val generator = DefaultReportGenerator(checker)
        val results = createMockResults()
        val metadata = createMockMetadata()
        
        val report = generator.generateReport(results, metadata)
        
        assertNotNull(report)
        assertEquals(metadata, report.metadata)
        assertNotNull(report.summary)
        assertNotNull(report.platformResults)
        assertNotNull(report.categoryResults)
        assertNotNull(report.failedTests)
        assertNotNull(report.pendingTests)
        assertNotNull(report.certificationStatus)
    }
    
    @Test
    fun `should build correct summary`() {
        val checker = DefaultCertificationChecker()
        val generator = DefaultReportGenerator(checker)
        val results = createMockResults()
        val metadata = createMockMetadata()
        
        val report = generator.generateReport(results, metadata)
        val summary = report.summary
        
        assertEquals(5, summary.totalTests)
        assertEquals(3, summary.passed)
        assertEquals(1, summary.failed)
        assertEquals(1, summary.pending)
        assertEquals(0, summary.skipped)
        assertEquals(0.6, summary.overallPassRate)
    }
    
    @Test
    fun `should build platform results`() {
        val checker = DefaultCertificationChecker()
        val generator = DefaultReportGenerator(checker)
        val results = createMockResults()
        val metadata = createMockMetadata()
        
        val report = generator.generateReport(results, metadata)
        
        assertEquals(3, report.platformResults.size)
        
        val jvmResults = report.platformResults.find { it.platform == "JVM" }
        assertNotNull(jvmResults)
        assertEquals(2, jvmResults.totalTests)
        assertEquals(1, jvmResults.passed)
        assertEquals(1, jvmResults.failed)
        assertEquals(0.5, jvmResults.passRate)
    }
    
    @Test
    fun `should build category results`() {
        val checker = DefaultCertificationChecker()
        val generator = DefaultReportGenerator(checker)
        val results = createMockResults()
        val metadata = createMockMetadata()
        
        val report = generator.generateReport(results, metadata)
        
        assertTrue(report.categoryResults.isNotEmpty())
        
        val paragraphResults = report.categoryResults.find { it.category == "BLOCK_PARAGRAPH" }
        assertNotNull(paragraphResults)
        assertEquals(2, paragraphResults.totalTests)
        assertEquals(1, paragraphResults.passed)
        assertEquals(1, paragraphResults.failed)
    }
    
    @Test
    fun `should build failed test details`() {
        val checker = DefaultCertificationChecker()
        val generator = DefaultReportGenerator(checker)
        val results = createMockResults()
        val metadata = createMockMetadata()
        
        val report = generator.generateReport(results, metadata)
        
        assertEquals(1, report.failedTests.size)
        
        val failedTest = report.failedTests.first()
        assertEquals("test-2", failedTest.testId)
        assertEquals("BLOCK_PARAGRAPH", failedTest.category)
        assertEquals("Output mismatch", failedTest.errorMessage)
        assertTrue(failedTest.platforms.contains("JVM"))
    }
    
    @Test
    fun `should build pending test details`() {
        val checker = DefaultCertificationChecker()
        val generator = DefaultReportGenerator(checker)
        val results = createMockResults()
        val metadata = createMockMetadata()
        
        val report = generator.generateReport(results, metadata)
        
        assertEquals(1, report.pendingTests.size)
        
        val pendingTest = report.pendingTests.first()
        assertEquals("test-4", pendingTest.testId)
        assertEquals("BLOCK_TABLE", pendingTest.category)
        assertEquals("Not implemented", pendingTest.reason)
    }
    
    @Test
    fun `should handle empty results`() {
        val checker = DefaultCertificationChecker()
        val generator = DefaultReportGenerator(checker)
        val aggregator = DefaultResultAggregator()
        val results = aggregator.aggregate(emptyList())
        val metadata = createMockMetadata()
        
        val report = generator.generateReport(results, metadata)
        
        assertEquals(0, report.summary.totalTests)
        assertTrue(report.platformResults.isEmpty())
        assertTrue(report.categoryResults.isEmpty())
        assertTrue(report.failedTests.isEmpty())
        assertTrue(report.pendingTests.isEmpty())
    }
    
    @Test
    fun `should calculate pass rates by source`() {
        val checker = DefaultCertificationChecker()
        val generator = DefaultReportGenerator(checker)
        val results = createMockResults()
        val metadata = createMockMetadata()
        
        val report = generator.generateReport(results, metadata)
        val summary = report.summary
        
        // Custom tests: 3 passed out of 3 = 100%
        assertEquals(1.0, summary.customTestsPassRate)
        
        // Official tests: 0 passed out of 2 = 0%
        assertEquals(0.0, summary.officialTestsPassRate)
    }
}
