package org.markup.poet.asciidoc.parser

import org.markup.poet.asciidoc.ast.AsciiDocList
import org.markup.poet.asciidoc.ast.CodeBlock
import org.markup.poet.asciidoc.ast.Comment
import org.markup.poet.asciidoc.ast.ListType
import org.markup.poet.asciidoc.ast.Paragraph
import org.markup.poet.asciidoc.ast.Section
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.random.Random

/**
 * Property-based tests for block element recognition.
 * **Feature: asciidoc-parser, Property 4: Block Element Recognition**
 * **Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6**
 */
class BlockElementRecognitionTest {

    private val blockParser = DefaultBlockParser()

    @Test
    fun `Property 4 - Block Element Recognition - Parser should correctly identify and create appropriate AST nodes for all block-level AsciiDoc syntax`() {
        // Run property-based test with multiple iterations
        repeat(100) {
            val testData = generateBlockElementTestData()
            
            when (testData) {
                is TestSectionData -> {
                    val section = blockParser.parseSection(testData.line, testData.lineNumber)
                    
                    // Verify correct node type
                    assertTrue(section is Section)
                    
                    // Verify level detection
                    assertEquals(testData.expectedLevel, section.level)
                    
                    // Verify title extraction
                    assertEquals(testData.expectedTitle, section.title)
                    
                    // Verify source location
                    assertEquals(testData.lineNumber, section.sourceLocation.line)
                }
                
                is TestParagraphData -> {
                    val paragraph = blockParser.parseParagraph(testData.lines, testData.lineNumber)
                    
                    // Verify correct node type
                    assertTrue(paragraph is Paragraph)
                    
                    // Verify content is not empty
                    assertTrue(paragraph.content.isNotEmpty())
                    
                    // Verify source location
                    assertEquals(testData.lineNumber, paragraph.sourceLocation.line)
                }
                
                is TestListData -> {
                    val list = blockParser.parseList(testData.lines, testData.lineNumber, testData.listType)
                    
                    // Verify correct node type
                    assertTrue(list is AsciiDocList)
                    
                    // Verify list type
                    assertEquals(testData.listType, list.type)
                    
                    // Verify items are created
                    assertTrue(list.items.isNotEmpty())
                    
                    // Verify each item has correct marker
                    list.items.forEach { item ->
                        assertNotEquals("", item.marker)
                        assertTrue(item.content.isNotEmpty())
                    }
                    
                    // Verify source location
                    assertEquals(testData.lineNumber, list.sourceLocation.line)
                }
                
                is TestCodeBlockData -> {
                    val codeBlock = blockParser.parseCodeBlock(testData.lines, testData.lineNumber, testData.language)
                    
                    // Verify correct node type
                    assertTrue(codeBlock is CodeBlock)
                    
                    // Verify language is preserved
                    assertEquals(testData.language, codeBlock.language)
                    
                    // Verify content is preserved
                    assertEquals(testData.lines.joinToString("\n"), codeBlock.content)
                    
                    // Verify source location
                    assertEquals(testData.lineNumber, codeBlock.sourceLocation.line)
                }
                
                is TestCommentData -> {
                    val comment = blockParser.parseComment(testData.line, testData.lineNumber)
                    
                    if (testData.shouldParse) {
                        // Verify correct node type
                        assertNotNull(comment)
                        assertTrue(comment is Comment)
                        
                        // Verify content extraction
                        assertEquals(testData.expectedContent, comment.content)
                        
                        // Verify source location
                        assertEquals(testData.lineNumber, comment.sourceLocation.line)
                    } else {
                        // Should return null for non-comment lines
                        assertNull(comment)
                    }
                }
            }
        }
    }

    @Test
    fun `Property 4a - Section header parsing should correctly detect levels and extract titles`() {
        repeat(100) {
            val sectionData = generateSectionHeaderTestData()
            val section = blockParser.parseSection(sectionData.line, sectionData.lineNumber)
            
            // Level should match the number of equals signs
            assertEquals(sectionData.expectedLevel, section.level)
            
            // Title should be extracted correctly
            assertEquals(sectionData.expectedTitle, section.title)
            
            // Should always create a Section node
            assertTrue(section is Section)
        }
    }

    @Test
    fun `Property 4b - List parsing should handle different list types and markers correctly`() {
        repeat(100) {
            val listData = generateListTestData()
            val list = blockParser.parseList(listData.lines, listData.lineNumber, listData.listType)
            
            // Should create correct list type
            assertEquals(listData.listType, list.type)
            
            // Should create items for non-empty lines
            val nonEmptyLines = listData.lines.filter { it.trim().isNotEmpty() }
            assertEquals(nonEmptyLines.size, list.items.size)
            
            // Each item should have appropriate marker
            list.items.forEach { item ->
                when (listData.listType) {
                    ListType.UNORDERED -> {
                        assertTrue(item.marker == "*" || item.marker == "-")
                    }
                    ListType.ORDERED -> {
                        assertNotEquals("", item.marker)
                        assertTrue(item.marker == "." || item.marker.matches(Regex("\\d+\\.")))
                    }
                    ListType.DEFINITION -> {
                        // Definition lists not fully implemented
                        assertNotNull(item.marker)
                    }
                }
            }
        }
    }

