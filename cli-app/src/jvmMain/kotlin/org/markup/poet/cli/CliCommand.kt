package org.markup.poet.cli

/**
 * Base interface for all CLI commands.
 * 
 * Each command implements this interface to provide its functionality,
 * name, description, and help information.
 */
interface CliCommand {
    /**
     * The name of the command (e.g., "convert", "process")
     */
    val name: String
    
    /**
     * A brief description of what the command does
     */
    val description: String
    
    /**
     * Execute the command with the given arguments.
     * 
     * @param args The parsed command arguments
     * @return The result of command execution
     */
    fun execute(args: CommandArgs): CommandResult
    
    /**
     * Print detailed help information for this command
     */
    fun printHelp()
}

/**
 * Parsed command-line arguments.
 * 
 * @property positional List of positional arguments (non-option arguments)
 * @property options Map of option names to their values (e.g., --output file.txt)
 * @property flags Set of boolean flags (e.g., --verbose)
 */
data class CommandArgs(
    val positional: List<String>,
    val options: Map<String, String>,
    val flags: Set<String>
)

/**
 * Result of command execution.
 * 
 * Commands return either Success or Error to indicate the outcome.
 */
sealed class CommandResult {
    /**
     * Command executed successfully.
     * 
     * @property message Optional success message to display
     */
    data class Success(val message: String? = null) : CommandResult()
    
    /**
     * Command execution failed.
     * 
     * @property message Error message to display
     * @property exitCode Exit code for the process (default: 1)
     */
    data class Error(val message: String, val exitCode: Int = 1) : CommandResult()
}
