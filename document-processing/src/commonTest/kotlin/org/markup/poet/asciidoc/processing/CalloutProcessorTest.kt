package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.ast.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CalloutProcessorTest {
    
    private val processor = DefaultCalloutProcessor()
    
    private fun createDocument(vararg blocks: BlockElement): Document {
        return Document(
            title = null,
            children = blocks.toList(),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(1, 1)
        )
    }
    
    private fun createCalloutListItem(number: Int, text: String, line: Int): CalloutListItem {
        return CalloutListItem(
            number = number,
            content = listOf(Text(text, emptyMap(), SourceLocation(line, 1))),
            attributes = emptyMap(),
            sourceLocation = SourceLocation(line, 1)
        )
    }
    
    @Test
    fun `should extract callout markers from code block`() {
        val codeBlock = CodeBlock(
            language = "kotlin",
            content = """
                fun example() {
                    println("Hello") <1>
                    println("World") <2>
                }
            """.trimIndent(),
            sourceLocation = SourceLocation(1, 1)
        )
        
        val document = createDocument(codeBlock)
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
        val codeBlock = CodeBlock(
            language = "kotlin",
            content = """
                println("Hello") <1>
                println("World") <2>
            """.trimIndent(),
            sourceLocation = SourceLocation(1, 1)
        )
        
        val calloutList = CalloutList(
            items = listOf(
                createCalloutListItem(1, "First explanation", 5),
                createCalloutListItem(2, "Second explanation", 6)
            ),
            sourceLocation = SourceLocation(5, 1)
        )
        
        val document = createDocument(codeBlock, calloutList)
        val result = processor.process(document)
        
        assertEquals(1, result.calloutsByBlock.size)
        val callouts = result.calloutsByBlock.values.first()
        assertEquals(2, callouts.size)
        assertEquals("First explanation", (callouts[0].explanation?.first() as? Text)?.content)
        assertEquals("Second explanation", (callouts[1].explanation?.first() as? Text)?.content)
        assertTrue(result.errors.isEmpty())
        assertTrue(result.warnings.isEmpty())
    }
    
    @Test
    fun `should warn when callout markers and list items don't match`() {
        val codeBlock = CodeBlock(
            language = "kotlin",
            content = """
                println("Hello") <1>
                println("World") <2>
                println("Test") <3>
            """.trimIndent(),
            sourceLocation = SourceLocation(1, 1)
        )
        
        val calloutList = CalloutList(
            items = listOf(
                createCalloutListItem(1, "First explanation", 6),
                createCalloutListItem(2, "Second explanation", 7)
            ),
            sourceLocation = SourceLocation(6, 1)
        )
        
        val document = createDocument(codeBlock, calloutList)
        val result = processor.process(document)
        
        assertEquals(1, result.warnings.size)
        assertEquals(ProcessingWarningType.CALLOUT_MISMATCH, result.warnings[0].warningType)
        assertTrue(result.warnings[0].message.contains("Missing explanations for: 3"))
    }
    
    @Test
    fun `should warn when extra list items exist`() {
        val codeBlock = CodeBlock(
            language = "kotlin",
            content = """
                println("Hello") <1>
            """.trimIndent(),
            sourceLocation = SourceLocation(1, 1)
        )
        
        val calloutList = CalloutList(
            items = listOf(
                createCalloutListItem(1, "First explanation", 4),
                createCalloutListItem(2, "Extra explanation", 5)
            ),
            sourceLocation = SourceLocation(4, 1)
        )
        
        val document = createDocument(codeBlock, calloutList)
        val result = processor.process(document)
        
        assertEquals(1, result.warnings.size)
        assertEquals(ProcessingWarningType.CALLOUT_MISMATCH, result.warnings[0].warningType)
        assertTrue(result.warnings[0].message.contains("Extra explanations for: 2"))
    }
    
    @Test
    fun `should error when callout list has no preceding code block`() {
        val calloutList = CalloutList(
            items = listOf(
                createCalloutListItem(1, "Orphaned explanation", 1)
            ),
            sourceLocation = SourceLocation(1, 1)
        )
        
        val document = createDocument(calloutList)
        val result = processor.process(document)
        
        assertEquals(1, result.errors.size)
        assertEquals(ProcessingErrorType.CALLOUT_INVALID_CONTEXT, result.errors[0].errorType)
        assertTrue(result.errors[0].message.contains("without a preceding code block"))
    }
    
    @Test
    fun `should maintain separate sequences for multiple code blocks`() {
        val codeBlock1 = CodeBlock(
            language = "kotlin",
            content = """
                println("First") <1>
                println("Block") <2>
            """.trimIndent(),
            sourceLocation = SourceLocation(1, 1)
        )
        
        val calloutList1 = CalloutList(
            items = listOf(
                createCalloutListItem(1, "First block, first", 5),
                createCalloutListItem(2, "First block, second", 6)
            ),
            sourceLocation = SourceLocation(5, 1)
        )
        
        val codeBlock2 = CodeBlock(
            language = "kotlin",
            content = """
                println("Second") <1>
                println("Block") <2>
            """.trimIndent(),
            sourceLocation = SourceLocation(8, 1)
        )
        
        val calloutList2 = CalloutList(
            items = listOf(
                createCalloutListItem(1, "Second block, first", 12),
                createCalloutListItem(2, "Second block, second", 13)
            ),
            sourceLocation = SourceLocation(12, 1)
        )
        
        val document = createDocument(codeBlock1, calloutList1, codeBlock2, calloutList2)
        val result = processor.process(document)
        
        assertEquals(2, result.calloutsByBlock.size)
        assertTrue(result.errors.isEmpty())
        assertTrue(result.warnings.isEmpty())
        
        // Verify each block has its own sequence
        val blocks = result.calloutsByBlock.values.toList()
        assertEquals(2, blocks[0].size)
        assertEquals(2, blocks[1].size)
        assertEquals("First block, first", (blocks[0][0].explanation?.first() as? Text)?.content)
        assertEquals("Second block, first", (blocks[1][0].explanation?.first() as? Text)?.content)
    }
    
    @Test
    fun `should handle code block without callouts`() {
        val codeBlock = CodeBlock(
            language = "kotlin",
            content = """
                println("Hello")
                println("World")
            """.trimIndent(),
            sourceLocation = SourceLocation(1, 1)
        )
        
        val document = createDocument(codeBlock)
        val result = processor.process(document)
        
        assertTrue(result.calloutsByBlock.isEmpty())
        assertTrue(result.errors.isEmpty())
        assertTrue(result.warnings.isEmpty())
    }
    
    @Test
    fun `should warn when code block has callouts but no list`() {
        val codeBlock = CodeBlock(
            language = "kotlin",
            content = """
                println("Hello") <1>
            """.trimIndent(),
            sourceLocation = SourceLocation(1, 1)
        )
        
        val document = createDocument(codeBlock)
        val result = processor.process(document)
        
        assertEquals(1, result.calloutsByBlock.size)
        assertEquals(1, result.warnings.size)
        assertEquals(ProcessingWarningType.CALLOUT_MISMATCH, result.warnings[0].warningType)
        assertTrue(result.warnings[0].message.contains("no callout list follows"))
    }
    
    @Test
    fun `should process callouts in nested sections`() {
        val codeBlock = CodeBlock(
            language = "kotlin",
            content = "println(\"Test\") <1>",
            sourceLocation = SourceLocation(3, 1)
        )
        
        val calloutList = CalloutList(
            items = listOf(
                createCalloutListItem(1, "Explanation", 5)
            ),
            sourceLocation = SourceLocation(5, 1)
        )
        
        val section = Section(
            level = 1,
            title = "Example",
            children = listOf(codeBlock, calloutList),
            sourceLocation = SourceLocation(1, 1)
        )
        
        val document = createDocument(section)
        val result = processor.process(document)
        
        assertEquals(1, result.calloutsByBlock.size)
        assertTrue(result.errors.isEmpty())
        assertTrue(result.warnings.isEmpty())
    }
    
    @Test
    fun `should handle multiple callouts on same line`() {
        val codeBlock = CodeBlock(
            language = "kotlin",
            content = "val x = 1 <1> val y = 2 <2>",
            sourceLocation = SourceLocation(1, 1)
        )
        
        val calloutList = CalloutList(
            items = listOf(
                createCalloutListItem(1, "First", 3),
                createCalloutListItem(2, "Second", 4)
            ),
            sourceLocation = SourceLocation(3, 1)
        )
        
        val document = createDocument(codeBlock, calloutList)
        val result = processor.process(document)
        
        assertEquals(1, result.calloutsByBlock.size)
        val callouts = result.calloutsByBlock.values.first()
        assertEquals(2, callouts.size)
        assertEquals(1, callouts[0].lineNumber)
        assertEquals(1, callouts[1].lineNumber)
    }
}
