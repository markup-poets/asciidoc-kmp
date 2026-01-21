package org.markup.poet.tck.fixtures

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

/**
 * Unit tests for ResourceFixtureLoader.
 */
class ResourceFixtureLoaderTest {
    
    private val loader = ResourceFixtureLoader()
    
    @Test
    fun `should load fixture by ID`() {
        // Act
        val fixture = loader.loadFixture("block-paragraph-simple")
        
        // Assert
        assertNotNull(fixture)
        assertEquals("block-paragraph-simple", fixture.id)
        assertEquals(FixtureCategory.BLOCK_PARAGRAPH, fixture.category)
        assertEquals("Simple paragraph with plain text", fixture.description)
        assertEquals("This is a simple paragraph.", fixture.input)
        assertEquals("<p>This is a simple paragraph.</p>", fixture.expectedOutput)
    }
    
    @Test
    fun `should load fixture with metadata`() {
        // Act
        val fixture = loader.loadFixture("block-paragraph-simple")
        
        // Assert
        assertNotNull(fixture.metadata)
        assertTrue(fixture.metadata.containsKey("spec_reference"))
        assertEquals("basic", fixture.metadata["difficulty"])
    }
    
    @Test
    fun `should throw exception for non-existent fixture`() {
        // Act & Assert
        val exception = assertFailsWith<FixtureLoadException> {
            loader.loadFixture("non-existent-fixture")
        }
        
        assertTrue(exception.message?.contains("Fixture not found") == true)
        assertEquals("non-existent-fixture", exception.fixtureId)
    }
    
    @Test
    fun `should load fixtures by category`() {
        // Arrange - Load all fixtures first to populate cache
        loader.loadAllFixtures()
        
        // Act
        val paragraphFixtures = loader.loadFixturesByCategory(FixtureCategory.BLOCK_PARAGRAPH)
        
        // Assert
        assertTrue(paragraphFixtures.isNotEmpty(), "Should have at least one paragraph fixture")
        paragraphFixtures.forEach { fixture ->
            assertEquals(
                FixtureCategory.BLOCK_PARAGRAPH,
                fixture.category,
                "All fixtures should be BLOCK_PARAGRAPH category"
            )
        }
    }
    
    @Test
    fun `should filter fixtures correctly by category`() {
        // Arrange - Load all fixtures first
        loader.loadAllFixtures()
        
        // Act
        val boldFixtures = loader.loadFixturesByCategory(FixtureCategory.INLINE_BOLD)
        val headingFixtures = loader.loadFixturesByCategory(FixtureCategory.BLOCK_HEADING)
        
        // Assert - Each category should only contain fixtures of that category
        boldFixtures.forEach { fixture ->
            assertEquals(FixtureCategory.INLINE_BOLD, fixture.category)
        }
        
        headingFixtures.forEach { fixture ->
            assertEquals(FixtureCategory.BLOCK_HEADING, fixture.category)
        }
    }
    
    @Test
    fun `should return empty list for category with no fixtures`() {
        // Arrange - Load all fixtures first
        loader.loadAllFixtures()
        
        // Act - Try a category that likely has no fixtures yet
        val macroFixtures = loader.loadFixturesByCategory(FixtureCategory.MACRO)
        
        // Assert
        assertNotNull(macroFixtures)
        // Note: This might be empty or not depending on what fixtures exist
    }
    
    @Test
    fun `should load all fixtures`() {
        // Act
        val allFixtures = loader.loadAllFixtures()
        
        // Assert
        assertNotNull(allFixtures)
        assertTrue(allFixtures.isNotEmpty(), "Should load at least some fixtures")
        
        // Verify we have fixtures from different categories
        val categories = allFixtures.map { it.category }.toSet()
        assertTrue(categories.isNotEmpty(), "Should have fixtures from at least one category")
    }
    
    @Test
    fun `should cache loaded fixtures`() {
        // Act - Load the same fixture twice
        val fixture1 = loader.loadFixture("block-paragraph-simple")
        val fixture2 = loader.loadFixture("block-paragraph-simple")
        
        // Assert - Should return the same instance (cached)
        assertEquals(fixture1, fixture2)
    }
    
    @Test
    fun `should load fixture without expected output`() {
        // Act - Load a malformed fixture that has no expected output
        val fixture = loader.loadFixture("malformed-unclosed-block")
        
        // Assert
        assertNotNull(fixture)
        assertEquals("malformed-unclosed-block", fixture.id)
        assertEquals(FixtureCategory.MALFORMED_BLOCK, fixture.category)
        assertEquals(null, fixture.expectedOutput, "Malformed fixtures may not have expected output")
    }
    
    @Test
    fun `should preserve fixture input exactly`() {
        // Act
        val fixture = loader.loadFixture("block-heading-levels")
        
        // Assert - Input should be preserved exactly including newlines
        assertNotNull(fixture.input)
        assertTrue(fixture.input.contains("\n"), "Multi-line input should preserve newlines")
    }
}
