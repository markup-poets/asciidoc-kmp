package org.markup.poet.plugin.integration

import org.markup.poet.asciidoc.ast.CustomBlock
import org.markup.poet.asciidoc.ast.PassthroughBlock
import org.markup.poet.asciidoc.parser.DefaultAsciidocParser
import org.markup.poet.asciidoc.render.DefaultBlockRenderer
import org.markup.poet.asciidoc.render.DefaultHtmlBuilder
import org.markup.poet.asciidoc.render.DefaultHtmlEscaper
import org.markup.poet.asciidoc.render.DefaultHtmlRenderer
import org.markup.poet.asciidoc.render.DefaultInlineRenderer
import org.markup.poet.plugin.engine.PluginEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BlockProcessorEndToEndTest {

    private val source = """
        = Plugin Demo

        before

        [shout]
        ----
        hello plugins
        ----

        after
    """.trimIndent()

    private fun loadEngine(): PluginEngine {
        val bytes = checkNotNull(javaClass.getResourceAsStream("/fixtures/shout.wasm")) {
            "fixture /fixtures/shout.wasm missing — build it with examples/plugins/shout-rust/build.sh"
        }.readBytes()
        return PluginEngine().also { it.loadPlugin(bytes, "shout.wasm") }
    }

    @Test
    fun parserPreservesUnknownBlockStyleAsCustomBlock() {
        val document = DefaultAsciidocParser().parse(source).document
        val custom = document.children.filterIsInstance<CustomBlock>().single()
        assertEquals("shout", custom.name)
        assertEquals("hello plugins", custom.rawContent)
        assertEquals("shout", custom.attributes["1"])
    }

    @Test
    fun pluginReplacesCustomBlockWithHtmlPassthrough() {
        val engine = loadEngine()
        val document = DefaultAsciidocParser().parse(source).document
        val result = WasmExtensions(engine).apply(document)

        val passthrough = result.document.children.filterIsInstance<PassthroughBlock>().single()
        assertEquals("<div class=\"shout\">HELLO PLUGINS!</div>", passthrough.content)
        assertTrue(result.warnings.isEmpty(), "unexpected warnings: ${result.warnings}")
        engine.unloadAll()
    }

    @Test
    fun renderedHtmlContainsPluginOutputVerbatim() {
        val engine = loadEngine()
        val parsed = DefaultAsciidocParser().parse(source).document
        val processed = WasmExtensions(engine).apply(parsed).document

        val escaper = DefaultHtmlEscaper()
        val builder = DefaultHtmlBuilder(escaper)
        val inlineRenderer = DefaultInlineRenderer(builder)
        val blockRenderer = DefaultBlockRenderer(builder, inlineRenderer)
        val html = DefaultHtmlRenderer(blockRenderer, inlineRenderer).render(processed).getOrThrow()

        assertTrue("<div class=\"shout\">HELLO PLUGINS!</div>" in html, "plugin output missing from:\n$html")
        assertTrue("before" in html && "after" in html)
        engine.unloadAll()
    }

    @Test
    fun inlineMacroIsReplacedByPluginHtml() {
        val engine = loadEngine()
        val parsed = DefaultAsciidocParser().parse("see issue:123[] for details").document
        val processed = WasmExtensions(engine).apply(parsed).document

        val paragraph = processed.children.filterIsInstance<org.markup.poet.asciidoc.ast.Paragraph>().single()
        val raw = paragraph.content.filterIsInstance<org.markup.poet.asciidoc.ast.RawInline>().single()
        assertTrue("issues/123" in raw.content && ">#123</a>" in raw.content)
        engine.unloadAll()
    }

    @Test
    fun failedMacroInvocationKeepsOriginalWithWarning() {
        val engine = loadEngine()
        val parsed = DefaultAsciidocParser().parse("bad issue:abc[] target").document
        val result = WasmExtensions(engine).apply(parsed)

        val paragraph = result.document.children.filterIsInstance<org.markup.poet.asciidoc.ast.Paragraph>().single()
        assertTrue(paragraph.content.any { it is org.markup.poet.asciidoc.ast.MacroInvocation })
        assertTrue(result.warnings.isNotEmpty())
        engine.unloadAll()
    }

    @Test
    fun unclaimedCustomBlockIsLeftForFallbackRendering() {
        val engine = loadEngine()
        val document = DefaultAsciidocParser().parse("[gallery]\n----\nx\n----").document
        val result = WasmExtensions(engine).apply(document)
        assertIs<CustomBlock>(result.document.children.single())
        engine.unloadAll()
    }
}
