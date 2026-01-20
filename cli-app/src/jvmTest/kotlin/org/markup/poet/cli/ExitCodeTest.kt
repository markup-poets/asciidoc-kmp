package org.markup.poet.cli

import org.markup.poet.asciidoc.processing.*
import org.markup.poet.asciidoc.parser.AsciidocParser
import org.markup.poet.asciidoc.parser.ParseResult
import org.markup.poet.asciidoc.ast.*
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
    
    @Test
    fun `ProcessCommand should return Success with exit code 0 on successful processing`() {
        // Create a temporary input file
        val inputFile = java.io.File.createTempFile("test-input", ".adoc")
        inputFile.writeText("= Test Document\n\nSome content")
        
        try {
            // Create mock dependencies
            val mockFileReader = object : FileReader {
                override fun readFile(path: String): FileReadResult {
                    return FileReadResult.Success("mock content")
                }
            }
            
            val mockParser = object : AsciidocParser {
                override fun parse(source: String): ParseResult {
                    return ParseResult(
                        document = Document(
                            title = null,
                            children = emptyList(),
                            documentAttributes = emptyMap(),
                            sourceLocation = SourceLocation(0, 0)
                        ),
                        errors = emptyList(),
                        warnings = emptyList()
                    )
                }
                
                override fun parse(lines: List<String>): ParseResult {
                    return parse(lines.joinToString("\n"))
                }
            }
            
            val mockDocumentProcessor = object : DocumentProcessor {
                override fun process(document: Document, config: ProcessingConfig): ProcessingResult {
                    return ProcessingResult(
                        document = document,
                        errors = emptyList(),
                        warnings = emptyList()
                    )
                }
            }
            
            val command = ProcessCommand(mockFileReader, mockParser, mockDocumentProcessor)
            
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
        // Create mock dependencies
        val mockFileReader = object : FileReader {
            override fun readFile(path: String): FileReadResult {
                return FileReadResult.Success("mock content")
            }
        }
        
        val mockParser = object : AsciidocParser {
            override fun parse(source: String): ParseResult {
                return ParseResult(
                    document = Document(
                        title = null,
                        children = emptyList(),
                        documentAttributes = emptyMap(),
                        sourceLocation = SourceLocation(0, 0)
                    ),
                    errors = emptyList(),
                    warnings = emptyList()
                )
            }
            
            override fun parse(lines: List<String>): ParseResult {
                return parse(lines.joinToString("\n"))
            }
        }
        
        val mockDocumentProcessor = object : DocumentProcessor {
            override fun process(document: Document, config: ProcessingConfig): ProcessingResult {
                return ProcessingResult(
                    document = document,
                    errors = emptyList(),
                    warnings = emptyList()
                )
            }
        }
        
        val command = ProcessCommand(mockFileReader, mockParser, mockDocumentProcessor)
        
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
            // Create mock dependencies that simulate processing errors
            val mockFileReader = object : FileReader {
                override fun readFile(path: String): FileReadResult {
                    return FileReadResult.Success("mock content")
                }
            }
            
            val mockParser = object : AsciidocParser {
                override fun parse(source: String): ParseResult {
                    return ParseResult(
                        document = Document(
                            title = null,
                            children = emptyList(),
                            documentAttributes = emptyMap(),
                            sourceLocation = SourceLocation(0, 0)
                        ),
                        errors = emptyList(),
                        warnings = emptyList()
                    )
                }
                
                override fun parse(lines: List<String>): ParseResult {
                    return parse(lines.joinToString("\n"))
                }
            }
            
            val mockDocumentProcessor = object : DocumentProcessor {
                override fun process(document: Document, config: ProcessingConfig): ProcessingResult {
                    // Simulate a processing error
                    return ProcessingResult(
                        document = document,
                        errors = listOf(
                            ProcessingError(
                                message = "Include file not found",
                                location = SourceLocation(5, 0),
                                errorType = ProcessingErrorType.INCLUDE_NOT_FOUND
                            )
                        ),
                        warnings = emptyList()
                    )
                }
            }
            
            val command = ProcessCommand(mockFileReader, mockParser, mockDocumentProcessor)
            
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
            // Create mock dependencies that simulate parse errors
            val mockFileReader = object : FileReader {
                override fun readFile(path: String): FileReadResult {
                    return FileReadResult.Success("mock content")
                }
            }
            
            val mockParser = object : AsciidocParser {
                override fun parse(source: String): ParseResult {
                    // Simulate a parse error
                    return ParseResult(
                        document = Document(
                            title = null,
                            children = emptyList(),
                            documentAttributes = emptyMap(),
                            sourceLocation = SourceLocation(0, 0)
                        ),
                        errors = listOf(
                            org.markup.poet.asciidoc.error.ParseError(
                                message = "Unexpected token",
                                location = SourceLocation(10, 5)
                            )
                        ),
                        warnings = emptyList()
                    )
                }
                
                override fun parse(lines: List<String>): ParseResult {
                    return parse(lines.joinToString("\n"))
                }
            }
            
            val mockDocumentProcessor = object : DocumentProcessor {
                override fun process(document: Document, config: ProcessingConfig): ProcessingResult {
                    return ProcessingResult(
                        document = document,
                        errors = emptyList(),
                        warnings = emptyList()
                    )
                }
            }
            
            val command = ProcessCommand(mockFileReader, mockParser, mockDocumentProcessor)
            
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
        // Create mock dependencies
        val mockFileReader = object : FileReader {
            override fun readFile(path: String): FileReadResult {
                return FileReadResult.Success("mock content")
            }
        }
        
        val mockParser = object : AsciidocParser {
            override fun parse(source: String): ParseResult {
                return ParseResult(
                    document = Document(
                        title = null,
                        children = emptyList(),
                        documentAttributes = emptyMap(),
                        sourceLocation = SourceLocation(0, 0)
                    ),
                    errors = emptyList(),
                    warnings = emptyList()
                )
            }
            
            override fun parse(lines: List<String>): ParseResult {
                return parse(lines.joinToString("\n"))
            }
        }
        
        val mockDocumentProcessor = object : DocumentProcessor {
            override fun process(document: Document, config: ProcessingConfig): ProcessingResult {
                return ProcessingResult(
                    document = document,
                    errors = emptyList(),
                    warnings = emptyList()
                )
            }
        }
        
        val command = ProcessCommand(mockFileReader, mockParser, mockDocumentProcessor)
        
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
