package org.markup.poet.tck.execution

import org.markup.poet.tck.fixtures.FixtureCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ResultAggregatorTest {
    
    private fun createTestResult(
        id: String,
        status: TestStatus,
        platform: String = "JVM",
        category: FixtureCategory? = FixtureCategory.BLOCK_PARAGRAPH,
        source: String? = "custom"
    ): TestExecutionResult {
        return TestExecutionResult(
            fixtureId = id,
            status = status,
            platform = platform,
            durationMs = 100,
            category = category,
            source = source
        )
    }
    
    @Test
    fun `should aggregate basic statistics`() {
        val aggregator = DefaultResultAggregator()
        val results = listOf(
            createTestResult("test-1", TestStatus.PASSED),
            createTestResult("test-2", TestStatus.FAILED),
            createTestResult("test-3", TestStatus.PASSED),
            createTestResult("test-4", TestStatus.PENDING),
            createTestResult("test-5", TestStatus.SKIPPED),
            createTestResult("test-6", TestStatus.ERROR)
        )
        
        val aggregated = aggregator.aggregate(results)
        
        assertEquals(6, aggregated.totalTests)
        assertEquals(2, aggregated.passed)
        assertEquals(1, aggregated.failed)
        assertEquals(1, aggregated.pending)
        assertEquals(1, aggregated.skipped)
        assertEquals(1, aggregated.errors)
    }
    
    @Test
    fun `should calculate pass rate correctly`() {
        val aggregator = DefaultResultAggregator()
        val results = listOf(
            createTestResult("test-1", TestStatus.PASSED),
            createTestResult("test-2", TestStatus.PASSED),
            createTestResult("test-3", TestStatus.PASSED),
            createTestResult("test-4", TestStatus.FAILED)
        )
        
        val aggregated = aggregator.aggregate(results)
        
        assertEquals(0.75, aggregated.passRate())
    }
    
    @Test
    fun `should handle zero tests gracefully`() {
        val aggregator = DefaultResultAggregator()
        val results = emptyList<TestExecutionResult>()
        
        val aggregated = aggregator.aggregate(results)
        
        assertEquals(0, aggregated.totalTests)
        assertEquals(0.0, aggregated.passRate())
    }
    
    @Test
    fun `should aggregate by platform`() {
        val aggregator = DefaultResultAggregator()
        val results = listOf(
            createTestResult("test-1", TestStatus.PASSED, platform = "JVM"),
            createTestResult("test-2", TestStatus.FAILED, platform = "JVM"),
            createTestResult("test-3", TestStatus.PASSED, platform = "iOS"),
            createTestResult("test-4", TestStatus.PASSED, platform = "iOS"),
            createTestResult("test-5", TestStatus.PASSED, platform = "Linux")
        )
        
        val aggregated = aggregator.aggregate(results)
        
        assertEquals(3, aggregated.byPlatform.size)
        
        val jvmResults = aggregated.byPlatform["JVM"]!!
        assertEquals(2, jvmResults.total)
        assertEquals(1, jvmResults.passed)
        assertEquals(1, jvmResults.failed)
        assertEquals(0.5, jvmResults.passRate)
        
        val iosResults = aggregated.byPlatform["iOS"]!!
        assertEquals(2, iosResults.total)
        assertEquals(2, iosResults.passed)
        assertEquals(0, iosResults.failed)
        assertEquals(1.0, iosResults.passRate)
        
        val linuxResults = aggregated.byPlatform["Linux"]!!
        assertEquals(1, linuxResults.total)
        assertEquals(1, linuxResults.passed)
        assertEquals(1.0, linuxResults.passRate)
    }
    
    @Test
    fun `should aggregate by category`() {
        val aggregator = DefaultResultAggregator()
        val results = listOf(
            createTestResult("test-1", TestStatus.PASSED, category = FixtureCategory.BLOCK_PARAGRAPH),
            createTestResult("test-2", TestStatus.FAILED, category = FixtureCategory.BLOCK_PARAGRAPH),
            createTestResult("test-3", TestStatus.PASSED, category = FixtureCategory.BLOCK_HEADING),
            createTestResult("test-4", TestStatus.PASSED, category = FixtureCategory.BLOCK_HEADING),
            createTestResult("test-5", TestStatus.PASSED, category = FixtureCategory.INLINE_BOLD)
        )
        
        val aggregated = aggregator.aggregate(results)
        
        assertEquals(3, aggregated.byCategory.size)
        
        val paragraphResults = aggregated.byCategory[FixtureCategory.BLOCK_PARAGRAPH]!!
        assertEquals(2, paragraphResults.total)
        assertEquals(1, paragraphResults.passed)
        assertEquals(1, paragraphResults.failed)
        assertEquals(0.5, paragraphResults.passRate)
        
        val headingResults = aggregated.byCategory[FixtureCategory.BLOCK_HEADING]!!
        assertEquals(2, headingResults.total)
        assertEquals(2, headingResults.passed)
        assertEquals(1.0, headingResults.passRate)
    }
    
    @Test
    fun `should aggregate by source`() {
        val aggregator = DefaultResultAggregator()
        val results = listOf(
            createTestResult("test-1", TestStatus.PASSED, source = "custom"),
            createTestResult("test-2", TestStatus.FAILED, source = "custom"),
            createTestResult("test-3", TestStatus.PASSED, source = "official-tck"),
            createTestResult("test-4", TestStatus.PASSED, source = "official-tck"),
            createTestResult("test-5", TestStatus.PASSED, source = "official-tck")
        )
        
        val aggregated = aggregator.aggregate(results)
        
        assertEquals(2, aggregated.bySource.size)
        
        val customResults = aggregated.bySource["custom"]!!
        assertEquals(2, customResults.total)
        assertEquals(1, customResults.passed)
        assertEquals(1, customResults.failed)
        assertEquals(0.5, customResults.passRate)
        
        val officialResults = aggregated.bySource["official-tck"]!!
        assertEquals(3, officialResults.total)
        assertEquals(3, officialResults.passed)
        assertEquals(1.0, officialResults.passRate)
    }
    
    @Test
    fun `should collect failed tests`() {
        val aggregator = DefaultResultAggregator()
        val results = listOf(
            createTestResult("test-1", TestStatus.PASSED),
            createTestResult("test-2", TestStatus.FAILED),
            createTestResult("test-3", TestStatus.PASSED),
            createTestResult("test-4", TestStatus.FAILED)
        )
        
        val aggregated = aggregator.aggregate(results)
        
        assertEquals(2, aggregated.failedTests.size)
        assertTrue(aggregated.failedTests.all { it.status == TestStatus.FAILED })
        assertEquals(setOf("test-2", "test-4"), aggregated.failedTests.map { it.fixtureId }.toSet())
    }
    
    @Test
    fun `should collect pending tests`() {
        val aggregator = DefaultResultAggregator()
        val results = listOf(
            createTestResult("test-1", TestStatus.PASSED),
            createTestResult("test-2", TestStatus.PENDING),
            createTestResult("test-3", TestStatus.PASSED),
            createTestResult("test-4", TestStatus.PENDING)
        )
        
        val aggregated = aggregator.aggregate(results)
        
        assertEquals(2, aggregated.pendingTests.size)
        assertTrue(aggregated.pendingTests.all { it.status == TestStatus.PENDING })
        assertEquals(setOf("test-2", "test-4"), aggregated.pendingTests.map { it.fixtureId }.toSet())
    }
    
    @Test
    fun `should generate summary string`() {
        val aggregator = DefaultResultAggregator()
        val results = listOf(
            createTestResult("test-1", TestStatus.PASSED, platform = "JVM"),
            createTestResult("test-2", TestStatus.FAILED, platform = "JVM"),
            createTestResult("test-3", TestStatus.PASSED, platform = "iOS")
        )
        
        val aggregated = aggregator.aggregate(results)
        val summary = aggregated.summary()
        
        assertTrue(summary.contains("Total: 3"))
        assertTrue(summary.contains("Passed: 2"))
        assertTrue(summary.contains("Failed: 1"))
        assertTrue(summary.contains("By Platform:"))
        assertTrue(summary.contains("JVM:"))
        assertTrue(summary.contains("iOS:"))
    }
    
    @Test
    fun `should handle tests without category`() {
        val aggregator = DefaultResultAggregator()
        val results = listOf(
            createTestResult("test-1", TestStatus.PASSED, category = null),
            createTestResult("test-2", TestStatus.PASSED, category = FixtureCategory.BLOCK_PARAGRAPH)
        )
        
        val aggregated = aggregator.aggregate(results)
        
        // Only tests with categories should be in byCategory
        assertEquals(1, aggregated.byCategory.size)
        assertTrue(aggregated.byCategory.containsKey(FixtureCategory.BLOCK_PARAGRAPH))
    }
    
    @Test
    fun `should handle tests without source`() {
        val aggregator = DefaultResultAggregator()
        val results = listOf(
            createTestResult("test-1", TestStatus.PASSED, source = null),
            createTestResult("test-2", TestStatus.PASSED, source = "custom")
        )
        
        val aggregated = aggregator.aggregate(results)
        
        // Tests without source should be treated as "custom"
        assertEquals(1, aggregated.bySource.size)
        val customResults = aggregated.bySource["custom"]!!
        assertEquals(2, customResults.total)
    }
}

