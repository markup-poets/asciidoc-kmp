package org.markup.poet.wasm

import org.markup.poet.asciidoc.parser.DefaultAsciidocParser
import org.markup.poet.asciidoc.render.DefaultBlockRenderer
import org.markup.poet.asciidoc.render.DefaultHtmlBuilder
import org.markup.poet.asciidoc.render.DefaultHtmlEscaper
import org.markup.poet.asciidoc.render.DefaultHtmlRenderer
import org.markup.poet.asciidoc.render.DefaultInlineRenderer
import org.markup.poet.asciidoc.render.OutputOptions
import org.markup.poet.asciidoc.render.RenderConfig

/**
 * Browser entry point for asciidoc-kmp, compiled to WebAssembly.
 *
 * Converts AsciiDoc source to an HTML body fragment (no <html>/<head>
 * wrapper, no inline CSS) suitable for embedding in a host page.
 *
 * Throws with parse error details when the document cannot be parsed.
 */
@OptIn(ExperimentalJsExport::class)
@JsExport
fun convertToHtml(source: String): String {
    val parseResult = DefaultAsciidocParser().parse(source)
    if (parseResult.errors.isNotEmpty()) {
        throw IllegalArgumentException(
            parseResult.errors.joinToString("\n") { "Line ${it.line}: ${it.message}" }
        )
    }

    val builder = DefaultHtmlBuilder(DefaultHtmlEscaper())
    val inlineRenderer = DefaultInlineRenderer(builder)
    val renderer = DefaultHtmlRenderer(DefaultBlockRenderer(builder, inlineRenderer), inlineRenderer)

    val config = RenderConfig(
        outputOptions = OutputOptions(standalone = false, includeMetadata = false)
    )
    return renderer.render(parseResult.document, config).getOrThrow()
}

/** Library version, for display in host UIs. */
@OptIn(ExperimentalJsExport::class)
@JsExport
fun version(): String = "0.1.1"
