package org.markup.poet.asciidoc.render

import org.markup.poet.asciidoc.asg.BibliographyEntryBlock
import org.markup.poet.asciidoc.asg.Block
import org.markup.poet.asciidoc.asg.BlockMacro
import org.markup.poet.asciidoc.asg.BlockMacroName
import org.markup.poet.asciidoc.asg.BlockMetadata
import org.markup.poet.asciidoc.asg.BreakBlock
import org.markup.poet.asciidoc.asg.BreakVariant
import org.markup.poet.asciidoc.asg.CommentBlock
import org.markup.poet.asciidoc.asg.ConditionalBlock
import org.markup.poet.asciidoc.asg.CustomBlockMacro
import org.markup.poet.asciidoc.asg.DListBlock
import org.markup.poet.asciidoc.asg.DiscreteHeading
import org.markup.poet.asciidoc.asg.IncludeBlock
import org.markup.poet.asciidoc.asg.Inline
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.LeafBlockName
import org.markup.poet.asciidoc.asg.ListBlock
import org.markup.poet.asciidoc.asg.ListVariant
import org.markup.poet.asciidoc.asg.ParentBlock
import org.markup.poet.asciidoc.asg.ParentBlockName
import org.markup.poet.asciidoc.asg.RawBlock
import org.markup.poet.asciidoc.asg.SectionBlock
import org.markup.poet.asciidoc.asg.TableBlock
import org.markup.poet.asciidoc.asg.TableColumnAlignment
import org.markup.poet.asciidoc.asg.TableRow
import org.markup.poet.asciidoc.asg.builtInBlockStyles
import org.markup.poet.asciidoc.asg.plainText

/**
 * Renders ASG block nodes to HTML.
 *
 * BlockRenderer handles structural components like sections, paragraphs, lists,
 * verbatim blocks, and block containers. It processes block nodes and their
 * nested content to produce semantic HTML5 output.
 *
 * Implementations must ensure:
 * - Block nodes are converted to appropriate semantic HTML5 elements
 * - Nested content (inlines, nested blocks) is rendered correctly
 * - CSS classes from the theme are applied consistently
 * - Block metadata (id, roles, title) is reflected in the output
 * - IDs are generated for elements that need them (e.g., headings)
 */
interface BlockRenderer {
    /**
     * Renders a block node to HTML.
     *
     * @param block The block node to render
     * @param context The rendering context containing configuration and state
     * @return The HTML string representation of the block node
     */
    fun render(block: Block, context: RenderContext): String
}

/**
 * Default implementation of BlockRenderer that produces semantic HTML5 output.
 *
 * This implementation:
 * - Converts SectionBlock nodes to `<section>` elements with `<h1>`-`<h6>` headings
 *   (ASG level 1 == `==` renders as `<h2>`)
 * - Converts LeafBlock paragraphs to `<p>`, listing/literal/stem to `<pre><code>`,
 *   pass to verbatim output, and verse to a `verseblock` `<pre>`
 * - Converts ParentBlock containers to their conventional structures
 *   (admonitionblock, exampleblock, sidebarblock, openblock, quoteblock)
 * - Converts ListBlock nodes to `<ul>`/`<ol>` (callout lists use `callout-list`
 *   styling) and DListBlock nodes to `<dl>`/`<dt>`/`<dd>`
 * - Applies metadata: `id` becomes the element id, roles become extra CSS
 *   classes, and `.Title` lines render as a block title `<div>`
 * - Renders processing-phase extension nodes (raw output, bibliography entries)
 *   and skips processing residue (comments, unresolved directives)
 *
 * @param builder The HtmlBuilder to use for constructing HTML
 * @param inlineRenderer The InlineRenderer to use for rendering inline content
 */
