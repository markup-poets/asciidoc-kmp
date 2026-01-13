package io.github.kotlin.asciidoc.parser

import io.github.kotlin.asciidoc.ast.Document
import io.github.kotlin.asciidoc.error.ParseError
import io.github.kotlin.asciidoc.error.ParseWarning

/**
 * Main parser interface for processing AsciiDoc source text.
 */
interface AsciidocParser {
    /**
     * Parse AsciiDoc source text into a document AST.
     */
    fun parse(source: String): ParseResult
    
    /**
     * Parse AsciiDoc source lines into a document AST.
     */
    fun parse(lines: List<String>): ParseResult
}

/**
 * Result of parsing operation containing the document AST and any errors/warnings.
 */
data class ParseResult(
    val document: Document,
    val errors: List<ParseError>,
    val warnings: List<ParseWarning>
)