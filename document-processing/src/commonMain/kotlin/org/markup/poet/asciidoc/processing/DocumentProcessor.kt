package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.ast.Document

/**
 * Main interface for document processing.
 * Orchestrates the processing pipeline to transform and enhance parsed AsciiDoc documents.
 */
interface DocumentProcessor {
    /**
     * Process a document according to the provided configuration.
     * 
     * @param document The parsed document to process
     * @param config Configuration controlling which processors are enabled and their behavior
     * @return ProcessingResult containing the processed document and any errors or warnings
     */
    fun process(document: Document, config: ProcessingConfig): ProcessingResult
}

