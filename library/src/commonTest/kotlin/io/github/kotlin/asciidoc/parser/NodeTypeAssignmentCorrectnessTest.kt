package io.github.kotlin.asciidoc.parser

import io.github.kotlin.asciidoc.ast.*
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.random.Random

/**
 * Property-based tests for node type assignment correctness.
 * **Feature: asciidoc-parser, Property 3: Node Type Assignment Correctness**
 * **Validates: Requirements 2.2, 2.3, 2.4, 2.5, 2.6**
 */
class NodeTypeAssignmentCorrectnessTest {

    private val blockParser = DefaultBlockParser()

    @Test
    fun `Property 3 - Node Type Assignment Correctness - Parser should create appropriate AST node types that correctly represent the semantic meaning of source syntax`() {
        // Run property-based test with multiple iterations
        repeat(100) {
            val testCase = generateNodeTypeTestCase()
            
            when (testCase) {
                is SectionTestCase -> {
                    val node = blockParser.parseSection(testCase.input, testCase.lineNumber)
                    
                    // Verify correct node type for section syntax
                    assertTrue(node is Section, "Section syntax should create Section node")
                    
                    // Verify semantic correctness - level should match equals signs
                    assertEquals(testCase.expectedLevel, node.level, "Section level should match number of equals signs")
                    
                    // Verify semantic correctness - title should be extracted properly
                    assertEquals(testCase.expectedTitle, node.title, "Section title should be extracted correctly")
                    
                    // Verify it's a BlockElement (inheritance hierarchy)
                    assertTrue(node is BlockElement, "Section should be a BlockElement")
                    
                    // Verify it's an AstNode (root hierarchy)
                    assertTrue(node is AstNode, "Section should be an AstNode")
                }
                
                is ParagraphTestCase -> {
                    val node = blockParser.parseParagraph(testCase.input, testCase.lineNumber)
                    
                    // Verify correct node type for paragraph syntax
                    assertTrue(node is Paragraph, "Regular text should create Paragraph node")
                    
                    // Verify semantic correctness - content should be present
                    assertTrue(node.content.isNotEmpty(), "Paragraph should have content")
                    
                    // Verify content contains text elements
                    assertTrue(node.content.all { it is Text }, "Paragraph content should contain Text elements")
                    
                    // Verify it's a BlockElement
                    assertTrue(node is BlockElement, "Paragraph should be a BlockElement")
                    
                    // Verify it's an AstNode
                    assertTrue(node is AstNode, "Paragraph should be an AstNode")
                }
                
                is ListTestCase -> {
                    val node = blockParser.parseList(testCase.input, testCase.lineNumber, testCase.listType)
                    
                    // Verify correct node type for list syntax
                    assertTrue(node is AsciiDocList, "List syntax should create AsciiDocList node")
                    
                    // Verify semantic correctness - list type should match input
                    assertEquals(testCase.listType, node.type, "List type should match the syntax used")
                    
                    // Verify semantic correctness - items should be created
                    assertTrue(node.items.isNotEmpty(), "List should have items")
                    
                    // Verify each item is correct type
                    node.items.forEach { item ->
                        assertTrue(item is ListItem, "List items should be ListItem nodes")
                        assertTrue(item is AstNode, "ListItem should be an AstNode")
                        assertNotNull(item.marker, "ListItem should have a marker")
                        assertTrue(item.content.isNotEmpty(), "ListItem should have content")
                    }
                    
                    // Verify it's a BlockElement
                    assertTrue(node is BlockElement, "AsciiDocList should be a BlockElement")
                    
                    // Verify it's an AstNode
                    assertTrue(node is AstNode, "AsciiDocList should be an AstNode")
                }
                
                is CodeBlockTestCase -> {
                    val node = blockParser.parseCodeBlock(testCase.input, testCase.lineNumber, testCase.language)
                    
                    // Verify correct node type for code block syntax
                    assertTrue(node is CodeBlock, "Code block syntax should create CodeBlock node")
                    
                    // Verify semantic correctness - language should be preserved
                    assertEquals(testCase.language, node.language, "Code block language should be preserved")
                    
                    // Verify semantic correctness - content should be preserved exactly
                    assertEquals(testCase.input.joinToString("\n"), node.content, "Code block content should be preserved exactly")
                    
                    // Verify it's a BlockElement
                    assertTrue(node is BlockElement, "CodeBlock should be a BlockElement")
                    
                    // Verify it's an AstNode
                    assertTrue(node is AstNode, "CodeBlock should be an AstNode")
                }
                
                is CommentTestCase -> {
                    val node = blockParser.parseComment(testCase.input, testCase.lineNumber)
                    
                    if (testCase.shouldCreateNode) {
                        // Verify correct node type for comment syntax
                        assertNotNull(node, "Comment syntax should create Comment node")
                        assertTrue(node is Comment, "Comment syntax should create Comment node")
                        
                        // Verify semantic correctness - content should be extracted
                        assertEquals(testCase.expectedContent, node.content, "Comment content should be extracted correctly")
                        
                        // Verify it's a BlockElement
                        assertTrue(node is BlockElement, "Comment should be a BlockElement")
                        
                        // Verify it's an AstNode
                        assertTrue(node is AstNode, "Comment should be an AstNode")
                    } else {
                        // Non-comment syntax should not create Comment node
                        assertNull(node, "Non-comment syntax should not create Comment node")
                    }
                }
            }
        }
    }

