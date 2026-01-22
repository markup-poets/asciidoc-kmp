package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.ast.Document
import org.markup.poet.asciidoc.ast.InlineElement

/**
 * Interface for processing source code callouts.
 * Extracts callout markers from code blocks, numbers them sequentially,
 * associates callout lists with code blocks, and validates marker-explanation matching.
 */
interface CalloutProcessor {
    /**
     * Process callouts in the document.
     * 
     * @param document The document to process
     * @return Result containing the processed document, errors, warnings, and callout information
     */
    fun process(document: Document): CalloutResult
}

/**
 * Represents a callout marker and its associated explanation.
 * This is different from the AST Callout inline element - this represents
 * the extracted callout information for processing purposes.
 */
data class CalloutInfo(
    /**
     * Sequential number of the callout within its code block.
     */
    val number: Int,
    
    /**
     * The marker text found in the code (e.g., "<1>", "<2>").
     */
    val marker: String,
    
    /**
     * Line number within the code block where the marker appears.
     */
    val lineNumber: Int,
    
    /**
     * The explanation content from the callout list, if associated.
     */
    val explanation: List<InlineElement>?
)

/**
 * Result of callout processing.
 */
data class CalloutResult(
    /**
     * The processed document with callouts extracted and associated.
     */
    val document: Document,
    
    /**
     * Errors encountered during processing (e.g., callouts without code blocks).
     */
    val errors: List<ProcessingError>,
    
    /**
     * Warnings encountered during processing (e.g., mismatched markers and lists).
     */
    val warnings: List<ProcessingWarning>,
    
    /**
     * Map of code block IDs to their callouts.
     * The key is a unique identifier for each code block.
     */
    val calloutsByBlock: Map<String, List<CalloutInfo>>
)