class CachingResultAggregatorTest {
    
    private fun createTestResult(id: String, status: TestStatus): TestExecutionResult {
        return TestExecutionResult(
            fixtureId = id,
            status = status,
            platform = "JVM",
            durationMs = 100
        )
    }
    
    @Test
    fun `should cache aggregation results`() {
        val aggregator = CachingResultAggregator()
        val results = listOf(
            createTestResult("test-1", TestStatus.PASSED),
            createTestResult("test-2", TestStatus.FAILED)
        )
        
        val aggregated1 = aggregator.aggregate(results)
        val aggregated2 = aggregator.aggregate(results)
        
        // Should return the same instance (cached)
        assertTrue(aggregated1 === aggregated2)
    }
    
    @Test
    fun `should invalidate cache when different results provided`() {
        val aggregator = CachingResultAggregator()
        val results1 = listOf(createTestResult("test-1", TestStatus.PASSED))
        val results2 = listOf(createTestResult("test-2", TestStatus.FAILED))
        
        val aggregated1 = aggregator.aggregate(results1)
        val aggregated2 = aggregator.aggregate(results2)
        
        // Should return different instances (cache invalidated)
        assertTrue(aggregated1 !== aggregated2)
        assertEquals(1, aggregated1.passed)
        assertEquals(1, aggregated2.failed)
    }
    
    @Test
    fun `should clear cache manually`() {
        val aggregator = CachingResultAggregator()
        val results = listOf(createTestResult("test-1", TestStatus.PASSED))
        
        val aggregated1 = aggregator.aggregate(results)
        aggregator.clearCache()
        val aggregated2 = aggregator.aggregate(results)
        
        // Should return different instances after cache clear
        assertTrue(aggregated1 !== aggregated2)
    }
}
