package org.markup.poet.asciidoc.export

import org.markup.poet.asciidoc.ast.*

/**
 * Provides visual styling for AST nodes based on their type and characteristics.
 * Applies different colors, shapes, and visual attributes to distinguish node types.
 */
class NodeStyler(private val colorScheme: ColorScheme) {
    
    /**
     * Get the visual style for a given AST node.
     */
    fun getNodeStyle(node: AstNode): NodeStyle {
        return when (node) {
            // Document root - special distinctive styling
            is Document -> getDocumentStyle()
            
            // Block elements
            is Section -> getSectionStyle(node.level)
            is Paragraph -> getBlockElementStyle("paragraph")
            is AsciiDocList -> getListStyle()
            is ListItem -> getListItemStyle()
            is CodeBlock -> getCodeBlockStyle()
            is Comment -> getCommentStyle()
            is CalloutList -> getCalloutListStyle()
            is CalloutListItem -> getCalloutListItemStyle()
            
            // Inline elements
            is Text -> getInlineElementStyle("text")
            is Strong -> getInlineElementStyle("strong")
            is Emphasis -> getInlineElementStyle("emphasis")
            is Code -> getInlineElementStyle("code")
            is Link -> getInlineElementStyle("link")
            is Image -> getInlineElementStyle("image")
            is AttributeReference -> getInlineElementStyle("attribute")
            is Callout -> getInlineElementStyle("callout")
            is IncludeDirective -> getBlockElementStyle("include")
            is CrossReference -> getInlineElementStyle("xref")
            is MacroInvocation -> getInlineElementStyle("macro")
        }
    }
    
    /**
     * Get the visual style for edges based on relationship type.
     */
    fun getEdgeStyle(edgeType: String): EdgeStyle {
        return when (edgeType) {
            "parent-child" -> EdgeStyle(
                color = getColorForScheme("edge_default"),
                style = "solid",
                arrowhead = "normal",
                penwidth = 1.0
            )
            "list-item" -> EdgeStyle(
                color = getColorForScheme("edge_list"),
                style = "solid",
                arrowhead = "normal",
                penwidth = 1.5
            )
            else -> EdgeStyle()
        }
    }
    
    // Document root styling - distinctive appearance
    private fun getDocumentStyle(): NodeStyle {
        return NodeStyle(
            fillColor = getColorForScheme("document"),
            shape = "doubleoctagon",
            fontColor = "black",
            peripheries = 2,
            style = "filled"
        )
    }
    
    // Section styling with level-based visual indicators
    private fun getSectionStyle(level: Int): NodeStyle {
        val baseColor = getColorForScheme("section")
        val intensity = when (level) {
            1 -> "1" // Darkest
            2 -> "2" 
            3 -> "3"
            4 -> "4"
            else -> "5" // Lightest
        }
        
        return NodeStyle(
            fillColor = "${baseColor}${intensity}",
            shape = "box",
            fontColor = "black",
            peripheries = if (level <= 2) 2 else 1,
            style = "filled"
        )
    }
    
    // List styling with grouping visual features
    private fun getListStyle(): NodeStyle {
        return NodeStyle(
            fillColor = getColorForScheme("list"),
            shape = "folder",
            fontColor = "black",
            peripheries = 1,
            style = "filled"
        )
    }
    
    private fun getListItemStyle(): NodeStyle {
        return NodeStyle(
            fillColor = getColorForScheme("list_item"),
            shape = "box",
            fontColor = "black",
            peripheries = 1,
            style = "filled,rounded"
        )
    }
    
    // Block element styling
    private fun getBlockElementStyle(elementType: String): NodeStyle {
        val color = when (elementType) {
            "paragraph" -> getColorForScheme("paragraph")
            else -> getColorForScheme("block_default")
        }
        
        return NodeStyle(
            fillColor = color,
            shape = "box",
            fontColor = "black",
            peripheries = 1,
            style = "filled"
        )
    }
    
    private fun getCodeBlockStyle(): NodeStyle {
        return NodeStyle(
            fillColor = getColorForScheme("code_block"),
            shape = "rectangle",
            fontColor = "black",
            peripheries = 1,
            style = "filled"
        )
    }
    
    private fun getCommentStyle(): NodeStyle {
        return NodeStyle(
            fillColor = getColorForScheme("comment"),
            shape = "box",
            fontColor = "black",
            peripheries = 1,
            style = "filled,dashed"
        )
    }
    
    private fun getCalloutListStyle(): NodeStyle {
        return NodeStyle(
            fillColor = getColorForScheme("callout_list"),
            shape = "folder",
            fontColor = "black",
            peripheries = 1,
            style = "filled"
        )
    }
    
    private fun getCalloutListItemStyle(): NodeStyle {
        return NodeStyle(
            fillColor = getColorForScheme("callout_item"),
            shape = "ellipse",
            fontColor = "black",
            peripheries = 1,
            style = "filled"
        )
    }
    
