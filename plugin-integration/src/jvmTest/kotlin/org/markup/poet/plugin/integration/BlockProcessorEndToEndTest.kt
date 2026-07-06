package org.markup.poet.plugin.integration

import org.markup.poet.asciidoc.asg.InlineMacro
import org.markup.poet.asciidoc.asg.InlineRaw
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.LeafBlockName
import org.markup.poet.asciidoc.asg.RawBlock
import org.markup.poet.asciidoc.asg.plainText
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
    fun parserPreservesUnknownBlockStyleAsCustomStyledLeafBlock() {
        val document = DefaultAsciidocParser().parse(source).document
        val custom = document.blocks.filterIsInstance<LeafBlock>()
            .single { it.metadata?.positional?.firstOrNull() == "shout" }
        assertEquals(LeafBlockName.LISTING, custom.name)
        assertEquals("hello plugins", plainText(custom.inlines))
        assertEquals("shout", custom.metadata?.positional?.firstOrNull())
    }

    @Test
    fun pluginReplacesCustomStyledBlockWithRawHtmlBlock() {
        val engine = loadEngine()
        val document = DefaultAsciidocParser().parse(source).document
        val result = WasmExtensions(engine).apply(document)

        val raw = result.document.blocks.filterIsInstance<RawBlock>().single()
        assertEquals("html", raw.format)
        assertEquals("<div class=\"shout\">HELLO PLUGINS!</div>", raw.content)
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

        val paragraph = processed.blocks.filterIsInstance<LeafBlock>()
            .single { it.name == LeafBlockName.PARAGRAPH }
        val raw = paragraph.inlines.filterIsInstance<InlineRaw>().single()
        assertTrue("issues/123" in raw.content && ">#123</a>" in raw.content)
        engine.unloadAll()
    }

    @Test
    fun failedMacroInvocationKeepsOriginalWithWarning() {
        val engine = loadEngine()
        val parsed = DefaultAsciidocParser().parse("bad issue:abc[] target").document
        val result = WasmExtensions(engine).apply(parsed)

        val paragraph = result.document.blocks.filterIsInstance<LeafBlock>()
            .single { it.name == LeafBlockName.PARAGRAPH }
        assertTrue(paragraph.inlines.any { it is InlineMacro })
        assertTrue(result.warnings.isNotEmpty())
        engine.unloadAll()
    }

    @Test
    fun unclaimedCustomStyleBlockIsLeftForFallbackRendering() {
        val engine = loadEngine()
        val document = DefaultAsciidocParser().parse("[gallery]\n----\nx\n----").document
        val result = WasmExtensions(engine).apply(document)
        val block = result.document.blocks.single()
        assertIs<LeafBlock>(block)
        assertEquals("gallery", block.metadata?.positional?.firstOrNull())
        engine.unloadAll()
    }
}
