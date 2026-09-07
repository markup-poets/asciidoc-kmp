package org.markup.poet.asciidoc.render

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.DiscreteHeading
import org.markup.poet.asciidoc.asg.SectionBlock
import org.markup.poet.asciidoc.asg.plainText
import org.markup.poet.asciidoc.asg.visitBlocks

/**
 * A "natural" cross-reference (`<<Some Section Title>>`, as opposed to `<<some-id>>`) is
 * resolved by matching the xref target against a heading's own title text, not by slugifying
 * the target -- mirroring how Asciidoctor's `Document#resolve_id` matches against each ref's
 * `xreftext` (title, absent an explicit `reftext`) rather than computing a slug from the xref
 * target itself (see `lib/asciidoctor/document.rb`, `resolve_id`, and `lib/asciidoctor/section.rb`,
 * `generate_id`, in the reference implementation).
 *
 * [knownIds] holds every id a heading will actually render with (explicit or generated), so an
 * xref that already targets a real id is left alone. [titleToId] maps each heading's plain title
 * text to that same id, first occurrence wins -- exactly Asciidoctor's `accum[xreftext] ||= id`.
 */
data class XrefIndex(val knownIds: Set<String>, val titleToId: Map<String, String>) {
    companion object {
        val EMPTY = XrefIndex(emptySet(), emptyMap())
    }
}

/**
 * Pre-pass over the whole document, run once before the real render pass starts, so that an
 * xref appearing *before* the heading it targets can still resolve correctly (headings
 * discovered incrementally during rendering, via [RenderContext.registerHeading], are too late
 * for that case).
 *
 * Uses its own, independent [IdGenerator] instance -- deliberately not [RenderContext]'s -- so
 * this simulated pass can never leak state into (or be corrupted by) the real one. Because
 * [IdGenerator] is a pure function of call sequence, walking the same document in the same
 * order (both use [visitBlocks], the same traversal [BlockRenderer] itself uses) reproduces the
 * exact same ids the real render pass will assign.
 */
fun buildXrefIndex(document: AsgDocument): XrefIndex {
    val idGenerator = IdGenerator()
    val knownIds = mutableSetOf<String>()
    val titleToId = mutableMapOf<String, String>()

    fun register(explicitId: String?, title: String) {
        val id = explicitId ?: idGenerator.next(title)
        knownIds += id
        if (title !in titleToId) titleToId[title] = id
    }

    visitBlocks(document.blocks) { block ->
        when (block) {
            is SectionBlock -> register(block.metadata?.id, plainText(block.title))
            is DiscreteHeading -> register(block.metadata?.id, plainText(block.title))
            else -> Unit
        }
    }

    return XrefIndex(knownIds, titleToId)
}
