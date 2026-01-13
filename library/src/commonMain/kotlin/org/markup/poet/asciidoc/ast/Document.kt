package org.markup.poet.asciidoc.ast

/**
 * Root document node that contains all other elements.
 * Represents the complete parsed AsciiDoc document.
 */
data class Document(
    val title: String?,
    val children: List<BlockElement>,
    val documentAttributes: Map<String, String>,
    override val attributes: Map<String, String> = emptyMap(),
    override val sourceLocation: SourceLocation
) : BlockElement() {
    
    /**
     * Get an attribute value by key.
     */
    fun getAttribute(key: String): String? = documentAttributes[key]
    
    /**
     * Check if an attribute is defined.
     */
    fun hasAttribute(key: String): Boolean = documentAttributes.containsKey(key)
    
    /**
     * Create a new Document with an additional attribute.
     * If the attribute already exists, the new value replaces the old one (last value wins).
     */
    fun withAttribute(key: String, value: String): Document {
        val updatedAttributes = documentAttributes.toMutableMap()
        updatedAttributes[key] = value
        return copy(documentAttributes = updatedAttributes)
    }
    
    /**
     * Create a new Document with multiple additional attributes.
     * For duplicate keys, the new values replace the old ones (last value wins).
     */
    fun withAttributes(newAttributes: Map<String, String>): Document {
        val updatedAttributes = documentAttributes.toMutableMap()
        updatedAttributes.putAll(newAttributes)
        return copy(documentAttributes = updatedAttributes)
    }
}