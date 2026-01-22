package org.markup.poet.antora.assembler.cli

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ArgumentParserTest {
    
    @Test
    fun `should parse positional arguments`() {
        val args = arrayOf("file1.adoc", "file2.adoc")
        val result = ArgumentParser.parse(args)
        
        assertEquals(listOf("file1.adoc", "file2.adoc"), result.positional)
        assertTrue(result.options.isEmpty())
        assertTrue(result.flags.isEmpty())
    }
    
    @Test
    fun `should parse long options with values`() {
        val args = arrayOf("--component-root", "/path/to/root", "--max-depth", "100")
        val result = ArgumentParser.parse(args)
        
        assertEquals(emptyList(), result.positional)
        assertEquals(mapOf("component-root" to "/path/to/root", "max-depth" to "100"), result.options)
        assertTrue(result.flags.isEmpty())
    }
    
    @Test
    fun `should parse short options with values`() {
        val args = arrayOf("-o", "output.adoc", "-d", "50")
        val result = ArgumentParser.parse(args)
        
        assertEquals(emptyList(), result.positional)
        assertEquals(mapOf("o" to "output.adoc", "d" to "50"), result.options)
        assertTrue(result.flags.isEmpty())
    }
    
    @Test
    fun `should parse boolean flags`() {
        val args = arrayOf("--help", "--verbose", "-f")
        val result = ArgumentParser.parse(args)
        
        assertEquals(emptyList(), result.positional)
        assertTrue(result.options.isEmpty())
        assertEquals(setOf("help", "verbose", "f"), result.flags)
    }
    
    @Test
    fun `should parse mixed arguments`() {
        val args = arrayOf(
            "index.adoc",
            "output.adoc",
            "--component-root",
            "docs",
            "--allow-missing",
            "-h"
        )
        val result = ArgumentParser.parse(args)
        
        assertEquals(listOf("index.adoc", "output.adoc"), result.positional)
        assertEquals(mapOf("component-root" to "docs"), result.options)
        assertEquals(setOf("allow-missing", "h"), result.flags)
    }
    
    @Test
    fun `should handle empty arguments`() {
        val args = arrayOf<String>()
        val result = ArgumentParser.parse(args)
        
        assertEquals(emptyList(), result.positional)
        assertTrue(result.options.isEmpty())
        assertTrue(result.flags.isEmpty())
    }
    
    @Test
    fun `should treat option without value as flag`() {
        val args = arrayOf("--output", "--verbose")
        val result = ArgumentParser.parse(args)
        
        assertEquals(emptyList(), result.positional)
        assertTrue(result.options.isEmpty())
        assertEquals(setOf("output", "verbose"), result.flags)
    }
}