    // Inline element styling
    private fun getInlineElementStyle(elementType: String): NodeStyle {
        val (color, shape, style) = when (elementType) {
            "text" -> Triple(getColorForScheme("text"), "ellipse", "filled")
            "strong" -> Triple(getColorForScheme("strong"), "ellipse", "filled,bold")
            "emphasis" -> Triple(getColorForScheme("emphasis"), "ellipse", "filled")
            "code" -> Triple(getColorForScheme("code_inline"), "ellipse", "filled")
            "link" -> Triple(getColorForScheme("link"), "ellipse", "filled")
            "image" -> Triple(getColorForScheme("image"), "ellipse", "filled")
            "attribute" -> Triple(getColorForScheme("attribute"), "ellipse", "filled,dotted")
            "callout" -> Triple(getColorForScheme("callout"), "circle", "filled")
            else -> Triple(getColorForScheme("inline_default"), "ellipse", "filled")
        }
        
        return NodeStyle(
            fillColor = color,
            shape = shape,
            fontColor = "black",
            peripheries = 1,
            style = style
        )
    }
    
    // Color scheme mapping
    private fun getColorForScheme(elementType: String): String {
        return when (colorScheme) {
            ColorScheme.DEFAULT -> getDefaultColors(elementType)
            ColorScheme.HIGH_CONTRAST -> getHighContrastColors(elementType)
            ColorScheme.COLORBLIND_FRIENDLY -> getColorblindFriendlyColors(elementType)
        }
    }
    
    private fun getDefaultColors(elementType: String): String {
        return when (elementType) {
            // Document and structure
            "document" -> "lightblue"
            "section1" -> "lightgreen"
            "section2" -> "lightgreen2"
            "section3" -> "lightgreen3"
            "section4" -> "lightgreen4"
            "section5" -> "palegreen"
            
            // Block elements
            "paragraph" -> "lightyellow"
            "list" -> "lightcoral"
            "list_item" -> "mistyrose"
            "code_block" -> "lightgray"
            "comment" -> "lightpink"
            "callout_list" -> "lightsalmon"
            "callout_item" -> "peachpuff"
            "block_default" -> "lightsteelblue"
            
            // Inline elements
            "text" -> "white"
            "strong" -> "gold"
            "emphasis" -> "lavender"
            "code_inline" -> "lightgray"
            "link" -> "lightcyan"
            "image" -> "lightsteelblue"
            "attribute" -> "wheat"
            "callout" -> "orange"
            "inline_default" -> "white"
            
            // Edges
            "edge_default" -> "black"
            "edge_list" -> "darkred"
            
            else -> "white"
        }
    }
    
    private fun getHighContrastColors(elementType: String): String {
        return when (elementType) {
            // Document and structure
            "document" -> "navy"
            "section1" -> "darkgreen"
            "section2" -> "green"
            "section3" -> "forestgreen"
            "section4" -> "limegreen"
            "section5" -> "lightgreen"
            
            // Block elements
            "paragraph" -> "yellow"
            "list" -> "red"
            "list_item" -> "pink"
            "code_block" -> "gray"
            "comment" -> "magenta"
            "callout_list" -> "darkorange"
            "callout_item" -> "orange"
            "block_default" -> "steelblue"
            
            // Inline elements
            "text" -> "white"
            "strong" -> "gold"
            "emphasis" -> "violet"
            "code_inline" -> "silver"
            "link" -> "cyan"
            "image" -> "blue"
            "attribute" -> "tan"
            "callout" -> "darkorange"
            "inline_default" -> "lightgray"
            
            // Edges
            "edge_default" -> "black"
            "edge_list" -> "darkred"
            
            else -> "white"
        }
    }
    
    private fun getColorblindFriendlyColors(elementType: String): String {
        return when (elementType) {
            // Document and structure - using blues and oranges
            "document" -> "steelblue"
            "section1" -> "orange"
            "section2" -> "lightsalmon"
            "section3" -> "peachpuff"
            "section4" -> "moccasin"
            "section5" -> "wheat"
            
            // Block elements - using safe color palette
            "paragraph" -> "lightblue"
            "list" -> "sandybrown"
            "list_item" -> "bisque"
            "code_block" -> "lightgray"
            "comment" -> "plum"
            "callout_list" -> "darksalmon"
            "callout_item" -> "lightsalmon"
            "block_default" -> "lightsteelblue"
            
            // Inline elements
            "text" -> "white"
            "strong" -> "gold"
            "emphasis" -> "thistle"
            "code_inline" -> "silver"
            "link" -> "lightblue"
            "image" -> "powderblue"
            "attribute" -> "beige"
            "callout" -> "orange"
            "inline_default" -> "white"
            
            // Edges
            "edge_default" -> "black"
            "edge_list" -> "saddlebrown"
            
            else -> "white"
        }
    }
}