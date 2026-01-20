package org.markup.poet.cli

/**
 * Routes command-line arguments to the appropriate command handler.
 * 
 * The CommandRouter is responsible for:
 * - Parsing command-line arguments using ArgumentParser
 * - Determining which command to execute based on the subcommand
 * - Handling backward compatibility (no subcommand defaults to convert)
 * - Displaying help information when no arguments are provided
 * - Executing the selected command and returning the result
 * 
 * Backward Compatibility:
 * When no subcommand is provided but a file argument is given, the router
 * defaults to the "convert" command for backward compatibility with the
 * original CLI behavior.
 * 
 * Example usage:
 * ```
 * val router = CommandRouter(mapOf(
 *     "convert" to ConvertCommand(),
 *     "process" to ProcessCommand()
 * ))
 * val result = router.route(args)
 * ```
 */
class CommandRouter(
    private val commands: Map<String, CliCommand>
) {
    /**
     * Route command-line arguments to the appropriate command.
     * 
     * Routing logic:
     * 1. If no arguments provided, show help
     * 2. If --help or -h flag present at top level (no subcommand), show help
     * 3. If subcommand is explicitly specified and valid, use that command
     * 4. If subcommand looks like a file path (has extension or separator), treat as positional arg and default to "convert"
     * 5. If subcommand is not a valid command name and doesn't look like a file, return error
     * 6. If no subcommand but positional arguments exist, default to "convert" (backward compatibility)
     * 7. Otherwise, show help
     * 
     * @param args The command-line arguments array
     * @return CommandResult indicating success or failure
     */
    fun route(args: Array<String>): CommandResult {
        // Parse arguments
        val parsedArgs = ArgumentParser.parse(args)
        
        // Check for help flag at top level (no subcommand)
        if (parsedArgs.subcommand == null && 
            (parsedArgs.commandArgs.flags.contains("help") || 
             parsedArgs.commandArgs.flags.contains("h"))) {
            return showHelp()
        }
        
        // Determine which command to execute
        val commandName: String
        val commandArgs: CommandArgs
        
        when {
            // Explicit subcommand provided and it's a valid command
            parsedArgs.subcommand != null && commands.containsKey(parsedArgs.subcommand) -> {
                commandName = parsedArgs.subcommand
                commandArgs = parsedArgs.commandArgs
            }
            
            // Subcommand provided but not a valid command
            parsedArgs.subcommand != null && !commands.containsKey(parsedArgs.subcommand) -> {
                // Check if it looks like a file path (has extension or path separator)
                val looksLikeFile = parsedArgs.subcommand.contains('.') || 
                                   parsedArgs.subcommand.contains('/') ||
                                   parsedArgs.subcommand.contains('\\')
                
                if (looksLikeFile) {
                    // Treat as file path for backward compatibility
                    commandName = "convert"
                    commandArgs = CommandArgs(
                        positional = listOf(parsedArgs.subcommand) + parsedArgs.commandArgs.positional,
                        options = parsedArgs.commandArgs.options,
                        flags = parsedArgs.commandArgs.flags
                    )
                } else {
                    // Looks like a command attempt, return error
                    return CommandResult.Error("Unknown command: ${parsedArgs.subcommand}\n\n${getHelpText()}")
                }
            }
            
            // No subcommand but has positional arguments - default to convert
            parsedArgs.commandArgs.positional.isNotEmpty() -> {
                commandName = "convert"
                commandArgs = parsedArgs.commandArgs
            }
            
            // No subcommand and no positional arguments - show help
            else -> return showHelp()
        }
        
        // Look up the command
        val command = commands[commandName]
        if (command == null) {
            return CommandResult.Error("Unknown command: $commandName\n\n${getHelpText()}")
        }
        
        // Check if command-specific help was requested
        if (commandArgs.flags.contains("help") || commandArgs.flags.contains("h")) {
            command.printHelp()
            return CommandResult.Success()
        }
        
        // Execute the command
        return command.execute(commandArgs)
    }
    
    /**
     * Display help information and return success result.
     * 
     * @return CommandResult.Success after displaying help
     */
    private fun showHelp(): CommandResult {
        println(getHelpText())
        return CommandResult.Success()
    }
    
    /**
     * Generate help text showing all available commands.
     * 
     * @return Formatted help text string
     */
    private fun getHelpText(): String {
        val sb = StringBuilder()
        sb.appendLine("AsciiDoc CLI Tool")
        sb.appendLine()
        sb.appendLine("Usage:")
        sb.appendLine("  asciidoc-tool <command> [arguments]")
        sb.appendLine("  asciidoc-tool <input.adoc> [output.dot]  (defaults to 'convert' for backward compatibility)")
        sb.appendLine()
        sb.appendLine("Commands:")
        
        // Sort commands by name for consistent output
        commands.values.sortedBy { it.name }.forEach { cmd ->
            sb.appendLine("  ${cmd.name.padEnd(15)} ${cmd.description}")
        }
        
        sb.appendLine()
        sb.appendLine("Options:")
        sb.appendLine("  -h, --help     Show this help message")
        sb.appendLine()
        sb.appendLine("Use '<command> --help' for more information about a specific command")
        
        return sb.toString()
    }
}
