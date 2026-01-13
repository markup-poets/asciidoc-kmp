package io.github.kotlin.asciidoc.ast

/**
 * Base class for all block-level elements in the document.
 * Block elements are structural components like sections, paragraphs, lists, etc.
 */
sealed class BlockElement : AstNode()

/**
 * Represents a section with a heading and nested content.
 */
data class Section(
    val level: Int,
    val title: String,
    val children: List<BlockElement>,
    override val attributes: Map<String, String> = emptyMap(),
    override val sourceLocation: SourceLocation
) : BlockElement()

/**
 * Represents a paragraph containing inline elements.
 */
data class Paragraph(
    val content: List<InlineElement>,
    override val attributes: Map<String, String> = emptyMap(),
    override val sourceLocation: SourceLocation
) : BlockElement()

/**
 * Represents a list (ordered or unordered) with list items.
 */
data class AsciiDocList(
    val type: ListType,
    val items: kotlin.collections.List<ListItem>,
    override val attributes: Map<String, String> = emptyMap(),
    override val sourceLocation: SourceLocation
) : BlockElement()

/**
 * Represents a code block with optional language specification.
 */
data class CodeBlock(
    val language: String?,
    val content: String,
    override val attributes: Map<String, String> = emptyMap(),
    override val sourceLocation: SourceLocation
) : BlockElement()

/**
 * Represents a comment block.
 */
data class Comment(
    val content: String,
    override val attributes: Map<String, String> = emptyMap(),
    override val sourceLocation: SourceLocation
) : BlockElement()

/**
 * Represents a list item within a list.
 */
data class ListItem(
    val marker: String,
    val content: List<InlineElement>,
    val nestedList: AsciiDocList? = null,
    override val attributes: Map<String, String> = emptyMap(),
    override val sourceLocation: SourceLocation
) : AstNode()

/**
 * Enumeration of supported list types.
 */
enum class ListType { 
    UNORDERED, 
    ORDERED, 
    DEFINITION 
}