package org.markup.poet.asciidoc.parser.asg

import org.markup.poet.asciidoc.asg.InlineSpan
import org.markup.poet.asciidoc.asg.InlineText
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.LeafBlockName
import org.markup.poet.asciidoc.asg.ListBlock
import org.markup.poet.asciidoc.asg.ParentBlock
import org.markup.poet.asciidoc.asg.ParentBlockName
import org.markup.poet.asciidoc.asg.Position
import org.markup.poet.asciidoc.asg.SectionBlock
import org.markup.poet.asciidoc.asg.SpanForm
import org.markup.poet.asciidoc.asg.SpanVariant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit-level mirror of the official TCK fixture semantics. The official Node
 * harness (`./run-official-tck.sh`) is the conformance gate; these tests keep
 * the same behaviors covered in the fast Gradle loop.
 */
class BlockTreeParserTest {

    private val parser = BlockTreeParser()

    @Test
    fun singleLineParagraphHasInclusiveLocations() {
        val doc = parser.parseDocument("body only")
        val paragraph = assertIs<LeafBlock>(doc.blocks.single())
        assertEquals(LeafBlockName.PARAGRAPH, paragraph.name)
        val text = assertIs<InlineText>(paragraph.inlines.single())
        assertEquals("body only", text.value)
        assertEquals(Position(1, 1), text.location?.start)
        assertEquals(Position(1, 9), text.location?.end)
        assertEquals(Position(1, 9), doc.location?.end)
    }

    @Test
    fun multiLineParagraphCoalescesIntoOneTextNodeWithNewline() {
        val doc = parser.parseDocument("line one\nline two!")
        val paragraph = assertIs<LeafBlock>(doc.blocks.single())
        val text = assertIs<InlineText>(paragraph.inlines.single())
        assertEquals("line one\nline two!", text.value)
        assertEquals(Position(2, 9), text.location?.end)
    }

    @Test
    fun emptyLinesSeparateSiblingParagraphs() {
        val doc = parser.parseDocument("first\n\n\nsecond")
        assertEquals(2, doc.blocks.size)
        val second = assertIs<LeafBlock>(doc.blocks[1])
        assertEquals(Position(4, 1), second.location?.start)
    }

    @Test
    fun documentHeaderWithTitleAndAttributes() {
        val doc = parser.parseDocument("= Document Title\n:icons: font\n:toc:\n")
        val header = doc.header!!
        val title = assertIs<InlineText>(header.title.single())
        assertEquals("Document Title", title.value)
        assertEquals(Position(1, 3), title.location?.start)
        assertEquals(Position(1, 16), title.location?.end)
        assertEquals(Position(3, 5), header.location?.end)
        assertEquals(mapOf("icons" to "font", "toc" to ""), doc.attributes)
    }

    @Test
    fun headerBodyDocument() {
        val doc = parser.parseDocument("= Document Title\n\nbody")
        assertEquals(Position(1, 16), doc.header?.location?.end)
        val paragraph = assertIs<LeafBlock>(doc.blocks.single())
        assertEquals(Position(3, 1), paragraph.location?.start)
    }

    @Test
    fun sectionNestsItsBlocksAndTitleInlines() {
        val doc = parser.parseDocument("== Section Title\n\nparagraph")
        val section = assertIs<SectionBlock>(doc.blocks.single())
        assertEquals(1, section.level)
        val title = assertIs<InlineText>(section.title.single())
        assertEquals(Position(1, 4), title.location?.start)
        assertEquals(Position(1, 16), title.location?.end)
        val child = assertIs<LeafBlock>(section.blocks.single())
        assertEquals(Position(3, 1), child.location?.start)
        assertEquals(Position(3, 9), section.location?.end)
    }

    @Test
    fun siblingSectionsDoNotNest() {
        val doc = parser.parseDocument("== One\n\ntext\n\n== Two\n\nmore")
        assertEquals(2, doc.blocks.size)
        val second = assertIs<SectionBlock>(doc.blocks[1])
        assertEquals("Two", assertIs<InlineText>(second.title.single()).value)
    }

    @Test
    fun deeperSectionNestsInsideShallowerOne() {
        val doc = parser.parseDocument("== Outer\n\n=== Inner\n\ntext")
        val outer = assertIs<SectionBlock>(doc.blocks.single())
        val inner = assertIs<SectionBlock>(outer.blocks.single())
        assertEquals(2, inner.level)
    }

    @Test
    fun constrainedStrongSpanIncludesDelimitersInLocation() {
        val inlines = parser.parseInline("*s*")
        val span = assertIs<InlineSpan>(inlines.single())
        assertEquals(SpanVariant.STRONG, span.variant)
        assertEquals(SpanForm.CONSTRAINED, span.form)
        assertEquals(Position(1, 1), span.location?.start)
        assertEquals(Position(1, 3), span.location?.end)
        val inner = assertIs<InlineText>(span.inlines.single())
        assertEquals(Position(1, 2), inner.location?.start)
        assertEquals(Position(1, 2), inner.location?.end)
    }

