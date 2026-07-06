package org.markup.poet.cli

import org.markup.poet.asciidoc.parser.AsciidocParser
import org.markup.poet.asciidoc.parser.AsgToLegacyAst
import org.markup.poet.asciidoc.processing.DocumentProcessor
import org.markup.poet.asciidoc.processing.FileReader
import org.markup.poet.asciidoc.processing.ProcessingConfig

/**
 * Command for processing AsciiDoc documents with include directive resolution.
 * 
 * This command parses an AsciiDoc file, resolves all include directives, and
 * produces a single concatenated output document with all includes embedded.
 * 
 * Usage:
 *   process <input.adoc> [options]
 * 
 * Options:
 *   -o, --output <file>       Output file path (default: stdout)
 *   -b, --base-path <path>    Base path for resolving includes (default: input file directory)
 *   --max-depth <n>           Maximum include nesting depth (default: 10)
 *   -v, --verbose             Display detailed processing information
 *   --no-overwrite            Fail if output file already exists
 */
class ProcessCommand(
    private val fileReader: FileReader,
    private val parser: AsciidocParser,
    private val documentProcessor: DocumentProcessor
) : CliCommand {
    
    override val name: String = "process"
    
    override val description: String = "Process AsciiDoc document with include resolution"
    
    override fun execute(args: CommandArgs): CommandResult {
        // Parse input file (required positional argument)
        val inputFile = args.positional.getOrNull(0)
            ?: return CommandResult.Error("Missing required argument: input file\n\nUse 'process --help' for usage information")
        
        // Parse output file option (-o, --output)
        val outputFile = args.options["output"] ?: args.options["o"]
        
        // Parse base path option (-b, --base-path)
        val basePath = args.options["base-path"] ?: args.options["b"]
        
        // Parse max depth option (--max-depth) and validate before checking file existence
        val maxDepthStr = args.options["max-depth"]
        val maxDepth = if (maxDepthStr != null) {
            maxDepthStr.toIntOrNull()
                ?: return CommandResult.Error("Invalid value for --max-depth: '$maxDepthStr' (must be a positive integer)")
        } else {
            10 // Default value
        }
        
        // Validate max depth is positive
        if (maxDepth <= 0) {
            return CommandResult.Error("Invalid value for --max-depth: $maxDepth (must be a positive integer)")
        }
        
        // Validate input file exists (after argument validation)
        val inputFileObj = java.io.File(inputFile)
        if (!inputFileObj.exists()) {
            return CommandResult.Error(ErrorFormatter.formatError(
                message = "Input file not found",
                filePath = inputFile
            ))
        }
        
        // Parse verbose flag (-v, --verbose)
        val verbose = args.flags.contains("verbose") || args.flags.contains("v")
        
        // Parse no-overwrite flag (--no-overwrite)
        val noOverwrite = args.flags.contains("no-overwrite")
        
        // Check if output file exists when no-overwrite flag is set
        if (noOverwrite && outputFile != null) {
            val outputFileObj = java.io.File(outputFile)
            if (outputFileObj.exists()) {
                return CommandResult.Error(ErrorFormatter.formatError(
                    message = "Output file already exists (use without --no-overwrite to overwrite)",
                    filePath = outputFile
                ))
            }
        }
        
        // Determine base path for resolving include directives
        // Use specified base path if provided, otherwise default to input file directory
        val resolvedBasePath = basePath ?: inputFileObj.parent ?: "."
        
        // Read input file content
        if (verbose) {
            System.err.println("Reading input file: $inputFile")
        }
        
        val fileContent = try {
            inputFileObj.readText()
        } catch (e: Exception) {
            return CommandResult.Error(ErrorFormatter.formatError(
                message = "Failed to read input file: ${e.message ?: "Unknown error"}",
                filePath = inputFile
            ))
        }
        
        // Parse AsciiDoc content to AST
        if (verbose) {
            System.err.println("Parsing AsciiDoc content...")
        }
        
        val parseResult = parser.parseToAsg(fileContent)
        
        // Report parse errors if any
        if (parseResult.errors.isNotEmpty()) {
            val errorMessages = ErrorFormatter.formatParseErrors(parseResult.errors, inputFile)
            return CommandResult.Error(errorMessages)
        }
        
        // Report parse warnings if verbose
        if (verbose && parseResult.warnings.isNotEmpty()) {
            ErrorFormatter.formatParseWarnings(parseResult.warnings, inputFile).forEach { warning ->
                System.err.println(warning)
            }
        }
        
        // Create ProcessingConfig with include resolution enabled
        val processingConfig = ProcessingConfig(
            enableIncludes = true,
            maxIncludeDepth = maxDepth,
            basePath = resolvedBasePath,
            enableAttributeSubstitution = false,
            enableCrossReferences = false,
            enableTocGeneration = false,
            enableMacroExpansion = false
        )
        
        if (verbose) {
            System.err.println("Processing configuration:")
            System.err.println("  Base path: $resolvedBasePath")
            System.err.println("  Max include depth: $maxDepth")
        }
        
        // Process document with include resolution
        if (verbose) {
            System.err.println("Processing document with include resolution...")
        }
        
        val processingResult = documentProcessor.process(parseResult.document, processingConfig)
        
        // Report warnings to stderr
        if (processingResult.warnings.isNotEmpty()) {
            ErrorFormatter.formatProcessingWarnings(processingResult.warnings).forEach { warning ->
                System.err.println(warning)
            }
        }
        
        // Report errors if any occurred
        if (processingResult.errors.isNotEmpty()) {
            val errorMessages = ErrorFormatter.formatProcessingErrors(processingResult.errors)
            return CommandResult.Error(errorMessages)
        }
        
        // Display processing summary if verbose
        if (verbose) {
            System.err.println("Processing completed successfully")
            System.err.println("  Warnings: ${processingResult.warnings.size}")
        }
        
        // Generate AsciiDoc output from processed AST
        if (verbose) {
            System.err.println("Generating AsciiDoc output...")
        }
        
        // Interim pipeline until the renderer migrates to the ASG (M3): the
        // processed ASG document is bridged back to the legacy AST for output.
        val prettyPrinter = AsciiDocPrettyPrinter()
        val outputContent = prettyPrinter.print(AsgToLegacyAst.convert(processingResult.document))
        
        // Write output to file or stdout
        val outputWriter: OutputWriter = if (outputFile != null) {
            FileOutputWriter(java.io.File(outputFile))
        } else {
            StdoutOutputWriter()
        }
        
        try {
            if (verbose && outputFile != null) {
                System.err.println("Writing output to: $outputFile")
            }
            
            outputWriter.write(outputContent)
            
            if (verbose && outputFile != null) {
                System.err.println("Output written successfully")
            }
            
            return CommandResult.Success()
        } catch (e: Exception) {
            return CommandResult.Error(ErrorFormatter.formatError(
                message = "Failed to write output: ${e.message ?: "Unknown error"}",
                filePath = outputFile
            ))
        }
    }
    
    override fun printHelp() {
        println("""
            Process AsciiDoc document with include resolution
            
            Usage:
              process <input.adoc> [options]
            
            Arguments:
              input.adoc    Input AsciiDoc file to process
            
            Options:
              -o, --output <file>       Output file path (writes to stdout if not specified)
              -b, --base-path <path>    Base path for resolving relative include paths
                                        (defaults to input file's directory)
              --max-depth <n>           Maximum include nesting depth (default: 10)
              -v, --verbose             Display detailed processing information
              --no-overwrite            Fail if output file already exists
            
            Description:
              Processes an AsciiDoc document by resolving all include directives
              and producing a single concatenated output document. Include directives
              are replaced with the content of the included files, supporting nested
              includes up to the specified maximum depth.
            
            Examples:
              process document.adoc
              process document.adoc -o output.adoc
              process document.adoc --base-path /path/to/includes
              process document.adoc -o output.adoc --max-depth 5 --verbose
              process document.adoc -o output.adoc --no-overwrite
        """.trimIndent())
    }
}
