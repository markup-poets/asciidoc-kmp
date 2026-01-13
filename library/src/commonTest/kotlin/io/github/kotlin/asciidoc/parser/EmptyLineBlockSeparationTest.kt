package io.github.kotlin.asciidoc.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.random.Random

/**
 * Property-based tests for empty line block separation.
 * **Feature: asciidoc-parser, Property 6: Empty Line Block Separation**
 * **Validates: Requirements 1.3, 1.4**
 */
class EmptyLineBlockSeparationTest {

    private val lineProcessor = DefaultLineProcessor()

    @Test
    fun `Property 6 - Empty lines and whitespace-only lines should properly separate block elements`() {
        // Run property-based test with multiple iterations
        repeat(100) {
            val lines = generateDocumentWithEmptyLines()
            val context = ParseContext()
            val results = mutableListOf<LineResult>()
            
            lines.forEachIndexed { index, line ->
                val lineNumber = index + 1
                val result = lineProcessor.processLine(line, lineNumber, context)
                results.add(result)
            }
            
            // Verify empty lines are correctly identified as block delimiters
            lines.forEachIndexed { index, line ->
                val isDelimiter = lineProcessor.isBlockDelimiter(line)
                val result = results[index]
                
                if (line.isBlank()) {
                    // Empty or whitespace-only lines should be block delimiters
                    assertTrue(isDelimiter, "Line '$line' should be a block delimiter")
                    assertEquals(BlockType.EMPTY, result.blockType, "Empty line should have EMPTY block type")
                } else {
                    // Non-empty lines should not be block delimiters
                    assertFalse(isDelimiter, "Line '$line' should not be a block delimiter")
                    assertNotEquals(BlockType.EMPTY, result.blockType, "Non-empty line should not have EMPTY block type")
                }
            }
            
            // Verify that blocks are properly separated by empty lines
            val blockBoundaries = findBlockBoundaries(results)
            blockBoundaries.forEach { (start, end) ->
                // Each block should be a contiguous sequence of non-empty lines
                for (i in start..end) {
                    assertNotEquals(BlockType.EMPTY, results[i].blockType, "Block content should not be EMPTY")
                }
                
                // Blocks should be separated by empty lines (except at document boundaries)
                if (start > 0) {
                    assertEquals(BlockType.EMPTY, results[start - 1].blockType, "Block should be preceded by empty line")
                }
                if (end < results.size - 1) {
                    assertEquals(BlockType.EMPTY, results[end + 1].blockType, "Block should be followed by empty line")
                }
            }
        }
    }

    @Test
    fun `Property 6a - Whitespace-only lines should be treated as empty lines`() {
        repeat(100) {
            val line = generateWhitespaceOnlyLine()
            val isDelimiter = lineProcessor.isBlockDelimiter(line)
            val blockType = lineProcessor.determineBlockType(line)
            
            // Whitespace-only lines should be block delimiters
            assertTrue(isDelimiter, "Whitespace-only line '$line' should be a block delimiter")
            assertEquals(BlockType.EMPTY, blockType, "Whitespace-only line should have EMPTY block type")
        }
    }

    @Test
    fun `Property 6b - Block delimiter detection should be consistent`() {
        repeat(100) {
            val line = generateMixedContentLine()
            val isDelimiter1 = lineProcessor.isBlockDelimiter(line)
            val isDelimiter2 = lineProcessor.isBlockDelimiter(line)
            
            // Block delimiter detection should be deterministic
            assertEquals(isDelimiter1, isDelimiter2, "Block delimiter detection should be consistent")
            
            // Should match the expected behavior based on content
            val expectedDelimiter = line.isBlank()
            assertEquals(expectedDelimiter, isDelimiter1, "Block delimiter detection should match blank check")
        }
    }
}
// Helper function to find block boundaries in a sequence of line results
private fun findBlockBoundaries(results: List<LineResult>): List<Pair<Int, Int>> {
    val boundaries = mutableListOf<Pair<Int, Int>>()
    var blockStart: Int? = null
    
    results.forEachIndexed { index, result ->
        when {
            result.blockType != BlockType.EMPTY && blockStart == null -> {
                // Start of a new block
                blockStart = index
            }
            result.blockType == BlockType.EMPTY && blockStart != null -> {
                // End of current block
                boundaries.add(blockStart!! to index - 1)
                blockStart = null
            }
        }
    }
    
    // Handle case where document ends with a block
    blockStart?.let { start ->
        boundaries.add(start to results.size - 1)
    }
    
    return boundaries
}

// Generators for property-based testing using plain Kotlin
private fun generateDocumentWithEmptyLines(): List<String> {
    val blocks = (1..Random.nextInt(1, 6)).map { generateContentBlock() }
    val result = mutableListOf<String>()
    
    blocks.forEachIndexed { index, block ->
        result.addAll(block)
        // Add empty lines between blocks (but not after the last block)
        if (index < blocks.size - 1) {
            val emptyLines = (1..Random.nextInt(1, 4)).map { generateEmptyOrWhitespaceLine() }
            result.addAll(emptyLines)
        }
    }
    
    return result
}

private fun generateContentBlock(): List<String> = 
    (1..Random.nextInt(1, 6)).map { generateNonEmptyContentLine() }

private fun generateNonEmptyContentLine(): String = when (Random.nextInt(4)) {
    0 -> generateSectionHeaderLine()
    1 -> generateListItemLine()
    2 -> generateParagraphLine()
    else -> generateCommentLine()
}

private fun generateSectionHeaderLine(): String {
    val level = Random.nextInt(1, 7)
    val equals = "=".repeat(level)
    val title = generateRandomString(1, 50)
    return "$equals $title"
}

private fun generateListItemLine(): String {
    val marker = listOf("*", "-", ".")[Random.nextInt(3)]
    val content = generateRandomString(1, 100)
    return "$marker $content"
}

private fun generateParagraphLine(): String {
    var line: String
    do {
        line = generateRandomString(1, 200).trim()
    } while (line.isEmpty() || 
             line.startsWith("=") || 
             line.startsWith("*") || 
             line.startsWith("-") || 
             line.startsWith(".") || 
             line.startsWith("//"))
    return line
}

private fun generateCommentLine(): String {
    val content = generateRandomString(1, 100)
    return "// $content"
}

private fun generateEmptyOrWhitespaceLine(): String = when (Random.nextInt(6)) {
    0 -> ""
    1 -> "   "
    2 -> "\t"
    3 -> "  \t  "
    4 -> "\t\t"
    else -> "    "
}

private fun generateWhitespaceOnlyLine(): String = when (Random.nextInt(4)) {
    0 -> ""
    1 -> " ".repeat(Random.nextInt(1, 11))
    2 -> "\t".repeat(Random.nextInt(1, 6))
    else -> {
        val spaces = Random.nextInt(0, 6)
        val tabs = Random.nextInt(0, 4)
        " ".repeat(spaces) + "\t".repeat(tabs)
    }
}

private fun generateMixedContentLine(): String = if (Random.nextBoolean()) {
    generateEmptyOrWhitespaceLine()
} else {
    generateNonEmptyContentLine()
}

private fun generateRandomString(minLength: Int, maxLength: Int): String {
    val length = Random.nextInt(minLength, maxLength + 1)
    val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 "
    return (1..length).map { chars[Random.nextInt(chars.length)] }.joinToString("")
}