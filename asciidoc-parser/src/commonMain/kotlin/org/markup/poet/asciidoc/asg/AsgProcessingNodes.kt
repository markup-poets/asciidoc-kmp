package org.markup.poet.asciidoc.asg

/**
 * ASG extension nodes for the post-parse processing phase.
 *
 * None of these are part of the official AsciiDoc ASG schema (`asg/schema.json`).
 * The parser core ([org.markup.poet.asciidoc.parser.asg.BlockTreeParser]) never
 * emits them; they are injected and consumed by the document-processing layer
 * (includes, conditionals, callouts, bibliography, footnotes) and by extension
 * plugins that splice pre-rendered output into the tree. The TCK serialization
 * path (`AsgDocumentJsonSerializer`) therefore never encounters them.
 */

// ---------------------------------------------------------------------------
// Blocks
// ---------------------------------------------------------------------------

/** A comment block (`////` delimited or `//` line run), preserved for round-tripping. */
data class CommentBlock(
    val text: String,
    override val location: Location? = null,
) : Block

/** An unresolved `include::path[]` directive, replaced by [IncludeBlock] resolution. */
data class IncludeBlock(
    val path: String,
    val lineRange: IntRange? = null,
    val attributes: Map<String, String> = emptyMap(),
    override val location: Location? = null,
) : Block

enum class ConditionalVariant { IFDEF, IFNDEF, IFEVAL }

/** An unresolved `ifdef::`/`ifndef::`/`ifeval::` region. */
data class ConditionalBlock(
    val variant: ConditionalVariant,
    val condition: String,
    val blocks: List<Block>,
    val elseBlocks: List<Block> = emptyList(),
    override val location: Location? = null,
) : Block

/** A `[[[id]]]` bibliography entry collected by the bibliography manager. */
data class BibliographyEntryBlock(
    val id: String,
    val citation: String,
    val entryMetadata: Map<String, String> = emptyMap(),
    override val location: Location? = null,
) : Block

/**
 * Raw pre-rendered output spliced into the document by an extension processor.
 * Renderers emit [content] verbatim (no escaping) when the output [format]
 * matches; other formats skip it.
 */
data class RawBlock(
    val format: String = "html",
    val content: String,
    override val location: Location? = null,
) : Block

// ---------------------------------------------------------------------------
// Inlines
// ---------------------------------------------------------------------------

/** An unresolved `{name}` attribute reference awaiting substitution. */
data class InlineAttributeRef(
    val name: String,
    override val location: Location? = null,
) : Inline

/** A callout marker `<1>` inside verbatim content, paired with a callout list item. */
data class InlineCallout(
    val number: Int,
    override val location: Location? = null,
) : Inline

/** A footnote reference; the footnote text is carried inline until rendering. */
data class InlineFootnote(
    val id: String,
    val inlines: List<Inline>,
    override val location: Location? = null,
) : Inline

/** A `<<citation-id>>` bibliography citation resolved against [BibliographyEntryBlock]s. */
data class InlineCitation(
    val citationId: String,
    override val location: Location? = null,
) : Inline

/** Raw pre-rendered inline output spliced in by an extension processor. */
data class InlineRaw(
    val format: String = "html",
    val content: String,
    override val location: Location? = null,
) : Inline
