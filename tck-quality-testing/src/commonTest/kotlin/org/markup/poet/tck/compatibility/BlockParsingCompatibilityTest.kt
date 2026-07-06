package org.markup.poet.tck.compatibility

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.InlineText
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.LeafBlockForm
import org.markup.poet.asciidoc.asg.LeafBlockName
import org.markup.poet.asciidoc.asg.ListBlock
import org.markup.poet.asciidoc.asg.ListVariant
import org.markup.poet.asciidoc.asg.ParentBlock
import org.markup.poet.asciidoc.asg.ParentBlockName
import org.markup.poet.asciidoc.asg.SectionBlock
import org.markup.poet.asciidoc.asg.TableBlock
import org.markup.poet.asciidoc.asg.TableColumnAlignment
import org.markup.poet.asciidoc.asg.plainText
import org.markup.poet.asciidoc.parser.DefaultAsciidocParser
import org.markup.poet.tck.fixtures.FixtureLoader
import org.markup.poet.tck.fixtures.ResourceFixtureLoader
import org.markup.poet.tck.validation.DefaultOutputValidator
import org.markup.poet.tck.validation.OutputValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Compatibility tests for block-level AsciiDoc parsing.
 *
 * These tests validate that block elements (paragraphs, headings, lists, etc.)
 * are parsed consistently across all platforms.
 *
 * Requirements: 2.1, 2.6
 */
class BlockParsingCompatibilityTest : CompatibilityTest() {
    override val fixtureLoader: FixtureLoader = ResourceFixtureLoader()
    override val validator: OutputValidator = DefaultOutputValidator()

    private val parser = DefaultAsciidocParser()

    private fun parse(source: String): AsgDocument = parser.parse(source).document

    // Paragraph Tests

    @Test
    fun `should parse simple paragraph`() {
        val document = parse("This is a simple paragraph.")
        val paragraph = assertIs<LeafBlock>(document.blocks.single())
        assertEquals(LeafBlockName.PARAGRAPH, paragraph.name)
        assertEquals(LeafBlockForm.PARAGRAPH, paragraph.form)
        assertEquals("This is a simple paragraph.", plainText(paragraph.inlines))
    }

    @Test
    fun `should parse multiline paragraph`() {
        val document = parse("First line of the paragraph.\nSecond line of the same paragraph.")
        val paragraph = assertIs<LeafBlock>(document.blocks.single())
        // Multi-line plain text is ONE text node with the `\n` inside its value.
        val text = assertIs<InlineText>(paragraph.inlines.single())
        assertEquals("First line of the paragraph.\nSecond line of the same paragraph.", text.value)
    }

    @Test
    fun `should parse multiple paragraphs separated by blank lines`() {
        val document = parse("Paragraph one.\n\nParagraph two.\n\nParagraph three.")
        val paragraphs = document.blocks.filterIsInstance<LeafBlock>()
        assertEquals(3, paragraphs.size)
        assertEquals(
            listOf("Paragraph one.", "Paragraph two.", "Paragraph three."),
            paragraphs.map { plainText(it.inlines) },
        )
    }

    // Heading Tests

    @Test
    fun `should parse heading level 1`() {
        val document = parse("== Level 1 Heading\n\nContent.")
        val section = assertIs<SectionBlock>(document.blocks.single())
        assertEquals(1, section.level)
        assertEquals("Level 1 Heading", plainText(section.title))
    }

    @Test
    fun `should parse heading level 2`() {
        val document = parse("=== Level 2 Heading\n\nContent.")
        val section = assertIs<SectionBlock>(document.blocks.single())
        assertEquals(2, section.level)
        assertEquals("Level 2 Heading", plainText(section.title))
    }

    @Test
    fun `should parse heading level 3`() {
        val document = parse("==== Level 3 Heading\n\nContent.")
        val section = assertIs<SectionBlock>(document.blocks.single())
        assertEquals(3, section.level)
        assertEquals("Level 3 Heading", plainText(section.title))
    }

