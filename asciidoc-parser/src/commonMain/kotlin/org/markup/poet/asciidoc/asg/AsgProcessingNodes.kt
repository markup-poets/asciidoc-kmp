package org.markup.poet.asciidoc.asg

/**
 * ASG extension nodes for the post-parse processing phase.
 *
 * None of these are part of the official AsciiDoc ASG schema (`asg/schema.json`).
 * The parser core ([org.markup.poet.asciidoc.parser.asg.BlockTreeParser]) emits
 * [IncludeBlock], [ConditionalBlock], [CustomBlockMacro], and the table nodes for
 * the corresponding source constructs; the rest are injected and consumed by the
 * document-processing layer (callouts, bibliography, footnotes) and by extension
 * plugins that splice pre-rendered output into the tree. The TCK serialization
 * path (`AsgDocumentJsonSerializer`) never encounters any of them (no TCK
 * fixture contains these constructs; it fails loudly if one ever does).
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

/**
 * A block macro `name::target[attrs]` whose name is not one of the built-in
 * [BlockMacroName]s (and not a processing directive). Unlike the other nodes
 * in this file it IS emitted by the parser core — the official schema has no
 * generic block-macro node, so it exists as an extension seam: WASM plugins
 * claim it by name (`blockMacro` capability); unclaimed macros render as
 * nothing plus a warning.
 */
data class CustomBlockMacro(
    val name: String,
    val target: String?,
    val metadata: BlockMetadata? = null,
    override val location: Location? = null,
) : Block

// ---------------------------------------------------------------------------
// Tables (extension)
// ---------------------------------------------------------------------------

/** Horizontal cell alignment from a `cols` spec (`<` / `^` / `>`). */
enum class TableColumnAlignment(val asgName: String) {
    LEFT("left"),
    CENTER("center"),
    RIGHT("right"),
}

/** One column definition from the `cols` attribute (alignment + relative width). */
data class TableColumn(
    val alignment: TableColumnAlignment = TableColumnAlignment.LEFT,
    val width: Int = 1,
)

/** One table cell; [colSpan] > 1 comes from a `N+|` cell spec. */
data class TableCell(
    val inlines: List<Inline>,
    val colSpan: Int = 1,
    val rowSpan: Int = 1,
    override val location: Location? = null,
) : AsgNode

/** One table row (a full set of cells covering the table's columns). */
data class TableRow(
    val cells: List<TableCell>,
    override val location: Location? = null,
) : AsgNode

/**
 * A `|===` delimited table. Like [CustomBlockMacro] this IS emitted by the
 * parser core, but the official ASG schema (`asg/schema.json` draft-01) defines
 * no table node — its block union stops at list/dlist/discreteHeading/break/
 * blockMacro/leafBlock/parentBlock — so it lives here as an extension node and
 * the TCK serializer refuses it until the spec pins a shape down.
 *
 * [columns] comes from the `cols` attribute when present, otherwise it is
 * derived from the first row (all LEFT, width 1). [header] is set for an
 * explicit `%header`/`options="header"` table or the implicit form (first row
 * on the delimiter-adjacent line followed by a blank line).
 */
data class TableBlock(
    val columns: List<TableColumn>,
    val header: TableRow? = null,
    val rows: List<TableRow>,
    val metadata: BlockMetadata? = null,
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
