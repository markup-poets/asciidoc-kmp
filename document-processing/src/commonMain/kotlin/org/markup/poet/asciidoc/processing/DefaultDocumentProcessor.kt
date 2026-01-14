package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.ast.Document
import org.markup.poet.asciidoc.ast.SourceLocation

/**
 * Default implementation of DocumentProcessor that orchestrates the processing pipeline.
 * 
 * Executes processors in the following order:
 * 1. Include Resolver - Embeds external content
 * 2. Attribute Substitutor - Resolves attribute references
 * 3. Macro Expander - Expands macros
 * 4. Cross-Reference Resolver - Resolves internal references
 * 5. TOC Generator - Generates table of contents
 * 6. Document Validator - Validates final structure
 */
class DefaultDocumentProcessor(
    private val includeResolver: IncludeResolver,
    private val attributeSubstitutor: AttributeSubstitutor,
    private val macroExpander: MacroExpander,
    private val crossReferenceResolver: CrossReferenceResolver,
    private val tocGenerator: TocGenerator,
    private val documentValidator: DocumentValidator
) : DocumentProcessor {
    
    override fun process(document: Document, config: ProcessingConfig): ProcessingResult {
        // Validate configuration first
        val configErrors = validateConfiguration(config)
        if (configErrors.isNotEmpty()) {
            return ProcessingResult(
                document = document,
                errors = configErrors,
                warnings = emptyList()
            )
        }
        
        val errors = mutableListOf<ProcessingError>()
        val warnings = mutableListOf<ProcessingWarning>()
        var currentDoc = document
        var shouldHalt = false
        
        // 1. Include resolution
        if (config.enableIncludes && !shouldHalt) {
            try {
                val includeConfig = IncludeConfig(
                    maxDepth = config.maxIncludeDepth,
                    basePath = "",
                    fileReader = createDefaultFileReader()
                )
                val includeResult = includeResolver.resolve(currentDoc, includeConfig)
                currentDoc = includeResult.document
                errors.addAll(includeResult.errors)
                
                // Check for fatal errors
                if (includeResult.errors.any { it.severity == ErrorSeverity.FATAL }) {
                    shouldHalt = true
                }
            } catch (e: Exception) {
                errors.add(createFatalError("Include resolution failed: ${e.message}"))
                shouldHalt = true
            }
        }
        
        // 2. Attribute substitution
        if (config.enableAttributeSubstitution && !shouldHalt) {
            try {
                val attributeConfig = AttributeConfig(
                    defaults = config.attributeDefaults,
                    maxRecursionDepth = 10,
                    undefinedBehavior = UndefinedAttributeBehavior.PRESERVE
                )
                val subResult = attributeSubstitutor.substitute(currentDoc, attributeConfig)
                currentDoc = subResult.document
                errors.addAll(subResult.errors)
                
                // Check for fatal errors
                if (subResult.errors.any { it.severity == ErrorSeverity.FATAL }) {
                    shouldHalt = true
                }
            } catch (e: Exception) {
                errors.add(createFatalError("Attribute substitution failed: ${e.message}"))
                shouldHalt = true
            }
        }
        
        // 3. Macro expansion
        if (config.enableMacroExpansion && !shouldHalt) {
            try {
                val macroConfig = MacroConfig(
                    customMacros = config.customMacros,
                    enableBuiltinMacros = true
                )
                val macroResult = macroExpander.expand(currentDoc, macroConfig)
                currentDoc = macroResult.document
                errors.addAll(macroResult.errors)
                
                // Check for fatal errors
                if (macroResult.errors.any { it.severity == ErrorSeverity.FATAL }) {
                    shouldHalt = true
                }
            } catch (e: Exception) {
                errors.add(createFatalError("Macro expansion failed: ${e.message}"))
                shouldHalt = true
            }
        }
        
        // 4. Cross-reference resolution
        if (config.enableCrossReferences && !shouldHalt) {
            try {
                val xrefResult = crossReferenceResolver.resolve(currentDoc)
                currentDoc = xrefResult.document
                errors.addAll(xrefResult.errors)
                warnings.addAll(xrefResult.warnings)
                
                // Check for fatal errors
                if (xrefResult.errors.any { it.severity == ErrorSeverity.FATAL }) {
                    shouldHalt = true
                }
            } catch (e: Exception) {
                errors.add(createFatalError("Cross-reference resolution failed: ${e.message}"))
                shouldHalt = true
            }
        }
        
        // 5. TOC generation
        if (config.enableTocGeneration && !shouldHalt) {
            try {
                val tocConfig = TocConfig(
                    maxDepth = config.tocDepth,
                    includeTitle = true
                )
                val tocResult = tocGenerator.generate(currentDoc, tocConfig)
                if (tocResult.tocNode != null) {
                    currentDoc = insertToc(currentDoc, tocResult.tocNode)
                }
                errors.addAll(tocResult.errors)
                
                // Check for fatal errors
                if (tocResult.errors.any { it.severity == ErrorSeverity.FATAL }) {
                    shouldHalt = true
                }
            } catch (e: Exception) {
                errors.add(createFatalError("TOC generation failed: ${e.message}"))
                shouldHalt = true
            }
        }
        
        // 6. Validation (always run unless halted)
        if (!shouldHalt) {
            try {
                val validationConfig = ValidationConfig(
                    strictness = config.validationStrictness,
                    checkSectionHierarchy = true,
                    checkDuplicateAnchors = true,
                    checkInvalidReferences = true
                )
                val validationResult = documentValidator.validate(currentDoc, validationConfig)
                errors.addAll(validationResult.errors)
                warnings.addAll(validationResult.warnings)
            } catch (e: Exception) {
                errors.add(createFatalError("Document validation failed: ${e.message}"))
            }
        }
        
        return ProcessingResult(
            document = currentDoc,
            errors = errors,
            warnings = warnings
        )
    }
    
    /**
     * Validates the processing configuration before starting.
     * Returns a list of configuration errors if any are found.
     */
    private fun validateConfiguration(config: ProcessingConfig): List<ProcessingError> {
        val errors = mutableListOf<ProcessingError>()
        
        // Validate maxIncludeDepth
        if (config.maxIncludeDepth < 1) {
            errors.add(
                ProcessingError(
                    message = "maxIncludeDepth must be at least 1, got ${config.maxIncludeDepth}",
                    location = SourceLocation(0, 0),
                    errorType = ProcessingErrorType.CONFIGURATION_INVALID,
                    severity = ErrorSeverity.FATAL
                )
            )
        }
        
        // Validate tocDepth
        if (config.tocDepth < 1) {
            errors.add(
                ProcessingError(
                    message = "tocDepth must be at least 1, got ${config.tocDepth}",
                    location = SourceLocation(0, 0),
                    errorType = ProcessingErrorType.CONFIGURATION_INVALID,
                    severity = ErrorSeverity.FATAL
                )
            )
        }
        
        return errors
    }
    
    /**
     * Creates a fatal error with a generic source location.
     */
    private fun createFatalError(message: String): ProcessingError {
        return ProcessingError(
            message = message,
            location = SourceLocation(0, 0),
            errorType = ProcessingErrorType.CONFIGURATION_INVALID,
            severity = ErrorSeverity.FATAL
        )
    }
    
    /**
     * Inserts the TOC into the document at the appropriate location.
     * For now, this is a placeholder that returns the document unchanged.
     */
    private fun insertToc(document: Document, tocNode: org.markup.poet.asciidoc.ast.AsciiDocList): Document {
        // TODO: Implement TOC insertion logic
        // This would typically insert the TOC at the beginning of the document or at a designated location
        return document
    }
    
    /**
     * Creates a default FileReader implementation.
     * This is a placeholder - actual implementation would be platform-specific.
     */
    private fun createDefaultFileReader(): FileReader {
        return object : FileReader {
            override fun readFile(path: String): FileReadResult {
                return FileReadResult.Error("FileReader not configured")
            }
        }
    }
}
