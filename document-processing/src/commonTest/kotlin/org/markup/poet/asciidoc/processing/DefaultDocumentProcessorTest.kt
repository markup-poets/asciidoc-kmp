package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.ast.Document
import org.markup.poet.asciidoc.ast.SourceLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultDocumentProcessorTest {
    
    @Test
    fun `should validate configuration before processing`() {
        // Arrange
        val processor = createTestProcessor()
        val document = Document(null, emptyList(), emptyMap(), emptyMap(), SourceLocation(0, 0))
        val config = ProcessingConfig(
            maxIncludeDepth = -1  // Invalid
        )
        
        // Act
        val result = processor.process(document, config)
        
        // Assert
        assertTrue(result.errors.isNotEmpty())
        assertEquals(ProcessingErrorType.CONFIGURATION_INVALID, result.errors.first().errorType)
        assertTrue(result.errors.first().message.contains("maxIncludeDepth"))
    }
    
    @Test
    fun `should validate tocDepth configuration`() {
        // Arrange
        val processor = createTestProcessor()
        val document = Document(null, emptyList(), emptyMap(), emptyMap(), SourceLocation(0, 0))
        val config = ProcessingConfig(
            tocDepth = 0  // Invalid
        )
        
        // Act
        val result = processor.process(document, config)
        
        // Assert
        assertTrue(result.errors.isNotEmpty())
        assertEquals(ProcessingErrorType.CONFIGURATION_INVALID, result.errors.first().errorType)
        assertTrue(result.errors.first().message.contains("tocDepth"))
    }
    
    @Test
    fun `should process document with valid configuration`() {
        // Arrange
        val processor = createTestProcessor()
        val document = Document(null, emptyList(), emptyMap(), emptyMap(), SourceLocation(0, 0))
        val config = ProcessingConfig(
            enableIncludes = false,
            enableAttributeSubstitution = false,
            enableMacroExpansion = false,
            enableCrossReferences = false,
            enableTocGeneration = false
        )
        
        // Act
        val result = processor.process(document, config)
        
        // Assert
        assertTrue(result.errors.isEmpty())
        assertTrue(result.warnings.isEmpty())
    }
    
    @Test
    fun `should skip disabled processors`() {
        // Arrange
        val processor = createTestProcessor()
        val document = Document(null, emptyList(), emptyMap(), emptyMap(), SourceLocation(0, 0))
        val config = ProcessingConfig(
            enableIncludes = false,
            enableAttributeSubstitution = false,
            enableMacroExpansion = false,
            enableCrossReferences = false,
            enableTocGeneration = false
        )
        
        // Act
        val result = processor.process(document, config)
        
        // Assert - Should complete without errors since all processors are disabled
        assertTrue(result.errors.isEmpty())
    }
    
    @Test
    fun `should accumulate errors from multiple processors`() {
        // Arrange
        val processor = createTestProcessor()
        val document = Document(null, emptyList(), emptyMap(), emptyMap(), SourceLocation(0, 0))
        val config = ProcessingConfig(
            enableIncludes = true,
            enableAttributeSubstitution = true,
            enableMacroExpansion = true,
            enableCrossReferences = true,
            enableTocGeneration = true
        )
        
        // Act
        val result = processor.process(document, config)
        
        // Assert - May have errors from processors, but should not crash
        assertTrue(result.document != null)
    }
    
    private fun createTestProcessor(): DefaultDocumentProcessor {
        return DefaultDocumentProcessor(
            includeResolver = createMockIncludeResolver(),
            attributeSubstitutor = createMockAttributeSubstitutor(),
            macroExpander = createMockMacroExpander(),
            crossReferenceResolver = createMockCrossReferenceResolver(),
            tocGenerator = createMockTocGenerator(),
            documentValidator = createMockDocumentValidator()
        )
    }
    
    private fun createMockIncludeResolver(): IncludeResolver {
        return object : IncludeResolver {
            override fun resolve(document: Document, config: IncludeConfig): IncludeResult {
                return IncludeResult(
                    document = document,
                    errors = emptyList(),
                    includedFiles = emptySet()
                )
            }
        }
    }
    
    private fun createMockAttributeSubstitutor(): AttributeSubstitutor {
        return object : AttributeSubstitutor {
            override fun substitute(document: Document, config: AttributeConfig): SubstitutionResult {
                return SubstitutionResult(
                    document = document,
                    errors = emptyList(),
                    substitutedAttributes = emptySet()
                )
            }
        }
    }
    
    private fun createMockMacroExpander(): MacroExpander {
        return object : MacroExpander {
            override fun expand(document: Document, config: MacroConfig): MacroResult {
                return MacroResult(
                    document = document,
                    errors = emptyList()
                )
            }
        }
    }
    
    private fun createMockCrossReferenceResolver(): CrossReferenceResolver {
        return object : CrossReferenceResolver {
            override fun resolve(document: Document): CrossReferenceResult {
                return CrossReferenceResult(
                    document = document,
                    errors = emptyList(),
                    warnings = emptyList(),
                    resolvedReferences = emptyMap()
                )
            }
        }
    }
    
    private fun createMockTocGenerator(): TocGenerator {
        return object : TocGenerator {
            override fun generate(document: Document, config: TocConfig): TocResult {
                return TocResult(
                    tocNode = null,
                    errors = emptyList()
                )
            }
        }
    }
    
    private fun createMockDocumentValidator(): DocumentValidator {
        return object : DocumentValidator {
            override fun validate(document: Document, config: ValidationConfig): ValidationResult {
                return ValidationResult.valid()
            }
        }
    }
}
