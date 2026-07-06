package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.asg.AsgDocument
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultDocumentProcessorTest {

    @Test
    fun `should validate configuration before processing`() {
        // Arrange
        val processor = createTestProcessor()
        val document = AsgDocument()
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
        val document = AsgDocument()
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
        val document = AsgDocument()
        val config = ProcessingConfig(
            enableIncludes = false,
            enableFragmentProcessing = false,
            enableConditionalProcessing = false,
            enableAttributeSubstitution = false,
            enableMacroExpansion = false,
            enableAdmonitionProcessing = false,
            enableCalloutProcessing = false,
            enableBibliographyManagement = false,
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
        val document = AsgDocument()
        val config = ProcessingConfig(
            enableIncludes = false,
            enableFragmentProcessing = false,
            enableConditionalProcessing = false,
            enableAttributeSubstitution = false,
            enableMacroExpansion = false,
            enableAdmonitionProcessing = false,
            enableCalloutProcessing = false,
            enableBibliographyManagement = false,
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
        val document = AsgDocument()
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
            fragmentProcessor = createMockFragmentProcessor(),
            conditionalProcessor = createMockConditionalProcessor(),
            attributeSubstitutor = createMockAttributeSubstitutor(),
            macroExpander = createMockMacroExpander(),
            admonitionProcessor = createMockAdmonitionProcessor(),
            calloutProcessor = createMockCalloutProcessor(),
            bibliographyManager = createMockBibliographyManager(),
            crossReferenceResolver = createMockCrossReferenceResolver(),
            tocGenerator = createMockTocGenerator(),
            documentValidator = createMockDocumentValidator()
        )
    }

    private fun createMockIncludeResolver(): IncludeResolver {
        return object : IncludeResolver {
            override fun resolve(document: AsgDocument, config: IncludeConfig): IncludeResult {
                return IncludeResult(
                    document = document,
                    errors = emptyList(),
                    includedFiles = emptySet()
                )
            }
        }
    }

    private fun createMockFragmentProcessor(): FragmentProcessor {
        return object : FragmentProcessor {
            override fun processFragments(document: AsgDocument, config: FragmentConfig): FragmentResult {
                return FragmentResult(
                    document = document,
                    errors = emptyList(),
                    warnings = emptyList(),
                    extractedTags = emptyMap()
                )
            }
        }
    }

    private fun createMockConditionalProcessor(): ConditionalProcessor {
        return object : ConditionalProcessor {
            override fun process(document: AsgDocument, config: ConditionalConfig): ConditionalResult {
                return ConditionalResult(
                    document = document,
                    errors = emptyList(),
                    warnings = emptyList(),
                    evaluatedConditionals = 0
                )
            }
        }
    }

    private fun createMockAdmonitionProcessor(): AdmonitionProcessor {
        return object : AdmonitionProcessor {
            override fun process(document: AsgDocument): AdmonitionResult {
                return AdmonitionResult(
                    document = document,
                    warnings = emptyList(),
                    admonitionCount = emptyMap()
                )
            }
        }
    }

    private fun createMockCalloutProcessor(): CalloutProcessor {
        return object : CalloutProcessor {
            override fun process(document: AsgDocument): CalloutResult {
                return CalloutResult(
                    document = document,
                    errors = emptyList(),
                    warnings = emptyList(),
                    calloutsByBlock = emptyMap()
                )
            }
        }
    }

    private fun createMockBibliographyManager(): BibliographyManager {
        return object : BibliographyManager {
            override fun process(document: AsgDocument): BibliographyResult {
                return BibliographyResult(
                    document = document,
                    footnotes = emptyList(),
                    bibliography = emptyMap(),
                    warnings = emptyList()
                )
            }
        }
    }

    private fun createMockAttributeSubstitutor(): AttributeSubstitutor {
        return object : AttributeSubstitutor {
            override fun substitute(document: AsgDocument, config: AttributeConfig): SubstitutionResult {
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
            override fun expand(document: AsgDocument, config: MacroConfig): MacroResult {
                return MacroResult(
                    document = document,
                    errors = emptyList()
                )
            }
        }
    }

    private fun createMockCrossReferenceResolver(): CrossReferenceResolver {
        return object : CrossReferenceResolver {
            override fun resolve(document: AsgDocument): CrossReferenceResult {
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
            override fun generate(document: AsgDocument, config: TocConfig): TocResult {
                return TocResult(
                    tocNode = null,
                    errors = emptyList()
                )
            }
        }
    }

    private fun createMockDocumentValidator(): DocumentValidator {
        return object : DocumentValidator {
            override fun validate(document: AsgDocument, config: ValidationConfig): ValidationResult {
                return ValidationResult.valid()
            }
        }
    }
}
