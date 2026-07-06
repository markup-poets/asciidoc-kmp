package org.markup.poet.tck.conformance

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.BlockMacro
import org.markup.poet.asciidoc.asg.BlockMacroName
import org.markup.poet.asciidoc.asg.ConditionalBlock
import org.markup.poet.asciidoc.asg.InlineMacro
import org.markup.poet.asciidoc.asg.InlineRef
import org.markup.poet.asciidoc.asg.InlineSpan
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.ListBlock
import org.markup.poet.asciidoc.asg.ListVariant
import org.markup.poet.asciidoc.asg.RefVariant
import org.markup.poet.asciidoc.asg.SectionBlock
import org.markup.poet.asciidoc.asg.SpanVariant
import org.markup.poet.asciidoc.asg.TableBlock
import org.markup.poet.asciidoc.asg.plainText
import org.markup.poet.asciidoc.parser.DefaultAsciidocParser
import org.markup.poet.asciidoc.processing.ConditionalConfig
import org.markup.poet.asciidoc.processing.DefaultConditionalProcessor
import org.markup.poet.tck.compatibility.CompatibilityTest
import org.markup.poet.tck.fixtures.FixtureLoader
import org.markup.poet.tck.fixtures.ResourceFixtureLoader
import org.markup.poet.tck.validation.DefaultOutputValidator
import org.markup.poet.tck.validation.OutputValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * AsciiDoc Specification Conformance Tests
 *
 * These tests validate conformance to the AsciiDoc specification using
 * fixtures derived from the official spec examples and documentation.
 *
 * Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7, 8.8
 *
 * ## Interpretation Notes
 *
 * When the AsciiDoc specification is ambiguous, this test suite documents
 * the interpretation used:
 *
 * 1. **List Continuation**: The `+` symbol on a line by itself continues
 *    the previous list item, allowing multiple blocks within a single item.
 *
 * 2. **Attribute Precedence**: Document attributes defined in the header
 *    take precedence over attributes defined later in the document.
 *
 * 3. **Table Cell Formatting**: Inline formatting within table cells is
 *    processed after the table structure is parsed.
 *
 * 4. **Section Nesting**: Sections can be nested up to 6 levels deep
 *    (corresponding to heading levels 1-6). Level 0 is the document title.
 *
 * 5. **Macro Attribute Parsing**: Macro attributes are parsed as positional
 *    first, then named. Positional attributes fill in the first N named
 *    attributes in order.
 */
class ConformanceTest : CompatibilityTest() {
    override val fixtureLoader: FixtureLoader = ResourceFixtureLoader()
    override val validator: OutputValidator = DefaultOutputValidator()

    private val parser = DefaultAsciidocParser()

    private fun parse(source: String): AsgDocument = parser.parse(source).document

    // Document Structure Tests (Requirements 8.1, 8.2)

    @Test
    fun `should parse document with header and author`() {
        // Fixture: conformance-document-structure-header
        // The line directly below the title becomes author metadata, not a paragraph.
        val document = parse(
            "= Document Title\nJohn Doe <john.doe@example.com>\n:revdate: 2024-01-15\n\nDocument content."
        )
        assertEquals("Document Title", plainText(assertNotNull(document.header).title))
        assertEquals("John Doe", document.attributes["author"])
        assertEquals("john.doe@example.com", document.attributes["email"])
        assertEquals("John", document.attributes["firstname"])
        assertEquals("Doe", document.attributes["lastname"])
        assertEquals("2024-01-15", document.attributes["revdate"])
        val paragraph = assertIs<LeafBlock>(document.blocks.single())
        assertEquals("Document content.", plainText(paragraph.inlines))
    }

    @Test
    fun `should parse document with preamble`() {
        // Fixture: conformance-document-structure-preamble
        // Content before the first section is treated as preamble.
        val document = parse(
            "= Document Title\n\nThis is the preamble.\nIt comes before any sections.\n\n" +
                "== First Section\n\nSection content here."
        )
        assertEquals("Document Title", plainText(assertNotNull(document.header).title))
        val preamble = assertIs<LeafBlock>(document.blocks[0])
        assertEquals("This is the preamble.\nIt comes before any sections.", plainText(preamble.inlines))
        val section = assertIs<SectionBlock>(document.blocks[1])
        assertEquals("First Section", plainText(section.title))
    }

    @Test
    fun `should parse hierarchical section structure`() {
        // Fixture: conformance-document-structure-sections
        // Interpretation: Sections can nest up to 6 levels (heading levels 1-6)
        val document = parse(
            "= Document Title\n\n== Level 1 Section\n\nContent for level 1.\n\n" +
                "=== Level 2 Section\n\nContent for level 2.\n\n" +
                "==== Level 3 Section\n\nContent for level 3.\n\n" +
                "== Another Level 1 Section\n\nMore content."
        )
        val topSections = document.blocks.filterIsInstance<SectionBlock>()
        assertEquals(2, topSections.size)
        assertEquals(listOf(1, 1), topSections.map { it.level })

        val level2 = assertIs<SectionBlock>(topSections[0].blocks.last())
        assertEquals(2, level2.level)
        assertEquals("Level 2 Section", plainText(level2.title))
        val level3 = assertIs<SectionBlock>(level2.blocks.last())
        assertEquals(3, level3.level)
        assertEquals("Level 3 Section", plainText(level3.title))
    }

