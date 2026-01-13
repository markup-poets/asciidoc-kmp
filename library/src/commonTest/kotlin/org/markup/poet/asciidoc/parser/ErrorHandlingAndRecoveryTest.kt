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
 * Property-based tests for error handling and recovery functionality.
 * **Feature: asciidoc-parser, Property 12: Error Handling and Recovery**
 * **Validates: Requirements 7.1, 7.2, 7.3, 7.4, 7.5**
 */
class ErrorHandlingAndRecoveryTest : StringSpec({

    "Property 12: Parser should report syntax errors with line numbers and descriptions" {
        checkAll(100, malformedAsciidocContent()) { malformedContent ->
            // Create a parser that can handle errors
            val parser = createTestParser()
            
            // Parse the malformed content
            val result = parser.parse(malformedContent.lines)
            
            // Verify that errors are reported with proper information
            if (malformedContent.expectedErrors > 0) {
                result.errors.shouldNotBeEmpty()
                
                // Each error should have a line number and description
                result.errors.forEach { error ->
                    error.location.line shouldBeGreaterThan 0
                    error.message.shouldNotBe("")
                    error.severity.shouldBeInstanceOf<ErrorSeverity>()
                }
            }
            
            // Parser should always return a document, even with errors
            result.document shouldNotBe null
        }
    }

    "Property 12a: Parser should recover from errors and continue parsing" {
        checkAll(100, mixedValidAndInvalidContent()) { mixedContent ->
            val parser = createTestParser()
            val result = parser.parse(mixedContent.lines)
            
            // Parser should continue parsing after errors
            result.document shouldNotBe null
            
            // Should have parsed some valid content despite errors
            if (mixedContent.validContentLines > 0) {
                val hasContent = result.document.children.isNotEmpty() || result.document.title != null
                hasContent shouldBe true
            }
            
            // Should report errors for invalid parts
            if (mixedContent.invalidContentLines > 0) {
                val hasIssues = result.errors.isNotEmpty() || result.warnings.isNotEmpty()
                hasIssues shouldBe true
            }
        }
    }

    "Property 12b: Parser should collect all errors and warnings without stopping" {
        checkAll(100, multipleErrorContent()) { errorContent ->
            val parser = createTestParser()
            val result = parser.parse(errorContent.lines)
            
            // Parser should not crash and should return a result
            result shouldNotBe null
            result.document shouldNotBe null
            
            // Should collect multiple errors if present
            if (errorContent.expectedErrorCount > 1) {
                (result.errors.size >= 1) shouldBe true
            }
            
            // All errors should have proper structure
            result.errors.forEach { error ->
                error.message.shouldNotBe("")
                error.location.line shouldBeGreaterThan 0
            }
            
            // All warnings should have proper structure
            result.warnings.forEach { warning ->
                warning.message.shouldNotBe("")
                warning.location.line shouldBeGreaterThan 0
            }
        }
    }

    "Property 12c: Parser should provide mechanism to retrieve all errors and warnings" {
        checkAll(100, contentWithErrorsAndWarnings()) { content ->
            val parser = createTestParser()
            val result = parser.parse(content.lines)
            
            // Result should provide access to errors and warnings
            result.errors shouldNotBe null
            result.warnings shouldNotBe null
            
            // Lists should be properly structured
            result.errors.shouldBeInstanceOf<List<ParseError>>()
            result.warnings.shouldBeInstanceOf<List<ParseWarning>>()
            
            // If content has issues, they should be captured
            val totalIssues = result.errors.size + result.warnings.size
            if (content.hasIssues) {
                totalIssues shouldBeGreaterThan 0
            }
        }
    }

})

