package org.markup.poet.tck.fixtures

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Unit tests for CompositeFixtureLoader.
 * 
 * These tests verify:
 * - Multi-format delegation
 * - Fixture aggregation
 * - Filtering by source
 * - Statistics generation
 */
class CompositeFixtureLoaderTest {
    
    @Test
    fun `should aggregate fixtures from multiple loaders`() {
        val customLoader = MockCustomLoader(
            fixtures = listOf(
                createTestFixture("custom-1", FixtureCategory.BLOCK_PARAGRAPH),
                createTestFixture("custom-2", FixtureCategory.INLINE_BOLD)
            )
        )
        
        val officialLoader = MockOfficialLoader(
            fixtures = listOf(
                createTestFixture("official-1", FixtureCategory.BLOCK_PARAGRAPH, isOfficial = true),
                createTestFixture("official-2", FixtureCategory.INLINE_ITALIC, isOfficial = true)
            )
        )
        
        val composite = CompositeFixtureLoader(
            loaders = listOf(customLoader, officialLoader)
        )
        
        val allFixtures = composite.loadAllFixtures()
        
        assertEquals(4, allFixtures.size)
    }
    
    @Test
    fun `should load fixtures by category from all loaders`() {
        val customLoader = MockCustomLoader(
            fixtures = listOf(
                createTestFixture("custom-1", FixtureCategory.BLOCK_PARAGRAPH),
                createTestFixture("custom-2", FixtureCategory.INLINE_BOLD)
            )
        )
        
        val officialLoader = MockOfficialLoader(
            fixtures = listOf(
                createTestFixture("official-1", FixtureCategory.BLOCK_PARAGRAPH, isOfficial = true),
                createTestFixture("official-2", FixtureCategory.INLINE_ITALIC, isOfficial = true)
            )
        )
        
        val composite = CompositeFixtureLoader(
            loaders = listOf(customLoader, officialLoader)
        )
        
        val paragraphFixtures = composite.loadFixturesByCategory(FixtureCategory.BLOCK_PARAGRAPH)
        
        assertEquals(2, paragraphFixtures.size)
        assertTrue(paragraphFixtures.any { it.id == "custom-1" })
        assertTrue(paragraphFixtures.any { it.id == "official-1" })
    }
    
    @Test
    fun `should load fixture by ID from first matching loader`() {
        val customLoader = MockCustomLoader(
            fixtures = listOf(
                createTestFixture("test-1", FixtureCategory.BLOCK_PARAGRAPH)
            )
        )
        
        val officialLoader = MockOfficialLoader(
            fixtures = listOf(
                createTestFixture("test-2", FixtureCategory.INLINE_BOLD, isOfficial = true)
            )
        )
        
        val composite = CompositeFixtureLoader(
            loaders = listOf(customLoader, officialLoader)
        )
        
        val fixture1 = composite.loadFixture("test-1")
        assertEquals("test-1", fixture1.id)
        
        val fixture2 = composite.loadFixture("test-2")
        assertEquals("test-2", fixture2.id)
    }
    
    @Test
    fun `should support paths if any loader supports them`() {
        val customLoader = MockCustomLoader()
        val officialLoader = MockOfficialLoader()
        
        val composite = CompositeFixtureLoader(
            loaders = listOf(customLoader, officialLoader)
        )
        
        assertTrue(composite.supports("fixtures/blocks/test.json"))
        assertTrue(composite.supports("official-tck/tests/block/test-input.adoc"))
        assertFalse(composite.supports("unknown/path/test.txt"))
    }
    
    @Test
    fun `should return UNKNOWN format for composite loader`() {
        val composite = CompositeFixtureLoader(
            loaders = listOf(MockCustomLoader(), MockOfficialLoader())
        )
        
        assertEquals(FixtureFormat.UNKNOWN, composite.getFormat())
    }
    
    @Test
    fun `should load only custom fixtures`() {
        val customLoader = MockCustomLoader(
            fixtures = listOf(
                createTestFixture("custom-1", FixtureCategory.BLOCK_PARAGRAPH),
                createTestFixture("custom-2", FixtureCategory.INLINE_BOLD)
            )
        )
        
        val officialLoader = MockOfficialLoader(
            fixtures = listOf(
                createTestFixture("official-1", FixtureCategory.BLOCK_PARAGRAPH, isOfficial = true)
            )
        )
        
        val composite = CompositeFixtureLoader(
            loaders = listOf(customLoader, officialLoader)
        )
        
        val customFixtures = composite.loadCustomFixtures()
        
        assertEquals(2, customFixtures.size)
        assertTrue(customFixtures.all { it.metadata["source"] != "official-tck" })
    }
    
    @Test
    fun `should load only official fixtures`() {
        val customLoader = MockCustomLoader(
            fixtures = listOf(
                createTestFixture("custom-1", FixtureCategory.BLOCK_PARAGRAPH)
            )
        )
        
        val officialLoader = MockOfficialLoader(
            fixtures = listOf(
                createTestFixture("official-1", FixtureCategory.BLOCK_PARAGRAPH, isOfficial = true),
                createTestFixture("official-2", FixtureCategory.INLINE_BOLD, isOfficial = true)
            )
        )
        
        val composite = CompositeFixtureLoader(
            loaders = listOf(customLoader, officialLoader)
        )
        
        val officialFixtures = composite.loadOfficialFixtures()
        
        assertEquals(2, officialFixtures.size)
        assertTrue(officialFixtures.all { it.metadata["source"] == "official-tck" })
    }
    
