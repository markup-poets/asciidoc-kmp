package org.markup.poet.asciidoc.parser

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.Block
import org.markup.poet.asciidoc.asg.Inline
import org.markup.poet.asciidoc.asg.InlineRef
import org.markup.poet.asciidoc.asg.InlineSpan
import org.markup.poet.asciidoc.asg.InlineText
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.LeafBlockName
import org.markup.poet.asciidoc.asg.ListBlock
import org.markup.poet.asciidoc.asg.ListVariant
import org.markup.poet.asciidoc.asg.Location
import org.markup.poet.asciidoc.asg.ParentBlock
import org.markup.poet.asciidoc.asg.RefVariant
import org.markup.poet.asciidoc.asg.SectionBlock
import org.markup.poet.asciidoc.asg.SpanVariant
import org.markup.poet.asciidoc.ast.AsciiDocList
import org.markup.poet.asciidoc.ast.BlockElement
import org.markup.poet.asciidoc.ast.Code
import org.markup.poet.asciidoc.ast.CodeBlock
import org.markup.poet.asciidoc.ast.CrossReference
import org.markup.poet.asciidoc.ast.Document
import org.markup.poet.asciidoc.ast.Emphasis
import org.markup.poet.asciidoc.ast.InlineElement
import org.markup.poet.asciidoc.ast.Link
import org.markup.poet.asciidoc.ast.ListItem
import org.markup.poet.asciidoc.ast.ListType
import org.markup.poet.asciidoc.ast.Paragraph
import org.markup.poet.asciidoc.ast.Section
import org.markup.poet.asciidoc.ast.SourceLocation
import org.markup.poet.asciidoc.ast.Strong
import org.markup.poet.asciidoc.ast.Text

/**
 * One-way mapping from the ASG model ([AsgDocument]) produced by the ASG-native
 * parser core to the legacy AST ([Document]) consumed by `document-processing`
 * and `html-renderer`.
 *
 * The mapping preserves legacy semantics where the two models disagree:
 *
 * - **Section levels**: legacy `Section.level` is the number of `=` characters
 *   (`==` -> 2), while the ASG level is one less (`==` -> 1). The bridge maps
 *   `legacy level = asg level + 1` so `<h{level}>` rendering is unchanged.
 * - **Parent blocks** (sidebar/example/quote/open/admonition): the legacy AST
 *   has no equivalent container, so their child blocks are spliced into the
 *   parent's position. Content is preserved; the container itself is LOSSY.
 * - **Verbatim blocks** (listing/literal/pass/stem/verse): mapped to
 *   [CodeBlock] with `language = null`; the ASG's name/form/delimiter axes are
 *   LOSSY. A `[source,lang]` attribute-line paragraph immediately preceding a
 *   listing block is folded into the CodeBlock's language.
 * - **Mark spans** (`#...#`): the legacy AST has no mark element, so the inner
 *   inlines are spliced in place of the span (delimiters LOSSY).
 * - **Code spans**: legacy [Code] holds a plain string, so nested formatting
 *   inside a code span is flattened to its concatenated text.
 * - **List items**: ASG nested item blocks are dropped except that the legacy
 *   model only carries the principal text (`nestedList` is not synthesized).
 */
object AsgToLegacyAst {

    private val sourceAttributeLine = Regex("""^\[source(?:\s*,\s*([^\]]+))?\]$""")

    fun convert(asg: AsgDocument): Document {
        return Document(
            title = asg.header?.title?.let { plainText(it) },
            children = mapBlocks(asg.blocks),
            documentAttributes = asg.attributes,
            sourceLocation = asg.location.toLegacy(),
        )
    }

    // -----------------------------------------------------------------------
    // Blocks
    // -----------------------------------------------------------------------

    private fun mapBlocks(blocks: List<Block>): List<BlockElement> {
        val result = mutableListOf<BlockElement>()
        var i = 0
        while (i < blocks.size) {
            val block = blocks[i]
            // Fold a `[source,lang]` attribute-line paragraph into the listing
            // block that follows it (legacy code blocks carry the language).
            val language = sourceLanguageOf(block)
            if (language != null && i + 1 < blocks.size) {
                val next = blocks[i + 1]
                if (next is LeafBlock && next.name != LeafBlockName.PARAGRAPH) {
                    result += CodeBlock(
                        language = language.ifEmpty { null },
                        content = plainText(next.inlines),
                        sourceLocation = next.location.toLegacy(),
                    )
                    i += 2
                    continue
                }
            }
            result += mapBlock(block)
            i++
        }
        return result
    }

