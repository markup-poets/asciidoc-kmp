package org.markup.poet.asciidoc.processor

import org.markup.poet.cli.*
import org.markup.poet.asciidoc.parser.DefaultAsciidocParser
import org.markup.poet.asciidoc.processing.*
import kotlin.system.exitProcess

/**
 * Main entry point for the AsciiDoc CLI tool.
 * 
 * This application provides command-line tools for working with AsciiDoc documents:
 * - convert: Convert AsciiDoc to Graphviz DOT format for ASG visualization
 * - process: Process AsciiDoc documents with include resolution
 * 
 * The application uses a command router pattern to dispatch to appropriate
 * command handlers based on the subcommand provided.
 */
fun main(args: Array<String>) {
    // Create shared dependencies
    val parser = DefaultAsciidocParser()
    val fileReader = org.markup.poet.cli.JvmFileReader()
    
    // Create document processor with all its dependencies
    val includeResolver = DefaultIncludeResolver(parser)
    val fragmentProcessor = DefaultFragmentProcessor()
    val conditionalProcessor = DefaultConditionalProcessor()
    val attributeSubstitutor = DefaultAttributeSubstitutor()
    val macroExpander = DefaultMacroExpander()
    val admonitionProcessor = DefaultAdmonitionProcessor()
    val calloutProcessor = DefaultCalloutProcessor()
    val bibliographyManager = DefaultBibliographyManager()
    val crossReferenceResolver = DefaultCrossReferenceResolver()
    val tocGenerator = DefaultTocGenerator()
    val documentValidator = DefaultDocumentValidator()
    
    val documentProcessor = DefaultDocumentProcessor(
        includeResolver = includeResolver,
        fragmentProcessor = fragmentProcessor,
        conditionalProcessor = conditionalProcessor,
        attributeSubstitutor = attributeSubstitutor,
        macroExpander = macroExpander,
        admonitionProcessor = admonitionProcessor,
        calloutProcessor = calloutProcessor,
        bibliographyManager = bibliographyManager,
        crossReferenceResolver = crossReferenceResolver,
        tocGenerator = tocGenerator,
        documentValidator = documentValidator,
        fileReaderFactory = { basePath -> 
            // Create a file reader that resolves paths relative to the base path
            object : FileReader {
                override fun readFile(path: String): FileReadResult {
                    val resolvedPath = if (path.startsWith("/") || path.contains(":")) {
                        // Absolute path
                        path
                    } else {
                        // Relative path - resolve against base path
                        if (basePath.isEmpty()) path else "$basePath/$path"
                    }
                    return fileReader.readFile(resolvedPath)
                }
            }
        }
    )
    
    // Create command router with available commands
    val router = CommandRouter(
        commands = mapOf(
            "convert" to ConvertCommand(),
            "process" to ProcessCommand(fileReader, parser, documentProcessor)
        )
    )
    
    // Route the command and get the result
    val result = router.route(args)
    
    // Handle the result and set exit code
    when (result) {
        is CommandResult.Success -> {
            // Print success message if provided
            result.message?.let { println(it) }
            exitProcess(0)
        }
        is CommandResult.Error -> {
            // Print error message to stderr
            System.err.println(result.message)
            exitProcess(result.exitCode)
        }
    }
}
