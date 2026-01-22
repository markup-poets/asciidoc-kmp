package org.markup.poet.antora.assembler.cli

/**
 * Simple argument parser for CLI commands.
 * 
 * Supports:
 * - Positional arguments
 * - Options with values: --option value, -o value
 * - Boolean flags: --flag, -f
 */
object ArgumentParser {
    
    /**
     * Parse command-line arguments.
     */
    fun parse(args: Array<String>): CommandArgs {
        val positional = mutableListOf<String>()
        val options = mutableMapOf<String, String>()
        val flags = mutableSetOf<String>()
        
        var i = 0
        while (i < args.size) {
            val arg = args[i]
            when {
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
        
        return CommandArgs(positional, options, flags)
    }
}
