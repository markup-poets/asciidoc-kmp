package org.markup.poet.asciidoc.export

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.AsgNode
import org.markup.poet.asciidoc.asg.BibliographyEntryBlock
import org.markup.poet.asciidoc.asg.Block
import org.markup.poet.asciidoc.asg.BlockMacro
import org.markup.poet.asciidoc.asg.BlockMetadata
import org.markup.poet.asciidoc.asg.BreakBlock
import org.markup.poet.asciidoc.asg.BreakVariant
import org.markup.poet.asciidoc.asg.CommentBlock
import org.markup.poet.asciidoc.asg.ConditionalBlock
import org.markup.poet.asciidoc.asg.DListBlock
import org.markup.poet.asciidoc.asg.DListItem
import org.markup.poet.asciidoc.asg.DiscreteHeading
import org.markup.poet.asciidoc.asg.IncludeBlock
import org.markup.poet.asciidoc.asg.Inline
import org.markup.poet.asciidoc.asg.InlineAttributeRef
import org.markup.poet.asciidoc.asg.InlineCallout
import org.markup.poet.asciidoc.asg.InlineCitation
import org.markup.poet.asciidoc.asg.InlineFootnote
import org.markup.poet.asciidoc.asg.InlineMacro
import org.markup.poet.asciidoc.asg.InlineRaw
import org.markup.poet.asciidoc.asg.InlineRef
import org.markup.poet.asciidoc.asg.InlineSpan
import org.markup.poet.asciidoc.asg.InlineText
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.LeafBlockName
import org.markup.poet.asciidoc.asg.ListBlock
import org.markup.poet.asciidoc.asg.ListItem
import org.markup.poet.asciidoc.asg.ListVariant
import org.markup.poet.asciidoc.asg.ParentBlock
import org.markup.poet.asciidoc.asg.ParentBlockName
import org.markup.poet.asciidoc.asg.RawBlock
import org.markup.poet.asciidoc.asg.RefVariant
import org.markup.poet.asciidoc.asg.SectionBlock
import org.markup.poet.asciidoc.asg.SpanVariant
import org.markup.poet.asciidoc.asg.TableBlock
import org.markup.poet.asciidoc.asg.TableCell
import org.markup.poet.asciidoc.asg.TableRow
import org.markup.poet.asciidoc.asg.builtInBlockStyles
import org.markup.poet.asciidoc.asg.metadataOf
import org.markup.poet.asciidoc.asg.plainText

/**
 * Visitor interface for traversing ASG nodes and collecting visualization data.
 */
interface AstVisitor {
    /**
     * Visits an ASG node and processes it for visualization.
     * @param node The ASG node to visit
     * @return Result of the visit operation
     */
    fun visit(node: AsgNode): VisitResult
}

/**
 * Result of visiting an ASG node.
 */
sealed class VisitResult {
    /**
     * Successful visit with collected data.
     */
    data class Success(val nodeData: NodeData) : VisitResult()

    /**
     * Visit failed with an error.
     */
    data class Error(val message: String, val node: AsgNode) : VisitResult()
}

/**
 * The visualization kind of an ASG node: the vocabulary used for node IDs,
 * styling, and the `comment` attribute in the DOT output. Where the ASG models
 * several syntaxes with one class, the kind reflects the name/variant axes
 * (e.g. a [LeafBlock] is `Paragraph`, `Verbatim`, or `Custom`).
 */
internal fun asgNodeKind(node: AsgNode): String = when (node) {
    is AsgDocument -> "Document"
    is SectionBlock -> "Section"
    is DiscreteHeading -> "DiscreteHeading"
    is LeafBlock -> {
        val style = node.metadata?.positional?.firstOrNull()
        when {
            style != null && style !in builtInBlockStyles -> "Custom"
            node.name == LeafBlockName.PARAGRAPH -> "Paragraph"
            else -> "Verbatim"
        }
    }
    is ParentBlock -> when (node.name) {
        ParentBlockName.ADMONITION -> "Admonition"
        ParentBlockName.EXAMPLE -> "Example"
        ParentBlockName.SIDEBAR -> "Sidebar"
        ParentBlockName.OPEN -> "Open"
        ParentBlockName.QUOTE -> "Quote"
    }
    is ListBlock -> if (node.variant == ListVariant.CALLOUT) "CalloutList" else "List"
    is ListItem -> if (calloutNumberOf(node) != null) "CalloutItem" else "ListItem"
    is DListBlock -> "DList"
    is DListItem -> "DListItem"
    is BreakBlock -> "Break"
    is BlockMacro -> "BlockMacro"
    is CommentBlock -> "Comment"
    is IncludeBlock -> "Include"
    is ConditionalBlock -> "Conditional"
    is BibliographyEntryBlock -> "BibliographyEntry"
    is RawBlock -> "RawBlock"
    is TableBlock -> "Table"
    is TableRow -> "TableRow"
    is TableCell -> "TableCell"
    is InlineText -> "Text"
    is InlineSpan -> when (node.variant) {
        SpanVariant.STRONG -> "Strong"
        SpanVariant.EMPHASIS -> "Emphasis"
        SpanVariant.CODE -> "CodeSpan"
        SpanVariant.MARK -> "Mark"
        SpanVariant.SUBSCRIPT -> "Subscript"
        SpanVariant.SUPERSCRIPT -> "Superscript"
    }
    is InlineRef -> if (node.variant == RefVariant.XREF) "XRef" else "Link"
    is InlineMacro -> if (node.name == "image") "Image" else "InlineMacro"
    is InlineAttributeRef -> "AttributeRef"
    is InlineCallout -> "Callout"
    is InlineFootnote -> "Footnote"
    is InlineCitation -> "Citation"
    is InlineRaw -> "RawInline"
    else -> node::class.simpleName ?: "Unknown"
}

