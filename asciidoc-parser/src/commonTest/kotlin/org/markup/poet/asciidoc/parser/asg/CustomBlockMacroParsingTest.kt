package org.markup.poet.asciidoc.parser.asg

import org.markup.poet.asciidoc.asg.BlockMacro
import org.markup.poet.asciidoc.asg.BlockMacroName
import org.markup.poet.asciidoc.asg.CustomBlockMacro
import org.markup.poet.asciidoc.asg.DListBlock
import org.markup.poet.asciidoc.asg.IncludeBlock
import org.markup.poet.asciidoc.asg.InlineText
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.LeafBlockName
import org.markup.poet.asciidoc.asg.Position
import org.markup.poet.asciidoc.asg.plainText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Generic (non-built-in) block macros `name::target[attrs]` become the
 * [CustomBlockMacro] extension node — the seam claimed by `blockMacro`
 * plugins. Recognition must not disturb built-in block macros, processing
 * directives, or description lists (`term:: description` has a space before
 * the description and no trailing `[...]`).
 */
class CustomBlockMacroParsingTest {

    private val parser = BlockTreeParser()

    @Test
    fun unknownBlockMacroBecomesCustomBlockMacro() {
        val doc = parser.parseDocument("gallery::photos/2024[]")
        val macro = assertIs<CustomBlockMacro>(doc.blocks.single())
        assertEquals("gallery", macro.name)
        assertEquals("photos/2024", macro.target)
        assertEquals(Position(1, 1), macro.location?.start)
        assertEquals(Position(1, 22), macro.location?.end)
    }

    @Test
    fun customBlockMacroCarriesPositionalAndNamedAttributes() {
        val doc = parser.parseDocument("chart::data.csv[bar,height=400]")
        val macro = assertIs<CustomBlockMacro>(doc.blocks.single())
        assertEquals("data.csv", macro.target)
        assertEquals(listOf("bar"), macro.metadata?.positional)
        assertEquals(mapOf("height" to "400"), macro.metadata?.named)
    }

    @Test
    fun customBlockMacroWithEmptyTargetHasNullTarget() {
        val doc = parser.parseDocument("gallery::[]")
        val macro = assertIs<CustomBlockMacro>(doc.blocks.single())
        assertNull(macro.target)
    }

    @Test
    fun attributeLineAboveCustomBlockMacroMergesIntoMetadata() {
        val doc = parser.parseDocument("[#photos.wide]\ngallery::dir[]")
        val macro = assertIs<CustomBlockMacro>(doc.blocks.single())
        assertEquals("photos", macro.metadata?.id)
        assertEquals(listOf("wide"), macro.metadata?.roles)
    }

    @Test
    fun macroNamesMayContainDigitsHyphensAndUnderscores() {
        val doc = parser.parseDocument("plant-uml_2::diagram.puml[]")
        val macro = assertIs<CustomBlockMacro>(doc.blocks.single())
        assertEquals("plant-uml_2", macro.name)
    }

    @Test
    fun builtInBlockMacroStaysBlockMacro() {
        val doc = parser.parseDocument("image::diagram.svg[Alt]")
        val macro = assertIs<BlockMacro>(doc.blocks.single())
        assertEquals(BlockMacroName.IMAGE, macro.name)
    }

    @Test
    fun includeDirectiveIsNotACustomBlockMacro() {
        val doc = parser.parseDocument("include::chapter.adoc[]")
        assertIs<IncludeBlock>(doc.blocks.single())
    }

    @Test
    fun malformedIncludeWithEmptyTargetFallsBackToParagraphNotMacro() {
        val doc = parser.parseDocument("include::[]")
        val paragraph = assertIs<LeafBlock>(doc.blocks.single())
        assertEquals(LeafBlockName.PARAGRAPH, paragraph.name)
        assertEquals("include::[]", plainText(paragraph.inlines))
    }

    @Test
    fun descriptionListIsUnaffected() {
        val doc = parser.parseDocument("term:: description")
        val dlist = assertIs<DListBlock>(doc.blocks.single())
        assertEquals("term", plainText(dlist.items.single().terms.single()))
        assertEquals("description", plainText(dlist.items.single().principal))
    }

    @Test
    fun macroShapedLineWithoutBracketsStaysParagraph() {
        val doc = parser.parseDocument("gallery::photos")
        val paragraph = assertIs<LeafBlock>(doc.blocks.single())
        val text = assertIs<InlineText>(paragraph.inlines.single())
        assertEquals("gallery::photos", text.value)
    }

    @Test
    fun customBlockMacroBetweenParagraphsKeepsSiblings() {
        val doc = parser.parseDocument("before\n\ngallery::dir[]\n\nafter")
        assertEquals(3, doc.blocks.size)
        assertIs<LeafBlock>(doc.blocks[0])
        assertIs<CustomBlockMacro>(doc.blocks[1])
        assertIs<LeafBlock>(doc.blocks[2])
    }
}