    @Test
    fun `should generate statistics`() {
        val customLoader = MockCustomLoader(
            fixtures = listOf(
                createTestFixture("custom-1", FixtureCategory.BLOCK_PARAGRAPH),
                createTestFixture("custom-2", FixtureCategory.BLOCK_PARAGRAPH),
                createTestFixture("custom-3", FixtureCategory.INLINE_BOLD)
            )
        )
        
        val officialLoader = MockOfficialLoader(
            fixtures = listOf(
                createTestFixture("official-1", FixtureCategory.BLOCK_PARAGRAPH, isOfficial = true),
                createTestFixture("official-2", FixtureCategory.INLINE_BOLD, isOfficial = true)
            )
        )
        
        val composite = CompositeFixtureLoader(
            loaders = listOf(customLoader, officialLoader)
        )
        
        val stats = composite.getStatistics()
        
        assertEquals(5, stats.totalCount)
        assertEquals(3, stats.customCount)
        assertEquals(2, stats.officialCount)
        assertEquals(3, stats.byCategory[FixtureCategory.BLOCK_PARAGRAPH])
        assertEquals(2, stats.byCategory[FixtureCategory.INLINE_BOLD])
    }
    
    @Test
    fun `should get loader for path`() {
        val customLoader = MockCustomLoader()
        val officialLoader = MockOfficialLoader()
        
        val composite = CompositeFixtureLoader(
            loaders = listOf(customLoader, officialLoader)
        )
        
        val loaderForCustom = composite.getLoaderForPath("fixtures/blocks/test.json")
        assertEquals(customLoader, loaderForCustom)
        
        val loaderForOfficial = composite.getLoaderForPath("official-tck/tests/block/test-input.adoc")
        assertEquals(officialLoader, loaderForOfficial)
    }
    
    @Test
    fun `should handle loader failures gracefully`() {
        val failingLoader = MockFailingLoader()
        val workingLoader = MockCustomLoader(
            fixtures = listOf(
                createTestFixture("test-1", FixtureCategory.BLOCK_PARAGRAPH)
            )
        )
        
        val composite = CompositeFixtureLoader(
            loaders = listOf(failingLoader, workingLoader)
        )
        
        // Should still load from working loader despite failing loader
        val allFixtures = composite.loadAllFixtures()
        assertEquals(1, allFixtures.size)
    }
}

/**
 * Mock custom fixture loader for testing.
 */
class MockCustomLoader(
    private val fixtures: List<TestFixture> = emptyList()
) : FixtureLoader {
    
    override fun loadFixture(id: String): TestFixture {
        return fixtures.find { it.id == id }
            ?: throw FixtureNotFoundException(id)
    }
    
    override fun loadFixturesByCategory(category: FixtureCategory): List<TestFixture> {
        return fixtures.filter { it.category == category }
    }
    
    override fun loadAllFixtures(): List<TestFixture> {
        return fixtures
    }
    
    override fun supports(path: String): Boolean {
        return path.contains("fixtures/") && path.endsWith(".json")
    }
    
    override fun getFormat(): FixtureFormat {
        return FixtureFormat.CUSTOM_JSON
    }
}

/**
 * Mock official TCK loader for testing.
 */
class MockOfficialLoader(
    private val fixtures: List<TestFixture> = emptyList()
) : FixtureLoader {
    
    override fun loadFixture(id: String): TestFixture {
        return fixtures.find { it.id == id }
            ?: throw FixtureNotFoundException(id)
    }
    
    override fun loadFixturesByCategory(category: FixtureCategory): List<TestFixture> {
        return fixtures.filter { it.category == category }
    }
    
    override fun loadAllFixtures(): List<TestFixture> {
        return fixtures
    }
    
    override fun supports(path: String): Boolean {
        return path.contains("official-tck") && path.endsWith("-input.adoc")
    }
    
    override fun getFormat(): FixtureFormat {
        return FixtureFormat.OFFICIAL_TCK
    }
}

/**
 * Mock loader that always fails for testing error handling.
 */
class MockFailingLoader : FixtureLoader {
    
    override fun loadFixture(id: String): TestFixture {
        throw Exception("Mock failure")
    }
    
    override fun loadFixturesByCategory(category: FixtureCategory): List<TestFixture> {
        throw Exception("Mock failure")
    }
    
    override fun loadAllFixtures(): List<TestFixture> {
        throw Exception("Mock failure")
    }
    
    override fun supports(path: String): Boolean {
        return false
    }
    
    override fun getFormat(): FixtureFormat {
        return FixtureFormat.UNKNOWN
    }
}

/**
 * Helper function to create test fixtures.
 */
private fun createTestFixture(
    id: String,
    category: FixtureCategory,
    isOfficial: Boolean = false
): TestFixture {
    return TestFixture(
        id = id,
        category = category,
        description = "Test fixture $id",
        input = "Test input",
        expectedOutput = null,
        metadata = if (isOfficial) {
            mapOf("source" to "official-tck")
        } else {
            mapOf("source" to "custom")
        }
    )
}
