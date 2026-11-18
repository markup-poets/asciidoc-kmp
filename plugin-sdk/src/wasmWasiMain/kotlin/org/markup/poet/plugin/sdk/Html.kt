package org.markup.poet.plugin.sdk

import org.markup.poet.plugin.api.PluginAsset
import org.markup.poet.plugin.api.PluginReplacement
import org.markup.poet.plugin.api.PluginResponse

/** Escapes text for use in HTML content or a double-quoted attribute value. */
fun escapeHtml(text: String): String = buildString(text.length + 16) {
    for (c in text) {
        when (c) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            '\'' -> append("&#39;")
            else -> append(c)
        }
    }
}

/** A response replacing the construct with [html], optionally depending on [assets]. */
fun htmlReplacement(html: String, assets: List<PluginAsset> = emptyList()): PluginResponse =
    PluginResponse(ok = true, replacement = PluginReplacement("html", html), assets = assets)

/** A response leaving the construct to the host's built-in rendering. */
fun decline(): PluginResponse = PluginResponse(ok = true)

/** True for an `http:` or `https:` URL, the only kind a plugin should put into a resource link. */
fun isWebUrl(url: String): Boolean = url.startsWith("https://") || url.startsWith("http://")

/**
 * True when [text] is non-empty and consists of ASCII letters, digits and the
 * characters in [extra] only: safe to place into a URL or a class name
 * unescaped. (A plain loop rather than a `Regex`, which would add about
 * 120 KB to the plugin binary.)
 */
fun isToken(text: String, extra: String = "-"): Boolean =
    text.isNotEmpty() && text.all { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it in extra }
