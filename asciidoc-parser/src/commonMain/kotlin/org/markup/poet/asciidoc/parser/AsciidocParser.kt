package org.markup.poet.asciidoc.parser

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.error.ParseError
import org.markup.poet.asciidoc.error.ParseWarning

/**
 * Main parser interface for processing AsciiDoc source text.
 */
interface AsciidocParser {
    /**
     * Parse AsciiDoc source text into the ASG document model.
     */
    fun parse(source: String): ParseResult
}

/**
 * Result of parsing operation containing the ASG document and any errors/warnings.
 */
data class ParseResult(
    val document: AsgDocument,
    val errors: List<ParseError>,
    val warnings: List<ParseWarning>
)
