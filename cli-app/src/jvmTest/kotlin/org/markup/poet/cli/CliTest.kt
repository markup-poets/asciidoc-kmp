package org.markup.poet.cli

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class CliTest {
    
    @Test
    fun `should convert simple asciidoc to dot format`() {
        // Arrange
        val tempInput = File.createTempFile("test-input", ".adoc")
        val tempOutput = File.createTempFile("test-output", ".dot")
        
        try {
            tempInput.writeText("""
                = Test Document
                
                == Section 1
                This is a paragraph.
                
                * Item 1
                * Item 2
            """.trimIndent())
            
            // Act
            convertAsciidocToDot(tempInput, tempOutput)
            
            // Assert
            assertTrue(tempOutput.exists(), "Output file should exist")
            val content = tempOutput.readText()
            assertTrue(content.contains("digraph AST"), "Should contain DOT graph declaration")
            assertTrue(content.contains("Test Document"), "Should contain document title")
            assertTrue(content.contains("Section"), "Should contain section nodes")
            assertTrue(content.contains("Paragraph"), "Should contain paragraph nodes")
            assertTrue(content.contains("List"), "Should contain list nodes")
        } finally {
            tempInput.delete()
            tempOutput.delete()
        }
    }
    
    @Test
    fun `should handle empty document`() {
        // Arrange
        val tempInput = File.createTempFile("test-empty", ".adoc")
        val tempOutput = File.createTempFile("test-empty-output", ".dot")
        
        try {
            tempInput.writeText("")
            
            // Act
            convertAsciidocToDot(tempInput, tempOutput)
            
            // Assert
            assertTrue(tempOutput.exists(), "Output file should exist")
            val content = tempOutput.readText()
            assertTrue(content.contains("digraph AST"), "Should contain DOT graph declaration")
        } finally {
            tempInput.delete()
            tempOutput.delete()
        }
    }
    
    @Test
    fun `should generate valid node IDs`() {
        // Arrange
        val tempInput = File.createTempFile("test-nodes", ".adoc")
        val tempOutput = File.createTempFile("test-nodes-output", ".dot")
        
        try {
            tempInput.writeText("""
                = Document Title
                
                Paragraph text.
            """.trimIndent())
            
            // Act
            convertAsciidocToDot(tempInput, tempOutput)
            
            // Assert
            val content = tempOutput.readText()
            
            // Check for node declarations (should have format: id [attributes])
            assertTrue(content.contains(Regex("\\w+_\\d+ \\[label=")), "Should contain valid node declarations")
            
            // Check for edges (should have format: id1 -> id2)
            assertTrue(content.contains(Regex("\\w+_\\d+ -> \\w+_\\d+")), "Should contain valid edges")
        } finally {
            tempInput.delete()
            tempOutput.delete()
        }
    }
}
