package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.Block
import org.markup.poet.asciidoc.asg.BlockMacro
import org.markup.poet.asciidoc.asg.BreakBlock
import org.markup.poet.asciidoc.asg.BreakVariant
import org.markup.poet.asciidoc.asg.CommentBlock
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
import org.markup.poet.asciidoc.asg.ListItem
import org.markup.poet.asciidoc.asg.ParentBlock
import org.markup.poet.asciidoc.asg.RefVariant
import org.markup.poet.asciidoc.asg.SectionBlock
import org.markup.poet.asciidoc.asg.SpanVariant
import org.markup.poet.asciidoc.asg.plainText

/**
 * Interface for rendering an ASG document back to AsciiDoc source text.
 */
interface AsciidocRenderer {
    /**
     * Render the given document to AsciiDoc text.
     */
    fun render(document: AsgDocument): String
}

/**
 * Default implementation of AsciidocRenderer.
 */
class DefaultAsciidocRenderer : AsciidocRenderer {

    override fun render(document: AsgDocument): String {
        val sb = StringBuilder()

        // Render document attributes
        document.attributes.forEach { (key, value) ->
            sb.append(":").append(key).append(":").append(if (value.isNotEmpty()) " $value" else "").append("\n")
        }

        if (document.attributes.isNotEmpty() && document.blocks.isNotEmpty()) {
            sb.append("\n")
        }

        // Render blocks
        document.blocks.forEachIndexed { index, block ->
            sb.append(renderBlock(block))
            if (index < document.blocks.size - 1) {
                sb.append("\n\n")
            }
        }

        return sb.toString()
    }

    private fun renderBlock(block: Block): String {
        return when (block) {
            is SectionBlock -> renderSection(block)
            is LeafBlock -> if (block.name == LeafBlockName.PARAGRAPH) {
                renderInlines(block.inlines)
            } else {
                renderVerbatimBlock(block)
            }
            is ListBlock -> renderList(block)
            is DListBlock -> renderDList(block)
            is CommentBlock -> renderComment(block)
            is IncludeBlock -> renderIncludeDirective(block)
            is ParentBlock -> block.blocks.joinToString("\n\n") { renderBlock(it) }
            is BreakBlock -> if (block.variant == BreakVariant.PAGE) "<<<" else "'''"
            is BlockMacro -> "${block.name.asgName}::${block.target ?: ""}[]"
            is DiscreteHeading -> "[discrete]\n${"=".repeat(block.level + 1)} ${renderInlines(block.title)}"
            else -> ""
        }
    }

    private fun renderSection(section: SectionBlock): String {
        val sb = StringBuilder()
        sb.append("=".repeat(section.level + 1)).append(" ").append(renderInlines(section.title)).append("\n")
        section.blocks.forEachIndexed { index, child ->
            sb.append(renderBlock(child))
            if (index < section.blocks.size - 1) {
                sb.append("\n\n")
            }
        }
        return sb.toString()
    }

    private fun renderList(list: ListBlock): String {
        return list.items.joinToString("\n") { renderListItem(it) }
    }

    private fun renderListItem(item: ListItem): String {
        val sb = StringBuilder()
        sb.append(item.marker).append(" ")
        sb.append(renderInlines(item.principal))
        item.blocks.forEach { nested ->
            sb.append("\n").append(renderBlock(nested))
        }
        return sb.toString()
    }

    private fun renderDList(list: DListBlock): String {
        return list.items.joinToString("\n") { item ->
            val terms = item.terms.joinToString("${list.marker}\n") { renderInlines(it) }
            val principal = renderInlines(item.principal)
            if (principal.isEmpty()) "$terms${list.marker}" else "$terms${list.marker} $principal"
        }
    }

    private fun renderVerbatimBlock(block: LeafBlock): String {
        val sb = StringBuilder()
        val style = block.metadata?.positional?.firstOrNull()
        val language = block.metadata?.positional?.getOrNull(1)
        if (style == "source" && language != null) {
            sb.append("[source,").append(language).append("]\n")
        }
        sb.append("----\n")
        val content = plainText(block.inlines)
        sb.append(content)
        if (content.isNotEmpty() && !content.endsWith("\n")) {
            sb.append("\n")
        }
        sb.append("----")
        return sb.toString()
    }

    private fun renderComment(comment: CommentBlock): String {
        return if (comment.text.contains("\n")) {
            "////\n${comment.text}\n////"
        } else {
            "// ${comment.text}"
        }
    }

    private fun renderIncludeDirective(include: IncludeBlock): String {
        return "include::${include.path}[]"
    }

    private fun renderInlines(inlines: List<Inline>): String =
        inlines.joinToString("") { renderInline(it) }

    private fun renderInline(inline: Inline): String {
        return when (inline) {
            is InlineText -> inline.value
            is InlineSpan -> when (inline.variant) {
                SpanVariant.STRONG -> "**${renderInlines(inline.inlines)}**"
                SpanVariant.EMPHASIS -> "__${renderInlines(inline.inlines)}__"
                SpanVariant.CODE -> "`${renderInlines(inline.inlines)}`"
                SpanVariant.MARK -> "#${renderInlines(inline.inlines)}#"
            }
            is InlineRef -> when (inline.variant) {
                RefVariant.LINK -> "link:${inline.target}[${renderInlines(inline.inlines)}]"
                RefVariant.XREF -> {
                    val text = renderInlines(inline.inlines)
                    if (text.isNotEmpty()) "<<${inline.target},$text>>" else "<<${inline.target}>>"
                }
            }
            is InlineMacro -> {
                val params = buildList {
                    addAll(inline.positional)
                    inline.named.forEach { (key, value) -> add("$key=$value") }
                }
                "${inline.name}:${inline.target}[${params.joinToString(",")}]"
            }
            is InlineAttributeRef -> "{${inline.name}}"
            is InlineCallout -> "<${inline.number}>"
            is InlineFootnote -> "footnote:${inline.id}[${renderInlines(inline.inlines)}]"
            is InlineCitation -> "<<${inline.citationId}>>"
            is InlineRaw -> if (inline.format == "asciidoc") inline.content else ""
        }
    }
}
