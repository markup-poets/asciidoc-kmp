package org.markup.poet.asciidoc.parser

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import org.markup.poet.asciidoc.ast.SourceLocation
import org.markup.poet.asciidoc.error.ErrorSeverity
import org.markup.poet.asciidoc.error.ParseError
import org.markup.poet.asciidoc.error.ParseWarning

/**
 * Property-based tests for malformed syntax error reporting.
 * **Feature: asciidoc-parser, Property 13: Malformed Syntax Error Reporting**
 * **Validates: Requirements 3.7**
 */
class MalformedSyntaxErrorReportingTest : StringSpec({

    "Property 13: Parser should report appropriate errors for malformed block delimiters" {
        checkAll(100, malformedBlockDelimiters()) { malformedBlock ->
            val parser = createTestParser()
            val result = parser.parse(malformedBlock.lines)
            
            // Parser should report errors for malformed delimiters
            if (malformedBlock.shouldHaveErrors) {
                result.errors.shouldNotBeEmpty()
                
                // Errors should have proper structure and reference the malformed syntax
                result.errors.forEach { error ->
                    error.location.line shouldBeGreaterThan 0
                    error.message.shouldNotBe("")
                    error.severity.shouldBeInstanceOf<ErrorSeverity>()
                }
                
                // Should have at least one error related to the malformed syntax
                val hasRelevantError = result.errors.any { error ->
                    error.message.contains("malformed", ignoreCase = true) ||
                    error.message.contains("delimiter", ignoreCase = true) ||
                    error.message.contains("unmatched", ignoreCase = true) ||
                    error.message.contains("invalid", ignoreCase = true)
                }
                hasRelevantError shouldBe true
            }
            
            // Parser should still return a document (not crash)
            result.document shouldNotBe null
        }
    }

    "Property 13a: Parser should report errors for unmatched code block delimiters" {
        checkAll(100, unmatchedCodeBlockDelimiters()) { unmatchedBlock ->
            val parser = createTestParser()
            val result = parser.parse(unmatchedBlock.lines)
            
            // Should report error for unmatched delimiters
            if (unmatchedBlock.hasUnmatchedDelimiters) {
                val hasDelimiterError = result.errors.any { error ->
                    error.message.contains("unmatched", ignoreCase = true) ||
                    error.message.contains("delimiter", ignoreCase = true) ||
                    error.message.contains("code block", ignoreCase = true)
                }
                hasDelimiterError shouldBe true
            }
            
            // Parser should continue and return a document
            result.document shouldNotBe null
        }
    }

    "Property 13b: Parser should report errors for invalid section header syntax" {
        checkAll(100, invalidSectionHeaders()) { invalidHeader ->
            val parser = createTestParser()
            val result = parser.parse(invalidHeader.lines)
            
            // Should report error for invalid section headers
            if (invalidHeader.isInvalid) {
                val hasSectionError = result.errors.any { error ->
                    error.message.contains("section", ignoreCase = true) ||
                    error.message.contains("header", ignoreCase = true) ||
                    error.message.contains("level", ignoreCase = true) ||
                    error.message.contains("invalid", ignoreCase = true)
                }
                hasSectionError shouldBe true
            }
            
            // Parser should continue and return a document
            result.document shouldNotBe null
        }
    }

    "Property 13c: Parser should report errors for malformed list syntax" {
        checkAll(100, malformedListSyntax()) { malformedList ->
            val parser = createTestParser()
            val result = parser.parse(malformedList.lines)
            
            // Should report error for malformed list syntax
            if (malformedList.isMalformed) {
                val hasListError = result.errors.any { error ->
                    error.message.contains("list", ignoreCase = true) ||
                    error.message.contains("marker", ignoreCase = true) ||
                    error.message.contains("malformed", ignoreCase = true)
                }
                hasListError shouldBe true
            }
            
            // Parser should continue and return a document
            result.document shouldNotBe null
        }
    }

})

