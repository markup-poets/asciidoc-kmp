package org.markup.poet.tck.fixtures

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Property-based tests for fixture category filtering.
 * 
 * **Validates: Requirements 1.8**
 * **Property 2: Fixture Category Filtering**
 * 
 * For any fixture category, when loading fixtures by that category, 
 * all returned fixtures SHALL belong to that category and no other.
 */
class FixtureCategoryFilteringPropertyTest {
    
    private val loader = ResourceFixtureLoader()
    
    @Test
    fun `property 2 - fixture category filtering - all categories`() {
        // Feature: tck-quality-testing, Property 2: Fixture Category Filtering
        // **Validates: Requirements 1.8**
        
        // Arrange - Load all fixtures first to populate the cache
        val allFixtures = loader.loadAllFixtures()
        
        // Property: For ANY fixture category, when loading fixtures by that category,
        // all returned fixtures SHALL belong to that category and no other.
        
        // Act & Assert - Test the property for every category
        FixtureCategory.values().forEach { category ->
            val fixturesInCategory = loader.loadFixturesByCategory(category)
            
            // Assert: All returned fixtures must belong to the requested category
            fixturesInCategory.forEach { fixture ->
                assertEquals(
                    expected = category,
                    actual = fixture.category,
                    message = "Fixture '${fixture.id}' was returned for category $category " +
                            "but has category ${fixture.category}"
                )
            }
            
            // Additional verification: No fixtures from other categories should be included
            val fixturesFromOtherCategories = allFixtures.filter { it.category != category }
            fixturesFromOtherCategories.forEach { fixture ->
                assertTrue(
                    actual = fixture !in fixturesInCategory,
                    message = "Fixture '${fixture.id}' with category ${fixture.category} " +
                            "should not be in results for category $category"
                )
            }
        }
    }
    
    @Test
    fun `property 2 - fixture category filtering - no cross contamination`() {
        // Feature: tck-quality-testing, Property 2: Fixture Category Filtering
        // **Validates: Requirements 1.8**
        
        // Arrange
        loader.loadAllFixtures()
        
        // Property: Fixtures from one category should never appear in another category's results
        
        // Act - Load fixtures for different categories
        val categories = FixtureCategory.values()
        val categoryResults = categories.associateWith { category ->
            loader.loadFixturesByCategory(category)
        }
        
        // Assert - Check that no fixture appears in multiple category results
        for (i in categories.indices) {
            for (j in i + 1 until categories.size) {
                val category1 = categories[i]
                val category2 = categories[j]
                val fixtures1 = categoryResults[category1] ?: emptyList()
                val fixtures2 = categoryResults[category2] ?: emptyList()
                
                // No fixture should appear in both lists
                val intersection = fixtures1.intersect(fixtures2.toSet())
                assertTrue(
                    actual = intersection.isEmpty(),
                    message = "Categories $category1 and $category2 should not share fixtures, " +
                            "but found: ${intersection.map { it.id }}"
                )
            }
        }
    }
    
    @Test
    fun `property 2 - fixture category filtering - completeness`() {
        // Feature: tck-quality-testing, Property 2: Fixture Category Filtering
        // **Validates: Requirements 1.8**
        
        // Arrange
        val allFixtures = loader.loadAllFixtures()
        
        // Property: Every fixture should be retrievable via its category
        
        // Act & Assert - For each fixture, verify it appears in its category's results
        allFixtures.forEach { fixture ->
            val categoryFixtures = loader.loadFixturesByCategory(fixture.category)
            
            assertTrue(
                actual = fixture in categoryFixtures,
                message = "Fixture '${fixture.id}' with category ${fixture.category} " +
                        "should be retrievable via loadFixturesByCategory(${fixture.category})"
            )
        }
    }
    
    @Test
    fun `property 2 - fixture category filtering - consistency across calls`() {
        // Feature: tck-quality-testing, Property 2: Fixture Category Filtering
        // **Validates: Requirements 1.8**
        
        // Arrange
        loader.loadAllFixtures()
        
        // Property: Multiple calls to loadFixturesByCategory should return consistent results
        
        // Act & Assert - Call each category multiple times and verify consistency
        FixtureCategory.values().forEach { category ->
            val firstCall = loader.loadFixturesByCategory(category)
            val secondCall = loader.loadFixturesByCategory(category)
            val thirdCall = loader.loadFixturesByCategory(category)
            
            // All calls should return the same fixtures (same IDs)
            val firstIds = firstCall.map { it.id }.toSet()
            val secondIds = secondCall.map { it.id }.toSet()
            val thirdIds = thirdCall.map { it.id }.toSet()
            
            assertEquals(
                expected = firstIds,
                actual = secondIds,
                message = "Second call to loadFixturesByCategory($category) returned different fixtures"
            )
            
            assertEquals(
                expected = firstIds,
                actual = thirdIds,
                message = "Third call to loadFixturesByCategory($category) returned different fixtures"
            )
        }
    }
    
    @Test
    fun `property 2 - fixture category filtering - empty categories are valid`() {
        // Feature: tck-quality-testing, Property 2: Fixture Category Filtering
        // **Validates: Requirements 1.8**
        
        // Arrange
        loader.loadAllFixtures()
        
        // Property: Categories with no fixtures should return empty lists (not null, not error)
        
        // Act & Assert - All categories should return a valid list (possibly empty)
        FixtureCategory.values().forEach { category ->
            val fixtures = loader.loadFixturesByCategory(category)
            
            // Should return a list (not null)
            assertTrue(
                actual = fixtures is List,
                message = "loadFixturesByCategory($category) should return a List"
            )
            
            // All fixtures in the list must have the correct category
            fixtures.forEach { fixture ->
                assertEquals(
                    expected = category,
                    actual = fixture.category,
                    message = "Fixture '${fixture.id}' in category $category has wrong category"
                )
            }
        }
    }
}
