package org.markup.poet.antora.assembler.cli

import kotlin.system.exitProcess

/**
 * Main entry point for the Antora Document Assembler CLI tool.
 * 
 * This application provides a command-line interface for assembling
 * multiple AsciiDoc files from an Antora directory structure into
 * a single consolidated document.
 * 
 * Usage:
 *   antora-assembler <index-file> <output-file> [options]
 * 
 * Use --help for detailed usage information.
 */
fun main(args: Array<String>) {
    // Parse arguments
    val parsedArgs = ArgumentParser.parse(args)
    
    // Check for help flag
    if (parsedArgs.flags.contains("help") || parsedArgs.flags.contains("h") || args.isEmpty()) {
        printHelp()
        exitProcess(0)
    }
    
    // Create and execute the assemble command
    val command = AssembleCommand()
    val result = command.execute(parsedArgs)
    
    // Handle the result
    when (result) {
        is CommandResult.Success -> {
            result.message?.let { println(it) }
            exitProcess(0)
        }
        is CommandResult.Error -> {
            System.err.println(result.message)
            exitProcess(result.exitCode)
        }
    }
}

/**
 * Print general help information.
 */
private fun printHelp() {
    println("""
        |Antora Document Assembler
        |
        |Assembles multiple AsciiDoc files from an Antora directory structure into
        |a single consolidated document.
        |
        |Usage:
        |  antora-assembler <index-file> <output-file> [options]
        |
        |Arguments:
        |  index-file              Path to the index AsciiDoc file (entry point)
        |  output-file             Path to the output consolidated file
        |
        |Options:
        |  --component-root <path> Path to the Antora component root directory
        |                          (default: current directory)
        |  --max-depth <n>         Maximum include recursion depth (default: 50)
        |  --no-preserve-comments  Do not preserve comments in the output
        |  --allow-missing         Continue processing when includes are missing
        |  --allow-circular        Continue processing when circular dependencies
        |                          are detected
        |  -h, --help              Show this help message
        |
        |Examples:
        |  # Basic assembly
        |  antora-assembler docs/index.adoc output.adoc
        |
        |  # Specify component root
        |  antora-assembler docs/index.adoc output.adoc --component-root docs
        |
        |  # Allow missing includes
        |  antora-assembler index.adoc output.adoc --allow-missing
        |
        |Exit Codes:
        |  0  Success
        |  1  Assembly failed (errors occurred)
    """.trimMargin())
}
