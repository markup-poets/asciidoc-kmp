package org.markup.poet.asciidoc.render

import org.markup.poet.asciidoc.asg.InlineAttributeRef
import org.markup.poet.asciidoc.asg.InlineCallout
import org.markup.poet.asciidoc.asg.InlineCitation
import org.markup.poet.asciidoc.asg.InlineFootnote
import org.markup.poet.asciidoc.asg.InlineMacro
import org.markup.poet.asciidoc.asg.InlineRaw
import org.markup.poet.asciidoc.asg.InlineRef
import org.markup.poet.asciidoc.asg.InlineSpan
import org.markup.poet.asciidoc.asg.InlineText
import org.markup.poet.asciidoc.asg.RefVariant
import org.markup.poet.asciidoc.asg.SpanForm
import org.markup.poet.asciidoc.asg.SpanVariant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for InlineRenderer implementation.
 *
 * Tests cover:
 * - Text rendering with HTML escaping
 * - Strong (bold) rendering
 * - Emphasis (italic) rendering
 * - Inline code rendering
 * - Mark rendering
 * - Link rendering with URL sanitization
 * - Image macro rendering with alt text
 * - Nested inline nodes
 * - Attribute references
 * - Callouts
 * - Cross-references
 * - Macro invocations
 * - Footnotes, citations, and raw inline output
 */
class InlineRendererTest {

    private val escaper = DefaultHtmlEscaper()
    private val builder = DefaultHtmlBuilder(escaper)
    private val renderer = DefaultInlineRenderer(builder)
    private val config = RenderConfig()
    private val context = RenderContext(config)

    private fun text(value: String) = InlineText(value)

    private fun span(variant: SpanVariant, vararg inlines: org.markup.poet.asciidoc.asg.Inline) =
        InlineSpan(variant = variant, form = SpanForm.CONSTRAINED, inlines = inlines.toList())

    // ========== Text Rendering Tests ==========

    @Test
    fun `renders plain text`() {
        val html = renderer.render(text("Hello, world!"), context)

        assertEquals("Hello, world!", html)
    }

    @Test
    fun `escapes HTML special characters in text`() {
        val html = renderer.render(text("<script>alert('xss')</script>"), context)

        assertEquals("&lt;script&gt;alert('xss')&lt;/script&gt;", html)
    }

    @Test
    fun `escapes ampersands in text`() {
        val html = renderer.render(text("Tom & Jerry"), context)

        assertEquals("Tom &amp; Jerry", html)
    }

    @Test
    fun `handles empty text`() {
        val html = renderer.render(text(""), context)

        assertEquals("", html)
    }

    // ========== Strong (Bold) Rendering Tests ==========

    @Test
    fun `renders strong text with strong tag`() {
        val html = renderer.render(span(SpanVariant.STRONG, text("bold text")), context)

        assertEquals("<strong>bold text</strong>", html)
    }

    @Test
    fun `escapes HTML in strong text`() {
        val html = renderer.render(span(SpanVariant.STRONG, text("<b>test</b>")), context)

        assertEquals("<strong>&lt;b&gt;test&lt;/b&gt;</strong>", html)
    }

    @Test
    fun `renders nested inline nodes in strong`() {
        val strong = span(
            SpanVariant.STRONG,
            text("bold "),
            span(SpanVariant.EMPHASIS, text("and italic"))
        )
        val html = renderer.render(strong, context)

        assertEquals("<strong>bold <em>and italic</em></strong>", html)
    }

    // ========== Emphasis (Italic) Rendering Tests ==========

    @Test
    fun `renders emphasis text with em tag`() {
        val html = renderer.render(span(SpanVariant.EMPHASIS, text("italic text")), context)

        assertEquals("<em>italic text</em>", html)
    }

    @Test
    fun `renders nested strong in emphasis`() {
        val emphasis = span(
            SpanVariant.EMPHASIS,
            text("italic "),
            span(SpanVariant.STRONG, text("and bold"))
        )
        val html = renderer.render(emphasis, context)

        assertEquals("<em>italic <strong>and bold</strong></em>", html)
    }

    // ========== Code Rendering Tests ==========

    @Test
    fun `renders inline code with code tag`() {
        val html = renderer.render(span(SpanVariant.CODE, text("println(\"Hello\")")), context)

        assertEquals("<code>println(\"Hello\")</code>", html)
    }

    @Test
    fun `escapes HTML in inline code`() {
        val html = renderer.render(span(SpanVariant.CODE, text("<div>test</div>")), context)

        assertEquals("<code>&lt;div&gt;test&lt;/div&gt;</code>", html)
    }

    @Test
    fun `handles empty code`() {
        val html = renderer.render(span(SpanVariant.CODE), context)

        assertEquals("<code></code>", html)
    }

    @Test
    fun `renders nested inlines in code as plain code text`() {
        val code = span(
            SpanVariant.CODE,
            text("a "),
            span(SpanVariant.STRONG, text("b"))
        )
        val html = renderer.render(code, context)

        assertEquals("<code>a b</code>", html)
    }

