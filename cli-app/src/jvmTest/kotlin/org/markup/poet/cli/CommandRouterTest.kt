package org.markup.poet.cli

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertIs

/**
 * Unit tests for CommandRouter.
 * 
 * Tests command routing logic, backward compatibility, and help display.
 */
class CommandRouterTest {
    
    // Mock command for testing
    private class MockCommand(
        override val name: String,
        override val description: String = "Mock command for testing",
        private val executeResult: CommandResult = CommandResult.Success("Mock executed")
    ) : CliCommand {
        var executeCalled = false
        var lastArgs: CommandArgs? = null
        var helpCalled = false
        
        override fun execute(args: CommandArgs): CommandResult {
            executeCalled = true
            lastArgs = args
            return executeResult
        }
        
        override fun printHelp() {
            helpCalled = true
        }
    }
    
    @Test
    fun `should show help when no arguments provided`() {
        val router = CommandRouter(emptyMap())
        val result = router.route(emptyArray())
        
        assertIs<CommandResult.Success>(result)
    }
    
    @Test
    fun `should show help when --help flag provided`() {
        val mockCommand = MockCommand("test")
        val router = CommandRouter(mapOf("test" to mockCommand))
        
        val result = router.route(arrayOf("--help"))
        
        assertIs<CommandResult.Success>(result)
        assertEquals(false, mockCommand.executeCalled, "Command should not be executed when --help is provided")
    }
    
    @Test
    fun `should show help when -h flag provided`() {
        val mockCommand = MockCommand("test")
        val router = CommandRouter(mapOf("test" to mockCommand))
        
        val result = router.route(arrayOf("-h"))
        
        assertIs<CommandResult.Success>(result)
        assertEquals(false, mockCommand.executeCalled, "Command should not be executed when -h is provided")
    }
    
    @Test
    fun `should route to explicit subcommand`() {
        val mockCommand = MockCommand("test")
        val router = CommandRouter(mapOf("test" to mockCommand))
        
        val result = router.route(arrayOf("test", "arg1", "arg2"))
        
        assertTrue(mockCommand.executeCalled, "Command should be executed")
        assertEquals(listOf("arg1", "arg2"), mockCommand.lastArgs?.positional)
        assertIs<CommandResult.Success>(result)
    }
    
    @Test
    fun `should default to convert command when no subcommand but file argument provided`() {
        val convertCommand = MockCommand("convert")
        val processCommand = MockCommand("process")
        val router = CommandRouter(mapOf(
            "convert" to convertCommand,
            "process" to processCommand
        ))
        
        val result = router.route(arrayOf("input.adoc", "output.dot"))
        
        assertTrue(convertCommand.executeCalled, "Convert command should be executed")
        assertEquals(false, processCommand.executeCalled, "Process command should not be executed")
        assertEquals(listOf("input.adoc", "output.dot"), convertCommand.lastArgs?.positional)
    }
    
    @Test
    fun `should route to process command when explicitly specified`() {
        val convertCommand = MockCommand("convert")
        val processCommand = MockCommand("process")
        val router = CommandRouter(mapOf(
            "convert" to convertCommand,
            "process" to processCommand
        ))
        
        val result = router.route(arrayOf("process", "input.adoc", "output.adoc"))
        
        assertTrue(processCommand.executeCalled, "Process command should be executed")
        assertEquals(false, convertCommand.executeCalled, "Convert command should not be executed")
        assertEquals(listOf("input.adoc", "output.adoc"), processCommand.lastArgs?.positional)
    }
    
    @Test
    fun `should return error for unknown command`() {
        val router = CommandRouter(mapOf("convert" to MockCommand("convert")))
        
        val result = router.route(arrayOf("unknown", "arg"))
        
        assertIs<CommandResult.Error>(result)
        assertTrue(result.message.contains("Unknown command: unknown"))
    }
    
    @Test
    fun `should pass options to command`() {
        val mockCommand = MockCommand("test")
        val router = CommandRouter(mapOf("test" to mockCommand))
        
        router.route(arrayOf("test", "--output", "file.txt", "-v"))
        
        assertEquals(mapOf("output" to "file.txt"), mockCommand.lastArgs?.options)
        assertTrue(mockCommand.lastArgs?.flags?.contains("v") == true)
    }
    
    @Test
    fun `should pass flags to command`() {
        val mockCommand = MockCommand("test")
        val router = CommandRouter(mapOf("test" to mockCommand))
        
        router.route(arrayOf("test", "--verbose", "--no-overwrite"))
        
        assertTrue(mockCommand.lastArgs?.flags?.contains("verbose") == true)
        assertTrue(mockCommand.lastArgs?.flags?.contains("no-overwrite") == true)
    }
    
    @Test
    fun `should show command-specific help when subcommand --help provided`() {
        val mockCommand = MockCommand("test")
        val router = CommandRouter(mapOf("test" to mockCommand))
        
        val result = router.route(arrayOf("test", "--help"))
        
        assertTrue(mockCommand.helpCalled, "Command's printHelp should be called")
        assertEquals(false, mockCommand.executeCalled, "Command should not be executed")
        assertIs<CommandResult.Success>(result)
    }
    
    @Test
    fun `should handle options with values in backward compatibility mode`() {
        val convertCommand = MockCommand("convert")
        val router = CommandRouter(mapOf("convert" to convertCommand))
        
        router.route(arrayOf("input.adoc", "--output", "output.dot"))
        
        assertTrue(convertCommand.executeCalled, "Convert command should be executed")
        assertEquals(listOf("input.adoc"), convertCommand.lastArgs?.positional)
        assertEquals(mapOf("output" to "output.dot"), convertCommand.lastArgs?.options)
    }
    
    @Test
    fun `should return command execution result`() {
        val errorCommand = MockCommand(
            "error",
            executeResult = CommandResult.Error("Test error", 42)
        )
        val router = CommandRouter(mapOf("error" to errorCommand))
        
        val result = router.route(arrayOf("error"))
        
        assertIs<CommandResult.Error>(result)
        assertEquals("Test error", result.message)
        assertEquals(42, result.exitCode)
    }
    
    @Test
    fun `should handle multiple commands registered`() {
        val cmd1 = MockCommand("cmd1")
        val cmd2 = MockCommand("cmd2")
        val cmd3 = MockCommand("cmd3")
        val router = CommandRouter(mapOf(
            "cmd1" to cmd1,
            "cmd2" to cmd2,
            "cmd3" to cmd3
        ))
        
        router.route(arrayOf("cmd2", "arg"))
        
        assertEquals(false, cmd1.executeCalled)
        assertTrue(cmd2.executeCalled)
        assertEquals(false, cmd3.executeCalled)
    }
}
