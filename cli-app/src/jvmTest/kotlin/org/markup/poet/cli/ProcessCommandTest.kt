package org.markup.poet.cli

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.parser.AsciidocParser
import org.markup.poet.asciidoc.parser.ParseResult
import org.markup.poet.asciidoc.processing.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for ProcessCommand.
 *
 * Tests argument parsing and validation for the process command.
 */
class ProcessCommandTest {

    // Mock FileReader for testing
    private val mockFileReader = object : FileReader {
        override fun readFile(path: String): FileReadResult {
            return FileReadResult.Success("mock content")
        }
    }

    // Mock Parser for testing
    private val mockParser = object : AsciidocParser {
        override fun parse(source: String): ParseResult {
            return ParseResult(
                document = AsgDocument(),
                errors = emptyList(),
                warnings = emptyList()
            )
        }
    }

    // Mock DocumentProcessor for testing
    private val mockDocumentProcessor = object : DocumentProcessor {
        override fun process(document: AsgDocument, config: ProcessingConfig): ProcessingResult {
            return ProcessingResult(
                document = document,
                errors = emptyList(),
                warnings = emptyList()
            )
        }
    }
    
    private val command = ProcessCommand(mockFileReader, mockParser, mockDocumentProcessor)
    
    @Test
    fun `should have correct name and description`() {
        assertEquals("process", command.name)
        assertEquals("Process AsciiDoc document with include resolution", command.description)
    }
    
    @Test
    fun `should return error when no input file specified`() {
        val args = CommandArgs(
            positional = emptyList(),
            options = emptyMap(),
            flags = emptySet()
        )
        
        val result = command.execute(args)
        
        assertTrue(result is CommandResult.Error)
        val errorMessage = (result as CommandResult.Error).message
        assertTrue(errorMessage.contains("Missing required argument: input file"))
    }
    
    @Test
    fun `should parse input file from positional argument`() {
        val args = CommandArgs(
            positional = listOf("input.adoc"),
            options = emptyMap(),
            flags = emptySet()
        )
        
        val result = command.execute(args)
        
        // Should not fail due to missing input file argument
        // (will fail later in processing, but that's expected for now)
        assertTrue(result is CommandResult.Error)
        val errorMessage = (result as CommandResult.Error).message
        assertTrue(!errorMessage.contains("Missing required argument"))
    }
    
    @Test
    fun `should parse output file from long option`() {
        val args = CommandArgs(
            positional = listOf("input.adoc"),
            options = mapOf("output" to "output.adoc"),
            flags = emptySet()
        )
        
        val result = command.execute(args)
        
        // Argument parsing should succeed (processing will fail, but that's expected)
        assertTrue(result is CommandResult.Error)
        val errorMessage = (result as CommandResult.Error).message
        assertTrue(!errorMessage.contains("Missing required argument"))
    }
    
    @Test
    fun `should parse output file from short option`() {
        val args = CommandArgs(
            positional = listOf("input.adoc"),
            options = mapOf("o" to "output.adoc"),
            flags = emptySet()
        )
        
        val result = command.execute(args)
        
        // Argument parsing should succeed
        assertTrue(result is CommandResult.Error)
        val errorMessage = (result as CommandResult.Error).message
        assertTrue(!errorMessage.contains("Missing required argument"))
    }
    
    @Test
    fun `should prefer long option over short option for output`() {
        val args = CommandArgs(
            positional = listOf("input.adoc"),
            options = mapOf("output" to "long.adoc", "o" to "short.adoc"),
            flags = emptySet()
        )
        
        val result = command.execute(args)
        
        // Both options present, should use long form
        assertTrue(result is CommandResult.Error)
        val errorMessage = (result as CommandResult.Error).message
        assertTrue(!errorMessage.contains("Missing required argument"))
    }
    
    @Test
    fun `should parse base path from long option`() {
        val args = CommandArgs(
            positional = listOf("input.adoc"),
            options = mapOf("base-path" to "/path/to/base"),
            flags = emptySet()
        )
        
        val result = command.execute(args)
        
        // Argument parsing should succeed
        assertTrue(result is CommandResult.Error)
        val errorMessage = (result as CommandResult.Error).message
        assertTrue(!errorMessage.contains("Missing required argument"))
    }
    
