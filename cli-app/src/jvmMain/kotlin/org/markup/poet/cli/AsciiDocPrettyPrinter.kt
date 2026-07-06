package org.markup.poet.cli

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.BibliographyEntryBlock
import org.markup.poet.asciidoc.asg.Block
import org.markup.poet.asciidoc.asg.BlockMacro
import org.markup.poet.asciidoc.asg.BreakBlock
import org.markup.poet.asciidoc.asg.BreakVariant
import org.markup.poet.asciidoc.asg.CommentBlock
import org.markup.poet.asciidoc.asg.ConditionalBlock
import org.markup.poet.asciidoc.asg.CustomBlockMacro
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
import org.markup.poet.asciidoc.asg.ParentBlock
import org.markup.poet.asciidoc.asg.ParentBlockName
import org.markup.poet.asciidoc.asg.RawBlock
import org.markup.poet.asciidoc.asg.RefVariant
import org.markup.poet.asciidoc.asg.SectionBlock
import org.markup.poet.asciidoc.asg.SpanVariant
import org.markup.poet.asciidoc.asg.builtInBlockStyles
import org.markup.poet.asciidoc.asg.plainText

/**
 * Converts an AsciiDoc ASG document back to AsciiDoc text format.
 *
 * This pretty printer traverses the ASG tree and generates valid AsciiDoc markup,
 * preserving the document structure and formatting.
 */
class AsciiDocPrettyPrinter {

    /**
     * Convert a document to AsciiDoc text.
     */
    fun print(document: AsgDocument): String {
        val builder = StringBuilder()

        // Print document title if present
        val title = document.header?.title?.let { plainText(it) }
        if (!title.isNullOrEmpty()) {
            builder.appendLine("= $title")
            builder.appendLine()
        }

        // Print document attributes
        document.attributes.forEach { (key, value) ->
            builder.appendLine(":$key: $value")
        }
        if (document.attributes.isNotEmpty()) {
            builder.appendLine()
        }

        // Print document body
        printBlocks(document.blocks, builder, indent = "")

        return builder.toString()
    }

    /**
     * Print a block list, separating sibling blocks with a blank line.
     */
    private fun printBlocks(blocks: List<Block>, builder: StringBuilder, indent: String) {
        blocks.forEachIndexed { index, block ->
            printBlock(block, builder, indent)
            // Add blank line between blocks (except after last one)
            if (index < blocks.size - 1) {
                builder.appendLine()
            }
        }
    }

