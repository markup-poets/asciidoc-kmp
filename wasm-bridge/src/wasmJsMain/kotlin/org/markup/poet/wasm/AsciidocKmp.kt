@file:OptIn(ExperimentalJsExport::class, kotlin.js.ExperimentalWasmJsInterop::class)

package org.markup.poet.wasm

import kotlin.js.Promise
import org.markup.poet.asciidoc.parser.DefaultAsciidocParser
import org.markup.poet.asciidoc.render.DefaultBlockRenderer
import org.markup.poet.asciidoc.render.DefaultHtmlBuilder
import org.markup.poet.asciidoc.render.DefaultHtmlEscaper
import org.markup.poet.asciidoc.render.DefaultHtmlRenderer
import org.markup.poet.asciidoc.render.DefaultInlineRenderer
import org.markup.poet.asciidoc.render.OutputOptions
import org.markup.poet.asciidoc.render.RenderConfig
import org.markup.poet.plugin.engine.BrowserPluginEngine
import org.markup.poet.plugin.integration.WasmExtensions

/*
 * Browser entry point for asciidoc-kmp, compiled to WebAssembly.
 *
 * Without plugins the output is the neutral core rendering, which loads
 * nothing from third parties. A page opts into presentation features by
 * loading plugins first:
 *
 *   await enableStandardPlugins("icons,math,video", "");
 *   const { html, head, warnings } = convert(source);
 *
 * `html` is a body fragment; `head` is the markup the plugins need in the
 * page's <head> (a stylesheet link, a script). The page decides whether and
 * where to place it.
 */

private val engine = BrowserPluginEngine()

/** One conversion: the HTML fragment, the plugins' head markup, and plugin warnings. */
private class Conversion(val html: String, val head: List<String>, val warnings: List<String>)

private fun runConversion(source: String): Conversion {
    val parseResult = DefaultAsciidocParser().parse(source)
    if (parseResult.errors.isNotEmpty()) {
        throw IllegalArgumentException(
            parseResult.errors.joinToString("\n") { "Line ${it.line}: ${it.message}" }
        )
    }

    // With no plugin loaded the pass has nothing to claim; skip it.
    val processed = if (engine.descriptors().isEmpty()) null else WasmExtensions(engine).apply(parseResult.document)

    val builder = DefaultHtmlBuilder(DefaultHtmlEscaper())
    val inlineRenderer = DefaultInlineRenderer(builder)
    val renderer = DefaultHtmlRenderer(DefaultBlockRenderer(builder, inlineRenderer), inlineRenderer)
    val config = RenderConfig(
        outputOptions = OutputOptions(standalone = false, includeMetadata = false)
    )
    val html = renderer.render(processed?.document ?: parseResult.document, config).getOrThrow()
    return Conversion(html, processed?.headContent.orEmpty(), processed?.warnings.orEmpty())
}

/**
 * Converts AsciiDoc source to an HTML body fragment (no <html>/<head>
 * wrapper, no inline CSS) suitable for embedding in a host page. Loaded
 * plugins are applied; use [convert] to also get the head markup they need.
 *
 * Throws with parse error details when the document cannot be parsed.
 */
@JsExport
fun convertToHtml(source: String): String = runConversion(source).html

/**
 * Converts AsciiDoc source and returns `{ html, head, warnings }`: the body
 * fragment, the markup loaded plugins want in the page's `<head>` (one entry
 * per line, empty without plugins), and an array of plugin warnings.
 */
@JsExport
fun convert(source: String): JsAny {
    val conversion = runConversion(source)
    val warnings = newArray()
    conversion.warnings.forEach { push(warnings, it) }
    return conversionResult(conversion.html, conversion.head.joinToString("\n"), warnings)
}

/**
 * Loads standard plugins by key, comma-separated: `icons` (Font Awesome),
 * `math` (MathJax), `video` (YouTube and Vimeo players). They make the page
 * load resources from third parties, which is why they are off until asked
 * for.
 *
 * The plugins are separate `.wasm` files, fetched on demand so that pages
 * that never use them do not download them. [baseUrl] is the directory they
 * are served from; the distribution ships them in `plugins/` next to the
 * bundle, and an empty [baseUrl] means `plugins/` relative to the page.
 *
 * Resolves with the keys loaded by this call (a key that is already loaded
 * is skipped). Rejects for an unknown key or a file that cannot be fetched.
 */
@JsExport
fun enableStandardPlugins(keys: String, baseUrl: String): Promise<JsAny?> {
    val requested = keys.split(',').map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    val unknown = requested.filter { it !in standardPluginFiles }
    if (unknown.isNotEmpty()) {
        val known = standardPluginFiles.keys.joinToString()
        return rejected("Unknown standard plugin: ${unknown.joinToString()} (available: $known)")
    }

    val loaded = newArray()
    var chain: Promise<JsAny?> = resolved()
    for (key in requested) {
        chain = chain.then {
            if (key in standardPluginIds) {
                resolved()
            } else {
                engine.loadPluginFromUrl(standardPluginUrl(key, baseUrl), "standard:$key").then { id ->
                    standardPluginIds[key] = id.toString()
                    push(loaded, key)
                    null
                }
            }
        }
    }
    return chain.then { loaded }
}

/** Standard plugin key → file name in the distribution's `plugins/` directory. */
internal val standardPluginFiles = linkedMapOf(
    "icons" to "icons-fontawesome.wasm",
    "math" to "stem-mathjax.wasm",
    "video" to "video-embed.wasm",
)

/** Where the plugin for [key] is fetched from, given the directory [baseUrl] (empty: `plugins/`). */
internal fun standardPluginUrl(key: String, baseUrl: String): String =
    baseUrl.ifBlank { "plugins" }.trimEnd('/') + "/" + standardPluginFiles.getValue(key)

/**
 * Fetches a plugin's `.wasm` module from [url] and loads it. Resolves with
 * the plugin id.
 */
@JsExport
fun loadPluginFromUrl(url: String): Promise<JsAny?> = engine.loadPluginFromUrl(url).then { it }

/**
 * Loads a plugin from the bytes of a `.wasm` module (an `ArrayBuffer` or a
 * typed array). Resolves with the plugin id; rejects when the module is
 * refused, for example because it imports a host function.
 */
@JsExport
fun loadPlugin(wasm: JsAny, name: String): Promise<JsAny?> = engine.loadPlugin(wasm, name).then { it }

/** Unloads every plugin, returning to the neutral core output. */
@JsExport
fun unloadPlugins() {
    engine.unloadAll()
    standardPluginIds.clear()
}

/** Ids of the loaded plugins, comma-separated, in load order. */
@JsExport
fun loadedPlugins(): String = engine.descriptors().joinToString(",") { it.id }

/** Library version, for display in host UIs. */
@JsExport
fun version(): String = "0.1.1"

/** Standard plugin key → id of the plugin it loaded. */
private val standardPluginIds = LinkedHashMap<String, String>()

private fun conversionResult(html: String, head: String, warnings: JsAny): JsAny =
    js("({ html: html, head: head, warnings: warnings })")

private fun newArray(): JsAny = js("[]")

private fun push(array: JsAny, value: String): Unit = js("{ array.push(value); }")

private fun resolved(): Promise<JsAny?> = js("Promise.resolve(null)")

private fun rejected(message: String): Promise<JsAny?> = js("Promise.reject(new Error(message))")
