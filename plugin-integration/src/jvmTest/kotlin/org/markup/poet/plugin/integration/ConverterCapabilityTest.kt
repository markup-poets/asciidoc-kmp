package org.markup.poet.plugin.integration

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.markup.poet.asciidoc.parser.DefaultAsciidocParser
import org.markup.poet.asciidoc.render.DefaultBlockRenderer
import org.markup.poet.asciidoc.render.DefaultHtmlBuilder
import org.markup.poet.asciidoc.render.DefaultHtmlEscaper
import org.markup.poet.asciidoc.render.DefaultHtmlRenderer
import org.markup.poet.asciidoc.render.DefaultInlineRenderer
import org.markup.poet.asciidoc.render.RenderConfig
import org.markup.poet.plugin.api.PluginCapability
import org.markup.poet.plugin.api.PluginReplacement
import org.markup.poet.plugin.api.PluginResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The `converter` capability: a plugin registered as a [WasmConverterRenderer]
 * receives the claimed node as official ASG JSON plus the document attributes
 * and returns HTML emitted verbatim.
 */
class ConverterCapabilityTest {

    private val source = """
        = Doc
        :brand: poet

        before

        [gallery]
        ----
        photo1.png
        photo2.png
        ----

        after
    """.trimIndent()

    private fun render(document: org.markup.poet.asciidoc.asg.AsgDocument, config: RenderConfig): String {
        val escaper = DefaultHtmlEscaper()
        val builder = DefaultHtmlBuilder(escaper)
        val inlineRenderer = DefaultInlineRenderer(builder)
        val blockRenderer = DefaultBlockRenderer(builder, inlineRenderer)
        return DefaultHtmlRenderer(blockRenderer, inlineRenderer).render(document, config).getOrThrow()
    }

    @Test
    fun converterPluginRendersClaimedBlockStyle() {
        val plugin = FakePlugin(
            id = "gallery-converter",
            capabilities = listOf(PluginCapability(type = "converter", name = "gallery")),
        ) { invocation ->
            // The node arrives as official ASG JSON with the claimed style.
            val node = Json.parseToJsonElement(invocation.content).jsonObject
            val style = node.getValue("metadata").jsonObject
                .getValue("attributes").jsonObject
                .getValue("style").jsonPrimitive.content
            PluginResponse(
                ok = true,
                replacement = PluginReplacement(
                    "html",
                    "<div class=\"$style\" data-brand=\"${invocation.documentAttributes["brand"]}\"></div>",
                ),
            )
        }

        val document = DefaultAsciidocParser().parse(source).document
        val renderers = converterRenderers(FakeDispatch(listOf(plugin)), document.attributes)
        assertEquals(setOf("gallery"), renderers.keys)

        val html = render(document, RenderConfig(customRenderers = renderers))
        assertTrue("<div class=\"gallery\" data-brand=\"poet\"></div>" in html, "converter output missing:\n$html")
        assertFalse("custom-block-gallery" in html, "fallback rendering must not fire")
        assertTrue("before" in html && "after" in html)

        val invocation = plugin.invocations.single()
        assertEquals("converter", invocation.extensionPoint)
        assertEquals("gallery", invocation.name)
        assertEquals("listing", Json.parseToJsonElement(invocation.content).jsonObject.getValue("name").jsonPrimitive.content)
    }

    @Test
    fun failingConverterRendersNothingButDocumentSurvives() {
        val plugin = FakePlugin(
            id = "broken-converter",
            capabilities = listOf(PluginCapability(type = "converter", name = "gallery")),
        ) { PluginResponse(ok = false, error = "cannot convert") }

        val document = DefaultAsciidocParser().parse(source).document
        val renderers = converterRenderers(FakeDispatch(listOf(plugin)), document.attributes)
        val html = render(document, RenderConfig(customRenderers = renderers))

        assertFalse("photo1.png" in html, "rejected node must render as nothing")
        assertTrue("before" in html && "after" in html)
    }

    @Test
    fun nonHtmlConverterResponseRendersNothing() {
        val plugin = FakePlugin(
            id = "weird-converter",
            capabilities = listOf(PluginCapability(type = "converter", name = "gallery")),
        ) { PluginResponse(ok = true, replacement = PluginReplacement("asciidoc", "nope")) }

        val document = DefaultAsciidocParser().parse(source).document
        val renderers = converterRenderers(FakeDispatch(listOf(plugin)), document.attributes)
        val html = render(document, RenderConfig(customRenderers = renderers))
        assertFalse("nope" in html)
    }

    @Test
    fun converterMayClaimANodeKindByClassName() {
        val plugin = FakePlugin(
            id = "list-converter",
            capabilities = listOf(PluginCapability(type = "converter", name = "ListBlock")),
        ) { PluginResponse(ok = true, replacement = PluginReplacement("html", "<ol class=\"fancy\"></ol>")) }

        val document = DefaultAsciidocParser().parse("* one\n* two").document
        val renderers = converterRenderers(FakeDispatch(listOf(plugin)), document.attributes)
        val html = render(document, RenderConfig(customRenderers = renderers))
        assertTrue("<ol class=\"fancy\"></ol>" in html, "class-name dispatch failed:\n$html")

        val node = Json.parseToJsonElement(plugin.invocations.single().content).jsonObject
        assertEquals("list", node.getValue("name").jsonPrimitive.content)
    }
}