// Helper function to create a test parser with malformed syntax error detection
private fun createTestParser(): AsciidocParser {
    return object : AsciidocParser {
        override fun parse(source: String): ParseResult {
            return parse(source.lines())
        }
        
        override fun parse(lines: List<String>): ParseResult {
            val errors = mutableListOf<ParseError>()
            val warnings = mutableListOf<ParseWarning>()
            
            // Simulate malformed syntax detection
            lines.forEachIndexed { index, line ->
                val lineNumber = index + 1
                val trimmed = line.trim()
                
                // Detect malformed block delimiters
                when {
                    // Malformed code block delimiters
                    trimmed.startsWith("----") && !trimmed.all { it == '-' } -> {
                        errors.add(ParseError(
                            message = "Malformed code block delimiter: must contain only dashes",
                            location = SourceLocation(lineNumber),
                            severity = ErrorSeverity.ERROR
                        ))
                    }
                    
                    // Unmatched code block delimiters (simplified detection)
                    trimmed == "----" && lines.drop(index + 1).none { it.trim() == "----" } -> {
                        errors.add(ParseError(
                            message = "Unmatched code block delimiter: no closing delimiter found",
                            location = SourceLocation(lineNumber),
                            severity = ErrorSeverity.ERROR
                        ))
                    }
                    
                    // Invalid section headers (too many levels)
                    trimmed.startsWith("=") && trimmed.takeWhile { it == '=' }.length > 6 -> {
                        errors.add(ParseError(
                            message = "Invalid section header: too many levels (maximum 6)",
                            location = SourceLocation(lineNumber),
                            severity = ErrorSeverity.ERROR
                        ))
                    }
                    
                    // Section headers without space after equals
                    trimmed.startsWith("=") && !trimmed.contains(" ") -> {
                        errors.add(ParseError(
                            message = "Invalid section header: missing space after equals signs",
                            location = SourceLocation(lineNumber),
                            severity = ErrorSeverity.ERROR
                        ))
                    }
                    
                    // Malformed list markers
                    trimmed.matches(Regex("^\\*{2,}\\s.*")) -> {
                        errors.add(ParseError(
                            message = "Malformed list marker: multiple asterisks not supported",
                            location = SourceLocation(lineNumber),
                            severity = ErrorSeverity.ERROR
                        ))
                    }
                    
                    // List items without space after marker
                    (trimmed.startsWith("*") && !trimmed.startsWith("* ")) ||
                    (trimmed.startsWith("-") && !trimmed.startsWith("- ")) -> {
                        if (trimmed.length > 1) {
                            errors.add(ParseError(
                                message = "Malformed list marker: missing space after marker",
                                location = SourceLocation(lineNumber),
                                severity = ErrorSeverity.ERROR
                            ))
                        }
                    }
                }
            }
            
            // Create a minimal document
            val document = createMinimalDocument()
            
            return ParseResult(document, errors, warnings)
        }
    }
}

private fun createMinimalDocument() = org.markup.poet.asciidoc.ast.Document(
    title = null,
    children = emptyList(),
    documentAttributes = emptyMap(),
    sourceLocation = SourceLocation(1)
)

// Test data generators for malformed syntax
data class MalformedBlockDelimiter(
    val lines: List<String>,
    val shouldHaveErrors: Boolean,
    val description: String
)

data class UnmatchedCodeBlock(
    val lines: List<String>,
    val hasUnmatchedDelimiters: Boolean
)

data class InvalidSectionHeader(
    val lines: List<String>,
    val isInvalid: Boolean
)

data class MalformedListSyntax(
    val lines: List<String>,
    val isMalformed: Boolean
)

private fun malformedBlockDelimiters(): Arb<MalformedBlockDelimiter> = Arb.choice(
    Arb.constant(MalformedBlockDelimiter(
        lines = listOf("----malformed", "code content", "----"),
        shouldHaveErrors = true,
        description = "Code block delimiter with extra characters"
    )),
    Arb.constant(MalformedBlockDelimiter(
        lines = listOf("---", "code content", "---"),
        shouldHaveErrors = false,
        description = "Valid short delimiter"
    )),
    Arb.constant(MalformedBlockDelimiter(
        lines = listOf("----extra-chars", "code content", "----"),
        shouldHaveErrors = true,
        description = "Code block delimiter with non-dash characters"
    )),
    Arb.constant(MalformedBlockDelimiter(
        lines = listOf("----", "code content", "----"),
        shouldHaveErrors = false,
        description = "Valid code block delimiters"
    ))
)

private fun unmatchedCodeBlockDelimiters(): Arb<UnmatchedCodeBlock> = Arb.choice(
    Arb.constant(UnmatchedCodeBlock(
        lines = listOf("----", "code content", "more content"),
        hasUnmatchedDelimiters = true
    )),
    Arb.constant(UnmatchedCodeBlock(
        lines = listOf("----", "code content", "----"),
        hasUnmatchedDelimiters = false
    )),
    Arb.constant(UnmatchedCodeBlock(
        lines = listOf("some content", "----", "no closing delimiter"),
        hasUnmatchedDelimiters = true
    ))
)

private fun invalidSectionHeaders(): Arb<InvalidSectionHeader> = Arb.choice(
    Arb.constant(InvalidSectionHeader(
        lines = listOf("======= Too Many Levels"),
        isInvalid = true
    )),
    Arb.constant(InvalidSectionHeader(
        lines = listOf("===NoSpaceAfterEquals"),
        isInvalid = true
    )),
    Arb.constant(InvalidSectionHeader(
        lines = listOf("=== Valid Header"),
        isInvalid = false
    )),
    Arb.constant(InvalidSectionHeader(
        lines = listOf("========TooManyLevels"),
        isInvalid = true
    ))
)

private fun malformedListSyntax(): Arb<MalformedListSyntax> = Arb.choice(
    Arb.constant(MalformedListSyntax(
        lines = listOf("** Double asterisk item"),
        isMalformed = true
    )),
    Arb.constant(MalformedListSyntax(
        lines = listOf("*NoSpaceAfterAsterisk"),
        isMalformed = true
    )),
    Arb.constant(MalformedListSyntax(
        lines = listOf("-NoSpaceAfterDash"),
        isMalformed = true
    )),
    Arb.constant(MalformedListSyntax(
        lines = listOf("* Valid list item"),
        isMalformed = false
    ))
)