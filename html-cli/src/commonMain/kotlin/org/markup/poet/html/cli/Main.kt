package org.markup.poet.html.cli

import org.markup.poet.asciidoc.parser.DefaultAsciidocParser
import org.markup.poet.asciidoc.render.*

// Platform-specific file operations
expect fun readFileContent(path: String): String
expect fun readFileBytes(path: String): ByteArray
expect fun writeFileContent(path: String, content: String)
expect fun fileExists(path: String): Boolean
expect fun exitProcess(code: Int): Nothing

fun main(args: Array<String>) {
    if (args.isEmpty() || args.contains("--help") || args.contains("-h")) {
        printHelp()
        exitProcess(if (args.isEmpty()) 1 else 0)
    }
    
    try {
        // Parse CLI arguments
        val cliOptions = parseArgs(args)
        
        println("[HTML-RENDERER] Converting AsciiDoc to HTML")
        println("[HTML-RENDERER] Input:  ${cliOptions.inputFile}")
        println("[HTML-RENDERER] Output: ${cliOptions.outputFile}")
        if (cliOptions.cssFile != null) {
            println("[HTML-RENDERER] CSS:    ${cliOptions.cssFile}")
        }
        if (cliOptions.theme != "default") {
            println("[HTML-RENDERER] Theme:  ${cliOptions.theme}")
        }
        println()
        
        // Read input file
        println("[HTML-RENDERER] Reading input file...")
        if (!fileExists(cliOptions.inputFile)) {
            printlnErr("✗ Error: Input file not found: ${cliOptions.inputFile}")
            exitProcess(1)
        }
        
        val content = readFileContent(cliOptions.inputFile)
        println("[HTML-RENDERER] Read ${content.length} bytes")
        
        // Parse AsciiDoc
        println("[HTML-RENDERER] Parsing AsciiDoc...")
        val parser = DefaultAsciidocParser()
        val parseResult = parser.parse(content)
        
        if (parseResult.errors.isNotEmpty()) {
            printlnErr("✗ Parse errors:")
            parseResult.errors.forEach { error ->
                printlnErr("  Line ${error.location.line}: ${error.message}")
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

        // Apply WASM extension plugins to custom blocks
        var document = parseResult.document
        if (cliOptions.plugins.isNotEmpty()) {
            println("[HTML-RENDERER] Loading ${cliOptions.plugins.size} plugin(s)...")
            val engine = org.markup.poet.plugin.engine.PluginEngine()
            cliOptions.plugins.forEach { pluginPath ->
                if (!fileExists(pluginPath)) {
                    printlnErr("✗ Error: Plugin not found: $pluginPath")
                    exitProcess(1)
                }
                val plugin = engine.loadPlugin(readFileBytes(pluginPath), pluginPath)
                println("[HTML-RENDERER] Loaded plugin '${plugin.descriptor.name}' (${plugin.id})")
            }
            val processed = org.markup.poet.plugin.integration.WasmBlockExtensions(engine).apply(document)
            processed.warnings.forEach { println("⚠ $it") }
            document = processed.document
            engine.unloadAll()
        }

        // Create CSS options from CLI arguments
        val cssOptions = createCssOptions(cliOptions)
        
        // Create render configuration
        val renderConfig = RenderConfig(
            cssOptions = cssOptions
        )
        
        // Render to HTML
        println("[HTML-RENDERER] Rendering to HTML...")
        val escaper = DefaultHtmlEscaper()
        val builder = DefaultHtmlBuilder(escaper)
        val inlineRenderer = DefaultInlineRenderer(builder)
        val blockRenderer = DefaultBlockRenderer(builder, inlineRenderer)
        val renderer = DefaultHtmlRenderer(blockRenderer, inlineRenderer)
        
        val renderResult = renderer.render(document, renderConfig)
        
        when {
            renderResult.isSuccess -> {
                val html = renderResult.getOrThrow()
                println("[HTML-RENDERER] Rendered ${html.length} bytes of HTML")
                
                // Write output
                println("[HTML-RENDERER] Writing output file...")
                writeFileContent(cliOptions.outputFile, html)
                
                println()
                println("✓ HTML generated successfully")
                println("  Output: ${cliOptions.outputFile}")
                println("  Size: ${html.length} bytes")
                exitProcess(0)
            }
            else -> {
                val error = renderResult.exceptionOrNull()
                printlnErr("✗ Rendering failed: ${error?.message}")
                
                // Provide specific error messages for CSS-related errors
                if (error is CssException.FileNotFound) {
                    printlnErr("  CSS file not found: ${error.path}")
                    printlnErr("  Please check that the file exists and the path is correct.")
                } else if (error is CssException.InvalidTheme) {
                    printlnErr("  Invalid theme: ${error.themeName}")
                    printlnErr("  Available themes: default, minimal, dark")
                }
                
                exitProcess(1)
            }
        }
        
    } catch (e: IllegalArgumentException) {
        // Handle CLI argument parsing errors
        printlnErr("✗ Error: ${e.message}")
        println()
        printHelp()
        exitProcess(1)
    } catch (e: Exception) {
        printlnErr("✗ Error: ${e.message}")
        exitProcess(1)
    }
}

/**
 * Creates CssOptions from CLI options.
 * 
 * Handles CSS file loading and validates CSS-related configuration.
 * 
 * @param cliOptions Parsed CLI options
 * @return CssOptions configured based on CLI arguments
 * @throws CssException.FileNotFound if CSS file doesn't exist
 */
internal fun createCssOptions(cliOptions: CliOptions): CssOptions {
    // Check if CSS file exists (if specified)
    if (cliOptions.cssFile != null && !fileExists(cliOptions.cssFile)) {
        throw CssException.FileNotFound(
            cliOptions.cssFile,
            "File does not exist"
        )
    }
    
    return CssOptions(
        customCssPath = cliOptions.cssFile,
        includeDefaultCss = !cliOptions.noDefaultCss,
        builtInTheme = cliOptions.theme,
        cssVariables = cliOptions.cssVariables
    )
}

// Helper function for stderr output
fun printlnErr(message: String) {
    println(message) // In native, both go to stdout, but we keep the semantic distinction
}

fun printHelp() {
    println("""
        |HTML Renderer - Convert AsciiDoc to HTML
        |
        |Usage:
        |  html-renderer <input-file> [output-file] [options]
        |
        |Arguments:
        |  input-file   Path to the AsciiDoc file (.adoc)
        |  output-file  Path to the output HTML file (optional)
        |               Default: input filename with .html extension
        |
        |Options:
        |  -h, --help              Show this help message
        |
        |CSS Styling Options:
        |  --css-file <path>       Path to custom CSS file to include in output
        |  --no-default-css        Disable default theme CSS (use only custom CSS)
        |  --theme <name>          Built-in theme to use (default, minimal, dark, kotlin)
        |                          Default: default
        |  --css-var <var>=<val>   Override CSS variable (can be used multiple times)
        |                          Example: --css-var --mp-color-primary=#007acc
        |  --plugin <path.wasm>    Load a WASM extension plugin (can be used multiple
        |                          times); custom blocks whose style a plugin claims
        |                          are replaced by the plugin's output (docs/PLUGINS.md)
        |
        |Examples:
        |  # Convert document.adoc to document.html with default styling
        |  html-renderer document.adoc
        |
        |  # Convert with custom output name
        |  html-renderer document.adoc output.html
        |
        |  # Use custom CSS file
        |  html-renderer document.adoc --css-file custom.css
        |
        |  # Use dark theme
        |  html-renderer document.adoc --theme dark
        |
        |  # Use custom CSS without default theme
        |  html-renderer document.adoc --css-file custom.css --no-default-css
        |
        |  # Override CSS variables
        |  html-renderer document.adoc --css-var --mp-color-primary=#ff0000
        |
        |  # Combine multiple options
        |  html-renderer document.adoc output.html --theme minimal --css-var --mp-font-size-base=18px
        |
        |Available Themes:
        |  default  - Standard styling with full feature set
        |  minimal  - Clean, minimal styling
        |  dark     - Dark color scheme for low-light environments
        |  kotlin   - Kotlin/neuroSKai design system with dark background and red accents
        |
        |CSS Variables:
        |  You can override any CSS variable defined in the theme. Common variables:
        |    --mp-color-primary      Primary accent color
        |    --mp-color-text         Main text color
        |    --mp-color-background   Background color
        |    --mp-font-family        Font family
        |    --mp-font-size-base     Base font size
        |    --mp-line-height        Line height
        |    --mp-spacing-unit       Base spacing unit
    """.trimMargin())
}