    @Test
    fun listingBlockKeepsDelimitersOutOfContentLocation() {
        val doc = parser.parseDocument("----\none\ntwo\n----")
        val listing = assertIs<LeafBlock>(doc.blocks.single())
        assertEquals(LeafBlockName.LISTING, listing.name)
        assertEquals("----", listing.delimiter)
        val text = assertIs<InlineText>(listing.inlines.single())
        assertEquals("one\ntwo", text.value)
        assertEquals(Position(2, 1), text.location?.start)
        assertEquals(Position(3, 3), text.location?.end)
        assertEquals(Position(1, 1), listing.location?.start)
        assertEquals(Position(4, 4), listing.location?.end)
    }

    @Test
    fun unorderedListUsesMarkerAndPrincipal() {
        val doc = parser.parseDocument("* phone\n* wallet")
        val list = assertIs<ListBlock>(doc.blocks.single())
        assertEquals("*", list.marker)
        assertEquals(2, list.items.size)
        val first = list.items[0]
        assertEquals("*", first.marker)
        val principal = assertIs<InlineText>(first.principal.single())
        assertEquals("phone", principal.value)
        assertEquals(Position(1, 3), principal.location?.start)
    }

    @Test
    fun sidebarContainsNestedBlocks() {
        val doc = parser.parseDocument("****\n* phone\n* wallet\n****")
        val sidebar = assertIs<ParentBlock>(doc.blocks.single())
        assertEquals(ParentBlockName.SIDEBAR, sidebar.name)
        assertEquals("****", sidebar.delimiter)
        assertIs<ListBlock>(sidebar.blocks.single())
        assertEquals(Position(4, 4), sidebar.location?.end)
    }

    @Test
    fun inlineMacroIsParsedWithTargetAndAttributes() {
        val inlines = parser.parseInline("see issue:123[title=Bug, urgent] now")
        assertEquals(3, inlines.size)
        val macro = assertIs<org.markup.poet.asciidoc.asg.InlineMacro>(inlines[1])
        assertEquals("issue", macro.name)
        assertEquals("123", macro.target)
        assertEquals(listOf("urgent"), macro.positional)
        assertEquals(mapOf("title" to "Bug"), macro.named)
        assertEquals("see ", assertIs<InlineText>(inlines[0]).value)
        assertEquals(" now", assertIs<InlineText>(inlines[2]).value)
    }

    @Test
    fun urlSchemesAreNotParsedAsInlineMacros() {
        val inlines = parser.parseInline("visit https://example.com[site] please")
        // Autolinks are not implemented yet; the URL must stay plain text, not become a macro.
        assertTrue(inlines.filterIsInstance<org.markup.poet.asciidoc.asg.InlineMacro>().isEmpty())
    }

    @Test
    fun wordWithColonInsideIsNotAMacro() {
        val inlines = parser.parseInline("ratio 1:2[approx]")
        // "2[approx]" — the name candidate "2..." starts mid-word, so no macro.
        assertTrue(inlines.filterIsInstance<org.markup.poet.asciidoc.asg.InlineMacro>().isEmpty())
    }

    @Test
    fun plainDocumentHasNoHeader() {
        val doc = parser.parseDocument("just text")
        assertNull(doc.header)
        assertEquals(emptyMap(), doc.attributes)
    }

    // -----------------------------------------------------------------------
    // B7 breadth
    // -----------------------------------------------------------------------

    @Test
    fun nestedListAttachesToParentItem() {
        val doc = parser.parseDocument("* one\n** one-a\n** one-b\n* two")
        val list = assertIs<ListBlock>(doc.blocks.single())
        assertEquals(2, list.items.size)
        val nested = assertIs<ListBlock>(list.items[0].blocks.single())
        assertEquals("**", nested.marker)
        assertEquals(2, nested.items.size)
        assertEquals("two", assertIs<InlineText>(list.items[1].principal.single()).value)
    }

    @Test
    fun numberedMarkersFormOneOrderedList() {
        val doc = parser.parseDocument("1. first\n2. second\n3. third")
        val list = assertIs<ListBlock>(doc.blocks.single())
        assertEquals(org.markup.poet.asciidoc.asg.ListVariant.ORDERED, list.variant)
        assertEquals(3, list.items.size)
    }

    @Test
    fun descriptionListParsesTermsAndPrincipal() {
        val doc = parser.parseDocument("CPU:: does the computing\nRAM:: remembers things")
        val dlist = assertIs<org.markup.poet.asciidoc.asg.DListBlock>(doc.blocks.single())
        assertEquals(2, dlist.items.size)
        val first = dlist.items[0]
        assertEquals("CPU", assertIs<InlineText>(first.terms.single().single()).value)
        assertEquals("does the computing", assertIs<InlineText>(first.principal.single()).value)
    }

    @Test
    fun descriptionOnFollowingLineBelongsToTerm() {
        val doc = parser.parseDocument("CPU::\nthe brain of the machine")
        val dlist = assertIs<org.markup.poet.asciidoc.asg.DListBlock>(doc.blocks.single())
        assertEquals(
            "the brain of the machine",
            assertIs<InlineText>(dlist.items.single().principal.single()).value,
        )
    }

