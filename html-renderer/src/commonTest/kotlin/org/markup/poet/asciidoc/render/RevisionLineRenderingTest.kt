package org.markup.poet.asciidoc.render

import org.markup.poet.asciidoc.parser.DefaultAsciidocParser
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The revision line below the author line is header metadata, not body text. */
class RevisionLineRenderingTest {

    private fun render(source: String): String {
        val document = DefaultAsciidocParser().parse(source).document
        val builder = DefaultHtmlBuilder(DefaultHtmlEscaper())
        val inlineRenderer = DefaultInlineRenderer(builder)
        return DefaultHtmlRenderer(DefaultBlockRenderer(builder, inlineRenderer), inlineRenderer)
            .render(document, RenderConfig())
            .getOrThrow()
    }

    @Test
    fun `shows number, date and remark in the header details`() {
        val html = render("= Title\nJane Doe\nv1.0, 2026-01-05: First <draft>\n\nbody")
        assertTrue(
            "<span id=\"author\" class=\"author\">Jane Doe</span><br>" +
                "<span id=\"revnumber\">version 1.0,</span> <span id=\"revdate\">2026-01-05</span><br>" +
                "<span id=\"revremark\">First &lt;draft&gt;</span>" in html,
            html,
        )
        assertFalse("<p>v1.0" in html, html)
    }

    @Test
    fun `shows a date-only revision line`() {
        val html = render("= Title\nJane Doe\n2026-01-05\n\nbody")
        assertTrue("<br><span id=\"revdate\">2026-01-05</span></div>" in html, html)
        assertFalse("revnumber" in html, html)
    }

    @Test
    fun `attribute entries after the revision line are document attributes`() {
        val html = render("= Title\nJane Doe\nv1.0, 2026\n:sectnums:\n\n== First\n\nbody")
        assertTrue(">1. First</h2>" in html, html)
    }
}