    // ========== Mark Rendering Tests ==========

    @Test
    fun `renders mark with mark tag`() {
        val html = renderer.render(span(SpanVariant.MARK, text("highlighted")), context)

        assertEquals("<mark>highlighted</mark>", html)
    }

    @Test
    fun `renders nested inline nodes in mark`() {
        val mark = span(
            SpanVariant.MARK,
            text("very "),
            span(SpanVariant.STRONG, text("important"))
        )
        val html = renderer.render(mark, context)

        assertEquals("<mark>very <strong>important</strong></mark>", html)
    }

    // ========== Subscript and Superscript Rendering Tests ==========

    @Test
    fun `renders subscript with sub tag`() {
        val html = renderer.render(span(SpanVariant.SUBSCRIPT, text("2")), context)

        assertEquals("<sub>2</sub>", html)
    }

    @Test
    fun `renders superscript with sup tag`() {
        val html = renderer.render(span(SpanVariant.SUPERSCRIPT, text("2")), context)

        assertEquals("<sup>2</sup>", html)
    }

    @Test
    fun `escapes HTML inside subscript and superscript`() {
        val sub = renderer.render(span(SpanVariant.SUBSCRIPT, text("<i>")), context)
        val sup = renderer.render(span(SpanVariant.SUPERSCRIPT, text("<i>")), context)

        assertEquals("<sub>&lt;i&gt;</sub>", sub)
        assertEquals("<sup>&lt;i&gt;</sup>", sup)
    }

    // ========== Link Rendering Tests ==========

    @Test
    fun `renders link with href and text`() {
        val link = InlineRef(
            variant = RefVariant.LINK,
            target = "https://example.com",
            inlines = listOf(text("Example"))
        )
        val html = renderer.render(link, context)

        assertEquals("<a href=\"https://example.com\">Example</a>", html)
    }

    @Test
    fun `renders link target as text when no link text given`() {
        val link = InlineRef(
            variant = RefVariant.LINK,
            target = "https://example.com",
            inlines = emptyList()
        )
        val html = renderer.render(link, context)

        assertEquals("<a href=\"https://example.com\">https://example.com</a>", html)
    }

    @Test
    fun `escapes link text`() {
        val link = InlineRef(
            variant = RefVariant.LINK,
            target = "https://example.com",
            inlines = listOf(text("<script>alert('xss')</script>"))
        )
        val html = renderer.render(link, context)

        assertTrue(html.contains("&lt;script&gt;"))
        assertTrue(html.contains("&lt;/script&gt;"))
    }

    @Test
    fun `sanitizes javascript URL`() {
        val link = InlineRef(
            variant = RefVariant.LINK,
            target = "javascript:alert('xss')",
            inlines = listOf(text("Click me"))
        )
        val html = renderer.render(link, context)

        assertEquals("<a href=\"#\">Click me</a>", html)
    }

    @Test
    fun `sanitizes data URL`() {
        val link = InlineRef(
            variant = RefVariant.LINK,
            target = "data:text/html,<script>alert('xss')</script>",
            inlines = listOf(text("Click me"))
        )
        val html = renderer.render(link, context)

        assertEquals("<a href=\"#\">Click me</a>", html)
    }

    @Test
    fun `sanitizes javascript URL case insensitive`() {
        val link = InlineRef(
            variant = RefVariant.LINK,
            target = "JaVaScRiPt:alert('xss')",
            inlines = listOf(text("Click me"))
        )
        val html = renderer.render(link, context)

        assertEquals("<a href=\"#\">Click me</a>", html)
    }

    @Test
    fun `allows safe URLs`() {
        val testCases = listOf(
            "https://example.com",
            "http://example.com",
            "/path/to/page",
            "../relative/path",
            "#anchor",
            "mailto:test@example.com",
            "ftp://files.example.com"
        )

        testCases.forEach { url ->
            val link = InlineRef(variant = RefVariant.LINK, target = url, inlines = listOf(text("Link")))
            val html = renderer.render(link, context)
            assertTrue(html.contains("href=\"$url\""), "URL $url should be allowed")
        }
    }

    // ========== Image Macro Rendering Tests ==========

    @Test
    fun `renders image macro with src and alt`() {
        val image = InlineMacro(
            name = "image",
            target = "/images/photo.jpg",
            positional = listOf("A photo")
        )
        val html = renderer.render(image, context)

        assertEquals("<img src=\"/images/photo.jpg\" alt=\"A photo\">", html)
    }

    @Test
    fun `renders image macro with empty alt text`() {
        val image = InlineMacro(
            name = "image",
            target = "/images/photo.jpg"
        )
        val html = renderer.render(image, context)

        assertTrue(html.contains("alt=\"\""), "Image should have alt attribute even if empty")
    }

