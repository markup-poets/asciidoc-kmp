package org.markup.poet.asciidoc.parser

import org.markup.poet.asciidoc.asg.InlineText
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.LeafBlockForm
import org.markup.poet.asciidoc.asg.LeafBlockName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CodeBlockParsingTest {
    private val parser = DefaultAsciidocParser()

    @Test
    fun `should parse code block with source attribute correctly`() {
        val adoc = """
[source,kotlin]
----
interface Converter<Output> {
    fun convert(document: Document): Output
}
----
        """.trimIndent()

        val result = parser.parse(adoc)
        val document = result.document

        assertTrue(result.errors.isEmpty(), "Should have no errors, but found: ${result.errors}")
        assertEquals(1, document.blocks.size, "Document should have one block")

        val listing = document.blocks[0] as? LeafBlock
        assertTrue(listing != null, "First block should be a LeafBlock, but was ${document.blocks[0]::class}")

        assertEquals(LeafBlockName.LISTING, listing.name)
        assertEquals(LeafBlockForm.DELIMITED, listing.form)
        assertEquals("----", listing.delimiter)
        assertEquals(listOf("source", "kotlin"), listing.metadata?.positional)

        val expectedContent = """
interface Converter<Output> {
    fun convert(document: Document): Output
}
        """.trimIndent()
        val text = listing.inlines.single() as? InlineText
        assertTrue(text != null, "Listing content should be a single text node")
        assertEquals(expectedContent, text.value)
    }
}
