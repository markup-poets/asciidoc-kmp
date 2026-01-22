package org.markup.poet.html.cli

import org.markup.poet.asciidoc.parser.DefaultAsciidocParser
import org.markup.poet.asciidoc.render.*
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.isEmpty() || args.contains("--help") || args.contains("-h")) {
        printHelp()
        exitProcess(if (args.isEmpty()) 1 else 0)
    }
    
    val inputFile = args[0]
    val outputFile = if (args.size > 1) args[1] else inputFile.removeSuffix(".adoc") + ".html"
    
    println("[HTML-RENDERER] Converting AsciiDoc to HTML")
    println("[HTML-RENDERER] Input:  $inputFile")
    println("[HTML-RENDERER] Output: $outputFile")
    println()
    
    try {
        // Read input file
        println("[HTML-RENDERER] Reading input file...")
        val input = File(inputFile)
        if (!input.exists()) {
            System.err.println("✗ Error: Input file not found: $inputFile")
            exitProcess(1)
        }
        
        val content = input.readText()
        println("[HTML-RENDERER] Read ${content.length} bytes")
        
        // Parse AsciiDoc
        println("[HTML-RENDERER] Parsing AsciiDoc...")
        val parser = DefaultAsciidocParser()
        val parseResult = parser.parse(content)
        
        if (parseResult.errors.isNotEmpty()) {
            System.err.println("✗ Parse errors:")
            parseResult.errors.forEach { error ->
                System.err.println("  Line ${error.location.line}: ${error.message}")
            }
            exitProcess(1)
        }
        
        if (parseResult.warnings.isNotEmpty()) {
            println("⚠ Parse warnings:")
            parseResult.warnings.forEach { warning ->
                println("  Line ${warning.location.line}: ${warning.message}")
            }
        }
        
        println("[HTML-RENDERER] Parse complete")
        
        // Render to HTML
        println("[HTML-RENDERER] Rendering to HTML...")
        val escaper = DefaultHtmlEscaper()
        val builder = DefaultHtmlBuilder(escaper)
        val inlineRenderer = DefaultInlineRenderer(builder)
        val blockRenderer = DefaultBlockRenderer(builder, inlineRenderer)
        val renderer = DefaultHtmlRenderer(blockRenderer, inlineRenderer, escaper)
        
        val renderResult = renderer.render(parseResult.document)
        
        when {
            renderResult.isSuccess -> {
                val html = renderResult.getOrThrow()
                println("[HTML-RENDERER] Rendered ${html.length} bytes of HTML")
                
                // Wrap in basic HTML structure
                val fullHtml = buildString {
                    appendLine("<!DOCTYPE html>")
                    appendLine("<html lang=\"en\">")
                    appendLine("<head>")
                    appendLine("    <meta charset=\"UTF-8\">")
                    appendLine("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
                    appendLine("    <title>${parseResult.document.title ?: "Document"}</title>")
                    appendLine("    <style>")
                    appendLine(getDefaultCSS())
                    appendLine("    </style>")
                    appendLine("</head>")
                    appendLine("<body>")
                    appendLine(html)
                    appendLine("</body>")
                    appendLine("</html>")
                }
                
                // Write output
                println("[HTML-RENDERER] Writing output file...")
                File(outputFile).writeText(fullHtml)
                
                println()
                println("✓ HTML generated successfully")
                println("  Output: $outputFile")
                println("  Size: ${fullHtml.length} bytes")
                exitProcess(0)
            }
            else -> {
                System.err.println("✗ Rendering failed: ${renderResult.exceptionOrNull()?.message}")
                renderResult.exceptionOrNull()?.printStackTrace()
                exitProcess(1)
            }
        }
        
    } catch (e: Exception) {
        System.err.println("✗ Error: ${e.message}")
        e.printStackTrace()
        exitProcess(1)
    }
}

fun printHelp() {
    println("""
        |HTML Renderer - Convert AsciiDoc to HTML
        |
        |Usage:
        |  html-renderer <input-file> [output-file]
        |
        |Arguments:
        |  input-file   Path to the AsciiDoc file (.adoc)
        |  output-file  Path to the output HTML file (optional)
        |               Default: input filename with .html extension
        |
        |Options:
        |  -h, --help   Show this help message
        |
        |Examples:
        |  # Convert document.adoc to document.html
        |  html-renderer document.adoc
        |
        |  # Convert with custom output name
        |  html-renderer document.adoc output.html
        |
        |  # Convert assembled article
        |  html-renderer article1/assembled.adoc article1/index.html
    """.trimMargin())
}

fun getDefaultCSS(): String {
    return """
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
            line-height: 1.6;
            max-width: 900px;
            margin: 0 auto;
            padding: 20px;
            color: #333;
        }
        
        h1, h2, h3, h4, h5, h6 {
            margin-top: 1.5em;
            margin-bottom: 0.5em;
            font-weight: 600;
            line-height: 1.3;
        }
        
        h1 { font-size: 2.5em; border-bottom: 2px solid #eee; padding-bottom: 0.3em; }
        h2 { font-size: 2em; border-bottom: 1px solid #eee; padding-bottom: 0.3em; }
        h3 { font-size: 1.5em; }
        h4 { font-size: 1.25em; }
        h5 { font-size: 1.1em; }
        h6 { font-size: 1em; }
        
        p {
            margin: 1em 0;
        }
        
        code {
            background-color: #f5f5f5;
            padding: 2px 6px;
            border-radius: 3px;
            font-family: 'Courier New', Courier, monospace;
            font-size: 0.9em;
        }
        
        pre {
            background-color: #f5f5f5;
            padding: 15px;
            border-radius: 5px;
            overflow-x: auto;
            border-left: 3px solid #007acc;
        }
        
        pre code {
            background-color: transparent;
            padding: 0;
        }
        
        ul, ol {
            margin: 1em 0;
            padding-left: 2em;
        }
        
        li {
            margin: 0.5em 0;
        }
        
        a {
            color: #007acc;
            text-decoration: none;
        }
        
        a:hover {
            text-decoration: underline;
        }
        
        strong {
            font-weight: 600;
        }
        
        em {
            font-style: italic;
        }
        
        img {
            max-width: 100%;
            height: auto;
        }
        
        .admonitionblock {
            margin: 1.5em 0;
            padding: 15px;
            border-left: 4px solid #007acc;
            background-color: #f0f8ff;
        }
        
        .admonitionblock.note { border-color: #007acc; background-color: #f0f8ff; }
        .admonitionblock.tip { border-color: #28a745; background-color: #f0fff4; }
        .admonitionblock.warning { border-color: #ffc107; background-color: #fffbf0; }
        .admonitionblock.caution { border-color: #ff9800; background-color: #fff8f0; }
        .admonitionblock.important { border-color: #dc3545; background-color: #fff0f0; }
        
        .admonitionblock .title {
            font-weight: 600;
            margin-bottom: 0.5em;
        }
        
        section {
            margin: 2em 0;
        }
    """.trimIndent()
}
