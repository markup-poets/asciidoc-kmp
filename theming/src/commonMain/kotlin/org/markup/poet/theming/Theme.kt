package org.markup.poet.theming

/**
 * Core theming abstraction for document rendering.
 * 
 * This interface provides complete separation between document structure (data)
 * and visual presentation (styling). Themes can be used across different output
 * formats (HTML, PDF, etc.) by providing format-specific implementations.
 * 
 * Design Principles:
 * - **Data/Presentation Separation**: Themes only define how things look, not what they are
 * - **Format Agnostic**: Same theme concept works for HTML, PDF, or other formats
 * - **Pluggable**: Easy to create and register custom themes
 * - **Composable**: Themes can extend or override other themes
 */
interface Theme {
    /**
     * Unique identifier for this theme.
     * Used for theme registration and selection.
     */
    val id: String
    
    /**
     * Human-readable name for this theme.
     */
    val name: String
    
    /**
     * Optional description of the theme's visual style.
     */
    val description: String?
        get() = null
    
    /**
     * Returns styling information for a specific element type.
     * 
     * @param element The element type to style
     * @param context Additional context for styling decisions
     * @return Styling information for the element
     */
    fun getStyle(element: ElementType, context: StyleContext = StyleContext.empty()): ElementStyle
    
    /**
     * Returns the complete stylesheet for this theme in the target format.
     * 
     * @param format The output format (e.g., "css", "pdf-styles")
     * @return Stylesheet content as a string, or null if format not supported
     */
    fun getStylesheet(format: String): String?
}

/**
 * Types of elements that can be styled.
 * 
 * This enum represents the semantic structure of documents,
 * independent of any specific output format.
 */
enum class ElementType {
    // Document structure
    DOCUMENT,
    SECTION,
    
    // Block elements
    HEADING,
    PARAGRAPH,
    CODE_BLOCK,
    QUOTE,
    LIST,
    LIST_ITEM,
    TABLE,
    TABLE_HEADER,
    TABLE_ROW,
    TABLE_CELL,
    
    // Inline elements
    TEXT,
    EMPHASIS,
    STRONG,
    CODE,
    LINK,
    
    // Special blocks
    ADMONITION,
    SIDEBAR,
    EXAMPLE,
    LITERAL,
    VERSE,
    
    // Media
    IMAGE,
    VIDEO,
    
    // Metadata
    AUTHOR,
    DATE,
    REVISION
}

/**
 * Context information for styling decisions.
 * 
 * Provides additional information that may affect how an element is styled,
 * such as nesting level, element attributes, or parent context.
 */
data class StyleContext(
    val level: Int? = null,
    val type: String? = null,
    val attributes: Map<String, String> = emptyMap(),
    val parent: ElementType? = null,
    val index: Int? = null
) {
    companion object {
        fun empty() = StyleContext()
        
        fun heading(level: Int) = StyleContext(level = level)
        
        fun admonition(type: String) = StyleContext(type = type)
        
        fun withAttributes(attributes: Map<String, String>) = StyleContext(attributes = attributes)
    }
}

/**
 * Styling information for an element.
 * 
 * This is format-agnostic styling data that can be translated to
 * format-specific output (CSS classes, PDF styles, etc.).
 */
data class ElementStyle(
    val classes: List<String> = emptyList(),
    val properties: Map<String, String> = emptyMap(),
    val customData: Map<String, Any> = emptyMap()
) {
    companion object {
        fun empty() = ElementStyle()
        
        fun withClasses(vararg classes: String) = ElementStyle(classes = classes.toList())
        
        fun withProperties(properties: Map<String, String>) = ElementStyle(properties = properties)
    }
}
