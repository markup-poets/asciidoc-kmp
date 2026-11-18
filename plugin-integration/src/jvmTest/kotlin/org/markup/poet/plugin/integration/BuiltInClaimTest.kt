package org.markup.poet.plugin.integration

import org.markup.poet.asciidoc.asg.BlockMacro
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.RawBlock
import org.markup.poet.asciidoc.parser.DefaultAsciidocParser
import org.markup.poet.plugin.api.PluginAsset
import org.markup.poet.plugin.api.PluginCapability
import org.markup.poet.plugin.api.PluginReplacement
import org.markup.poet.plugin.api.PluginResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * A plugin's claim takes precedence over built-in names (block macros such
 * as `video`, block styles such as `stem`), a plugin may decline a single
 * construct, and assets are collected once per key.
 */
class BuiltInClaimTest {

    private fun parse(text: String) = DefaultAsciidocParser().parse(text.trimIndent()).document

    private fun html(value: String, vararg assets: PluginAsset) =
        PluginResponse(ok = true, replacement = PluginReplacement("html", value), assets = assets.toList())

    @Test
    fun builtInBlockMacroGoesToTheClaimingPluginWithItsMetadata() {
        val plugin = FakePlugin("video", listOf(PluginCapability("blockMacro", "video"))) { html("<x-video/>") }
        val result = WasmExtensions(FakeDispatch(listOf(plugin)), backend = "html5").apply(
            parse(
                """
                .Launch
                [#clip.wide]
                video::abc123[youtube,width=640]
                """,
            ),
        )

        assertEquals("<x-video/>", assertIs<RawBlock>(result.document.blocks.single()).content)
        val invocation = plugin.invocations.single()
        assertEquals("blockMacro", invocation.extensionPoint)
        assertEquals("video", invocation.name)
        assertEquals("abc123", invocation.content)
        assertEquals("html5", invocation.backend)
        assertEquals(
            mapOf(
                "target" to "abc123", "1" to "youtube", "width" to "640",
                "id" to "clip", "role" to "wide", "title" to "Launch",
            ),
            invocation.attributes,
        )
    }

    @Test
    fun aDecliningPluginLeavesTheBuiltInInPlaceWithoutWarning() {
        val plugin = FakePlugin("video", listOf(PluginCapability("blockMacro", "video"))) { PluginResponse(ok = true) }
        val result = WasmExtensions(FakeDispatch(listOf(plugin))).apply(parse("video::clip.mp4[]"))

        assertIs<BlockMacro>(result.document.blocks.single())
        assertEquals(emptyList(), result.warnings)
        assertEquals(1, plugin.invocations.size)
    }

    @Test
    fun builtInBlockStyleCanBeClaimedOrDeclined() {
        val source = """
            [stem]
            ++++
            x^2
            ++++
        """
        val claiming = FakePlugin("math", listOf(PluginCapability("block", "stem"))) { html("<x-math/>") }
        val claimed = WasmExtensions(FakeDispatch(listOf(claiming))).apply(parse(source))
        assertEquals("<x-math/>", assertIs<RawBlock>(claimed.document.blocks.single()).content)
        assertEquals("x^2", claiming.invocations.single().content)

        val declining = FakePlugin("math", listOf(PluginCapability("block", "stem"))) { PluginResponse(ok = true) }
        val declined = WasmExtensions(FakeDispatch(listOf(declining))).apply(parse(source))
        assertIs<LeafBlock>(declined.document.blocks.single())
        assertEquals(emptyList(), declined.warnings)
    }

    @Test
    fun assetsAreCollectedOncePerKeyAndOnlyFromAcceptedReplacements() {
        val css = PluginAsset("icons-css", "<link rel=\"stylesheet\" href=\"icons.css\">")
        val body = PluginAsset("late", "<script src=\"late.js\"></script>", placement = "body-end")
        val plugin = FakePlugin("icons", listOf(PluginCapability("inlineMacro", "icon"))) { invocation ->
            if (invocation.content == "skip") {
                PluginResponse(ok = true, assets = listOf(PluginAsset("unused", "<link href=\"unused.css\">")))
            } else {
                html("<i/>", css, body)
            }
        }
        val extensions = WasmExtensions(FakeDispatch(listOf(plugin)))
        val result = extensions.apply(parse("icon:a[] icon:b[] icon:skip[]\n\n* icon:c[]"))

        assertEquals(listOf("icons-css", "late"), result.assets.map { it.key })
        assertEquals(listOf(css.html), result.headContent)

        // A second document starts with a clean slate.
        assertTrue(extensions.apply(parse("no icons here")).assets.isEmpty())
    }
}
