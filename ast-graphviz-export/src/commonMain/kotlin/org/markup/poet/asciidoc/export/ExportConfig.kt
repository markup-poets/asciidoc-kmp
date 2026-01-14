package org.markup.poet.asciidoc.export

/**
 * Configuration options for AST to Graphviz export.
 */
data class ExportConfig(
    val includeAttributes: Boolean = true,
    val includeSourceLocations: Boolean = false,
    val colorScheme: ColorScheme = ColorScheme.DEFAULT,
    val nodeShape: NodeShape = NodeShape.ELLIPSE,
    val orientation: GraphOrientation = GraphOrientation.LEFT_RIGHT
) {
    companion object {
        fun default(): ExportConfig = ExportConfig()
    }
}

/**
 * Available color schemes for visual styling.
 */
enum class ColorScheme {
    DEFAULT,
    HIGH_CONTRAST,
    COLORBLIND_FRIENDLY
}

/**
 * Available node shapes for visual representation.
 */
enum class NodeShape {
    ELLIPSE,
    BOX,
    CIRCLE,
    DIAMOND,
    RECTANGLE,
    DOUBLEOCTAGON,
    FOLDER
}

/**
 * Graph layout orientation options.
 */
enum class GraphOrientation {
    TOP_DOWN,
    LEFT_RIGHT
}