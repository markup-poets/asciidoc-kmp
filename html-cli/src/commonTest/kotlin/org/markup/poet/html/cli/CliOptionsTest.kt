package org.markup.poet.html.cli

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CliOptionsTest {
    
    @Test
    fun `should parse minimal arguments with input file only`() {
        val args = arrayOf("input.adoc")
        val options = parseArgs(args)
        
        assertEquals("input.adoc", options.inputFile)
        assertEquals("input.html", options.outputFile)
        assertEquals(null, options.cssFile)
        assertEquals(false, options.noDefaultCss)
        assertEquals("default", options.theme)
        assertTrue(options.cssVariables.isEmpty())
    }
    
    @Test
    fun `should parse input and output files`() {
        val args = arrayOf("input.adoc", "output.html")
        val options = parseArgs(args)
        
        assertEquals("input.adoc", options.inputFile)
        assertEquals("output.html", options.outputFile)
    }
    
    @Test
    fun `should parse css-file flag`() {
        val args = arrayOf("--css-file", "custom.css", "input.adoc")
        val options = parseArgs(args)
        
        assertEquals("input.adoc", options.inputFile)
        assertEquals("custom.css", options.cssFile)
    }
    
    @Test
    fun `should parse no-default-css flag`() {
        val args = arrayOf("--no-default-css", "input.adoc")
        val options = parseArgs(args)
        
        assertEquals(true, options.noDefaultCss)
    }
    
    @Test
    fun `should parse theme flag`() {
        val args = arrayOf("--theme", "dark", "input.adoc")
        val options = parseArgs(args)
        
        assertEquals("dark", options.theme)
    }
    
    @Test
    fun `should parse single css-var flag`() {
        val args = arrayOf("--css-var", "--mp-color-primary=#007acc", "input.adoc")
        val options = parseArgs(args)
        
        assertEquals(1, options.cssVariables.size)
        assertEquals("#007acc", options.cssVariables["--mp-color-primary"])
    }
    
    @Test
    fun `should parse multiple css-var flags`() {
        val args = arrayOf(
            "--css-var", "--mp-color-primary=#007acc",
            "--css-var", "--mp-font-size-base=18px",
            "input.adoc"
        )
        val options = parseArgs(args)
        
        assertEquals(2, options.cssVariables.size)
        assertEquals("#007acc", options.cssVariables["--mp-color-primary"])
        assertEquals("18px", options.cssVariables["--mp-font-size-base"])
    }
    
    @Test
    fun `should parse all flags together`() {
        val args = arrayOf(
            "--css-file", "custom.css",
            "--no-default-css",
            "--theme", "minimal",
            "--css-var", "--mp-color-text=#333",
            "input.adoc",
            "output.html"
        )
        val options = parseArgs(args)
        
        assertEquals("input.adoc", options.inputFile)
        assertEquals("output.html", options.outputFile)
        assertEquals("custom.css", options.cssFile)
        assertEquals(true, options.noDefaultCss)
        assertEquals("minimal", options.theme)
        assertEquals(1, options.cssVariables.size)
        assertEquals("#333", options.cssVariables["--mp-color-text"])
    }
    
    @Test
    fun `should throw error when input file is missing`() {
        val args = arrayOf<String>()
        val exception = assertFailsWith<IllegalArgumentException> {
            parseArgs(args)
        }
        assertTrue(exception.message!!.contains("Input file is required"))
    }
    
    @Test
    fun `should throw error when css-file path is missing`() {
        val args = arrayOf("input.adoc", "--css-file")
        val exception = assertFailsWith<IllegalArgumentException> {
            parseArgs(args)
        }
        assertTrue(exception.message!!.contains("--css-file requires a path argument"))
    }
    
    @Test
    fun `should throw error when theme name is missing`() {
        val args = arrayOf("input.adoc", "--theme")
        val exception = assertFailsWith<IllegalArgumentException> {
            parseArgs(args)
        }
        assertTrue(exception.message!!.contains("--theme requires a theme name"))
    }
    
    @Test
    fun `should throw error when css-var value is missing`() {
        val args = arrayOf("input.adoc", "--css-var")
        val exception = assertFailsWith<IllegalArgumentException> {
            parseArgs(args)
        }
        assertTrue(exception.message!!.contains("--css-var requires variable=value"))
    }
    
    @Test
    fun `should throw error when css-var format is invalid`() {
        val args = arrayOf("--css-var", "invalid-format", "input.adoc")
        val exception = assertFailsWith<IllegalArgumentException> {
            parseArgs(args)
        }
        assertTrue(exception.message!!.contains("--css-var format must be: variable=value"))
    }
    
    @Test
    fun `should throw error for invalid theme name`() {
        val args = arrayOf("--theme", "invalid-theme", "input.adoc")
        val exception = assertFailsWith<IllegalArgumentException> {
            parseArgs(args)
        }
        assertTrue(exception.message!!.contains("Invalid theme: invalid-theme"))
        assertTrue(exception.message!!.contains("Available themes:"))
    }
    
    @Test
    fun `should throw error for unknown flag`() {
        val args = arrayOf("--unknown-flag", "input.adoc")
        val exception = assertFailsWith<IllegalArgumentException> {
            parseArgs(args)
        }
        assertTrue(exception.message!!.contains("Unknown flag: --unknown-flag"))
    }
    
    @Test
    fun `should throw error for too many positional arguments`() {
        val args = arrayOf("input.adoc", "output.html", "extra.txt")
        val exception = assertFailsWith<IllegalArgumentException> {
            parseArgs(args)
        }
        assertTrue(exception.message!!.contains("Too many positional arguments"))
    }
    
    @Test
    fun `should handle flags in any order`() {
        val args = arrayOf(
            "input.adoc",
            "--theme", "dark",
            "--css-file", "custom.css",
            "output.html",
            "--no-default-css"
        )
        val options = parseArgs(args)
        
        assertEquals("input.adoc", options.inputFile)
        assertEquals("output.html", options.outputFile)
        assertEquals("custom.css", options.cssFile)
        assertEquals(true, options.noDefaultCss)
        assertEquals("dark", options.theme)
    }
    
    @Test
    fun `should accept all valid theme names`() {
        val validThemes = listOf("default", "minimal", "dark")
        
        for (themeName in validThemes) {
            val args = arrayOf("--theme", themeName, "input.adoc")
            val options = parseArgs(args)
            assertEquals(themeName, options.theme)
        }
    }
    
    @Test
    fun `should handle css-var with equals sign in value`() {
        val args = arrayOf("--css-var", "var=value=with=equals", "input.adoc")
        val options = parseArgs(args)
        
        assertEquals("value=with=equals", options.cssVariables["var"])
    }
}
