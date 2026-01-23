package org.markup.poet.asciidoc.render

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for MinimalTheme.
 * 
 * Verifies that the minimal theme provides correct CSS classes and valid CSS content.
 */
class MinimalThemeTest {
    
    private val theme = MinimalTheme()
    
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
    fun `should generate valid CSS`() {
        val css = theme.getCss()
        
        // Verify CSS is not empty
        assertTrue(css.isNotEmpty(), "CSS should not be empty")
        
        // Verify CSS contains :root declaration
        assertTrue(css.contains(":root"), "CSS should contain :root declaration")
        
        // Verify CSS variables are defined
        assertTrue(css.contains("--mp-color-text"), "CSS should define --mp-color-text variable")
        assertTrue(css.contains("--mp-color-background"), "CSS should define --mp-color-background variable")
        assertTrue(css.contains("--mp-color-border"), "CSS should define --mp-color-border variable")
        assertTrue(css.contains("--mp-color-muted"), "CSS should define --mp-color-muted variable")
        assertTrue(css.contains("--mp-font-family"), "CSS should define --mp-font-family variable")
        assertTrue(css.contains("--mp-font-size-base"), "CSS should define --mp-font-size-base variable")
        assertTrue(css.contains("--mp-spacing-unit"), "CSS should define --mp-spacing-unit variable")
        assertTrue(css.contains("--mp-line-height"), "CSS should define --mp-line-height variable")
    }
    
    @Test
    fun `should use CSS variables in styles`() {
        val css = theme.getCss()
        
        // Verify CSS uses var() references for customizable properties
        assertTrue(css.contains("var(--mp-font-family)"), "CSS should use font-family variable")
        assertTrue(css.contains("var(--mp-font-size-base)"), "CSS should use font-size-base variable")
        assertTrue(css.contains("var(--mp-line-height)"), "CSS should use line-height variable")
        assertTrue(css.contains("var(--mp-color-text)"), "CSS should use color-text variable")
        assertTrue(css.contains("var(--mp-color-background)"), "CSS should use color-background variable")
        assertTrue(css.contains("var(--mp-color-border)"), "CSS should use color-border variable")
        assertTrue(css.contains("var(--mp-color-muted)"), "CSS should use color-muted variable")
        assertTrue(css.contains("var(--mp-spacing-unit)"), "CSS should use spacing-unit variable")
    }
    
    @Test
    fun `should include styles for all element types`() {
        val css = theme.getCss()
        
        // Verify CSS includes rules for all supported element types
        assertTrue(css.contains(".heading"), "CSS should include heading styles")
        assertTrue(css.contains(".heading-1"), "CSS should include heading-1 styles")
        assertTrue(css.contains(".heading-2"), "CSS should include heading-2 styles")
        assertTrue(css.contains(".paragraph"), "CSS should include paragraph styles")
        assertTrue(css.contains(".code-block"), "CSS should include code-block styles")
        assertTrue(css.contains(".table"), "CSS should include table styles")
        assertTrue(css.contains(".list"), "CSS should include list styles")
        assertTrue(css.contains(".quote"), "CSS should include quote styles")
        assertTrue(css.contains(".admonition"), "CSS should include admonition styles")
    }
    
    @Test
    fun `should follow CSS variable naming convention`() {
        val css = theme.getCss()
        
        // Extract all CSS variable definitions
        val variablePattern = Regex("--mp-[a-z-]+:")
        val variables = variablePattern.findAll(css).map { it.value.removeSuffix(":") }.toList()
        
        // Verify all variables follow the --mp-{category}-{property} pattern
        for (variable in variables) {
            assertTrue(
                variable.startsWith("--mp-"),
                "Variable $variable should start with --mp-"
            )
            
            val parts = variable.removePrefix("--mp-").split("-")
            assertTrue(
                parts.size >= 2,
                "Variable $variable should have at least category and property"
            )
            
            // Verify category is one of the expected values
            val category = parts[0]
            assertTrue(
                category in listOf("color", "font", "spacing", "line"),
                "Variable $variable category should be color, font, spacing, or line"
            )
        }
    }
    
    @Test
    fun `should be more minimal than DefaultTheme`() {
        val minimalCss = theme.getCss()
        val defaultCss = DefaultTheme().getCss()
        
        // Minimal theme should have less CSS than default theme
        assertTrue(
            minimalCss.length < defaultCss.length,
            "MinimalTheme CSS should be shorter than DefaultTheme CSS (minimal: ${minimalCss.length}, default: ${defaultCss.length})"
        )
        
        // Minimal theme should have fewer CSS variables
        val minimalVarCount = Regex("--mp-[a-z-]+:").findAll(minimalCss).count()
        val defaultVarCount = Regex("--mp-[a-z-]+:").findAll(defaultCss).count()
        
        assertTrue(
            minimalVarCount < defaultVarCount,
            "MinimalTheme should have fewer CSS variables than DefaultTheme (minimal: $minimalVarCount, default: $defaultVarCount)"
        )
    }
}
