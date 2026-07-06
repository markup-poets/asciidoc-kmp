package org.markup.poet.asciidoc.parser

import org.markup.poet.asciidoc.ast.Document
import org.markup.poet.asciidoc.ast.SourceLocation
import org.markup.poet.asciidoc.error.ErrorSeverity
import org.markup.poet.asciidoc.error.ParseError
import org.markup.poet.asciidoc.parser.asg.BlockTreeParser

/**
 * Default implementation of [AsciidocParser].
 *
 * Parsing is delegated to the ASG-native core ([BlockTreeParser]) — the engine
 * validated against the official AsciiDoc TCK — and the resulting ASG is
 * converted to the legacy AST via [AsgToLegacyAst] for downstream consumers.
 *
 * The ASG core is lenient: malformed input degrades to plain content instead
 * of producing errors, so [ParseResult.errors]/[ParseResult.warnings] are
 * normally empty. A fatal error is reported only if parsing throws, in which
 * case a minimal empty document is returned.
 */
class DefaultAsciidocParser : AsciidocParser {

    private val blockTreeParser = BlockTreeParser()

    override fun parseToAsg(source: String): AsgParseResult {
        return try {
            AsgParseResult(
                document = blockTreeParser.parseDocument(source),
                errors = emptyList(),
                warnings = emptyList(),
            )
        } catch (e: Exception) {
            AsgParseResult(
                document = org.markup.poet.asciidoc.asg.AsgDocument(),
                errors = listOf(
                    ParseError(
                        message = "Critical parsing failure: ${e.message}",
                        location = SourceLocation(1),
                        severity = ErrorSeverity.FATAL,
                    )
                ),
                warnings = emptyList(),
            )
        }
    }

    override fun parse(source: String): ParseResult {
        return try {
            val asg = blockTreeParser.parseDocument(source)
            ParseResult(
                document = AsgToLegacyAst.convert(asg),
                errors = emptyList(),
                warnings = emptyList(),
            )
        } catch (e: Exception) {
            val fallbackDocument = Document(
                title = null,
                children = emptyList(),
                documentAttributes = emptyMap(),
                sourceLocation = SourceLocation(1),
            )
            ParseResult(
                document = fallbackDocument,
                errors = listOf(
                    ParseError(
                        message = "Critical parsing failure: ${e.message}",
                        location = SourceLocation(1),
                        severity = ErrorSeverity.FATAL,
                    )
                ),
                warnings = emptyList(),
            )
        }
    }

    override fun parse(lines: List<String>): ParseResult = parse(lines.joinToString("\n"))
}
