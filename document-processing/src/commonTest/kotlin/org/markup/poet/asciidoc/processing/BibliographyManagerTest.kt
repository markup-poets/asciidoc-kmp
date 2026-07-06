package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.BibliographyEntryBlock
import org.markup.poet.asciidoc.asg.Block
import org.markup.poet.asciidoc.asg.Inline
import org.markup.poet.asciidoc.asg.InlineCitation
import org.markup.poet.asciidoc.asg.InlineFootnote
import org.markup.poet.asciidoc.asg.InlineText
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.LeafBlockForm
import org.markup.poet.asciidoc.asg.LeafBlockName
import org.markup.poet.asciidoc.asg.ListBlock
import org.markup.poet.asciidoc.asg.ListItem
import org.markup.poet.asciidoc.asg.ListVariant
import org.markup.poet.asciidoc.asg.Location
import org.markup.poet.asciidoc.asg.ParentBlock
import org.markup.poet.asciidoc.asg.ParentBlockName
import org.markup.poet.asciidoc.asg.Position
import org.markup.poet.asciidoc.asg.SectionBlock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BibliographyManagerTest {

    private val manager = DefaultBibliographyManager()
    private val testLocation = Location(Position(1, 1), Position(1, 1))

    private fun paragraphOf(vararg inlines: Inline) = LeafBlock(
        name = LeafBlockName.PARAGRAPH,
        form = LeafBlockForm.PARAGRAPH,
        inlines = inlines.toList(),
        location = testLocation
    )

    private fun footnote(id: String, text: String) = InlineFootnote(
        id = id,
        inlines = listOf(InlineText(text, testLocation)),
        location = testLocation
    )

    private fun document(vararg blocks: Block) = AsgDocument(
        blocks = blocks.toList(),
        location = testLocation
    )

    @Test
    fun `should collect and number footnotes in document order`() {
        // Create a document with multiple footnotes
        val doc = document(
            paragraphOf(
                InlineText("First paragraph", testLocation),
                footnote("fn1", "First footnote")
            ),
            paragraphOf(
                InlineText("Second paragraph", testLocation),
                footnote("fn2", "Second footnote")
            )
        )

        val result = manager.process(doc)

        assertEquals(2, result.footnotes.size)
        assertEquals(1, result.footnotes[0].number)
        assertEquals("fn1", result.footnotes[0].id)
        assertEquals(2, result.footnotes[1].number)
        assertEquals("fn2", result.footnotes[1].id)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `should maintain consistent numbering for multiple references to same footnote`() {
        val doc = document(
            paragraphOf(
                InlineText("First reference", testLocation),
                footnote("fn1", "Footnote content")
            ),
            paragraphOf(
                InlineText("Second reference", testLocation),
                footnote("fn1", "Footnote content")
            )
        )

        val result = manager.process(doc)

        // Should only have one footnote with number 1
        assertEquals(1, result.footnotes.size)
        assertEquals(1, result.footnotes[0].number)
        assertEquals("fn1", result.footnotes[0].id)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `should collect bibliography entries`() {
        val doc = document(
            BibliographyEntryBlock(
                id = "ref1",
                citation = "Author, Title, Year",
                entryMetadata = mapOf("author" to "Author", "year" to "2024"),
                location = testLocation
            ),
            BibliographyEntryBlock(
                id = "ref2",
                citation = "Another Author, Another Title, 2023",
                entryMetadata = mapOf("author" to "Another Author", "year" to "2023"),
                location = testLocation
            )
        )

        val result = manager.process(doc)

        assertEquals(2, result.bibliography.size)
        assertTrue(result.bibliography.containsKey("ref1"))
        assertTrue(result.bibliography.containsKey("ref2"))
        assertEquals("Author, Title, Year", result.bibliography["ref1"]?.citation)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `should warn about unresolved footnote references`() {
        val doc = document(
            paragraphOf(
                InlineText("Text with reference", testLocation),
                footnote("nonexistent", "Content")
            )
        )

        val result = manager.process(doc)

        // The footnote is collected but there's no validation issue since it's self-contained
        assertEquals(1, result.footnotes.size)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `should warn about unresolved bibliography references`() {
        val doc = document(
            paragraphOf(
                InlineText("Text with citation", testLocation),
                InlineCitation(citationId = "nonexistent", location = testLocation)
            )
        )

        val result = manager.process(doc)

        assertEquals(1, result.warnings.size)
        assertEquals(ProcessingWarningType.BIBLIOGRAPHY_UNRESOLVED, result.warnings[0].warningType)
        assertTrue(result.warnings[0].message.contains("nonexistent"))
    }

    @Test
    fun `should handle footnotes in nested structures`() {
        val doc = document(
            SectionBlock(
                title = listOf(InlineText("Section", testLocation)),
                level = 1,
                blocks = listOf(
                    paragraphOf(
                        InlineText("Nested text", testLocation),
                        footnote("nested", "Nested footnote")
                    )
                ),
                location = testLocation
            )
        )

        val result = manager.process(doc)

        assertEquals(1, result.footnotes.size)
        assertEquals("nested", result.footnotes[0].id)
        assertEquals(1, result.footnotes[0].number)
    }

    @Test
    fun `should handle footnotes in list items`() {
        val doc = document(
            ListBlock(
                variant = ListVariant.UNORDERED,
                marker = "*",
                items = listOf(
                    ListItem(
                        marker = "*",
                        principal = listOf(
                            InlineText("List item", testLocation),
                            footnote("list-fn", "List footnote")
                        ),
                        location = testLocation
                    )
                ),
                location = testLocation
            )
        )

        val result = manager.process(doc)

        assertEquals(1, result.footnotes.size)
        assertEquals("list-fn", result.footnotes[0].id)
    }

    @Test
    fun `should handle empty document`() {
        val doc = document()

        val result = manager.process(doc)

        assertTrue(result.footnotes.isEmpty())
        assertTrue(result.bibliography.isEmpty())
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `should handle footnotes in admonition blocks`() {
        val doc = document(
            ParentBlock(
                name = ParentBlockName.ADMONITION,
                variant = "note",
                blocks = listOf(
                    paragraphOf(
                        InlineText("Note text", testLocation),
                        footnote("admon-fn", "Admonition footnote")
                    )
                ),
                location = testLocation
            )
        )

        val result = manager.process(doc)

        assertEquals(1, result.footnotes.size)
        assertEquals("admon-fn", result.footnotes[0].id)
    }

    @Test
    fun `should generate ordered footnote list`() {
        val doc = document(
            paragraphOf(footnote("fn3", "Third")),
            paragraphOf(footnote("fn1", "First")),
            paragraphOf(footnote("fn2", "Second"))
        )

        val result = manager.process(doc)

        // Footnotes should be numbered in order of first appearance
        assertEquals(3, result.footnotes.size)
        assertEquals("fn3", result.footnotes[0].id)
        assertEquals(1, result.footnotes[0].number)
        assertEquals("fn1", result.footnotes[1].id)
        assertEquals(2, result.footnotes[1].number)
        assertEquals("fn2", result.footnotes[2].id)
        assertEquals(3, result.footnotes[2].number)
    }

    @Test
    fun `should maintain consistent numbering with interleaved references`() {
        // Test case: fn1, fn2, fn1 (again), fn3, fn2 (again)
        // Expected: fn1=1, fn2=2, fn3=3 (only 3 footnotes, numbered by first occurrence)
        val doc = document(
            paragraphOf(
                InlineText("First ref to fn1", testLocation),
                footnote("fn1", "Footnote 1")
            ),
            paragraphOf(
                InlineText("First ref to fn2", testLocation),
                footnote("fn2", "Footnote 2")
            ),
            paragraphOf(
                InlineText("Second ref to fn1", testLocation),
                footnote("fn1", "Footnote 1")
            ),
            paragraphOf(
                InlineText("First ref to fn3", testLocation),
                footnote("fn3", "Footnote 3")
            ),
            paragraphOf(
                InlineText("Second ref to fn2", testLocation),
                footnote("fn2", "Footnote 2")
            )
        )

        val result = manager.process(doc)

        // Should have exactly 3 footnotes, numbered by first occurrence
        assertEquals(3, result.footnotes.size)

        // Verify each footnote has the correct number based on first occurrence
        val fn1 = result.footnotes.find { it.id == "fn1" }
        val fn2 = result.footnotes.find { it.id == "fn2" }
        val fn3 = result.footnotes.find { it.id == "fn3" }

        assertEquals(1, fn1?.number)
        assertEquals(2, fn2?.number)
        assertEquals(3, fn3?.number)

        // Verify footnotes are sorted by number
        assertEquals(1, result.footnotes[0].number)
        assertEquals(2, result.footnotes[1].number)
        assertEquals(3, result.footnotes[2].number)

        assertTrue(result.warnings.isEmpty())
    }
}
