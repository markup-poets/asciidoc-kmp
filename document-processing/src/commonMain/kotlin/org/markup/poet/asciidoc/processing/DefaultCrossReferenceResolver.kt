package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.AsgNode
import org.markup.poet.asciidoc.asg.Block
import org.markup.poet.asciidoc.asg.CommentBlock
import org.markup.poet.asciidoc.asg.IncludeBlock
import org.markup.poet.asciidoc.asg.InlineRef
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.LeafBlockName
import org.markup.poet.asciidoc.asg.ListBlock
import org.markup.poet.asciidoc.asg.RefVariant
import org.markup.poet.asciidoc.asg.SectionBlock
import org.markup.poet.asciidoc.asg.inlineListsOf
import org.markup.poet.asciidoc.asg.metadataOf
import org.markup.poet.asciidoc.asg.plainText
import org.markup.poet.asciidoc.asg.visitBlocks
import org.markup.poet.asciidoc.asg.visitInlines

/**
 * Default implementation of CrossReferenceResolver.
 */
class DefaultCrossReferenceResolver : CrossReferenceResolver {

    override fun resolve(document: AsgDocument): CrossReferenceResult {
        val errors = mutableListOf<ProcessingError>()
        val warnings = mutableListOf<ProcessingWarning>()

        // Build anchor index
        val anchorIndex = buildAnchorIndex(document, errors)

        // Report unresolved cross-references
        checkCrossReferences(document, anchorIndex, warnings)

        return CrossReferenceResult(
            document = document,
            errors = errors,
            warnings = warnings,
            resolvedReferences = anchorIndex
        )
    }

    /**
     * Checks all xref inlines in the document against the anchor index.
     * Unresolved references are reported as warnings; resolution itself is
     * tracked in the returned index, the nodes are left untouched.
     */
    private fun checkCrossReferences(
        document: AsgDocument,
        anchorIndex: Map<String, AnchorTarget>,
        warnings: MutableList<ProcessingWarning>
    ) {
        visitBlocks(document.blocks) { block ->
            inlineListsOf(block).forEach { inlines ->
                visitInlines(inlines) { inline ->
                    if (inline is InlineRef && inline.variant == RefVariant.XREF &&
                        !anchorIndex.containsKey(inline.target)
                    ) {
                        warnings.add(
                            ProcessingWarning(
                                message = "Unresolved cross-reference to '${inline.target}'",
                                location = inline.location,
                                warningType = ProcessingWarningType.CROSS_REFERENCE_UNRESOLVED
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * Builds an index of all anchors in the document.
     * Detects duplicate anchors and reports them as errors.
     */
    private fun buildAnchorIndex(
        document: AsgDocument,
        errors: MutableList<ProcessingError>
    ): Map<String, AnchorTarget> {
        val anchorIndex = mutableMapOf<String, AnchorTarget>()
        val duplicateAnchors = mutableMapOf<String, MutableList<AsgNode>>()

        visitBlocks(document.blocks) { block ->
            val anchorId = metadataOf(block)?.id ?: return@visitBlocks
            // Track for duplicate detection
            duplicateAnchors.getOrPut(anchorId) { mutableListOf() }.add(block)

            // Add to index (first occurrence wins)
            if (!anchorIndex.containsKey(anchorId)) {
                anchorIndex[anchorId] = AnchorTarget(
                    anchorId = anchorId,
                    targetNode = block,
                    generatedText = generateLinkText(block)
                )
            }
        }

        // Report duplicate anchors as errors
        for ((anchorId, nodes) in duplicateAnchors) {
            if (nodes.size > 1) {
                val locations = nodes.joinToString(", ") {
                    "line ${it.location?.start?.line ?: "?"}"
                }
                errors.add(
                    ProcessingError(
                        message = "Duplicate anchor ID '$anchorId' found at: $locations",
                        location = nodes.first().location,
                        errorType = ProcessingErrorType.CROSS_REFERENCE_DUPLICATE_ANCHOR,
                        severity = ErrorSeverity.ERROR
                    )
                )
            }
        }

        return anchorIndex
    }

    /**
     * Generates appropriate link text based on the target block type.
     */
    private fun generateLinkText(block: Block): String {
        return when (block) {
            is SectionBlock -> plainText(block.title)
            is LeafBlock -> if (block.name == LeafBlockName.PARAGRAPH) {
                plainText(block.inlines).trim().ifEmpty { "[Untitled Paragraph]" }
            } else {
                val lang = block.metadata?.positional?.getOrNull(1) ?: "code"
                "[$lang block]"
            }
            is ListBlock -> {
                val text = block.items.firstOrNull()?.principal?.let { plainText(it) }?.trim().orEmpty()
                text.ifEmpty { "[Untitled List Item]" }
            }
            is CommentBlock -> "[Comment]"
            is IncludeBlock -> "[Include: ${block.path}]"
            else -> "[Untitled]"
        }
    }
}
