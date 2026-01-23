package org.markup.poet.asciidoc.render

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for DarkTheme.
 * 
 * Verifies that the dark theme:
 * - Implements all Theme interface methods correctly
 * - Returns appropriate CSS class names
 * - Generates valid CSS with dark color scheme
 * - Uses CSS variables for customization
 * - Follows the --mp-{category}-{property} naming convention
 */
class DarkThemeTest {
    
    private val theme = DarkTheme()
    
    @Test
    fun `should return correct heading classes`() {
        assertEquals("heading heading-1", theme.headingClasses(1))
        assertEquals("heading heading-2", theme.headingClasses(2))
        assertEquals("heading heading-3", theme.headingClasses(3))
        assertEquals("heading heading-4", theme.headingClasses(4))
        assertEquals("heading heading-5", theme.headingClasses(5))
        assertEquals("heading heading-6", theme.headingClasses(6))
    }
    
    @Test
    fun `should return correct paragraph classes`() {
        assertEquals("paragraph", theme.paragraphClasses())
    }
    
    @Test
    fun `should return correct code block classes`() {
        assertEquals("code-block", theme.codeBlockClasses())
    }
    
    @Test
    fun `should return correct table classes`() {
        assertEquals("table", theme.tableClasses())
    }
    
    @Test
    fun `should return correct list classes`() {
        assertEquals("list", theme.listClasses())
    }
    
    @Test
    fun `should return correct quote classes`() {
        assertEquals("quote", theme.quoteClasses())
    }
    
    @Test
    fun `should return correct admonition classes`() {
        assertEquals("admonition admonition-note", theme.admonitionClasses("note"))
        assertEquals("admonition admonition-tip", theme.admonitionClasses("tip"))
        assertEquals("admonition admonition-warning", theme.admonitionClasses("warning"))
        assertEquals("admonition admonition-important", theme.admonitionClasses("important"))
        assertEquals("admonition admonition-caution", theme.admonitionClasses("caution"))
    }
    
    @Test
    fun `should generate non-empty CSS`() {
        val css = theme.getCss()
        assertTrue(css.isNotEmpty(), "CSS should not be empty")
    }
    
    @Test
    fun `should define dark color scheme CSS variables`() {
        val css = theme.getCss()
        
        // Verify dark background and light text
        assertTrue(css.contains("--mp-color-text: #e0e0e0"), "Should define light text color")
        assertTrue(css.contains("--mp-color-background: #1e1e1e"), "Should define dark background")
        assertTrue(css.contains("--mp-color-code-bg: #2d2d2d"), "Should define dark code background")
    }
    
    @Test
    fun `should define all required color variables`() {
        val css = theme.getCss()
        
        // Core colors
        assertTrue(css.contains("--mp-color-text:"))
        assertTrue(css.contains("--mp-color-background:"))
        assertTrue(css.contains("--mp-color-border:"))
        
        // Admonition colors
        assertTrue(css.contains("--mp-color-note:"))
        assertTrue(css.contains("--mp-color-tip:"))
        assertTrue(css.contains("--mp-color-warning:"))
        assertTrue(css.contains("--mp-color-important:"))
        assertTrue(css.contains("--mp-color-caution:"))
    }
    
    @Test
    fun `should define font variables`() {
        val css = theme.getCss()
        
        assertTrue(css.contains("--mp-font-family:"))
        assertTrue(css.contains("--mp-font-size-base:"))
    }
    
    @Test
    fun `should define spacing variables`() {
        val css = theme.getCss()
        
        assertTrue(css.contains("--mp-spacing-unit:"))
    }
    
    @Test
    fun `should define line height variables`() {
        val css = theme.getCss()
        
        assertTrue(css.contains("--mp-line-height-base:"))
        assertTrue(css.contains("--mp-line-height-heading:"))
    }
    
    @Test
    fun `should use CSS variables in rules`() {
        val css = theme.getCss()
        
        // Verify that CSS rules use var() references
        assertTrue(css.contains("var(--mp-color-text)"))
        assertTrue(css.contains("var(--mp-color-background)"))
        assertTrue(css.contains("var(--mp-font-family)"))
        assertTrue(css.contains("var(--mp-spacing-unit)"))
    }
    
    @Test
    fun `should include styles for all element types`() {
        val css = theme.getCss()
        
        // Verify styles for all major element types
        assertTrue(css.contains(".heading"))
        assertTrue(css.contains(".paragraph"))
        assertTrue(css.contains(".code-block"))
        assertTrue(css.contains(".table"))
        assertTrue(css.contains(".list"))
        assertTrue(css.contains(".quote"))
        assertTrue(css.contains(".admonition"))
    }
    
    @Test
    fun `should include styles for all heading levels`() {
        val css = theme.getCss()
        
        assertTrue(css.contains(".heading-1"))
        assertTrue(css.contains(".heading-2"))
        assertTrue(css.contains(".heading-3"))
        assertTrue(css.contains(".heading-4"))
        assertTrue(css.contains(".heading-5"))
        assertTrue(css.contains(".heading-6"))
    }
    
    @Test
    fun `should include styles for all admonition types`() {
        val css = theme.getCss()
        
        assertTrue(css.contains(".admonition-note"))
        assertTrue(css.contains(".admonition-tip"))
        assertTrue(css.contains(".admonition-warning"))
        assertTrue(css.contains(".admonition-important"))
        assertTrue(css.contains(".admonition-caution"))
    }
    
    @Test
    fun `should follow CSS variable naming convention`() {
        val css = theme.getCss()
        
        // Extract all CSS variable definitions
        val variablePattern = Regex("--mp-[a-z-]+:")
        val variables = variablePattern.findAll(css).map { it.value.removeSuffix(":") }.toList()
        
        // Verify all variables follow the --mp-{category}-{property} pattern
        val validPattern = Regex("^--mp-(color|font|spacing|line-height|border)-.+$")
        variables.forEach { variable ->
            assertTrue(
                validPattern.matches(variable),
                "Variable '$variable' should follow --mp-{category}-{property} pattern"
            )
        }
    }
    
    @Test
    fun `should have dark theme characteristics`() {
        val css = theme.getCss()
        
        // Verify dark theme has appropriate color values
        // Dark backgrounds should have low hex values (closer to #000000)
        assertTrue(css.contains("#1e1e1e") || css.contains("#2d2d2d"), 
            "Should have dark background colors")
        
        // Light text should have high hex values (closer to #ffffff)
        assertTrue(css.contains("#e0e0e0") || css.contains("#a0a0a0"), 
            "Should have light text colors")
    }
    
    @Test
    fun `should include link styling for dark theme`() {
        val css = theme.getCss()
        
        // Dark themes typically need specific link colors
        assertTrue(css.contains("--mp-color-link:"))
        assertTrue(css.contains("a {") || css.contains("a{"))
    }
}