    @Test
    fun `should parse all heading levels 1-6`() {
        // `=` is the document title (level 0); `==`..`======` are section levels 1-5.
        val document = parse(
            "= Level 0 (Document Title)\n\n== Level 1\n\n=== Level 2\n\n==== Level 3\n\n===== Level 4\n\n====== Level 5"
        )
        val header = assertNotNull(document.header)
        assertEquals("Level 0 (Document Title)", plainText(header.title))
        var section = assertIs<SectionBlock>(document.blocks.single())
        for (level in 1..5) {
            assertEquals(level, section.level)
            assertEquals("Level $level", plainText(section.title))
            if (level < 5) section = assertIs<SectionBlock>(section.blocks.single())
        }
    }

    @Test
    fun `should reject heading level 7 and above`() {
        // Asciidoctor caps headings at 6 markers; 7+ '=' stays a plain paragraph.
        val document = parse("======= Too deep\n\nContent.")
        val paragraph = assertIs<LeafBlock>(document.blocks.first())
        assertEquals(LeafBlockName.PARAGRAPH, paragraph.name)
        assertEquals("======= Too deep", plainText(paragraph.inlines))
        assertTrue(document.blocks.none { it is SectionBlock })
    }

    // List Tests

    @Test
    fun `should parse unordered list with single level`() {
        val document = parse("* First item\n* Second item\n* Third item")
        val list = assertIs<ListBlock>(document.blocks.single())
        assertEquals(ListVariant.UNORDERED, list.variant)
        assertEquals("*", list.marker)
        assertEquals(
            listOf("First item", "Second item", "Third item"),
            list.items.map { plainText(it.principal) },
        )
    }

    @Test
    fun `should parse ordered list with single level`() {
        val document = parse(". First step\n. Second step\n. Third step")
        val list = assertIs<ListBlock>(document.blocks.single())
        assertEquals(ListVariant.ORDERED, list.variant)
        assertEquals(3, list.items.size)
        assertEquals("First step", plainText(list.items.first().principal))
    }

    @Test
    fun `should parse nested unordered list`() {
        val document = parse("* Parent one\n** Child one\n** Child two\n* Parent two")
        val list = assertIs<ListBlock>(document.blocks.single())
        assertEquals(2, list.items.size)
        val nested = assertIs<ListBlock>(list.items.first().blocks.single())
        assertEquals(ListVariant.UNORDERED, nested.variant)
        assertEquals("**", nested.marker)
        assertEquals(listOf("Child one", "Child two"), nested.items.map { plainText(it.principal) })
    }

    @Test
    fun `should parse nested ordered list`() {
        val document = parse(". Step one\n.. Substep one\n.. Substep two\n. Step two")
        val list = assertIs<ListBlock>(document.blocks.single())
        assertEquals(ListVariant.ORDERED, list.variant)
        assertEquals(2, list.items.size)
        val nested = assertIs<ListBlock>(list.items.first().blocks.single())
        assertEquals(ListVariant.ORDERED, nested.variant)
        assertEquals(listOf("Substep one", "Substep two"), nested.items.map { plainText(it.principal) })
    }

    @Test
    fun `should parse mixed nested lists`() {
        val document = parse("* Item one\n** Nested item\n* Item two\n\n. First\n.. Sub first\n. Second")
        val lists = document.blocks.filterIsInstance<ListBlock>()
        assertEquals(2, lists.size)
        val (unordered, ordered) = lists
        assertEquals(ListVariant.UNORDERED, unordered.variant)
        assertEquals(ListVariant.ORDERED, ordered.variant)
        // Both list families nest by marker depth.
        assertIs<ListBlock>(unordered.items.first().blocks.single())
        assertIs<ListBlock>(ordered.items.first().blocks.single())
    }

    // Code Block Tests

    @Test
    fun `should parse code block with language`() {
        val document = parse("[source,kotlin]\n----\nval x = 1\n----")
        val listing = assertIs<LeafBlock>(document.blocks.single())
        assertEquals(LeafBlockName.LISTING, listing.name)
        assertEquals(LeafBlockForm.DELIMITED, listing.form)
        val metadata = assertNotNull(listing.metadata)
        assertEquals(listOf("source", "kotlin"), metadata.positional)
        assertEquals("val x = 1", plainText(listing.inlines))
    }

