package org.markup.poet.asciidoc.export

import org.markup.poet.asciidoc.ast.Document

/**
 * Main API for exporting AST structures to Graphviz DOT format.
 * Coordinates the visitor, builder, and styling components to generate complete DOT output.
 */
class GraphvizExporter(
    private val config: ExportConfig = ExportConfig.default()
) {
    
    private val visitor = GraphvizAstVisitor(config)
    private val dotBuilder = DotBuilder(config)
    private val fileWriter = FileWriter()
    
    /**
     * Exports an AST document to DOT format string.
     * @param document The AST document to export
     * @return Valid DOT format string ready for Graphviz rendering
     */
    fun export(document: Document): String {
        // Reset visitor state for fresh export
        visitor.reset()
        
        // Visit the document and collect graph data
        visitor.visit(document)
        val graphData = visitor.getCollectedData(document)
        
        // Build DOT format output
        return dotBuilder.buildDot(graphData)
    }
    
    /**
     * Exports an AST document to a DOT format file.
     * Creates parent directories if they don't exist.
     * 
     * @param document The AST document to export
     * @param filePath The path where the DOT file should be written
     * @return FileWriteResult indicating success or failure with details
     */
    fun exportToFile(document: Document, filePath: String): FileWriteResult {
        // Generate DOT content
        val dotContent = export(document)
        
        // Write to file using platform-specific implementation
        return fileWriter.writeToFile(filePath, dotContent)
    }
}