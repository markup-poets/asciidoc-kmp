package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.AsgNode
import org.markup.poet.asciidoc.asg.Block
import org.markup.poet.asciidoc.asg.InlineAttributeRef
import org.markup.poet.asciidoc.asg.InlineText
import org.markup.poet.asciidoc.asg.Location
import org.markup.poet.asciidoc.asg.SectionBlock
import org.markup.poet.asciidoc.asg.inlineListsOf
import org.markup.poet.asciidoc.asg.metadataOf
import org.markup.poet.asciidoc.asg.plainText
import org.markup.poet.asciidoc.asg.visitBlocks
import org.markup.poet.asciidoc.asg.visitInlines

/**
 * Default implementation of DocumentValidator.
 * Validates document structure including section hierarchy, duplicate anchors, and invalid references.
 */
class DefaultDocumentValidator : DocumentValidator {

    override fun validate(document: AsgDocument, config: ValidationConfig): ValidationResult {
        val errors = mutableListOf<ProcessingError>()
        val warnings = mutableListOf<ProcessingWarning>()

        // Validate section hierarchy
        if (config.checkSectionHierarchy) {
            validateSectionHierarchy(document, warnings)
        }

        // Check for duplicate anchors
        if (config.checkDuplicateAnchors) {
            checkDuplicateAnchors(document, errors)
        }

        // Check for invalid attribute references
        if (config.checkInvalidReferences) {
            checkInvalidAttributeReferences(document, warnings)
        }

        // Normalize whitespace
        // Note: Whitespace normalization is typically done during parsing or rendering,
        // but we can check for common issues here
        checkWhitespaceIssues(document, warnings)

        return ValidationResult.fromIssues(errors, warnings)
    }

    /**
     * Validates that section levels follow proper hierarchy (no skipped levels).
     * Reports violations with location information.
     */
    private fun validateSectionHierarchy(
        document: AsgDocument,
        warnings: MutableList<ProcessingWarning>
    ) {
        var previousLevel: Int? = null

        visitBlocks(document.blocks) { block ->
            if (block is SectionBlock) {
                val prev = previousLevel
                if (prev != null) {
                    val levelDiff = block.level - prev
                    if (levelDiff > 1) {
                        warnings.add(
                            ProcessingWarning(
                                message = "Section level ${block.level} skips from level $prev (expected level ${prev + 1})",
                                location = block.location,
                                warningType = ProcessingWarningType.SECTION_HIERARCHY_VIOLATION
                            )
                        )
                    }
                }
                previousLevel = block.level
            }
        }
    }

    /**
     * Checks for duplicate anchor IDs in the document.
     * Reuses logic from cross-reference resolver.
     */
    private fun checkDuplicateAnchors(
        document: AsgDocument,
        errors: MutableList<ProcessingError>
    ) {
        val anchorOccurrences = mutableMapOf<String, MutableList<AsgNode>>()

        visitBlocks(document.blocks) { block ->
            val anchorId = metadataOf(block)?.id
            if (anchorId != null) {
                anchorOccurrences.getOrPut(anchorId) { mutableListOf() }.add(block)
            }
        }

        // Report duplicate anchors as errors
        for ((anchorId, nodes) in anchorOccurrences) {
            if (nodes.size > 1) {
                val locations = nodes.joinToString(", ") {
                    "line ${it.location?.start?.line ?: "?"}"
                }
                errors.add(
                    ProcessingError(
                        message = "Duplicate anchor ID '$anchorId' found at: $locations",
                        location = nodes.first().location,
                        errorType = ProcessingErrorType.VALIDATION_DUPLICATE_ANCHOR,
                        severity = ErrorSeverity.ERROR
                    )
                )
            }
        }
    }

    /**
     * Checks for invalid attribute references in the document.
     * Collects all issues and reports them in a single validation report.
     */
    private fun checkInvalidAttributeReferences(
        document: AsgDocument,
        warnings: MutableList<ProcessingWarning>
    ) {
        val invalidReferences = mutableListOf<Pair<String, Location?>>()

        visitBlocks(document.blocks) { block ->
            inlineListsOf(block).forEach { inlines ->
                visitInlines(inlines) { inline ->
                    if (inline is InlineAttributeRef && !document.attributes.containsKey(inline.name)) {
                        invalidReferences.add(inline.name to inline.location)
                    }
                }
            }
        }

        // Report all invalid references
        for ((key, location) in invalidReferences) {
            warnings.add(
                ProcessingWarning(
                    message = "Invalid attribute reference '{$key}' - attribute is not defined",
                    location = location,
                    warningType = ProcessingWarningType.ATTRIBUTE_UNDEFINED
                )
            )
        }
    }

    /**
     * Checks for common whitespace issues in the document.
     * Reports warnings for normalization opportunities.
     */
    private fun checkWhitespaceIssues(
        document: AsgDocument,
        warnings: MutableList<ProcessingWarning>
    ) {
        visitBlocks(document.blocks) { block ->
            if (block is SectionBlock) {
                checkSectionTitleWhitespace(block, warnings)
            } else {
                inlineListsOf(block).forEach { inlines ->
                    visitInlines(inlines) { inline ->
                        if (inline is InlineText && inline.value.contains("  ")) {
                            warnings.add(
                                ProcessingWarning(
                                    message = "Text contains multiple consecutive spaces",
                                    location = inline.location,
                                    warningType = ProcessingWarningType.WHITESPACE_NORMALIZATION
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Checks a section title for leading or trailing whitespace.
     */
    private fun checkSectionTitleWhitespace(
        section: SectionBlock,
        warnings: MutableList<ProcessingWarning>
    ) {
        val title = plainText(section.title)
        if (title.startsWith(" ") || title.endsWith(" ")) {
            warnings.add(
                ProcessingWarning(
                    message = "Section title has leading or trailing whitespace: '$title'",
                    location = section.location,
                    warningType = ProcessingWarningType.WHITESPACE_NORMALIZATION
                )
            )
        }
    }
}
