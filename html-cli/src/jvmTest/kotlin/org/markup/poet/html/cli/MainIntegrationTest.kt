package org.markup.poet.html.cli

import org.markup.poet.asciidoc.render.CssException
import org.markup.poet.asciidoc.render.CssOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Integration tests for the main CLI function's CSS handling.
 * 
 * These tests verify that the createCssOptions function correctly
 * translates CLI options into CssOptions for the renderer.
 */
class MainIntegrationTest {
    
    @Test
    fun `createCssOptions should create default options when no CSS flags provided`() {
        val cliOptions = CliOptions(
            inputFile = "test.adoc",
            outputFile = "test.html"
        )
        
        val cssOptions = createCssOptions(cliOptions)
        
        assertEquals(null, cssOptions.customCssPath)
        assertEquals(true, cssOptions.includeDefaultCss)
        assertEquals("default", cssOptions.builtInTheme)
        assertTrue(cssOptions.cssVariables.isEmpty())
    }
    
    @Test
    fun `createCssOptions should set custom CSS path when provided`() {
        // Create a temporary CSS file for testing
        val tempFile = java.io.File.createTempFile("test", ".css")
        tempFile.writeText("body { color: red; }")
        tempFile.deleteOnExit()
        
        val cliOptions = CliOptions(
            inputFile = "test.adoc",
            outputFile = "test.html",
            cssFile = tempFile.absolutePath
        )
        
        val cssOptions = createCssOptions(cliOptions)
        
        assertEquals(tempFile.absolutePath, cssOptions.customCssPath)
        assertEquals(true, cssOptions.includeDefaultCss)
    }
    
    @Test
    fun `createCssOptions should disable default CSS when noDefaultCss is true`() {
        val cliOptions = CliOptions(
            inputFile = "test.adoc",
            outputFile = "test.html",
            noDefaultCss = true
        )
        
        val cssOptions = createCssOptions(cliOptions)
        
        assertFalse(cssOptions.includeDefaultCss)
    }
    
    @Test
    fun `createCssOptions should set theme when provided`() {
        val cliOptions = CliOptions(
            inputFile = "test.adoc",
            outputFile = "test.html",
            theme = "dark"
        )
        
        val cssOptions = createCssOptions(cliOptions)
        
        assertEquals("dark", cssOptions.builtInTheme)
    }
    
    @Test
    fun `createCssOptions should pass through CSS variables`() {
        val cliOptions = CliOptions(
            inputFile = "test.adoc",
            outputFile = "test.html",
            cssVariables = mapOf(
                "--mp-color-primary" to "#007acc",
                "--mp-font-size-base" to "18px"
            )
        )
        
        val cssOptions = createCssOptions(cliOptions)
        
        assertEquals(2, cssOptions.cssVariables.size)
        assertEquals("#007acc", cssOptions.cssVariables["--mp-color-primary"])
        assertEquals("18px", cssOptions.cssVariables["--mp-font-size-base"])
    }
    
    @Test
    fun `createCssOptions should throw CssException when CSS file does not exist`() {
        val cliOptions = CliOptions(
            inputFile = "test.adoc",
            outputFile = "test.html",
            cssFile = "/nonexistent/path/to/file.css"
        )
        
        val exception = assertFailsWith<CssException.FileNotFound> {
            createCssOptions(cliOptions)
        }
        
        assertTrue(exception.path.contains("nonexistent"))
        assertTrue(exception.message!!.contains("CSS file not found"))
    }
    
    @Test
    fun `createCssOptions should create complete options with all flags`() {
        // Create a temporary CSS file for testing
        val tempFile = java.io.File.createTempFile("test", ".css")
        tempFile.writeText("body { color: blue; }")
        tempFile.deleteOnExit()
        
        val cliOptions = CliOptions(
            inputFile = "test.adoc",
            outputFile = "test.html",
            cssFile = tempFile.absolutePath,
            noDefaultCss = true,
            theme = "minimal",
            cssVariables = mapOf("--mp-color-text" to "#333")
        )
        
        val cssOptions = createCssOptions(cliOptions)
        
        assertEquals(tempFile.absolutePath, cssOptions.customCssPath)
        assertFalse(cssOptions.includeDefaultCss)
        assertEquals("minimal", cssOptions.builtInTheme)
        assertEquals(1, cssOptions.cssVariables.size)
        assertEquals("#333", cssOptions.cssVariables["--mp-color-text"])
    }
}
