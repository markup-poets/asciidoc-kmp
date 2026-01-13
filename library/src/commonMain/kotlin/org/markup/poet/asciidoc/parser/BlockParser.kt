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
import org.markup.poet.asciidoc.ast.Text
import org.markup.poet.asciidoc.error.ErrorSeverity
import org.markup.poet.asciidoc.error.ParseError
import org.markup.poet.asciidoc.error.ParseWarning

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
 * Default implementation of BlockParser with enhanced error handling.
 */
class DefaultBlockParser(
    private val inlineParser: InlineParser = DefaultInlineParser(),
    private val attributeParser: AttributeParser = DefaultAttributeParser()
) : BlockParser {
    
    // Error collection for this parser instance
    private val errors = mutableListOf<ParseError>()
    private val warnings = mutableListOf<ParseWarning>()
    
    /**
     * Get collected errors from parsing operations.
     */
    fun getErrors(): List<ParseError> = errors.toList()
    
    /**
     * Get collected warnings from parsing operations.
     */
    fun getWarnings(): List<ParseWarning> = warnings.toList()
    
    /**
     * Clear collected errors and warnings.
     */
    fun clearErrorsAndWarnings() {
        errors.clear()
        warnings.clear()
    }
    
    override fun parseSection(line: String, lineNumber: Int): Section {
        val trimmed = line.trim()
        val equalsSigns = trimmed.takeWhile { it == '=' }
        val level = equalsSigns.length
        val title = trimmed.drop(level).trim()
        
        // Validate section header
        if (level == 0) {
            addError(
                message = "Invalid section header: no equals signs found",
                location = SourceLocation(lineNumber),
                severity = ErrorSeverity.ERROR
            )
        } else if (level > 6) {
            addError(
                message = "Section header level too deep (maximum 6 levels)",
                location = SourceLocation(lineNumber),
                severity = ErrorSeverity.ERROR
            )
        }
        
        if (title.isEmpty()) {
            addError(
                message = "Section header missing title",
                location = SourceLocation(lineNumber),
                severity = ErrorSeverity.ERROR
            )
        }
        
        return Section(
            level = level.coerceIn(1, 6), // Clamp to valid range
            title = title.ifEmpty { "Untitled Section" }, // Provide fallback
            children = emptyList(), // Children will be added by the main parser
            sourceLocation = SourceLocation(lineNumber)
        )
    }
    
    override fun parseParagraph(lines: List<String>, startLineNumber: Int): Paragraph {
        if (lines.isEmpty()) {
            addWarning(
                message = "Empty paragraph content",
                location = SourceLocation(startLineNumber)
            )
            return Paragraph(
                content = emptyList(),
                sourceLocation = SourceLocation(startLineNumber)
            )
        }
        
        try {
            val content = parseInlineContent(lines.joinToString(" "), startLineNumber)
            return Paragraph(
                content = content,
                sourceLocation = SourceLocation(startLineNumber)
            )
        } catch (e: Exception) {
            addError(
                message = "Error parsing paragraph inline content: ${e.message}",
                location = SourceLocation(startLineNumber),
                severity = ErrorSeverity.ERROR
            )
            
            // Fallback to plain text
            return Paragraph(
                content = listOf(Text(
                    content = lines.joinToString(" "),
                    sourceLocation = SourceLocation(startLineNumber)
                )),
                sourceLocation = SourceLocation(startLineNumber)
            )
        }
    }
    
    override fun parseList(lines: List<String>, startLineNumber: Int, listType: ListType): AsciiDocList {
        val items = mutableListOf<ListItem>()
        var currentLineNumber = startLineNumber
        
        if (lines.isEmpty()) {
            addWarning(
                message = "Empty list content",
                location = SourceLocation(startLineNumber)
            )
            return AsciiDocList(
                type = listType,
                items = emptyList(),
                sourceLocation = SourceLocation(startLineNumber)
            )
        }
        
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isNotEmpty()) {
                try {
                    val listItem = parseListItem(trimmed, currentLineNumber, listType)
                    items.add(listItem)
                } catch (e: Exception) {
                    addError(
                        message = "Error parsing list item at line $currentLineNumber: ${e.message}",
                        location = SourceLocation(currentLineNumber),
                        severity = ErrorSeverity.ERROR
                    )
                    
                    // Create fallback list item
                    val fallbackItem = ListItem(
                        marker = "*",
                        content = listOf(Text(
                            content = trimmed,
                            sourceLocation = SourceLocation(currentLineNumber)
                        )),
                        nestedList = null,
                        sourceLocation = SourceLocation(currentLineNumber)
                    )
                    items.add(fallbackItem)
                }
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
        return try {
            inlineParser.parseInlineElements(text, lineNumber)
        } catch (e: Exception) {
            addError(
                message = "Error parsing inline content: ${e.message}",
                location = SourceLocation(lineNumber),
                severity = ErrorSeverity.ERROR
            )
            
            // Fallback to plain text
            listOf(Text(
                content = text,
                sourceLocation = SourceLocation(lineNumber)
            ))
        }
    }
    
    private fun addError(message: String, location: SourceLocation, severity: ErrorSeverity = ErrorSeverity.ERROR) {
        errors.add(ParseError(message, location, severity))
    }
    
    private fun addWarning(message: String, location: SourceLocation) {
        warnings.add(ParseWarning(message, location))
    }
}