    @Test
    fun `renders image macro with title attribute`() {
        val image = InlineMacro(
            name = "image",
            target = "/images/photo.jpg",
            positional = listOf("A photo"),
            named = mapOf("title" to "Photo Title")
        )
        val html = renderer.render(image, context)

        assertTrue(html.contains("src=\"/images/photo.jpg\""))
        assertTrue(html.contains("alt=\"A photo\""))
        assertTrue(html.contains("title=\"Photo Title\""))
    }

    // ========== Attribute Reference Tests ==========

    @Test
    fun `renders unresolved attribute reference with warning`() {
        val ref = InlineAttributeRef(name = "author")
        val html = renderer.render(ref, context)

        assertEquals("{author}", html)
        assertTrue(context.getWarnings().any { it.contains("Unresolved attribute reference") })
    }

    // ========== Callout Tests ==========

    @Test
    fun `renders callout with number`() {
        val html = renderer.render(InlineCallout(number = 1), context)

        assertEquals("<span class=\"callout\">&lt;1&gt;</span>", html)
    }

    @Test
    fun `renders callout with multiple digit number`() {
        val html = renderer.render(InlineCallout(number = 42), context)

        assertEquals("<span class=\"callout\">&lt;42&gt;</span>", html)
    }

    // ========== Cross-Reference Tests ==========

    @Test
    fun `renders cross-reference with target ID`() {
        val ref = InlineRef(
            variant = RefVariant.XREF,
            target = "section-1",
            inlines = emptyList()
        )
        val html = renderer.render(ref, context)

        assertEquals("<a href=\"#section-1\">section-1</a>", html)
    }

    @Test
    fun `renders cross-reference with custom text`() {
        val ref = InlineRef(
            variant = RefVariant.XREF,
            target = "section-1",
            inlines = listOf(text("See Section 1"))
        )
        val html = renderer.render(ref, context)

        assertEquals("<a href=\"#section-1\">See Section 1</a>", html)
    }

    @Test
    fun `escapes custom text in cross-reference`() {
        val ref = InlineRef(
            variant = RefVariant.XREF,
            target = "section-1",
            inlines = listOf(text("<script>alert('xss')</script>"))
        )
        val html = renderer.render(ref, context)

        assertTrue(html.contains("&lt;script&gt;"))
        assertTrue(html.contains("&lt;/script&gt;"))
    }

    // ========== Macro Invocation Tests ==========

    @Test
    fun `renders unexpanded macro with warning`() {
        val macro = InlineMacro(
            name = "issue",
            target = "123"
        )
        val html = renderer.render(macro, context)

        assertEquals("<span class=\"macro-placeholder\">issue[]</span>", html)
        assertTrue(context.getWarnings().any { it.contains("Unexpanded macro") })
    }

    // ========== Footnote, Citation, and Raw Inline Tests ==========

    @Test
    fun `renders footnote reference as superscript anchor`() {
        val footnote = InlineFootnote(id = "1", inlines = listOf(text("footnote text")))
        val html = renderer.render(footnote, context)

        assertEquals("<sup class=\"footnote-ref\"><a href=\"#fn-1\">1</a></sup>", html)
    }

    @Test
    fun `renders citation as bibliography anchor`() {
        val citation = InlineCitation(citationId = "knuth84")
        val html = renderer.render(citation, context)

        assertEquals("<a href=\"#knuth84\" class=\"bibliography-ref\">[knuth84]</a>", html)
    }

    @Test
    fun `renders raw inline HTML verbatim`() {
        val raw = InlineRaw(format = "html", content = "<kbd>Ctrl</kbd>")
        val html = renderer.render(raw, context)

        assertEquals("<kbd>Ctrl</kbd>", html)
    }

    @Test
    fun `skips raw inline output of other formats`() {
        val raw = InlineRaw(format = "pdf", content = "raw bytes")
        val html = renderer.render(raw, context)

        assertEquals("", html)
    }

    // ========== Complex Nesting Tests ==========

    @Test
    fun `renders deeply nested inline nodes`() {
        val nested = span(
            SpanVariant.STRONG,
            text("bold "),
            span(
                SpanVariant.EMPHASIS,
                text("italic "),
                span(SpanVariant.CODE, text("code"))
            )
        )
        val html = renderer.render(nested, context)

        assertEquals("<strong>bold <em>italic <code>code</code></em></strong>", html)
    }

    @Test
    fun `renders multiple inline nodes in sequence`() {
        val elements = listOf(
            text("Plain "),
            span(SpanVariant.STRONG, text("bold")),
            text(" and "),
            span(SpanVariant.EMPHASIS, text("italic")),
            text(" text.")
        )

        val html = elements.joinToString("") { renderer.render(it, context) }

        assertEquals("Plain <strong>bold</strong> and <em>italic</em> text.", html)
    }

    @Test
    fun `handles empty nested content`() {
        val html = renderer.render(span(SpanVariant.STRONG), context)

        assertEquals("<strong></strong>", html)
    }
}
