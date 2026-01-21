package org.markup.poet.tck.fixtures

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for FixtureCategory enum.
 */
class FixtureCategoryTest {
    
    @Test
    fun `should have all required block categories`() {
        // Arrange
        val blockCategories = listOf(
            FixtureCategory.BLOCK_PARAGRAPH,
            FixtureCategory.BLOCK_HEADING,
            FixtureCategory.BLOCK_LIST,
            FixtureCategory.BLOCK_TABLE,
            FixtureCategory.BLOCK_CODE,
            FixtureCategory.BLOCK_QUOTE
        )
        
        // Assert
        blockCategories.forEach { category ->
            assertTrue(
                category.name.startsWith("BLOCK_"),
                "Block category should start with BLOCK_: ${category.name}"
            )
        }
    }
    
    @Test
    fun `should have all required inline categories`() {
        // Arrange
        val inlineCategories = listOf(
            FixtureCategory.INLINE_BOLD,
            FixtureCategory.INLINE_ITALIC,
            FixtureCategory.INLINE_MONOSPACE,
            FixtureCategory.INLINE_SUBSCRIPT,
            FixtureCategory.INLINE_SUPERSCRIPT
        )
        
        // Assert
        inlineCategories.forEach { category ->
            assertTrue(
                category.name.startsWith("INLINE_"),
                "Inline category should start with INLINE_: ${category.name}"
            )
        }
    }
    
    @Test
    fun `should have all required malformed categories`() {
        // Arrange
        val malformedCategories = listOf(
            FixtureCategory.MALFORMED_BLOCK,
            FixtureCategory.MALFORMED_INLINE,
            FixtureCategory.MALFORMED_ATTRIBUTE
        )
        
        // Assert
        malformedCategories.forEach { category ->
            assertTrue(
                category.name.startsWith("MALFORMED_"),
                "Malformed category should start with MALFORMED_: ${category.name}"
            )
        }
    }
    
    @Test
    fun `should have all required special categories`() {
        // Arrange & Assert
        val allCategories = FixtureCategory.values().toList()
        
        assertTrue(allCategories.contains(FixtureCategory.ATTRIBUTE))
        assertTrue(allCategories.contains(FixtureCategory.MACRO))
        assertTrue(allCategories.contains(FixtureCategory.CROSS_REFERENCE))
        assertTrue(allCategories.contains(FixtureCategory.INCLUDE))
        assertTrue(allCategories.contains(FixtureCategory.CIRCULAR_INCLUDE))
        assertTrue(allCategories.contains(FixtureCategory.MISSING_INCLUDE))
        assertTrue(allCategories.contains(FixtureCategory.CONFORMANCE))
    }
    
    @Test
    fun `should have exactly 24 categories`() {
        // Arrange & Act
        val categoryCount = FixtureCategory.values().size
        
        // Assert
        assertEquals(24, categoryCount, "Expected 24 fixture categories")
    }
    
    @Test
    fun `should be able to iterate over all categories`() {
        // Arrange & Act
        val categories = FixtureCategory.values()
        
        // Assert
        assertTrue(categories.isNotEmpty())
        categories.forEach { category ->
            assertTrue(category.name.isNotEmpty())
        }
    }
}
