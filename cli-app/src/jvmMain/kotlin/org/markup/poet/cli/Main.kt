package org.markup.poet.cli

import org.markup.poet.asciidoc.parser.AsciidocParser
import org.markup.poet.asciidoc.parser.DefaultAsciidocParser
import org.markup.poet.asciidoc.export.GraphvizAstVisitor
import org.markup.poet.asciidoc.export.DotBuilder
import org.markup.poet.asciidoc.export.ExportConfig
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        printUsage()
        exitProcess(1)
    }
    
    val inputFile = File(args[0]).absoluteFile
    val outputFile = if (args.size > 1) {
        File(args[1]).absoluteFile
    } else {
        File(inputFile.parentFile, "${inputFile.nameWithoutExtension}.dot")
    }
    
    if (!inputFile.exists()) {
        println("Error: Input file '${inputFile.path}' not found")
        println("Current directory: ${File(".").absolutePath}")
        exitProcess(1)
    }
    
    try {
        convertAsciidocToDot(inputFile, outputFile)
        println("Successfully converted '${inputFile.name}' to '${outputFile.absolutePath}'")
    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
        exitProcess(1)
    }
}

fun convertAsciidocToDot(inputFile: File, outputFile: File) {
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
    
    outputFile.writeText(dotContent)
}

fun printUsage() {
    println("""
        AsciiDoc to Graphviz DOT Converter
        
        Usage: asciidoc2dot <input.adoc> [output.dot]
        
        Arguments:
          input.adoc   - Input AsciiDoc file
          output.dot   - Output DOT file (optional, defaults to input name with .dot extension)
        
        Example:
          asciidoc2dot document.adoc
          asciidoc2dot document.adoc graph.dot
    """.trimIndent())
}
