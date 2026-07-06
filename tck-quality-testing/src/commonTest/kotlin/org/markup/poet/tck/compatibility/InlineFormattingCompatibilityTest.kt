package org.markup.poet.tck.compatibility

import org.markup.poet.asciidoc.asg.Inline
import org.markup.poet.asciidoc.asg.InlineSpan
import org.markup.poet.asciidoc.asg.InlineText
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.LeafBlockName
import org.markup.poet.asciidoc.asg.SpanVariant
import org.markup.poet.asciidoc.asg.plainText
import org.markup.poet.asciidoc.parser.DefaultAsciidocParser
import org.markup.poet.tck.fixtures.FixtureLoader
import org.markup.poet.tck.fixtures.ResourceFixtureLoader
import org.markup.poet.tck.validation.DefaultOutputValidator
import org.markup.poet.tck.validation.OutputValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Compatibility tests for inline formatting in AsciiDoc.
 *
 * These tests validate that inline formatting (bold, italic, monospace, etc.)
 * is parsed and rendered consistently across all platforms.
 *
 * Requirements: 2.1, 2.6
 */
class InlineFormattingCompatibilityTest : CompatibilityTest() {
    override val fixtureLoader: FixtureLoader = ResourceFixtureLoader()
    override val validator: OutputValidator = DefaultOutputValidator()

    private val parser = DefaultAsciidocParser()

    /** Parses [source] and returns the inlines of its first (paragraph) block. */
    private fun parseInlines(source: String): List<Inline> {
        val document = parser.parse(source).document
        val paragraph = document.blocks.filterIsInstance<LeafBlock>().first()
        return paragraph.inlines
    }

    private fun List<Inline>.spans(variant: SpanVariant): List<InlineSpan> =
        filterIsInstance<InlineSpan>().filter { it.variant == variant }

    // Bold Formatting Tests

    @Test
    fun `should parse simple bold text`() {
        val inlines = parseInlines("This is *bold text* in a sentence.")
        val bold = inlines.spans(SpanVariant.STRONG).single()
        assertEquals("bold text", plainText(bold.inlines))
        assertEquals("This is bold text in a sentence.", plainText(inlines))
    }

    @Test
    fun `should parse bold text at start of line`() {
        val inlines = parseInlines("*Bold* at the start.")
        val first = assertIs<InlineSpan>(inlines.first())
        assertEquals(SpanVariant.STRONG, first.variant)
        assertEquals("Bold", plainText(first.inlines))
    }

    @Test
    fun `should parse bold text at end of line`() {
        val inlines = parseInlines("Line that ends *bold*")
        val last = assertIs<InlineSpan>(inlines.last())
        assertEquals(SpanVariant.STRONG, last.variant)
        assertEquals("bold", plainText(last.inlines))
    }

    @Test
    fun `should parse multiple bold sections in same line`() {
        val inlines = parseInlines("Both *first* and *second* are bold.")
        val bolds = inlines.spans(SpanVariant.STRONG)
        assertEquals(2, bolds.size)
        assertEquals(listOf("first", "second"), bolds.map { plainText(it.inlines) })
    }

    @Test
    fun `should parse nested bold formatting`() {
        val inlines = parseInlines("*bold with _italic_ inside*")
        val bold = inlines.spans(SpanVariant.STRONG).single()
        val nested = bold.inlines.spans(SpanVariant.EMPHASIS).single()
        assertEquals("italic", plainText(nested.inlines))
        assertEquals("bold with italic inside", plainText(bold.inlines))
    }

    // Italic Formatting Tests

    @Test
    fun `should parse simple italic text`() {
        val inlines = parseInlines("This is _italic text_ in a sentence.")
        val italic = inlines.spans(SpanVariant.EMPHASIS).single()
        assertEquals("italic text", plainText(italic.inlines))
    }

    @Test
    fun `should parse italic text at start of line`() {
        val inlines = parseInlines("_Italic_ at the start.")
        val first = assertIs<InlineSpan>(inlines.first())
        assertEquals(SpanVariant.EMPHASIS, first.variant)
        assertEquals("Italic", plainText(first.inlines))
    }

