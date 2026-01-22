package org.markup.poet.antora.assembler.cli

import org.markup.poet.antora.assembler.*
import org.markup.poet.antora.*
import org.markup.poet.asciidoc.parser.DefaultAsciidocParser

/**
 * CLI command for assembling Antora documents.
 * 
 * Usage:
 *   assemble <index-file> <output-file> [options]
 * 
 * Arguments:
 *   index-file              Path to the index AsciiDoc file
 *   output-file             Path to the output consolidated file
 * 
 * Options:
 *   --component-root <path> Path to the Antora component root (default: current directory)
 *   --max-depth <n>         Maximum include depth (default: 50)
 *   --no-preserve-comments  Do not preserve comments in output
 *   --allow-missing         Continue on missing includes (default: fail)
 *   --allow-circular        Continue on circular dependencies (default: fail)
 *   -h, --help              Show this help message
 */
class AssembleCommand : CliCommand {
    
    override val name: String = "assemble"
    
    override val description: String = "Assemble multiple Antora AsciiDoc files into a single document"
    
    override fun execute(args: CommandArgs): CommandResult {
        // Validate positional arguments
        if (args.positional.size < 2) {
            return CommandResult.Error(
                "Error: Missing required arguments\n\n" +
                "Usage: assemble <index-file> <output-file> [options]\n" +
                "Use 'assemble --help' for more information"
            )
        }
        
        val indexFile = args.positional[0]
        val outputFile = args.positional[1]
        
        // Parse options
        val componentRoot = args.options["component-root"] ?: "."
        val maxDepth = args.options["max-depth"]?.toIntOrNull() ?: 50
        val preserveComments = !args.flags.contains("no-preserve-comments")
        val failOnMissingIncludes = !args.flags.contains("allow-missing")
        val failOnCircularDependencies = !args.flags.contains("allow-circular")
        
        // Validate max depth
        if (maxDepth < 1) {
            return CommandResult.Error("Error: max-depth must be at least 1")
        }
        
        // Create configuration
        val config = AssemblerConfig(
            indexFile = indexFile,
            outputFile = outputFile,
            componentRoot = componentRoot,
            maxDepth = maxDepth,
            preserveComments = preserveComments,
            failOnMissingIncludes = failOnMissingIncludes,
            failOnCircularDependencies = failOnCircularDependencies
        )
        
        // Create dependencies
        val parser = DefaultAsciidocParser()
        val fileSystem = DefaultFileSystemAccess()
        val resolver = DefaultAntoraResolver(fileSystem)
        
        // Create assembler and execute
        val assembler = DefaultDocumentAssembler(parser, resolver, fileSystem)
        val result = assembler.assemble(config)
        
        // Handle result
        return if (result.success) {
            val message = buildString {
                appendLine("✓ Document assembled successfully")
                appendLine("  Output: ${result.outputPath}")
                appendLine("  Included files: ${result.includedFiles.size}")
                
                if (result.warnings.isNotEmpty()) {
                    appendLine()
                    appendLine("Warnings (${result.warnings.size}):")
                    result.warnings.forEach { warning ->
                        append("  - ${warning.message}")
                        if (warning.filePath != null) {
                            append(" (${warning.filePath}")
                            if (warning.lineNumber != null) {
                                append(":${warning.lineNumber}")
                            }
                            append(")")
                        }
                        appendLine()
                    }
                }
            }
            CommandResult.Success(message)
        } else {
            val message = buildString {
                appendLine("✗ Document assembly failed")
                appendLine()
                appendLine("Errors (${result.errors.size}):")
                result.errors.forEach { error ->
                    append("  - ${error.message}")
                    if (error.filePath != null) {
                        append(" (${error.filePath}")
                        if (error.lineNumber != null) {
                            append(":${error.lineNumber}")
                        }
                        append(")")
                    }
                    appendLine()
                }
                
                if (result.warnings.isNotEmpty()) {
                    appendLine()
                    appendLine("Warnings (${result.warnings.size}):")
                    result.warnings.forEach { warning ->
                        append("  - ${warning.message}")
                        if (warning.filePath != null) {
                            append(" (${warning.filePath}")
                            if (warning.lineNumber != null) {
                                append(":${warning.lineNumber}")
                            }
                            append(")")
                        }
                        appendLine()
                    }
                }
            }
            CommandResult.Error(message)
        }
    }
    
    override fun printHelp() {
        println("""
            |Assemble Command
            |
            |Assembles multiple AsciiDoc files from an Antora directory structure into
            |a single consolidated document. Resolves all include directives, handles
            |cross-references, and merges document attributes.
            |
            |Usage:
            |  assemble <index-file> <output-file> [options]
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
            |                          (default: fail on missing includes)
            |  --allow-circular        Continue processing when circular dependencies
            |                          are detected (default: fail on circular deps)
            |  -h, --help              Show this help message
            |
            |Examples:
            |  # Basic assembly
            |  assemble docs/index.adoc output.adoc
            |
            |  # Specify component root
            |  assemble docs/index.adoc output.adoc --component-root docs
            |
            |  # Allow missing includes and set max depth
            |  assemble index.adoc output.adoc --allow-missing --max-depth 100
            |
            |Exit Codes:
            |  0  Success
            |  1  Assembly failed (errors occurred)
        """.trimMargin())
    }
}
