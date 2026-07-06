package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.parser.DefaultAsciidocParser
import org.markup.poet.asciidoc.render.DefaultBlockRenderer
import org.markup.poet.asciidoc.render.DefaultHtmlBuilder
import org.markup.poet.asciidoc.render.DefaultHtmlEscaper
import org.markup.poet.asciidoc.render.DefaultHtmlRenderer
import org.markup.poet.asciidoc.render.DefaultInlineRenderer
import org.markup.poet.asciidoc.render.RenderConfig
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * End-to-end proof that a document requesting a TOC via `:toc:` renders HTML
 * containing the TOC list, and that its `#anchor` hrefs resolve against the
 * ids the renderer gives the section headings — including the `-N` suffixes
 * for repeated titles.
 */
class TocRenderingEndToEndJvmTest {

    private val parser = DefaultAsciidocParser()

    private val processor = DefaultDocumentProcessor(
        includeResolver = DefaultIncludeResolver(parser),
        fragmentProcessor = DefaultFragmentProcessor(),
        conditionalProcessor = DefaultConditionalProcessor(),
        attributeSubstitutor = DefaultAttributeSubstitutor(),
        macroExpander = DefaultMacroExpander(),
        admonitionProcessor = DefaultAdmonitionProcessor(),
        calloutProcessor = DefaultCalloutProcessor(),
        bibliographyManager = DefaultBibliographyManager(),
        crossReferenceResolver = DefaultCrossReferenceResolver(),
        tocGenerator = DefaultTocGenerator(),
        documentValidator = DefaultDocumentValidator(),
    )

    private fun renderHtml(source: String): String {
        val parsed = parser.parse(source)
        assertTrue(parsed.errors.isEmpty(), "unexpected parse errors: ${parsed.errors}")

        val processed = processor.process(parsed.document, ProcessingConfig(enableIncludes = false))

        val escaper = DefaultHtmlEscaper()
        val builder = DefaultHtmlBuilder(escaper)
        val inlineRenderer = DefaultInlineRenderer(builder)
        val blockRenderer = DefaultBlockRenderer(builder, inlineRenderer)
        val renderer = DefaultHtmlRenderer(blockRenderer, inlineRenderer)

        return renderer.render(processed.document, RenderConfig()).getOrThrow()
    }

    @Test
    fun `document with toc attribute renders a TOC whose hrefs resolve`() {
        val source = """
            = Sample Document
            :toc:

            Preamble text.

            == Getting Started

            Intro paragraph.

            === Deep Dive

            Details.

            == Duplicate

            First occurrence.

            == Duplicate

            Second occurrence.
        """.trimIndent()

        val html = renderHtml(source)

        // The inserted TOC list is marked for styling
        assertTrue(html.contains("id=\"toc\""), "TOC id missing in:\n$html")
        assertTrue(Regex("<ul[^>]*class=\"[^\"]*toc[^\"]*\"[^>]*>").containsMatchIn(html), "TOC role class missing in:\n$html")

        // Every TOC href points at a heading id the renderer actually emitted
        val hrefs = Regex("href=\"#([^\"]+)\"").findAll(html).map { it.groupValues[1] }.toList()
        val headingIds = Regex("<h[1-6][^>]*\\bid=\"([^\"]+)\"").findAll(html).map { it.groupValues[1] }.toSet()
        assertTrue(hrefs.isNotEmpty(), "no TOC hrefs found in:\n$html")
        for (href in hrefs) {
            assertTrue(href in headingIds, "TOC href #$href has no matching heading id in:\n$html")
        }

        // Placement and dedup specifics
        assertTrue(hrefs.containsAll(listOf("getting-started", "deep-dive", "duplicate", "duplicate-1")), "unexpected hrefs: $hrefs")
        assertTrue(html.indexOf("id=\"toc\"") < html.indexOf("Preamble text"), "TOC should precede the preamble")
    }

    @Test
    fun `toc macro placement renders the TOC where the macro sat`() {
        val source = """
            = Sample Document
            :toc: macro

            Preamble text.

            toc::[]

            == Only Section

            Body.
        """.trimIndent()

        val html = renderHtml(source)

        assertTrue(html.contains("id=\"toc\""), "TOC id missing in:\n$html")
        assertTrue(html.contains("href=\"#only-section\""), "TOC link missing in:\n$html")
        // The TOC list replaced the macro: it sits after the preamble paragraph
        assertTrue(html.indexOf("Preamble text") < html.indexOf("id=\"toc\""), "TOC should follow the preamble")
        // and no empty macro placeholder <div id="toc"> remains
        assertTrue(!html.contains("<div id=\"toc\""), "macro placeholder should have been replaced in:\n$html")
    }

    @Test
    fun `without toc attribute the toc macro keeps its placeholder rendering`() {
        val source = """
            = Sample Document

            toc::[]

            == Only Section

            Body.
        """.trimIndent()

        val html = renderHtml(source)

        assertTrue(html.contains("<div id=\"toc\" class=\"toc\"></div>"), "macro placeholder missing in:\n$html")
        assertTrue(!html.contains("href=\"#only-section\""), "no TOC links expected in:\n$html")
    }
}
