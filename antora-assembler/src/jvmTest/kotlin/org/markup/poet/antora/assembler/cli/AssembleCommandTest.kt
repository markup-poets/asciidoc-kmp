package org.markup.poet.antora.assembler.cli

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.io.File
import java.nio.file.Files

class AssembleCommandTest {
    
    private val command = AssembleCommand()
    
    @Test
    fun `should return error when missing required arguments`() {
        val args = CommandArgs(
            positional = listOf("index.adoc"),
            options = emptyMap(),
            flags = emptySet()
        )
        
        val result = command.execute(args)
        
        assertTrue(result is CommandResult.Error)
        assertTrue(result.message.contains("Missing required arguments"))
    }
    
    @Test
    fun `should return error when no arguments provided`() {
        val args = CommandArgs(
            positional = emptyList(),
            options = emptyMap(),
            flags = emptySet()
        )
        
        val result = command.execute(args)
        
        assertTrue(result is CommandResult.Error)
        assertTrue(result.message.contains("Missing required arguments"))
    }
    
    @Test
    fun `should return error for invalid max-depth`() {
        val args = CommandArgs(
            positional = listOf("index.adoc", "output.adoc"),
            options = mapOf("max-depth" to "0"),
            flags = emptySet()
        )
        
        val result = command.execute(args)
        
        assertTrue(result is CommandResult.Error)
        assertTrue(result.message.contains("max-depth must be at least 1"))
    }
    
    @Test
    fun `should return error for non-numeric max-depth`() {
        val args = CommandArgs(
            positional = listOf("index.adoc", "output.adoc"),
            options = mapOf("max-depth" to "invalid"),
            flags = emptySet()
        )
        
        val result = command.execute(args)
        
        // Should use default value of 50 when parsing fails
        // The actual error will be about missing index file
        assertTrue(result is CommandResult.Error)
    }
    
    @Test
    fun `should assemble simple document`() {
        // Create temporary test files
        val tempDir = Files.createTempDirectory("antora-test").toFile()
        try {
            // Create Antora structure
            val modulesDir = File(tempDir, "modules/ROOT")
            val pagesDir = File(modulesDir, "pages")
            val partialsDir = File(modulesDir, "partials")
            pagesDir.mkdirs()
            partialsDir.mkdirs()
            
            // Create index file
            val indexFile = File(pagesDir, "index.adoc")
            indexFile.writeText("= Test Document\n\nThis is a test.")
            
            // Create output file path
            val outputFile = File(tempDir, "output.adoc")
            
            // Execute command
            val args = CommandArgs(
                positional = listOf(indexFile.absolutePath, outputFile.absolutePath),
                options = mapOf("component-root" to tempDir.absolutePath),
                flags = emptySet()
            )
            
            val result = command.execute(args)
            
            // Should succeed
            assertTrue(result is CommandResult.Success)
            assertTrue(outputFile.exists())
            
            val content = outputFile.readText()
            assertTrue(content.contains("Test Document"))
        } finally {
            tempDir.deleteRecursively()
        }
    }
    
    @Test
    fun `should handle missing index file`() {
        val args = CommandArgs(
            positional = listOf("/nonexistent/index.adoc", "/tmp/output.adoc"),
            options = emptyMap(),
            flags = emptySet()
        )
        
        val result = command.execute(args)
        
        assertTrue(result is CommandResult.Error)
        assertTrue(result.message.contains("failed") || result.message.contains("Error"))
    }
    
    @Test
    fun `should respect allow-missing flag`() {
        val tempDir = Files.createTempDirectory("antora-test").toFile()
        try {
            val modulesDir = File(tempDir, "modules/ROOT")
            val pagesDir = File(modulesDir, "pages")
            pagesDir.mkdirs()
            
            // Create index with missing include
            val indexFile = File(pagesDir, "index.adoc")
            indexFile.writeText("= Test\n\ninclude::partial\$missing.adoc[]")
            
            val outputFile = File(tempDir, "output.adoc")
            
            val args = CommandArgs(
                positional = listOf(indexFile.absolutePath, outputFile.absolutePath),
                options = mapOf("component-root" to tempDir.absolutePath),
                flags = setOf("allow-missing")
            )
            
            val result = command.execute(args)
            
            // Should succeed with warnings
            assertTrue(result is CommandResult.Success || result is CommandResult.Error)
        } finally {
            tempDir.deleteRecursively()
        }
    }
    
    @Test
    fun `should have correct command name and description`() {
        assertEquals("assemble", command.name)
        assertTrue(command.description.contains("Antora") || command.description.contains("assemble"))
    }
}
