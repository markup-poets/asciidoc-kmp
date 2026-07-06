package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.Block
import org.markup.poet.asciidoc.asg.BlockMetadata
import org.markup.poet.asciidoc.asg.InlineText
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.LeafBlockForm
import org.markup.poet.asciidoc.asg.LeafBlockName
import org.markup.poet.asciidoc.asg.ListBlock
import org.markup.poet.asciidoc.asg.ListItem
import org.markup.poet.asciidoc.asg.ListVariant
import org.markup.poet.asciidoc.asg.Location
import org.markup.poet.asciidoc.asg.Position
import org.markup.poet.asciidoc.asg.SectionBlock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CalloutProcessorTest {

    private val processor = DefaultCalloutProcessor()

    private fun loc(line: Int) = Location(Position(line, 1), Position(line, 1))

    private fun createDocument(vararg blocks: Block): AsgDocument {
        return AsgDocument(blocks = blocks.toList(), location = loc(1))
    }

    private fun codeBlock(language: String, content: String, line: Int = 1) = LeafBlock(
        name = LeafBlockName.LISTING,
        form = LeafBlockForm.DELIMITED,
        delimiter = "----",
        inlines = listOf(InlineText(content, loc(line))),
        metadata = BlockMetadata(positional = listOf("source", language)),
        location = loc(line)
    )

    private fun calloutList(vararg items: ListItem, line: Int = 1) = ListBlock(
        variant = ListVariant.CALLOUT,
        marker = "<1>",
        items = items.toList(),
        location = loc(line)
    )

    private fun calloutListItem(number: Int, text: String, line: Int): ListItem {
        return ListItem(
            marker = "<$number>",
            principal = listOf(InlineText(text, loc(line))),
            location = loc(line)
        )
    }

    @Test
    fun `should extract callout markers from code block`() {
        val code = codeBlock(
            "kotlin",
            """
                fun example() {
                    println("Hello") <1>
                    println("World") <2>
                }
            """.trimIndent()
        )

        val document = createDocument(code)
        val result = processor.process(document)

        assertEquals(1, result.calloutsByBlock.size)
        val callouts = result.calloutsByBlock.values.first()
        assertEquals(2, callouts.size)
        assertEquals(1, callouts[0].number)
        assertEquals("<1>", callouts[0].marker)
        assertEquals(2, callouts[0].lineNumber)
        assertEquals(2, callouts[1].number)
        assertEquals("<2>", callouts[1].marker)
        assertEquals(3, callouts[1].lineNumber)
    }

    @Test
    fun `should associate callout list with code block`() {
        val code = codeBlock(
            "kotlin",
            """
                println("Hello") <1>
                println("World") <2>
            """.trimIndent()
        )

        val list = calloutList(
            calloutListItem(1, "First explanation", 5),
            calloutListItem(2, "Second explanation", 6),
            line = 5
        )

        val document = createDocument(code, list)
        val result = processor.process(document)

        assertEquals(1, result.calloutsByBlock.size)
        val callouts = result.calloutsByBlock.values.first()
        assertEquals(2, callouts.size)
        assertEquals("First explanation", (callouts[0].explanation?.first() as? InlineText)?.value)
        assertEquals("Second explanation", (callouts[1].explanation?.first() as? InlineText)?.value)
        assertTrue(result.errors.isEmpty())
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `should warn when callout markers and list items don't match`() {
        val code = codeBlock(
            "kotlin",
            """
                println("Hello") <1>
                println("World") <2>
                println("Test") <3>
            """.trimIndent()
        )

        val list = calloutList(
            calloutListItem(1, "First explanation", 6),
            calloutListItem(2, "Second explanation", 7),
            line = 6
        )

        val document = createDocument(code, list)
        val result = processor.process(document)

        assertEquals(1, result.warnings.size)
        assertEquals(ProcessingWarningType.CALLOUT_MISMATCH, result.warnings[0].warningType)
        assertTrue(result.warnings[0].message.contains("Missing explanations for: 3"))
    }

    @Test
    fun `should warn when extra list items exist`() {
        val code = codeBlock(
            "kotlin",
            """
                println("Hello") <1>
            """.trimIndent()
        )

        val list = calloutList(
            calloutListItem(1, "First explanation", 4),
            calloutListItem(2, "Extra explanation", 5),
            line = 4
        )

        val document = createDocument(code, list)
        val result = processor.process(document)

        assertEquals(1, result.warnings.size)
        assertEquals(ProcessingWarningType.CALLOUT_MISMATCH, result.warnings[0].warningType)
        assertTrue(result.warnings[0].message.contains("Extra explanations for: 2"))
    }

    @Test
    fun `should error when callout list has no preceding code block`() {
        val list = calloutList(
            calloutListItem(1, "Orphaned explanation", 1),
            line = 1
        )

        val document = createDocument(list)
        val result = processor.process(document)

        assertEquals(1, result.errors.size)
        assertEquals(ProcessingErrorType.CALLOUT_INVALID_CONTEXT, result.errors[0].errorType)
        assertTrue(result.errors[0].message.contains("without a preceding code block"))
    }

    @Test
    fun `should maintain separate sequences for multiple code blocks`() {
        val code1 = codeBlock(
            "kotlin",
            """
                println("First") <1>
                println("Block") <2>
            """.trimIndent(),
            line = 1
        )

        val list1 = calloutList(
            calloutListItem(1, "First block, first", 5),
            calloutListItem(2, "First block, second", 6),
            line = 5
        )

        val code2 = codeBlock(
            "kotlin",
            """
                println("Second") <1>
                println("Block") <2>
            """.trimIndent(),
            line = 8
        )

        val list2 = calloutList(
            calloutListItem(1, "Second block, first", 12),
            calloutListItem(2, "Second block, second", 13),
            line = 12
        )

        val document = createDocument(code1, list1, code2, list2)
        val result = processor.process(document)

        assertEquals(2, result.calloutsByBlock.size)
        assertTrue(result.errors.isEmpty())
        assertTrue(result.warnings.isEmpty())

        // Verify each block has its own sequence
        val blocks = result.calloutsByBlock.values.toList()
        assertEquals(2, blocks[0].size)
        assertEquals(2, blocks[1].size)
        assertEquals("First block, first", (blocks[0][0].explanation?.first() as? InlineText)?.value)
        assertEquals("Second block, first", (blocks[1][0].explanation?.first() as? InlineText)?.value)
    }

    @Test
    fun `should handle code block without callouts`() {
        val code = codeBlock(
            "kotlin",
            """
                println("Hello")
                println("World")
            """.trimIndent()
        )

        val document = createDocument(code)
        val result = processor.process(document)

        assertTrue(result.calloutsByBlock.isEmpty())
        assertTrue(result.errors.isEmpty())
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `should warn when code block has callouts but no list`() {
        val code = codeBlock(
            "kotlin",
            """
                println("Hello") <1>
            """.trimIndent()
        )

        val document = createDocument(code)
        val result = processor.process(document)

        assertEquals(1, result.calloutsByBlock.size)
        assertEquals(1, result.warnings.size)
        assertEquals(ProcessingWarningType.CALLOUT_MISMATCH, result.warnings[0].warningType)
        assertTrue(result.warnings[0].message.contains("no callout list follows"))
    }

    @Test
    fun `should process callouts in nested sections`() {
        val code = codeBlock("kotlin", "println(\"Test\") <1>", line = 3)

        val list = calloutList(
            calloutListItem(1, "Explanation", 5),
            line = 5
        )

        val section = SectionBlock(
            title = listOf(InlineText("Example", loc(1))),
            level = 1,
            blocks = listOf(code, list),
            location = loc(1)
        )

        val document = createDocument(section)
        val result = processor.process(document)

        assertEquals(1, result.calloutsByBlock.size)
        assertTrue(result.errors.isEmpty())
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `should handle multiple callouts on same line`() {
        val code = codeBlock("kotlin", "val x = 1 <1> val y = 2 <2>")

        val list = calloutList(
            calloutListItem(1, "First", 3),
            calloutListItem(2, "Second", 4),
            line = 3
        )

        val document = createDocument(code, list)
        val result = processor.process(document)

        assertEquals(1, result.calloutsByBlock.size)
        val callouts = result.calloutsByBlock.values.first()
        assertEquals(2, callouts.size)
        assertEquals(1, callouts[0].lineNumber)
        assertEquals(1, callouts[1].lineNumber)
    }
}
