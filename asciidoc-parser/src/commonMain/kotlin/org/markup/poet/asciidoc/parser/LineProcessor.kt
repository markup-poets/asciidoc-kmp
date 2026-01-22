package org.markup.poet.asciidoc.parser

import org.markup.poet.asciidoc.ast.SourceLocation

/**
 * Interface for processing AsciiDoc lines and determining block types.
 */
interface LineProcessor {
    /**
     * Process a single line and determine its block type and content.
     */
    fun processLine(line: String, lineNumber: Int, context: ParseContext): LineResult
    
    /**
     * Check if a line is a block delimiter (empty or whitespace-only).
     */
    fun isBlockDelimiter(line: String): Boolean
    
    /**
     * Determine the block type based on line content.
     */
    fun determineBlockType(line: String): BlockType
}

/**
 * Result of processing a single line.
 */
data class LineResult(
    val blockType: BlockType,
    val content: String,
    val attributes: Map<String, String> = emptyMap(),
    val sourceLocation: SourceLocation
)

/**
 * Enumeration of block types that can be detected from line content.
 */
enum class BlockType {
    EMPTY,
    SECTION_HEADER,
    UNORDERED_LIST,
    ORDERED_LIST,
    CODE_BLOCK_DELIMITER,
    PARAGRAPH,
    COMMENT,
    ATTRIBUTE_DEFINITION,
    INCLUDE_DIRECTIVE
}

/**
 * Parsing context to track state across lines.
 */
data class ParseContext(
    val inCodeBlock: Boolean = false,
    val currentListLevel: Int = 0,
    val listType: BlockType? = null
)

/**
 * Default implementation of LineProcessor.
 */
class DefaultLineProcessor : LineProcessor {
    
    override fun processLine(line: String, lineNumber: Int, context: ParseContext): LineResult {
        val blockType = if (context.inCodeBlock && !isCodeBlockDelimiter(line)) {
            // Inside code block, treat everything as paragraph content
            BlockType.PARAGRAPH
        } else {
            determineBlockType(line)
        }
        
        val content = extractContent(line, blockType)
        val attributes = extractAttributes(line, blockType)
        val sourceLocation = SourceLocation(lineNumber)
        
        return LineResult(blockType, content, attributes, sourceLocation)
    }
    
    override fun isBlockDelimiter(line: String): Boolean {
        return line.isBlank()
    }
    
    override fun determineBlockType(line: String): BlockType {
        val trimmed = line.trim()
        
        return when {
            trimmed.isEmpty() -> BlockType.EMPTY
            isCodeBlockDelimiter(trimmed) -> BlockType.CODE_BLOCK_DELIMITER
            isSectionHeader(trimmed) -> BlockType.SECTION_HEADER
            isUnorderedListItem(trimmed) -> BlockType.UNORDERED_LIST
            isOrderedListItem(trimmed) -> BlockType.ORDERED_LIST
            isComment(trimmed) -> BlockType.COMMENT
            isIncludeDirective(trimmed) -> BlockType.INCLUDE_DIRECTIVE
            isAttributeDefinition(trimmed) -> BlockType.ATTRIBUTE_DEFINITION
            else -> BlockType.PARAGRAPH
        }
    }
    
    private fun isCodeBlockDelimiter(line: String): Boolean {
        return line.startsWith("----") && line.all { it == '-' } && line.length >= 4
    }
    
    private fun isSectionHeader(line: String): Boolean {
        return line.startsWith("=") && line.contains(" ")
    }
    
    private fun isUnorderedListItem(line: String): Boolean {
        return line.startsWith("* ") || line.startsWith("- ")
    }
    
    private fun isOrderedListItem(line: String): Boolean {
        return line.matches(Regex("^\\d+\\. .*")) || line.startsWith(". ")
    }
    
    private fun isComment(line: String): Boolean {
        return line.startsWith("//")
    }
    
    private fun isAttributeDefinition(line: String): Boolean {
        val trimmed = line.trim()
        return trimmed.startsWith(":") && trimmed.indexOf(':', 1) != -1
    }

    private fun isIncludeDirective(line: String): Boolean {
        val trimmed = line.trim()
        return trimmed.startsWith("include::") && trimmed.contains("[") && trimmed.endsWith("]")
    }
    
    private fun extractContent(line: String, blockType: BlockType): String {
        val trimmed = line.trim()
        
        return when (blockType) {
            BlockType.SECTION_HEADER -> {
                val equalsSigns = trimmed.takeWhile { it == '=' }
                trimmed.drop(equalsSigns.length).trim()
            }
            BlockType.UNORDERED_LIST -> {
                when {
                    trimmed.startsWith("* ") -> trimmed.drop(2)
                    trimmed.startsWith("- ") -> trimmed.drop(2)
                    else -> trimmed
                }
            }
            BlockType.ORDERED_LIST -> {
                when {
                    trimmed.startsWith(". ") -> trimmed.drop(2)
                    trimmed.matches(Regex("^\\d+\\. .*")) -> {
                        val dotIndex = trimmed.indexOf(". ")
                        trimmed.drop(dotIndex + 2)
                    }
                    else -> trimmed
                }
            }
            BlockType.COMMENT -> {
                if (trimmed.startsWith("// ")) trimmed.drop(3) else trimmed.drop(2)
            }
            BlockType.ATTRIBUTE_DEFINITION -> {
                val keyEndIndex = trimmed.indexOf(':', 1)
                if (keyEndIndex > 0) trimmed.substring(keyEndIndex + 1).trim() else ""
            }
            BlockType.CODE_BLOCK_DELIMITER -> ""
            BlockType.EMPTY -> ""
            BlockType.PARAGRAPH -> trimmed
            BlockType.INCLUDE_DIRECTIVE -> trimmed
        }
    }
    
    private fun extractAttributes(line: String, blockType: BlockType): Map<String, String> {
        return when (blockType) {
            BlockType.SECTION_HEADER -> {
                val trimmed = line.trim()
                val equalsSigns = trimmed.takeWhile { it == '=' }
                mapOf("level" to equalsSigns.length.toString())
            }
            BlockType.UNORDERED_LIST -> {
                val marker = when {
                    line.trim().startsWith("* ") -> "*"
                    line.trim().startsWith("- ") -> "-"
                    else -> ""
                }
                mapOf("marker" to marker)
            }
            BlockType.ORDERED_LIST -> {
                val trimmed = line.trim()
                val marker = when {
                    trimmed.startsWith(". ") -> "."
                    trimmed.matches(Regex("^\\d+\\. .*")) -> {
                        val dotIndex = trimmed.indexOf(". ")
                        trimmed.substring(0, dotIndex + 1)
                    }
                    else -> ""
                }
                mapOf("marker" to marker)
            }
            BlockType.ATTRIBUTE_DEFINITION -> {
                val trimmed = line.trim()
                val keyEndIndex = trimmed.indexOf(':', 1)
                if (keyEndIndex > 0) {
                    val key = trimmed.substring(1, keyEndIndex)
                    val value = trimmed.substring(keyEndIndex + 1).trim()
                    mapOf("key" to key, "value" to value)
                } else {
                    emptyMap()
                }
            }
            else -> emptyMap()
        }
    }
}