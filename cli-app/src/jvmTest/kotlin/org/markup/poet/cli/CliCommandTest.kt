package org.markup.poet.cli

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CliCommandTest {
    
    @Test
    fun `CommandArgs should store positional arguments`() {
        val args = CommandArgs(
            positional = listOf("file1.txt", "file2.txt"),
            options = emptyMap(),
            flags = emptySet()
        )
        
        assertEquals(2, args.positional.size)
        assertEquals("file1.txt", args.positional[0])
        assertEquals("file2.txt", args.positional[1])
    }
    
    @Test
    fun `CommandArgs should store options`() {
        val args = CommandArgs(
            positional = emptyList(),
            options = mapOf("output" to "result.txt", "base-path" to "/tmp"),
            flags = emptySet()
        )
        
        assertEquals(2, args.options.size)
        assertEquals("result.txt", args.options["output"])
        assertEquals("/tmp", args.options["base-path"])
    }
    
    @Test
    fun `CommandArgs should store flags`() {
        val args = CommandArgs(
            positional = emptyList(),
            options = emptyMap(),
            flags = setOf("verbose", "no-overwrite")
        )
        
        assertEquals(2, args.flags.size)
        assertTrue(args.flags.contains("verbose"))
        assertTrue(args.flags.contains("no-overwrite"))
    }
    
    @Test
    fun `CommandResult Success should have optional message`() {
        val successWithMessage = CommandResult.Success("Operation completed")
        assertEquals("Operation completed", successWithMessage.message)
        
        val successWithoutMessage = CommandResult.Success()
        assertNull(successWithoutMessage.message)
    }
    
    @Test
    fun `CommandResult Error should have message and exit code`() {
        val error = CommandResult.Error("File not found", 2)
        assertEquals("File not found", error.message)
        assertEquals(2, error.exitCode)
    }
    
    @Test
    fun `CommandResult Error should default to exit code 1`() {
        val error = CommandResult.Error("Something went wrong")
        assertEquals("Something went wrong", error.message)
        assertEquals(1, error.exitCode)
    }
    
    @Test
    fun `CliCommand interface should be implementable`() {
        // Create a simple test implementation
        val testCommand = object : CliCommand {
            override val name = "test"
            override val description = "Test command"
            
            override fun execute(args: CommandArgs): CommandResult {
                return if (args.positional.isEmpty()) {
                    CommandResult.Error("No arguments provided")
                } else {
                    CommandResult.Success("Executed with ${args.positional.size} arguments")
                }
            }
            
            override fun printHelp() {
                println("Test command help")
            }
        }
        
        assertEquals("test", testCommand.name)
        assertEquals("Test command", testCommand.description)
        
        // Test execution with no arguments
        val errorResult = testCommand.execute(CommandArgs(emptyList(), emptyMap(), emptySet()))
        assertTrue(errorResult is CommandResult.Error)
        assertEquals("No arguments provided", (errorResult as CommandResult.Error).message)
        
        // Test execution with arguments
        val successResult = testCommand.execute(CommandArgs(listOf("arg1"), emptyMap(), emptySet()))
        assertTrue(successResult is CommandResult.Success)
        assertEquals("Executed with 1 arguments", (successResult as CommandResult.Success).message)
    }
}
