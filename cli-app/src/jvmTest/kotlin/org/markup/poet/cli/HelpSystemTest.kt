package org.markup.poet.cli

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertIs
import java.io.ByteArrayOutputStream
import java.io.PrintStream

/**
 * Tests for the help system implementation.
 * 
 * Validates Requirements 4.2, 4.3, and 7.5:
 * - 4.2: Display usage when no arguments provided
 * - 4.3: Display detailed help with --help flag
 * - 7.5: Display unified help message showing all commands
 */
class HelpSystemTest {
    
    private class TestCommand(
        override val name: String,
        override val description: String
    ) : CliCommand {
        override fun execute(args: CommandArgs): CommandResult {
            return CommandResult.Success()
        }
        
        override fun printHelp() {
            println("Detailed help for $name command")
        }
    }
    
    @Test
    fun `should display usage information when no arguments provided`() {
        // Requirement 4.2: Display usage when no arguments provided
        val convertCmd = TestCommand("convert", "Convert AsciiDoc to DOT")
        val processCmd = TestCommand("process", "Process AsciiDoc documents")
        val router = CommandRouter(mapOf(
            "convert" to convertCmd,
            "process" to processCmd
        ))
        
        // Capture stdout
        val outputStream = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(outputStream))
        
        try {
            val result = router.route(emptyArray())
            
            // Should return success
            assertIs<CommandResult.Success>(result)
            
            // Should display help text
            val output = outputStream.toString()
            assertTrue(output.contains("AsciiDoc CLI Tool"), "Should display tool name")
            assertTrue(output.contains("Usage:"), "Should display usage section")
            assertTrue(output.contains("Commands:"), "Should display commands section")
            assertTrue(output.contains("convert"), "Should list convert command")
            assertTrue(output.contains("process"), "Should list process command")
        } finally {
            System.setOut(originalOut)
        }
    }
    
    @Test
    fun `should display detailed help with --help flag`() {
        // Requirement 4.3: Display detailed help with --help flag
        val convertCmd = TestCommand("convert", "Convert AsciiDoc to DOT")
        val processCmd = TestCommand("process", "Process AsciiDoc documents")
        val router = CommandRouter(mapOf(
            "convert" to convertCmd,
            "process" to processCmd
        ))
        
        // Capture stdout
        val outputStream = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(outputStream))
        
        try {
            val result = router.route(arrayOf("--help"))
            
            // Should return success
            assertIs<CommandResult.Success>(result)
            
            // Should display detailed help
            val output = outputStream.toString()
            assertTrue(output.contains("AsciiDoc CLI Tool"), "Should display tool name")
            assertTrue(output.contains("Usage:"), "Should display usage section")
            assertTrue(output.contains("Commands:"), "Should display commands section")
            assertTrue(output.contains("Options:"), "Should display options section")
            assertTrue(output.contains("--help"), "Should document --help flag")
            assertTrue(output.contains("convert"), "Should list convert command")
            assertTrue(output.contains("process"), "Should list process command")
            assertTrue(output.contains("Convert AsciiDoc to DOT"), "Should show command descriptions")
            assertTrue(output.contains("Process AsciiDoc documents"), "Should show command descriptions")
        } finally {
            System.setOut(originalOut)
        }
    }
    
    @Test
    fun `should display detailed help with -h flag`() {
        // Requirement 4.3: Support short form of help flag
        val convertCmd = TestCommand("convert", "Convert AsciiDoc to DOT")
        val router = CommandRouter(mapOf("convert" to convertCmd))
        
        // Capture stdout
        val outputStream = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(outputStream))
        
        try {
            val result = router.route(arrayOf("-h"))
            
            // Should return success
            assertIs<CommandResult.Success>(result)
            
            // Should display help
            val output = outputStream.toString()
            assertTrue(output.contains("AsciiDoc CLI Tool"), "Should display tool name")
            assertTrue(output.contains("Commands:"), "Should display commands section")
        } finally {
            System.setOut(originalOut)
        }
    }
    
    @Test
    fun `should display unified help message showing all available commands`() {
        // Requirement 7.5: Display unified help message showing all commands
        val cmd1 = TestCommand("convert", "Convert AsciiDoc to DOT")
        val cmd2 = TestCommand("process", "Process AsciiDoc documents")
        val cmd3 = TestCommand("validate", "Validate AsciiDoc syntax")
        val router = CommandRouter(mapOf(
            "convert" to cmd1,
            "process" to cmd2,
            "validate" to cmd3
        ))
        
        // Capture stdout
        val outputStream = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(outputStream))
        
        try {
            val result = router.route(emptyArray())
            
            // Should return success
            assertIs<CommandResult.Success>(result)
            
            // Should display all commands in a unified message
            val output = outputStream.toString()
            assertTrue(output.contains("convert"), "Should list convert command")
            assertTrue(output.contains("process"), "Should list process command")
            assertTrue(output.contains("validate"), "Should list validate command")
            assertTrue(output.contains("Convert AsciiDoc to DOT"), "Should show convert description")
            assertTrue(output.contains("Process AsciiDoc documents"), "Should show process description")
            assertTrue(output.contains("Validate AsciiDoc syntax"), "Should show validate description")
        } finally {
            System.setOut(originalOut)
        }
    }
    
    @Test
    fun `should display command-specific help when command --help is used`() {
        // Additional test: Command-specific help
        val convertCmd = TestCommand("convert", "Convert AsciiDoc to DOT")
        val router = CommandRouter(mapOf("convert" to convertCmd))
        
        // Capture stdout
        val outputStream = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(outputStream))
        
        try {
            val result = router.route(arrayOf("convert", "--help"))
            
            // Should return success
            assertIs<CommandResult.Success>(result)
            
            // Should display command-specific help
            val output = outputStream.toString()
            assertTrue(output.contains("Detailed help for convert command"), 
                "Should call command's printHelp method")
        } finally {
            System.setOut(originalOut)
        }
    }
    
    @Test
    fun `help message should include usage examples`() {
        // Verify help includes usage examples for clarity
        val convertCmd = TestCommand("convert", "Convert AsciiDoc to DOT")
        val router = CommandRouter(mapOf("convert" to convertCmd))
        
        // Capture stdout
        val outputStream = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(outputStream))
        
        try {
            router.route(arrayOf("--help"))
            
            val output = outputStream.toString()
            assertTrue(output.contains("Usage:"), "Should have usage section")
            assertTrue(output.contains("<command>"), "Should show command placeholder")
            assertTrue(output.contains("for more information"), 
                "Should guide users to command-specific help")
        } finally {
            System.setOut(originalOut)
        }
    }
    
    @Test
    fun `help message should show both short and long option forms`() {
        // Verify help documents both -h and --help
        val convertCmd = TestCommand("convert", "Convert AsciiDoc to DOT")
        val router = CommandRouter(mapOf("convert" to convertCmd))
        
        // Capture stdout
        val outputStream = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(outputStream))
        
        try {
            router.route(arrayOf("--help"))
            
            val output = outputStream.toString()
            assertTrue(output.contains("-h"), "Should document short form -h")
            assertTrue(output.contains("--help"), "Should document long form --help")
        } finally {
            System.setOut(originalOut)
        }
    }
    
    @Test
    fun `help message should be sorted for consistency`() {
        // Commands should be displayed in a consistent order
        val cmd1 = TestCommand("zebra", "Last alphabetically")
        val cmd2 = TestCommand("alpha", "First alphabetically")
        val cmd3 = TestCommand("middle", "Middle alphabetically")
        val router = CommandRouter(mapOf(
            "zebra" to cmd1,
            "alpha" to cmd2,
            "middle" to cmd3
        ))
        
        // Capture stdout
        val outputStream = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(outputStream))
        
        try {
            router.route(emptyArray())
            
            val output = outputStream.toString()
            val alphaIndex = output.indexOf("alpha")
            val middleIndex = output.indexOf("middle")
            val zebraIndex = output.indexOf("zebra")
            
            assertTrue(alphaIndex > 0, "Should contain alpha")
            assertTrue(middleIndex > 0, "Should contain middle")
            assertTrue(zebraIndex > 0, "Should contain zebra")
            assertTrue(alphaIndex < middleIndex, "alpha should come before middle")
            assertTrue(middleIndex < zebraIndex, "middle should come before zebra")
        } finally {
            System.setOut(originalOut)
        }
    }
}