    @Test
    fun `should parse italic text at end of line`() {
        val inlines = parseInlines("Line that ends _italic_")
        val last = assertIs<InlineSpan>(inlines.last())
        assertEquals(SpanVariant.EMPHASIS, last.variant)
        assertEquals("italic", plainText(last.inlines))
    }

    @Test
    fun `should parse multiple italic sections in same line`() {
        val inlines = parseInlines("Both _first_ and _second_ are italic.")
        val italics = inlines.spans(SpanVariant.EMPHASIS)
        assertEquals(2, italics.size)
        assertEquals(listOf("first", "second"), italics.map { plainText(it.inlines) })
    }

    // Monospace Formatting Tests

    @Test
    fun `should parse simple monospace text`() {
        val inlines = parseInlines("Use the `parse` function.")
        val code = inlines.spans(SpanVariant.CODE).single()
        assertEquals("parse", plainText(code.inlines))
    }

    @Test
    fun `should parse monospace with backticks`() {
        val inlines = parseInlines("Text with `backtick` formatting")
        val code = inlines.spans(SpanVariant.CODE).single()
        assertEquals("backtick", plainText(code.inlines))
    }

    @Test
    fun `should preserve spaces in monospace text`() {
        val inlines = parseInlines("`a  b   c`")
        val code = inlines.spans(SpanVariant.CODE).single()
        assertEquals("a  b   c", plainText(code.inlines))
    }

    // Subscript and Superscript Tests

    @Test
    fun `should parse subscript text`() {
        pending("Subscript spans (~sub~) not yet implemented in the ASG inline parser")
    }

    @Test
    fun `should parse superscript text`() {
        pending("Superscript spans (^super^) not yet implemented in the ASG inline parser")
    }

    // Combined Formatting Tests

    @Test
    fun `should parse bold and italic together`() {
        val inlines = parseInlines("*_bold italic_*")
        val bold = inlines.spans(SpanVariant.STRONG).single()
        val italic = bold.inlines.spans(SpanVariant.EMPHASIS).single()
        assertEquals("bold italic", plainText(italic.inlines))
    }

    @Test
    fun `should parse bold italic and monospace in same paragraph`() {
        val inlines = parseInlines("Mix *bold* with _italic_ and `mono` text.")
        assertEquals("bold", plainText(inlines.spans(SpanVariant.STRONG).single().inlines))
        assertEquals("italic", plainText(inlines.spans(SpanVariant.EMPHASIS).single().inlines))
        assertEquals("mono", plainText(inlines.spans(SpanVariant.CODE).single().inlines))
    }

    @Test
    fun `should parse nested formatting combinations`() {
        val inlines = parseInlines("*bold with `code` inside*")
        val bold = inlines.spans(SpanVariant.STRONG).single()
        val code = bold.inlines.spans(SpanVariant.CODE).single()
        assertEquals("code", plainText(code.inlines))
    }

    // Edge Cases

    @Test
    fun `should handle unclosed formatting markers`() {
        val source = "This has *unclosed bold"
        val inlines = parseInlines(source)
        // No span is produced; the marker stays literal in a single text run.
        val text = assertIs<InlineText>(inlines.single())
        assertEquals(source, text.value)
    }

    @Test
    fun `should handle empty formatting markers`() {
        val source = "Some ** empty and __ markers."
        val inlines = parseInlines(source)
        val text = assertIs<InlineText>(inlines.single())
        assertEquals(source, text.value)
    }

    @Test
    fun `should handle formatting markers in code blocks`() {
        val document = parser.parse("----\n*not bold* and _not italic_\n----").document
        val listing = document.blocks.filterIsInstance<LeafBlock>().single()
        assertEquals(LeafBlockName.LISTING, listing.name)
        // Verbatim content: markers are preserved literally, no spans created.
        assertTrue(listing.inlines.all { it is InlineText })
        assertEquals("*not bold* and _not italic_", plainText(listing.inlines))
    }

    @Test
    fun `should handle escaped formatting markers`() {
        val inlines = parseInlines("""\*not bold* stays literal""")
        val text = assertIs<InlineText>(inlines.single())
        assertEquals("*not bold* stays literal", text.value)
    }
}
