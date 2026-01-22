package org.markup.poet.antora.assembler

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DocumentAssemblerTest {
    
    @Test
    fun `placeholder test for module setup`() {
        // This test ensures the test infrastructure is working
        // Real tests will be added in later tasks
        assertNotNull(AssemblerErrorType.INDEX_FILE_NOT_FOUND)
    }
    
    @Test
    fun `should have DocumentAssembler interface with assemble method`() {
        // Create a simple test implementation
        val testAssembler = object : DocumentAssembler {
            override fun assemble(config: AssemblerConfig): AssemblerResult {
                return AssemblerResult(
                    success = true,
                    outputPath = config.outputFile,
                    errors = emptyList(),
                    warnings = emptyList(),
                    includedFiles = setOf(config.indexFile)
                )
            }
        }
        
        val config = AssemblerConfig(
            indexFile = "test.adoc",
            outputFile = "output.adoc",
            componentRoot = "."
        )
        
        val result = testAssembler.assemble(config)
        
        assertTrue(result.success)
        assertEquals("output.adoc", result.outputPath)
        assertTrue(result.errors.isEmpty())
        assertTrue(result.warnings.isEmpty())
        assertEquals(1, result.includedFiles.size)
    }
    
    @Test
    fun `should support error result from assembler`() {
        val testAssembler = object : DocumentAssembler {
            override fun assemble(config: AssemblerConfig): AssemblerResult {
                return AssemblerResult(
                    success = false,
                    outputPath = null,
                    errors = listOf(
                        AssemblerError(
                            message = "Index file not found",
                            filePath = config.indexFile,
                            lineNumber = null,
                            errorType = AssemblerErrorType.INDEX_FILE_NOT_FOUND
                        )
                    ),
                    warnings = emptyList(),
                    includedFiles = emptySet()
                )
            }
        }
        
        val config = AssemblerConfig(
            indexFile = "missing.adoc",
            outputFile = "output.adoc",
            componentRoot = "."
        )
        
        val result = testAssembler.assemble(config)
        
        assertFalse(result.success)
        assertEquals(1, result.errors.size)
        assertEquals(AssemblerErrorType.INDEX_FILE_NOT_FOUND, result.errors[0].errorType)
    }
}
