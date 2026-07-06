package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.AsgNode

/**
 * Resolves cross-references within a document by matching reference IDs to anchor targets.
 */
interface CrossReferenceResolver {
    /**
     * Resolves all cross-references in the document.
     *
     * @param document The document to process
     * @return Result containing the processed document with resolved references and any errors/warnings
     */
    fun resolve(document: AsgDocument): CrossReferenceResult
}

/**
 * Result of cross-reference resolution.
 */
data class CrossReferenceResult(
    val document: AsgDocument,
    val errors: List<ProcessingError>,
    val warnings: List<ProcessingWarning>,
    val resolvedReferences: Map<String, AnchorTarget>
)

/**
 * Represents a target element that can be referenced by an anchor ID.
 */
data class AnchorTarget(
    val anchorId: String,
    val targetNode: AsgNode,
    val generatedText: String
)
