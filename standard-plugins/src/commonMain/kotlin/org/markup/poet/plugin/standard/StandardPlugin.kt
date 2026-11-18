package org.markup.poet.plugin.standard

import kotlin.io.encoding.Base64

/**
 * The WASM plugins that ship with the project. Each adds a presentation
 * feature the core renderer deliberately leaves out because it is a
 * convention of web output, not part of the AsciiDoc language, and because it
 * makes the generated page load resources from third parties.
 *
 * A host opts in per plugin: load [wasm] into a plugin engine before
 * processing a document.
 *
 * @param key short name used on command lines and in configuration
 */
enum class StandardPlugin(val key: String, val description: String) {
    /** `icon:name[]` as Font Awesome icons under `:icons: font`; links the Font Awesome stylesheet. */
    ICONS_FONT_AWESOME("icons", "Font Awesome icons for icon: macros (:icons: font)"),

    /** `stem:`, `latexmath:`, `asciimath:` and `[stem]` blocks typeset by MathJax under `:stem:`; loads MathJax. */
    STEM_MATHJAX("math", "MathJax typesetting for stem, latexmath and asciimath (:stem:)"),

    /** `video::id[youtube]` / `[vimeo]` as embedded players. */
    VIDEO_EMBED("video", "Embedded YouTube and Vimeo players for video:: macros");

    /** The plugin's WebAssembly module, ready for `PluginEngine.loadPlugin`. */
    fun wasm(): ByteArray = Base64.decode(wasmBase64())

    /**
     * The module in Base64, for hosts that hand it to another runtime without
     * touching the bytes (`BrowserPluginEngine.loadPluginBase64` in a browser).
     */
    fun wasmBase64(): String = when (this) {
        ICONS_FONT_AWESOME -> PluginBinaries.iconsFontawesome
        STEM_MATHJAX -> PluginBinaries.stemMathjax
        VIDEO_EMBED -> PluginBinaries.videoEmbed
    }.joinToString("")

    companion object {
        /** The plugin with the given [key], or null. */
        fun byKey(key: String): StandardPlugin? = entries.firstOrNull { it.key == key }
    }
}
