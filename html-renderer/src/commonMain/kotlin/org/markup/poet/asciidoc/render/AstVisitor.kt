package org.markup.poet.asciidoc.render

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.AsgNode
import org.markup.poet.asciidoc.asg.Block
import org.markup.poet.asciidoc.asg.Inline
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.builtInBlockStyles

/**
 * Traverses the ASG tree and delegates rendering to appropriate renderers.
 *
 * AstVisitor implements the visitor pattern to walk through an AsciiDoc ASG
 * and convert it to HTML. It dispatches to BlockRenderer for block nodes
 * and InlineRenderer for inline nodes, maintaining proper HTML structure
 * throughout the traversal.
 *
 * The visitor handles:
 * - AsgDocument nodes by visiting all top-level blocks
 * - Block nodes by delegating to BlockRenderer
 * - Inline nodes by delegating to InlineRenderer
 * - Custom renderers registered for a node class or block style
 *
 * This class is the core traversal engine used by HtmlRenderer to convert
 * ASG nodes into HTML strings.
 *
 * @param blockRenderer The renderer to use for block nodes
 * @param inlineRenderer The renderer to use for inline nodes
 * @param context The rendering context containing configuration and state
 */
class AstVisitor(
    private val blockRenderer: BlockRenderer,
    private val inlineRenderer: InlineRenderer,
    private val context: RenderContext
) {
    /**
     * Visits an ASG node and returns its HTML representation.
     *
     * This method dispatches to the appropriate rendering logic based on
     * the node type:
     * - Custom renderers are checked first: for LeafBlock nodes carrying a
     *   non-built-in block style the style name is tried as key, then the
     *   node class simple name
     * - AsgDocument nodes are traversed to visit all top-level blocks
     * - Block nodes are delegated to BlockRenderer
     * - Inline nodes are delegated to InlineRenderer
     * - Other node kinds (list items) trigger a warning and render nothing;
     *   they are rendered by their owning list block
     *
     * @param node The ASG node to visit and render
     * @return The HTML string representation of the node
     */
    fun visit(node: AsgNode): String {
        // Check for custom renderer first
        val customRenderer = customRendererFor(node)
        if (customRenderer != null) {
            return customRenderer.render(node, context)
        }

        // Fall back to default rendering
        return when (node) {
            is AsgDocument -> visitDocument(node)
            is Block -> blockRenderer.render(node, context)
            is Inline -> inlineRenderer.render(node, context)
            else -> {
                context.logWarning("Unknown node type: ${node::class.simpleName}")
                ""
            }
        }
    }

    /**
     * Looks up a custom renderer for [node]: unknown-style leaf blocks are
     * keyed by their block style (`metadata.positional.first()`), everything
     * else by class simple name.
     */
    private fun customRendererFor(node: AsgNode): CustomRenderer? {
        if (node is LeafBlock) {
            val style = node.metadata?.positional?.firstOrNull()
            if (style != null && style !in builtInBlockStyles) {
                context.config.customRenderers[style]?.let { return it }
            }
        }
        val nodeTypeName = node::class.simpleName ?: "Unknown"
        return context.config.customRenderers[nodeTypeName]
    }

    /**
     * Visits an AsgDocument node by rendering all its top-level blocks.
     *
     * The AsgDocument node is the root of the ASG tree. This method traverses
     * all top-level blocks and concatenates their HTML output with newlines
     * for readability.
     *
     * Note: This method only renders the document's block content. The
     * HtmlRenderer is responsible for wrapping this content in the full
     * HTML document structure (html, head, body) when in standalone mode.
     *
     * @param document The document node to visit
     * @return The concatenated HTML of all top-level blocks
     */
    private fun visitDocument(document: AsgDocument): String {
        return document.blocks.joinToString("\n") { visit(it) }
    }
}
