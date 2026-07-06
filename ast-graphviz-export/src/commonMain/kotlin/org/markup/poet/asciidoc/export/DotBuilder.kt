package org.markup.poet.asciidoc.export

/**
 * Builds DOT format output from collected AST graph data.
 * Generates valid Graphviz DOT syntax with proper formatting and structure.
 */
class DotBuilder(private val config: ExportConfig) {
    
    /**
     * Builds complete DOT format string from graph data.
     * @param graphData The collected graph data to convert
     * @return Valid DOT format string
     */
    fun buildDot(graphData: GraphData): String {
        val builder = StringBuilder()
        
        // Generate DOT header
        builder.append(generateHeader(graphData.metadata))
        builder.append("\n")
        
        // Generate nodes
        builder.append(generateNodes(graphData.nodes))
        builder.append("\n")
        
        // Generate edges
        builder.append(generateEdges(graphData.edges))
        
        // Close the graph
        builder.append("}\n")
        
        return builder.toString()
    }
    
    /**
     * Generates the DOT file header with graph configuration.
     * @param metadata Graph metadata for title and configuration
     * @return DOT header string
     */
    private fun generateHeader(metadata: GraphMetadata): String {
        val builder = StringBuilder()
        
        // Start digraph declaration
        builder.append("digraph AST {\n")
        
        // Set graph attributes
        builder.append("  // Graph configuration\n")
        builder.append("  rankdir=${getOrientationValue()};\n")
        builder.append("  node [fontname=\"Arial\", fontsize=10];\n")
        builder.append("  edge [fontname=\"Arial\", fontsize=8];\n")
        
        // Add title if present
        metadata.title?.let { title ->
            builder.append("  label=\"${escapeLabel(title)}\";\n")
            builder.append("  labelloc=t;\n")
        }
        
        // Add metadata as comment
        builder.append("  // Metadata: ${metadata.nodeCount} nodes, max depth ${metadata.maxDepth}\n")
        
        return builder.toString()
    }
    
    /**
     * Generates DOT node declarations.
     * @param nodes List of node data to generate
     * @return DOT nodes string
     */
    private fun generateNodes(nodes: List<NodeData>): String {
        val builder = StringBuilder()
        builder.append("  // Nodes\n")
        
        nodes.forEach { node ->
            val safeId = escapeNodeId(node.id)
            builder.append("  $safeId [")
            
            // Add label
            builder.append("label=\"${escapeLabel(node.label)}\"")
            
            // Add node type as comment
            builder.append(", comment=\"${escapeAttributeValue(node.nodeType)}\"")
            
            // Add basic styling based on node type
            val style = getBasicNodeStyle(node.nodeType)
            if (style.isNotEmpty()) {
                builder.append(", $style")
            }
            
            // Add attributes if configured
            if (config.includeAttributes && node.attributes.isNotEmpty()) {
                val attrString = node.attributes.entries.joinToString("\\n") { (key, value) ->
                    "${escapeAttributeValue(key)}: ${escapeAttributeValue(value)}"
                }
                if (attrString.isNotEmpty()) {
                    builder.append(", tooltip=\"${escapeLabel(attrString)}\"")
                }
            }
            
            // Add source location if configured
            if (config.includeLocations && node.location != null) {
                val location = node.location
                builder.append(", xlabel=\"${location.start.line}:${location.start.col}\"")
            }
            
            builder.append("];\n")
        }
        
        return builder.toString()
    }
    
    /**
     * Generates DOT edge declarations.
     * @param edges List of edge data to generate
     * @return DOT edges string
     */
    private fun generateEdges(edges: List<EdgeData>): String {
        val builder = StringBuilder()
        builder.append("  // Edges\n")
        
        edges.forEach { edge ->
            val safeFromId = escapeNodeId(edge.fromId)
            val safeToId = escapeNodeId(edge.toId)
            builder.append("  $safeFromId -> $safeToId")
            
            // Add edge label if present
            edge.label?.let { label ->
                builder.append(" [label=\"${escapeLabel(label)}\"]")
            }
            
            builder.append(";\n")
        }
        
        return builder.toString()
    }
    
    /**
     * Gets the DOT orientation value based on configuration.
     * @return DOT rankdir value
     */
    private fun getOrientationValue(): String {
        return when (config.orientation) {
            GraphOrientation.TOP_DOWN -> "TB"
            GraphOrientation.LEFT_RIGHT -> "LR"
        }
    }
    