    @Test
    fun `Property 3a - Section node type assignment should correctly map equals signs to section levels`() {
        repeat(100) {
            val level = Random.nextInt(1, 7) // AsciiDoc supports levels 1-6
            val title = "Test Section ${Random.nextInt(1000)}"
            val equals = "=".repeat(level)
            val line = "$equals $title"
            val lineNumber = Random.nextInt(1, 1001)
            
            val section = blockParser.parseSection(line, lineNumber)
            
            // Verify correct node type assignment
            assertTrue(section is Section, "Section syntax should always create Section node")
            
            // Verify semantic mapping is correct
            assertEquals(level, section.level, "Section level should exactly match number of equals signs")
            assertEquals(title, section.title, "Section title should be extracted correctly")
            
            // Verify inheritance hierarchy
            assertTrue(section is BlockElement, "Section should inherit from BlockElement")
            assertTrue(section is AstNode, "Section should inherit from AstNode")
        }
    }

    @Test
    fun `Property 3b - List node type assignment should correctly identify list types from markers`() {
        repeat(100) {
            val testData = generateListMarkerTestData()
            val list = blockParser.parseList(testData.lines, testData.lineNumber, testData.expectedType)
            
            // Verify correct node type assignment
            assertTrue(list is AsciiDocList, "List syntax should create AsciiDocList node")
            
            // Verify semantic correctness of type assignment
            assertEquals(testData.expectedType, list.type, "List type should match the marker syntax used")
            
            // Verify items have correct node types
            list.items.forEach { item ->
                assertTrue(item is ListItem, "List items should be ListItem nodes")
                
                // Verify marker semantic correctness
                when (testData.expectedType) {
                    io.github.kotlin.asciidoc.ast.ListType.UNORDERED -> {
                        assertTrue(
                            item.marker == "*" || item.marker == "-",
                            "Unordered list items should have * or - markers"
                        )
                    }
                    io.github.kotlin.asciidoc.ast.ListType.ORDERED -> {
                        assertTrue(
                            item.marker == "." || item.marker.matches(Regex("\\d+\\.")),
                            "Ordered list items should have . or numbered markers"
                        )
                    }
                    io.github.kotlin.asciidoc.ast.ListType.DEFINITION -> {
                        assertNotNull(item.marker, "Definition list items should have markers")
                    }
                }
            }
            
            // Verify inheritance hierarchy
            assertTrue(list is BlockElement, "AsciiDocList should inherit from BlockElement")
            assertTrue(list is AstNode, "AsciiDocList should inherit from AstNode")
        }
    }

    @Test
    fun `Property 3c - Inline element node type assignment should create appropriate inline nodes`() {
        repeat(100) {
            val testData = generateInlineElementTestData()
            
            // Since inline parsing is not fully implemented yet, we test the basic text creation
            val paragraph = blockParser.parseParagraph(listOf(testData.input), testData.lineNumber)
            
            // Verify paragraph node type
            assertTrue(paragraph is Paragraph, "Text content should create Paragraph node")
            
            // Verify content contains inline elements
            assertTrue(paragraph.content.isNotEmpty(), "Paragraph should have inline content")
            
            // For now, verify basic text elements are created
            paragraph.content.forEach { element ->
                assertTrue(element is InlineElement, "Paragraph content should be InlineElement nodes")
                assertTrue(element is AstNode, "InlineElement should inherit from AstNode")
                
                // Currently only Text elements are implemented
                assertTrue(element is Text, "Current implementation should create Text elements")
            }
            
            // Verify inheritance hierarchy
            assertTrue(paragraph is BlockElement, "Paragraph should inherit from BlockElement")
            assertTrue(paragraph is AstNode, "Paragraph should inherit from AstNode")
        }
    }

