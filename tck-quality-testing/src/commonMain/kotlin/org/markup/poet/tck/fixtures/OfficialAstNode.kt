package org.markup.poet.tck.fixtures

import kotlinx.serialization.Serializable

/**
 * Represents a node in the official AsciiDoc TCK AST format.
 * 
 * The official TCK uses a JSON AST format to represent parsed AsciiDoc documents.
 * This data model matches the structure used in the official test output files.
 * 
 * **Format Overview:**
 * - Documents are represented as block nodes with nested blocks and inlines
 * - Each node has a name, type, and optional location information
 * - Blocks contain other blocks or inline content
 * - Inlines represent formatted text spans
 * 
 * **Example JSON:**
 * ```json
 * {
 *   "name": "document",
 *   "type": "block",
 *   "blocks": [
 *     {
 *       "name": "paragraph",
 *       "type": "block",
 *       "inlines": [
 *         {
 *           "name": "text",
 *           "type": "string",
 *           "value": "Hello world",
 *           "location": [{"line": 1, "col": 1}, {"line": 1, "col": 11}]
 *         }
 *       ],
 *       "location": [{"line": 1, "col": 1}, {"line": 1, "col": 11}]
 *     }
 *   ],
 *   "location": [{"line": 1, "col": 1}, {"line": 1, "col": 11}]
 * }
 * ```
 */
@Serializable
data class OfficialAstNode(
    /**
     * The name of the node (e.g., "document", "paragraph", "text", "span").
     */
    val name: String,
    
    /**
     * The type of the node (e.g., "block", "inline", "string").
     */
    val type: String,
    
    /**
     * For block nodes, contains child blocks.
     */
    val blocks: List<OfficialAstNode>? = null,
    
    /**
     * For block nodes with inline content, contains inline nodes.
     */
    val inlines: List<OfficialAstNode>? = null,
    
    /**
     * For text nodes (type="string"), contains the text value.
     */
    val value: String? = null,
    
    /**
     * For inline spans, specifies the variant (e.g., "strong", "emphasis", "monospace").
     */
    val variant: String? = null,
    
    /**
     * For inline spans, specifies the form (e.g., "constrained", "unconstrained").
     */
    val form: String? = null,
    
    /**
     * Location information for the node in the source document.
     * Array of two positions: [start, end]
     */
    val location: List<SourcePosition>? = null,
    
    /**
     * Additional attributes that may be present on specific node types.
     * This allows for extensibility without modifying the core data model.
     */
    val attributes: Map<String, String>? = null
)

/**
 * Represents a position in the source AsciiDoc document.
 * 
 * Used to track where each AST node originated in the source file.
 */
@Serializable
data class SourcePosition(
    /**
     * Line number (1-based).
     */
    val line: Int,
    
    /**
     * Column number (1-based).
     */
    val col: Int
)

/**
 * Extension functions for working with official AST nodes.
 */

/**
 * Check if this node is a document root node.
 */
fun OfficialAstNode.isDocument(): Boolean = name == "document" && type == "block"

/**
 * Check if this node is a block node.
 */
fun OfficialAstNode.isBlock(): Boolean = type == "block"

/**
 * Check if this node is an inline node.
 */
fun OfficialAstNode.isInline(): Boolean = type == "inline"

/**
 * Check if this node is a text node.
 */
fun OfficialAstNode.isText(): Boolean = type == "string"

/**
 * Get all child nodes (blocks and inlines combined).
 */
fun OfficialAstNode.getAllChildren(): List<OfficialAstNode> {
    return (blocks ?: emptyList()) + (inlines ?: emptyList())
}

/**
 * Recursively find all nodes matching a predicate.
 */
fun OfficialAstNode.findAll(predicate: (OfficialAstNode) -> Boolean): List<OfficialAstNode> {
    val results = mutableListOf<OfficialAstNode>()
    if (predicate(this)) {
        results.add(this)
    }
    getAllChildren().forEach { child ->
        results.addAll(child.findAll(predicate))
    }
    return results
}

/**
 * Get the text content of this node and all descendants.
 */
fun OfficialAstNode.getTextContent(): String {
    return when {
        isText() -> value ?: ""
        else -> getAllChildren().joinToString("") { it.getTextContent() }
    }
}