    // Attribute Processing Tests (Requirements 8.3)

    @Test
    fun `should parse document attributes in header`() {
        // Fixture: conformance-attribute-document-attributes
        val document = parse(
            "= Document Title\n:author: John Doe\n:email: john@example.com\n" +
                ":revdate: 2024-01-15\n:version: 1.0\n\nDocument content with attributes."
        )
        assertEquals("John Doe", document.attributes["author"])
        assertEquals("john@example.com", document.attributes["email"])
        assertEquals("2024-01-15", document.attributes["revdate"])
        assertEquals("1.0", document.attributes["version"])
    }

    @Test
    fun `should substitute attribute references in content`() {
        // Fixture: conformance-attribute-substitution
        // {attribute-name} references in text are replaced with the attribute value.
        val document = parse(
            "= Product Guide\n:product-name: AsciiDoc Converter\n:version: 1.0.0\n\n" +
                "Welcome to {product-name} version {version}."
        )
        val paragraph = assertIs<LeafBlock>(document.blocks.single())
        assertEquals("Welcome to AsciiDoc Converter version 1.0.0.", plainText(paragraph.inlines))
    }

    @Test
    fun `should process conditional directives`() {
        // Fixture: conformance-attribute-conditional
        // ifdef/ifndef conditional content inclusion.
        val document = parse(
            "ifdef::feature-enabled[]\nThis content is shown when feature-enabled is set.\nendif::[]\n\n" +
                "ifndef::feature-disabled[]\nThis content is shown when feature-disabled is not set.\nendif::[]"
        )
        assertEquals(2, document.blocks.filterIsInstance<ConditionalBlock>().size)

        val result = DefaultConditionalProcessor().process(
            document,
            ConditionalConfig(definedAttributes = setOf("feature-enabled")),
        )
        assertEquals(2, result.evaluatedConditionals)
        assertTrue(result.errors.isEmpty())
        val paragraphs = result.document.blocks.filterIsInstance<LeafBlock>()
        assertEquals(
            listOf(
                "This content is shown when feature-enabled is set.",
                "This content is shown when feature-disabled is not set.",
            ),
            paragraphs.map { plainText(it.inlines) },
        )
        assertTrue(result.document.blocks.none { it is ConditionalBlock })
    }

    // Macro Syntax Tests (Requirements 8.4)

    @Test
    fun `should parse link macro with text`() {
        // Fixture: conformance-macro-link
        // link:url[text] produces the same LINK ref as a bare-URL autolink.
        val document = parse("Visit link:https://example.com[Example Website] for more information.")
        val paragraph = assertIs<LeafBlock>(document.blocks.single())
        val link = paragraph.inlines.filterIsInstance<InlineRef>().single()
        assertEquals(RefVariant.LINK, link.variant)
        assertEquals("https://example.com", link.target)
        assertEquals("Example Website", plainText(link.inlines))
        assertEquals("Visit Example Website for more information.", plainText(paragraph.inlines))

        // Empty brackets fall back to the target as the link text.
        val bare = parse("See link:https://example.org[] here.")
        val bareLink = assertIs<LeafBlock>(bare.blocks.single()).inlines.filterIsInstance<InlineRef>().single()
        assertEquals("https://example.org", bareLink.target)
        assertEquals("https://example.org", plainText(bareLink.inlines))
    }

    @Test
    fun `should parse image macro with attributes`() {
        // Fixture: conformance-macro-image
        // Interpretation: First 3 positional args are alt, width, height
        val document = parse("image::diagram.png[Diagram,800,600,align=center]")
        val image = assertIs<BlockMacro>(document.blocks.single())
        assertEquals(BlockMacroName.IMAGE, image.name)
        assertEquals("diagram.png", image.target)
        val metadata = assertNotNull(image.metadata)
        assertEquals(listOf("Diagram", "800", "600"), metadata.positional)
        assertEquals("center", metadata.named["align"])
    }

    @Test
    fun `should parse inline passthrough macro`() {
        // Fixture: conformance-macro-inline-pass
        val document = parse("Use pass:[<u>underlined text</u>] for custom HTML.")
        val paragraph = assertIs<LeafBlock>(document.blocks.single())
        val macro = paragraph.inlines.filterIsInstance<InlineMacro>().single()
        assertEquals("pass", macro.name)
        assertEquals(listOf("<u>underlined text</u>"), macro.positional)
    }

    // List Nesting Tests (Requirements 8.5)

