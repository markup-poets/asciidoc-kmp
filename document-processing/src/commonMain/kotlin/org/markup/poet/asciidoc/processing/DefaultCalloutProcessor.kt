package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.Block
import org.markup.poet.asciidoc.asg.ConditionalBlock
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.LeafBlockName
import org.markup.poet.asciidoc.asg.ListBlock
import org.markup.poet.asciidoc.asg.ListItem
import org.markup.poet.asciidoc.asg.ListVariant
import org.markup.poet.asciidoc.asg.Location
import org.markup.poet.asciidoc.asg.ParentBlock
import org.markup.poet.asciidoc.asg.SectionBlock
import org.markup.poet.asciidoc.asg.plainText

/**
 * Default implementation of CalloutProcessor.
 *
 * Processes callouts in code blocks by:
 * 1. Extracting callout markers from code block content
 * 2. Numbering markers sequentially within each code block
 * 3. Associating callout lists with their corresponding code blocks
 * 4. Validating marker-explanation matching
 * 5. Maintaining separate sequences per code block
 */
class DefaultCalloutProcessor : CalloutProcessor {

    private val calloutMarkerRegex = Regex("""<(\d+)>""")

    override fun process(document: AsgDocument): CalloutResult {
        val errors = mutableListOf<ProcessingError>()
        val warnings = mutableListOf<ProcessingWarning>()
        val calloutsByBlock = mutableMapOf<String, List<CalloutInfo>>()

        // Process the document to find code blocks and their associated callout lists
        processBlocks(document.blocks, errors, warnings, calloutsByBlock)

        return CalloutResult(
            document = document,
            errors = errors,
            warnings = warnings,
            calloutsByBlock = calloutsByBlock
        )
    }

    /** True for verbatim leaf blocks (listing/literal/...) that can carry callout markers. */
    private fun isCodeBlock(block: Block): Boolean =
        block is LeafBlock && block.name != LeafBlockName.PARAGRAPH

    private fun isCalloutList(block: Block): Boolean =
        block is ListBlock && block.variant == ListVariant.CALLOUT

    /**
     * Process a list of blocks to find code blocks and callout lists.
     */
    private fun processBlocks(
        blocks: List<Block>,
        errors: MutableList<ProcessingError>,
        warnings: MutableList<ProcessingWarning>,
        calloutsByBlock: MutableMap<String, List<CalloutInfo>>
    ) {
        var i = 0
        while (i < blocks.size) {
            val block = blocks[i]

            when {
                isCodeBlock(block) -> {
                    val codeBlock = block as LeafBlock
                    // Extract callouts from the code block
                    val callouts = extractCallouts(codeBlock)

                    if (callouts.isNotEmpty()) {
                        // Look for a callout list immediately following this code block
                        val nextBlock = blocks.getOrNull(i + 1)

                        if (nextBlock is ListBlock && nextBlock.variant == ListVariant.CALLOUT) {
                            // Associate the callout list with the code block
                            val associatedCallouts = associateCallouts(
                                callouts,
                                nextBlock,
                                codeBlock.location,
                                warnings
                            )

                            // Generate a unique ID for this code block
                            calloutsByBlock[generateBlockId(codeBlock)] = associatedCallouts
                        } else {
                            // No callout list found - create callouts without explanations
                            calloutsByBlock[generateBlockId(codeBlock)] =
                                callouts.map { it.copy(explanation = null) }

                            warnings.add(
                                ProcessingWarning(
                                    message = "Code block contains callout markers but no callout list follows",
                                    location = codeBlock.location,
                                    warningType = ProcessingWarningType.CALLOUT_MISMATCH
                                )
                            )
                        }
                    }
                }

                isCalloutList(block) -> {
                    // Check if this callout list is orphaned (not preceded by a code block)
                    val prevBlock = blocks.getOrNull(i - 1)
                    if (prevBlock == null || !isCodeBlock(prevBlock)) {
                        errors.add(
                            ProcessingError(
                                message = "Callout list found without a preceding code block",
                                location = block.location,
                                errorType = ProcessingErrorType.CALLOUT_INVALID_CONTEXT,
                                severity = ErrorSeverity.ERROR
                            )
                        )
                    }
                }

                block is SectionBlock ->
                    processBlocks(block.blocks, errors, warnings, calloutsByBlock)

                block is ParentBlock ->
                    processBlocks(block.blocks, errors, warnings, calloutsByBlock)

                block is ConditionalBlock -> {
                    processBlocks(block.blocks, errors, warnings, calloutsByBlock)
                    processBlocks(block.elseBlocks, errors, warnings, calloutsByBlock)
                }

                else -> {
                    // Other block types don't contain nested blocks we need to process
                }
            }

            i++
        }
    }

    /**
     * Extract callout markers from a code block's content.
     * Callout markers are in the format <1>, <2>, etc.
     */
    private fun extractCallouts(codeBlock: LeafBlock): List<CalloutInfo> {
        val callouts = mutableListOf<CalloutInfo>()
        val lines = plainText(codeBlock.inlines).lines()

        lines.forEachIndexed { lineIndex, line ->
            // Find all callout markers in this line
            calloutMarkerRegex.findAll(line).forEach { match ->
                val number = match.groupValues[1].toInt()
                callouts.add(
                    CalloutInfo(
                        number = number,
                        marker = match.value,
                        lineNumber = lineIndex + 1,
                        explanation = null
                    )
                )
            }
        }

        // Sort by number to ensure sequential ordering
        return callouts.sortedBy { it.number }
    }

    /**
     * The callout number of a callout-list item, derived from its `<n>` marker.
     */
    private fun itemNumber(item: ListItem, fallback: Int): Int =
        calloutMarkerRegex.matchEntire(item.marker.trim())?.groupValues?.get(1)?.toInt() ?: fallback

    /**
     * Associate callout markers with their explanations from a callout list.
     */
    private fun associateCallouts(
        callouts: List<CalloutInfo>,
        calloutList: ListBlock,
        codeBlockLocation: Location?,
        warnings: MutableList<ProcessingWarning>
    ): List<CalloutInfo> {
        val listItemsByNumber = calloutList.items
            .mapIndexed { index, item -> itemNumber(item, index + 1) to item }
            .toMap()

        // Check for mismatches
        val calloutNumbers = callouts.map { it.number }.toSet()
        val listNumbers = listItemsByNumber.keys

        if (calloutNumbers != listNumbers) {
            val missing = calloutNumbers - listNumbers
            val extra = listNumbers - calloutNumbers

            val message = buildString {
                append("Callout markers and list items don't match")
                if (missing.isNotEmpty()) {
                    append(". Missing explanations for: ${missing.sorted().joinToString(", ")}")
                }
                if (extra.isNotEmpty()) {
                    append(". Extra explanations for: ${extra.sorted().joinToString(", ")}")
                }
            }

            warnings.add(
                ProcessingWarning(
                    message = message,
                    location = codeBlockLocation,
                    warningType = ProcessingWarningType.CALLOUT_MISMATCH
                )
            )
        }

        // Associate explanations with callouts
        return callouts.map { callout ->
            val listItem = listItemsByNumber[callout.number]
            callout.copy(explanation = listItem?.principal)
        }
    }

    /**
     * Generate a unique ID for a code block based on its location.
     */
    private fun generateBlockId(codeBlock: LeafBlock): String {
        val start = codeBlock.location?.start
        return "code-${start?.line ?: 0}-${start?.col ?: 0}"
    }
}
