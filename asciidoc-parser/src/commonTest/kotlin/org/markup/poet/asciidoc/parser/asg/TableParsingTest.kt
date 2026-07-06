package org.markup.poet.asciidoc.parser.asg

import org.markup.poet.asciidoc.asg.InlineSpan
import org.markup.poet.asciidoc.asg.InlineText
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.LeafBlockName
import org.markup.poet.asciidoc.asg.SpanVariant
import org.markup.poet.asciidoc.asg.TableBlock
import org.markup.poet.asciidoc.asg.TableColumnAlignment
import org.markup.poet.asciidoc.asg.plainText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Unit tests for `|===` table parsing (an ASG extension node, see [TableBlock]). */
class TableParsingTest {

    private val parser = BlockTreeParser()

    private fun parseTable(source: String): TableBlock {
        val doc = parser.parseDocument(source)
        return assertIs<TableBlock>(doc.blocks.single())
    }

    private fun cellTexts(table: TableBlock): List<List<String>> =
        table.rows.map { row -> row.cells.map { plainText(it.inlines) } }

    @Test
    fun simpleTableWithMultipleCellsPerLine() {
        val table = parseTable("|===\n|Cell 1 |Cell 2\n|Cell 3 |Cell 4\n|===")
        assertNull(table.header)
        assertEquals(2, table.columns.size)
        assertEquals(listOf(listOf("Cell 1", "Cell 2"), listOf("Cell 3", "Cell 4")), cellTexts(table))
    }

    @Test
    fun oneCellPerLineGroupsRowsByColumnCount() {
        val table = parseTable("[cols=\"1,1\"]\n|===\n|A\n|B\n|C\n|D\n|===")
        assertEquals(listOf(listOf("A", "B"), listOf("C", "D")), cellTexts(table))
    }

    @Test
    fun implicitHeaderNeedsAdjacentFirstRowAndBlankLine() {
        val table = parseTable("|===\n|Name |Age\n\n|Alice |30\n|===")
        val header = assertNotNull(table.header)
        assertEquals(listOf("Name", "Age"), header.cells.map { plainText(it.inlines) })
        assertEquals(listOf(listOf("Alice", "30")), cellTexts(table))

        // No blank line after the first row: no implicit header.
        val plain = parseTable("|===\n|Name |Age\n|Alice |30\n|===")
        assertNull(plain.header)
        assertEquals(2, plain.rows.size)
    }

    @Test
    fun explicitHeaderOptionMarksFirstRow() {
        val optionsAttr = parseTable("[options=\"header\"]\n|===\n|H1 |H2\n|a |b\n|===")
        assertNotNull(optionsAttr.header)
        assertEquals(listOf(listOf("a", "b")), cellTexts(optionsAttr))

        val shorthand = parseTable("[%header]\n|===\n|H1 |H2\n|a |b\n|===")
        assertNotNull(shorthand.header)
        assertEquals(listOf(listOf("a", "b")), cellTexts(shorthand))
    }

    @Test
    fun headerOnlyTableKeepsItsSingleRowAsBody() {
        val table = parseTable("[%header]\n|===\n|Only |Row\n|===")
        assertNull(table.header)
        assertEquals(1, table.rows.size)
    }

    @Test
    fun colsAttributeDefinesAlignmentAndCount() {
        val table = parseTable("[cols=\"<,^,>\"]\n|===\n|l |c |r\n|===")
        assertEquals(
            listOf(TableColumnAlignment.LEFT, TableColumnAlignment.CENTER, TableColumnAlignment.RIGHT),
            table.columns.map { it.alignment },
        )
        assertEquals(listOf(listOf("l", "c", "r")), cellTexts(table))
    }

    @Test
    fun colsRepetitionAndWidths() {
        val repeated = parseTable("[cols=\"3*\"]\n|===\n|a |b |c\n|===")
        assertEquals(3, repeated.columns.size)

        val widths = parseTable("[cols=\"1,2,3\"]\n|===\n|a |b |c\n|===")
        assertEquals(listOf(1, 2, 3), widths.columns.map { it.width })

        val combined = parseTable("[cols=\"2*^2\"]\n|===\n|a |b\n|===")
        assertEquals(2, combined.columns.size)
        assertTrue(combined.columns.all { it.alignment == TableColumnAlignment.CENTER && it.width == 2 })
    }

    @Test
    fun columnSpanSpecConsumesMultipleColumns() {
        val table = parseTable("|===\n|A |B\n2+|Wide\n|===")
        assertEquals(2, table.rows.size)
        val wide = table.rows.last().cells.single()
        assertEquals(2, wide.colSpan)
        assertEquals("Wide", plainText(wide.inlines))
    }

    @Test
    fun spanSpecInsideALineStartsANewCell() {
        val table = parseTable("[cols=\"1,1,1\"]\n|===\n|a 2+|wide\n|===")
        val row = table.rows.single()
        assertEquals(listOf("a", "wide"), row.cells.map { plainText(it.inlines) })
        assertEquals(listOf(1, 2), row.cells.map { it.colSpan })
    }

    @Test
    fun cellContentIsInlineParsed() {
        val table = parseTable("|===\n|*bold* |plain\n|===")
        val bold = table.rows.single().cells.first().inlines.filterIsInstance<InlineSpan>().single()
        assertEquals(SpanVariant.STRONG, bold.variant)
        assertEquals("bold", plainText(bold.inlines))
    }

    @Test
    fun escapedPipeStaysLiteralInsideCell() {
        val table = parseTable("|===\n|a \\| b |c\n|===")
        assertEquals(listOf(listOf("a | b", "c")), cellTexts(table))
    }

    @Test
    fun continuationLineJoinsPreviousCell() {
        val table = parseTable("[cols=\"1\"]\n|===\n|first line\nsecond line\n|===")
        val cell = table.rows.single().cells.single()
        val text = assertIs<InlineText>(cell.inlines.single())
        assertEquals("first line\nsecond line", text.value)
    }

    @Test
    fun unclosedTableExtendsToEndOfInput() {
        val table = parseTable("|===\n|a |b")
        assertEquals(listOf(listOf("a", "b")), cellTexts(table))
    }

    @Test
    fun tableDelimiterEndsAPrecedingParagraph() {
        val doc = parser.parseDocument("Intro text\n|===\n|a\n|===")
        assertEquals(2, doc.blocks.size)
        val paragraph = assertIs<LeafBlock>(doc.blocks.first())
        assertEquals(LeafBlockName.PARAGRAPH, paragraph.name)
        assertEquals("Intro text", plainText(paragraph.inlines))
        assertIs<TableBlock>(doc.blocks[1])
    }

    @Test
    fun tableLocationsSpanDelimiters() {
        val table = parseTable("|===\n|a\n|===")
        assertEquals(1, table.location?.start?.line)
        assertEquals(3, table.location?.end?.line)
        val cell = table.rows.single().cells.single()
        assertEquals(2, cell.location?.start?.line)
        assertEquals(2, cell.location?.start?.col)
    }
}
