package org.markup.poet.asciidoc.parser

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.error.ErrorSeverity
import org.markup.poet.asciidoc.error.ParseError
import org.markup.poet.asciidoc.parser.asg.BlockTreeParser

/**
 * Default implementation of [AsciidocParser].
 *
 * Parsing is delegated to the ASG-native core ([BlockTreeParser]) — the engine
 * validated against the official AsciiDoc TCK.
 *
 * The ASG core is lenient: malformed input degrades to plain content instead
 * of producing errors, so [ParseResult.errors]/[ParseResult.warnings] are
 * normally empty. A fatal error is reported only if parsing throws, in which
 * case a minimal empty document is returned.
 */
class DefaultAsciidocParser : AsciidocParser {

    private val blockTreeParser = BlockTreeParser()

    override fun parse(source: String): ParseResult {
        return try {
            ParseResult(
                document = blockTreeParser.parseDocument(source),
                errors = emptyList(),
                warnings = emptyList(),
            )
        } catch (e: Exception) {
            ParseResult(
                document = AsgDocument(),
                errors = listOf(
                    ParseError(
                        message = "Critical parsing failure: ${e.message}",
                        line = 1,
                        column = 1,
                        severity = ErrorSeverity.FATAL,
                    )
                ),
                warnings = emptyList(),
            )
        }
    }
}
