package org.markup.poet.cli

import org.markup.poet.asciidoc.parser.AsciidocParser
import org.markup.poet.asciidoc.parser.DefaultAsciidocParser
import org.markup.poet.asciidoc.export.GraphvizAstVisitor
import org.markup.poet.asciidoc.export.DotBuilder
import org.markup.poet.asciidoc.export.ExportConfig
import java.io.File

/**
 * Command for converting AsciiDoc documents to Graphviz DOT format.
 * 
 * This command parses an AsciiDoc file and generates a DOT file representing
 * the document's AST structure, which can be visualized using Graphviz tools.
 * 
 * Usage:
 *   convert <input.adoc> [output.dot]
 * 
 * If no output file is specified, defaults to <input-name>.dot in the same directory.
 */
class ConvertCommand(
    private val parser: AsciidocParser = DefaultAsciidocParser()
) : CliCommand {
    
    override val name: String = "convert"
    
    override val description: String = "Convert AsciiDoc to Graphviz DOT format"
    
    override fun execute(args: CommandArgs): CommandResult {
        // Validate we have an input file
        if (args.positional.isEmpty()) {
            return CommandResult.Error("Missing required argument: input file\n\nUse 'convert --help' for usage information")
        }
        
        val inputFile = File(args.positional[0]).absoluteFile
        
        // Check input file exists
        if (!inputFile.exists()) {
            return CommandResult.Error(ErrorFormatter.formatError(
                message = "Input file not found",
                filePath = inputFile.path
            ))
        }
        
        // Determine output file
        val outputFile = if (args.positional.size > 1) {
            File(args.positional[1]).absoluteFile
        } else {
            File(inputFile.parentFile, "${inputFile.nameWithoutExtension}.dot")
        }
        
        // Perform conversion
        return try {
            convertAsciidocToDot(inputFile, outputFile)
            CommandResult.Success("Successfully converted '${inputFile.name}' to '${outputFile.absolutePath}'")
        } catch (e: Exception) {
            CommandResult.Error(ErrorFormatter.formatError(
                message = "Conversion failed: ${e.message ?: "Unknown error"}"
            ))
        }
    }
    
    override fun printHelp() {
        println("""
            Convert AsciiDoc to Graphviz DOT format
            
            Usage:
              convert <input.adoc> [output.dot]
            
            Arguments:
              input.adoc    Input AsciiDoc file to convert
              output.dot    Output DOT file (optional, defaults to <input-name>.dot)
            
            Description:
              Parses an AsciiDoc document and generates a Graphviz DOT file
              representing the document's Abstract Syntax Tree (AST) structure.
              The DOT file can be visualized using Graphviz tools like 'dot'.
            
            Examples:
              convert document.adoc
              convert document.adoc output.dot
              convert path/to/file.adoc path/to/output.dot
        """.trimIndent())
    }
    
    /**
     * Convert an AsciiDoc file to Graphviz DOT format.
     * 
     * @param inputFile The input AsciiDoc file
     * @param outputFile The output DOT file
     */
    private fun convertAsciidocToDot(inputFile: File, outputFile: File) {
        // Read and parse the input file into the ASG model
        val source = inputFile.readText()
        val parseResult = parser.parse(source)
        
        // Report parse errors and warnings using consistent formatting
        if (parseResult.errors.isNotEmpty()) {
            System.err.println(ErrorFormatter.formatParseErrors(parseResult.errors, inputFile.path))
            System.err.println()
        }
        
        if (parseResult.warnings.isNotEmpty()) {
            ErrorFormatter.formatParseWarnings(parseResult.warnings, inputFile.path).forEach { warning ->
                System.err.println(warning)
            }
            System.err.println()
        }
        
        // Generate DOT representation
        val config = ExportConfig.default()
        val visitor = GraphvizAstVisitor(config)
        
        visitor.visit(parseResult.document)
        val graphData = visitor.getCollectedData(parseResult.document)
        
        val dotBuilder = DotBuilder(config)
        val dotContent = dotBuilder.buildDot(graphData)
        
        // Write output file
        outputFile.writeText(dotContent)
    }
}
