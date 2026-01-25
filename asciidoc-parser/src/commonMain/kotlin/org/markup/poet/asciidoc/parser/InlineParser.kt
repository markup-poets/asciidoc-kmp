package org.markup.poet.asciidoc.parser

import org.markup.poet.asciidoc.ast.AttributeReference
import org.markup.poet.asciidoc.ast.Code
import org.markup.poet.asciidoc.ast.Emphasis
import org.markup.poet.asciidoc.ast.Image
import org.markup.poet.asciidoc.ast.InlineElement
import org.markup.poet.asciidoc.ast.Link
import org.markup.poet.asciidoc.ast.SourceLocation
import org.markup.poet.asciidoc.ast.Strong
import org.markup.poet.asciidoc.ast.Text

/**
 * Interface for parsing inline markup within text content.
 */
interface InlineParser {
    /**
     * Parse inline elements from text content.
     */
    fun parseInlineElements(text: String, startLineNumber: Int = 0): List<InlineElement>
    
    /**
     * Parse strong (*bold*) markup from text starting at the given index.
     */
    fun parseStrong(text: String, startIndex: Int, lineNumber: Int): ParsedInline?
    
    /**
     * Parse emphasis (_italic_) markup from text starting at the given index.
     */
    fun parseEmphasis(text: String, startIndex: Int, lineNumber: Int): ParsedInline?
    
    /**
     * Parse inline code (`code`) markup from text starting at the given index.
     */
    fun parseCode(text: String, startIndex: Int, lineNumber: Int): ParsedInline?
    
    /**
     * Parse link (link:url[text]) markup from text starting at the given index.
     */
    fun parseLink(text: String, startIndex: Int, lineNumber: Int): ParsedInline?
    
    /**
     * Parse image (image:path[alt]) markup from text starting at the given index.
     */
    fun parseImage(text: String, startIndex: Int, lineNumber: Int): ParsedInline?
    
    /**
     * Parse attribute reference ({key}) markup from text starting at the given index.
     */
    fun parseAttributeReference(text: String, startIndex: Int, lineNumber: Int): ParsedInline?
}

/**
 * Result of parsing an inline element, containing the element and the end index.
 */
data class ParsedInline(
    val element: InlineElement,
    val endIndex: Int
)

/**
 * Default implementation of InlineParser.
 */
class DefaultInlineParser : InlineParser {
    
    companion object {
        private const val MAX_NESTING_DEPTH = 10
    }
    
    override fun parseInlineElements(text: String, startLineNumber: Int): List<InlineElement> {
        return parseInlineElementsWithDepth(text, startLineNumber, 0)
    }
    
    private fun parseInlineElementsWithDepth(text: String, startLineNumber: Int, depth: Int): List<InlineElement> {
        // Prevent infinite recursion
        if (depth > MAX_NESTING_DEPTH) {
            return listOf(Text(
                content = text,
                sourceLocation = SourceLocation(startLineNumber)
            ))
        }
        
        val elements = mutableListOf<InlineElement>()
        var currentIndex = 0
        
        while (currentIndex < text.length) {
            val char = text[currentIndex]
            
            // Try to parse different inline elements
            val parsed = when (char) {
                '*' -> parseStrongWithDepth(text, currentIndex, startLineNumber, depth)
                '_' -> parseEmphasisWithDepth(text, currentIndex, startLineNumber, depth)
                '`' -> parseCode(text, currentIndex, startLineNumber)
                '{' -> parseAttributeReference(text, currentIndex, startLineNumber)
                'l' -> if (text.substring(currentIndex).startsWith("link:")) {
                    parseLink(text, currentIndex, startLineNumber)
                } else null
                'i' -> if (text.substring(currentIndex).startsWith("image:")) {
                    parseImage(text, currentIndex, startLineNumber)
                } else null
                '\\' -> parseEscapedCharacter(text, currentIndex, startLineNumber)
                else -> null
            }
            
            if (parsed != null) {
                elements.add(parsed.element)
                currentIndex = parsed.endIndex
            } else {
                // If we couldn't parse a markup element at this position,
                // find the next markup character (starting from currentIndex + 1)
                val nextMarkupIndex = findNextMarkupCharacter(text, currentIndex + 1)
                
                // If nextMarkupIndex is still currentIndex (shouldn't happen with +1),
                // or if we're at a markup char that failed to parse, include it as text
                val endIndex = if (nextMarkupIndex > currentIndex) nextMarkupIndex else currentIndex + 1
                val textContent = text.substring(currentIndex, endIndex)
                
                if (textContent.isNotEmpty()) {
                    // Trim trailing whitespace/newlines for position calculation
                    val trimmedContent = textContent.trimEnd()
                    
                    if (trimmedContent.isNotEmpty()) {
                        // Track actual character positions (1-based)
                        // Start column is 1-based index of first character
                        // End column is 1-based index of LAST character (INCLUSIVE, not exclusive!)
                        val startCol = currentIndex + 1
                        val endCol = currentIndex + trimmedContent.length  // Last char position (inclusive)
                        
                        elements.add(
                            Text(
                                content = trimmedContent,
                                sourceLocation = SourceLocation(
                                    line = startLineNumber,
                                    column = startCol,
                                    endLine = startLineNumber,
                                    endColumn = endCol
                                )
                            )
                        )
                    }
                }
                
                currentIndex = endIndex
            }
        }
        
        // If no elements were parsed, return the entire text as a single Text element
        if (elements.isEmpty() && text.isNotEmpty()) {
            val trimmedText = text.trimEnd()
            if (trimmedText.isNotEmpty()) {
                elements.add(
                    Text(
                        content = trimmedText,
                        sourceLocation = SourceLocation(
                            line = startLineNumber,
                            column = 1,
                            endLine = startLineNumber,
                            endColumn = trimmedText.length  // Last char position (inclusive)
                        )
                    )
                )
            }
        }
        
        return elements
    }
    
