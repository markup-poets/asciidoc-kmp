package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.Block
import org.markup.poet.asciidoc.asg.ConditionalBlock
import org.markup.poet.asciidoc.asg.ConditionalVariant
import org.markup.poet.asciidoc.asg.InlineText
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.LeafBlockForm
import org.markup.poet.asciidoc.asg.LeafBlockName
import org.markup.poet.asciidoc.asg.Location
import org.markup.poet.asciidoc.asg.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConditionalProcessorTest {

    private val processor = DefaultConditionalProcessor()
    private val location = Location(Position(1, 1), Position(1, 1))

    private fun paragraph(text: String) = LeafBlock(
        name = LeafBlockName.PARAGRAPH,
        form = LeafBlockForm.PARAGRAPH,
        inlines = listOf(InlineText(text, location)),
        location = location
    )

    private fun createDocument(blocks: List<Block>): AsgDocument {
        return AsgDocument(blocks = blocks, location = location)
    }

    @Test
    fun `should include content when ifdef attribute is defined`() {
        val config = ConditionalConfig(definedAttributes = setOf("myattr"))
        val directive = ConditionalBlock(
            variant = ConditionalVariant.IFDEF,
            condition = "myattr",
            blocks = listOf(paragraph("Included content")),
            location = location
        )
        val document = createDocument(listOf(directive))

        val result = processor.process(document, config)

        assertEquals(1, result.evaluatedConditionals)
        assertEquals(1, result.document.blocks.size)
        assertTrue(result.document.blocks[0] is LeafBlock)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `should exclude content when ifdef attribute is not defined`() {
        val config = ConditionalConfig(definedAttributes = emptySet())
        val directive = ConditionalBlock(
            variant = ConditionalVariant.IFDEF,
            condition = "myattr",
            blocks = listOf(paragraph("Excluded content")),
            location = location
        )
        val document = createDocument(listOf(directive))

        val result = processor.process(document, config)

        assertEquals(1, result.evaluatedConditionals)
        assertEquals(0, result.document.blocks.size)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `should include content when ifndef attribute is not defined`() {
        val config = ConditionalConfig(definedAttributes = emptySet())
        val directive = ConditionalBlock(
            variant = ConditionalVariant.IFNDEF,
            condition = "myattr",
            blocks = listOf(paragraph("Included content")),
            location = location
        )
        val document = createDocument(listOf(directive))

        val result = processor.process(document, config)

        assertEquals(1, result.evaluatedConditionals)
        assertEquals(1, result.document.blocks.size)
        assertTrue(result.document.blocks[0] is LeafBlock)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `should exclude content when ifndef attribute is defined`() {
        val config = ConditionalConfig(definedAttributes = setOf("myattr"))
        val directive = ConditionalBlock(
            variant = ConditionalVariant.IFNDEF,
            condition = "myattr",
            blocks = listOf(paragraph("Excluded content")),
            location = location
        )
        val document = createDocument(listOf(directive))

        val result = processor.process(document, config)

        assertEquals(1, result.evaluatedConditionals)
        assertEquals(0, result.document.blocks.size)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `should evaluate ifeval with equality operator`() {
        val config = ConditionalConfig(definedAttributes = setOf("version"))
        val directive = ConditionalBlock(
            variant = ConditionalVariant.IFEVAL,
            condition = """{version} == "version"""",
            blocks = listOf(paragraph("Version matches")),
            location = location
        )
        val document = createDocument(listOf(directive))

        val result = processor.process(document, config)

        assertEquals(1, result.evaluatedConditionals)
        assertEquals(1, result.document.blocks.size)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `should handle nested conditionals`() {
        val config = ConditionalConfig(definedAttributes = setOf("outer", "inner"))
        val innerDirective = ConditionalBlock(
            variant = ConditionalVariant.IFDEF,
            condition = "inner",
            blocks = listOf(paragraph("Inner content")),
            location = location
        )
        val outerDirective = ConditionalBlock(
            variant = ConditionalVariant.IFDEF,
            condition = "outer",
            blocks = listOf(innerDirective),
            location = location
        )
        val document = createDocument(listOf(outerDirective))

        val result = processor.process(document, config)

        assertEquals(2, result.evaluatedConditionals)
        assertEquals(1, result.document.blocks.size)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `should report error when max nesting depth exceeded`() {
        val config = ConditionalConfig(
            definedAttributes = setOf("attr"),
            maxNestingDepth = 2
        )

        // Create deeply nested conditionals
        var innermost: Block = paragraph("Deep content")

        for (i in 0 until 5) {
            innermost = ConditionalBlock(
                variant = ConditionalVariant.IFDEF,
                condition = "attr",
                blocks = listOf(innermost),
                location = location
            )
        }

        val document = createDocument(listOf(innermost))

        val result = processor.process(document, config)

        assertTrue(result.errors.isNotEmpty())
        assertTrue(result.errors.any { it.errorType == ProcessingErrorType.CONDITIONAL_MAX_DEPTH_EXCEEDED })
    }

    @Test
    fun `should support OR operator with comma-separated attributes`() {
        val config = ConditionalConfig(definedAttributes = setOf("attr2"))
        val directive = ConditionalBlock(
            variant = ConditionalVariant.IFDEF,
            condition = "attr1,attr2,attr3",
            blocks = listOf(paragraph("At least one defined")),
            location = location
        )
        val document = createDocument(listOf(directive))

        val result = processor.process(document, config)

        assertEquals(1, result.evaluatedConditionals)
        assertEquals(1, result.document.blocks.size)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `should support AND operator with plus-separated attributes`() {
        val config = ConditionalConfig(definedAttributes = setOf("attr1", "attr2"))
        val directive = ConditionalBlock(
            variant = ConditionalVariant.IFDEF,
            condition = "attr1+attr2",
            blocks = listOf(paragraph("All defined")),
            location = location
        )
        val document = createDocument(listOf(directive))

        val result = processor.process(document, config)

        assertEquals(1, result.evaluatedConditionals)
        assertEquals(1, result.document.blocks.size)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `should exclude when AND condition not fully satisfied`() {
        val config = ConditionalConfig(definedAttributes = setOf("attr1"))
        val directive = ConditionalBlock(
            variant = ConditionalVariant.IFDEF,
            condition = "attr1+attr2",
            blocks = listOf(paragraph("Should be excluded")),
            location = location
        )
        val document = createDocument(listOf(directive))

        val result = processor.process(document, config)

        assertEquals(1, result.evaluatedConditionals)
        assertEquals(0, result.document.blocks.size)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `should report error for invalid ifeval expression`() {
        val config = ConditionalConfig(definedAttributes = emptySet())
        val directive = ConditionalBlock(
            variant = ConditionalVariant.IFEVAL,
            condition = "invalid expression",
            blocks = emptyList(),
            location = location
        )
        val document = createDocument(listOf(directive))

        val result = processor.process(document, config)

        assertTrue(result.errors.isNotEmpty())
        assertTrue(result.errors.any { it.errorType == ProcessingErrorType.CONDITIONAL_INVALID_EXPRESSION })
    }

    @Test
    fun `should use else content when condition is false`() {
        val config = ConditionalConfig(definedAttributes = emptySet())
        val directive = ConditionalBlock(
            variant = ConditionalVariant.IFDEF,
            condition = "undefined",
            blocks = listOf(paragraph("If content")),
            elseBlocks = listOf(paragraph("Else content")),
            location = location
        )
        val document = createDocument(listOf(directive))

        val result = processor.process(document, config)

        assertEquals(1, result.evaluatedConditionals)
        assertEquals(1, result.document.blocks.size)
        val paragraph = result.document.blocks[0] as LeafBlock
        val text = paragraph.inlines[0] as InlineText
        assertEquals("Else content", text.value)
    }
}