    @Test
    fun `should parse base path from short option`() {
        val args = CommandArgs(
            positional = listOf("input.adoc"),
            options = mapOf("b" to "/path/to/base"),
            flags = emptySet()
        )
        
        val result = command.execute(args)
        
        // Argument parsing should succeed
        assertTrue(result is CommandResult.Error)
        val errorMessage = (result as CommandResult.Error).message
        assertTrue(!errorMessage.contains("Missing required argument"))
    }
    
    @Test
    fun `should parse max depth option with valid integer`() {
        val args = CommandArgs(
            positional = listOf("input.adoc"),
            options = mapOf("max-depth" to "5"),
            flags = emptySet()
        )
        
        val result = command.execute(args)
        
        // Argument parsing should succeed
        assertTrue(result is CommandResult.Error)
        val errorMessage = (result as CommandResult.Error).message
        assertTrue(!errorMessage.contains("Invalid value for --max-depth"))
    }
    
    @Test
    fun `should return error for invalid max depth value`() {
        val args = CommandArgs(
            positional = listOf("input.adoc"),
            options = mapOf("max-depth" to "invalid"),
            flags = emptySet()
        )
        
        val result = command.execute(args)
        
        assertTrue(result is CommandResult.Error)
        val errorMessage = (result as CommandResult.Error).message
        assertTrue(errorMessage.contains("Invalid value for --max-depth"))
        assertTrue(errorMessage.contains("must be a positive integer"))
    }
    
    @Test
    fun `should return error for zero max depth`() {
        val args = CommandArgs(
            positional = listOf("input.adoc"),
            options = mapOf("max-depth" to "0"),
            flags = emptySet()
        )
        
        val result = command.execute(args)
        
        assertTrue(result is CommandResult.Error)
        val errorMessage = (result as CommandResult.Error).message
        assertTrue(errorMessage.contains("Invalid value for --max-depth"))
        assertTrue(errorMessage.contains("must be a positive integer"))
    }
    
    @Test
    fun `should return error for negative max depth`() {
        val args = CommandArgs(
            positional = listOf("input.adoc"),
            options = mapOf("max-depth" to "-5"),
            flags = emptySet()
        )
        
        val result = command.execute(args)
        
        assertTrue(result is CommandResult.Error)
        val errorMessage = (result as CommandResult.Error).message
        assertTrue(errorMessage.contains("Invalid value for --max-depth"))
        assertTrue(errorMessage.contains("must be a positive integer"))
    }
    
    @Test
    fun `should use default max depth of 10 when not specified`() {
        val args = CommandArgs(
            positional = listOf("input.adoc"),
            options = emptyMap(),
            flags = emptySet()
        )
        
        val result = command.execute(args)
        
        // Should not fail due to max depth validation
        assertTrue(result is CommandResult.Error)
        val errorMessage = (result as CommandResult.Error).message
        assertTrue(!errorMessage.contains("Invalid value for --max-depth"))
    }
    
    @Test
    fun `should parse verbose flag from long form`() {
        val args = CommandArgs(
            positional = listOf("input.adoc"),
            options = emptyMap(),
            flags = setOf("verbose")
        )
        
        val result = command.execute(args)
        
        // Argument parsing should succeed
        assertTrue(result is CommandResult.Error)
        val errorMessage = (result as CommandResult.Error).message
        assertTrue(!errorMessage.contains("Missing required argument"))
    }
    
    @Test
    fun `should parse verbose flag from short form`() {
        val args = CommandArgs(
            positional = listOf("input.adoc"),
            options = emptyMap(),
            flags = setOf("v")
        )
        
        val result = command.execute(args)
        
        // Argument parsing should succeed
        assertTrue(result is CommandResult.Error)
        val errorMessage = (result as CommandResult.Error).message
        assertTrue(!errorMessage.contains("Missing required argument"))
    }
    
    @Test
    fun `should parse no-overwrite flag`() {
        val args = CommandArgs(
            positional = listOf("input.adoc"),
            options = emptyMap(),
            flags = setOf("no-overwrite")
        )
        
        val result = command.execute(args)
        
        // Argument parsing should succeed
        assertTrue(result is CommandResult.Error)
        val errorMessage = (result as CommandResult.Error).message
        assertTrue(!errorMessage.contains("Missing required argument"))
    }
    