    @Test
    fun `Property 4c - Code block parsing should preserve content and language information`() {
        repeat(100) {
            val codeBlockData = generateCodeBlockTestData()
            val codeBlock = blockParser.parseCodeBlock(codeBlockData.lines, codeBlockData.lineNumber, codeBlockData.language)
            
            // Should preserve language
            assertEquals(codeBlockData.language, codeBlock.language)
            
            // Should preserve content exactly
            assertEquals(codeBlockData.lines.joinToString("\n"), codeBlock.content)
            
            // Should create CodeBlock node
            assertTrue(codeBlock is CodeBlock)
        }
    }

    // Test data generation functions
    private fun generateBlockElementTestData(): TestBlockElement {
        return when (Random.nextInt(5)) {
            0 -> generateSectionHeaderTestData()
            1 -> generateParagraphTestData()
            2 -> generateListTestData()
            3 -> generateCodeBlockTestData()
            else -> generateCommentTestData()
        }
    }

    private fun generateSectionHeaderTestData(): TestSectionData {
        val level = Random.nextInt(1, 7)
        val title = "Test Section ${Random.nextInt(1000)}"
        val equals = "=".repeat(level)
        val line = "$equals $title"
        val lineNumber = Random.nextInt(1, 1001)
        
        return TestSectionData(line, lineNumber, level, title)
    }

    private fun generateParagraphTestData(): TestParagraphData {
        val lines = (1..Random.nextInt(1, 6)).map { "Test paragraph line $it" }
        val lineNumber = Random.nextInt(1, 1001)
        
        return TestParagraphData(lines, lineNumber)
    }

    private fun generateListTestData(): TestListData {
        val listType = when (Random.nextInt(3)) {
            0 -> ListType.UNORDERED
            1 -> ListType.ORDERED
            else -> ListType.DEFINITION
        }
        
        val lines = when (listType) {
            ListType.UNORDERED -> {
                (1..Random.nextInt(1, 6)).map { 
                    val marker = if (Random.nextBoolean()) "*" else "-"
                    "$marker Test item $it"
                }
            }
            ListType.ORDERED -> {
                (1..Random.nextInt(1, 6)).map { 
                    if (Random.nextBoolean()) {
                        ". Test item $it"
                    } else {
                        "$it. Test item $it"
                    }
                }
            }
            ListType.DEFINITION -> {
                (1..Random.nextInt(1, 6)).map { "Test definition $it" }
            }
        }
        
        val lineNumber = Random.nextInt(1, 1001)
        
        return TestListData(lines, lineNumber, listType)
    }

    private fun generateCodeBlockTestData(): TestCodeBlockData {
        val lines = (0..Random.nextInt(0, 11)).map { "code line $it" }
        val language = if (Random.nextBoolean()) {
            listOf("kotlin", "java", "python", "javascript", null).random()
        } else {
            null
        }
        val lineNumber = Random.nextInt(1, 1001)
        
        return TestCodeBlockData(lines, lineNumber, language)
    }

    private fun generateCommentTestData(): TestCommentData {
        val shouldParse = Random.nextBoolean()
        
        return if (shouldParse) {
            val content = "Test comment ${Random.nextInt(1000)}"
            val line = if (content.isEmpty()) "//" else "// $content"
            val lineNumber = Random.nextInt(1, 1001)
            TestCommentData(line, lineNumber, true, content)
        } else {
            // Generate non-comment line
            val line = "Not a comment line ${Random.nextInt(1000)}"
            val lineNumber = Random.nextInt(1, 1001)
            TestCommentData(line, lineNumber, false, "")
        }
    }
}

// Test data classes
sealed class TestBlockElement
data class TestSectionData(val line: String, val lineNumber: Int, val expectedLevel: Int, val expectedTitle: String) : TestBlockElement()
data class TestParagraphData(val lines: List<String>, val lineNumber: Int) : TestBlockElement()
data class TestListData(val lines: List<String>, val lineNumber: Int, val listType: ListType) : TestBlockElement()
data class TestCodeBlockData(val lines: List<String>, val lineNumber: Int, val language: String?) : TestBlockElement()
data class TestCommentData(val line: String, val lineNumber: Int, val shouldParse: Boolean, val expectedContent: String) : TestBlockElement()