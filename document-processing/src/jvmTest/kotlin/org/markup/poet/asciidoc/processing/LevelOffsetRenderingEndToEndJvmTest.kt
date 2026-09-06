package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.parser.DefaultAsciidocParser
import org.markup.poet.asciidoc.render.DefaultBlockRenderer
import org.markup.poet.asciidoc.render.DefaultHtmlBuilder
import org.markup.poet.asciidoc.render.DefaultHtmlEscaper
import org.markup.poet.asciidoc.render.DefaultHtmlRenderer
import org.markup.poet.asciidoc.render.DefaultInlineRenderer
import org.markup.poet.asciidoc.render.RenderConfig
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Full pipeline (parse -> process -> render) proof that `include::` honors `leveloffset` and
 * that the included document's own title becomes a heading instead of being dropped.
 */
class LevelOffsetRenderingEndToEndJvmTest {

    private val parser = DefaultAsciidocParser()

    private fun renderHtml(source: String, basePath: String): String {
        val parsed = parser.parse(source)
        assertTrue(parsed.errors.isEmpty(), "unexpected parse errors: ${parsed.errors}")

        val processor = DefaultDocumentProcessor(
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
            fileReaderFactory = { JvmFileReader() },
        )
        val processed = processor.process(parsed.document, ProcessingConfig(basePath = basePath))
        assertTrue(processed.errors.isEmpty(), "unexpected processing errors: ${processed.errors}")

        val builder = DefaultHtmlBuilder(DefaultHtmlEscaper())
        val inlineRenderer = DefaultInlineRenderer(builder)
        val renderer = DefaultHtmlRenderer(DefaultBlockRenderer(builder, inlineRenderer), inlineRenderer)
        return renderer.render(processed.document, RenderConfig()).getOrThrow()
    }

    @Test
    fun `include with leveloffset renders the included title and its sections at the shifted level`() {
        val dir = Files.createTempDirectory("mp-leveloffset-render-e2e").toFile()
        try {
            dir.resolve("child.adoc").writeText("= Child Title\n\n== Child Section\n\nBody.\n")
            val parent = """
                = Parent

                == Group

                include::child.adoc[leveloffset=+2]
            """.trimIndent()

            val html = renderHtml(parent, dir.absolutePath)

            // level 0 (doc title) + 2 -> level 2 -> <h3>; level 1 (== Child Section) + 2 -> level 3 -> <h4>
            assertTrue(Regex("<h3[^>]*>Child Title</h3>").containsMatchIn(html), "expected Child Title as an h3 in:\n$html")
            assertTrue(Regex("<h4[^>]*>Child Section</h4>").containsMatchIn(html), "expected Child Section as an h4 in:\n$html")
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `include without leveloffset leaves included headings at their original level`() {
        val dir = Files.createTempDirectory("mp-leveloffset-render-noop-e2e").toFile()
        try {
            dir.resolve("child.adoc").writeText("== Untouched Section\n\nBody.\n")
            val parent = "include::child.adoc[]"

            val html = renderHtml(parent, dir.absolutePath)

            assertTrue(Regex("<h2[^>]*>Untouched Section</h2>").containsMatchIn(html), "expected Untouched Section as an h2 in:\n$html")
        } finally {
            dir.deleteRecursively()
        }
    }
}
