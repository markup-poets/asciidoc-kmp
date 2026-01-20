package org.markup.poet.cli

/**
 * Result of parsing command-line arguments.
 * 
 * @property subcommand The subcommand name if present (first non-option argument)
 * @property commandArgs The parsed arguments for the command
 */
data class ParsedArguments(
    val subcommand: String?,
    val commandArgs: CommandArgs
)

/**
 * Parser for command-line arguments.
 * 
 * Supports:
 * - Subcommands (first non-option argument)
 * - Positional arguments
 * - Options with values: --option value, -o value
 * - Boolean flags: --flag, -f
 * - Both short (-o) and long (--output) option forms
 * - Single dash (-) as positional argument (stdin convention)
 * 
 * Limitations:
 * - Option values that start with a dash will be treated as separate options/flags
 *   (e.g., "--output --file" will treat "--file" as a flag, not as the value for "--output")
 */
object ArgumentParser {
    
    /**
     * Parse command-line arguments.
     * 
     * The first non-option argument is treated as a subcommand.
     * Subsequent non-option arguments are positional arguments.
     * 
     * Options can be specified as:
     * - Long form with value: --option value
     * - Short form with value: -o value
     * - Long form as flag: --flag
     * - Short form as flag: -f
     * 
     * An option is treated as a flag if it's not followed by a value
     * (i.e., followed by another option or end of arguments).
     * 
     * @param args The command-line arguments array
     * @return ParsedArguments containing the subcommand and parsed arguments
     */
    fun parse(args: Array<String>): ParsedArguments {
        if (args.isEmpty()) {
            return ParsedArguments(null, CommandArgs(emptyList(), emptyMap(), emptySet()))
        }
        
        // Check if first argument is a subcommand (doesn't start with -)
        val subcommand = if (args[0].startsWith("-")) null else args[0]
        val startIndex = if (subcommand != null) 1 else 0
        
        val positional = mutableListOf<String>()
        val options = mutableMapOf<String, String>()
        val flags = mutableSetOf<String>()
        
        var i = startIndex
        while (i < args.size) {
            val arg = args[i]
            when {
                arg == "-" -> {
                    // Single dash is treated as positional (stdin convention)
                    positional.add(arg)
                    i++
                }
                arg.startsWith("--") -> {
                    // Long option form
                    val key = arg.substring(2)
                    if (i + 1 < args.size && !args[i + 1].startsWith("-")) {
                        // Has a value
                        options[key] = args[i + 1]
                        i += 2
                    } else {
                        // Boolean flag
                        flags.add(key)
                        i++
                    }
                }
                arg.startsWith("-") && arg.length > 1 -> {
                    // Short option form
                    val key = arg.substring(1)
                    if (i + 1 < args.size && !args[i + 1].startsWith("-")) {
                        // Has a value
                        options[key] = args[i + 1]
                        i += 2
                    } else {
                        // Boolean flag
                        flags.add(key)
                        i++
                    }
                }
                else -> {
                    // Positional argument
                    positional.add(arg)
                    i++
                }
            }
        }
        
        return ParsedArguments(subcommand, CommandArgs(positional, options, flags))
    }
}
