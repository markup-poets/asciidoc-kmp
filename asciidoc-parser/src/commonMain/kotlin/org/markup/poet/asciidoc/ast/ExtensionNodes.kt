package org.markup.poet.asciidoc.ast

/**
 * A block whose style (`[name]` attribute line) is not a built-in AsciiDoc
 * style. Preserved verbatim so extension processors (e.g. WASM plugins) can
 * claim and replace it; without a processor it renders as a listing fallback.
 *
 * @param name the block style, i.e. the first positional attribute
 * @param rawContent the block's raw text content
 * @param attributes positional attributes keyed by 1-based index plus named ones
 */
data class CustomBlock(
    val name: String,
    val rawContent: String,
    override val attributes: Map<String, String> = emptyMap(),
    override val sourceLocation: SourceLocation,
) : BlockElement()

/**
 * Raw pre-rendered output spliced into the document by an extension processor.
 * Renderers emit [content] verbatim (no escaping) when the output [format]
 * matches; other formats skip it.
 */
data class PassthroughBlock(
    val format: String = "html",
    val content: String,
    override val attributes: Map<String, String> = emptyMap(),
    override val sourceLocation: SourceLocation,
) : BlockElement()

/**
 * Raw pre-rendered inline output spliced in by an extension processor (e.g. an
 * inline-macro plugin). Emitted verbatim when the output [format] matches.
 */
data class RawInline(
    val format: String = "html",
    val content: String,
    override val attributes: Map<String, String> = emptyMap(),
    override val sourceLocation: SourceLocation,
) : InlineElement()
