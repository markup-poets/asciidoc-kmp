package org.markup.poet.asciidoc.export

import org.markup.poet.asciidoc.ast.AstNode

/**
 * Generates unique identifiers for AST nodes in the DOT graph.
 * Uses a combination of node type prefix and sequential counter to ensure uniqueness.
 */
class NodeIdGenerator {
    private val counters = mutableMapOf<String, Int>()
    
    /**
     * Generates a unique identifier for the given AST node.
     * @param node The AST node to generate an ID for
     * @return A unique string identifier
     */
    fun generateId(node: AstNode): String {
        val prefix = getTypePrefix(node)
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
     * Gets the type prefix for a given AST node.
     * @param node The AST node
     * @return A short prefix representing the node type
     */
    private fun getTypePrefix(node: AstNode): String {
        return when (node::class.simpleName) {
            "Document" -> "doc_"
            "Section" -> "sec_"
            "Paragraph" -> "para_"
            "AsciiDocList" -> "list_"
            "ListItem" -> "item_"
            "CodeBlock" -> "code_"
            "Comment" -> "comm_"
            "CalloutList" -> "clist_"
            "CalloutListItem" -> "citem_"
            "Text" -> "text_"
            "Strong" -> "strong_"
            "Emphasis" -> "em_"
            "Code" -> "inline_code_"
            "Link" -> "link_"
            "Image" -> "img_"
            "AttributeReference" -> "attr_"
            "Callout" -> "callout_"
            else -> "node_"
        }
    }
}