    override fun parseStrong(text: String, startIndex: Int, lineNumber: Int): ParsedInline? {
        return parseStrongWithDepth(text, startIndex, lineNumber, 0)
    }
    
    private fun parseStrongWithDepth(text: String, startIndex: Int, lineNumber: Int, depth: Int): ParsedInline? {
        if (startIndex >= text.length || text[startIndex] != '*') {
            return null
        }
        
        // Prevent infinite recursion
        if (depth > MAX_NESTING_DEPTH) {
            return null
        }
        
        // Find the closing *
        val closingIndex = findClosingDelimiter(text, startIndex + 1, '*')
        if (closingIndex == -1) {
            return null
        }
        
        val innerText = text.substring(startIndex + 1, closingIndex)
        if (innerText.isEmpty()) {
            return null
        }
        
        // Parse nested inline elements within the strong text with depth tracking
        val innerElements = parseInlineElementsWithDepth(innerText, lineNumber, depth + 1)
        
        // Track actual character positions (1-based)
        val startCol = startIndex + 1
        val endCol = closingIndex + 2  // Position after the closing *
        
        return ParsedInline(
            element = Strong(
                content = innerElements,
                sourceLocation = SourceLocation(
                    line = lineNumber,
                    column = startCol,
                    endLine = lineNumber,
                    endColumn = endCol
                )
            ),
            endIndex = closingIndex + 1
        )
    }
    
    override fun parseEmphasis(text: String, startIndex: Int, lineNumber: Int): ParsedInline? {
        return parseEmphasisWithDepth(text, startIndex, lineNumber, 0)
    }
    
    private fun parseEmphasisWithDepth(text: String, startIndex: Int, lineNumber: Int, depth: Int): ParsedInline? {
        if (startIndex >= text.length || text[startIndex] != '_') {
            return null
        }
        
        // Prevent infinite recursion
        if (depth > MAX_NESTING_DEPTH) {
            return null
        }
        
        // Find the closing _
        val closingIndex = findClosingDelimiter(text, startIndex + 1, '_')
        if (closingIndex == -1) {
            return null
        }
        
        val innerText = text.substring(startIndex + 1, closingIndex)
        if (innerText.isEmpty()) {
            return null
        }
        
        // Parse nested inline elements within the emphasis text with depth tracking
        val innerElements = parseInlineElementsWithDepth(innerText, lineNumber, depth + 1)
        
        // Track actual character positions (1-based)
        val startCol = startIndex + 1
        val endCol = closingIndex + 2  // +2 to include the closing _
        
        return ParsedInline(
            element = Emphasis(
                content = innerElements,
                sourceLocation = SourceLocation(
                    line = lineNumber,
                    column = startCol,
                    endLine = lineNumber,
                    endColumn = endCol
                )
            ),
            endIndex = closingIndex + 1
        )
    }
    
    override fun parseCode(text: String, startIndex: Int, lineNumber: Int): ParsedInline? {
        if (startIndex >= text.length || text[startIndex] != '`') {
            return null
        }
        
        // Find the closing `
        val closingIndex = findClosingDelimiter(text, startIndex + 1, '`')
        if (closingIndex == -1) {
            return null
        }
        
        val codeContent = text.substring(startIndex + 1, closingIndex)
        
        // Track actual character positions (1-based)
        val startCol = startIndex + 1
        val endCol = closingIndex + 2  // +2 to include the closing `
        
        return ParsedInline(
            element = Code(
                content = codeContent,
                sourceLocation = SourceLocation(
                    line = lineNumber,
                    column = startCol,
                    endLine = lineNumber,
                    endColumn = endCol
                )
            ),
            endIndex = closingIndex + 1
        )
    }
    
