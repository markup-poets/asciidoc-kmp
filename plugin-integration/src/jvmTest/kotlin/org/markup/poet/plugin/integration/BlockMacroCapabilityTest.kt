package org.markup.poet.plugin.integration

import org.markup.poet.asciidoc.asg.CustomBlockMacro
import org.markup.poet.asciidoc.asg.InlineRaw
import org.markup.poet.asciidoc.asg.InlineText
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.LeafBlockName
import org.markup.poet.asciidoc.asg.RawBlock
import org.markup.poet.asciidoc.asg.plainText
import org.markup.poet.asciidoc.parser.DefaultAsciidocParser
import org.markup.poet.plugin.api.PluginCapability
import org.markup.poet.plugin.api.PluginReplacement
import org.markup.poet.plugin.api.PluginResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** The `blockMacro` capability: plugins claim [CustomBlockMacro] nodes by name. */
class BlockMacroCapabilityTest {

    private fun galleryPlugin(response: PluginResponse) = FakePlugin(
        id = "gallery-plugin",
        capabilities = listOf(PluginCapability(type = "blockMacro", name = "gallery")),
    ) { response }

    private fun apply(source: String, plugin: FakePlugin) =
        WasmExtensions(FakeDispatch(listOf(plugin)))
            .apply(DefaultAsciidocParser().parse(source).document)

    @Test
    fun htmlReplacementSplicesRawBlock() {
        val plugin = galleryPlugin(
            PluginResponse(
                ok = true,
                replacement = PluginReplacement("html", "<div class=\"gallery\">photos</div>"),
            ),
        )
        val result = apply("before\n\ngallery::photos/2024[cols,size=big]\n\nafter", plugin)

        val raw = result.document.blocks.filterIsInstance<RawBlock>().single()
        assertEquals("<div class=\"gallery\">photos</div>", raw.content)
        assertTrue(result.warnings.isEmpty(), "unexpected warnings: ${result.warnings}")

        // Envelope: name/target/content plus metadata as 1-based positional + named.
        val invocation = plugin.invocations.single()
        assertEquals("blockMacro", invocation.extensionPoint)
        assertEquals("gallery", invocation.name)
        assertEquals("photos/2024", invocation.content)
        assertEquals("photos/2024", invocation.attributes["target"])
        assertEquals("cols", invocation.attributes["1"])
        assertEquals("big", invocation.attributes["size"])
        assertEquals(3, invocation.location?.line)
    }

    @Test
    fun asciidocReplacementIsReparsedAndSpliced() {
        val plugin = galleryPlugin(
            PluginResponse(ok = true, replacement = PluginReplacement("asciidoc", "replacement *text*")),
        )
        val result = apply("gallery::dir[]", plugin)

        val paragraph = assertIs<LeafBlock>(result.document.blocks.single())
        assertEquals(LeafBlockName.PARAGRAPH, paragraph.name)
        assertEquals("replacement text", plainText(paragraph.inlines))
    }

    @Test
    fun unclaimedBlockMacroIsLeftInPlace() {
        val plugin = galleryPlugin(PluginResponse(ok = true))
        val result = apply("unclaimed::dir[]", plugin)
        val macro = assertIs<CustomBlockMacro>(result.document.blocks.single())
        assertEquals("unclaimed", macro.name)
        assertTrue(plugin.invocations.isEmpty())
    }

    @Test
    fun rejectedInvocationKeepsMacroAndWarns() {
        val plugin = galleryPlugin(PluginResponse(ok = false, error = "no photos found"))
        val result = apply("gallery::dir[]", plugin)
        assertIs<CustomBlockMacro>(result.document.blocks.single())
        assertTrue(result.warnings.any { "no photos found" in it }, "warnings: ${result.warnings}")
    }

    @Test
    fun blockMacroNestedInSectionIsClaimed() {
        val plugin = galleryPlugin(
            PluginResponse(ok = true, replacement = PluginReplacement("html", "<hr class=\"g\">")),
        )
        val result = apply("== Section\n\ngallery::dir[]", plugin)
        assertEquals(1, plugin.invocations.size)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun inlineMacroCapabilityStillWorksThroughFakeDispatch() {
        val plugin = FakePlugin(
            id = "issue-plugin",
            capabilities = listOf(PluginCapability(type = "inlineMacro", name = "issue")),
        ) { PluginResponse(ok = true, replacement = PluginReplacement("html", "<a href=\"#42\">#42</a>")) }
        val result = apply("see issue:42[] here", plugin)

        val paragraph = assertIs<LeafBlock>(result.document.blocks.single())
        val raw = paragraph.inlines.filterIsInstance<InlineRaw>().single()
        assertEquals("<a href=\"#42\">#42</a>", raw.content)
        assertTrue(paragraph.inlines.filterIsInstance<InlineText>().isNotEmpty())
    }
}
