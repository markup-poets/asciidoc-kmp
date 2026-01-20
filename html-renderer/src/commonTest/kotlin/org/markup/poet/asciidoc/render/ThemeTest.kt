package org.markup.poet.asciidoc.render

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for Theme interface and DefaultTheme implementation.
 * 
 * These tests verify that the theme correctly generates CSS classes
 * and provides valid CSS styles for all supported element types.
 */
class ThemeTest {
    
    private val theme: Theme = DefaultTheme()
    
    @Test
    fun `should generate heading classes with level`() {
        // Test all heading levels
        assertEquals("heading heading-1", theme.headingClasses(1))
        assertEquals("heading heading-2", theme.headingClasses(2))
        assertEquals("heading heading-3", theme.headingClasses(3))
        assertEquals("heading heading-4", theme.headingClasses(4))
        assertEquals("heading heading-5", theme.headingClasses(5))
        assertEquals("heading heading-6", theme.headingClasses(6))
    }
    
    @Test
    fun `should generate paragraph classes`() {
        val classes = theme.paragraphClasses()
        assertEquals("paragraph", classes)
    }
    
    @Test
    fun `should generate code block classes`() {
        val classes = theme.codeBlockClasses()
        assertEquals("code-block", classes)
    }
    
    @Test
    fun `should generate table classes`() {
        val classes = theme.tableClasses()
        assertEquals("table", classes)
    }
    
    @Test
    fun `should generate list classes`() {
        val classes = theme.listClasses()
        assertEquals("list", classes)
    }
    
    @Test
    fun `should generate quote classes`() {
        val classes = theme.quoteClasses()
        assertEquals("quote", classes)
    }
    
    @Test
    fun `should generate admonition classes with type`() {
        assertEquals("admonition admonition-note", theme.admonitionClasses("note"))
        assertEquals("admonition admonition-tip", theme.admonitionClasses("tip"))
        assertEquals("admonition admonition-warning", theme.admonitionClasses("warning"))
        assertEquals("admonition admonition-important", theme.admonitionClasses("important"))
        assertEquals("admonition admonition-caution", theme.admonitionClasses("caution"))
    }
    
    @Test
    fun `should generate CSS stylesheet`() {
        val css = theme.getCss()
        
        // Verify CSS is not empty
        assertTrue(css.isNotEmpty(), "CSS should not be empty")
        
        // Verify CSS contains rules for all element types
        assertTrue(css.contains(".heading"), "CSS should contain heading styles")
        assertTrue(css.contains(".heading-1"), "CSS should contain heading-1 styles")
        assertTrue(css.contains(".heading-2"), "CSS should contain heading-2 styles")
        assertTrue(css.contains(".heading-3"), "CSS should contain heading-3 styles")
        assertTrue(css.contains(".paragraph"), "CSS should contain paragraph styles")
        assertTrue(css.contains(".code-block"), "CSS should contain code-block styles")
        assertTrue(css.contains(".table"), "CSS should contain table styles")
        assertTrue(css.contains(".list"), "CSS should contain list styles")
        assertTrue(css.contains(".quote"), "CSS should contain quote styles")
        assertTrue(css.contains(".admonition"), "CSS should contain admonition styles")
        assertTrue(css.contains(".admonition-note"), "CSS should contain admonition-note styles")
        assertTrue(css.contains(".admonition-tip"), "CSS should contain admonition-tip styles")
        assertTrue(css.contains(".admonition-warning"), "CSS should contain admonition-warning styles")
    }
    
    @Test
    fun `should provide default theme via companion object`() {
        val defaultTheme = Theme.default()
        
        // Verify it's a DefaultTheme instance
        assertTrue(defaultTheme is DefaultTheme, "Default theme should be DefaultTheme instance")
        
        // Verify it works correctly
        assertEquals("heading heading-1", defaultTheme.headingClasses(1))
        assertEquals("paragraph", defaultTheme.paragraphClasses())
    }
    
    @Test
    fun `should handle edge case heading levels`() {
        // Test boundary values
        assertEquals("heading heading-1", theme.headingClasses(1))
        assertEquals("heading heading-6", theme.headingClasses(6))
        
        // Test out-of-range values (implementation should handle gracefully)
        // Note: The implementation doesn't validate level, so it will generate classes for any level
        assertEquals("heading heading-0", theme.headingClasses(0))
        assertEquals("heading heading-7", theme.headingClasses(7))
    }
    
    @Test
    fun `should handle custom admonition types`() {
        // Test that custom types work (not just predefined ones)
        assertEquals("admonition admonition-custom", theme.admonitionClasses("custom"))
        assertEquals("admonition admonition-danger", theme.admonitionClasses("danger"))
    }
    
    @Test
    fun `CSS should contain proper formatting`() {
        val css = theme.getCss()
        
        // Verify CSS contains common properties
        assertTrue(css.contains("margin"), "CSS should contain margin properties")
        assertTrue(css.contains("padding"), "CSS should contain padding properties")
        assertTrue(css.contains("font-size"), "CSS should contain font-size properties")
        assertTrue(css.contains("border"), "CSS should contain border properties")
    }
    
    @Test
    fun `CSS should be valid and well-formed`() {
        val css = theme.getCss()
        
        // Basic validation: check for balanced braces
        val openBraces = css.count { it == '{' }
        val closeBraces = css.count { it == '}' }
        assertEquals(openBraces, closeBraces, "CSS should have balanced braces")
        
        // Check that selectors are present
        assertTrue(css.contains("."), "CSS should contain class selectors")
    }
}