/** The callout number of a `<n>`-marked callout list item, or null for regular items. */
internal fun calloutNumberOf(item: ListItem): Int? =
    Regex("""<(\d+)>""").matchEntire(item.marker)?.groupValues?.get(1)?.toIntOrNull()

/**
 * Concrete implementation of AstVisitor that collects data for Graphviz export.
 * Traverses the ASG recursively and builds a graph representation.
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
     * @param node The ASG node to visit
     * @return Result of the visit operation
     */
    override fun visit(node: AsgNode): VisitResult {
        return try {
            val nodeId = nodeIdGenerator.generateId(node)
            val label = generateNodeLabel(node)
            val nodeType = asgNodeKind(node)

            // Track maximum depth
            maxDepth = maxOf(maxDepth, currentDepth)

            // Create node data
            val data = NodeData(
                id = nodeId,
                label = label,
                nodeType = nodeType,
                attributes = if (config.includeAttributes) attributesOf(node) else emptyMap(),
                location = if (config.includeLocations) node.location else null
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
    fun getCollectedData(document: AsgDocument): GraphData {
        val metadata = GraphMetadata(
            title = document.header?.title?.let { plainText(it) },
            nodeCount = nodeData.size,
            maxDepth = maxDepth,
            documentAttributes = document.attributes
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
    private fun processChildren(node: AsgNode, parentId: String) {
        currentDepth++

        when (node) {
            is AsgDocument -> visitAll(node.blocks, parentId)
            // The section title stays in the section's own label; children are its blocks.
            is SectionBlock -> visitAll(node.blocks, parentId)
            is LeafBlock -> visitAll(node.inlines, parentId)
            is ParentBlock -> visitAll(node.blocks, parentId)
            is ListBlock -> visitAll(node.items, parentId)
            is ListItem -> {
                visitAll(node.principal, parentId)
                visitAll(node.blocks, parentId)
            }
            is DListBlock -> visitAll(node.items, parentId)
            is DListItem -> {
                node.terms.forEach { visitAll(it, parentId) }
                visitAll(node.principal, parentId)
                visitAll(node.blocks, parentId)
            }
            is ConditionalBlock -> {
                visitAll(node.blocks, parentId)
                visitAll(node.elseBlocks, parentId)
            }
            is TableBlock -> {
                node.header?.let { visitAll(listOf(it), parentId) }
                visitAll(node.rows, parentId)
            }
            is TableRow -> visitAll(node.cells, parentId)
            is TableCell -> visitAll(node.inlines, parentId)
            is InlineSpan -> visitAll(node.inlines, parentId)
            is InlineRef -> visitAll(node.inlines, parentId)
            is InlineFootnote -> visitAll(node.inlines, parentId)
            // Leaf nodes (no children to process)
            else -> Unit
        }

        currentDepth--
    }

    /** Visits every child and records a parent-child edge for each successful visit. */
    private fun visitAll(children: List<AsgNode>, parentId: String) {
        children.forEach { child ->
            val result = visit(child)
            if (result is VisitResult.Success) {
                edges.add(EdgeData(parentId, result.nodeData.id))
            }
        }
    }

    /**
     * Generates a human-readable label for a node.
     * @param node The ASG node
     * @return A descriptive label for the node
     */
    private fun generateNodeLabel(node: AsgNode): String {
        return when (node) {
            is AsgDocument -> "Document${node.header?.let { ": ${truncateText(plainText(it.title), 30)}" } ?: ""}"
            is SectionBlock -> "Section L${node.level}: ${truncateText(plainText(node.title), 30)}"
            is DiscreteHeading -> "Discrete Heading L${node.level}: ${truncateText(plainText(node.title), 30)}"
            is LeafBlock -> generateLeafBlockLabel(node)
            is ParentBlock -> if (node.name == ParentBlockName.ADMONITION) {
                "Admonition [${(node.variant ?: "note").uppercase()}]"
            } else {
                "${node.name.asgName.replaceFirstChar { it.uppercase() }} (${node.blocks.size} blocks)"
            }
            is ListBlock -> "${node.variant.asgName.replaceFirstChar { it.uppercase() }} List (${node.items.size} items)"
            is ListItem -> calloutNumberOf(node)?.let { number ->
                "Callout $number: ${truncateText(plainText(node.principal), 25)}"
            } ?: "List Item: ${truncateText(plainText(node.principal), 25)}"
            is DListBlock -> "Description List (${node.items.size} items)"
            is DListItem -> "DList Item: ${truncateText(node.terms.joinToString(", ") { plainText(it) }, 25)}"
            is BreakBlock -> if (node.variant == BreakVariant.PAGE) "Page Break" else "Thematic Break"
            is BlockMacro -> "Macro: ${node.name.asgName}::${node.target ?: ""}"
            is CommentBlock -> "Comment: ${truncateText(node.text, 30)}"
            is IncludeBlock -> "Include: ${node.path}"
            is ConditionalBlock -> "${node.variant.name.lowercase()}::${node.condition}"
            is BibliographyEntryBlock -> "Bibliography [${node.id}]"
            is RawBlock -> "Raw Block (${node.format})"
            is TableBlock -> "Table (${node.columns.size} cols, ${node.rows.size} rows)"
            is TableRow -> "Table Row (${node.cells.size} cells)"
            is TableCell -> "Cell: ${truncateText(plainText(node.inlines), 25)}"
            is InlineText -> "Text: ${truncateText(node.value, 40)}"
            is InlineSpan -> {
                val name = when (node.variant) {
                    SpanVariant.STRONG -> "Strong"
                    SpanVariant.EMPHASIS -> "Emphasis"
                    SpanVariant.CODE -> "Code"
                    SpanVariant.MARK -> "Mark"
                    SpanVariant.SUBSCRIPT -> "Subscript"
                    SpanVariant.SUPERSCRIPT -> "Superscript"
                }
                "$name: ${truncateText(plainText(node.inlines), 30)}"
            }
            is InlineRef -> if (node.variant == RefVariant.XREF) {
                "XRef: ${node.target}"
            } else {
                "Link: ${truncateText(plainText(node.inlines).ifEmpty { node.target }, 25)}"
            }
            is InlineMacro -> if (node.name == "image") {
                "Image: ${truncateText(node.positional.firstOrNull() ?: node.target, 25)}"
            } else {
                "Macro: ${node.name}"
            }
            is InlineAttributeRef -> "Attr: {${node.name}}"
            is InlineCallout -> "Callout <${node.number}>"
            is InlineFootnote -> "Footnote: ${truncateText(plainText(node.inlines), 25)}"
            is InlineCitation -> "Citation: [${node.citationId}]"
            is InlineRaw -> "Raw Inline (${node.format})"
            else -> "Node: ${node::class.simpleName}"
        }
    }

    /** Label for a leaf block: paragraph, verbatim (with source language), or custom style. */
    private fun generateLeafBlockLabel(block: LeafBlock): String {
        val style = block.metadata?.positional?.firstOrNull()
        return when {
            style != null && style !in builtInBlockStyles -> "Custom Block [$style]"
            block.name == LeafBlockName.PARAGRAPH -> "Paragraph (${block.inlines.size} elements)"
            else -> {
                val language = if (style == "source") {
                    block.metadata?.positional?.getOrNull(1) ?: block.metadata?.named?.get("language")
                } else {
                    null
                }
                val name = block.name.asgName.replaceFirstChar { it.uppercase() }
                "$name Block${language?.let { " ($it)" } ?: ""}"
            }
        }
    }

    /**
     * The attribute map shown in a node's tooltip: document attributes for the
     * root, block metadata (style/positional/named/id/roles/options/title) for blocks.
     */
    private fun attributesOf(node: AsgNode): Map<String, String> = when (node) {
        is AsgDocument -> node.attributes
        is Block -> metadataOf(node)?.toAttributeMap() ?: emptyMap()
        else -> emptyMap()
    }

    /** Flattens block metadata to a display map: positional by 1-based index plus the rest. */
    private fun BlockMetadata.toAttributeMap(): Map<String, String> = buildMap {
        positional.forEachIndexed { index, value -> put((index + 1).toString(), value) }
        putAll(named)
        id?.let { put("id", it) }
        if (roles.isNotEmpty()) put("roles", roles.joinToString(","))
        if (options.isNotEmpty()) put("options", options.joinToString(","))
        title?.let { put("title", plainText(it)) }
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
}
