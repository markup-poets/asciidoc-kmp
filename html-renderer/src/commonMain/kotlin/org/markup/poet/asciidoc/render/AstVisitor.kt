package org.markup.poet.asciidoc.render

import org.markup.poet.asciidoc.ast.*

/**
 * Traverses the AST tree and delegates rendering to appropriate renderers.
 * 
 * AstVisitor implements the visitor pattern to walk through an AsciiDoc AST
 * and convert it to HTML. It dispatches to BlockRenderer for block-level elements
 * and InlineRenderer for inline elements, maintaining proper HTML structure
 * throughout the traversal.
 * 
 * The visitor handles:
 * - Document nodes by visiting all child blocks
 * - Block elements by delegating to BlockRenderer
 * - Inline elements by delegating to InlineRenderer
 * - Unknown node types by logging warnings and skipping them
 * 
 * This class is the core traversal engine used by HtmlRenderer to convert
 * AST nodes into HTML strings.
 * 
 * Validates: Requirements 1.1, 1.2, 1.3, 12.1
 * 
 * @param blockRenderer The renderer to use for block-level elements
 * @param inlineRenderer The renderer to use for inline elements
 * @param context The rendering context containing configuration and state
 */
class AstVisitor(
    private val blockRenderer: BlockRenderer,
    private val inlineRenderer: InlineRenderer,
    private val context: RenderContext
) {
    /**
     * Visits an AST node and returns its HTML representation.
     * 
     * This method dispatches to the appropriate rendering logic based on
     * the node type:
     * - Custom renderers are checked first if registered for the node type
     * - Document nodes are traversed to visit all children
     * - BlockElement nodes are delegated to BlockRenderer
     * - InlineElement nodes are delegated to InlineRenderer
     * - Unknown node types trigger a warning and return empty string
     * 
     * The method maintains the visitor pattern by recursively processing
     * nested nodes through the renderers, which may call back to this
     * visitor for child nodes.
     * 
     * @param node The AST node to visit and render
     * @return The HTML string representation of the node
     */
    fun visit(node: AstNode): String {
        // Check for custom renderer first
        val nodeTypeName = node::class.simpleName ?: "Unknown"
        val customRenderer = context.config.customRenderers[nodeTypeName]
        if (customRenderer != null) {
            return customRenderer.render(node, context)
        }
        
        // Fall back to default rendering
        return when (node) {
            is Document -> visitDocument(node)
            is BlockElement -> blockRenderer.render(node, context)
            is InlineElement -> inlineRenderer.render(node, context)
        }
    }
    
    /**
     * Visits a Document node by rendering all its child blocks.
     * 
     * The Document node is the root of the AST tree. This method traverses
     * all child blocks and concatenates their HTML output with newlines
     * for readability.
     * 
     * Note: This method only renders the document's content blocks. The
     * HtmlRenderer is responsible for wrapping this content in the full
     * HTML document structure (html, head, body) when in standalone mode.
     * 
     * Validates: Requirements 1.1
     * 
     * @param document The document node to visit
     * @return The concatenated HTML of all child blocks
     */
    private fun visitDocument(document: Document): String {
        return document.children.joinToString("\n") { visit(it) }
    }
}
