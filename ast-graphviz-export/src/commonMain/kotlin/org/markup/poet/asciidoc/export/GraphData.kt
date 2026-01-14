package org.markup.poet.asciidoc.export

import org.markup.poet.asciidoc.ast.SourceLocation

/**
 * Represents a node in the DOT graph with all necessary visualization data.
 */
data class NodeData(
    val id: String,
    val label: String,
    val nodeType: String,
    val attributes: Map<String, String>,
    val sourceLocation: SourceLocation?
)

/**
 * Represents an edge between two nodes in the DOT graph.
 */
data class EdgeData(
    val fromId: String,
    val toId: String,
    val label: String? = null
)

/**
 * Complete graph data structure containing all nodes, edges, and metadata.
 */
data class GraphData(
    val nodes: List<NodeData>,
    val edges: List<EdgeData>,
    val metadata: GraphMetadata
)

/**
 * Metadata about the graph structure and source document.
 */
data class GraphMetadata(
    val title: String?,
    val nodeCount: Int,
    val maxDepth: Int,
    val documentAttributes: Map<String, String>
)