@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.markup.poet.wasm

import kotlin.js.Promise
import org.markup.poet.plugin.standard.StandardPlugin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The bridge in a real browser: the standard plugins (Kotlin/Wasm modules)
 * run on the browser's WebAssembly engine through `BrowserPluginEngine`.
 * Tests share one page, so each starts by unloading whatever is loaded.
 */
class BrowserPluginsTest {

    private val source = """
        = Demo
        :icons: font
        :stem: asciimath

        == Media

        An arrow icon:arrow-right[] and icon:github[set=brands].

        .A random video
        video::RvRhUHTV_8k[youtube,list=PLDitloyBcHOm49bxNhvGgg0f9NRZ5lSaP]

        A formula stem:[[[a,b\],[c,d\]\]((n),(k))].

        [stem]
        ++++
        sqrt(4) = 2
        ++++
    """.trimIndent()

    private fun assertContains(text: String, fragment: String) =
        assertTrue(fragment in text, "expected to contain: $fragment\n--- actual ---\n$text")

    @Test
    fun withoutPluginsTheOutputIsNeutral() {
        unloadPlugins()
        val result = convert(source)
        val html = field(result, "html")
        assertContains(html, "<span class=\"icon\">[arrow-right&#93;</span>")
        assertContains(html, "<code class=\"stem\" data-notation=\"asciimath\">")
        assertFalse("<iframe" in html)
        assertEquals("", field(result, "head"))
        assertEquals(html, convertToHtml(source))
    }

    @Test
    fun standardPluginsRenderIconsMathAndVideo(): Promise<JsAny?> {
        unloadPlugins()
        // Fetched and instantiated by the browser; data: URLs stand in for the served files.
        return loadPluginFromUrl(dataUrl(StandardPlugin.ICONS_FONT_AWESOME))
            .then { loadPluginFromUrl(dataUrl(StandardPlugin.STEM_MATHJAX)) }
            .then { loadPluginFromUrl(dataUrl(StandardPlugin.VIDEO_EMBED)) }
            .then { lastId ->
            assertEquals("org.markup-poet.video-embed", lastId.toString())
            assertEquals(3, loadedPlugins().split(',').size)

            val result = convert(source)
            val html = field(result, "html")
            assertContains(html, "<span class=\"icon\"><i class=\"fa fa-arrow-right\"></i></span>")
            assertContains(html, "<i class=\"fa-brands fa-github\"></i>")
            assertContains(html, "\\\$[[a,b],[c,d]]((n),(k))\\\$")
            assertContains(html, "<div class=\"stemblock\"><div class=\"content\">\n\\\$sqrt(4) = 2\\\$\n</div></div>")
            assertContains(
                html,
                "<iframe src=\"https://www.youtube.com/embed/RvRhUHTV_8k?rel=0&amp;list=PLDitloyBcHOm49bxNhvGgg0f9NRZ5lSaP\"",
            )
            assertContains(html, "<div class=\"title\">A random video</div>")

            // The fragment carries no head markup; the page gets it separately, once per asset.
            assertFalse("<link " in html || "<script" in html)
            val head = field(result, "head")
            assertEquals(1, head.split("font-awesome/6.5.2/css/all.min.css").size - 1)
            assertEquals(1, head.split("mathjax@3/es5/tex-chtml.js").size - 1)
            assertEquals("", joined(warnings(result)))

            // The same instances serve many conversions.
            repeat(20) { assertEquals(html, field(convert(source), "html")) }

            // Loading the same plugin twice is refused and leaves the first one in place.
            outcome(loadPluginFromUrl(dataUrl(StandardPlugin.VIDEO_EMBED)))
        }.then { again ->
            assertContains(again.toString(), "rejected: ")
            assertContains(again.toString(), "already loaded")
            assertEquals(3, loadedPlugins().split(',').size)
            unloadPlugins()
            assertEquals("", loadedPlugins())
            assertContains(convertToHtml(source), "<span class=\"icon\">[arrow-right&#93;</span>")
            null
        }
    }

    @Test
    fun unknownStandardPluginIsRejected(): Promise<JsAny?> {
        unloadPlugins()
        return outcome(enableStandardPlugins("icons,pdf", "")).then { message ->
            assertContains(message.toString(), "rejected: Unknown standard plugin: pdf")
            assertEquals("", loadedPlugins())
            null
        }
    }

    @Test
    fun standardPluginsAreFetchedFromTheGivenDirectory() {
        assertEquals("plugins/icons-fontawesome.wasm", standardPluginUrl("icons", ""))
        assertEquals("/assets/poet/stem-mathjax.wasm", standardPluginUrl("math", "/assets/poet/"))
        assertEquals("https://cdn.example.org/p/video-embed.wasm", standardPluginUrl("video", "https://cdn.example.org/p"))
        // Every standard plugin has a file.
        assertEquals(StandardPlugin.entries.map { it.key }, standardPluginFiles.keys.toList())
    }

    @Test
    fun aPluginFileThatCannotBeFetchedRejects(): Promise<JsAny?> {
        unloadPlugins()
        return outcome(enableStandardPlugins("icons", "/no-such-directory")).then { message ->
            assertContains(message.toString(), "rejected: cannot fetch plugin /no-such-directory/icons-fontawesome.wasm: HTTP 404")
            assertEquals("", loadedPlugins())
            null
        }
    }

    @Test
    fun moduleImportingAHostFunctionIsRefused(): Promise<JsAny?> {
        unloadPlugins()
        // (module (import "env" "host_call" (func)))
        val module = bytes("AGFzbQEAAAABBAFgAAACEQEDZW52CWhvc3RfY2FsbAAA")
        return outcome(loadPlugin(module, "imports-host.wasm")).then { message ->
            assertContains(message.toString(), "rejected: import env.host_call is not available to plugins")
            assertEquals("", loadedPlugins())
            null
        }
    }
}

private fun dataUrl(plugin: StandardPlugin): String = "data:application/wasm;base64," + plugin.wasmBase64()

private fun field(result: JsAny, name: String): String = js("result[name]")

private fun warnings(result: JsAny): JsAny = js("result.warnings")

private fun joined(array: JsAny): String = js("array.join(',')")

private fun bytes(base64: String): JsAny = js("Uint8Array.from(atob(base64), (c) => c.charCodeAt(0))")

/** Settles to "resolved: value" or "rejected: message", so a test can assert on a rejection. */
private fun outcome(promise: Promise<JsAny?>): Promise<JsString> =
    js("promise.then((v) => 'resolved: ' + v, (e) => 'rejected: ' + (e && e.message ? e.message : e))")