class DefaultBlockRenderer(
    private val builder: HtmlBuilder,
    private val inlineRenderer: InlineRenderer
) : BlockRenderer {

    override fun render(block: Block, context: RenderContext): String {
        return when (block) {
            is SectionBlock -> renderSection(block, context)
            is LeafBlock -> renderLeafBlock(block, context)
            is ParentBlock -> renderParentBlock(block, context)
            is ListBlock -> renderList(block, context)
            is DListBlock -> renderDList(block, context)
            is BreakBlock -> renderBreak(block)
            is BlockMacro -> renderBlockMacro(block, context)
            is DiscreteHeading -> renderDiscreteHeading(block, context)
            is TableBlock -> renderTable(block, context)
            is BibliographyEntryBlock -> renderBibliographyEntry(block)
            is RawBlock -> if (block.format == "html") block.content else ""
            // Comments never render; unresolved include/conditional directives
            // are processing residue and produce no output.
            is CommentBlock -> ""
            is IncludeBlock -> {
                context.logWarning("Unresolved include directive: ${block.path}")
                ""
            }
            is ConditionalBlock -> {
                context.logWarning("Unresolved conditional directive: ${block.condition}")
                ""
            }
            // A custom block macro no extension processor claimed: like the
            // unresolved directives above it produces no output, only a warning.
            is CustomBlockMacro -> {
                context.logWarning("Unclaimed block macro: ${block.name}")
                ""
            }
        }
    }

    // -----------------------------------------------------------------------
    // Sections and headings
    // -----------------------------------------------------------------------

    /**
     * Renders a section with heading and nested content.
     *
     * Sections are rendered as `<section>` elements containing a heading
     * (`<h1>` through `<h6>`) with a unique ID plus the nested blocks. The
     * explicit block id (`[#id]`) wins over the title-derived generated id.
     */
    private fun renderSection(section: SectionBlock, context: RenderContext): String {
        // ASG level 1 == `==`, which renders as <h2>.
        val level = (section.level + 1).coerceIn(1, 6)
        val titleText = plainText(section.title)
        val id = section.metadata?.id ?: context.generateId(titleText)

        // Register heading for TOC and hierarchy validation
        context.registerHeading(level, id, titleText)

        val headingClasses = withRoles(context.theme.headingClasses(level), section.metadata)

        // Process attribute handlers
        val handlerAttrs = context.processAttributeHandlers(section)
        val headingAttrs = mutableMapOf("id" to id, "class" to headingClasses)
        headingAttrs.putAll(handlerAttrs)

        // Rendered before build {} — the shared builder is not re-entrant.
        val title = renderInlineContent(section.title, context)
        val heading = builder.build {
            openTag("h$level", headingAttrs)
            text(title)
            closeTag("h$level")
        }

        val children = section.blocks.joinToString("\n") { render(it, context) }

        return builder.build {
            openTag("section", mapOf("id" to "$id-section"))
            text(heading)
            if (children.isNotEmpty()) {
                text("\n")
                text(children)
            }
            closeTag("section")
        }
    }

    /**
     * Renders a `[discrete]` heading: an `<h1>`-`<h6>` element styled like a
     * section title but without a `<section>` wrapper or TOC registration.
     */
    private fun renderDiscreteHeading(heading: DiscreteHeading, context: RenderContext): String {
        val level = (heading.level + 1).coerceIn(1, 6)
        val id = heading.metadata?.id ?: context.generateId(plainText(heading.title))
        val classes = withRoles("${context.theme.headingClasses(level)} discrete", heading.metadata)

        val title = renderInlineContent(heading.title, context)
        return builder.build {
            openTag("h$level", mapOf("id" to id, "class" to classes))
            text(title)
            closeTag("h$level")
        }
    }

    // -----------------------------------------------------------------------
    // Leaf blocks
    // -----------------------------------------------------------------------

    private fun renderLeafBlock(block: LeafBlock, context: RenderContext): String {
        val style = block.metadata?.positional?.firstOrNull()
        // A non-built-in style marks the block for extension processors; if no
        // processor claimed it, fall back to a visible listing-style rendering.
        if (style != null && style !in builtInBlockStyles) {
            return renderUnclaimedCustomBlock(block, style, context)
        }
        return when (block.name) {
            LeafBlockName.PARAGRAPH -> renderParagraph(block, context)
            LeafBlockName.LISTING,
            LeafBlockName.LITERAL,
            LeafBlockName.STEM -> renderVerbatim(block, style, context)
            LeafBlockName.PASS -> plainText(block.inlines)
            LeafBlockName.VERSE -> renderVerse(block, context)
        }
    }

    /**
     * Fallback rendering for a custom-style block no extension processor claimed:
     * a listing-style `<pre>` carrying the style name as a CSS class, so the
     * content stays visible instead of silently disappearing.
     */
    private fun renderUnclaimedCustomBlock(block: LeafBlock, style: String, context: RenderContext): String {
        val preClasses = "${context.theme.codeBlockClasses()} custom-block custom-block-$style"
        val escaped = builder.escape(plainText(block.inlines))
        return builder.build {
            openTag("pre", mapOf("class" to preClasses))
            openTag("code")
            text(escaped)
            closeTag("code")
            closeTag("pre")
        }
    }

    /**
     * Renders a paragraph as `<p>` element.
     *
     * Paragraphs contain inlines that are rendered recursively. Theme CSS
     * classes, roles, and the block id are applied for styling.
     */
    private fun renderParagraph(paragraph: LeafBlock, context: RenderContext): String {
        val classes = withRoles(context.theme.paragraphClasses(), paragraph.metadata)
        val content = renderInlineContent(paragraph.inlines, context)

        // Process attribute handlers
        val handlerAttrs = context.processAttributeHandlers(paragraph)
        val attrs = mutableMapOf("class" to classes)
        paragraph.metadata?.id?.let { attrs["id"] = it }
        attrs.putAll(handlerAttrs)

        return builder.build {
            openTag("p", attrs)
            text(content)
            closeTag("p")
        }
    }

    /**
     * Renders a verbatim block (listing/literal/stem) as a `<pre><code>` structure.
     *
     * The language for `[source,lang]` blocks becomes a `language-{lang}` class
     * for compatibility with syntax highlighting libraries. Content is rendered
     * through the inline renderer so text is escaped and callout markers keep
     * their styling.
     */
    private fun renderVerbatim(block: LeafBlock, style: String?, context: RenderContext): String {
        val language = if (style == "source") {
            block.metadata?.positional?.getOrNull(1) ?: block.metadata?.named?.get("language")
        } else {
            null
        }
        val languageClass = language?.let { "language-$it" } ?: ""
        val preClasses = withRoles(context.theme.codeBlockClasses(), block.metadata)

        val preAttrs = mutableMapOf("class" to preClasses)
        block.metadata?.id?.let { preAttrs["id"] = it }

        val content = renderInlineContent(block.inlines, context)
        val title = renderBlockTitle(block.metadata, context)

        return builder.build {
            text(title)
            openTag("pre", preAttrs)
            openTag("code", mapOf("class" to languageClass))
            text(content)
            closeTag("code")
            closeTag("pre")
        }
    }

    /** Renders a verse block as a `verseblock` `<pre>` preserving line breaks. */
    private fun renderVerse(block: LeafBlock, context: RenderContext): String {
        val classes = withRoles("verseblock", block.metadata)
        val attrs = mutableMapOf("class" to classes)
        block.metadata?.id?.let { attrs["id"] = it }

        val content = renderInlineContent(block.inlines, context)
        val title = renderBlockTitle(block.metadata, context)

        return builder.build {
            text(title)
            openTag("pre", attrs)
            text(content)
            closeTag("pre")
        }
    }

    // -----------------------------------------------------------------------
    // Parent blocks
    // -----------------------------------------------------------------------

    private fun renderParentBlock(block: ParentBlock, context: RenderContext): String {
        return when (block.name) {
            ParentBlockName.ADMONITION -> renderAdmonition(block, context)
            ParentBlockName.QUOTE -> renderQuote(block, context)
            ParentBlockName.EXAMPLE -> renderContainer(block, "exampleblock", context)
            ParentBlockName.SIDEBAR -> renderContainer(block, "sidebarblock", context)
            ParentBlockName.OPEN -> renderContainer(block, "openblock", context)
        }
    }

    /**
     * Renders an admonition block (NOTE, TIP, WARNING, etc.) using the
     * conventional icon/content table structure.
     */
    private fun renderAdmonition(block: ParentBlock, context: RenderContext): String {
        val variant = (block.variant ?: "note").lowercase()
        val classes = withRoles("admonitionblock $variant", block.metadata)
        val idAttr = block.metadata?.id?.let { " id=\"${builder.escapeAttribute(it)}\"" } ?: ""
        val title = renderBlockTitle(block.metadata, context)
        val content = block.blocks.joinToString("\n") { render(it, context) }
        return """<div class="$classes"$idAttr>
<table>
<tr>
<td class="icon"><div class="title">${variant.uppercase()}</div></td>
<td class="content">${if (title.isEmpty()) "" else "$title\n"}$content</td>
</tr>
</table>
</div>"""
    }

    /** Renders a quote block as a `quoteblock` with attribution when present. */
    private fun renderQuote(block: ParentBlock, context: RenderContext): String {
        val classes = withRoles("quoteblock", block.metadata)
        val attrs = mutableMapOf("class" to classes)
        block.metadata?.id?.let { attrs["id"] = it }

        val title = renderBlockTitle(block.metadata, context)
        val children = block.blocks.joinToString("\n") { render(it, context) }
        // `[quote, author, source]` positional metadata supplies the attribution.
        val author = block.metadata?.positional?.getOrNull(1)
        val source = block.metadata?.positional?.getOrNull(2)
        val attribution = when {
            author != null && source != null ->
                "<div class=\"attribution\">&#8212; ${builder.escape(author)}, <cite>${builder.escape(source)}</cite></div>"
            author != null ->
                "<div class=\"attribution\">&#8212; ${builder.escape(author)}</div>"
            else -> null
        }
        val quoteClasses = context.theme.quoteClasses()

        return builder.build {
            openTag("div", attrs)
            text(title)
            openTag("blockquote", mapOf("class" to quoteClasses))
            text("\n")
            text(children)
            text("\n")
            closeTag("blockquote")
            attribution?.let {
                text("\n")
                text(it)
            }
            closeTag("div")
        }
    }

    /**
     * Renders a generic block container (example, sidebar, open) as a `<div>`
     * carrying the conventional block class, with an optional title and the
     * nested blocks wrapped in a content `<div>`.
     */
    private fun renderContainer(block: ParentBlock, blockClass: String, context: RenderContext): String {
        val classes = withRoles(blockClass, block.metadata)
        val attrs = mutableMapOf("class" to classes)
        block.metadata?.id?.let { attrs["id"] = it }

        val title = renderBlockTitle(block.metadata, context)
        val children = block.blocks.joinToString("\n") { render(it, context) }

        return builder.build {
            openTag("div", attrs)
            text(title)
            openTag("div", mapOf("class" to "content"))
            text("\n")
            text(children)
            text("\n")
            closeTag("div")
            closeTag("div")
        }
    }

    // -----------------------------------------------------------------------
    // Lists
    // -----------------------------------------------------------------------

    /**
     * Renders a list as `<ul>` or `<ol>` element.
     *
     * Callout lists render as `<ol class="callout-list">` with the callout
     * number carried in a `data-callout` attribute (derived from the `<n>`
     * item marker).
     */
    private fun renderList(list: ListBlock, context: RenderContext): String {
        val tagName = when (list.variant) {
            ListVariant.UNORDERED -> "ul"
            ListVariant.ORDERED, ListVariant.CALLOUT -> "ol"
        }
        val classes = when (list.variant) {
            ListVariant.CALLOUT -> withRoles("callout-list", list.metadata)
            else -> withRoles(context.theme.listClasses(), list.metadata)
        }
        val attrs = mutableMapOf("class" to classes)
        list.metadata?.id?.let { attrs["id"] = it }

        // Items must be rendered BEFORE entering build {}: the shared builder
        // is not re-entrant, so nested build calls corrupt the outer buffer.
        val renderedItems = list.items.map { item ->
            val itemAttrs = if (list.variant == ListVariant.CALLOUT) {
                calloutNumber(item.marker)?.let { mapOf("data-callout" to it.toString()) } ?: emptyMap()
            } else {
                emptyMap()
            }
            val principal = renderInlineContent(item.principal, context)
            val nested = item.blocks.joinToString("\n") { render(it, context) }
            Triple(itemAttrs, principal, nested)
        }
        val title = renderBlockTitle(list.metadata, context)

        return builder.build {
            text(title)
            openTag(tagName, attrs)
            renderedItems.forEach { (itemAttrs, principal, nested) ->
                text("\n")
                openTag("li", itemAttrs)
                text(principal)
                if (nested.isNotEmpty()) {
                    text("\n")
                    text(nested)
                }
                closeTag("li")
            }
            text("\n")
            closeTag(tagName)
        }
    }

    /** Extracts the callout number from a `<n>` item marker. */
    private fun calloutNumber(marker: String): Int? =
        marker.filter { it.isDigit() }.toIntOrNull()

    /**
     * Renders a description list as `<dl>` with `<dt>` terms and `<dd>`
     * descriptions (principal text plus any nested blocks).
     */
    private fun renderDList(list: DListBlock, context: RenderContext): String {
        val classes = withRoles(context.theme.listClasses(), list.metadata)
        val attrs = mutableMapOf("class" to classes)
        list.metadata?.id?.let { attrs["id"] = it }

        // Rendered before build {} — the shared builder is not re-entrant.
        val renderedItems = list.items.map { item ->
            val terms = item.terms.map { renderInlineContent(it, context) }
            val principal = renderInlineContent(item.principal, context)
            val nested = item.blocks.joinToString("\n") { render(it, context) }
            Triple(terms, principal, nested)
        }
        val title = renderBlockTitle(list.metadata, context)

        return builder.build {
            text(title)
            openTag("dl", attrs)
            renderedItems.forEach { (terms, principal, nested) ->
                terms.forEach { term ->
                    text("\n")
                    openTag("dt")
                    text(term)
                    closeTag("dt")
                }
                text("\n")
                openTag("dd")
                text(principal)
                if (nested.isNotEmpty()) {
                    text("\n")
                    text(nested)
                }
                closeTag("dd")
            }
            text("\n")
            closeTag("dl")
        }
    }

    // -----------------------------------------------------------------------
    // Tables
    // -----------------------------------------------------------------------

    /**
     * Renders a table as `<table class="tableblock">` with a `<thead>` for the
     * header row (when present) and a `<tbody>` for the body rows. Non-default
     * column alignments become `halign-center`/`halign-right` cell classes;
     * column spans become `colspan` attributes.
     */
    private fun renderTable(table: TableBlock, context: RenderContext): String {
        val classes = withRoles("tableblock", table.metadata)
        val attrs = mutableMapOf("class" to classes)
        table.metadata?.id?.let { attrs["id"] = it }

        // Rendered before build {} — the shared builder is not re-entrant.
        val title = renderBlockTitle(table.metadata, context)
        val header = table.header?.let { row ->
            row.cells.mapIndexed { index, cell -> renderInlineContent(cell.inlines, context) to cellAttributes(table, row, index) }
        }
        val body = table.rows.map { row ->
            row.cells.mapIndexed { index, cell -> renderInlineContent(cell.inlines, context) to cellAttributes(table, row, index) }
        }

        return builder.build {
            text(title)
            openTag("table", attrs)
            if (header != null) {
                text("\n")
                openTag("thead")
                text("\n")
                openTag("tr")
                header.forEach { (content, cellAttrs) ->
                    openTag("th", cellAttrs)
                    text(content)
                    closeTag("th")
                }
                closeTag("tr")
                text("\n")
                closeTag("thead")
            }
            text("\n")
            openTag("tbody")
            body.forEach { cells ->
                text("\n")
                openTag("tr")
                cells.forEach { (content, cellAttrs) ->
                    openTag("td", cellAttrs)
                    text(content)
                    closeTag("td")
                }
                closeTag("tr")
            }
            text("\n")
            closeTag("tbody")
            text("\n")
            closeTag("table")
        }
    }

    /** The class/colspan attributes of the cell at [index] of [row]. */
    private fun cellAttributes(table: TableBlock, row: TableRow, index: Int): Map<String, String> {
        // The column a cell lands in accounts for the spans of the cells before it.
        val columnIndex = row.cells.take(index).sumOf { it.colSpan }
        val alignment = table.columns.getOrNull(columnIndex)?.alignment ?: TableColumnAlignment.LEFT
        val attrs = mutableMapOf<String, String>()
        when (alignment) {
            TableColumnAlignment.LEFT -> Unit
            TableColumnAlignment.CENTER -> attrs["class"] = "halign-center"
            TableColumnAlignment.RIGHT -> attrs["class"] = "halign-right"
        }
        val cell = row.cells[index]
        if (cell.colSpan > 1) attrs["colspan"] = cell.colSpan.toString()
        if (cell.rowSpan > 1) attrs["rowspan"] = cell.rowSpan.toString()
        return attrs
    }

    // -----------------------------------------------------------------------
    // Breaks, block macros, bibliography
    // -----------------------------------------------------------------------

    private fun renderBreak(block: BreakBlock): String = when (block.variant) {
        BreakVariant.THEMATIC -> "<hr/>"
        BreakVariant.PAGE -> "<div style=\"page-break-after: always;\"></div>"
    }

    private fun renderBlockMacro(block: BlockMacro, context: RenderContext): String {
        val target = block.target ?: ""
        return when (block.name) {
            BlockMacroName.IMAGE -> renderImageBlock(block, target, context)
            BlockMacroName.TOC -> "<div id=\"toc\" class=\"toc\"></div>"
            BlockMacroName.AUDIO -> {
                val src = builder.escapeAttribute(target)
                "<div class=\"audioblock\"><audio controls src=\"$src\"></audio></div>"
            }
            BlockMacroName.VIDEO -> {
                val src = builder.escapeAttribute(target)
                "<div class=\"videoblock\"><video controls src=\"$src\"></video></div>"
            }
        }
    }

    /**
     * Renders an `image::target[alt]` block macro as an `imageblock` `<div>`
     * holding the `<img>`; the alt text comes from the first positional
     * attribute.
     */
    private fun renderImageBlock(block: BlockMacro, target: String, context: RenderContext): String {
        val classes = withRoles("imageblock", block.metadata)
        val attrs = mutableMapOf("class" to classes)
        block.metadata?.id?.let { attrs["id"] = it }

        val escapedSrc = builder.escapeAttribute(target)
        val escapedAlt = builder.escapeAttribute(block.metadata?.positional?.firstOrNull() ?: "")
        val title = renderBlockTitle(block.metadata, context)

        return builder.build {
            openTag("div", attrs)
            text("<img src=\"$escapedSrc\" alt=\"$escapedAlt\">")
            text(title)
            closeTag("div")
        }
    }

    /** Renders a bibliography entry as a labeled `<div>` matching its citations. */
    private fun renderBibliographyEntry(block: BibliographyEntryBlock): String {
        return """<div class="bibliography-entry" id="${block.id}">
<span class="bibliography-label">[${block.id}]</span>
<span class="bibliography-text">${builder.escape(block.citation)}</span>
</div>"""
    }

    // -----------------------------------------------------------------------
    // Shared helpers
    // -----------------------------------------------------------------------

    /** Base CSS classes plus the metadata roles as extra classes. */
    private fun withRoles(base: String, metadata: BlockMetadata?): String {
        val roles = metadata?.roles.orEmpty()
        return if (roles.isEmpty()) base else "$base ${roles.joinToString(" ")}"
    }

    /** The `.Title` line of a block as a title `<div>`, or an empty string. */
    private fun renderBlockTitle(metadata: BlockMetadata?, context: RenderContext): String {
        val title = metadata?.title ?: return ""
        return "<div class=\"title\">${renderInlineContent(title, context)}</div>"
    }

    /** Renders a list of inlines recursively. */
    private fun renderInlineContent(content: List<Inline>, context: RenderContext): String {
        return content.joinToString("") { inlineRenderer.render(it, context) }
    }
}
