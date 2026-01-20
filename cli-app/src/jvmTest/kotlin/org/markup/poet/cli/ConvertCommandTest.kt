package org.markup.poet.cli

import org.markup.poet.asciidoc.parser.DefaultAsciidocParser
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Unit tests for ConvertCommand.
 * 
 * Tests the conversion of AsciiDoc documents to Graphviz DOT format.
 */
class ConvertCommandTest {
    
    private val command = ConvertCommand(DefaultAsciidocParser())
    
    @Test
    fun `should have correct name and description`() {
        assertEquals("convert", command.name)
        assertEquals("Convert AsciiDoc to Graphviz DOT format", command.description)
    }
    
    @Test
    fun `should return error when no input file specified`() {
        val args = CommandArgs(
            positional = emptyList(),
            options = emptyMap(),
            flags = emptySet()
        )
        
        val result = command.execute(args)
        
        assertTrue(result is CommandResult.Error)
        val errorMessage = (result as CommandResult.Error).message
        assertTrue(errorMessage.contains("Missing required argument: input file"))
    }
    
    @Test
    fun `should return error when input file does not exist`() {
        val args = CommandArgs(
            positional = listOf("nonexistent.adoc"),
            options = emptyMap(),
            flags = emptySet()
        )
        
        val result = command.execute(args)
        
        assertTrue(result is CommandResult.Error)
        assertTrue((result as CommandResult.Error).message.contains("not found"))
    }
    
    @Test
    fun `should convert existing file successfully`() {
        // Create a temporary input file
        val tempDir = File(System.getProperty("java.io.tmpdir"))
        val inputFile = File(tempDir, "test-convert-${System.currentTimeMillis()}.adoc")
        val outputFile = File(tempDir, "test-convert-${System.currentTimeMillis()}.dot")
        
        try {
            // Write test content
            inputFile.writeText("""
                = Test Document
                
                This is a test paragraph.
            """.trimIndent())
            
            val args = CommandArgs(
                positional = listOf(inputFile.absolutePath, outputFile.absolutePath),
                options = emptyMap(),
                flags = emptySet()
            )
            
            val result = command.execute(args)
            
            // Verify success
            assertTrue(result is CommandResult.Success)
            assertTrue((result as CommandResult.Success).message?.contains("Successfully converted") == true)
            
            // Verify output file was created
            assertTrue(outputFile.exists())
            
            // Verify output contains DOT format
            val dotContent = outputFile.readText()
            assertTrue(dotContent.contains("digraph"))
            
        } finally {
            // Clean up
            inputFile.delete()
            outputFile.delete()
        }
    }
    
    @Test
    fun `should use default output filename when not specified`() {
        // Create a temporary input file
        val tempDir = File(System.getProperty("java.io.tmpdir"))
        val inputFile = File(tempDir, "test-default-${System.currentTimeMillis()}.adoc")
        val expectedOutputFile = File(tempDir, "test-default-${System.currentTimeMillis()}.dot")
        
        try {
            // Write test content
            inputFile.writeText("""
                = Test Document
                
                This is a test paragraph.
            """.trimIndent())
            
            val args = CommandArgs(
                positional = listOf(inputFile.absolutePath),
                options = emptyMap(),
                flags = emptySet()
            )
            
            val result = command.execute(args)
            
            // Verify success
            assertTrue(result is CommandResult.Success)
            
            // Verify default output file was created
            val defaultOutputFile = File(inputFile.parentFile, "${inputFile.nameWithoutExtension}.dot")
            assertTrue(defaultOutputFile.exists())
            
            // Clean up
            defaultOutputFile.delete()
            
        } finally {
            // Clean up
            inputFile.delete()
        }
    }
    
    @Test
    fun `should handle parse errors gracefully`() {
        // Create a temporary input file with potentially problematic content
        val tempDir = File(System.getProperty("java.io.tmpdir"))
        val inputFile = File(tempDir, "test-errors-${System.currentTimeMillis()}.adoc")
        val outputFile = File(tempDir, "test-errors-${System.currentTimeMillis()}.dot")
        
        try {
            // Write content that might generate warnings
            inputFile.writeText("""
                = Test Document
                
                Some content here.
            """.trimIndent())
            
            val args = CommandArgs(
                positional = listOf(inputFile.absolutePath, outputFile.absolutePath),
                options = emptyMap(),
                flags = emptySet()
            )
            
            val result = command.execute(args)
            
            // Should still succeed even with warnings
            assertTrue(result is CommandResult.Success)
            
        } finally {
            // Clean up
            inputFile.delete()
            outputFile.delete()
        }
    }
}
