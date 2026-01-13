package org.markup.poet.asciidoc.parser

import org.markup.poet.asciidoc.ast.AsciiDocList
import org.markup.poet.asciidoc.ast.CodeBlock
import org.markup.poet.asciidoc.ast.Comment
import org.markup.poet.asciidoc.ast.InlineElement
import org.markup.poet.asciidoc.ast.ListItem
import org.markup.poet.asciidoc.ast.ListType
import org.markup.poet.asciidoc.ast.Paragraph
import org.markup.poet.asciidoc.ast.Section
import org.markup.poet.asciidoc.ast.SourceLocation

/**
 * Interface for parsing block-level elements from AsciiDoc source.
 */
interface BlockParser {
    /**
     * Parse a section header from a line.
     */
    fun parseSection(line: String, lineNumber: Int): Section
    
    /**
     * Parse a paragraph from multiple lines.
     */
    fun parseParagraph(lines: List<String>, startLineNumber: Int): Paragraph
    
    /**
     * Parse a list from multiple lines with nesting support.
     */
    fun parseList(lines: List<String>, startLineNumber: Int, listType: ListType): AsciiDocList
    
    /**
     * Parse a code block from lines between delimiters.
     */
    fun parseCodeBlock(lines: List<String>, startLineNumber: Int, language: String? = null): CodeBlock
    
    /**
     * Parse a comment from a line.
     */
    fun parseComment(line: String, lineNumber: Int): Comment?
    
    /**
     * Parse an attribute definition from a line.
     */
    fun parseAttributeDefinition(line: String, lineNumber: Int): AttributeDefinition?
}

/**
 * Default implementation of BlockParser.
 */
class DefaultBlockParser(
    private val inlineParser: InlineParser = DefaultInlineParser(),
    private val attributeParser: AttributeParser = DefaultAttributeParser()
) : BlockParser {
    
    override fun parseSection(line: String, lineNumber: Int): Section {
        val trimmed = line.trim()
        val equalsSigns = trimmed.takeWhile { it == '=' }
        val level = equalsSigns.length
        val title = trimmed.drop(level).trim()
        
        return Section(
            level = level,
            title = title,
            children = emptyList(), // Children will be added by the main parser
            sourceLocation = SourceLocation(lineNumber)
        )
    }
    
    override fun parseParagraph(lines: List<String>, startLineNumber: Int): Paragraph {
        val content = parseInlineContent(lines.joinToString(" "), startLineNumber)
        
        return Paragraph(
            content = content,
            sourceLocation = SourceLocation(startLineNumber)
        )
    }
    
    override fun parseList(lines: List<String>, startLineNumber: Int, listType: ListType): AsciiDocList {
        val items = mutableListOf<ListItem>()
        var currentLineNumber = startLineNumber
        
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isNotEmpty()) {
                val listItem = parseListItem(trimmed, currentLineNumber, listType)
                items.add(listItem)
            }
            currentLineNumber++
        }
        
        return AsciiDocList(
            type = listType,
            items = items,
            sourceLocation = SourceLocation(startLineNumber)
        )
    }
    
    override fun parseCodeBlock(lines: List<String>, startLineNumber: Int, language: String?): CodeBlock {
        val content = lines.joinToString("\n")
        
        return CodeBlock(
            language = language,
            content = content,
            sourceLocation = SourceLocation(startLineNumber)
        )
    }
    
    override fun parseComment(line: String, lineNumber: Int): Comment? {
        val trimmed = line.trim()
        if (!trimmed.startsWith("//")) {
            return null
        }
        
        val content = if (trimmed.startsWith("// ")) {
            trimmed.drop(3)
        } else {
            trimmed.drop(2)
        }
        
        return Comment(
            content = content,
            sourceLocation = SourceLocation(lineNumber)
        )
    }
    
    override fun parseAttributeDefinition(line: String, lineNumber: Int): AttributeDefinition? {
        return attributeParser.parseAttributeDefinition(line, lineNumber)
    }
    
    private fun parseListItem(line: String, lineNumber: Int, listType: ListType): ListItem {
        val (marker, content) = extractListItemContent(line, listType)
        val inlineContent = parseInlineContent(content, lineNumber)
        
        return ListItem(
            marker = marker,
            content = inlineContent,
            nestedList = null, // Nesting will be handled by the main parser
            sourceLocation = SourceLocation(lineNumber)
        )
    }
    
    private fun extractListItemContent(line: String, listType: ListType): Pair<String, String> {
        val trimmed = line.trim()
        
        return when (listType) {
            ListType.UNORDERED -> {
                when {
                    trimmed.startsWith("* ") -> "*" to trimmed.drop(2)
                    trimmed.startsWith("- ") -> "-" to trimmed.drop(2)
                    else -> "*" to trimmed // Default to * marker for unmatched unordered items
                }
            }
            ListType.ORDERED -> {
                when {
                    trimmed.startsWith(". ") -> "." to trimmed.drop(2)
                    trimmed.matches(Regex("^\\d+\\. .*")) -> {
                        val dotIndex = trimmed.indexOf(". ")
                        val marker = trimmed.substring(0, dotIndex + 1)
                        marker to trimmed.drop(dotIndex + 2)
                    }
                    else -> "1." to trimmed // Default to 1. marker for unmatched ordered items
                }
            }
            ListType.DEFINITION -> {
                // Definition lists not implemented in this basic version
                ":" to trimmed // Default marker for definition lists
            }
        }
    }
    
    private fun parseInlineContent(text: String, lineNumber: Int = 0): List<InlineElement> {
        return inlineParser.parseInlineElements(text, lineNumber)
    }
}