package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.Block
import org.markup.poet.asciidoc.asg.BlockMacro
import org.markup.poet.asciidoc.asg.BlockMacroName
import org.markup.poet.asciidoc.asg.BlockMetadata
import org.markup.poet.asciidoc.asg.ListBlock
import org.markup.poet.asciidoc.asg.ParentBlock
import org.markup.poet.asciidoc.asg.SectionBlock
import org.markup.poet.asciidoc.asg.visitBlocks

/**
 * Default implementation of DocumentProcessor that orchestrates the processing pipeline.
 *
 * Executes processors in the following order:
 * 1. Include Resolver - Embeds external content
 * 2. Fragment Processor - Extracts tagged sections
 * 3. Conditional Processor - Evaluates conditionals
 * 4. Attribute Substitutor - Resolves attribute references
 * 5. Macro Expander - Expands macros
 * 6. Admonition Processor - Processes admonitions
 * 7. Callout Processor - Processes code callouts
 * 8. Bibliography Manager - Manages footnotes and citations
 * 9. Cross-Reference Resolver - Resolves internal references
 * 10. TOC Generator - Generates and inserts the table of contents
 * 11. Document Validator - Validates final structure
 */
class DefaultDocumentProcessor(
    private val includeResolver: IncludeResolver,
    private val fragmentProcessor: FragmentProcessor,
    private val conditionalProcessor: ConditionalProcessor,
    private val attributeSubstitutor: AttributeSubstitutor,
    private val macroExpander: MacroExpander,
    private val admonitionProcessor: AdmonitionProcessor,
    private val calloutProcessor: CalloutProcessor,
    private val bibliographyManager: BibliographyManager,
    private val crossReferenceResolver: CrossReferenceResolver,
    private val tocGenerator: TocGenerator,
    private val documentValidator: DocumentValidator,
    private val fileReaderFactory: (String) -> FileReader = { path ->
        object : FileReader {
            override fun readFile(path: String): FileReadResult {
                return FileReadResult.Error("FileReader not configured")
            }
        }
    }
) : DocumentProcessor {

    override fun process(document: AsgDocument, config: ProcessingConfig): ProcessingResult {
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
        val sharedData = mutableMapOf<String, Any>()

        // Helper function to execute custom processors for a phase
        fun executeCustomProcessors(phase: ProcessingPhase): Boolean {
            val registry = config.extensionRegistry ?: return false
            val processors = registry.getProcessors(phase)

            for (processor in processors) {
                if (shouldHalt) break

                try {
                    val context = ProcessingContext(
                        config = config,
                        currentPhase = phase,
                        sharedData = sharedData
                    )
                    val result = processor.process(currentDoc, context)

                    // Validate AST modifications
                    val validationErrors = AstValidator.validateDocument(result.document)
                    if (validationErrors.isNotEmpty()) {
                        errors.add(
                            ProcessingError(
                                message = "Custom processor '${processor.name}' produced invalid AST: ${validationErrors.joinToString(", ")}",
                                location = null,
                                errorType = ProcessingErrorType.CONFIGURATION_INVALID,
                                severity = ErrorSeverity.ERROR
                            )
                        )
                        // Continue with next processor, don't use the invalid document
                        continue
                    }

                    currentDoc = result.document
                    errors.addAll(result.errors)
                    warnings.addAll(result.warnings)

                    if (!result.continueProcessing) {
                        shouldHalt = true
                        break
                    }

                    // Check for fatal errors
                    if (result.errors.any { it.severity == ErrorSeverity.FATAL }) {
                        shouldHalt = true
                        break
                    }
                } catch (e: Exception) {
                    // Error isolation: continue processing on custom processor failure
                    errors.add(
                        ProcessingError(
                            message = "Custom processor '${processor.name}' failed: ${e.message}",
                            location = null,
                            errorType = ProcessingErrorType.CONFIGURATION_INVALID,
                            severity = ErrorSeverity.ERROR
                        )
                    )
                    // Continue with next processor
                }
            }

            return shouldHalt
        }

        // Execute PRE_INCLUDE custom processors
        executeCustomProcessors(ProcessingPhase.PRE_INCLUDE)

        // 1. Include resolution
        if (config.enableIncludes && !shouldHalt) {
            try {
                val includeConfig = IncludeConfig(
                    maxDepth = config.maxIncludeDepth,
                    basePath = config.basePath,
                    fileReader = fileReaderFactory(config.basePath)
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

        // Execute POST_INCLUDE custom processors
        executeCustomProcessors(ProcessingPhase.POST_INCLUDE)

        // 2. Fragment processing
        if (config.enableFragmentProcessing && !shouldHalt) {
            try {
                val fragmentConfig = FragmentConfig(
                    tagPrefix = "tag::",
                    tagSuffix = "[]",
                    allowNestedTags = false
                )
                val fragmentResult = fragmentProcessor.processFragments(currentDoc, fragmentConfig)
                currentDoc = fragmentResult.document
                errors.addAll(fragmentResult.errors)
                warnings.addAll(fragmentResult.warnings)

                // Check for fatal errors
                if (fragmentResult.errors.any { it.severity == ErrorSeverity.FATAL }) {
                    shouldHalt = true
                }
            } catch (e: Exception) {
                errors.add(createFatalError("Fragment processing failed: ${e.message}"))
                shouldHalt = true
            }
        }

        // 3. Conditional processing
        if (config.enableConditionalProcessing && !shouldHalt) {
            try {
                val conditionalConfig = ConditionalConfig(
                    definedAttributes = config.attributeDefaults.keys,
                    allowNestedConditionals = true,
                    maxNestingDepth = 10
                )
                val conditionalResult = conditionalProcessor.process(currentDoc, conditionalConfig)
                currentDoc = conditionalResult.document
                errors.addAll(conditionalResult.errors)
                warnings.addAll(conditionalResult.warnings)

                // Check for fatal errors
                if (conditionalResult.errors.any { it.severity == ErrorSeverity.FATAL }) {
                    shouldHalt = true
                }
            } catch (e: Exception) {
                errors.add(createFatalError("Conditional processing failed: ${e.message}"))
                shouldHalt = true
            }
        }

        // Execute PRE_ATTRIBUTE custom processors
        executeCustomProcessors(ProcessingPhase.PRE_ATTRIBUTE)

        // 4. Attribute substitution
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

        // Execute POST_ATTRIBUTE custom processors
        executeCustomProcessors(ProcessingPhase.POST_ATTRIBUTE)

        // Execute PRE_MACRO custom processors
        executeCustomProcessors(ProcessingPhase.PRE_MACRO)

        // 5. Macro expansion
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

        // Execute POST_MACRO custom processors
        executeCustomProcessors(ProcessingPhase.POST_MACRO)

        // 6. Admonition processing
        if (config.enableAdmonitionProcessing && !shouldHalt) {
            try {
                val admonitionResult = admonitionProcessor.process(currentDoc)
                currentDoc = admonitionResult.document
                warnings.addAll(admonitionResult.warnings)
            } catch (e: Exception) {
                errors.add(createFatalError("Admonition processing failed: ${e.message}"))
                shouldHalt = true
            }
        }

        // 7. Callout processing
        if (config.enableCalloutProcessing && !shouldHalt) {
            try {
                val calloutResult = calloutProcessor.process(currentDoc)
                currentDoc = calloutResult.document
                errors.addAll(calloutResult.errors)
                warnings.addAll(calloutResult.warnings)

                // Check for fatal errors
                if (calloutResult.errors.any { it.severity == ErrorSeverity.FATAL }) {
                    shouldHalt = true
                }
            } catch (e: Exception) {
                errors.add(createFatalError("Callout processing failed: ${e.message}"))
                shouldHalt = true
            }
        }

        // 8. Bibliography management
        if (config.enableBibliographyManagement && !shouldHalt) {
            try {
                val bibliographyResult = bibliographyManager.process(currentDoc)
                currentDoc = bibliographyResult.document
                warnings.addAll(bibliographyResult.warnings)
            } catch (e: Exception) {
                errors.add(createFatalError("Bibliography management failed: ${e.message}"))
                shouldHalt = true
            }
        }

        // 9. Cross-reference resolution
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

        // 10. TOC generation. The step runs when the configuration asks for it
        // OR the document itself carries a `:toc:` attribute (asciidoctor
        // semantics); the attribute value picks the placement, the `toclevels`
        // attribute overrides the configured depth.
        val tocAttribute = currentDoc.attributes["toc"]?.trim()?.lowercase()
        if ((config.enableTocGeneration || tocAttribute != null) && !shouldHalt) {
            try {
                val tocDepth = currentDoc.attributes["toclevels"]?.trim()?.toIntOrNull()
                    ?.takeIf { it >= 1 }
                    ?: config.tocDepth
                val tocConfig = TocConfig(
                    maxDepth = tocDepth,
                    includeTitle = true
                )
                val tocResult = tocGenerator.generate(currentDoc, tocConfig)
                if (tocResult.tocNode != null) {
                    currentDoc = insertToc(currentDoc, tocResult.tocNode, tocPlacement(tocAttribute))
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

        // Execute PRE_VALIDATION custom processors
        executeCustomProcessors(ProcessingPhase.PRE_VALIDATION)

        // 11. Validation (always run unless halted)
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

        // Execute POST_VALIDATION custom processors
        executeCustomProcessors(ProcessingPhase.POST_VALIDATION)

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
                    location = null,
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
                    location = null,
                    errorType = ProcessingErrorType.CONFIGURATION_INVALID,
                    severity = ErrorSeverity.FATAL
                )
            )
        }

        return errors
    }

    /**
     * Creates a fatal error with no source location.
     */
    private fun createFatalError(message: String): ProcessingError {
        return ProcessingError(
            message = message,
            location = null,
            errorType = ProcessingErrorType.CONFIGURATION_INVALID,
            severity = ErrorSeverity.FATAL
        )
    }

    /** Where the generated TOC lands in the document, per the `toc` attribute. */
    private enum class TocPlacement {
        /** After the document header, i.e. before all body blocks. */
        AUTO,

        /** After the preamble: before the first top-level section. */
        PREAMBLE,

        /** In place of a `toc::[]` block macro; without one, no TOC is inserted. */
        MACRO,
    }

    /**
     * Maps the `toc` document attribute value to a placement. An absent
     * attribute (config-driven generation), an empty value (`:toc:`), `auto`,
     * and any unsupported position (e.g. `left`) all place the TOC after the
     * document header.
     */
    private fun tocPlacement(tocAttribute: String?): TocPlacement = when (tocAttribute) {
        "preamble" -> TocPlacement.PREAMBLE
        "macro" -> TocPlacement.MACRO
        else -> TocPlacement.AUTO
    }

    /**
     * Inserts the generated TOC into the document at the location selected by
     * [placement]. The list is marked with id [TOC_ID] and a `toc` role so the
     * HTML renderer emits `<ul id="toc" class="... toc">`. Insertion is
     * idempotent: when the document already contains a list carrying the TOC
     * id (e.g. a consumer runs the processor twice), it is returned unchanged.
     */
    private fun insertToc(document: AsgDocument, tocNode: ListBlock, placement: TocPlacement): AsgDocument {
        if (containsTocList(document.blocks)) {
            return document
        }

        val metadata = tocNode.metadata ?: BlockMetadata()
        val toc = tocNode.copy(
            metadata = metadata.copy(
                id = TOC_ID,
                roles = if (TOC_ROLE in metadata.roles) metadata.roles else metadata.roles + TOC_ROLE,
            )
        )

        return when (placement) {
            TocPlacement.AUTO -> document.copy(blocks = listOf(toc) + document.blocks)
            TocPlacement.PREAMBLE -> {
                val firstSection = document.blocks.indexOfFirst { it is SectionBlock }
                val insertAt = if (firstSection >= 0) firstSection else document.blocks.size
                document.copy(
                    blocks = document.blocks.take(insertAt) + toc + document.blocks.drop(insertAt)
                )
            }
            TocPlacement.MACRO -> {
                val (blocks, replaced) = replaceTocMacro(document.blocks, toc)
                if (replaced) document.copy(blocks = blocks) else document
            }
        }
    }

    /** Whether [blocks] already contain an inserted TOC list (id [TOC_ID]). */
    private fun containsTocList(blocks: List<Block>): Boolean {
        var found = false
        visitBlocks(blocks) { block ->
            if (block is ListBlock && block.metadata?.id == TOC_ID) {
                found = true
            }
        }
        return found
    }

    /**
     * Replaces the first `toc::[]` block macro in [blocks] with [toc],
     * recursing into sections and parent blocks (the macro may sit inside a
     * container). Returns the rewritten blocks and whether a macro was found;
     * untouched subtrees are returned as-is.
     */
    private fun replaceTocMacro(blocks: List<Block>, toc: ListBlock): Pair<List<Block>, Boolean> {
        var replaced = false

        fun rewrite(list: List<Block>): List<Block> = list.map { block ->
            if (replaced) return@map block
            when {
                block is BlockMacro && block.name == BlockMacroName.TOC -> {
                    replaced = true
                    toc
                }
                block is SectionBlock -> block.copy(blocks = rewrite(block.blocks))
                block is ParentBlock -> block.copy(blocks = rewrite(block.blocks))
                else -> block
            }
        }

        val rewritten = rewrite(blocks)
        return rewritten to replaced
    }

    private companion object {
        /** Anchor id and role marking the inserted TOC list for styling. */
        const val TOC_ID = "toc"
        const val TOC_ROLE = "toc"
    }
}