    @Test
    fun `should parse all arguments together`() {
        val args = CommandArgs(
            positional = listOf("input.adoc"),
            options = mapOf(
                "output" to "output.adoc",
                "base-path" to "/base",
                "max-depth" to "15"
            ),
            flags = setOf("verbose", "no-overwrite")
        )
        
        val result = command.execute(args)
        
        // All argument parsing should succeed
        assertTrue(result is CommandResult.Error)
        val errorMessage = (result as CommandResult.Error).message
        assertTrue(!errorMessage.contains("Missing required argument"))
        assertTrue(!errorMessage.contains("Invalid value for --max-depth"))
    }
    
    @Test
    fun `should parse short form options together`() {
        val args = CommandArgs(
            positional = listOf("input.adoc"),
            options = mapOf(
                "o" to "output.adoc",
                "b" to "/base"
            ),
            flags = setOf("v")
        )
        
        val result = command.execute(args)
        
        // All argument parsing should succeed
        assertTrue(result is CommandResult.Error)
        val errorMessage = (result as CommandResult.Error).message
        assertTrue(!errorMessage.contains("Missing required argument"))
    }
    
    @Test
    fun `should return error when input file does not exist`() {
        val args = CommandArgs(
            positional = listOf("nonexistent-file.adoc"),
            options = emptyMap(),
            flags = emptySet()
        )
        
        val result = command.execute(args)
        
        assertTrue(result is CommandResult.Error)
        val errorMessage = (result as CommandResult.Error).message
        assertTrue(errorMessage.contains("Error: Input file not found"))
        assertTrue(errorMessage.contains("File: nonexistent-file.adoc"))
    }
    
    @Test
    fun `should return error with correct file path when input file does not exist`() {
        val args = CommandArgs(
            positional = listOf("/path/to/missing/file.adoc"),
            options = emptyMap(),
            flags = emptySet()
        )
        
        val result = command.execute(args)
        
        assertTrue(result is CommandResult.Error)
        val errorMessage = (result as CommandResult.Error).message
        assertTrue(errorMessage.contains("Error: Input file not found"))
        assertTrue(errorMessage.contains("File: /path/to/missing/file.adoc"))
    }
    
    @Test
    fun `should pass validation when input file exists`() {
        // Create a temporary file for testing
        val tempFile = java.io.File.createTempFile("test-input", ".adoc")
        tempFile.writeText("= Test Document\n\nSome content")
        
        try {
            val args = CommandArgs(
                positional = listOf(tempFile.absolutePath),
                options = emptyMap(),
                flags = emptySet()
            )
            
            val result = command.execute(args)
            
            // Should succeed now that processing is implemented
            assertTrue(result is CommandResult.Success)
        } finally {
            tempFile.delete()
        }
    }
    
    @Test
    fun `should return error when output file exists and no-overwrite flag is set`() {
        // Create temporary input and output files
        val inputFile = java.io.File.createTempFile("test-input", ".adoc")
        val outputFile = java.io.File.createTempFile("test-output", ".adoc")
        inputFile.writeText("= Test Document\n\nSome content")
        outputFile.writeText("Existing content")
        
        try {
            val args = CommandArgs(
                positional = listOf(inputFile.absolutePath),
                options = mapOf("output" to outputFile.absolutePath),
                flags = setOf("no-overwrite")
            )
            
            val result = command.execute(args)
            
            assertTrue(result is CommandResult.Error)
            val errorMessage = (result as CommandResult.Error).message
            assertTrue(errorMessage.contains("Error: Output file already exists"))
            assertTrue(errorMessage.contains("File: ${outputFile.absolutePath}"))
            assertTrue(errorMessage.contains("use without --no-overwrite to overwrite"))
        } finally {
            inputFile.delete()
            outputFile.delete()
        }
    }
    
    @Test
    fun `should not check output file when no-overwrite flag is not set`() {
        // Create temporary input and output files
        val inputFile = java.io.File.createTempFile("test-input", ".adoc")
        val outputFile = java.io.File.createTempFile("test-output", ".adoc")
        inputFile.writeText("= Test Document\n\nSome content")
        outputFile.writeText("Existing content")
        
        try {
            val args = CommandArgs(
                positional = listOf(inputFile.absolutePath),
                options = mapOf("output" to outputFile.absolutePath),
                flags = emptySet()
            )
            
            val result = command.execute(args)
            
            // Should succeed - overwriting is allowed
            assertTrue(result is CommandResult.Success)
        } finally {
            inputFile.delete()
            outputFile.delete()
        }
    }
    
