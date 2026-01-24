package org.markup.poet.tck.conformance

import org.markup.poet.tck.execution.*
import org.markup.poet.tck.fixtures.FixtureCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class CertificationCheckerTest {
    
    private fun createPassingResults(): AggregatedResults {
        val results = (1..100).map { i ->
            TestExecutionResult(
                "test-$i",
                TestStatus.PASSED,
                "JVM",
                100,
                FixtureCategory.BLOCK_PARAGRAPH,
                "official-tck"
            )
        }
        
        val aggregator = DefaultResultAggregator()
        return aggregator.aggregate(results)
    }
    
    private fun createFailingResults(): AggregatedResults {
        val results = (1..50).map { i ->
            TestExecutionResult(
                "test-$i",
                TestStatus.PASSED,
                "JVM",
                100,
                FixtureCategory.BLOCK_PARAGRAPH,
                "official-tck"
            )
        } + (51..100).map { i ->
            TestExecutionResult(
                "test-$i",
                TestStatus.FAILED,
                "JVM",
                100,
                FixtureCategory.BLOCK_PARAGRAPH,
                "official-tck",
                errorMessage = "Test failed"
            )
        }
        
        val aggregator = DefaultResultAggregator()
        return aggregator.aggregate(results)
    }
    
    private fun createMixedResults(): AggregatedResults {
        val results = listOf(
            TestExecutionResult("test-1", TestStatus.PASSED, "JVM", 100, FixtureCategory.BLOCK_PARAGRAPH, "official-tck"),
            TestExecutionResult("test-2", TestStatus.PASSED, "JVM", 100, FixtureCategory.BLOCK_PARAGRAPH, "official-tck"),
            TestExecutionResult("test-3", TestStatus.FAILED, "JVM", 100, FixtureCategory.BLOCK_PARAGRAPH, "official-tck", errorMessage = "Failed"),
            TestExecutionResult("test-4", TestStatus.PENDING, "iOS", 100, FixtureCategory.BLOCK_TABLE, "official-tck", errorMessage = "Pending"),
            TestExecutionResult("test-5", TestStatus.PASSED, "iOS", 100, FixtureCategory.INLINE_BOLD, "custom")
        )
        
        val aggregator = DefaultResultAggregator()
        return aggregator.aggregate(results)
    }
    
    @Test
    fun `should mark as ready when all tests pass`() {
        val checker = DefaultCertificationChecker()
        val results = createPassingResults()
        
        val status = checker.checkStatus(results)
        
        assertTrue(status.isReady)
        assertEquals(100.0, status.overallProgress)
        assertTrue(status.blockingIssues.isEmpty())
    }
    
    @Test
    fun `should mark as not ready when tests fail`() {
        val checker = DefaultCertificationChecker()
        val results = createFailingResults()
        
        val status = checker.checkStatus(results)
        
        assertFalse(status.isReady)
        assertTrue(status.overallProgress < 100.0)
        assertTrue(status.blockingIssues.isNotEmpty())
    }
    
    @Test
    fun `should identify critical issue for failing official tests`() {
        val checker = DefaultCertificationChecker()
        val results = createFailingResults()
        
        val status = checker.checkStatus(results)
        
        val criticalIssues = status.blockingIssues.filter { it.severity == IssueSeverity.CRITICAL }
        assertTrue(criticalIssues.isNotEmpty())
        
        val officialTestIssue = criticalIssues.find { it.description.contains("Official TCK") }
        assertNotNull(officialTestIssue)
    }
    
    @Test
    fun `should identify high severity issue for low pass rate`() {
        val checker = DefaultCertificationChecker()
        val results = createFailingResults()
        
        val status = checker.checkStatus(results)
        
        val highIssues = status.blockingIssues.filter { it.severity == IssueSeverity.HIGH }
        assertTrue(highIssues.isNotEmpty())
    }
    
    @Test
    fun `should identify medium severity issue for many pending tests`() {
        val checker = DefaultCertificationChecker()
        
        // Create results with >10% pending
        val results = (1..80).map { i ->
            TestExecutionResult("test-$i", TestStatus.PASSED, "JVM", 100, FixtureCategory.BLOCK_PARAGRAPH, "custom")
        } + (81..100).map { i ->
            TestExecutionResult("test-$i", TestStatus.PENDING, "JVM", 100, FixtureCategory.BLOCK_TABLE, "custom", errorMessage = "Pending")
        }
        
        val aggregator = DefaultResultAggregator()
        val aggregated = aggregator.aggregate(results)
        
        val status = checker.checkStatus(aggregated)
        
        val mediumIssues = status.blockingIssues.filter { it.severity == IssueSeverity.MEDIUM }
        assertTrue(mediumIssues.isNotEmpty())
        
        val pendingIssue = mediumIssues.find { it.description.contains("pending") }
        assertNotNull(pendingIssue)
    }
    
    @Test
    fun `should calculate progress correctly`() {
        val checker = DefaultCertificationChecker()
        val results = createMixedResults()
        
        val status = checker.checkStatus(results)
        
        // Progress should be between 0 and 100
        assertTrue(status.overallProgress >= 0.0)
        assertTrue(status.overallProgress <= 100.0)
    }
    
    @Test
    fun `should generate recommendations for failing tests`() {
        val checker = DefaultCertificationChecker()
        val results = createFailingResults()
        
        val status = checker.checkStatus(results)
        
        assertTrue(status.recommendations.isNotEmpty())
        assertTrue(status.recommendations.any { it.contains("failing") || it.contains("fix") })
    }
    
    @Test
    fun `should generate recommendations for pending tests`() {
        val checker = DefaultCertificationChecker()
        val results = createMixedResults()
        
        val status = checker.checkStatus(results)
        
        assertTrue(status.recommendations.any { it.contains("pending") || it.contains("Implement") })
    }
    
    @Test
    fun `should recommend certification when ready`() {
        val checker = DefaultCertificationChecker()
        val results = createPassingResults()
        
        val status = checker.checkStatus(results)
        
        assertTrue(status.recommendations.any { it.contains("ready") || it.contains("certification") })
    }
    
    @Test
    fun `should return certification requirements`() {
        val checker = DefaultCertificationChecker()
        
        val requirements = checker.getRequirements()
        
        assertTrue(requirements.isNotEmpty())
        assertTrue(requirements.all { it.id.isNotEmpty() })
        assertTrue(requirements.all { it.description.isNotEmpty() })
        assertTrue(requirements.any { it.required })
    }
    
    @Test
    fun `should identify platform-specific issues`() {
        val checker = DefaultCertificationChecker()
        
        // Create results with one platform failing
        val results = (1..50).map { i ->
            TestExecutionResult("test-$i", TestStatus.PASSED, "JVM", 100, FixtureCategory.BLOCK_PARAGRAPH, "custom")
        } + (51..100).map { i ->
            TestExecutionResult("test-$i", TestStatus.FAILED, "iOS", 100, FixtureCategory.BLOCK_PARAGRAPH, "custom", errorMessage = "Failed")
        }
        
        val aggregator = DefaultResultAggregator()
        val aggregated = aggregator.aggregate(results)
        
        val status = checker.checkStatus(aggregated)
        
        val platformIssues = status.blockingIssues.filter { it.description.contains("Platform") }
        assertTrue(platformIssues.isNotEmpty())
    }
}
