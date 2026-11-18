@file:OptIn(kotlin.wasm.ExperimentalWasmInterop::class)

import org.markup.poet.plugin.api.BACKEND_HTML5
import org.markup.poet.plugin.api.PluginCapability
import org.markup.poet.plugin.api.PluginDescriptor
import org.markup.poet.plugin.api.PluginInvocation
import org.markup.poet.plugin.api.PluginResponse
import org.markup.poet.plugin.sdk.PluginGuest
import org.markup.poet.plugin.sdk.decline
import org.markup.poet.plugin.sdk.escapeHtml
import org.markup.poet.plugin.sdk.htmlReplacement
import org.markup.poet.plugin.sdk.isToken

/*
 * Embedded players for `video::id[youtube]` and `video::id[vimeo]`.
 *
 * HTML output only. Media files (`video::clip.mp4[]`) are declined and stay
 * with the host's `<video>` element. `list=` selects a YouTube playlist;
 * `width=` and `height=` size the frame.
 */

private fun isVideoId(text: String) = isToken(text, extra = "_-")

private fun render(invocation: PluginInvocation): PluginResponse {
    val attributes = invocation.attributes
    val id = invocation.content
    if (invocation.backend != BACKEND_HTML5 || !isVideoId(id)) return decline()

    val source = when (attributes["1"]) {
        "youtube" -> {
            val list = attributes["list"]?.takeIf(::isVideoId)?.let { "&amp;list=$it" } ?: ""
            "https://www.youtube.com/embed/$id?rel=0$list"
        }
        "vimeo" -> "https://player.vimeo.com/video/$id"
        else -> return decline()
    }

    val size = listOfNotNull(
        attributes["width"]?.toIntOrNull()?.let { " width=\"$it\"" },
        attributes["height"]?.toIntOrNull()?.let { " height=\"$it\"" },
    ).joinToString("")
    val blockId = attributes["id"]?.let { " id=\"${escapeHtml(it)}\"" } ?: ""
    val roles = attributes["role"]?.let { " ${escapeHtml(it)}" } ?: ""
    val title = attributes["title"]?.let { "<div class=\"title\">${escapeHtml(it)}</div>" } ?: ""
    return htmlReplacement(
        "<div class=\"videoblock$roles\"$blockId>$title<div class=\"content\">" +
            "<iframe$size src=\"$source\" frameborder=\"0\" allowfullscreen></iframe></div></div>",
    )
}

private val guest = PluginGuest(
    PluginDescriptor(
        abiVersion = 1,
        id = "org.markup-poet.video-embed",
        name = "Video embeds",
        version = "0.1.0",
        description = "Embeds YouTube and Vimeo players for video:: block macros",
        capabilities = listOf(PluginCapability("blockMacro", "video")),
    ),
    ::render,
)

@WasmExport("plugin_alloc") fun pluginAlloc(size: Int): Int = guest.alloc(size)
@WasmExport("plugin_dealloc") fun pluginDealloc(ptr: Int, size: Int) = guest.dealloc(ptr, size)
@WasmExport("plugin_info") fun pluginInfo(): Int = guest.info()
@WasmExport("process") fun process(ptr: Int, len: Int): Int = guest.process(ptr, len)

fun main() {}
