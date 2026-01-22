package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.ast.*

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
    
    override fun process(document: Document): CalloutResult {
        val errors = mutableListOf<ProcessingError>()
        val warnings = mutableListOf<ProcessingWarning>()
        val calloutsByBlock = mutableMapOf<String, List<CalloutInfo>>()
        
        // Process the document to find code blocks and their associated callout lists
        processBlocks(document.children, errors, warnings, calloutsByBlock)
        
        return CalloutResult(
            document = document,
            errors = errors,
            warnings = warnings,
            calloutsByBlock = calloutsByBlock
        )
    }
    
    /**
     * Process a list of block elements to find code blocks and callout lists.
     */
    private fun processBlocks(
        blocks: List<BlockElement>,
        errors: MutableList<ProcessingError>,
        warnings: MutableList<ProcessingWarning>,
        calloutsByBlock: MutableMap<String, List<CalloutInfo>>
    ) {
        var i = 0
        while (i < blocks.size) {
            val block = blocks[i]
            
            when (block) {
                is CodeBlock -> {
                    // Extract callouts from the code block
                    val callouts = extractCallouts(block)
                    
                    if (callouts.isNotEmpty()) {
                        // Look for a callout list immediately following this code block
                        val nextBlock = blocks.getOrNull(i + 1)
                        
                        if (nextBlock is CalloutList) {
                            // Associate the callout list with the code block
                            val associatedCallouts = associateCallouts(
                                callouts,
                                nextBlock,
                                block.sourceLocation,
                                warnings
                            )
                            
                            // Generate a unique ID for this code block
                            val blockId = generateBlockId(block)
                            calloutsByBlock[blockId] = associatedCallouts
                        } else {
                            // No callout list found - create callouts without explanations
                            val blockId = generateBlockId(block)
                            calloutsByBlock[blockId] = callouts.map { it.copy(explanation = null) }
                            
                            warnings.add(
                                ProcessingWarning(
                                    message = "Code block contains callout markers but no callout list follows",
                                    location = block.sourceLocation,
                                    warningType = ProcessingWarningType.CALLOUT_MISMATCH
                                )
                            )
                        }
                    }
                }
                
                is CalloutList -> {
                    // Check if this callout list is orphaned (not preceded by a code block)
                    val prevBlock = blocks.getOrNull(i - 1)
                    if (prevBlock !is CodeBlock) {
                        errors.add(
                            ProcessingError(
                                message = "Callout list found without a preceding code block",
                                location = block.sourceLocation,
                                errorType = ProcessingErrorType.CALLOUT_INVALID_CONTEXT,
                                severity = ErrorSeverity.ERROR
                            )
                        )
                    }
                }
                
                is Section -> {
                    // Recursively process section children
                    processBlocks(block.children, errors, warnings, calloutsByBlock)
                }
                
                is AdmonitionBlock -> {
                    // Recursively process admonition content
                    processBlocks(block.content, errors, warnings, calloutsByBlock)
                }
                
                is ConditionalDirective -> {
                    // Recursively process conditional content
                    processBlocks(block.content, errors, warnings, calloutsByBlock)
                    processBlocks(block.elseContent, errors, warnings, calloutsByBlock)
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
    private fun extractCallouts(codeBlock: CodeBlock): List<CalloutInfo> {
        val callouts = mutableListOf<CalloutInfo>()
        val lines = codeBlock.content.lines()
        val calloutRegex = Regex("""<(\d+)>""")
        
        lines.forEachIndexed { lineIndex, line ->
            // Find all callout markers in this line
            calloutRegex.findAll(line).forEach { match ->
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
     * Associate callout markers with their explanations from a callout list.
     */
    private fun associateCallouts(
        callouts: List<CalloutInfo>,
        calloutList: CalloutList,
        codeBlockLocation: SourceLocation,
        warnings: MutableList<ProcessingWarning>
    ): List<CalloutInfo> {
        val listItemsByNumber = calloutList.items.associateBy { it.number }
        
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
            callout.copy(explanation = listItem?.content)
        }
    }
    
    /**
     * Generate a unique ID for a code block based on its location.
     */
    private fun generateBlockId(codeBlock: CodeBlock): String {
        return "code-${codeBlock.sourceLocation.line}-${codeBlock.sourceLocation.column}"
    }
}
