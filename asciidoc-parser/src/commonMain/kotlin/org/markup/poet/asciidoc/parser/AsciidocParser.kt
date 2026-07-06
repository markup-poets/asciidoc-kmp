package org.markup.poet.asciidoc.parser

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.ast.Document
import org.markup.poet.asciidoc.error.ParseError
import org.markup.poet.asciidoc.error.ParseWarning

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

    /**
     * Parse AsciiDoc source text into the ASG model. This is the native output
     * of the parser core; [parse] derives the legacy AST from it. Downstream
     * phases are migrating to this entry point, after which [parse] and the
     * legacy AST will be removed.
     */
    fun parseToAsg(source: String): AsgParseResult
}

/**
 * Result of parsing operation containing the document AST and any errors/warnings.
 */
data class ParseResult(
    val document: Document,
    val errors: List<ParseError>,
    val warnings: List<ParseWarning>
)

/**
 * Result of parsing to the ASG model, with any errors/warnings.
 */
data class AsgParseResult(
    val document: AsgDocument,
    val errors: List<ParseError>,
    val warnings: List<ParseWarning>
)