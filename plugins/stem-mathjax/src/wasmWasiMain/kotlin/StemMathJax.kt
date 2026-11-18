@file:OptIn(kotlin.wasm.ExperimentalWasmInterop::class)

import org.markup.poet.plugin.api.BACKEND_HTML5
import org.markup.poet.plugin.api.PluginAsset
import org.markup.poet.plugin.api.PluginCapability
import org.markup.poet.plugin.api.PluginDescriptor
import org.markup.poet.plugin.api.PluginInvocation
import org.markup.poet.plugin.api.PluginResponse
import org.markup.poet.plugin.sdk.PluginGuest
import org.markup.poet.plugin.sdk.decline
import org.markup.poet.plugin.sdk.escapeHtml
import org.markup.poet.plugin.sdk.htmlReplacement

/*
 * MathJax typesetting for `stem:[]`, `latexmath:[]`, `asciimath:[]` and the
 * `[stem]`, `[latexmath]`, `[asciimath]` block styles.
 *
 * Active for HTML output when the document sets `:stem:`. `stem` follows the
 * notation named by that attribute (AsciiMath unless it says `latexmath`).
 * Formulas are wrapped in the delimiters the MathJax configuration below
 * declares: `\(..\)` and `\[..\]` for LaTeX, `\$..\$` for AsciiMath.
 */

private const val SCRIPT = "https://cdn.jsdelivr.net/npm/mathjax@3/es5/tex-chtml.js"
private const val CONFIG =
    "MathJax = {loader: {load: ['input/asciimath']}, " +
        "tex: {inlineMath: [['\\\\(', '\\\\)']], displayMath: [['\\\\[', '\\\\]']]}, " +
        "asciimath: {delimiters: [['\\\\\$', '\\\\\$']]}};"

private val assets = listOf(
    PluginAsset("mathjax", "<script>$CONFIG</script>\n  <script async src=\"$SCRIPT\"></script>"),
)
private val latexNotations = setOf("latexmath", "latex", "tex")

private fun render(invocation: PluginInvocation): PluginResponse {
    val document = invocation.documentAttributes
    if (invocation.backend != BACKEND_HTML5 || "stem" !in document) return decline()

    val latex = invocation.name == "latexmath" ||
        (invocation.name == "stem" && document["stem"]?.lowercase() in latexNotations)

    return when (invocation.extensionPoint) {
        "inlineMacro" -> {
            val formula = escapeHtml(invocation.attributes["1"].orEmpty())
            htmlReplacement(if (latex) "\\($formula\\)" else "\\\$$formula\\\$", assets)
        }
        "block" -> {
            val formula = escapeHtml(invocation.content)
            val delimited = if (latex) "\\[$formula\\]" else "\\\$$formula\\\$"
            val id = invocation.attributes["id"]?.let { " id=\"${escapeHtml(it)}\"" } ?: ""
            val roles = invocation.attributes["role"]?.let { " ${escapeHtml(it)}" } ?: ""
            val title = invocation.attributes["title"]?.let { "<div class=\"title\">${escapeHtml(it)}</div>" } ?: ""
            htmlReplacement(
                "<div class=\"stemblock$roles\"$id>$title<div class=\"content\">\n$delimited\n</div></div>",
                assets,
            )
        }
        else -> decline()
    }
}

private val names = listOf("stem", "latexmath", "asciimath")

private val guest = PluginGuest(
    PluginDescriptor(
        abiVersion = 1,
        id = "org.markup-poet.stem-mathjax",
        name = "MathJax formulas",
        version = "0.1.0",
        description = "Typesets stem, latexmath and asciimath macros and blocks with MathJax under :stem:",
        capabilities = names.map { PluginCapability("inlineMacro", it) } + names.map { PluginCapability("block", it) },
    ),
    ::render,
)

@WasmExport("plugin_alloc") fun pluginAlloc(size: Int): Int = guest.alloc(size)
@WasmExport("plugin_dealloc") fun pluginDealloc(ptr: Int, size: Int) = guest.dealloc(ptr, size)
@WasmExport("plugin_info") fun pluginInfo(): Int = guest.info()
@WasmExport("process") fun process(ptr: Int, len: Int): Int = guest.process(ptr, len)

fun main() {}
