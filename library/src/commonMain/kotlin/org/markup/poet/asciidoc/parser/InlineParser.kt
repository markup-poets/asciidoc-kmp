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
    
    override fun parseInlineElements(text: String, startLineNumber: Int): List<InlineElement> {
        val elements = mutableListOf<InlineElement>()
        var currentIndex = 0
        
        while (currentIndex < text.length) {
            val char = text[currentIndex]
            
            // Try to parse different inline elements
            val parsed = when (char) {
                '*' -> parseStrong(text, currentIndex, startLineNumber)
                '_' -> parseEmphasis(text, currentIndex, startLineNumber)
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
                // Find the next markup character or end of text
                val nextMarkupIndex = findNextMarkupCharacter(text, currentIndex)
                val textContent = text.substring(currentIndex, nextMarkupIndex)
                
                if (textContent.isNotEmpty()) {
                    elements.add(
                        Text(
                            content = textContent,
                            sourceLocation = SourceLocation(startLineNumber)
                        )
                    )
                }
                
                currentIndex = nextMarkupIndex
            }
        }
        
        // If no elements were parsed, return the entire text as a single Text element
        if (elements.isEmpty() && text.isNotEmpty()) {
            elements.add(
                Text(
                    content = text,
                    sourceLocation = SourceLocation(startLineNumber)
                )
            )
        }
        
        return elements
    }
    
    override fun parseStrong(text: String, startIndex: Int, lineNumber: Int): ParsedInline? {
        if (startIndex >= text.length || text[startIndex] != '*') {
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
        
        // Parse nested inline elements within the strong text
        val innerElements = parseInlineElements(innerText, lineNumber)
        
        return ParsedInline(
            element = Strong(
                content = innerElements,
                sourceLocation = SourceLocation(lineNumber)
            ),
            endIndex = closingIndex + 1
        )
    }
    
    override fun parseEmphasis(text: String, startIndex: Int, lineNumber: Int): ParsedInline? {
        if (startIndex >= text.length || text[startIndex] != '_') {
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
        
        // Parse nested inline elements within the emphasis text
        val innerElements = parseInlineElements(innerText, lineNumber)
        
        return ParsedInline(
            element = Emphasis(
                content = innerElements,
                sourceLocation = SourceLocation(lineNumber)
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
        
        return ParsedInline(
            element = Code(
                content = codeContent,
                sourceLocation = SourceLocation(lineNumber)
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
        
        return ParsedInline(
            element = Link(
                url = url,
                text = linkText,
                sourceLocation = SourceLocation(lineNumber)
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
        
        return ParsedInline(
            element = Image(
                path = path,
                altText = altText,
                sourceLocation = SourceLocation(lineNumber)
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
        
        return ParsedInline(
            element = AttributeReference(
                key = key,
                sourceLocation = SourceLocation(lineNumber)
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
            return ParsedInline(
                element = Text(
                    content = escapedChar.toString(),
                    sourceLocation = SourceLocation(lineNumber)
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