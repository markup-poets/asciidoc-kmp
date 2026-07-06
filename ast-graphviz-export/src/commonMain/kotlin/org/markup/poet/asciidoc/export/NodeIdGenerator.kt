package org.markup.poet.asciidoc.export

import org.markup.poet.asciidoc.asg.AsgNode

/**
 * Generates unique identifiers for ASG nodes in the DOT graph.
 * Uses a combination of node-kind prefix and sequential counter to ensure uniqueness.
 */
class NodeIdGenerator {
    private val counters = mutableMapOf<String, Int>()

    /**
     * Generates a unique identifier for the given ASG node.
     * @param node The ASG node to generate an ID for
     * @return A unique string identifier
     */
    fun generateId(node: AsgNode): String {
        val prefix = getKindPrefix(asgNodeKind(node))
        val counter = counters.getOrPut(prefix) { 0 } + 1
        counters[prefix] = counter
        return "${prefix}${counter}"
    }

    /**
     * Resets all counters. Useful for generating consistent IDs across multiple exports.
     */
    fun reset() {
        counters.clear()
    }

    /**
     * Gets the ID prefix for a given node kind (see [asgNodeKind]).
     * @param kind The node kind string
     * @return A short prefix representing the node kind
     */
    private fun getKindPrefix(kind: String): String {
        return when (kind) {
            "Document" -> "doc_"
            "Section" -> "sec_"
            "DiscreteHeading" -> "hd_"
            "Paragraph" -> "para_"
            "Verbatim" -> "code_"
            "Custom" -> "custom_"
            "Admonition" -> "adm_"
            "Example" -> "ex_"
            "Sidebar" -> "side_"
            "Open" -> "open_"
            "Quote" -> "quote_"
            "List" -> "list_"
            "CalloutList" -> "clist_"
            "ListItem" -> "item_"
            "CalloutItem" -> "citem_"
            "DList" -> "dlist_"
            "DListItem" -> "ditem_"
            "Break" -> "break_"
            "BlockMacro" -> "macro_"
            "Comment" -> "comm_"
            "Include" -> "inc_"
            "Conditional" -> "cond_"
            "BibliographyEntry" -> "bib_"
            "RawBlock" -> "raw_"
            "Text" -> "text_"
            "Strong" -> "strong_"
            "Emphasis" -> "em_"
            "CodeSpan" -> "inline_code_"
            "Mark" -> "mark_"
            "Link" -> "link_"
            "XRef" -> "xref_"
            "Image" -> "img_"
            "InlineMacro" -> "imacro_"
            "AttributeRef" -> "attr_"
            "Callout" -> "callout_"
            "Footnote" -> "fn_"
            "Citation" -> "cite_"
            "RawInline" -> "rawi_"
            else -> "node_"
        }
    }
}