    /**
     * If [block] is a single-line paragraph of the form `[source]` or
     * `[source,lang]`, returns the language (possibly empty), else null.
     */
    private fun sourceLanguageOf(block: Block): String? {
        if (block !is LeafBlock || block.name != LeafBlockName.PARAGRAPH) return null
        val only = block.inlines.singleOrNull() as? InlineText ?: return null
        if (only.value.contains('\n')) return null
        val match = sourceAttributeLine.matchEntire(only.value) ?: return null
        return match.groupValues[1].trim()
    }

    private fun mapBlock(block: Block): List<BlockElement> = when (block) {
        is SectionBlock -> listOf(
            Section(
                level = block.level + 1, // legacy level == number of '=' chars
                title = plainText(block.title),
                children = mapBlocks(block.blocks),
                sourceLocation = block.location.toLegacy(),
            )
        )

        is LeafBlock -> when (block.name) {
            LeafBlockName.PARAGRAPH -> listOf(
                Paragraph(
                    content = mapInlines(block.inlines),
                    sourceLocation = block.location.toLegacy(),
                )
            )
            // Listing/literal/pass/stem/verse all map to the legacy CodeBlock;
            // the distinction between them is lost.
            LeafBlockName.LISTING,
            LeafBlockName.LITERAL,
            LeafBlockName.PASS,
            LeafBlockName.STEM,
            LeafBlockName.VERSE,
            -> listOf(
                CodeBlock(
                    language = null,
                    content = plainText(block.inlines),
                    sourceLocation = block.location.toLegacy(),
                )
            )
        }

        // The legacy AST has no sidebar/example/quote/open container: splice
        // the mapped children into the parent's position (container is lost).
        is ParentBlock -> mapBlocks(block.blocks)

        is ListBlock -> listOf(
            AsciiDocList(
                type = if (block.variant == ListVariant.ORDERED) ListType.ORDERED else ListType.UNORDERED,
                items = block.items.map { item ->
                    ListItem(
                        marker = item.marker,
                        content = mapInlines(item.principal),
                        nestedList = null,
                        sourceLocation = item.location.toLegacy(),
                    )
                },
                sourceLocation = block.location.toLegacy(),
            )
        )
    }

    // -----------------------------------------------------------------------
    // Inlines
    // -----------------------------------------------------------------------

    private fun mapInlines(inlines: List<Inline>): List<InlineElement> =
        inlines.flatMap { mapInline(it) }

    private fun mapInline(inline: Inline): List<InlineElement> = when (inline) {
        is InlineText -> listOf(
            Text(content = inline.value, sourceLocation = inline.location.toLegacy())
        )

        is InlineSpan -> when (inline.variant) {
            SpanVariant.STRONG -> listOf(
                Strong(content = mapInlines(inline.inlines), sourceLocation = inline.location.toLegacy())
            )
            SpanVariant.EMPHASIS -> listOf(
                Emphasis(content = mapInlines(inline.inlines), sourceLocation = inline.location.toLegacy())
            )
            SpanVariant.CODE -> listOf(
                Code(content = plainText(inline.inlines), sourceLocation = inline.location.toLegacy())
            )
            // Legacy AST has no mark element: splice the inner inlines.
            SpanVariant.MARK -> mapInlines(inline.inlines)
        }

        is InlineRef -> when (inline.variant) {
            RefVariant.LINK -> listOf(
                Link(
                    url = inline.target,
                    text = plainText(inline.inlines),
                    sourceLocation = inline.location.toLegacy(),
                )
            )
            RefVariant.XREF -> listOf(
                CrossReference(
                    targetId = inline.target,
                    customText = plainText(inline.inlines).ifEmpty { null },
                    sourceLocation = inline.location.toLegacy(),
                )
            )
        }
    }

    /** Concatenated plain-text value of [inlines], recursing into spans/refs. */
    private fun plainText(inlines: List<Inline>): String = buildString {
        fun visit(inline: Inline) {
            when (inline) {
                is InlineText -> append(inline.value)
                is InlineSpan -> inline.inlines.forEach(::visit)
                is InlineRef -> inline.inlines.forEach(::visit)
            }
        }
        inlines.forEach(::visit)
    }

    // -----------------------------------------------------------------------
    // Locations
    // -----------------------------------------------------------------------

    private fun Location?.toLegacy(): SourceLocation =
        if (this == null) SourceLocation(1, 1, 1, 1)
        else SourceLocation(
            line = start.line,
            column = start.col,
            endLine = end.line,
            endColumn = end.col,
        )
}