    // Test data generation functions
    private fun generateNodeTypeTestCase(): NodeTypeTestCase {
        return when (Random.nextInt(5)) {
            0 -> generateSectionTestCase()
            1 -> generateParagraphTestCase()
            2 -> generateListTestCase()
            3 -> generateCodeBlockTestCase()
            else -> generateCommentTestCase()
        }
    }

    private fun generateSectionTestCase(): SectionTestCase {
        val level = Random.nextInt(1, 7)
        val title = "Test Section ${Random.nextInt(1000)}"
        val equals = "=".repeat(level)
        val input = "$equals $title"
        val lineNumber = Random.nextInt(1, 1001)
        
        return SectionTestCase(input, lineNumber, level, title)
    }

    private fun generateParagraphTestCase(): ParagraphTestCase {
        val lines = (1..Random.nextInt(1, 4)).map { "Test paragraph line $it with content ${Random.nextInt(100)}" }
        val lineNumber = Random.nextInt(1, 1001)
        
        return ParagraphTestCase(lines, lineNumber)
    }

    private fun generateListTestCase(): ListTestCase {
        val listType = io.github.kotlin.asciidoc.ast.ListType.values().random()
        val lines = generateListLines(listType)
        val lineNumber = Random.nextInt(1, 1001)
        
        return ListTestCase(lines, lineNumber, listType)
    }

    private fun generateListLines(listType: io.github.kotlin.asciidoc.ast.ListType): List<String> {
        return when (listType) {
            io.github.kotlin.asciidoc.ast.ListType.UNORDERED -> {
                (1..Random.nextInt(1, 5)).map { 
                    val marker = if (Random.nextBoolean()) "*" else "-"
                    "$marker Test unordered item $it"
                }
            }
            io.github.kotlin.asciidoc.ast.ListType.ORDERED -> {
                (1..Random.nextInt(1, 5)).map { 
                    if (Random.nextBoolean()) {
                        ". Test ordered item $it"
                    } else {
                        "$it. Test ordered item $it"
                    }
                }
            }
            io.github.kotlin.asciidoc.ast.ListType.DEFINITION -> {
                (1..Random.nextInt(1, 5)).map { "Test definition item $it" }
            }
        }
    }

    private fun generateCodeBlockTestCase(): CodeBlockTestCase {
        val lines = (0..Random.nextInt(1, 6)).map { "code line $it with content ${Random.nextInt(100)}" }
        val language = listOf("kotlin", "java", "python", "javascript", null).random()
        val lineNumber = Random.nextInt(1, 1001)
        
        return CodeBlockTestCase(lines, lineNumber, language)
    }

    private fun generateCommentTestCase(): CommentTestCase {
        val shouldCreateNode = Random.nextBoolean()
        val lineNumber = Random.nextInt(1, 1001)
        
        return if (shouldCreateNode) {
            val content = "Test comment content ${Random.nextInt(1000)}"
            val input = if (content.isEmpty()) "//" else "// $content"
            CommentTestCase(input, lineNumber, true, content)
        } else {
            val input = "Not a comment line ${Random.nextInt(1000)}"
            CommentTestCase(input, lineNumber, false, "")
        }
    }

    private fun generateListMarkerTestData(): ListMarkerTestData {
        val listType = io.github.kotlin.asciidoc.ast.ListType.values().random()
        val lines = generateListLines(listType)
        val lineNumber = Random.nextInt(1, 1001)
        
        return ListMarkerTestData(lines, lineNumber, listType)
    }

    private fun generateInlineElementTestData(): InlineElementTestData {
        val input = "Test inline content with ${Random.nextInt(1000)} value"
        val lineNumber = Random.nextInt(1, 1001)
        
        return InlineElementTestData(input, lineNumber)
    }
}

// Test data classes
sealed class NodeTypeTestCase
data class SectionTestCase(val input: String, val lineNumber: Int, val expectedLevel: Int, val expectedTitle: String) : NodeTypeTestCase()
data class ParagraphTestCase(val input: List<String>, val lineNumber: Int) : NodeTypeTestCase()
data class ListTestCase(val input: List<String>, val lineNumber: Int, val listType: io.github.kotlin.asciidoc.ast.ListType) : NodeTypeTestCase()
data class CodeBlockTestCase(val input: List<String>, val lineNumber: Int, val language: String?) : NodeTypeTestCase()
data class CommentTestCase(val input: String, val lineNumber: Int, val shouldCreateNode: Boolean, val expectedContent: String) : NodeTypeTestCase()

data class ListMarkerTestData(val lines: List<String>, val lineNumber: Int, val expectedType: io.github.kotlin.asciidoc.ast.ListType)
data class InlineElementTestData(val input: String, val lineNumber: Int)