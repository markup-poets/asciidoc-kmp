package org.markup.poet.html.cli

/**
 * Configuration options for the HTML CLI tool.
 * 
 * Provides command-line interface options for customizing HTML rendering,
 * including input/output files and CSS styling options.
 * 
 * @param inputFile Path to the input AsciiDoc file (.adoc)
 * @param outputFile Path to the output HTML file (defaults to input filename with .html extension)
 * @param cssFile Path to custom CSS file to include in the output (optional)
 * @param noDefaultCss Whether to disable default theme CSS (false by default)
 * @param theme Name of built-in theme to use ("default", "minimal", "dark")
 * @param cssVariables Map of CSS variable overrides (e.g., "--mp-color-primary" to "#007acc")
 */
data class CliOptions(
    val inputFile: String,
    val outputFile: String,
    val cssFile: String? = null,
    val noDefaultCss: Boolean = false,
    val theme: String = "default",
    val cssVariables: Map<String, String> = emptyMap()
)

/**
 * Parses command-line arguments into CliOptions.
 * 
 * Supports the following flags:
 * - `--css-file <path>`: Path to custom CSS file
 * - `--no-default-css`: Disable default theme CSS
 * - `--theme <name>`: Built-in theme name (default, minimal, dark)
 * - `--css-var <variable>=<value>`: CSS variable override (can be used multiple times)
 * 
 * Positional arguments:
 * - First: input file (required)
 * - Second: output file (optional, defaults to input with .html extension)
 * 
 * @param args Command-line arguments array
 * @return Parsed CliOptions
 * @throws IllegalArgumentException if required arguments are missing or invalid
 */
fun parseArgs(args: Array<String>): CliOptions {
    var inputFile: String? = null
    var outputFile: String? = null
    var cssFile: String? = null
    var noDefaultCss = false
    var theme = "default"
    val cssVariables = mutableMapOf<String, String>()
    
    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--css-file" -> {
                i++
                if (i >= args.size) {
                    throw IllegalArgumentException("--css-file requires a path argument")
                }
                cssFile = args[i]
            }
            "--no-default-css" -> {
                noDefaultCss = true
            }
            "--theme" -> {
                i++
                if (i >= args.size) {
                    throw IllegalArgumentException("--theme requires a theme name")
                }
                theme = args[i]
            }
            "--css-var" -> {
                i++
                if (i >= args.size) {
                    throw IllegalArgumentException("--css-var requires variable=value")
                }
                val varDef = args[i]
                val parts = varDef.split("=", limit = 2)
                if (parts.size != 2) {
                    throw IllegalArgumentException("--css-var format must be: variable=value (got: $varDef)")
                }
                cssVariables[parts[0]] = parts[1]
            }
            else -> {
                // Positional arguments
                if (args[i].startsWith("--")) {
                    throw IllegalArgumentException("Unknown flag: ${args[i]}")
                }
                if (inputFile == null) {
                    inputFile = args[i]
                } else if (outputFile == null) {
                    outputFile = args[i]
                } else {
                    throw IllegalArgumentException("Too many positional arguments: ${args[i]}")
                }
            }
        }
        i++
    }
    
    // Validate required arguments
    if (inputFile == null) {
        throw IllegalArgumentException("Input file is required")
    }
    
    // Default output file if not specified
    if (outputFile == null) {
        outputFile = inputFile.removeSuffix(".adoc") + ".html"
    }
    
    // Validate theme name
    val validThemes = setOf("default", "minimal", "dark", "kotlin")
    if (theme !in validThemes) {
        throw IllegalArgumentException("Invalid theme: $theme. Available themes: ${validThemes.joinToString(", ")}")
    }
    
    return CliOptions(
        inputFile = inputFile,
        outputFile = outputFile,
        cssFile = cssFile,
        noDefaultCss = noDefaultCss,
        theme = theme,
        cssVariables = cssVariables
    )
}