    @Test
    fun `should parse deeply nested unordered list`() {
        // Fixture: conformance-list-nesting-deep
        // Interpretation: Nesting depth is unlimited but practical limit is 5-6 levels
        val document = parse(
            "* Level 1\n** Level 2\n*** Level 3\n**** Level 4\n***** Level 5\n" +
                "**** Back to level 4\n** Back to level 2\n* Another level 1"
        )
        var list = assertIs<ListBlock>(document.blocks.single())
        assertEquals(listOf("Level 1", "Another level 1"), list.items.map { plainText(it.principal) })
        // Walk the nesting chain down to level 5.
        for (depth in 2..5) {
            list = assertIs<ListBlock>(list.items.first().blocks.single())
            assertEquals("*".repeat(depth), list.marker)
            assertEquals("Level $depth", plainText(list.items.first().principal))
        }
        assertTrue(list.items.isNotEmpty())
    }

    @Test
    fun `should parse mixed nested lists`() {
        // Fixture: conformance-list-nesting-mixed
        // Unordered and ordered lists mixed in one document, each nesting by marker depth.
        val document = parse(
            "* Item 1\n** Nested item 1.1\n** Nested item 1.2\n*** Deep nested 1.2.1\n* Item 2\n\n" +
                ". Ordered 1\n.. Ordered nested 1.1\n. Ordered 2"
        )
        val lists = document.blocks.filterIsInstance<ListBlock>()
        assertEquals(2, lists.size)
        val unordered = lists[0]
        assertEquals(ListVariant.UNORDERED, unordered.variant)
        val nestedUnordered = assertIs<ListBlock>(unordered.items.first().blocks.single())
        assertEquals(2, nestedUnordered.items.size)
        assertIs<ListBlock>(nestedUnordered.items.last().blocks.single()) // level-3 nesting

        val ordered = lists[1]
        assertEquals(ListVariant.ORDERED, ordered.variant)
        val nestedOrdered = assertIs<ListBlock>(ordered.items.first().blocks.single())
        assertEquals("Ordered nested 1.1", plainText(nestedOrdered.items.single().principal))
    }

    @Test
    fun `should parse list with continuation`() {
        // Fixture: conformance-list-nesting-continuation
        // Interpretation note 1: a `+` line by itself attaches the following
        // block to the previous list item.
        val document = parse(
            "* First item\n+\nThis paragraph belongs to the first item.\n* Second item"
        )
        val list = assertIs<ListBlock>(document.blocks.single())
        assertEquals(2, list.items.size)
        val first = list.items.first()
        assertEquals("First item", plainText(first.principal))
        val attached = assertIs<LeafBlock>(first.blocks.single())
        assertEquals("This paragraph belongs to the first item.", plainText(attached.inlines))
        assertEquals("Second item", plainText(list.items.last().principal))
        assertTrue(list.items.last().blocks.isEmpty())
    }

    // Table Syntax Tests (Requirements 8.6)

    @Test
    fun `should parse basic table with header`() {
        // Fixture: conformance-table-basic
        val document = parse(
            "[cols=\"1,1,1\", options=\"header\"]\n|===\n|Name |Age |City\n\n" +
                "|Alice\n|30\n|New York\n\n|Bob\n|25\n|London\n|==="
        )
        val table = assertIs<TableBlock>(document.blocks.single())
        assertEquals(3, table.columns.size)
        val header = assertNotNull(table.header)
        assertEquals(listOf("Name", "Age", "City"), header.cells.map { plainText(it.inlines) })
        assertEquals(
            listOf(listOf("Alice", "30", "New York"), listOf("Bob", "25", "London")),
            table.rows.map { row -> row.cells.map { plainText(it.inlines) } },
        )
    }

    @Test
    fun `should parse table with column spans`() {
        // Fixture: conformance-table-column-spans
        // `2+|` makes a cell span two columns.
        val document = parse("|===\n|A |B\n2+|Spans both columns\n|===")
        val table = assertIs<TableBlock>(document.blocks.single())
        assertEquals(2, table.columns.size)
        assertEquals(2, table.rows.size)
        val spanning = table.rows.last().cells.single()
        assertEquals(2, spanning.colSpan)
        assertEquals("Spans both columns", plainText(spanning.inlines))
    }

    @Test
    fun `should parse table with formatted cells`() {
        // Fixture: conformance-table-formatted-cells
        // Interpretation note 3: cell content is inline-parsed after the table
        // structure, so formatting spans work inside cells.
        val document = parse("|===\n|*bold* |_italic_\n|`mono` |plain\n|===")
        val table = assertIs<TableBlock>(document.blocks.single())
        val (firstRow, secondRow) = table.rows
        val bold = firstRow.cells[0].inlines.filterIsInstance<InlineSpan>().single()
        assertEquals(SpanVariant.STRONG, bold.variant)
        assertEquals("bold", plainText(bold.inlines))
        val italic = firstRow.cells[1].inlines.filterIsInstance<InlineSpan>().single()
        assertEquals(SpanVariant.EMPHASIS, italic.variant)
        val mono = secondRow.cells[0].inlines.filterIsInstance<InlineSpan>().single()
        assertEquals(SpanVariant.CODE, mono.variant)
        assertEquals("plain", plainText(secondRow.cells[1].inlines))
    }
}
