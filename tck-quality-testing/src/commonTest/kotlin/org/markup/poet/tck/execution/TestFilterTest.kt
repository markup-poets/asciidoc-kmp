package org.markup.poet.tck.execution

import org.markup.poet.tck.fixtures.TestFixture
import org.markup.poet.tck.fixtures.FixtureCategory
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class TestFilterTest {
    
    @Test
    fun `CategoryFilter should allow tests in specified categories`() {
        val filter = CategoryFilter(setOf(
            FixtureCategory.BLOCK_PARAGRAPH,
            FixtureCategory.BLOCK_HEADING
        ))
        
        val paragraphTest = TestFixture(
            "test-1", FixtureCategory.BLOCK_PARAGRAPH, "Paragraph", "input", null, emptyMap()
        )
        val headingTest = TestFixture(
            "test-2", FixtureCategory.BLOCK_HEADING, "Heading", "input", null, emptyMap()
        )
        val listTest = TestFixture(
            "test-3", FixtureCategory.BLOCK_LIST, "List", "input", null, emptyMap()
        )
        
        assertTrue(filter.shouldRun(paragraphTest))
        assertTrue(filter.shouldRun(headingTest))
        assertFalse(filter.shouldRun(listTest))
    }
    
    @Test
    fun `SourceFilter should allow custom tests when configured`() {
        val filter = SourceFilter(allowCustom = true, allowOfficial = false)
        
        val customTest = TestFixture(
            "test-1", FixtureCategory.BLOCK_PARAGRAPH, "Custom", "input", null,
            mapOf("source" to "custom")
        )
        val officialTest = TestFixture(
            "test-2", FixtureCategory.BLOCK_PARAGRAPH, "Official", "input", null,
            mapOf("source" to "official-tck")
        )
        
        assertTrue(filter.shouldRun(customTest))
        assertFalse(filter.shouldRun(officialTest))
    }
    
    @Test
    fun `SourceFilter should allow official tests when configured`() {
        val filter = SourceFilter(allowCustom = false, allowOfficial = true)
        
        val customTest = TestFixture(
            "test-1", FixtureCategory.BLOCK_PARAGRAPH, "Custom", "input", null,
            mapOf("source" to "custom")
        )
        val officialTest = TestFixture(
            "test-2", FixtureCategory.BLOCK_PARAGRAPH, "Official", "input", null,
            mapOf("source" to "official-tck")
        )
        
        assertFalse(filter.shouldRun(customTest))
        assertTrue(filter.shouldRun(officialTest))
    }
    
    @Test
    fun `SourceFilter should allow both when configured`() {
        val filter = SourceFilter(allowCustom = true, allowOfficial = true)
        
        val customTest = TestFixture(
            "test-1", FixtureCategory.BLOCK_PARAGRAPH, "Custom", "input", null,
            mapOf("source" to "custom")
        )
        val officialTest = TestFixture(
            "test-2", FixtureCategory.BLOCK_PARAGRAPH, "Official", "input", null,
            mapOf("source" to "official-tck")
        )
        
        assertTrue(filter.shouldRun(customTest))
        assertTrue(filter.shouldRun(officialTest))
    }
    
    @Test
    fun `SourceFilter should allow tests without source metadata by default`() {
        val filter = SourceFilter(allowCustom = true, allowOfficial = false)
        
        val testWithoutSource = TestFixture(
            "test-1", FixtureCategory.BLOCK_PARAGRAPH, "No source", "input", null, emptyMap()
        )
        
        assertTrue(filter.shouldRun(testWithoutSource))
    }
    
    @Test
    fun `SpecSectionFilter should allow tests in specified sections`() {
        val filter = SpecSectionFilter(setOf("4.2", "5.1"))
        
        val section42Test = TestFixture(
            "test-1", FixtureCategory.BLOCK_PARAGRAPH, "Section 4.2", "input", null,
            mapOf("spec_section" to "4.2")
        )
        val section51Test = TestFixture(
            "test-2", FixtureCategory.BLOCK_HEADING, "Section 5.1", "input", null,
            mapOf("spec_section" to "5.1")
        )
        val section60Test = TestFixture(
            "test-3", FixtureCategory.BLOCK_LIST, "Section 6.0", "input", null,
            mapOf("spec_section" to "6.0")
        )
        
        assertTrue(filter.shouldRun(section42Test))
        assertTrue(filter.shouldRun(section51Test))
        assertFalse(filter.shouldRun(section60Test))
    }
    
    @Test
    fun `SpecSectionFilter should allow tests without spec section by default`() {
        val filter = SpecSectionFilter(setOf("4.2"))
        
        val testWithoutSection = TestFixture(
            "test-1", FixtureCategory.BLOCK_PARAGRAPH, "No section", "input", null, emptyMap()
        )
        
        assertTrue(filter.shouldRun(testWithoutSection))
    }
    
    @Test
    fun `CompositeFilter with AND mode should require all filters to pass`() {
        val categoryFilter = CategoryFilter(setOf(FixtureCategory.BLOCK_PARAGRAPH))
        val sourceFilter = SourceFilter(allowCustom = true, allowOfficial = false)
        
        val compositeFilter = CompositeFilter(
            listOf(categoryFilter, sourceFilter),
            CompositeFilter.FilterMode.AND
        )
        
        val matchesBoth = TestFixture(
            "test-1", FixtureCategory.BLOCK_PARAGRAPH, "Matches both", "input", null,
            mapOf("source" to "custom")
        )
        val matchesCategory = TestFixture(
            "test-2", FixtureCategory.BLOCK_PARAGRAPH, "Matches category", "input", null,
            mapOf("source" to "official-tck")
        )
        val matchesSource = TestFixture(
            "test-3", FixtureCategory.BLOCK_HEADING, "Matches source", "input", null,
            mapOf("source" to "custom")
        )
        
        assertTrue(compositeFilter.shouldRun(matchesBoth))
        assertFalse(compositeFilter.shouldRun(matchesCategory))
        assertFalse(compositeFilter.shouldRun(matchesSource))
    }
    
    @Test
    fun `CompositeFilter with OR mode should require at least one filter to pass`() {
        val categoryFilter = CategoryFilter(setOf(FixtureCategory.BLOCK_PARAGRAPH))
        val sourceFilter = SourceFilter(allowCustom = false, allowOfficial = true)
        
        val compositeFilter = CompositeFilter(
            listOf(categoryFilter, sourceFilter),
            CompositeFilter.FilterMode.OR
        )
        
        val matchesBoth = TestFixture(
            "test-1", FixtureCategory.BLOCK_PARAGRAPH, "Matches both", "input", null,
            mapOf("source" to "official-tck")
        )
        val matchesCategory = TestFixture(
            "test-2", FixtureCategory.BLOCK_PARAGRAPH, "Matches category", "input", null,
            mapOf("source" to "custom")
        )
        val matchesSource = TestFixture(
            "test-3", FixtureCategory.BLOCK_HEADING, "Matches source", "input", null,
            mapOf("source" to "official-tck")
        )
        val matchesNeither = TestFixture(
            "test-4", FixtureCategory.BLOCK_HEADING, "Matches neither", "input", null,
            mapOf("source" to "custom")
        )
        
        assertTrue(compositeFilter.shouldRun(matchesBoth))
        assertTrue(compositeFilter.shouldRun(matchesCategory))
        assertTrue(compositeFilter.shouldRun(matchesSource))
        assertFalse(compositeFilter.shouldRun(matchesNeither))
    }
    
    @Test
    fun `CompositeFilter with empty filter list should allow all tests`() {
        val compositeFilter = CompositeFilter(emptyList())
        
        val test = TestFixture(
            "test-1", FixtureCategory.BLOCK_PARAGRAPH, "Test", "input", null, emptyMap()
        )
        
        assertTrue(compositeFilter.shouldRun(test))
    }
    
    @Test
    fun `AllowAllFilter should allow all tests`() {
        val filter = AllowAllFilter()
        
        val test1 = TestFixture("test-1", FixtureCategory.BLOCK_PARAGRAPH, "Test 1", "input", null, emptyMap())
        val test2 = TestFixture("test-2", FixtureCategory.BLOCK_HEADING, "Test 2", "input", null, emptyMap())
        
        assertTrue(filter.shouldRun(test1))
        assertTrue(filter.shouldRun(test2))
    }
    
    @Test
    fun `BlockAllFilter should block all tests`() {
        val filter = BlockAllFilter()
        
        val test1 = TestFixture("test-1", FixtureCategory.BLOCK_PARAGRAPH, "Test 1", "input", null, emptyMap())
        val test2 = TestFixture("test-2", FixtureCategory.BLOCK_HEADING, "Test 2", "input", null, emptyMap())
        
        assertFalse(filter.shouldRun(test1))
        assertFalse(filter.shouldRun(test2))
    }
    
    @Test
    fun `PredicateFilter should use custom predicate`() {
        val filter = PredicateFilter { fixture ->
            fixture.id.startsWith("test-")
        }
        
        val matchingTest = TestFixture("test-1", FixtureCategory.BLOCK_PARAGRAPH, "Test", "input", null, emptyMap())
        val nonMatchingTest = TestFixture("other-1", FixtureCategory.BLOCK_PARAGRAPH, "Test", "input", null, emptyMap())
        
        assertTrue(filter.shouldRun(matchingTest))
        assertFalse(filter.shouldRun(nonMatchingTest))
    }
}
