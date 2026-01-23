package org.markup.poet.asciidoc.render

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Unit tests for built-in theme factory functionality in DefaultCssProvider.
 * 
 * Validates that the theme factory method correctly:
 * - Returns DefaultTheme for "default" theme name
 * - Returns MinimalTheme for "minimal" theme name
 * - Returns DarkTheme for "dark" theme name
 * - Returns null for invalid theme names
 * - Handles case-insensitive theme names
 * 
 * Validates: Requirements 5.1, 5.2
 */
class ThemeFactoryTest {
    
    /**
     * Test FileReader that always fails (not needed for theme tests).
     */
    private class NoOpFileReader : FileReader {
        override fun readFile(path: String): Result<String> {
            return Result.failure(Exception("File reading not supported in this test"))
        }
    }
    
    /**
     * Simple test theme for comparison.
     */
    private class TestTheme : Theme {
        override fun headingClasses(level: Int) = "test-heading-$level"
        override fun paragraphClasses() = "test-paragraph"
        override fun codeBlockClasses() = "test-code-block"
        override fun tableClasses() = "test-table"
        override fun listClasses() = "test-list"
        override fun quoteClasses() = "test-quote"
        override fun admonitionClasses(type: String) = "test-admonition-$type"
        override fun getCss() = "/* Test Theme CSS */"
    }
    
    @Test
    fun `should return DefaultTheme for default theme name`() {
        val fileReader = NoOpFileReader()
        val provider = DefaultCssProvider(fileReader)
        val fallbackTheme = TestTheme()
        
        val options = CssOptions(
            builtInTheme = "default",
            includeDefaultCss = true
        )
        val result = provider.provideCss(options, fallbackTheme)
        
        assertTrue(result.isSuccess, "Should successfully provide CSS")
        val css = result.getOrNull() ?: ""
        
        // Verify DefaultTheme CSS is used (not TestTheme)
        assertFalse(css.contains("/* Test Theme CSS */"), "Should not contain TestTheme CSS")
        assertTrue(css.contains(".heading"), "Should contain DefaultTheme heading styles")
        assertTrue(css.contains("--mp-color-primary"), "Should contain DefaultTheme CSS variables")
    }
    
    @Test
    fun `should return MinimalTheme for minimal theme name`() {
        val fileReader = NoOpFileReader()
        val provider = DefaultCssProvider(fileReader)
        val fallbackTheme = TestTheme()
        
        val options = CssOptions(
            builtInTheme = "minimal",
            includeDefaultCss = true
        )
        val result = provider.provideCss(options, fallbackTheme)
        
        assertTrue(result.isSuccess, "Should successfully provide CSS")
        val css = result.getOrNull() ?: ""
        
        // Verify MinimalTheme CSS is used
        assertFalse(css.contains("/* Test Theme CSS */"), "Should not contain TestTheme CSS")
        assertTrue(css.contains("--mp-color-text"), "Should contain MinimalTheme CSS variables")
        assertTrue(css.contains("--mp-font-family"), "Should contain MinimalTheme font family")
    }
    
    @Test
    fun `should return DarkTheme for dark theme name`() {
        val fileReader = NoOpFileReader()
        val provider = DefaultCssProvider(fileReader)
        val fallbackTheme = TestTheme()
        
        val options = CssOptions(
            builtInTheme = "dark",
            includeDefaultCss = true
        )
        val result = provider.provideCss(options, fallbackTheme)
        
        assertTrue(result.isSuccess, "Should successfully provide CSS")
        val css = result.getOrNull() ?: ""
        
        // Verify DarkTheme CSS is used
        assertFalse(css.contains("/* Test Theme CSS */"), "Should not contain TestTheme CSS")
        assertTrue(css.contains("--mp-color-background: #1e1e1e"), "Should contain DarkTheme dark background")
        assertTrue(css.contains("--mp-color-code-bg"), "Should contain DarkTheme code background")
    }
    
    @Test
    fun `should return null for invalid theme name and use fallback theme`() {
        val fileReader = NoOpFileReader()
        val provider = DefaultCssProvider(fileReader)
        val fallbackTheme = TestTheme()
        
        val options = CssOptions(
            builtInTheme = "invalid-theme-name",
            includeDefaultCss = true
        )
        val result = provider.provideCss(options, fallbackTheme)
        
        assertTrue(result.isSuccess, "Should successfully provide CSS with fallback theme")
        val css = result.getOrNull() ?: ""
        
        // Verify fallback TestTheme CSS is used when theme name is invalid
        assertTrue(css.contains("/* Test Theme CSS */"), "Should contain fallback TestTheme CSS")
    }
    
