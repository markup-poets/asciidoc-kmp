package org.markup.poet.asciidoc.processor

import org.markup.poet.asciidoc.parser.AsciidocParser
import org.markup.poet.asciidoc.parser.DefaultAsciidocParser
import org.markup.poet.asciidoc.export.GraphvizAstVisitor
import org.markup.poet.asciidoc.export.DotBuilder
import org.markup.poet.asciidoc.export.ExportConfig
import org.markup.poet.asciidoc.processing.*
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        printUsage()
        exitProcess(1)
    }

    val subcommand = if (args[0] == "convert" || args[0] == "process") args[0] else null
    val remainingArgs = if (subcommand != null) args.drop(1) else args.toList()

    if (remainingArgs.isEmpty()) {
        if (args[0] == "--help" || args[0] == "-h") {
            printUsage()
            exitProcess(0)
        }
        println("Error: No input file specified")
        printUsage()
        exitProcess(1)
    }

    val inputFile = File(remainingArgs[0]).absoluteFile
    if (!inputFile.exists()) {
        println("Error: Input file '${inputFile.path}' not found")
        exitProcess(1)
    }

    // Default command is 'convert' if not specified
    val effectiveCommand = subcommand ?: "convert"

    try {
        when (effectiveCommand) {
            "convert" -> {
                val outputFile = if (remainingArgs.size > 1) {
                    File(remainingArgs[1]).absoluteFile
                } else {
                    File(inputFile.parentFile, "${inputFile.nameWithoutExtension}.dot")
                }
                convertAsciidocToDot(inputFile, outputFile)
                println("Successfully converted '${inputFile.name}' to '${outputFile.absolutePath}'")
            }
            "process" -> {
                val outputFile = if (remainingArgs.size > 1) {
                    File(remainingArgs[1]).absoluteFile
                } else {
                    null // Defaults to stdout
                }
                processAsciidoc(inputFile, outputFile)
            }
        }
    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
        exitProcess(1)
    }
}

fun processAsciidoc(inputFile: File, outputFile: File?) {
    val parser: AsciidocParser = DefaultAsciidocParser()
    val source = inputFile.readText()
    val parseResult = parser.parse(source)

    if (parseResult.errors.isNotEmpty()) {
        println("Parse errors in '${inputFile.name}':")
        parseResult.errors.forEach { error ->
            println("  Line ${error.location.line}: ${error.message}")
        }
        exitProcess(1)
    }

    val processor = DefaultDocumentProcessor(
        includeResolver = DefaultIncludeResolver(parser),
        attributeSubstitutor = DefaultAttributeSubstitutor(),
        macroExpander = DefaultMacroExpander(),
        crossReferenceResolver = DefaultCrossReferenceResolver(),
        tocGenerator = DefaultTocGenerator(),
        documentValidator = DefaultDocumentValidator(),
        fileReaderFactory = { _ -> JvmFileReader() }
    )

    val includeConfig = ProcessingConfig(
        enableIncludes = true,
        maxIncludeDepth = 10,
        basePath = inputFile.parentFile?.absolutePath ?: ".",
        enableAttributeSubstitution = true,
        enableCrossReferences = true,
        enableMacroExpansion = true,
        enableTocGeneration = false
    )

    val result = processor.process(parseResult.document, includeConfig)

    if (result.errors.isNotEmpty()) {
        println("Processing errors:")
        result.errors.forEach { error ->
            println("  ${error.severity} [${error.location.line}]: ${error.message}")
        }
        if (result.errors.any { it.severity == ErrorSeverity.FATAL || it.severity == ErrorSeverity.ERROR }) {
            exitProcess(1)
        }
    }

    val renderer: AsciidocRenderer = DefaultAsciidocRenderer()
    val processedContent = renderer.render(result.document)

    if (outputFile != null) {
        outputFile.parentFile?.mkdirs()
        outputFile.writeText(processedContent)
        println("Successfully processed '${inputFile.name}' to '${outputFile.absolutePath}'")
    } else {
        println(processedContent)
    }
}

fun convertAsciidocToDot(inputFile: File, outputFile: File?) {
    val parser: AsciidocParser = DefaultAsciidocParser()
    val source = inputFile.readText()
    
    val parseResult = parser.parse(source)
    
    if (parseResult.errors.isNotEmpty()) {
        println("Parse errors:")
        parseResult.errors.forEach { error ->
            println("  Line ${error.location.line}: ${error.message}")
        }
    }
    
    if (parseResult.warnings.isNotEmpty()) {
        println("Parse warnings:")
        parseResult.warnings.forEach { warning ->
            println("  Line ${warning.location.line}: ${warning.message}")
        }
    }
    
    val config = ExportConfig.default()
    val visitor = GraphvizAstVisitor(config)
    
    visitor.visit(parseResult.document)
    val graphData = visitor.getCollectedData(parseResult.document)
    
    val dotBuilder = DotBuilder(config)
    val dotContent = dotBuilder.buildDot(graphData)
    
    outputFile?.writeText(dotContent)
}

fun printUsage() {
    println("""
        AsciiDoc Toolset
        
        Usage: 
          asciidoc-tool <command> [arguments]
          
        Commands:
          convert <input.adoc> [output.dot]   - Convert AsciiDoc to Graphviz DOT
          process <input.adoc> [output.adoc]  - Process includes, attributes, and xrefs
          
        Default (for backward compatibility):
          asciidoc-tool <input.adoc> [output.dot] -> equivalent to 'convert'
          
        Examples:
          asciidoc-tool convert document.adoc
          asciidoc-tool process document.adoc consolidated.adoc
    """.trimIndent())
}
