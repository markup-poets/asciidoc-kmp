package org.markup.poet.plugin.integration

import kotlinx.serialization.json.Json
import org.markup.poet.asciidoc.asg.CustomBlockMacro
import org.markup.poet.asciidoc.asg.InlineMacro
import org.markup.poet.asciidoc.asg.InlineSpan
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.LeafBlockName
import org.markup.poet.asciidoc.asg.SpanVariant
import org.markup.poet.asciidoc.asg.plainText
import org.markup.poet.asciidoc.parser.DefaultAsciidocParser
import org.markup.poet.plugin.api.PluginCapability
import org.markup.poet.plugin.api.PluginReplacement
import org.markup.poet.plugin.api.PluginResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * `contentType: "asg"` responses: official ASG node JSON is decoded and
 * spliced — blocks in block context, inlines in inline context; a mismatch
 * takes the plugin error path (warning + original construct), never a crash.
 */
class AsgReplacementTest {

    private val paragraphNode =
        """{"name":"paragraph","type":"block","inlines":[{"name":"text","type":"string","value":"from asg"}]}"""
    private val strongNode =
        """{"name":"span","type":"inline","variant":"strong","form":"constrained",
           "inlines":[{"name":"text","type":"string","value":"loud"}]}"""

    private fun asgReplacement(nodesJson: String) = PluginReplacement(
        contentType = "asg",
        nodes = Json.parseToJsonElement(nodesJson),
    )

    private fun blockMacroPlugin(replacement: PluginReplacement) = FakePlugin(
        id = "asg-plugin",
        capabilities = listOf(PluginCapability(type = "blockMacro", name = "gallery")),
    ) { PluginResponse(ok = true, replacement = replacement) }

    private fun inlineMacroPlugin(replacement: PluginReplacement) = FakePlugin(
        id = "asg-plugin",
        capabilities = listOf(PluginCapability(type = "inlineMacro", name = "issue")),
    ) { PluginResponse(ok = true, replacement = replacement) }

    private fun apply(source: String, plugin: FakePlugin) =
        WasmExtensions(FakeDispatch(listOf(plugin)))
            .apply(DefaultAsciidocParser().parse(source).document)

    @Test
    fun asgBlockNodesAreDecodedAndSpliced() {
        val result = apply("gallery::dir[]", blockMacroPlugin(asgReplacement("[$paragraphNode,$paragraphNode]")))
        assertEquals(2, result.document.blocks.size)
        result.document.blocks.forEach { block ->
            val paragraph = assertIs<LeafBlock>(block)
            assertEquals(LeafBlockName.PARAGRAPH, paragraph.name)
            assertEquals("from asg", plainText(paragraph.inlines))
        }
        assertTrue(result.warnings.isEmpty(), "unexpected warnings: ${result.warnings}")
    }

    @Test
    fun asgNodesFallBackToValueStringWhenNodesFieldAbsent() {
        val result = apply(
            "gallery::dir[]",
            blockMacroPlugin(PluginReplacement(contentType = "asg", value = "[$paragraphNode]")),
        )
        val paragraph = assertIs<LeafBlock>(result.document.blocks.single())
        assertEquals("from asg", plainText(paragraph.inlines))
    }

    @Test
    fun asgInlineNodesAreDecodedAndSplicedInInlineContext() {
        val result = apply("see issue:42[] here", inlineMacroPlugin(asgReplacement("[$strongNode]")))
        val paragraph = assertIs<LeafBlock>(result.document.blocks.single())
        val span = paragraph.inlines.filterIsInstance<InlineSpan>().single()
        assertEquals(SpanVariant.STRONG, span.variant)
        assertEquals("loud", plainText(span.inlines))
        assertTrue(result.warnings.isEmpty(), "unexpected warnings: ${result.warnings}")
    }

    @Test
    fun inlineNodesInBlockContextTakeErrorPathNotCrash() {
        val result = apply("gallery::dir[]", blockMacroPlugin(asgReplacement("[$strongNode]")))
        assertIs<CustomBlockMacro>(result.document.blocks.single())
        assertTrue(
            result.warnings.any { "invalid ASG block replacement" in it },
            "warnings: ${result.warnings}",
        )
    }

    @Test
    fun blockNodesInInlineContextTakeErrorPathNotCrash() {
        val result = apply("see issue:42[] here", inlineMacroPlugin(asgReplacement("[$paragraphNode]")))
        val paragraph = assertIs<LeafBlock>(result.document.blocks.single())
        assertTrue(paragraph.inlines.any { it is InlineMacro }, "original macro must survive")
        assertTrue(
            result.warnings.any { "invalid ASG inline replacement" in it },
            "warnings: ${result.warnings}",
        )
    }

    @Test
    fun malformedAsgJsonTakesErrorPathNotCrash() {
        val result = apply(
            "gallery::dir[]",
            blockMacroPlugin(PluginReplacement(contentType = "asg", value = "{not json")),
        )
        assertIs<CustomBlockMacro>(result.document.blocks.single())
        assertTrue(result.warnings.any { "invalid ASG block replacement" in it })
    }

    @Test
    fun asgReplacementAlsoWorksForBlockStyleCapability() {
        val plugin = FakePlugin(
            id = "asg-plugin",
            capabilities = listOf(PluginCapability(type = "block", name = "shout")),
        ) { PluginResponse(ok = true, replacement = asgReplacement("[$paragraphNode]")) }
        val result = apply("[shout]\n----\nhello\n----", plugin)
        val paragraph = assertIs<LeafBlock>(result.document.blocks.single())
        assertEquals("from asg", plainText(paragraph.inlines))
    }
}
