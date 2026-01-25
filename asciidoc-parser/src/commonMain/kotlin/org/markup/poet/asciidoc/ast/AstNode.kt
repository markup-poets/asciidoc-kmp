package org.markup.poet.asciidoc.ast

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
 * 
 * Note: Both line and column use 1-based indexing to match the official AsciiDoc TCK.
 * 
 * @param line The line number (1-based)
 * @param column The starting column (1-based)
 * @param endLine The ending line number (1-based, defaults to same as line)
 * @param endColumn The ending column (1-based, defaults to same as column)
 */
data class SourceLocation(
    val line: Int,
    val column: Int = 1,
    val endLine: Int = line,
    val endColumn: Int = column
)