package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.Block
import org.markup.poet.asciidoc.asg.BlockMetadata
import org.markup.poet.asciidoc.asg.Inline
import org.markup.poet.asciidoc.asg.InlineSpan
import org.markup.poet.asciidoc.asg.InlineText
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.LeafBlockForm
import org.markup.poet.asciidoc.asg.LeafBlockName
import org.markup.poet.asciidoc.asg.Location
import org.markup.poet.asciidoc.asg.ParentBlock
import org.markup.poet.asciidoc.asg.ParentBlockName
import org.markup.poet.asciidoc.asg.Position
import org.markup.poet.asciidoc.asg.SectionBlock
import org.markup.poet.asciidoc.asg.SpanForm
import org.markup.poet.asciidoc.asg.SpanVariant
import org.markup.poet.asciidoc.asg.plainText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdmonitionProcessorTest {

    private val processor = DefaultAdmonitionProcessor()
    private val testLocation = Location(Position(1, 1), Position(1, 1))

    private fun paragraphOf(inlines: List<Inline>, metadata: BlockMetadata? = null) = LeafBlock(
        name = LeafBlockName.PARAGRAPH,
        form = LeafBlockForm.PARAGRAPH,
        inlines = inlines,
        metadata = metadata,
        location = testLocation
    )

    private fun textParagraph(text: String, metadata: BlockMetadata? = null) =
        paragraphOf(listOf(InlineText(text, testLocation)), metadata)

    private fun document(vararg blocks: Block) = AsgDocument(
        blocks = blocks.toList(),
        location = testLocation
    )

    @Test
    fun `should recognize NOTE admonition with inline syntax`() {
        val document = document(textParagraph("NOTE: This is a note"))

        val result = processor.process(document)

        assertEquals(1, result.document.blocks.size)
        val admonition = result.document.blocks.first() as ParentBlock
        assertEquals(ParentBlockName.ADMONITION, admonition.name)
        assertEquals("note", admonition.variant)
        assertEquals(1, result.admonitionCount["note"])
    }

    @Test
    fun `should recognize TIP admonition with inline syntax`() {
        val document = document(textParagraph("TIP: This is a tip"))

        val result = processor.process(document)

        val admonition = result.document.blocks.first() as ParentBlock
        assertEquals("tip", admonition.variant)
    }

    @Test
    fun `should recognize WARNING admonition with inline syntax`() {
        val document = document(textParagraph("WARNING: This is a warning"))

        val result = processor.process(document)

        val admonition = result.document.blocks.first() as ParentBlock
        assertEquals("warning", admonition.variant)
    }

    @Test
    fun `should recognize CAUTION admonition with inline syntax`() {
        val document = document(textParagraph("CAUTION: This is a caution"))

        val result = processor.process(document)

        val admonition = result.document.blocks.first() as ParentBlock
        assertEquals("caution", admonition.variant)
    }

    @Test
    fun `should recognize IMPORTANT admonition with inline syntax`() {
        val document = document(textParagraph("IMPORTANT: This is important"))

        val result = processor.process(document)

        val admonition = result.document.blocks.first() as ParentBlock
        assertEquals("important", admonition.variant)
    }

    @Test
    fun `should recognize admonition with block syntax using style attribute`() {
        val document = document(
            textParagraph("This is the content", metadata = BlockMetadata(positional = listOf("NOTE")))
        )

        val result = processor.process(document)

        val admonition = result.document.blocks.first() as ParentBlock
        assertEquals("note", admonition.variant)
        assertEquals(1, admonition.blocks.size)
    }

    @Test
    fun `should handle custom title in admonition`() {
        val document = document(
            textParagraph(
                "Content here",
                metadata = BlockMetadata(
                    positional = listOf("TIP"),
                    title = listOf(InlineText("Custom Title", testLocation))
                )
            )
        )

        val result = processor.process(document)

        val admonition = result.document.blocks.first() as ParentBlock
        assertEquals("tip", admonition.variant)
        assertEquals("Custom Title", admonition.metadata?.title?.let { plainText(it) })
    }

    @Test
    fun `should report warning for invalid admonition type`() {
        val document = document(
            textParagraph("Content", metadata = BlockMetadata(positional = listOf("INVALID")))
        )

        val result = processor.process(document)

        assertEquals(1, result.warnings.size)
        assertEquals(ProcessingWarningType.ADMONITION_INVALID_TYPE, result.warnings.first().warningType)
        assertTrue(result.warnings.first().message.contains("INVALID"))
    }

    @Test
    fun `should count admonitions by type`() {
        val doc = document(
            textParagraph("NOTE: First note"),
            textParagraph("NOTE: Second note"),
            textParagraph("TIP: A tip")
        )

        val result = processor.process(doc)

        assertEquals(2, result.admonitionCount["note"])
        assertEquals(1, result.admonitionCount["tip"])
    }

    @Test
    fun `should process nested admonitions in sections`() {
        val section = SectionBlock(
            title = listOf(InlineText("Test Section", testLocation)),
            level = 1,
            blocks = listOf(textParagraph("WARNING: Nested warning")),
            location = testLocation
        )
        val document = document(section)

        val result = processor.process(document)

        val processedSection = result.document.blocks.first() as SectionBlock
        val admonition = processedSection.blocks.first() as ParentBlock
        assertEquals("warning", admonition.variant)
        assertEquals(1, result.admonitionCount["warning"])
    }

    @Test
    fun `should preserve content after colon in inline syntax`() {
        val paragraph = paragraphOf(
            listOf(
                InlineText("NOTE: This is ", testLocation),
                InlineSpan(
                    variant = SpanVariant.STRONG,
                    form = SpanForm.CONSTRAINED,
                    inlines = listOf(InlineText("important", testLocation)),
                    location = testLocation
                ),
                InlineText(" content", testLocation)
            )
        )
        val document = document(paragraph)

        val result = processor.process(document)

        val admonition = result.document.blocks.first() as ParentBlock
        assertEquals("note", admonition.variant)
        assertEquals(1, admonition.blocks.size)
        val contentParagraph = admonition.blocks.first() as LeafBlock
        assertTrue(contentParagraph.inlines.size >= 2)
    }

    @Test
    fun `should not process regular paragraphs as admonitions`() {
        val document = document(textParagraph("This is just regular text"))

        val result = processor.process(document)

        assertEquals(1, result.document.blocks.size)
        assertTrue(result.document.blocks.first() is LeafBlock)
        assertTrue(result.admonitionCount.isEmpty())
    }

    @Test
    fun `should handle case-insensitive admonition type recognition`() {
        val document = document(textParagraph("note: lowercase note"))

        val result = processor.process(document)

        val admonition = result.document.blocks.first() as ParentBlock
        assertEquals("note", admonition.variant)
    }

    @Test
    fun `should process already existing admonition blocks`() {
        val existingAdmonition = ParentBlock(
            name = ParentBlockName.ADMONITION,
            variant = "tip",
            blocks = listOf(textParagraph("Content")),
            metadata = BlockMetadata(title = listOf(InlineText("Existing", testLocation))),
            location = testLocation
        )
        val document = document(existingAdmonition)

        val result = processor.process(document)

        assertEquals(1, result.admonitionCount["tip"])
        assertTrue(result.document.blocks.first() is ParentBlock)
    }
}
