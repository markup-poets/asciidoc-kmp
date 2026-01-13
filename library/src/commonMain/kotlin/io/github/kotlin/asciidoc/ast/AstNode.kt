package io.github.kotlin.asciidoc.ast

/**
 * Base class for all AST nodes in the AsciiDoc document tree.
 * Each node contains attributes and source location information.
 */
sealed class AstNode {
    abstract val attributes: Map<String, String>
    abstract val sourceLocation: SourceLocation
}

/**
 * Represents a location in the source document for error reporting and debugging.
 */
data class SourceLocation(
    val line: Int,
    val column: Int = 0
)