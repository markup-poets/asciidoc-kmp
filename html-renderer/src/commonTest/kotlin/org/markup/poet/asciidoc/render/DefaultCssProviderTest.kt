package org.markup.poet.asciidoc.render

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Unit tests for DefaultCssProvider.
 * 
 * Tests CSS loading, merging, variable overrides, and error handling.
 * 
 * Validates: Requirements 1.1, 2.1, 2.2, 3.1, 3.2, 6.2, 8.1
 */
class DefaultCssProviderTest {
    
    /**
     * Test FileReader implementation that returns predefined content.
     */
    private class TestFileReader(
        private val fileContents: Map<String, String> = emptyMap()
    ) : FileReader {
        override fun readFile(path: String): Result<String> {
            return fileContents[path]?.let { Result.success(it) }
                ?: Result.failure(Exception("File not found: $path"))
        }
    }
    
    /**
     * Simple test theme for testing.
     */
    private class TestTheme : Theme {
        override fun headingClasses(level: Int) = "heading-$level"
        override fun paragraphClasses() = "paragraph"
        override fun codeBlockClasses() = "code-block"
        override fun tableClasses() = "table"
        override fun listClasses() = "list"
        override fun quoteClasses() = "quote"
        override fun admonitionClasses(type: String) = "admonition-$type"
        override fun getCss() = "/* Test Theme CSS */"
    }
    
    @Test
    fun `should return empty CSS when no options provided`() {
        val fileReader = TestFileReader()
        val provider = DefaultCssProvider(fileReader)
        val theme = TestTheme()
        
        val options = CssOptions(includeDefaultCss = false)
        val result = provider.provideCss(options, theme)
        
        assertTrue(result.isSuccess)
        assertEquals("", result.getOrNull()?.trim())
    }
    
    @Test
    fun `should include default theme CSS when enabled`() {
        val fileReader = TestFileReader()
        val provider = DefaultCssProvider(fileReader)
        val theme = TestTheme()
        
        // When builtInTheme is "default" (the default value), it uses DefaultTheme, not the provided theme
        val options = CssOptions(
            includeDefaultCss = true,
            builtInTheme = "default"
        )
        val result = provider.provideCss(options, theme)
        
        assertTrue(result.isSuccess)
        val css = result.getOrNull() ?: ""
        // Should use DefaultTheme, not TestTheme
        assertTrue(css.contains(".heading {"), "Expected CSS to contain DefaultTheme styles")
    }
    
    @Test
    fun `should use built-in default theme when specified`() {
        val fileReader = TestFileReader()
        val provider = DefaultCssProvider(fileReader)
        val theme = TestTheme()
        
        val options = CssOptions(
            builtInTheme = "default",
            includeDefaultCss = true
        )
        val result = provider.provideCss(options, theme)
        
        assertTrue(result.isSuccess)
        val css = result.getOrNull() ?: ""
        // DefaultTheme should be used instead of TestTheme
        assertFalse(css.contains("/* Test Theme CSS */"))
        assertTrue(css.contains(".heading"))
    }
    
    @Test
    fun `should use built-in minimal theme when specified`() {
        val fileReader = TestFileReader()
        val provider = DefaultCssProvider(fileReader)
        val theme = TestTheme()
        
        val options = CssOptions(
            builtInTheme = "minimal",
            includeDefaultCss = true
        )
        val result = provider.provideCss(options, theme)
        
        assertTrue(result.isSuccess)
        val css = result.getOrNull() ?: ""
        // MinimalTheme should be used
        assertTrue(css.contains("--mp-color-text"))
        assertTrue(css.contains("--mp-font-family"))
    }
    
    @Test
    fun `should use built-in dark theme when specified`() {
        val fileReader = TestFileReader()
        val provider = DefaultCssProvider(fileReader)
        val theme = TestTheme()
        
        val options = CssOptions(
            builtInTheme = "dark",
            includeDefaultCss = true
        )
        val result = provider.provideCss(options, theme)
        
        assertTrue(result.isSuccess)
        val css = result.getOrNull() ?: ""
        // DarkTheme should be used
        assertTrue(css.contains("--mp-color-background: #1e1e1e"))
        assertTrue(css.contains("--mp-color-code-bg"))
    }
    
    @Test
    fun `should fall back to provided theme for unknown built-in theme`() {
        val fileReader = TestFileReader()
        val provider = DefaultCssProvider(fileReader)
        val theme = TestTheme()
        
        val options = CssOptions(
            builtInTheme = "unknown-theme",
            includeDefaultCss = true
        )
        val result = provider.provideCss(options, theme)
        
        assertTrue(result.isSuccess)
        val css = result.getOrNull() ?: ""
        // Should fall back to TestTheme
        assertTrue(css.contains("/* Test Theme CSS */"))
    }
    
    @Test
    fun `should handle empty CSS variables map`() {
        val fileReader = TestFileReader()
        val provider = DefaultCssProvider(fileReader)
        val theme = TestTheme()
        
        val options = CssOptions(
            cssVariables = emptyMap(),
            includeDefaultCss = true,
            builtInTheme = "nonexistent" // Use non-existent theme to fall back to TestTheme
        )
        val result = provider.provideCss(options, theme)
        
        assertTrue(result.isSuccess)
        val css = result.getOrNull() ?: ""
        // Should not contain :root block when no variables and theme doesn't have one
        assertFalse(css.trim().startsWith(":root {"))
    }
    
    @Test
    fun `should handle all CSS options together`() {
        val customCss = "/* Custom CSS */"
        val fileReader = TestFileReader()
        val provider = DefaultCssProvider(fileReader)
        val theme = TestTheme()
        
        val options = CssOptions(
            customCssContent = customCss,
            includeDefaultCss = true,
            builtInTheme = "minimal",
            cssVariables = mapOf("--mp-color-primary" to "#ff0000")
        )
        val result = provider.provideCss(options, theme)
        
        assertTrue(result.isSuccess)
        val css = result.getOrNull() ?: ""
        
        // Should contain all parts in correct order
        assertTrue(css.contains(":root {"))
        assertTrue(css.contains("--mp-color-primary: #ff0000;"))
        assertTrue(css.contains("--mp-font-family")) // From minimal theme
        assertTrue(css.contains("/* Custom CSS */"))
        
        // Verify order: variables -> theme -> custom
        val varIndex = css.indexOf(":root {")
        val themeIndex = css.indexOf("--mp-font-family")
        val customIndex = css.indexOf("/* Custom CSS */")
        assertTrue(varIndex < themeIndex)
        assertTrue(themeIndex < customIndex)
    }
}
