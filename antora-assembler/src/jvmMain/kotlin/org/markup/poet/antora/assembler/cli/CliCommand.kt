package org.markup.poet.antora.assembler.cli

/**
 * Base interface for CLI commands.
 */
interface CliCommand {
    /**
     * The name of the command
     */
    val name: String
    
    /**
     * A brief description of what the command does
     */
    val description: String
    
    /**
     * Execute the command with the given arguments.
     */
    fun execute(args: CommandArgs): CommandResult
    
    /**
     * Print detailed help information for this command
     */
    fun printHelp()
}

/**
 * Parsed command-line arguments.
 */
data class CommandArgs(
    val positional: List<String>,
    val options: Map<String, String>,
    val flags: Set<String>
)

/**
 * Result of command execution.
 */
sealed class CommandResult {
    data class Success(val message: String? = null) : CommandResult()
    data class Error(val message: String, val exitCode: Int = 1) : CommandResult()
}
