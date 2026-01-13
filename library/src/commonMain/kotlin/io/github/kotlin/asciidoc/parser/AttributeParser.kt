package io.github.kotlin.asciidoc.parser

import io.github.kotlin.asciidoc.ast.SourceLocation

/**
 * Interface for parsing AsciiDoc document attributes.
 */
interface AttributeParser {
    /**
     * Parse an attribute definition line and extract key-value pair.
     */
    fun parseAttributeDefinition(line: String, lineNumber: Int): AttributeDefinition?
    
    /**
     * Find and mark attribute references in text for later substitution.
     */
    fun findAttributeReferences(text: String): List<AttributeReferenceLocation>
    
    /**
     * Check if a line is an attribute definition.
     */
    fun isAttributeDefinition(line: String): Boolean
}

/**
 * Represents a parsed attribute definition.
 */
data class AttributeDefinition(
    val key: String,
    val value: String,
    val sourceLocation: SourceLocation
)

/**
 * Represents an attribute reference found in text.
 */
data class AttributeReferenceLocation(
    val key: String,
    val startIndex: Int,
    val endIndex: Int,
    val sourceLocation: SourceLocation
)

/**
 * Default implementation of AttributeParser.
 */
class DefaultAttributeParser : AttributeParser {
    
    override fun parseAttributeDefinition(line: String, lineNumber: Int): AttributeDefinition? {
        val trimmed = line.trim()
        
        // Attribute definitions start with ':' and contain at least one more ':'
        if (!trimmed.startsWith(":") || trimmed.length < 3) {
            return null
        }
        
        // Find the second colon that ends the key
        val keyEndIndex = trimmed.indexOf(':', 1)
        if (keyEndIndex == -1) {
            return null
        }
        
        val key = trimmed.substring(1, keyEndIndex)
        val value = trimmed.substring(keyEndIndex + 1).trim()
        
        // Key must not be empty
        if (key.isEmpty()) {
            return null
        }
        
        return AttributeDefinition(
            key = key,
            value = value, // Preserve spaces in value
            sourceLocation = SourceLocation(lineNumber)
        )
    }
    
    override fun findAttributeReferences(text: String): List<AttributeReferenceLocation> {
        val references = mutableListOf<AttributeReferenceLocation>()
        val regex = Regex("\\{([^}]+)\\}")
        
        regex.findAll(text).forEach { match ->
            val key = match.groupValues[1]
            references.add(
                AttributeReferenceLocation(
                    key = key,
                    startIndex = match.range.first,
                    endIndex = match.range.last + 1,
                    sourceLocation = SourceLocation(0) // Line number will be set by caller
                )
            )
        }
        
        return references
    }
    
    override fun isAttributeDefinition(line: String): Boolean {
        val trimmed = line.trim()
        return trimmed.startsWith(":") && trimmed.indexOf(':', 1) != -1
    }
}