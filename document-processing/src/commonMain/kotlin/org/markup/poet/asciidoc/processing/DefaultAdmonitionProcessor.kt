package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.Block
import org.markup.poet.asciidoc.asg.BlockMetadata
import org.markup.poet.asciidoc.asg.ConditionalBlock
import org.markup.poet.asciidoc.asg.DListBlock
import org.markup.poet.asciidoc.asg.InlineText
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.LeafBlockName
import org.markup.poet.asciidoc.asg.ListBlock
import org.markup.poet.asciidoc.asg.ParentBlock
import org.markup.poet.asciidoc.asg.ParentBlockName
import org.markup.poet.asciidoc.asg.SectionBlock

/**
 * Default implementation of AdmonitionProcessor.
 * Processes admonition blocks by identifying types, extracting content,
 * handling custom titles, and validating structure.
 *
 * Admonitions already parsed into [ParentBlock]s with name ADMONITION are
 * counted; paragraphs using the inline (`NOTE: text`) or style (`[NOTE]`)
 * forms are upgraded into admonition parent blocks.
 */
class DefaultAdmonitionProcessor : AdmonitionProcessor {

    /** Valid admonition variants (ASG spelling, lowercase). */
    private val admonitionVariants = setOf("note", "tip", "warning", "caution", "important")

    override fun process(document: AsgDocument): AdmonitionResult {
        val warnings = mutableListOf<ProcessingWarning>()
        val admonitionCounts = mutableMapOf<String, Int>()

        val processedBlocks = processBlocks(document.blocks, warnings, admonitionCounts)

        return AdmonitionResult(
            document = document.copy(blocks = processedBlocks),
            warnings = warnings,
            admonitionCount = admonitionCounts.toMap()
        )
    }

    private fun processBlocks(
        blocks: List<Block>,
        warnings: MutableList<ProcessingWarning>,
        counts: MutableMap<String, Int>
    ): List<Block> {
        val result = mutableListOf<Block>()

        for (block in blocks) {
            when (block) {
                is LeafBlock -> {
                    if (block.name == LeafBlockName.PARAGRAPH) {
                        val admonition = tryParseAdmonition(block, warnings, counts)
                        result.add(admonition ?: block)
                    } else {
                        result.add(block)
                    }
                }
                is SectionBlock -> result.add(
                    block.copy(blocks = processBlocks(block.blocks, warnings, counts))
                )
                is ListBlock -> result.add(
                    block.copy(items = block.items.map { item ->
                        item.copy(blocks = processBlocks(item.blocks, warnings, counts))
                    })
                )
                is DListBlock -> result.add(
                    block.copy(items = block.items.map { item ->
                        item.copy(blocks = processBlocks(item.blocks, warnings, counts))
                    })
                )
                is ParentBlock -> {
                    if (block.name == ParentBlockName.ADMONITION) {
                        // Already an admonition, just count it
                        val variant = block.variant ?: "note"
                        counts[variant] = (counts[variant] ?: 0) + 1
                    }
                    // Process nested content
                    result.add(block.copy(blocks = processBlocks(block.blocks, warnings, counts)))
                }
                is ConditionalBlock -> result.add(
                    block.copy(
                        blocks = processBlocks(block.blocks, warnings, counts),
                        elseBlocks = processBlocks(block.elseBlocks, warnings, counts)
                    )
                )
                else -> result.add(block)
            }
        }

        return result
    }

    /**
     * Try to parse a paragraph as an admonition block.
     * Admonitions in AsciiDoc can be in the form:
     * - NOTE: content
     * - TIP: content
     * - WARNING: content
     * - CAUTION: content
     * - IMPORTANT: content
     *
     * Or with the block style form:
     * - [NOTE]
     * - .Custom Title
     * - content
     */
    private fun tryParseAdmonition(
        paragraph: LeafBlock,
        warnings: MutableList<ProcessingWarning>,
        counts: MutableMap<String, Int>
    ): ParentBlock? {
        // Check if the paragraph carries an admonition block style
        val metadata = paragraph.metadata
        val style = metadata?.positional?.firstOrNull()
        if (metadata != null && style != null) {
            val variant = recognizeAdmonitionVariant(style)
            if (variant != null) {
                // This is an admonition with block syntax
                counts[variant] = (counts[variant] ?: 0) + 1

                return ParentBlock(
                    name = ParentBlockName.ADMONITION,
                    variant = variant,
                    blocks = listOf(paragraph.copy(metadata = null)),
                    metadata = BlockMetadata(
                        positional = metadata.positional.drop(1),
                        named = metadata.named,
                        id = metadata.id,
                        roles = metadata.roles,
                        options = metadata.options,
                        title = metadata.title
                    ),
                    location = paragraph.location
                )
            } else {
                // Invalid admonition type
                warnings.add(
                    ProcessingWarning(
                        message = "Invalid admonition type: $style",
                        location = paragraph.location,
                        warningType = ProcessingWarningType.ADMONITION_INVALID_TYPE
                    )
                )
                return null
            }
        }

        // Check for inline admonition syntax (TYPE: content)
        if (paragraph.inlines.isNotEmpty()) {
            val firstElement = paragraph.inlines.first()
            if (firstElement is InlineText) {
                val text = firstElement.value
                val colonIndex = text.indexOf(':')
                if (colonIndex > 0) {
                    val potentialType = text.substring(0, colonIndex).trim()
                    val variant = recognizeAdmonitionVariant(potentialType)

                    if (variant != null) {
                        counts[variant] = (counts[variant] ?: 0) + 1

                        // Extract content after the colon
                        val contentAfterColon = text.substring(colonIndex + 1).trim()
                        val newContent = if (contentAfterColon.isNotEmpty()) {
                            listOf(InlineText(contentAfterColon, firstElement.location)) +
                                paragraph.inlines.drop(1)
                        } else {
                            paragraph.inlines.drop(1)
                        }

                        return ParentBlock(
                            name = ParentBlockName.ADMONITION,
                            variant = variant,
                            blocks = listOf(paragraph.copy(inlines = newContent, metadata = null)),
                            metadata = paragraph.metadata,
                            location = paragraph.location
                        )
                    }
                }
            }
        }

        return null
    }

    /**
     * Recognize an admonition variant from a style string (case-insensitive).
     * Returns null if the string doesn't match any known admonition type.
     */
    private fun recognizeAdmonitionVariant(typeString: String): String? {
        val variant = typeString.lowercase()
        return if (variant in admonitionVariants) variant else null
    }
}