    @Test
    fun `should parse code block without language`() {
        val document = parse("----\nplain code\n----")
        val listing = assertIs<LeafBlock>(document.blocks.single())
        assertEquals(LeafBlockName.LISTING, listing.name)
        assertEquals(LeafBlockForm.DELIMITED, listing.form)
        assertEquals("plain code", plainText(listing.inlines))
    }

    @Test
    fun `should preserve whitespace in code blocks`() {
        val document = parse("----\n    indented line\n\ttab line\n  two spaces\n----")
        val listing = assertIs<LeafBlock>(document.blocks.single())
        val text = assertIs<InlineText>(listing.inlines.single())
        assertEquals("    indented line\n\ttab line\n  two spaces", text.value)
    }

    // Quote Block Tests

    @Test
    fun `should parse simple quote block`() {
        val document = parse("____\nA memorable quotation.\n____")
        val quote = assertIs<ParentBlock>(document.blocks.single())
        assertEquals(ParentBlockName.QUOTE, quote.name)
        val paragraph = assertIs<LeafBlock>(quote.blocks.single())
        assertEquals("A memorable quotation.", plainText(paragraph.inlines))
    }

    @Test
    fun `should parse quote block with attribution`() {
        val document = parse("[quote,Author Name,Source Title]\n____\nQuoted words.\n____")
        val quote = assertIs<ParentBlock>(document.blocks.single())
        assertEquals(ParentBlockName.QUOTE, quote.name)
        val metadata = assertNotNull(quote.metadata)
        assertEquals(listOf("quote", "Author Name", "Source Title"), metadata.positional)
        assertTrue(quote.blocks.isNotEmpty())
    }

    // Table Tests

    @Test
    fun `should parse simple table`() {
        val document = parse("|===\n|Cell 1 |Cell 2\n|Cell 3 |Cell 4\n|===")
        val table = assertIs<TableBlock>(document.blocks.single())
        assertNull(table.header)
        assertEquals(2, table.columns.size)
        assertEquals(2, table.rows.size)
        assertEquals(
            listOf(listOf("Cell 1", "Cell 2"), listOf("Cell 3", "Cell 4")),
            table.rows.map { row -> row.cells.map { plainText(it.inlines) } },
        )
    }

    @Test
    fun `should parse table with header row`() {
        // Implicit header: first row on the delimiter-adjacent line, then a blank line.
        val document = parse("|===\n|Name |Age\n\n|Alice |30\n|Bob |29\n|===")
        val table = assertIs<TableBlock>(document.blocks.single())
        val header = assertNotNull(table.header)
        assertEquals(listOf("Name", "Age"), header.cells.map { plainText(it.inlines) })
        assertEquals(
            listOf(listOf("Alice", "30"), listOf("Bob", "29")),
            table.rows.map { row -> row.cells.map { plainText(it.inlines) } },
        )

        // Explicit header via options="header" (no blank-line separation needed).
        val explicit = parse("[options=\"header\"]\n|===\n|Name |Age\n|Alice |30\n|===")
        val explicitTable = assertIs<TableBlock>(explicit.blocks.single())
        val explicitHeader = assertNotNull(explicitTable.header)
        assertEquals(listOf("Name", "Age"), explicitHeader.cells.map { plainText(it.inlines) })
        assertEquals(1, explicitTable.rows.size)
    }

    @Test
    fun `should parse table with column alignment`() {
        val document = parse("[cols=\"<,^,>\"]\n|===\n|Left |Center |Right\n|===")
        val table = assertIs<TableBlock>(document.blocks.single())
        assertEquals(
            listOf(TableColumnAlignment.LEFT, TableColumnAlignment.CENTER, TableColumnAlignment.RIGHT),
            table.columns.map { it.alignment },
        )
        val row = table.rows.single()
        assertEquals(listOf("Left", "Center", "Right"), row.cells.map { plainText(it.inlines) })

        // `N*` repetition expands to N identical columns.
        val repeated = parse("[cols=\"2*\"]\n|===\n|A |B\n|C |D\n|===")
        val repeatedTable = assertIs<TableBlock>(repeated.blocks.single())
        assertEquals(2, repeatedTable.columns.size)
        assertEquals(2, repeatedTable.rows.size)
    }
}
