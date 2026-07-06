package org.markup.poet.cli

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.Location
import org.markup.poet.asciidoc.asg.Position
import org.markup.poet.asciidoc.error.ParseError
import org.markup.poet.asciidoc.parser.AsciidocParser
import org.markup.poet.asciidoc.parser.ParseResult
import org.markup.poet.asciidoc.processing.DocumentProcessor
import org.markup.poet.asciidoc.processing.FileReadResult
import org.markup.poet.asciidoc.processing.FileReader
import org.markup.poet.asciidoc.processing.ProcessingConfig
import org.markup.poet.asciidoc.processing.ProcessingError
import org.markup.poet.asciidoc.processing.ProcessingErrorType
import org.markup.poet.asciidoc.processing.ProcessingResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for exit code handling in CLI commands.
 *
 * Verifies that commands return appropriate exit codes:
 * - 0 for successful execution
 * - Non-zero for errors
 *
 * Validates Requirements 1.5, 3.4
 */
class ExitCodeTest {

    private val mockFileReader = object : FileReader {
        override fun readFile(path: String): FileReadResult {
            return FileReadResult.Success("mock content")
        }
    }

    /** Mock parser producing an empty ASG document plus the given parse errors. */
    private fun mockParser(errors: List<ParseError> = emptyList()) = object : AsciidocParser {
        override fun parse(source: String): ParseResult {
            return ParseResult(
                document = AsgDocument(),
                errors = errors,
                warnings = emptyList()
            )
        }
    }

    /** Mock document processor producing the given processing errors. */
    private fun mockDocumentProcessor(errors: List<ProcessingError> = emptyList()) =
        object : DocumentProcessor {
            override fun process(document: AsgDocument, config: ProcessingConfig): ProcessingResult {
                return ProcessingResult(
                    document = document,
                    errors = errors,
                    warnings = emptyList()
                )
            }
        }

    @Test
    fun `ProcessCommand should return Success with exit code 0 on successful processing`() {
        // Create a temporary input file
        val inputFile = java.io.File.createTempFile("test-input", ".adoc")
        inputFile.writeText("= Test Document\n\nSome content")

        try {
            val command = ProcessCommand(mockFileReader, mockParser(), mockDocumentProcessor())

            val args = CommandArgs(
                positional = listOf(inputFile.absolutePath),
                options = emptyMap(),
                flags = emptySet()
            )

            val result = command.execute(args)

            // Verify result is Success (which implies exit code 0)
            assertTrue(result is CommandResult.Success, "Expected Success result for successful processing")
        } finally {
            inputFile.delete()
        }
    }

    @Test
    fun `ProcessCommand should return Error with non-zero exit code when input file not found`() {
        val command = ProcessCommand(mockFileReader, mockParser(), mockDocumentProcessor())

        val args = CommandArgs(
            positional = listOf("nonexistent-file.adoc"),
            options = emptyMap(),
            flags = emptySet()
        )

        val result = command.execute(args)

        // Verify result is Error with default exit code 1
        assertTrue(result is CommandResult.Error, "Expected Error result for missing input file")
        assertEquals(1, (result as CommandResult.Error).exitCode, "Expected exit code 1 for error")
    }

    @Test
    fun `ProcessCommand should return Error with non-zero exit code on processing errors`() {
        // Create a temporary input file
        val inputFile = java.io.File.createTempFile("test-input", ".adoc")
        inputFile.writeText("= Test Document\n\nSome content")

        try {
            // Simulate a processing error
            val documentProcessor = mockDocumentProcessor(
                errors = listOf(
                    ProcessingError(
                        message = "Include file not found",
                        location = Location(Position(5, 1), Position(5, 1)),
                        errorType = ProcessingErrorType.INCLUDE_NOT_FOUND
                    )
                )
            )

            val command = ProcessCommand(mockFileReader, mockParser(), documentProcessor)

            val args = CommandArgs(
                positional = listOf(inputFile.absolutePath),
                options = emptyMap(),
                flags = emptySet()
            )

            val result = command.execute(args)

            // Verify result is Error with default exit code 1
            assertTrue(result is CommandResult.Error, "Expected Error result for processing errors")
            assertEquals(1, (result as CommandResult.Error).exitCode, "Expected exit code 1 for error")
        } finally {
            inputFile.delete()
        }
    }

    @Test
    fun `ProcessCommand should return Error with non-zero exit code on parse errors`() {
        // Create a temporary input file
        val inputFile = java.io.File.createTempFile("test-input", ".adoc")
        inputFile.writeText("= Test Document\n\nSome content")

        try {
            // Simulate a parse error
            val parser = mockParser(
                errors = listOf(
                    ParseError(
                        message = "Unexpected token",
                        line = 10,
                        column = 5
                    )
                )
            )

            val command = ProcessCommand(mockFileReader, parser, mockDocumentProcessor())

            val args = CommandArgs(
                positional = listOf(inputFile.absolutePath),
                options = emptyMap(),
                flags = emptySet()
            )

            val result = command.execute(args)

            // Verify result is Error with default exit code 1
            assertTrue(result is CommandResult.Error, "Expected Error result for parse errors")
            assertEquals(1, (result as CommandResult.Error).exitCode, "Expected exit code 1 for error")
        } finally {
            inputFile.delete()
        }
    }

    @Test
    fun `ProcessCommand should return Error with non-zero exit code for invalid arguments`() {
        val command = ProcessCommand(mockFileReader, mockParser(), mockDocumentProcessor())

        // Test with missing input file argument
        val args = CommandArgs(
            positional = emptyList(),
            options = emptyMap(),
            flags = emptySet()
        )

        val result = command.execute(args)

        // Verify result is Error with default exit code 1
        assertTrue(result is CommandResult.Error, "Expected Error result for missing arguments")
        assertEquals(1, (result as CommandResult.Error).exitCode, "Expected exit code 1 for error")
    }

    @Test
    fun `CommandResult Error should use default exit code 1 when not specified`() {
        val error = CommandResult.Error("Test error message")
        assertEquals(1, error.exitCode, "Default exit code should be 1")
    }

    @Test
    fun `CommandResult Error should allow custom exit codes`() {
        val error = CommandResult.Error("Test error message", exitCode = 42)
        assertEquals(42, error.exitCode, "Custom exit code should be preserved")
    }
}
