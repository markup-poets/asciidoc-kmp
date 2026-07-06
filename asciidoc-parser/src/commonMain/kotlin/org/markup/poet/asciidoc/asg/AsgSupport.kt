package org.markup.poet.asciidoc.asg

/**
 * Dependency-free helpers for working with the ASG tree: plain-text extraction
 * and generic walkers. Shared by the document-processing phase and (from M3 on)
 * the renderers, so it must stay free of any processing- or rendering-specific
 * logic.
 */

/**
 * Block styles with a built-in meaning (verbatim/container styles that keep
 * their standard mapping). Any other block style marks the block as claimable
 * by extension processors (WASM plugins); unclaimed blocks fall back to a
 * visible listing-style rendering.
 */
val builtInBlockStyles: Set<String> =
    setOf("source", "listing", "literal", "verse", "quote", "pass", "stem", "example", "sidebar")

/** Concatenated plain-text value of [inlines], recursing into spans and refs. */
fun plainText(inlines: List<Inline>): String = buildString {
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

/**
 * Depth-first pre-order walk over [blocks] and every nested block: section and
 * parent-block children, list/dlist item blocks, and both branches of
 * conditional blocks.
 */
fun visitBlocks(blocks: List<Block>, action: (Block) -> Unit) {
    for (block in blocks) {
        action(block)
        when (block) {
            is SectionBlock -> visitBlocks(block.blocks, action)
            is ParentBlock -> visitBlocks(block.blocks, action)
            is ListBlock -> block.items.forEach { visitBlocks(it.blocks, action) }
            is DListBlock -> block.items.forEach { visitBlocks(it.blocks, action) }
            is ConditionalBlock -> {
                visitBlocks(block.blocks, action)
                visitBlocks(block.elseBlocks, action)
            }
            else -> Unit
        }
    }
}

/** Recursive walk over [inlines] and every inline nested in spans, refs, and footnotes. */
fun visitInlines(inlines: List<Inline>, action: (Inline) -> Unit) {
    for (inline in inlines) {
        action(inline)
        when (inline) {
            is InlineSpan -> visitInlines(inline.inlines, action)
            is InlineRef -> visitInlines(inline.inlines, action)
            is InlineFootnote -> visitInlines(inline.inlines, action)
            else -> Unit
        }
    }
}

/**
 * The inline lists directly owned by [block]: leaf-block content, list-item
 * principals, dlist terms and principals, and section/heading titles. Child
 * blocks are NOT descended into — combine with [visitBlocks] for a full sweep.
 */
fun inlineListsOf(block: Block): List<List<Inline>> = when (block) {
    is LeafBlock -> listOf(block.inlines)
    is SectionBlock -> listOf(block.title)
    is DiscreteHeading -> listOf(block.title)
    is ListBlock -> block.items.map { it.principal }
    is DListBlock -> block.items.flatMap { it.terms + listOf(it.principal) }
    is TableBlock -> (listOfNotNull(block.header) + block.rows).flatMap { row -> row.cells.map { it.inlines } }
    else -> emptyList()
}

/** The [BlockMetadata] attached to [block], or null for kinds that carry none. */
fun metadataOf(block: Block): BlockMetadata? = when (block) {
    is LeafBlock -> block.metadata
    is ParentBlock -> block.metadata
    is SectionBlock -> block.metadata
    is ListBlock -> block.metadata
    is DListBlock -> block.metadata
    is BlockMacro -> block.metadata
    is CustomBlockMacro -> block.metadata
    is DiscreteHeading -> block.metadata
    is TableBlock -> block.metadata
    else -> null
}