    override fun parseLink(text: String, startIndex: Int, lineNumber: Int): ParsedInline? {
        if (!text.substring(startIndex).startsWith("link:")) {
            return null
        }
        
        val urlStartIndex = startIndex + 5 // "link:".length
        val bracketIndex = text.indexOf('[', urlStartIndex)
        if (bracketIndex == -1) {
            return null
        }
        
        val closingBracketIndex = text.indexOf(']', bracketIndex + 1)
        if (closingBracketIndex == -1) {
            return null
        }
        
        val url = text.substring(urlStartIndex, bracketIndex)
        val linkText = text.substring(bracketIndex + 1, closingBracketIndex)
        
        if (url.isEmpty()) {
            return null
        }
        
        // Track actual character positions (1-based)
        val startCol = startIndex + 1
        val endCol = closingBracketIndex + 2  // +2 to include the closing ]
        
        return ParsedInline(
            element = Link(
                url = url,
                text = linkText,
                sourceLocation = SourceLocation(
                    line = lineNumber,
                    column = startCol,
                    endLine = lineNumber,
                    endColumn = endCol
                )
            ),
            endIndex = closingBracketIndex + 1
        )
    }
    
    override fun parseImage(text: String, startIndex: Int, lineNumber: Int): ParsedInline? {
        if (!text.substring(startIndex).startsWith("image:")) {
            return null
        }
        
        val pathStartIndex = startIndex + 6 // "image:".length
        val bracketIndex = text.indexOf('[', pathStartIndex)
        if (bracketIndex == -1) {
            return null
        }
        
        val closingBracketIndex = text.indexOf(']', bracketIndex + 1)
        if (closingBracketIndex == -1) {
            return null
        }
        
        val path = text.substring(pathStartIndex, bracketIndex)
        val altText = text.substring(bracketIndex + 1, closingBracketIndex)
        
        if (path.isEmpty()) {
            return null
        }
        
        // Track actual character positions (1-based)
        val startCol = startIndex + 1
        val endCol = closingBracketIndex + 2  // +2 to include the closing ]
        
        return ParsedInline(
            element = Image(
                path = path,
                altText = altText,
                sourceLocation = SourceLocation(
                    line = lineNumber,
                    column = startCol,
                    endLine = lineNumber,
                    endColumn = endCol
                )
            ),
            endIndex = closingBracketIndex + 1
        )
    }
    
    override fun parseAttributeReference(text: String, startIndex: Int, lineNumber: Int): ParsedInline? {
        if (startIndex >= text.length || text[startIndex] != '{') {
            return null
        }
        
        // Find the closing }
        val closingIndex = text.indexOf('}', startIndex + 1)
        if (closingIndex == -1) {
            return null
        }
        
        val key = text.substring(startIndex + 1, closingIndex)
        if (key.isEmpty()) {
            return null
        }

        // AsciiDoc attributes must start with a letter or underscore and contain only alphanumeric, underscores, or hyphens
        val attributeKeyRegex = Regex("^[a-zA-Z_][a-zA-Z0-9_-]*$")
        if (!attributeKeyRegex.matches(key)) {
            return null
        }
        
        // Track actual character positions (1-based)
        val startCol = startIndex + 1
        val endCol = closingIndex + 2  // +2 to include the closing }
        
        return ParsedInline(
            element = AttributeReference(
                key = key,
                sourceLocation = SourceLocation(
                    line = lineNumber,
                    column = startCol,
                    endLine = lineNumber,
                    endColumn = endCol
                )
            ),
            endIndex = closingIndex + 1
        )
    }
    
    private fun parseEscapedCharacter(text: String, startIndex: Int, lineNumber: Int): ParsedInline? {
        if (startIndex >= text.length - 1 || text[startIndex] != '\\') {
            return null
        }
        
        val escapedChar = text[startIndex + 1]
        
        // Only escape markup characters
        if (escapedChar in setOf('*', '_', '`', '\\', '[', ']', '{', '}')) {
            // Track actual character positions (1-based)
            val startCol = startIndex + 1
            val endCol = startIndex + 3  // +3 for backslash + escaped char
            
            return ParsedInline(
                element = Text(
                    content = escapedChar.toString(),
                    sourceLocation = SourceLocation(
                        line = lineNumber,
                        column = startCol,
                        endLine = lineNumber,
                        endColumn = endCol
                    )
                ),
                endIndex = startIndex + 2
            )
        }
        
        return null
    }
    
    private fun findClosingDelimiter(text: String, startIndex: Int, delimiter: Char): Int {
        var index = startIndex
        
        while (index < text.length) {
            if (text[index] == delimiter) {
                // Check if it's escaped
                if (index > 0 && text[index - 1] == '\\') {
                    index++
                    continue
                }
                return index
            }
            index++
        }
        
        return -1
    }
    
    private fun findNextMarkupCharacter(text: String, startIndex: Int): Int {
        val markupChars = setOf('*', '_', '`', '\\', '{')
        
        for (i in startIndex until text.length) {
            val char = text[i]
            if (char in markupChars) {
                return i
            }
            
            // Check for link: and image: patterns
            if (char == 'l' && text.substring(i).startsWith("link:")) {
                return i
            }
            if (char == 'i' && text.substring(i).startsWith("image:")) {
                return i
            }
        }
        
        return text.length
    }
}