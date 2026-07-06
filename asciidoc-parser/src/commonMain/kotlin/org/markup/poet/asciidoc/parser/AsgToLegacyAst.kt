package org.markup.poet.asciidoc.parser

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.BibliographyEntryBlock
import org.markup.poet.asciidoc.asg.Block
import org.markup.poet.asciidoc.asg.BlockMacro
import org.markup.poet.asciidoc.asg.BlockMacroName
import org.markup.poet.asciidoc.asg.BlockMetadata
import org.markup.poet.asciidoc.asg.BreakBlock
import org.markup.poet.asciidoc.asg.BreakVariant
import org.markup.poet.asciidoc.asg.CommentBlock
import org.markup.poet.asciidoc.asg.ConditionalBlock
import org.markup.poet.asciidoc.asg.DListBlock
import org.markup.poet.asciidoc.asg.DiscreteHeading
import org.markup.poet.asciidoc.asg.IncludeBlock
import org.markup.poet.asciidoc.asg.Inline
import org.markup.poet.asciidoc.asg.InlineAttributeRef
import org.markup.poet.asciidoc.asg.InlineCallout
import org.markup.poet.asciidoc.asg.InlineCitation
import org.markup.poet.asciidoc.asg.InlineFootnote
import org.markup.poet.asciidoc.asg.InlineMacro
import org.markup.poet.asciidoc.asg.InlineRaw
import org.markup.poet.asciidoc.asg.InlineRef
import org.markup.poet.asciidoc.asg.InlineSpan
import org.markup.poet.asciidoc.asg.InlineText
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.LeafBlockName
import org.markup.poet.asciidoc.asg.ListBlock
import org.markup.poet.asciidoc.asg.ListVariant
import org.markup.poet.asciidoc.asg.Location
import org.markup.poet.asciidoc.asg.ParentBlock
import org.markup.poet.asciidoc.asg.ParentBlockName
import org.markup.poet.asciidoc.asg.RawBlock
import org.markup.poet.asciidoc.asg.RefVariant
import org.markup.poet.asciidoc.asg.SectionBlock
import org.markup.poet.asciidoc.asg.SpanVariant
import org.markup.poet.asciidoc.ast.AdmonitionBlock
import org.markup.poet.asciidoc.ast.AdmonitionType
import org.markup.poet.asciidoc.ast.AsciiDocList
import org.markup.poet.asciidoc.ast.AttributeReference
import org.markup.poet.asciidoc.ast.BibliographyEntry
import org.markup.poet.asciidoc.ast.BibliographyReference
import org.markup.poet.asciidoc.ast.BlockElement
import org.markup.poet.asciidoc.ast.Callout
import org.markup.poet.asciidoc.ast.Code
import org.markup.poet.asciidoc.ast.CodeBlock
import org.markup.poet.asciidoc.ast.Comment
import org.markup.poet.asciidoc.ast.ConditionalDirective
import org.markup.poet.asciidoc.ast.ConditionalType
import org.markup.poet.asciidoc.ast.CrossReference
import org.markup.poet.asciidoc.ast.CustomBlock
import org.markup.poet.asciidoc.ast.Document
import org.markup.poet.asciidoc.ast.Emphasis
import org.markup.poet.asciidoc.ast.FootnoteReference
import org.markup.poet.asciidoc.ast.Image
import org.markup.poet.asciidoc.ast.IncludeDirective
import org.markup.poet.asciidoc.ast.InlineElement
import org.markup.poet.asciidoc.ast.Link
import org.markup.poet.asciidoc.ast.MacroInvocation
import org.markup.poet.asciidoc.ast.ListItem
import org.markup.poet.asciidoc.ast.ListType
import org.markup.poet.asciidoc.ast.Paragraph
import org.markup.poet.asciidoc.ast.PassthroughBlock
import org.markup.poet.asciidoc.ast.RawInline
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
 *   [CodeBlock]; the ASG's name/form/delimiter axes are LOSSY. `[source,lang]`
 *   block metadata supplies the CodeBlock's language. Blocks whose style is not
 *   a built-in one become [CustomBlock] for extension processors to claim.
 * - **Mark spans** (`#...#`): the legacy AST has no mark element, so the inner
 *   inlines are spliced in place of the span (delimiters LOSSY).
 * - **Code spans**: legacy [Code] holds a plain string, so nested formatting
 *   inside a code span is flattened to its concatenated text.
 * - **List items**: ASG nested item blocks are dropped except that the legacy
 *   model only carries the principal text (`nestedList` is not synthesized).
 */
object AsgToLegacyAst {