    @Test
    fun admonitionParagraphBecomesAdmonitionBlock() {
        val doc = parser.parseDocument("NOTE: remember this")
        val admonition = assertIs<ParentBlock>(doc.blocks.single())
        assertEquals(ParentBlockName.ADMONITION, admonition.name)
        assertEquals("note", admonition.variant)
        assertNull(admonition.delimiter)
        val paragraph = assertIs<LeafBlock>(admonition.blocks.single())
        val text = assertIs<InlineText>(paragraph.inlines.single())
        assertEquals("remember this", text.value)
        assertEquals(Position(1, 7), text.location?.start)
    }

    @Test
    fun admonitionStyleOnExampleBlock() {
        val doc = parser.parseDocument("[WARNING]\n====\ncareful now\n====")
        val admonition = assertIs<ParentBlock>(doc.blocks.single())
        assertEquals(ParentBlockName.ADMONITION, admonition.name)
        assertEquals("warning", admonition.variant)
        assertEquals("====", admonition.delimiter)
        assertIs<LeafBlock>(admonition.blocks.single())
    }

    @Test
    fun breaksAreParsed() {
        val doc = parser.parseDocument("before\n\n'''\n\n<<<\n\nafter")
        assertEquals(4, doc.blocks.size)
        val thematic = assertIs<org.markup.poet.asciidoc.asg.BreakBlock>(doc.blocks[1])
        assertEquals(org.markup.poet.asciidoc.asg.BreakVariant.THEMATIC, thematic.variant)
        val page = assertIs<org.markup.poet.asciidoc.asg.BreakBlock>(doc.blocks[2])
        assertEquals(org.markup.poet.asciidoc.asg.BreakVariant.PAGE, page.variant)
    }

    @Test
    fun imageBlockMacroIsParsed() {
        val doc = parser.parseDocument("image::diagram.svg[Architecture]")
        val macro = assertIs<org.markup.poet.asciidoc.asg.BlockMacro>(doc.blocks.single())
        assertEquals(org.markup.poet.asciidoc.asg.BlockMacroName.IMAGE, macro.name)
        assertEquals("diagram.svg", macro.target)
        assertEquals("Architecture", macro.metadata?.positional?.firstOrNull())
    }

    @Test
    fun shorthandMetadataAndTitleAttachToBlock() {
        val doc = parser.parseDocument(".Listing Title\n[source#main.primary%nowrap,kotlin]\n----\nfun x() {}\n----")
        val listing = assertIs<LeafBlock>(doc.blocks.single())
        val metadata = listing.metadata!!
        assertEquals("source", metadata.positional.firstOrNull())
        assertEquals("kotlin", metadata.positional.getOrNull(1))
        assertEquals("main", metadata.id)
        assertEquals(listOf("primary"), metadata.roles)
        assertEquals(listOf("nowrap"), metadata.options)
        assertEquals("Listing Title", assertIs<InlineText>(metadata.title!!.single()).value)
    }

    @Test
    fun discreteHeadingOpensNoSection() {
        val doc = parser.parseDocument("[discrete]\n== Standalone\n\ntext after")
        assertEquals(2, doc.blocks.size)
        val heading = assertIs<org.markup.poet.asciidoc.asg.DiscreteHeading>(doc.blocks[0])
        assertEquals(1, heading.level)
        assertEquals("Standalone", assertIs<InlineText>(heading.title.single()).value)
        assertIs<LeafBlock>(doc.blocks[1]) // paragraph is a sibling, not a section child
    }

    @Test
    fun bareUrlBecomesLinkRef() {
        val inlines = parser.parseInline("docs at https://example.com/spec. done")
        val ref = inlines.filterIsInstance<org.markup.poet.asciidoc.asg.InlineRef>().single()
        assertEquals("https://example.com/spec", ref.target) // trailing dot stays outside
        assertEquals(org.markup.poet.asciidoc.asg.RefVariant.LINK, ref.variant)
    }

    @Test
    fun urlWithBracketTextBecomesLabeledLink() {
        val inlines = parser.parseInline("see https://example.com[the site] now")
        val ref = inlines.filterIsInstance<org.markup.poet.asciidoc.asg.InlineRef>().single()
        assertEquals("https://example.com", ref.target)
        assertEquals("the site", assertIs<InlineText>(ref.inlines.single()).value)
    }

    @Test
    fun definedAttributeReferenceIsSubstituted() {
        val doc = parser.parseDocument("= T\n:product: Markup Poet\n\nuse {product} and {undefined} here")
        val paragraph = assertIs<LeafBlock>(doc.blocks.single())
        val values = paragraph.inlines.map { assertIs<InlineText>(it).value }
        assertEquals(listOf("use ", "Markup Poet", " and {undefined} here"), values)
        // The substituted node's location spans the reference in the source.
        val substituted = paragraph.inlines[1]
        assertEquals(Position(4, 5), substituted.location?.start)
        assertEquals(Position(4, 13), substituted.location?.end)
    }
}
