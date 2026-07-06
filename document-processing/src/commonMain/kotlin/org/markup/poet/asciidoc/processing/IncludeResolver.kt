package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.asg.AsgDocument

/**
 * Interface for resolving include directives in AsciiDoc documents.
 * Handles file path resolution, content embedding, and error reporting.
 */
interface IncludeResolver {
    /**
     * Resolve all include directives in the document.
     *
     * @param document The document containing include directives
     * @param config Configuration for include resolution
     * @return IncludeResult containing the processed document and any errors
     */
    fun resolve(document: AsgDocument, config: IncludeConfig): IncludeResult
}

/**
 * Configuration for include directive resolution.
 */
data class IncludeConfig(
    val maxDepth: Int = 10,
    val basePath: String = "",
    val fileReader: FileReader
)

/**
 * Result of include directive resolution.
 */
data class IncludeResult(
    val document: AsgDocument,
    val errors: List<ProcessingError>,
    val includedFiles: Set<String>
)
