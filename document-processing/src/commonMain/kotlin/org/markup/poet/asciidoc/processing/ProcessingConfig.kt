package org.markup.poet.asciidoc.processing

/**
 * Configuration for document processing pipeline.
 * Controls which processors are enabled and their behavior.
 */
data class ProcessingConfig(
    val enableIncludes: Boolean = true,
    val maxIncludeDepth: Int = 10,
    val enableAttributeSubstitution: Boolean = true,
    val attributeDefaults: Map<String, String> = emptyMap(),
    val enableCrossReferences: Boolean = true,
    val enableTocGeneration: Boolean = false,
    val tocDepth: Int = 3,
    val validationStrictness: ValidationLevel = ValidationLevel.NORMAL,
    val enableMacroExpansion: Boolean = true,
    val customMacros: Map<String, MacroProcessor> = emptyMap()
)

/**
 * Validation strictness levels for document processing.
 */
enum class ValidationLevel {
    PERMISSIVE,
    NORMAL,
    STRICT
}

/**
 * Interface for custom macro processors.
 */
interface MacroProcessor {
    fun process(macroName: String, parameters: Map<String, String>, context: MacroContext): MacroExpansionResult
}

/**
 * Context information available to macro processors.
 */
data class MacroContext(
    val document: org.markup.poet.asciidoc.ast.Document,
    val sourceLocation: org.markup.poet.asciidoc.ast.SourceLocation
)

/**
 * Result of macro expansion.
 */
sealed class MacroExpansionResult {
    data class Success(val nodes: List<org.markup.poet.asciidoc.ast.AstNode>) : MacroExpansionResult()
    data class Error(val message: String) : MacroExpansionResult()
}