    /** Built-in block styles that keep their standard mapping. */
    private val builtInStyles = org.markup.poet.asciidoc.asg.builtInBlockStyles

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

    private fun mapBlocks(blocks: List<Block>): List<BlockElement> =
        blocks.flatMap { mapBlock(it) }

    /** Attributes map for [CustomBlock]: positional by 1-based index plus named. */
    private fun BlockMetadata.toAttributeMap(): Map<String, String> = buildMap {
        positional.forEachIndexed { index, value -> put((index + 1).toString(), value) }
        putAll(named)
    }

    private fun mapBlock(block: Block): List<BlockElement> = when (block) {
        is SectionBlock -> listOf(
            Section(
                level = block.level + 1, // legacy level == number of '=' chars
                title = plainText(block.title),
                children = mapBlocks(block.blocks),
                attributes = buildMap { block.metadata?.id?.let { put("id", it) } },
                sourceLocation = block.location.toLegacy(),
            )
        )

        is LeafBlock -> {
            val style = block.metadata?.positional?.firstOrNull()
            when {
                // A non-built-in style claims the block for extension processors.
                style != null && style !in builtInStyles -> listOf(
                    CustomBlock(
                        name = style,
                        rawContent = plainText(block.inlines),
                        attributes = block.metadata.toAttributeMap(),
                        sourceLocation = block.location.toLegacy(),
                    )
                )
                block.name == LeafBlockName.PARAGRAPH -> listOf(
                    Paragraph(
                        content = mapInlines(block.inlines),
                        sourceLocation = block.location.toLegacy(),
                    )
                )
                // Listing/literal/pass/stem/verse all map to the legacy CodeBlock;
                // the distinction between them is lost. `[source,lang]` metadata
                // supplies the language.
                else -> listOf(
                    CodeBlock(
                        language = if (style == "source") {
                            block.metadata.positional.getOrNull(1) ?: block.metadata.named["language"]
                        } else {
                            null
                        },
                        content = plainText(block.inlines),
                        sourceLocation = block.location.toLegacy(),
                    )
                )
            }
        }

        // Admonitions map to the legacy AdmonitionBlock; other parent containers
        // (sidebar/example/quote/open) have no legacy equivalent, so their
        // mapped children are spliced into the parent's position (container lost).
        is ParentBlock -> if (block.name == ParentBlockName.ADMONITION) {
            listOf(
                AdmonitionBlock(
                    type = AdmonitionType.valueOf((block.variant ?: "note").uppercase()),
                    title = block.metadata?.title?.let { plainText(it) },
                    content = mapBlocks(block.blocks),
                    sourceLocation = block.location.toLegacy(),
                )
            )
        } else {
            mapBlocks(block.blocks)
        }

        is ListBlock -> listOf(
            AsciiDocList(
                type = if (block.variant == ListVariant.ORDERED) ListType.ORDERED else ListType.UNORDERED,
                items = block.items.map { item ->
                    ListItem(
                        marker = item.marker,
                        content = mapInlines(item.principal),
                        nestedList = item.blocks.filterIsInstance<ListBlock>().firstOrNull()?.let { nested ->
                            mapBlock(nested).filterIsInstance<AsciiDocList>().firstOrNull()
                        },
                        sourceLocation = item.location.toLegacy(),
                    )
                },
                sourceLocation = block.location.toLegacy(),
            )
        )

        // Legacy DEFINITION lists were never fully modeled: terms and principal
        // are joined into the item content with a separator.
        is DListBlock -> listOf(
            AsciiDocList(
                type = ListType.DEFINITION,
                items = block.items.map { item ->
                    val content = mutableListOf<InlineElement>()
                    item.terms.forEach { content += mapInlines(it) }
                    if (item.principal.isNotEmpty()) {
                        content += Text(content = ": ", sourceLocation = item.location.toLegacy())
                        content += mapInlines(item.principal)
                    }
                    ListItem(
                        marker = item.marker,
                        content = content,
                        nestedList = null,
                        sourceLocation = item.location.toLegacy(),
                    )
                },
                sourceLocation = block.location.toLegacy(),
            )
        )

        // The legacy AST has no break node: render-ready passthrough.
        is BreakBlock -> listOf(
            PassthroughBlock(
                format = "html",
                content = if (block.variant == BreakVariant.PAGE) {
                    "<div style=\"page-break-after: always;\"></div>"
                } else {
                    "<hr/>"
                },
                sourceLocation = block.location.toLegacy(),
            )
        )

        is BlockMacro -> when (block.name) {
            BlockMacroName.IMAGE -> listOf(
                Paragraph(
                    content = listOf(
                        Image(
                            path = block.target ?: "",
                            altText = block.metadata?.positional?.firstOrNull() ?: "",
                            sourceLocation = block.location.toLegacy(),
                        ),
                    ),
                    sourceLocation = block.location.toLegacy(),
                )
            )
            else -> listOf(
                Paragraph(
                    content = listOf(
                        MacroInvocation(
                            macroName = block.name.asgName,
                            parameters = buildMap {
                                block.target?.let { put("target", it) }
                                block.metadata?.let { putAll(it.toAttributeMap()) }
                            },
                            isBlock = true,
                            sourceLocation = block.location.toLegacy(),
                        ),
                    ),
                    sourceLocation = block.location.toLegacy(),
                )
            )
        }

        // Discrete headings render like section titles but open no section.
        is DiscreteHeading -> listOf(
            Section(
                level = block.level + 1,
                title = plainText(block.title),
                children = emptyList(),
                sourceLocation = block.location.toLegacy(),
            )
        )

        // Processing-phase extension nodes (never produced by the parser core)
        // map 1:1 onto their legacy counterparts.
        is CommentBlock -> listOf(
            Comment(content = block.text, sourceLocation = block.location.toLegacy())
        )
        is IncludeBlock -> listOf(
            IncludeDirective(
                path = block.path,
                lineRange = block.lineRange,
                attributes = block.attributes,
                sourceLocation = block.location.toLegacy(),
            )
        )
        is ConditionalBlock -> listOf(
            ConditionalDirective(
                type = ConditionalType.valueOf(block.variant.name),
                condition = block.condition,
                content = mapBlocks(block.blocks),
                elseContent = mapBlocks(block.elseBlocks),
                sourceLocation = block.location.toLegacy(),
            )
        )
        is BibliographyEntryBlock -> listOf(
            BibliographyEntry(
                id = block.id,
                citation = block.citation,
                metadata = block.entryMetadata,
                sourceLocation = block.location.toLegacy(),
            )
        )
        is RawBlock -> listOf(
            PassthroughBlock(
                format = block.format,
                content = block.content,
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

        is InlineMacro -> when (inline.name) {
            "link" -> listOf(
                Link(
                    url = inline.target,
                    text = inline.positional.firstOrNull() ?: inline.target,
                    sourceLocation = inline.location.toLegacy(),
                )
            )
            "image" -> listOf(
                Image(
                    path = inline.target,
                    altText = inline.positional.firstOrNull() ?: "",
                    sourceLocation = inline.location.toLegacy(),
                )
            )
            "xref" -> listOf(
                CrossReference(
                    targetId = inline.target,
                    customText = inline.positional.firstOrNull(),
                    sourceLocation = inline.location.toLegacy(),
                )
            )
            else -> listOf(
                MacroInvocation(
                    macroName = inline.name,
                    parameters = buildMap {
                        put("target", inline.target)
                        inline.positional.forEachIndexed { index, value -> put((index + 1).toString(), value) }
                        putAll(inline.named)
                    },
                    isBlock = false,
                    sourceLocation = inline.location.toLegacy(),
                )
            )
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

        // Processing-phase extension nodes map 1:1 onto their legacy counterparts.
        is InlineAttributeRef -> listOf(
            AttributeReference(key = inline.name, sourceLocation = inline.location.toLegacy())
        )
        is InlineCallout -> listOf(
            Callout(number = inline.number, sourceLocation = inline.location.toLegacy())
        )
        is InlineFootnote -> listOf(
            FootnoteReference(
                id = inline.id,
                content = mapInlines(inline.inlines),
                sourceLocation = inline.location.toLegacy(),
            )
        )
        is InlineCitation -> listOf(
            BibliographyReference(citationId = inline.citationId, sourceLocation = inline.location.toLegacy())
        )
        is InlineRaw -> listOf(
            RawInline(
                format = inline.format,
                content = inline.content,
                sourceLocation = inline.location.toLegacy(),
            )
        )
    }

    /** Concatenated plain-text value of [inlines], recursing into spans/refs. */
    private fun plainText(inlines: List<Inline>): String = buildString {
        fun visit(inline: Inline) {
            when (inline) {
                is InlineText -> append(inline.value)
                is InlineSpan -> inline.inlines.forEach(::visit)
                is InlineRef -> inline.inlines.forEach(::visit)
                is InlineMacro -> append("${inline.name}:${inline.target}[]")
                is InlineAttributeRef -> append("{${inline.name}}")
                is InlineFootnote -> inline.inlines.forEach(::visit)
                is InlineCitation -> append(inline.citationId)
                is InlineCallout, is InlineRaw -> Unit
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