    /**
     * Print a single block to the builder.
     */
    private fun printBlock(block: Block, builder: StringBuilder, indent: String = "") {
        when (block) {
            is SectionBlock -> printSection(block, builder, indent)
            is LeafBlock -> printLeafBlock(block, builder, indent)
            is ParentBlock -> printParentBlock(block, builder, indent)
            is ListBlock -> printList(block, builder, indent)
            is DListBlock -> printDList(block, builder, indent)
            is CommentBlock -> printComment(block, builder, indent)
            is IncludeBlock -> printInclude(block, builder, indent)
            is BreakBlock -> builder.appendLine(
                "$indent${if (block.variant == BreakVariant.PAGE) "<<<" else "'''"}"
            )
            is BlockMacro -> {
                val attrlist = block.metadata?.positional?.joinToString(",") ?: ""
                builder.appendLine("$indent${block.name.asgName}::${block.target ?: ""}[$attrlist]")
            }
            is CustomBlockMacro -> {
                val attrlist = block.metadata?.positional?.joinToString(",") ?: ""
                builder.appendLine("$indent${block.name}::${block.target ?: ""}[$attrlist]")
            }
            is DiscreteHeading -> {
                builder.appendLine("$indent[discrete]")
                builder.appendLine("$indent${"=".repeat(block.level + 1)} ${plainText(block.title)}")
            }
            is BibliographyEntryBlock -> {
                builder.appendLine("$indent[${block.id}] ${block.citation}")
            }
            is ConditionalBlock -> {
                // Should not normally appear in processed output
                builder.appendLine("$indent${block.variant.name}::${block.condition}[]")
                block.blocks.forEach { printBlock(it, builder, indent) }
                if (block.elseBlocks.isNotEmpty()) {
                    builder.appendLine("${indent}else::[]")
                    block.elseBlocks.forEach { printBlock(it, builder, indent) }
                }
                builder.appendLine("${indent}endif::[]")
            }
            is RawBlock -> {
                builder.appendLine("$indent++++")
                block.content.lines().forEach { builder.appendLine("$indent$it") }
                builder.appendLine("$indent++++")
            }
        }
    }

    /**
     * Print a section with its heading and children.
     */
    private fun printSection(section: SectionBlock, builder: StringBuilder, indent: String) {
        // Print section heading (ASG level 1 == `==`)
        val marker = "=".repeat(section.level + 1)
        builder.appendLine("$indent$marker ${plainText(section.title)}")
        builder.appendLine()

        // Print section children
        printBlocks(section.blocks, builder, indent)
    }

    /**
     * Print a leaf block: a paragraph, a verbatim block (with optional source
     * language), or a custom-styled block.
     */
    private fun printLeafBlock(block: LeafBlock, builder: StringBuilder, indent: String) {
        val style = block.metadata?.positional?.firstOrNull()
        when {
            // A non-built-in style: print the attribute line plus fenced raw content.
            style != null && style !in builtInBlockStyles -> {
                builder.appendLine("$indent[$style]")
                builder.appendLine("$indent----")
                plainText(block.inlines).lines().forEach { builder.appendLine("$indent$it") }
                builder.appendLine("$indent----")
            }
            block.name == LeafBlockName.PARAGRAPH -> {
                val content = block.inlines.joinToString("") { printInline(it) }
                builder.appendLine("$indent$content")
            }
            else -> {
                val language = if (style == "source") {
                    block.metadata?.positional?.getOrNull(1) ?: block.metadata?.named?.get("language")
                } else {
                    null
                }
                if (language != null) {
                    builder.appendLine("$indent[source,$language]")
                }
                val delimiter = when (block.name) {
                    LeafBlockName.PASS -> "++++"
                    LeafBlockName.LITERAL -> "...."
                    LeafBlockName.STEM -> "++++"
                    LeafBlockName.VERSE -> "____"
                    else -> "----"
                }
                builder.appendLine("$indent$delimiter")
                plainText(block.inlines).lines().forEach { builder.appendLine("$indent$it") }
                builder.appendLine("$indent$delimiter")
            }
        }
    }

    /**
     * Print a parent block: an admonition (`NOTE: ...` style) or a delimited
     * container (example, sidebar, quote, open).
     */
    private fun printParentBlock(block: ParentBlock, builder: StringBuilder, indent: String) {
        if (block.name == ParentBlockName.ADMONITION) {
            builder.appendLine("$indent${(block.variant ?: "note").uppercase()}:")
            block.blocks.forEach { printBlock(it, builder, indent) }
        } else {
            val delimiter = block.delimiter ?: when (block.name) {
                ParentBlockName.EXAMPLE -> "===="
                ParentBlockName.SIDEBAR -> "****"
                ParentBlockName.QUOTE -> "____"
                else -> "--"
            }
            builder.appendLine("$indent$delimiter")
            printBlocks(block.blocks, builder, indent)
            builder.appendLine("$indent$delimiter")
        }
    }

    /**
     * Print a list (unordered, ordered, or callout) with its items.
     */
    private fun printList(list: ListBlock, builder: StringBuilder, indent: String) {
        list.items.forEach { item ->
            val content = item.principal.joinToString("") { printInline(it) }
            builder.appendLine("$indent${item.marker} $content")

            // Print any nested blocks (e.g. nested lists) indented under the item
            item.blocks.forEach { nested ->
                printBlock(nested, builder, "$indent  ")
            }
        }
    }

    /**
     * Print a description list (`term:: description`) with its items.
     */
    private fun printDList(list: DListBlock, builder: StringBuilder, indent: String) {
        list.items.forEach { item ->
            item.terms.forEachIndexed { index, term ->
                val termText = "$indent${plainText(term)}${item.marker}"
                if (index == item.terms.size - 1 && item.principal.isNotEmpty()) {
                    val content = item.principal.joinToString("") { printInline(it) }
                    builder.appendLine("$termText $content")
                } else {
                    builder.appendLine(termText)
                }
            }
            item.blocks.forEach { nested ->
                printBlock(nested, builder, "$indent  ")
            }
        }
    }

    /**
     * Print a comment block.
     */
    private fun printComment(comment: CommentBlock, builder: StringBuilder, indent: String) {
        builder.appendLine("$indent////")
        comment.text.lines().forEach { line ->
            builder.appendLine("$indent$line")
        }
        builder.appendLine("$indent////")
    }

    /**
     * Print an include directive.
     * Note: This should not normally appear in processed output since includes are resolved.
     */
    private fun printInclude(directive: IncludeBlock, builder: StringBuilder, indent: String) {
        val lineRange = directive.lineRange
        val lineRangeStr = if (lineRange != null) {
            ",lines=${lineRange.first}..${lineRange.last}"
        } else {
            ""
        }
        builder.appendLine("${indent}include::${directive.path}[$lineRangeStr]")
    }

    /**
     * Print an inline element and return its text representation.
     */
    private fun printInline(inline: Inline): String {
        return when (inline) {
            is InlineText -> inline.value
            is InlineRaw -> inline.content
            is InlineSpan -> {
                val content = inline.inlines.joinToString("") { printInline(it) }
                when (inline.variant) {
                    SpanVariant.STRONG -> "*$content*"
                    SpanVariant.EMPHASIS -> "_${content}_"
                    SpanVariant.CODE -> "`$content`"
                    SpanVariant.MARK -> "#$content#"
                }
            }
            is InlineRef -> when (inline.variant) {
                RefVariant.LINK -> "link:${inline.target}[${plainText(inline.inlines)}]"
                RefVariant.XREF -> {
                    val text = plainText(inline.inlines)
                    if (text.isNotEmpty()) "<<${inline.target},$text>>" else "<<${inline.target}>>"
                }
            }
            is InlineMacro -> {
                if (inline.name == "image") {
                    "image:${inline.target}[${inline.positional.firstOrNull() ?: ""}]"
                } else {
                    val params = (inline.positional + inline.named.map { "${it.key}=${it.value}" })
                        .joinToString(",")
                    "${inline.name}:${inline.target}[$params]"
                }
            }
            is InlineAttributeRef -> "{${inline.name}}"
            is InlineCallout -> "<${inline.number}>"
            is InlineFootnote -> {
                val content = inline.inlines.joinToString("") { printInline(it) }
                "footnote:[$content]"
            }
            is InlineCitation -> "[${inline.citationId}]"
        }
    }
}