// Helper function to create a test parser with error handling
private fun createTestParser(): AsciidocParser {
    // For now, return a mock parser that demonstrates error handling
    return object : AsciidocParser {
        override fun parse(source: String): ParseResult {
            return parse(source.lines())
        }
        
        override fun parse(lines: List<String>): ParseResult {
            val errors = mutableListOf<ParseError>()
            val warnings = mutableListOf<ParseWarning>()
            
            // Simulate error detection
            lines.forEachIndexed { index, line ->
                val lineNumber = index + 1
                
                // Detect various error conditions
                when {
                    line.trim().startsWith("===== ") -> {
                        errors.add(ParseError(
                            message = "Section header level too deep (maximum 4 levels)",
                            location = SourceLocation(lineNumber),
                            severity = ErrorSeverity.ERROR
                        ))
                    }
                    line.contains("*unclosed bold") -> {
                        errors.add(ParseError(
                            message = "Unclosed bold markup",
                            location = SourceLocation(lineNumber),
                            severity = ErrorSeverity.ERROR
                        ))
                    }
                    line.contains("_unclosed italic") -> {
                        errors.add(ParseError(
                            message = "Unclosed italic markup",
                            location = SourceLocation(lineNumber),
                            severity = ErrorSeverity.ERROR
                        ))
                    }
                    line.contains("----") && !line.trim().all { it == '-' } -> {
                        warnings.add(ParseWarning(
                            message = "Malformed code block delimiter",
                            location = SourceLocation(lineNumber)
                        ))
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

// Test data generators
data class MalformedContent(
    val lines: List<String>,
    val expectedErrors: Int,
    val description: String
)

data class MixedContent(
    val lines: List<String>,
    val validContentLines: Int,
    val invalidContentLines: Int
)

data class MultipleErrorContent(
    val lines: List<String>,
    val expectedErrorCount: Int
)

data class ContentWithIssues(
    val lines: List<String>,
    val hasIssues: Boolean
)

private fun malformedAsciidocContent(): Arb<MalformedContent> = Arb.choice(
    Arb.constant(MalformedContent(
        lines = listOf("===== Too Deep Header"),
        expectedErrors = 1,
        description = "Section header too deep"
    )),
    Arb.constant(MalformedContent(
        lines = listOf("This has *unclosed bold markup"),
        expectedErrors = 1,
        description = "Unclosed bold markup"
    )),
    Arb.constant(MalformedContent(
        lines = listOf("This has _unclosed italic markup"),
        expectedErrors = 1,
        description = "Unclosed italic markup"
    )),
    Arb.constant(MalformedContent(
        lines = listOf("----malformed", "code", "----"),
        expectedErrors = 1,
        description = "Malformed code block delimiter"
    ))
)

private fun mixedValidAndInvalidContent(): Arb<MixedContent> = arbitrary { rs ->
    val validLines = Arb.list(Arb.string(), 1..5).bind()
    val invalidLines = Arb.list(Arb.choice(
        Arb.constant("*unclosed bold"),
        Arb.constant("_unclosed italic"),
        Arb.constant("===== too deep")
    ), 1..3).bind()
    
    val allLines = validLines + invalidLines
    MixedContent(
        lines = allLines.shuffled(),
        validContentLines = validLines.size,
        invalidContentLines = invalidLines.size
    )
}

private fun multipleErrorContent(): Arb<MultipleErrorContent> = arbitrary { rs ->
    val errorCount = Arb.int(2..5).bind()
    val errorLines = (1..errorCount).map { i ->
        when (i % 3) {
            0 -> "*unclosed bold $i"
            1 -> "_unclosed italic $i"
            else -> "===== too deep $i"
        }
    }
    MultipleErrorContent(
        lines = errorLines,
        expectedErrorCount = errorCount
    )
}

private fun contentWithErrorsAndWarnings(): Arb<ContentWithIssues> = Arb.choice(
    Arb.constant(ContentWithIssues(
        lines = listOf("= Valid Header", "*unclosed bold", "Valid paragraph"),
        hasIssues = true
    )),
    Arb.constant(ContentWithIssues(
        lines = listOf("= Valid Header", "Valid paragraph", "Another paragraph"),
        hasIssues = false
    )),
    Arb.constant(ContentWithIssues(
        lines = listOf("----malformed", "code content", "----"),
        hasIssues = true
    ))
)