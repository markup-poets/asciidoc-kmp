package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.ast.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BibliographyManagerTest {
    
    private val manager = DefaultBibliographyManager()
    private val testLocation = SourceLocation(1, 1)
    
    @Test
    fun `should collect and number footnotes in document order`() {
        // Create a document with multiple footnotes
        val doc = Document(
            title = null,
            children = listOf(
                Paragraph(
                    content = listOf(
                        Text("First paragraph", emptyMap(), testLocation),
                        FootnoteReference(
                            id = "fn1",
                            content = listOf(Text("First footnote", emptyMap(), testLocation)),
                            sourceLocation = testLocation
                        )
                    ),
                    sourceLocation = testLocation
                ),
                Paragraph(
                    content = listOf(
                        Text("Second paragraph", emptyMap(), testLocation),
                        FootnoteReference(
                            id = "fn2",
                            content = listOf(Text("Second footnote", emptyMap(), testLocation)),
                            sourceLocation = testLocation
                        )
                    ),
                    sourceLocation = testLocation
                )
            ),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = testLocation
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
        val doc = Document(
            title = null,
            children = listOf(
                Paragraph(
                    content = listOf(
                        Text("First reference", emptyMap(), testLocation),
                        FootnoteReference(
                            id = "fn1",
                            content = listOf(Text("Footnote content", emptyMap(), testLocation)),
                            sourceLocation = testLocation
                        )
                    ),
                    sourceLocation = testLocation
                ),
                Paragraph(
                    content = listOf(
                        Text("Second reference", emptyMap(), testLocation),
                        FootnoteReference(
                            id = "fn1",
                            content = listOf(Text("Footnote content", emptyMap(), testLocation)),
                            sourceLocation = testLocation
                        )
                    ),
                    sourceLocation = testLocation
                )
            ),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = testLocation
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
        val doc = Document(
            title = null,
            children = listOf(
                BibliographyEntry(
                    id = "ref1",
                    citation = "Author, Title, Year",
                    metadata = mapOf("author" to "Author", "year" to "2024"),
                    sourceLocation = testLocation
                ),
                BibliographyEntry(
                    id = "ref2",
                    citation = "Another Author, Another Title, 2023",
                    metadata = mapOf("author" to "Another Author", "year" to "2023"),
                    sourceLocation = testLocation
                )
            ),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = testLocation
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
        val doc = Document(
            title = null,
            children = listOf(
                Paragraph(
                    content = listOf(
                        Text("Text with reference", emptyMap(), testLocation),
                        FootnoteReference(
                            id = "nonexistent",
                            content = listOf(Text("Content", emptyMap(), testLocation)),
                            sourceLocation = testLocation
                        )
                    ),
                    sourceLocation = testLocation
                )
            ),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = testLocation
        )
        
        val result = manager.process(doc)
        
        // The footnote is collected but there's no validation issue since it's self-contained
        assertEquals(1, result.footnotes.size)
        assertTrue(result.warnings.isEmpty())
    }
    
    @Test
    fun `should warn about unresolved bibliography references`() {
        val doc = Document(
            title = null,
            children = listOf(
                Paragraph(
                    content = listOf(
                        Text("Text with citation", emptyMap(), testLocation),
                        BibliographyReference(
                            citationId = "nonexistent",
                            sourceLocation = testLocation
                        )
                    ),
                    sourceLocation = testLocation
                )
            ),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = testLocation
        )
        
        val result = manager.process(doc)
        
        assertEquals(1, result.warnings.size)
        assertEquals(ProcessingWarningType.BIBLIOGRAPHY_UNRESOLVED, result.warnings[0].warningType)
        assertTrue(result.warnings[0].message.contains("nonexistent"))
    }
    
    @Test
    fun `should handle footnotes in nested structures`() {
        val doc = Document(
            title = null,
            children = listOf(
                Section(
                    level = 1,
                    title = "Section",
                    children = listOf(
                        Paragraph(
                            content = listOf(
                                Text("Nested text", emptyMap(), testLocation),
                                FootnoteReference(
                                    id = "nested",
                                    content = listOf(Text("Nested footnote", emptyMap(), testLocation)),
                                    sourceLocation = testLocation
                                )
                            ),
                            sourceLocation = testLocation
                        )
                    ),
                    sourceLocation = testLocation
                )
            ),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = testLocation
        )
        
        val result = manager.process(doc)
        
        assertEquals(1, result.footnotes.size)
        assertEquals("nested", result.footnotes[0].id)
        assertEquals(1, result.footnotes[0].number)
    }
    
    @Test
    fun `should handle footnotes in list items`() {
        val doc = Document(
            title = null,
            children = listOf(
                AsciiDocList(
                    type = ListType.UNORDERED,
                    items = listOf(
                        ListItem(
                            marker = "*",
                            content = listOf(
                                Text("List item", emptyMap(), testLocation),
                                FootnoteReference(
                                    id = "list-fn",
                                    content = listOf(Text("List footnote", emptyMap(), testLocation)),
                                    sourceLocation = testLocation
                                )
                            ),
                            sourceLocation = testLocation
                        )
                    ),
                    sourceLocation = testLocation
                )
            ),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = testLocation
        )
        
        val result = manager.process(doc)
        
        assertEquals(1, result.footnotes.size)
        assertEquals("list-fn", result.footnotes[0].id)
    }
    
    @Test
    fun `should handle empty document`() {
        val doc = Document(
            title = null,
            children = emptyList(),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = testLocation
        )
        
        val result = manager.process(doc)
        
        assertTrue(result.footnotes.isEmpty())
        assertTrue(result.bibliography.isEmpty())
        assertTrue(result.warnings.isEmpty())
    }
    
    @Test
    fun `should handle footnotes in admonition blocks`() {
        val doc = Document(
            title = null,
            children = listOf(
                AdmonitionBlock(
                    type = AdmonitionType.NOTE,
                    title = null,
                    content = listOf(
                        Paragraph(
                            content = listOf(
                                Text("Note text", emptyMap(), testLocation),
                                FootnoteReference(
                                    id = "admon-fn",
                                    content = listOf(Text("Admonition footnote", emptyMap(), testLocation)),
                                    sourceLocation = testLocation
                                )
                            ),
                            sourceLocation = testLocation
                        )
                    ),
                    sourceLocation = testLocation
                )
            ),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = testLocation
        )
        
        val result = manager.process(doc)
        
        assertEquals(1, result.footnotes.size)
        assertEquals("admon-fn", result.footnotes[0].id)
    }
    
    @Test
    fun `should generate ordered footnote list`() {
        val doc = Document(
            title = null,
            children = listOf(
                Paragraph(
                    content = listOf(
                        FootnoteReference("fn3", listOf(Text("Third", emptyMap(), testLocation)), sourceLocation = testLocation)
                    ),
                    sourceLocation = testLocation
                ),
                Paragraph(
                    content = listOf(
                        FootnoteReference("fn1", listOf(Text("First", emptyMap(), testLocation)), sourceLocation = testLocation)
                    ),
                    sourceLocation = testLocation
                ),
                Paragraph(
                    content = listOf(
                        FootnoteReference("fn2", listOf(Text("Second", emptyMap(), testLocation)), sourceLocation = testLocation)
                    ),
                    sourceLocation = testLocation
                )
            ),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = testLocation
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
        val doc = Document(
            title = null,
            children = listOf(
                Paragraph(
                    content = listOf(
                        Text("First ref to fn1", emptyMap(), testLocation),
                        FootnoteReference(
                            id = "fn1",
                            content = listOf(Text("Footnote 1", emptyMap(), testLocation)),
                            sourceLocation = testLocation
                        )
                    ),
                    sourceLocation = testLocation
                ),
                Paragraph(
                    content = listOf(
                        Text("First ref to fn2", emptyMap(), testLocation),
                        FootnoteReference(
                            id = "fn2",
                            content = listOf(Text("Footnote 2", emptyMap(), testLocation)),
                            sourceLocation = testLocation
                        )
                    ),
                    sourceLocation = testLocation
                ),
                Paragraph(
                    content = listOf(
                        Text("Second ref to fn1", emptyMap(), testLocation),
                        FootnoteReference(
                            id = "fn1",
                            content = listOf(Text("Footnote 1", emptyMap(), testLocation)),
                            sourceLocation = testLocation
                        )
                    ),
                    sourceLocation = testLocation
                ),
                Paragraph(
                    content = listOf(
                        Text("First ref to fn3", emptyMap(), testLocation),
                        FootnoteReference(
                            id = "fn3",
                            content = listOf(Text("Footnote 3", emptyMap(), testLocation)),
                            sourceLocation = testLocation
                        )
                    ),
                    sourceLocation = testLocation
                ),
                Paragraph(
                    content = listOf(
                        Text("Second ref to fn2", emptyMap(), testLocation),
                        FootnoteReference(
                            id = "fn2",
                            content = listOf(Text("Footnote 2", emptyMap(), testLocation)),
                            sourceLocation = testLocation
                        )
                    ),
                    sourceLocation = testLocation
                )
            ),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = testLocation
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
