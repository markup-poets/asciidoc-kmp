package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.asg.AsgDocument

/**
 * Interface for expanding macros in AsciiDoc documents.
 * Macros are processing instructions that generate content during document processing.
 */
interface MacroExpander {
    /**
     * Expands all macros in the document according to the provided configuration.
     *
     * @param document The document to process
     * @param config Configuration for macro expansion
     * @return Result containing the processed document and any errors
     */
    fun expand(document: AsgDocument, config: MacroConfig): MacroResult
}

/**
 * Configuration for macro expansion.
 */
data class MacroConfig(
    val customMacros: Map<String, MacroProcessor> = emptyMap(),
    val enableBuiltinMacros: Boolean = true
)

/**
 * Result of macro expansion.
 */
data class MacroResult(
    val document: AsgDocument,
    val errors: List<ProcessingError>
)
