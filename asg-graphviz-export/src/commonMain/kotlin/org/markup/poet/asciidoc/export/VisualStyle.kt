package org.markup.poet.asciidoc.export

/**
 * Visual styling attributes for nodes in the DOT graph.
 */
data class NodeStyle(
    val fillColor: String,
    val shape: String,
    val fontColor: String = "black",
    val peripheries: Int = 1,
    val style: String = "filled"
)

/**
 * Visual styling attributes for edges in the DOT graph.
 */
data class EdgeStyle(
    val color: String = "black",
    val style: String = "solid",
    val arrowhead: String = "normal",
    val penwidth: Double = 1.0
)