package org.markup.poet.tck.execution

import org.markup.poet.tck.fixtures.FixtureCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class ResultCollectorTest {
    
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
    fun `should collect single result`() {
        val collector = InMemoryResultCollector()
        val result = createTestResult("test-1", TestStatus.PASSED)
        
        collector.addResult(result)
        
        assertEquals(1, collector.size())
        assertEquals(result, collector.getAllResults().first())
    }
    
    @Test
    fun `should collect multiple results`() {
        val collector = InMemoryResultCollector()
        val results = listOf(
            createTestResult("test-1", TestStatus.PASSED),
            createTestResult("test-2", TestStatus.FAILED),
            createTestResult("test-3", TestStatus.PENDING)
        )
        
        collector.addResults(results)
        
        assertEquals(3, collector.size())
        assertEquals(results, collector.getAllResults())
    }
    
    @Test
    fun `should filter results by platform`() {
        val collector = InMemoryResultCollector()
        collector.addResults(listOf(
            createTestResult("test-1", TestStatus.PASSED, platform = "JVM"),
            createTestResult("test-2", TestStatus.PASSED, platform = "iOS"),
            createTestResult("test-3", TestStatus.PASSED, platform = "JVM")
        ))
        
        val jvmResults = collector.getResultsByPlatform("JVM")
        
        assertEquals(2, jvmResults.size)
        assertTrue(jvmResults.all { it.platform == "JVM" })
    }
    
    @Test
    fun `should filter results by status`() {
        val collector = InMemoryResultCollector()
        collector.addResults(listOf(
            createTestResult("test-1", TestStatus.PASSED),
            createTestResult("test-2", TestStatus.FAILED),
            createTestResult("test-3", TestStatus.PASSED),
            createTestResult("test-4", TestStatus.PENDING)
        ))
        
        val passedResults = collector.getResultsByStatus(TestStatus.PASSED)
        val failedResults = collector.getResultsByStatus(TestStatus.FAILED)
        
        assertEquals(2, passedResults.size)
        assertEquals(1, failedResults.size)
        assertTrue(passedResults.all { it.status == TestStatus.PASSED })
        assertTrue(failedResults.all { it.status == TestStatus.FAILED })
    }
    
    @Test
    fun `should filter results by category`() {
        val collector = InMemoryResultCollector()
        collector.addResults(listOf(
            createTestResult("test-1", TestStatus.PASSED, category = FixtureCategory.BLOCK_PARAGRAPH),
            createTestResult("test-2", TestStatus.PASSED, category = FixtureCategory.BLOCK_HEADING),
            createTestResult("test-3", TestStatus.PASSED, category = FixtureCategory.BLOCK_PARAGRAPH)
        ))
        
        val paragraphResults = collector.getResultsByCategory(FixtureCategory.BLOCK_PARAGRAPH)
        
        assertEquals(2, paragraphResults.size)
        assertTrue(paragraphResults.all { it.category == FixtureCategory.BLOCK_PARAGRAPH })
    }
    
    @Test
    fun `should report empty when no results collected`() {
        val collector = InMemoryResultCollector()
        
        assertTrue(collector.isEmpty())
        assertEquals(0, collector.size())
    }
    
    @Test
    fun `should report not empty when results collected`() {
        val collector = InMemoryResultCollector()
        collector.addResult(createTestResult("test-1", TestStatus.PASSED))
        
        assertFalse(collector.isEmpty())
        assertEquals(1, collector.size())
    }
    
    @Test
    fun `should clear all results`() {
        val collector = InMemoryResultCollector()
        collector.addResults(listOf(
            createTestResult("test-1", TestStatus.PASSED),
            createTestResult("test-2", TestStatus.FAILED)
        ))
        
        assertEquals(2, collector.size())
        
        collector.clear()
        
        assertTrue(collector.isEmpty())
        assertEquals(0, collector.size())
    }
    
    @Test
    fun `should accumulate results from multiple additions`() {
        val collector = InMemoryResultCollector()
        
        collector.addResults(listOf(
            createTestResult("test-1", TestStatus.PASSED),
            createTestResult("test-2", TestStatus.PASSED)
        ))
        
        collector.addResults(listOf(
            createTestResult("test-3", TestStatus.FAILED)
        ))
        
        collector.addResult(createTestResult("test-4", TestStatus.PENDING))
        
        assertEquals(4, collector.size())
    }
    
    @Test
    fun `should generate summary`() {
        val collector = InMemoryResultCollector()
        collector.addResults(listOf(
            createTestResult("test-1", TestStatus.PASSED),
            createTestResult("test-2", TestStatus.FAILED),
            createTestResult("test-3", TestStatus.PENDING),
            createTestResult("test-4", TestStatus.SKIPPED),
            createTestResult("test-5", TestStatus.ERROR)
        ))
        
        val summary = collector.summary()
        
        assertTrue(summary.contains("Total: 5"))
        assertTrue(summary.contains("Passed: 1"))
        assertTrue(summary.contains("Failed: 1"))
        assertTrue(summary.contains("Pending: 1"))
        assertTrue(summary.contains("Skipped: 1"))
        assertTrue(summary.contains("Errors: 1"))
    }
}

class CompositeResultCollectorTest {
    
    private fun createTestResult(id: String, status: TestStatus): TestExecutionResult {
        return TestExecutionResult(
            fixtureId = id,
            status = status,
            platform = "JVM",
            durationMs = 100
        )
    }
    
    @Test
    fun `should delegate to all collectors`() {
        val collector1 = InMemoryResultCollector()
        val collector2 = InMemoryResultCollector()
        val composite = CompositeResultCollector(listOf(collector1, collector2))
        
        val results = listOf(
            createTestResult("test-1", TestStatus.PASSED),
            createTestResult("test-2", TestStatus.FAILED)
        )
        
        composite.addResults(results)
        
        assertEquals(2, collector1.size())
        assertEquals(2, collector2.size())
        assertEquals(results, collector1.getAllResults())
        assertEquals(results, collector2.getAllResults())
    }
    
    @Test
    fun `should clear all collectors`() {
        val collector1 = InMemoryResultCollector()
        val collector2 = InMemoryResultCollector()
        val composite = CompositeResultCollector(listOf(collector1, collector2))
        
        composite.addResults(listOf(createTestResult("test-1", TestStatus.PASSED)))
        
        assertEquals(1, collector1.size())
        assertEquals(1, collector2.size())
        
        composite.clear()
        
        assertTrue(collector1.isEmpty())
        assertTrue(collector2.isEmpty())
    }
    
    @Test
    fun `should return results from first collector`() {
        val collector1 = InMemoryResultCollector()
        val collector2 = InMemoryResultCollector()
        val composite = CompositeResultCollector(listOf(collector1, collector2))
        
        val results = listOf(createTestResult("test-1", TestStatus.PASSED))
        composite.addResults(results)
        
        assertEquals(results, composite.getAllResults())
    }
    
    @Test
    fun `should handle empty collector list`() {
        val composite = CompositeResultCollector(emptyList())
        
        composite.addResults(listOf(createTestResult("test-1", TestStatus.PASSED)))
        
        assertTrue(composite.getAllResults().isEmpty())
    }
}
