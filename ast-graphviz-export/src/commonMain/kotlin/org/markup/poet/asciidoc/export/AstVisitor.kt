package org.markup.poet.asciidoc.export

import org.markup.poet.asciidoc.ast.*

/**
 * Visitor interface for traversing AST nodes and collecting visualization data.
 */
interface AstVisitor {
    /**
     * Visits an AST node and processes it for visualization.
     * @param node The AST node to visit
     * @return Result of the visit operation
     */
    fun visit(node: AstNode): VisitResult
}

/**
 * Result of visiting an AST node.
 */
sealed class VisitResult {
    /**
     * Successful visit with collected data.
     */
    data class Success(val nodeData: NodeData) : VisitResult()
    
    /**
     * Visit failed with an error.
     */
    data class Error(val message: String, val node: AstNode) : VisitResult()
}

/**
 * Concrete implementation of AstVisitor that collects data for Graphviz export.
 * Traverses the AST recursively and builds a graph representation.
 */
class GraphvizAstVisitor(
    private val config: ExportConfig = ExportConfig.default()
) : AstVisitor {
    
    private val nodeIdGenerator = NodeIdGenerator()
    private val nodeData = mutableListOf<NodeData>()
    private val edges = mutableListOf<EdgeData>()
    private var maxDepth = 0
    private var currentDepth = 0
    
    /**
     * Visits a node and recursively processes its children.
     * @param node The AST node to visit
     * @return Result of the visit operation
     */
    override fun visit(node: AstNode): VisitResult {
        return try {
            val nodeId = nodeIdGenerator.generateId(node)
            val label = generateNodeLabel(node)
            val nodeType = node::class.simpleName ?: "Unknown"
            
            // Track maximum depth
            maxDepth = maxOf(maxDepth, currentDepth)
            
            // Create node data
            val data = NodeData(
                id = nodeId,
                label = label,
                nodeType = nodeType,
                attributes = if (config.includeAttributes) node.attributes else emptyMap(),
                sourceLocation = if (config.includeSourceLocations) node.sourceLocation else null
            )
            
            nodeData.add(data)
            
            // Process children and create edges
            processChildren(node, nodeId)
            
            VisitResult.Success(data)
        } catch (e: Exception) {
            VisitResult.Error("Failed to visit node: ${e.message}", node)
        }
    }
    
    /**
     * Gets all collected graph data after traversal is complete.
     * @param document The root document node for metadata
     * @return Complete graph data structure
     */
    fun getCollectedData(document: Document): GraphData {
        val metadata = GraphMetadata(
            title = document.title,
            nodeCount = nodeData.size,
            maxDepth = maxDepth,
            documentAttributes = document.documentAttributes
        )
        
        return GraphData(
            nodes = nodeData.toList(),
            edges = edges.toList(),
            metadata = metadata
        )
    }
    
    /**
     * Resets the visitor state for a new traversal.
     */
    fun reset() {
        nodeIdGenerator.reset()
        nodeData.clear()
        edges.clear()
        maxDepth = 0
        currentDepth = 0
    }
    
    /**
     * Processes child nodes and creates edges to them.
     * @param node The parent node
     * @param parentId The parent node's ID
     */
    private fun processChildren(node: AstNode, parentId: String) {
        currentDepth++
        
        when (node) {
            is Document -> {
                node.children.forEach { child ->
                    val result = visit(child)
                    if (result is VisitResult.Success) {
                        edges.add(EdgeData(parentId, result.nodeData.id))
                    }
                }
            }
            is Section -> {
                node.children.forEach { child ->
                    val result = visit(child)
                    if (result is VisitResult.Success) {
                        edges.add(EdgeData(parentId, result.nodeData.id))
                    }
                }
            }
            is Paragraph -> {
                node.content.forEach { child ->
                    val result = visit(child)
                    if (result is VisitResult.Success) {
                        edges.add(EdgeData(parentId, result.nodeData.id))
                    }
                }
            }
            is AsciiDocList -> {
                node.items.forEach { child ->
                    val result = visit(child)
                    if (result is VisitResult.Success) {
                        edges.add(EdgeData(parentId, result.nodeData.id))
                    }
                }
            }
            is ListItem -> {
                // Process inline content
                node.content.forEach { child ->
                    val result = visit(child)
                    if (result is VisitResult.Success) {
                        edges.add(EdgeData(parentId, result.nodeData.id))
                    }
                }
                // Process nested list if present
                node.nestedList?.let { nestedList ->
                    val result = visit(nestedList)
                    if (result is VisitResult.Success) {
                        edges.add(EdgeData(parentId, result.nodeData.id))
                    }
                }
            }
            is CalloutList -> {
                node.items.forEach { child ->
                    val result = visit(child)
                    if (result is VisitResult.Success) {
                        edges.add(EdgeData(parentId, result.nodeData.id))
                    }
                }
            }
            is CalloutListItem -> {
                node.content.forEach { child ->
                    val result = visit(child)
                    if (result is VisitResult.Success) {
                        edges.add(EdgeData(parentId, result.nodeData.id))
                    }
                }
            }
            is Strong -> {
                node.content.forEach { child ->
                    val result = visit(child)
                    if (result is VisitResult.Success) {
                        edges.add(EdgeData(parentId, result.nodeData.id))
                    }
                }
            }
            is Emphasis -> {
                node.content.forEach { child ->
                    val result = visit(child)
                    if (result is VisitResult.Success) {
                        edges.add(EdgeData(parentId, result.nodeData.id))
                    }
                }
            }
            // Leaf nodes (no children to process)
            is CodeBlock, is Comment, is Text, is Code, is Link, is Image, 
            is AttributeReference, is Callout, is IncludeDirective, 
            is CrossReference, is MacroInvocation -> {
                // These nodes have no children to process
            }
            // Default for any other node types
            else -> {
                // Other node types don't have children or are handled elsewhere
            }
        }
        
        currentDepth--
    }
    
    /**
     * Generates a human-readable label for a node.
     * @param node The AST node
     * @return A descriptive label for the node
     */
    private fun generateNodeLabel(node: AstNode): String {
        return when (node) {
            is Document -> "Document${node.title?.let { ": $it" } ?: ""}"
            is Section -> "Section L${node.level}: ${truncateText(node.title, 30)}"
            is Paragraph -> "Paragraph (${node.content.size} elements)"
            is AsciiDocList -> "${node.type.name.lowercase().replaceFirstChar { it.uppercase() }} List (${node.items.size} items)"
            is ListItem -> "List Item: ${truncateText(extractTextContent(node.content), 25)}"
            is CodeBlock -> "Code Block${node.language?.let { " ($it)" } ?: ""}"
            is Comment -> "Comment: ${truncateText(node.content, 30)}"
            is CalloutList -> "Callout List (${node.items.size} items)"
            is CalloutListItem -> "Callout ${node.number}: ${truncateText(extractTextContent(node.content), 25)}"
            is Text -> "Text: ${truncateText(node.content, 40)}"
            is Strong -> "Strong: ${truncateText(extractTextContent(node.content), 30)}"
            is Emphasis -> "Emphasis: ${truncateText(extractTextContent(node.content), 30)}"
            is Code -> "Code: ${truncateText(node.content, 30)}"
            is Link -> "Link: ${truncateText(node.text, 25)}"
            is Image -> "Image: ${truncateText(node.altText, 25)}"
            is AttributeReference -> "Attr: {${node.key}}"
            is Callout -> "Callout <${node.number}>"
            is IncludeDirective -> "Include: ${node.path}"
            is CrossReference -> "XRef: ${node.targetId}"
            is MacroInvocation -> "Macro: ${node.macroName}"
            else -> "Node: ${node::class.simpleName}"
        }
    }
    
    /**
     * Truncates text to a maximum length with ellipsis.
     * @param text The text to truncate
     * @param maxLength Maximum length before truncation
     * @return Truncated text with ellipsis if needed
     */
    private fun truncateText(text: String, maxLength: Int): String {
        return if (text.length <= maxLength) {
            text
        } else {
            text.take(maxLength - 3) + "..."
        }
    }
    
    /**
     * Extracts plain text content from a list of inline elements.
     * @param elements List of inline elements
     * @return Concatenated text content
     */
    private fun extractTextContent(elements: List<InlineElement>): String {
        return elements.joinToString("") { element ->
            when (element) {
                is Text -> element.content
                is Strong -> extractTextContent(element.content)
                is Emphasis -> extractTextContent(element.content)
                is Code -> element.content
                is Link -> element.text
                is Image -> element.altText
                is AttributeReference -> "{${element.key}}"
                is Callout -> "<${element.number}>"
                is CrossReference -> element.customText ?: "<<${element.targetId}>>"
                is MacroInvocation -> "${element.macroName}::[]"
                else -> ""
            }
        }
    }
}