    /**
     * Gets basic styling attributes for a node kind (see [asgNodeKind]).
     * @param nodeType The ASG node kind
     * @return DOT styling attributes string
     */
    private fun getBasicNodeStyle(nodeType: String): String {
        return when (nodeType) {
            "Document" -> "shape=doubleoctagon, fillcolor=lightblue, style=filled"
            "Section" -> "shape=box, fillcolor=lightgreen, style=filled"
            "DiscreteHeading" -> "shape=box, fillcolor=palegreen, style=filled"
            "Paragraph" -> "shape=box, fillcolor=lightyellow, style=filled"
            "List", "DList" -> "shape=folder, fillcolor=lightcoral, style=filled"
            "ListItem", "DListItem" -> "shape=box, fillcolor=mistyrose, style=filled"
            "Verbatim" -> "shape=box, fillcolor=lightgray, style=filled, fontname=\"Courier\""
            "Custom" -> "shape=box, fillcolor=lightsteelblue, style=\"filled,dashed\""
            "Admonition" -> "shape=box, fillcolor=khaki, style=filled, peripheries=2"
            "Example", "Sidebar", "Open", "Quote" -> "shape=tab, fillcolor=lightgoldenrod, style=filled"
            "Comment" -> "shape=box, fillcolor=lightpink, style=\"filled,dashed\""
            "CalloutList" -> "shape=folder, fillcolor=lightsalmon, style=filled"
            "CalloutItem" -> "shape=box, fillcolor=peachpuff, style=filled"
            "Break" -> "shape=box, fillcolor=gray90, style=filled"
            "BlockMacro" -> "shape=box, fillcolor=lightsteelblue, style=filled"
            "Include" -> "shape=box, fillcolor=wheat, style=filled"
            "Conditional" -> "shape=diamond, fillcolor=wheat, style=filled"
            "BibliographyEntry" -> "shape=box, fillcolor=thistle, style=filled"
            "RawBlock" -> "shape=box, fillcolor=gray80, style=filled"
            "Text" -> "shape=ellipse, fillcolor=white, style=filled"
            "Strong" -> "shape=ellipse, fillcolor=gold, style=\"filled,bold\""
            "Emphasis" -> "shape=ellipse, fillcolor=lavender, style=filled, fontname=\"Arial-Italic\""
            "CodeSpan" -> "shape=ellipse, fillcolor=lightgray, style=filled, fontname=\"Courier\""
            "Mark" -> "shape=ellipse, fillcolor=yellow, style=filled"
            "Link", "XRef" -> "shape=ellipse, fillcolor=lightcyan, style=filled"
            "Image" -> "shape=ellipse, fillcolor=lightsteelblue, style=filled"
            "InlineMacro" -> "shape=ellipse, fillcolor=white, style=filled"
            "AttributeRef" -> "shape=diamond, fillcolor=wheat, style=filled"
            "Callout" -> "shape=circle, fillcolor=orange, style=filled"
            "Footnote" -> "shape=ellipse, fillcolor=mistyrose, style=filled"
            "Citation" -> "shape=ellipse, fillcolor=thistle, style=filled"
            "RawInline" -> "shape=ellipse, fillcolor=gray80, style=filled"
            else -> "shape=ellipse, fillcolor=white, style=filled"
        }
    }
    
    /**
     * Escapes special characters in DOT labels and attribute values.
     * Handles quotes, backslashes, and newlines according to DOT specification.
     * @param text The text to escape
     * @return Escaped text safe for DOT format
     */
    private fun escapeLabel(text: String): String {
        return text
            .replace("\\", "\\\\")  // Escape backslashes first (must be first)
            .replace("\"", "\\\"")  // Escape double quotes
            .replace("\n", "\\n")   // Escape newlines
            .replace("\r", "\\r")   // Escape carriage returns
            .replace("\t", "\\t")   // Escape tabs
            .replace("{", "\\{")    // Escape left braces (DOT record syntax)
            .replace("}", "\\}")    // Escape right braces (DOT record syntax)
            .replace("|", "\\|")    // Escape pipes (DOT record syntax)
            .replace("<", "\\<")    // Escape less than (DOT HTML-like labels)
            .replace(">", "\\>")    // Escape greater than (DOT HTML-like labels)
    }
    
    /**
     * Escapes special characters specifically for DOT attribute values.
     * More restrictive than label escaping to ensure valid DOT syntax.
     * @param value The attribute value to escape
     * @return Escaped attribute value safe for DOT format
     */
    private fun escapeAttributeValue(value: String): String {
        return value
            .replace("\\", "\\\\")  // Escape backslashes first
            .replace("\"", "\\\"")  // Escape double quotes
            .replace("\n", " ")     // Replace newlines with spaces in attributes
            .replace("\r", " ")     // Replace carriage returns with spaces
            .replace("\t", " ")     // Replace tabs with spaces
            .trim()                 // Remove leading/trailing whitespace
    }
    
    /**
     * Escapes special characters for DOT node IDs.
     * Node IDs have stricter requirements than labels.
     * @param id The node ID to escape
     * @return Escaped node ID safe for DOT format
     */
    private fun escapeNodeId(id: String): String {
        // Node IDs should only contain alphanumeric characters and underscores
        return id.replace(Regex("[^a-zA-Z0-9_]"), "_")
    }
}