    @Test
    fun `should handle case-insensitive theme names`() {
        val fileReader = NoOpFileReader()
        val provider = DefaultCssProvider(fileReader)
        val fallbackTheme = TestTheme()
        
        // Test uppercase
        val optionsUpper = CssOptions(
            builtInTheme = "DEFAULT",
            includeDefaultCss = true
        )
        val resultUpper = provider.provideCss(optionsUpper, fallbackTheme)
        assertTrue(resultUpper.isSuccess, "Should handle uppercase theme name")
        val cssUpper = resultUpper.getOrNull() ?: ""
        assertTrue(cssUpper.contains(".heading"), "Should contain DefaultTheme styles for uppercase")
        
        // Test mixed case
        val optionsMixed = CssOptions(
            builtInTheme = "MiNiMaL",
            includeDefaultCss = true
        )
        val resultMixed = provider.provideCss(optionsMixed, fallbackTheme)
        assertTrue(resultMixed.isSuccess, "Should handle mixed case theme name")
        val cssMixed = resultMixed.getOrNull() ?: ""
        assertTrue(cssMixed.contains("--mp-color-text"), "Should contain MinimalTheme styles for mixed case")
        
        // Test lowercase
        val optionsLower = CssOptions(
            builtInTheme = "dark",
            includeDefaultCss = true
        )
        val resultLower = provider.provideCss(optionsLower, fallbackTheme)
        assertTrue(resultLower.isSuccess, "Should handle lowercase theme name")
        val cssLower = resultLower.getOrNull() ?: ""
        assertTrue(cssLower.contains("--mp-color-background: #1e1e1e"), "Should contain DarkTheme styles for lowercase")
    }
    
    @Test
    fun `should return null for empty theme name and use fallback theme`() {
        val fileReader = NoOpFileReader()
        val provider = DefaultCssProvider(fileReader)
        val fallbackTheme = TestTheme()
        
        val options = CssOptions(
            builtInTheme = "",
            includeDefaultCss = true
        )
        val result = provider.provideCss(options, fallbackTheme)
        
        assertTrue(result.isSuccess, "Should successfully provide CSS with fallback theme")
        val css = result.getOrNull() ?: ""
        
        // Verify fallback TestTheme CSS is used when theme name is empty
        assertTrue(css.contains("/* Test Theme CSS */"), "Should contain fallback TestTheme CSS for empty theme name")
    }
    
    @Test
    fun `should return null for whitespace-only theme name and use fallback theme`() {
        val fileReader = NoOpFileReader()
        val provider = DefaultCssProvider(fileReader)
        val fallbackTheme = TestTheme()
        
        val options = CssOptions(
            builtInTheme = "   ",
            includeDefaultCss = true
        )
        val result = provider.provideCss(options, fallbackTheme)
        
        assertTrue(result.isSuccess, "Should successfully provide CSS with fallback theme")
        val css = result.getOrNull() ?: ""
        
        // Verify fallback TestTheme CSS is used when theme name is whitespace
        assertTrue(css.contains("/* Test Theme CSS */"), "Should contain fallback TestTheme CSS for whitespace theme name")
    }
    
    @Test
    fun `should support all three built-in themes`() {
        val fileReader = NoOpFileReader()
        val provider = DefaultCssProvider(fileReader)
        val fallbackTheme = TestTheme()
        
        val themeNames = listOf("default", "minimal", "dark")
        
        themeNames.forEach { themeName ->
            val options = CssOptions(
                builtInTheme = themeName,
                includeDefaultCss = true
            )
            val result = provider.provideCss(options, fallbackTheme)
            
            assertTrue(result.isSuccess, "Should successfully provide CSS for theme: $themeName")
            val css = result.getOrNull()
            assertNotNull(css, "CSS should not be null for theme: $themeName")
            assertTrue(css.isNotEmpty(), "CSS should not be empty for theme: $themeName")
            assertFalse(css.contains("/* Test Theme CSS */"), "Should not use fallback theme for valid theme: $themeName")
        }
    }
}
