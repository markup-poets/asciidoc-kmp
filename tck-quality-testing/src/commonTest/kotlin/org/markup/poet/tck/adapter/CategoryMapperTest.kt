package org.markup.poet.tck.adapter

import org.markup.poet.tck.fixtures.FixtureCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for CategoryMapper.
 */
class CategoryMapperTest {
    
    private val mapper = DefaultCategoryMapper()
    
    @Test
    fun `should map block paragraph category`() {
        val category = mapper.mapCategory("block/paragraph")
        assertEquals(FixtureCategory.BLOCK_PARAGRAPH, category)
    }
    
    @Test
    fun `should map block section to heading category`() {
        val category = mapper.mapCategory("block/section")
        assertEquals(FixtureCategory.BLOCK_HEADING, category)
    }
    
    @Test
    fun `should map block list category`() {
        val category = mapper.mapCategory("block/list")
        assertEquals(FixtureCategory.BLOCK_LIST, category)
    }
    
    @Test
    fun `should map block listing to code category`() {
        val category = mapper.mapCategory("block/listing")
        assertEquals(FixtureCategory.BLOCK_CODE, category)
    }
    
    @Test
    fun `should map inline strong to bold category`() {
        val category = mapper.mapCategory("inline/span/strong")
        assertEquals(FixtureCategory.INLINE_BOLD, category)
    }
    
    @Test
    fun `should map inline emphasis to italic category`() {
        val category = mapper.mapCategory("inline/span/emphasis")
        assertEquals(FixtureCategory.INLINE_ITALIC, category)
    }
    
    @Test
    fun `should map inline monospace category`() {
        val category = mapper.mapCategory("inline/span/monospace")
        assertEquals(FixtureCategory.INLINE_MONOSPACE, category)
    }
    
    @Test
    fun `should map inline subscript category`() {
        val category = mapper.mapCategory("inline/span/subscript")
        assertEquals(FixtureCategory.INLINE_SUBSCRIPT, category)
    }
    
    @Test
    fun `should map inline superscript category`() {
        val category = mapper.mapCategory("inline/span/superscript")
        assertEquals(FixtureCategory.INLINE_SUPERSCRIPT, category)
    }
    
    @Test
    fun `should handle tests prefix in path`() {
        val category = mapper.mapCategory("tests/block/paragraph")
        assertEquals(FixtureCategory.BLOCK_PARAGRAPH, category)
    }
    
    @Test
    fun `should handle trailing slash in path`() {
        val category = mapper.mapCategory("block/paragraph/")
        assertEquals(FixtureCategory.BLOCK_PARAGRAPH, category)
    }
    
    @Test
    fun `should map unknown category to conformance`() {
        val category = mapper.mapCategory("unknown/category")
        assertEquals(FixtureCategory.CONFORMANCE, category)
    }
    
    @Test
    fun `should check if category is supported`() {
        assertTrue(mapper.isSupported("block/paragraph"))
        assertTrue(mapper.isSupported("inline/span/strong"))
        assertTrue(mapper.isSupported("tests/block/list"))
    }
    
    @Test
    fun `should return all mappings`() {
        val mappings = mapper.getAllMappings()
        assertTrue(mappings.isNotEmpty())
        assertTrue("block/paragraph" in mappings)
        assertTrue("inline/span/strong" in mappings)
    }
}

/**
 * Unit tests for path extraction extension functions.
 */
class PathExtensionsTest {
    
    @Test
    fun `should extract category from full path`() {
        val path = "tests/block/paragraph/single-line-input.adoc"
        val category = path.extractCategory()
        assertEquals("block/paragraph", category)
    }
    
    @Test
    fun `should extract category from path without tests prefix`() {
        val path = "block/paragraph/single-line-input.adoc"
        val category = path.extractCategory()
        assertEquals("block/paragraph", category)
    }
    
    @Test
    fun `should extract category from nested path`() {
        val path = "tests/inline/span/strong/constrained-single-char-input.adoc"
        val category = path.extractCategory()
        assertEquals("inline/span/strong", category)
    }
    
    @Test
    fun `should extract test name from input file`() {
        val path = "tests/block/paragraph/single-line-input.adoc"
        val testName = path.extractTestName()
        assertEquals("single-line", testName)
    }
    
    @Test
    fun `should extract test name from output file`() {
        val path = "tests/block/paragraph/single-line-output.json"
        val testName = path.extractTestName()
        assertEquals("single-line", testName)
    }
    
    @Test
    fun `should extract test name with hyphens`() {
        val path = "tests/inline/span/strong/constrained-single-char-input.adoc"
        val testName = path.extractTestName()
        assertEquals("constrained-single-char", testName)
    }
    
    @Test
    fun `should build full test ID from path`() {
        val path = "tests/block/paragraph/single-line-input.adoc"
        val testId = path.buildTestId()
        assertEquals("block/paragraph/single-line", testId)
    }
    
    @Test
    fun `should build test ID for nested category`() {
        val path = "tests/inline/span/strong/constrained-single-char-input.adoc"
        val testId = path.buildTestId()
        assertEquals("inline/span/strong/constrained-single-char", testId)
    }
    
    @Test
    fun `should handle path without tests prefix`() {
        val path = "block/paragraph/multiple-lines-input.adoc"
        val testId = path.buildTestId()
        assertEquals("block/paragraph/multiple-lines", testId)
    }
}
