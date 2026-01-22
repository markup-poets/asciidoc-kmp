package org.markup.poet.asciidoc.ast

/**
 * Base class for all inline elements within text content.
 * Inline elements are text-level markup like emphasis, strong text, code spans, etc.
 */
sealed class InlineElement : AstNode()

/**
 * Represents plain text content.
 */
data class Text(
    val content: String,
    override val attributes: Map<String, String> = emptyMap(),
    override val sourceLocation: SourceLocation
) : InlineElement()

/**
 * Represents strong (bold) text formatting.
 */
data class Strong(
    val content: List<InlineElement>,
    override val attributes: Map<String, String> = emptyMap(),
    override val sourceLocation: SourceLocation
) : InlineElement()

/**
 * Represents emphasized (italic) text formatting.
 */
data class Emphasis(
    val content: List<InlineElement>,
    override val attributes: Map<String, String> = emptyMap(),
    override val sourceLocation: SourceLocation
) : InlineElement()

/**
 * Represents inline code formatting.
 */
data class Code(
    val content: String,
    override val attributes: Map<String, String> = emptyMap(),
    override val sourceLocation: SourceLocation
) : InlineElement()

/**
 * Represents a hyperlink with URL and display text.
 */
data class Link(
    val url: String,
    val text: String,
    override val attributes: Map<String, String> = emptyMap(),
    override val sourceLocation: SourceLocation
) : InlineElement()

/**
 * Represents an image with path and alternative text.
 */
data class Image(
    val path: String,
    val altText: String,
    override val attributes: Map<String, String> = emptyMap(),
    override val sourceLocation: SourceLocation
) : InlineElement()

/**
 * Represents an attribute reference that will be substituted later.
 */
data class AttributeReference(
    val key: String,
    override val attributes: Map<String, String> = emptyMap(),
    override val sourceLocation: SourceLocation
) : InlineElement()

/**
 * Represents a callout number (e.g., <1>) within text or code.
 */
data class Callout(
    val number: Int,
    override val attributes: Map<String, String> = emptyMap(),
    override val sourceLocation: SourceLocation
) : InlineElement()

/**
 * Represents a cross-reference to another element in the document.
 */
data class CrossReference(
    val targetId: String,
    val customText: String? = null,
    override val attributes: Map<String, String> = emptyMap(),
    override val sourceLocation: SourceLocation
) : InlineElement()

/**
 * Represents a macro invocation that will be expanded during processing.
 */
data class MacroInvocation(
    val macroName: String,
    val parameters: Map<String, String>,
    val isBlock: Boolean,
    override val attributes: Map<String, String> = emptyMap(),
    override val sourceLocation: SourceLocation
) : InlineElement()

/**
 * Represents a footnote reference in the document.
 * The footnote content is stored separately and referenced by ID.
 */
data class FootnoteReference(
    val id: String,
    val content: List<InlineElement>,
    override val attributes: Map<String, String> = emptyMap(),
    override val sourceLocation: SourceLocation
) : InlineElement()

/**
 * Represents a bibliography citation reference.
 */
data class BibliographyReference(
    val citationId: String,
    override val attributes: Map<String, String> = emptyMap(),
    override val sourceLocation: SourceLocation
) : InlineElement()