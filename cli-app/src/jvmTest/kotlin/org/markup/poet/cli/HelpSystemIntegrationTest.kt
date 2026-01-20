package org.markup.poet.cli

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertIs
import java.io.ByteArrayOutputStream
import java.io.PrintStream

/**
 * Integration tests for the help system with real commands.
 * 
 * Tests the complete help system with ConvertCommand to ensure
 * all requirements are met in a realistic scenario.
 */
class HelpSystemIntegrationTest {
    
    @Test
    fun `should display help with convert command registered`() {
        val convertCmd = ConvertCommand()
        val router = CommandRouter(mapOf("convert" to convertCmd))
        
        // Capture stdout
        val outputStream = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(outputStream))
        
        try {
            val result = router.route(emptyArray())
            
            assertIs<CommandResult.Success>(result)
            
            val output = outputStream.toString()
            assertTrue(output.contains("AsciiDoc CLI Tool"), "Should display tool name")
            assertTrue(output.contains("convert"), "Should list convert command")
            assertTrue(output.contains("Convert AsciiDoc to Graphviz DOT format"), 
                "Should show convert command description")
        } finally {
            System.setOut(originalOut)
        }
    }
    
    @Test
    fun `should display convert command specific help`() {
        val convertCmd = ConvertCommand()
        val router = CommandRouter(mapOf("convert" to convertCmd))
        
        // Capture stdout
        val outputStream = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(outputStream))
        
        try {
            val result = router.route(arrayOf("convert", "--help"))
            
            assertIs<CommandResult.Success>(result)
            
            val output = outputStream.toString()
            assertTrue(output.contains("Convert AsciiDoc to Graphviz DOT format"), 
                "Should show command description")
            assertTrue(output.contains("Usage:"), "Should show usage")
            assertTrue(output.contains("input.adoc"), "Should document input argument")
            assertTrue(output.contains("output.dot"), "Should document output argument")
            assertTrue(output.contains("Examples:"), "Should include examples")
        } finally {
            System.setOut(originalOut)
        }
    }
    
    @Test
    fun `should handle help flag before executing convert command`() {
        val convertCmd = ConvertCommand()
        val router = CommandRouter(mapOf("convert" to convertCmd))
        
        // Capture stdout
        val outputStream = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(outputStream))
        
        try {
            // This should show help, not try to convert a file named "--help"
            val result = router.route(arrayOf("convert", "--help"))
            
            assertIs<CommandResult.Success>(result)
            
            val output = outputStream.toString()
            assertTrue(output.contains("Usage:"), "Should show help, not execute command")
        } finally {
            System.setOut(originalOut)
        }
    }
    
    @Test
    fun `should show general help with --help flag`() {
        val convertCmd = ConvertCommand()
        val router = CommandRouter(mapOf("convert" to convertCmd))
        
        // Capture stdout
        val outputStream = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(outputStream))
        
        try {
            val result = router.route(arrayOf("--help"))
            
            assertIs<CommandResult.Success>(result)
            
            val output = outputStream.toString()
            assertTrue(output.contains("AsciiDoc CLI Tool"), "Should show general help")
            assertTrue(output.contains("Commands:"), "Should list commands")
            assertTrue(output.contains("convert"), "Should include convert command")
        } finally {
            System.setOut(originalOut)
        }
    }
    
    @Test
    fun `should show general help with -h flag`() {
        val convertCmd = ConvertCommand()
        val router = CommandRouter(mapOf("convert" to convertCmd))
        
        // Capture stdout
        val outputStream = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(outputStream))
        
        try {
            val result = router.route(arrayOf("-h"))
            
            assertIs<CommandResult.Success>(result)
            
            val output = outputStream.toString()
            assertTrue(output.contains("AsciiDoc CLI Tool"), "Should show general help")
        } finally {
            System.setOut(originalOut)
        }
    }
    
    @Test
    fun `should include backward compatibility note in help`() {
        val convertCmd = ConvertCommand()
        val router = CommandRouter(mapOf("convert" to convertCmd))
        
        // Capture stdout
        val outputStream = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(outputStream))
        
        try {
            router.route(emptyArray())
            
            val output = outputStream.toString()
            assertTrue(output.contains("backward compatibility"), 
                "Should mention backward compatibility")
        } finally {
            System.setOut(originalOut)
        }
    }
}
