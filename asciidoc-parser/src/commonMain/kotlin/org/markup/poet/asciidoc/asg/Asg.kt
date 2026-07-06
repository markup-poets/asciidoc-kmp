package org.markup.poet.asciidoc.asg

/**
 * ASG (Abstract Semantic Graph) model mirroring the official AsciiDoc schema
 * (asciidoc-lang `asg/schema.json`). The axes of the schema — `name`, `variant`,
 * `form` — are modeled as fields so that new syntax mostly means parser work,
 * not new node classes.
 */
sealed interface AsgNode {
    val location: Location?
}

// ---------------------------------------------------------------------------
// Inlines
// ---------------------------------------------------------------------------

sealed interface Inline : AsgNode

/** Plain text run; multi-line runs keep the `\n` inside [value]. */
data class InlineText(
    val value: String,
    override val location: Location? = null,
) : Inline

enum class SpanVariant(val asgName: String) {
    STRONG("strong"),
    EMPHASIS("emphasis"),
    CODE("code"),
    MARK("mark"),
}

enum class SpanForm(val asgName: String) {
    CONSTRAINED("constrained"),
    UNCONSTRAINED("unconstrained"),
}

/** Formatting span; the location includes the delimiters. */
data class InlineSpan(
    val variant: SpanVariant,
    val form: SpanForm,
    val inlines: List<Inline>,
    override val location: Location? = null,
) : Inline

enum class RefVariant(val asgName: String) {
    LINK("link"),
    XREF("xref"),
}

data class InlineRef(
    val variant: RefVariant,
    val target: String,
    val inlines: List<Inline>,
    override val location: Location? = null,
) : Inline

/**
 * A generic inline macro `name:target[attrlist]`. Not part of the official ASG
 * schema — it exists as an extension seam (WASM plugins claim macros by name);
 * built-in names (link, xref, image) are mapped to their proper nodes downstream.
 */
data class InlineMacro(
    val name: String,
    val target: String,
    val positional: List<String> = emptyList(),
    val named: Map<String, String> = emptyMap(),
    override val location: Location? = null,
) : Inline

// ---------------------------------------------------------------------------
// Blocks
// ---------------------------------------------------------------------------

sealed interface Block : AsgNode

/**
 * Block attribute-line metadata (`[style,pos2,key=value]` above a block).
 * The block style is `positional.firstOrNull()`.
 */
data class BlockMetadata(
    val positional: List<String> = emptyList(),
    val named: Map<String, String> = emptyMap(),
)

enum class LeafBlockName(val asgName: String) {
    PARAGRAPH("paragraph"),
    LISTING("listing"),
    LITERAL("literal"),
    PASS("pass"),
    STEM("stem"),
    VERSE("verse"),
}

enum class LeafBlockForm(val asgName: String) {
    PARAGRAPH("paragraph"),
    DELIMITED("delimited"),
    INDENTED("indented"),
}

/** A block holding inline content (paragraph, listing, literal, ...). */
data class LeafBlock(
    val name: LeafBlockName,
    val form: LeafBlockForm,
    val delimiter: String? = null,
    val inlines: List<Inline>,
    val metadata: BlockMetadata? = null,
    override val location: Location? = null,
) : Block

enum class ParentBlockName(val asgName: String) {
    ADMONITION("admonition"),
    EXAMPLE("example"),
    SIDEBAR("sidebar"),
    OPEN("open"),
    QUOTE("quote"),
}

/** A delimited block containing other blocks (sidebar, example, quote, ...). */
data class ParentBlock(
    val name: ParentBlockName,
    val variant: String? = null,
    val delimiter: String,
    val blocks: List<Block>,
    val metadata: BlockMetadata? = null,
    override val location: Location? = null,
) : Block

/** A section: `==`-style heading plus its nested content. Level 1 = `==`. */
data class SectionBlock(
    val title: List<Inline>,
    val level: Int,
    val blocks: List<Block>,
    override val location: Location? = null,
) : Block

enum class ListVariant(val asgName: String) {
    ORDERED("ordered"),
    UNORDERED("unordered"),
    CALLOUT("callout"),
}

data class ListItem(
    val marker: String,
    val principal: List<Inline>,
    val blocks: List<Block> = emptyList(),
    override val location: Location? = null,
) : AsgNode

data class ListBlock(
    val variant: ListVariant,
    val marker: String,
    val items: List<ListItem>,
    override val location: Location? = null,
) : Block

// ---------------------------------------------------------------------------
// Document
// ---------------------------------------------------------------------------

/** Document header: `= Title` plus the attribute entries directly below it. */
data class Header(
    val title: List<Inline>,
    val location: Location? = null,
)

data class AsgDocument(
    val attributes: Map<String, String> = emptyMap(),
    val header: Header? = null,
    val blocks: List<Block> = emptyList(),
    override val location: Location? = null,
) : AsgNode
