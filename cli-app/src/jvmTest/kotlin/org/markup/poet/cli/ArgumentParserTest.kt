package org.markup.poet.cli

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ArgumentParserTest {
    
    @Test
    fun `should return empty result for empty arguments`() {
        val result = ArgumentParser.parse(emptyArray())
        
        assertNull(result.subcommand)
        assertTrue(result.commandArgs.positional.isEmpty())
        assertTrue(result.commandArgs.options.isEmpty())
        assertTrue(result.commandArgs.flags.isEmpty())
    }
    
    @Test
    fun `should parse subcommand as first non-option argument`() {
        val result = ArgumentParser.parse(arrayOf("process", "file.txt"))
        
        assertEquals("process", result.subcommand)
        assertEquals(listOf("file.txt"), result.commandArgs.positional)
    }
    
    @Test
    fun `should not treat option as subcommand`() {
        val result = ArgumentParser.parse(arrayOf("--help"))
        
        assertNull(result.subcommand)
        assertTrue(result.commandArgs.flags.contains("help"))
    }
    
    @Test
    fun `should parse positional arguments`() {
        val result = ArgumentParser.parse(arrayOf("convert", "input.adoc", "output.dot"))
        
        assertEquals("convert", result.subcommand)
        assertEquals(listOf("input.adoc", "output.dot"), result.commandArgs.positional)
    }
    
    @Test
    fun `should parse long option with value`() {
        val result = ArgumentParser.parse(arrayOf("process", "--output", "file.txt"))
        
        assertEquals("process", result.subcommand)
        assertEquals("file.txt", result.commandArgs.options["output"])
    }
    
    @Test
    fun `should parse short option with value`() {
        val result = ArgumentParser.parse(arrayOf("process", "-o", "file.txt"))
        
        assertEquals("process", result.subcommand)
        assertEquals("file.txt", result.commandArgs.options["o"])
    }
    
    @Test
    fun `should parse long flag without value`() {
        val result = ArgumentParser.parse(arrayOf("process", "--verbose"))
        
        assertEquals("process", result.subcommand)
        assertTrue(result.commandArgs.flags.contains("verbose"))
    }
    
    @Test
    fun `should parse short flag without value`() {
        val result = ArgumentParser.parse(arrayOf("process", "-v"))
        
        assertEquals("process", result.subcommand)
        assertTrue(result.commandArgs.flags.contains("v"))
    }
    
    @Test
    fun `should parse multiple options and flags`() {
        val result = ArgumentParser.parse(arrayOf(
            "process",
            "input.adoc",
            "--output", "output.adoc",
            "-b", "/base/path",
            "--verbose",
            "-f"
        ))
        
        assertEquals("process", result.subcommand)
        assertEquals(listOf("input.adoc"), result.commandArgs.positional)
        assertEquals("output.adoc", result.commandArgs.options["output"])
        assertEquals("/base/path", result.commandArgs.options["b"])
        assertTrue(result.commandArgs.flags.contains("verbose"))
        assertTrue(result.commandArgs.flags.contains("f"))
    }
    
    @Test
    fun `should treat option at end as flag`() {
        val result = ArgumentParser.parse(arrayOf("process", "file.txt", "--verbose"))
        
        assertEquals("process", result.subcommand)
        assertEquals(listOf("file.txt"), result.commandArgs.positional)
        assertTrue(result.commandArgs.flags.contains("verbose"))
    }
    
    @Test
    fun `should treat option followed by another option as flag`() {
        val result = ArgumentParser.parse(arrayOf("process", "--verbose", "--output", "file.txt"))
        
        assertEquals("process", result.subcommand)
        assertTrue(result.commandArgs.flags.contains("verbose"))
        assertEquals("file.txt", result.commandArgs.options["output"])
    }
    
    @Test
    fun `should handle mixed positional arguments and options`() {
        val result = ArgumentParser.parse(arrayOf(
            "process",
            "input.adoc",
            "--output", "output.adoc",
            "extra-arg",
            "--verbose"
        ))
        
        assertEquals("process", result.subcommand)
        assertEquals(listOf("input.adoc", "extra-arg"), result.commandArgs.positional)
        assertEquals("output.adoc", result.commandArgs.options["output"])
        assertTrue(result.commandArgs.flags.contains("verbose"))
    }
    
    @Test
    fun `should handle no subcommand with options`() {
        val result = ArgumentParser.parse(arrayOf("--help", "--verbose"))
        
        assertNull(result.subcommand)
        assertTrue(result.commandArgs.flags.contains("help"))
        assertTrue(result.commandArgs.flags.contains("verbose"))
    }
    
    @Test
    fun `should parse complex real-world example`() {
        val result = ArgumentParser.parse(arrayOf(
            "process",
            "document.adoc",
            "--output", "processed.adoc",
            "--base-path", "/docs",
            "--max-depth", "5",
            "--verbose",
            "--no-overwrite"
        ))
        
        assertEquals("process", result.subcommand)
        assertEquals(listOf("document.adoc"), result.commandArgs.positional)
        assertEquals("processed.adoc", result.commandArgs.options["output"])
        assertEquals("/docs", result.commandArgs.options["base-path"])
        assertEquals("5", result.commandArgs.options["max-depth"])
        assertTrue(result.commandArgs.flags.contains("verbose"))
        assertTrue(result.commandArgs.flags.contains("no-overwrite"))
    }
    
    @Test
    fun `should handle single dash as positional argument`() {
        val result = ArgumentParser.parse(arrayOf("process", "-"))
        
        assertEquals("process", result.subcommand)
        // Single dash should be treated as positional (stdin convention)
        assertEquals(listOf("-"), result.commandArgs.positional)
    }
    
    @Test
    fun `should support both short and long forms for same option`() {
        // Test that both -o and --output can be used
        val result1 = ArgumentParser.parse(arrayOf("process", "-o", "file1.txt"))
        val result2 = ArgumentParser.parse(arrayOf("process", "--output", "file2.txt"))
        
        assertEquals("file1.txt", result1.commandArgs.options["o"])
        assertEquals("file2.txt", result2.commandArgs.options["output"])
    }
    
    @Test
    fun `should handle empty string as option value`() {
        val result = ArgumentParser.parse(arrayOf("process", "--output", ""))
        
        assertEquals("", result.commandArgs.options["output"])
    }
    
    @Test
    fun `should handle option value that looks like a flag`() {
        // If an option value starts with -, it should still be treated as a value
        // However, our current implementation treats it as a flag
        // This is a known limitation - documenting the behavior
        val result = ArgumentParser.parse(arrayOf("process", "--output", "--file"))
        
        // Current behavior: --file is treated as a flag, not a value
        assertTrue(result.commandArgs.flags.contains("file"))
    }
}