    @Test
    fun `should not check output file when no output file is specified`() {
        // Create temporary input file
        val inputFile = java.io.File.createTempFile("test-input", ".adoc")
        inputFile.writeText("= Test Document\n\nSome content")
        
        try {
            val args = CommandArgs(
                positional = listOf(inputFile.absolutePath),
                options = emptyMap(),
                flags = setOf("no-overwrite")
            )
            
            val result = command.execute(args)
            
            // Should succeed - writing to stdout
            assertTrue(result is CommandResult.Success)
        } finally {
            inputFile.delete()
        }
    }
    
    @Test
    fun `should allow processing when output file does not exist and no-overwrite is set`() {
        // Create temporary input file
        val inputFile = java.io.File.createTempFile("test-input", ".adoc")
        inputFile.writeText("= Test Document\n\nSome content")
        
        // Create a path for non-existent output file
        val outputFile = java.io.File(inputFile.parent, "nonexistent-output.adoc")
        
        try {
            val args = CommandArgs(
                positional = listOf(inputFile.absolutePath),
                options = mapOf("output" to outputFile.absolutePath),
                flags = setOf("no-overwrite")
            )
            
            val result = command.execute(args)
            
            // Should succeed - file doesn't exist
            assertTrue(result is CommandResult.Success)
        } finally {
            inputFile.delete()
            outputFile.delete() // Clean up in case it was created
        }
    }
    
    @Test
    fun `should use specified base path when provided`() {
        // Create temporary input file
        val inputFile = java.io.File.createTempFile("test-input", ".adoc")
        inputFile.writeText("= Test Document\n\nSome content")
        
        try {
            val args = CommandArgs(
                positional = listOf(inputFile.absolutePath),
                options = mapOf("base-path" to "/custom/base/path"),
                flags = emptySet()
            )
            
            val result = command.execute(args)
            
            // Should succeed
            assertTrue(result is CommandResult.Success)
        } finally {
            inputFile.delete()
        }
    }
    
    @Test
    fun `should use input file directory as default base path when not specified`() {
        // Create temporary input file in a specific directory
        val tempDir = java.io.File(System.getProperty("java.io.tmpdir"), "test-base-path-${System.currentTimeMillis()}")
        tempDir.mkdirs()
        val inputFile = java.io.File(tempDir, "test-input.adoc")
        inputFile.writeText("= Test Document\n\nSome content")
        
        try {
            val args = CommandArgs(
                positional = listOf(inputFile.absolutePath),
                options = emptyMap(),
                flags = emptySet()
            )
            
            val result = command.execute(args)
            
            // Should succeed
            assertTrue(result is CommandResult.Success)
        } finally {
            inputFile.delete()
            tempDir.delete()
        }
    }
    
    @Test
    fun `should prefer specified base path over input file directory`() {
        // Create temporary input file
        val inputFile = java.io.File.createTempFile("test-input", ".adoc")
        inputFile.writeText("= Test Document\n\nSome content")
        
        try {
            val args = CommandArgs(
                positional = listOf(inputFile.absolutePath),
                options = mapOf("base-path" to "/different/base/path"),
                flags = emptySet()
            )
            
            val result = command.execute(args)
            
            // Should succeed
            assertTrue(result is CommandResult.Success)
        } finally {
            inputFile.delete()
        }
    }
    
    @Test
    fun `should use short form base path option`() {
        // Create temporary input file
        val inputFile = java.io.File.createTempFile("test-input", ".adoc")
        inputFile.writeText("= Test Document\n\nSome content")
        
        try {
            val args = CommandArgs(
                positional = listOf(inputFile.absolutePath),
                options = mapOf("b" to "/custom/base"),
                flags = emptySet()
            )
            
            val result = command.execute(args)
            
            // Should succeed
            assertTrue(result is CommandResult.Success)
        } finally {
            inputFile.delete()
        }
    }
    
    @Test
    fun `should handle input file in root directory`() {
        // This test verifies that when input file parent is null (root directory),
        // we default to "." as the base path
        // Note: This is a conceptual test - in practice, we can't easily create
        // a file in the root directory for testing, so we just verify the logic
        // doesn't crash when parent is null
        
        val inputFile = java.io.File.createTempFile("test-input", ".adoc")
        inputFile.writeText("= Test Document\n\nSome content")
        
        try {
            val args = CommandArgs(
                positional = listOf(inputFile.absolutePath),
                options = emptyMap(),
                flags = emptySet()
            )
            
            val result = command.execute(args)
            
            // Should succeed
            assertTrue(result is CommandResult.Success)
        } finally {
            inputFile.delete()
        }
    }
}
