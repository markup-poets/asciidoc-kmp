package org.markup.poet.tck.fixtures

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit tests for TestFixture data class.
 */
class TestFixtureTest {
    
    @Test
    fun `should create fixture with all required fields`() {
        // Arrange & Act
        val fixture = TestFixture(
            id = "test-fixture-1",
            category = FixtureCategory.BLOCK_PARAGRAPH,
            description = "Test description",
            input = "Test input"
        )
        
        // Assert
        assertEquals("test-fixture-1", fixture.id)
        assertEquals(FixtureCategory.BLOCK_PARAGRAPH, fixture.category)
        assertEquals("Test description", fixture.description)
        assertEquals("Test input", fixture.input)
        assertNull(fixture.expectedOutput)
        assertEquals(emptyMap(), fixture.metadata)
    }
    
    @Test
    fun `should create fixture with expected output`() {
        // Arrange & Act
        val fixture = TestFixture(
            id = "test-fixture-2",
            category = FixtureCategory.INLINE_BOLD,
            description = "Bold formatting test",
            input = "*bold text*",
            expectedOutput = "<strong>bold text</strong>"
        )
        
        // Assert
        assertEquals("test-fixture-2", fixture.id)
        assertEquals(FixtureCategory.INLINE_BOLD, fixture.category)
        assertEquals("Bold formatting test", fixture.description)
        assertEquals("*bold text*", fixture.input)
        assertEquals("<strong>bold text</strong>", fixture.expectedOutput)
    }
    
    @Test
    fun `should create fixture with metadata`() {
        // Arrange & Act
        val metadata = mapOf(
            "spec_reference" to "AsciiDoc Language Documentation - Paragraphs",
            "difficulty" to "basic"
        )
        val fixture = TestFixture(
            id = "test-fixture-3",
            category = FixtureCategory.BLOCK_HEADING,
            description = "Heading test",
            input = "= Document Title",
            metadata = metadata
        )
        
        // Assert
        assertEquals("test-fixture-3", fixture.id)
        assertEquals(metadata, fixture.metadata)
        assertEquals("AsciiDoc Language Documentation - Paragraphs", fixture.metadata["spec_reference"])
        assertEquals("basic", fixture.metadata["difficulty"])
    }
    
    @Test
    fun `should support all fixture categories`() {
        // Arrange & Act - Create fixtures for each category
        val categories = listOf(
            FixtureCategory.BLOCK_PARAGRAPH,
            FixtureCategory.BLOCK_HEADING,
            FixtureCategory.BLOCK_LIST,
            FixtureCategory.BLOCK_TABLE,
            FixtureCategory.BLOCK_CODE,
            FixtureCategory.BLOCK_QUOTE,
            FixtureCategory.INLINE_BOLD,
            FixtureCategory.INLINE_ITALIC,
            FixtureCategory.INLINE_MONOSPACE,
            FixtureCategory.INLINE_SUBSCRIPT,
            FixtureCategory.INLINE_SUPERSCRIPT,
            FixtureCategory.ATTRIBUTE,
            FixtureCategory.MACRO,
            FixtureCategory.CROSS_REFERENCE,
            FixtureCategory.INCLUDE,
            FixtureCategory.MALFORMED_BLOCK,
            FixtureCategory.MALFORMED_INLINE,
            FixtureCategory.MALFORMED_ATTRIBUTE,
            FixtureCategory.CIRCULAR_INCLUDE,
            FixtureCategory.MISSING_INCLUDE,
            FixtureCategory.CONFORMANCE
        )
        
        // Assert - All categories can be used
        categories.forEach { category ->
            val fixture = TestFixture(
                id = "test-${category.name.lowercase()}",
                category = category,
                description = "Test for $category",
                input = "test input"
            )
            assertEquals(category, fixture.category)
        }
    }
}
