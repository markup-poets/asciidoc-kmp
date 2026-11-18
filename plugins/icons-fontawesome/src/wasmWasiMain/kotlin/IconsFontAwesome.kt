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
import org.markup.poet.plugin.sdk.isToken
import org.markup.poet.plugin.sdk.isWebUrl

/*
 * Font Awesome icons for `icon:name[]`.
 *
 * Active for HTML output when the document sets `:icons: font`, following the
 * convention of other AsciiDoc converters. The stylesheet comes from a CDN
 * unless the document turns that off (`:iconfont-remote: false`) or names
 * another location (`:iconfont-cdn: <url>`).
 */

private const val DEFAULT_STYLESHEET = "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css"
private val off = setOf("false", "no", "off", "0")

private fun render(invocation: PluginInvocation): PluginResponse {
    val document = invocation.documentAttributes
    val name = invocation.content
    if (invocation.backend != BACKEND_HTML5 || document["icons"] != "font" || !isToken(name)) {
        return decline()
    }

    val family = when (invocation.attributes["set"]) {
        "brands", "fab" -> "fa-brands"
        "regular", "far" -> "fa-regular"
        else -> "fa"
    }
    val classes = buildString {
        append(family).append(" fa-").append(name)
        invocation.attributes["size"]?.takeIf { isToken(it) }?.let { append(" fa-").append(it) }
        invocation.attributes["role"]?.let { append(' ').append(escapeHtml(it)) }
    }
    val title = invocation.attributes["title"]?.let { " title=\"${escapeHtml(it)}\"" } ?: ""
    val html = "<span class=\"icon\"><i class=\"$classes\"$title></i></span>"

    val remote = document["iconfont-remote"]?.lowercase() !in off && "iconfont-remote!" !in document
    val stylesheet = document["iconfont-cdn"]?.takeIf(::isWebUrl) ?: DEFAULT_STYLESHEET
    val assets = if (remote) {
        listOf(PluginAsset("font-awesome", "<link rel=\"stylesheet\" href=\"${escapeHtml(stylesheet)}\">"))
    } else {
        emptyList()
    }
    return htmlReplacement(html, assets)
}

private val guest = PluginGuest(
    PluginDescriptor(
        abiVersion = 1,
        id = "org.markup-poet.icons-fontawesome",
        name = "Font Awesome icons",
        version = "0.1.0",
        description = "Renders icon: macros as Font Awesome icons under :icons: font",
        capabilities = listOf(PluginCapability("inlineMacro", "icon")),
    ),
    ::render,
)

@WasmExport("plugin_alloc") fun pluginAlloc(size: Int): Int = guest.alloc(size)
@WasmExport("plugin_dealloc") fun pluginDealloc(ptr: Int, size: Int) = guest.dealloc(ptr, size)
@WasmExport("plugin_info") fun pluginInfo(): Int = guest.info()
@WasmExport("process") fun process(ptr: Int, len: Int): Int = guest.process(ptr, len)

fun main() {}
