package org.markup.poet.plugin.integration

import org.markup.poet.asciidoc.parser.DefaultAsciidocParser
import org.markup.poet.asciidoc.render.DefaultBlockRenderer
import org.markup.poet.asciidoc.render.DefaultHtmlBuilder
import org.markup.poet.asciidoc.render.DefaultHtmlEscaper
import org.markup.poet.asciidoc.render.DefaultHtmlRenderer
import org.markup.poet.asciidoc.render.DefaultInlineRenderer
import org.markup.poet.asciidoc.render.OutputOptions
import org.markup.poet.asciidoc.render.RenderConfig
import org.markup.poet.plugin.engine.PluginEngine
import org.markup.poet.plugin.standard.StandardPlugin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The standard plugins (Kotlin/Wasm modules under `plugins/`) loaded into the
 * real engine and applied to a document using the constructs from issue #139.
 * Without them the core renders the same document with no third-party resource.
 */
class StandardPluginsEndToEndTest {

    private val source = """
        = Demo
        :github-icon: icon:github[set=brands]
        :icons: font
        :stem: asciimath

        == Media

        An arrow icon:arrow-right[] and {github-icon} in one sentence, and icon:heart[title="Like"] again.

        .A random video
        video::RvRhUHTV_8k[youtube,list=PLDitloyBcHOm49bxNhvGgg0f9NRZ5lSaP]

        video::clip.mp4[]

        == Math

        A matrix can be written as stem:[[[a,b\],[c,d\]\]((n),(k))] and
        latexmath:[C = \alpha + \beta Y^{\gamma} + \epsilon] too.

        [stem]
        ++++
        sqrt(4) = 2
        ++++
    """.trimIndent()

    private fun render(vararg plugins: StandardPlugin, text: String = source): Pair<String, List<String>> {
        val engine = PluginEngine()
        try {
            plugins.forEach { engine.loadPlugin(it.wasm(), it.key) }
            val processed = WasmExtensions(engine).apply(DefaultAsciidocParser().parse(text).document)
            val builder = DefaultHtmlBuilder(DefaultHtmlEscaper())
            val inlineRenderer = DefaultInlineRenderer(builder)
            val html = DefaultHtmlRenderer(DefaultBlockRenderer(builder, inlineRenderer), inlineRenderer)
                .render(
                    processed.document,
                    RenderConfig(outputOptions = OutputOptions(headContent = processed.headContent)),
                )
                .getOrThrow()
            return html to processed.warnings
        } finally {
            engine.unloadAll()
        }
    }

    private fun String.occurrences(fragment: String): Int = split(fragment).size - 1

    private fun assertContains(html: String, fragment: String) =
        assertTrue(fragment in html, "expected output to contain: $fragment\n--- output ---\n$html")

    @Test
    fun withoutPluginsNothingIsLoadedFromThirdParties() {
        val (html, warnings) = render()
        assertEquals(emptyList(), warnings)
        listOf("<script", "<iframe", "<link ", "cdnjs", "jsdelivr").forEach {
            assertFalse(it in html, "core output must not contain $it")
        }
        assertContains(html, "<span class=\"icon\">[arrow-right&#93;</span>")
    }

    @Test
    fun allStandardPluginsRenderTheDemoConstructs() {
        val (html, warnings) = render(*StandardPlugin.entries.toTypedArray())
        assertEquals(emptyList(), warnings)

        // Icons, including one that arrives through an attribute value.
        assertContains(html, "<span class=\"icon\"><i class=\"fa fa-arrow-right\"></i></span>")
        assertContains(html, "<i class=\"fa-brands fa-github\"></i>")
        assertContains(html, "<i class=\"fa fa-heart\" title=\"Like\"></i>")

        // Math in the delimiters the MathJax configuration declares; `\]` stays part of the formula.
        assertContains(html, "\\\$[[a,b],[c,d]]((n),(k))\\\$")
        assertContains(html, "\\(C = \\alpha + \\beta Y^{\\gamma} + \\epsilon\\)")
        assertContains(html, "<div class=\"stemblock\"><div class=\"content\">\n\\\$sqrt(4) = 2\\\$\n</div></div>")

        // The YouTube player with its playlist and block title; a media file stays a <video>.
        assertContains(
            html,
            "<iframe src=\"https://www.youtube.com/embed/RvRhUHTV_8k?rel=0&amp;list=PLDitloyBcHOm49bxNhvGgg0f9NRZ5lSaP\"",
        )
        assertContains(html, "<div class=\"title\">A random video</div>")
        assertContains(html, "<video controls src=\"clip.mp4\"></video>")

        // Each asset once in <head>, however many constructs asked for it.
        val head = html.substringBefore("</head>")
        assertEquals(1, head.occurrences("font-awesome/6.5.2/css/all.min.css"))
        assertEquals(1, head.occurrences("mathjax@3/es5/tex-chtml.js"))
        // Without this MathJax turns the \$ AsciiMath delimiters into literal $ signs.
        assertTrue("processEscapes: false" in head)
        assertEquals(1, html.occurrences("font-awesome/6.5.2/css/all.min.css"))
    }

    @Test
    fun pluginsStayInactiveWithoutTheDocumentAttributes() {
        val plain = source.replace(":icons: font\n", "").replace(":stem: asciimath\n", "")
        val (html, warnings) = render(StandardPlugin.ICONS_FONT_AWESOME, StandardPlugin.STEM_MATHJAX, text = plain)
        assertEquals(emptyList(), warnings)
        assertContains(html, "<span class=\"icon\">[arrow-right&#93;</span>")
        assertContains(html, "<code class=\"stem\" data-notation=\"asciimath\">")
        assertFalse("<link " in html || "<script" in html)
    }

    @Test
    fun iconStylesheetCanBeRedirectedOrSwitchedOff() {
        val redirected = source.replace(":icons: font", ":icons: font\n:iconfont-cdn: https://assets.example.org/fa.css")
        assertContains(
            render(StandardPlugin.ICONS_FONT_AWESOME, text = redirected).first,
            "<link rel=\"stylesheet\" href=\"https://assets.example.org/fa.css\">",
        )

        val local = source.replace(":icons: font", ":icons: font\n:iconfont-remote: false")
        val html = render(StandardPlugin.ICONS_FONT_AWESOME, text = local).first
        assertContains(html, "<i class=\"fa fa-arrow-right\"></i>")
        assertFalse("<link " in html)
    }

    @Test
    fun aSinglePluginLeavesTheOtherConstructsNeutral() {
        val (html, _) = render(StandardPlugin.VIDEO_EMBED)
        assertContains(html, "<iframe ")
        assertContains(html, "<span class=\"icon\">[arrow-right&#93;</span>")
        assertFalse("<script" in html)
    }
}
