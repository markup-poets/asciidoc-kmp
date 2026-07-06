package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.asg.AsgDocument

/**
 * Result of document processing containing the processed document and any errors or warnings.
 */
data class ProcessingResult(
    val document: AsgDocument,
    val errors: List<ProcessingError>,
    val warnings: List<ProcessingWarning>
)
