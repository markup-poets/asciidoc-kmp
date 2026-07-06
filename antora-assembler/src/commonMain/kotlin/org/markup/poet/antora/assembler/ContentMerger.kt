package org.markup.poet.antora.assembler

import org.markup.poet.antora.AntoraResolver
import org.markup.poet.antora.FileReadResult
import org.markup.poet.antora.FileSystemAccess
import org.markup.poet.antora.ResolutionContext
import org.markup.poet.antora.ResolutionResult
import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.AsgNode
import org.markup.poet.asciidoc.asg.Block
import org.markup.poet.asciidoc.asg.ConditionalBlock
import org.markup.poet.asciidoc.asg.DListBlock
import org.markup.poet.asciidoc.asg.DiscreteHeading
import org.markup.poet.asciidoc.asg.IncludeBlock
import org.markup.poet.asciidoc.asg.Inline
import org.markup.poet.asciidoc.asg.InlineMacro
import org.markup.poet.asciidoc.asg.InlineRef
import org.markup.poet.asciidoc.asg.InlineSpan
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.ListBlock
import org.markup.poet.asciidoc.asg.ParentBlock
import org.markup.poet.asciidoc.asg.RefVariant
import org.markup.poet.asciidoc.asg.SectionBlock
import org.markup.poet.asciidoc.asg.metadataOf
import org.markup.poet.asciidoc.parser.AsciidocParser

/**
 * Merges included content into a single document.
 * Handles attribute merging, cross-reference resolution, and path updates.
 */
class ContentMerger(
    private val resolver: AntoraResolver,
    private val parser: AsciidocParser,
    private val fileSystem: FileSystemAccess
) {
    // Registry of all anchors in the assembled document
    private val anchorRegistry = mutableMapOf<String, AnchorInfo>()
    /**
     * Merge all includes in the document recursively.
     *
     * @param document The document to process
     * @param context The resolution context for the document
     * @param config The assembler configuration
     * @return MergeResult containing the merged document and any warnings/errors
     */
    fun merge(
        document: AsgDocument,
        context: ResolutionContext,
        config: AssemblerConfig
    ): MergeResult {
        val warnings = mutableListOf<AssemblerWarning>()
        val errors = mutableListOf<AssemblerError>()
        val visited = mutableSetOf<String>()

        // Add the current document to visited set
        context.currentFilePath?.let { visited.add(it) }

        // Track merged attributes (first definition wins)
        val mergedAttributes = document.attributes.toMutableMap()

        // Clear anchor registry for this merge operation
        anchorRegistry.clear()

        // Process all includes recursively
        val processedBlocks = processBlocks(
            document.blocks,
            context,
            config,
            visited,
            0,
            warnings,
            errors,
            mergedAttributes
        )

        // Build anchor registry from the processed document tree
        // This happens after includes are resolved so we have all anchors
        buildAnchorRegistry(processedBlocks, context.currentFilePath ?: "")

        // Second pass: Resolve cross-references in the processed document
        val resolvedBlocks = resolveXrefsInBlocks(processedBlocks, warnings)

        // Create the merged document with processed blocks and merged attributes
        val mergedDocument = document.copy(
            blocks = resolvedBlocks,
            attributes = mergedAttributes
        )

        return MergeResult(
            document = mergedDocument,
            warnings = warnings,
            errors = errors
        )
    }

    /**
     * Process a list of blocks, resolving any include directives.
     */
    private fun processBlocks(
        blocks: List<Block>,
        context: ResolutionContext,
        config: AssemblerConfig,
        visited: MutableSet<String>,
        depth: Int,
        warnings: MutableList<AssemblerWarning>,
        errors: MutableList<AssemblerError>,
        mergedAttributes: MutableMap<String, String>
    ): List<Block> {
        val result = mutableListOf<Block>()

        for (block in blocks) {
            when (block) {
                is IncludeBlock -> {
                    // Process the include directive
                    val includedBlocks = processInclude(
                        block,
                        context,
                        config,
                        visited,
                        depth,
                        warnings,
                        errors,
                        mergedAttributes
                    )
                    result.addAll(includedBlocks)
                }
                else -> {
                    // For other blocks, recursively process their children if they have any
                    val processedBlock = processBlock(
                        block,
                        context,
                        config,
                        visited,
                        depth,
                        warnings,
                        errors,
                        mergedAttributes
                    )
                    result.add(processedBlock)
                }
            }
        }

        return result
    }

    /**
     * Process a single block, recursively processing any nested blocks.
     */
    private fun processBlock(
        block: Block,
        context: ResolutionContext,
        config: AssemblerConfig,
        visited: MutableSet<String>,
        depth: Int,
        warnings: MutableList<AssemblerWarning>,
        errors: MutableList<AssemblerError>,
        mergedAttributes: MutableMap<String, String>
    ): Block {
        fun recurse(nested: List<Block>): List<Block> =
            processBlocks(nested, context, config, visited, depth, warnings, errors, mergedAttributes)

        return when (block) {
            is SectionBlock -> block.copy(blocks = recurse(block.blocks))
            is ParentBlock -> block.copy(blocks = recurse(block.blocks))
            is ConditionalBlock -> block.copy(
                blocks = recurse(block.blocks),
                elseBlocks = recurse(block.elseBlocks)
            )
            else -> block
        }
    }

    /**
     * Process an include directive by resolving the path, reading the file,
     * parsing it, and recursively processing any nested includes.
     */
    private fun processInclude(
        directive: IncludeBlock,
        context: ResolutionContext,
        config: AssemblerConfig,
        visited: MutableSet<String>,
        depth: Int,
        warnings: MutableList<AssemblerWarning>,
        errors: MutableList<AssemblerError>,
        mergedAttributes: MutableMap<String, String>
    ): List<Block> {
        val directiveLine = directive.location?.start?.line

        // Check depth limit
        if (depth >= config.maxDepth) {
            errors.add(
                AssemblerError(
                    message = "Maximum include depth (${config.maxDepth}) exceeded at ${directive.path}",
                    filePath = context.currentFilePath,
                    lineNumber = directiveLine,
                    errorType = AssemblerErrorType.MAX_DEPTH_EXCEEDED
                )
            )
            return emptyList()
        }

        // Resolve the include path
        val resolutionResult = resolver.resolveInclude(directive.path, context)

        when (resolutionResult) {
            is ResolutionResult.Error -> {
                val error = AssemblerError(
                    message = "Failed to resolve include '${directive.path}': ${resolutionResult.message}",
                    filePath = context.currentFilePath,
                    lineNumber = directiveLine,
                    errorType = AssemblerErrorType.INCLUDE_NOT_FOUND
                )
                errors.add(error)

                if (config.failOnMissingIncludes) {
                    return emptyList()
                } else {
                    warnings.add(
                        AssemblerWarning(
                            message = error.message,
                            filePath = error.filePath,
                            lineNumber = error.lineNumber
                        )
                    )
                    return emptyList()
                }
            }
            is ResolutionResult.Success -> {
                val resolvedPath = resolutionResult.resolvedPath

                // Check for circular dependency
                if (resolvedPath in visited) {
                    val cycle = visited.toList() + resolvedPath
                    val error = AssemblerError(
                        message = "Circular dependency detected: ${cycle.joinToString(" -> ")}",
                        filePath = context.currentFilePath,
                        lineNumber = directiveLine,
                        errorType = AssemblerErrorType.CIRCULAR_DEPENDENCY
                    )
                    errors.add(error)

                    if (config.failOnCircularDependencies) {
                        return emptyList()
                    } else {
                        warnings.add(
                            AssemblerWarning(
                                message = error.message,
                                filePath = error.filePath,
                                lineNumber = error.lineNumber
                            )
                        )
                        return emptyList()
                    }
                }

                // Read the file
                val fileContent = when (val readResult = fileSystem.readFile(resolvedPath)) {
                    is FileReadResult.Success -> readResult.content
                    is FileReadResult.Error -> {
                        errors.add(
                            AssemblerError(
                                message = "Failed to read file '$resolvedPath': ${readResult.message}",
                                filePath = context.currentFilePath,
                                lineNumber = directiveLine,
                                errorType = AssemblerErrorType.INCLUDE_NOT_FOUND
                            )
                        )
                        return emptyList()
                    }
                }

                // Apply line range filtering if specified
                val lineRange = directive.lineRange
                val filteredContent = if (lineRange != null) {
                    filterLineRange(fileContent, lineRange)
                } else {
                    fileContent
                }

                // Apply tag filtering if specified
                val tagFilteredContent = if (directive.attributes.containsKey("tags")) {
                    val tags = directive.attributes["tags"]?.split(",")?.map { it.trim() } ?: emptyList()
                    filterTags(filteredContent, tags)
                } else {
                    filteredContent
                }

                // Apply indentation if the include directive is indented
                // (ASG columns are 1-based: column 1 means no indentation)
                val indentLevel = ((directive.location?.start?.col ?: 1) - 1).coerceAtLeast(0)
                val indentedContent = if (indentLevel > 0) {
                    applyIndentation(tagFilteredContent, indentLevel)
                } else {
                    tagFilteredContent
                }

                // Parse the included file
                val parseResult = parser.parseToAsg(indentedContent)

                // Report any parse errors
                parseResult.errors.forEach { parseError ->
                    errors.add(
                        AssemblerError(
                            message = "Parse error in included file '$resolvedPath': ${parseError.message}",
                            filePath = resolvedPath,
                            lineNumber = parseError.location.line,
                            errorType = AssemblerErrorType.PARSE_ERROR
                        )
                    )
                }

                // Report any parse warnings
                parseResult.warnings.forEach { parseWarning ->
                    warnings.add(
                        AssemblerWarning(
                            message = "Parse warning in included file '$resolvedPath': ${parseWarning.message}",
                            filePath = resolvedPath,
                            lineNumber = parseWarning.location.line
                        )
                    )
                }

                // Merge attributes from the included document (first definition wins)
                mergeDocumentAttributes(
                    mergedAttributes,
                    parseResult.document.attributes,
                    resolvedPath,
                    warnings
                )

                // Add the resolved path to visited set
                visited.add(resolvedPath)

                // Create a new context for the included file
                val includedContext = context.withFile(resolvedPath)

                // Recursively process the included document's blocks
                val processedBlocks = processBlocks(
                    parseResult.document.blocks,
                    includedContext,
                    config,
                    visited,
                    depth + 1,
                    warnings,
                    errors,
                    mergedAttributes
                )

                // Remove the resolved path from visited set (backtrack)
                visited.remove(resolvedPath)

                return processedBlocks
            }
        }
    }

    /**
     * Filter content by line range.
     */
    private fun filterLineRange(content: String, lineRange: IntRange): String {
        val lines = content.lines()
        val startLine = (lineRange.first - 1).coerceIn(0, lines.size)
        val endLine = lineRange.last.coerceIn(0, lines.size)

        return lines.subList(startLine, endLine).joinToString("\n")
    }

    /**
     * Filter content by tags.
     * Tags are marked with comments like: // tag::tagname[] and // end::tagname[]
     * Also supports AsciiDoc comment syntax: # tag::tagname[] and # end::tagname[]
     */
    private fun filterTags(content: String, tags: List<String>): String {
        val lines = content.lines()
        val result = mutableListOf<String>()
        val activeTagStack = mutableListOf<String>()

        for (line in lines) {
            // Check for tag start (supports //, #, and other comment styles)
            val tagStartMatch = Regex("""[/#]*\s*tag::(\w+)\[\]""").find(line)
            if (tagStartMatch != null) {
                val tagName = tagStartMatch.groupValues[1]
                activeTagStack.add(tagName)
                continue
            }

            // Check for tag end
            val tagEndMatch = Regex("""[/#]*\s*end::(\w+)\[\]""").find(line)
            if (tagEndMatch != null) {
                val tagName = tagEndMatch.groupValues[1]
                if (activeTagStack.isNotEmpty() && activeTagStack.last() == tagName) {
                    activeTagStack.removeAt(activeTagStack.size - 1)
                }
                continue
            }

            // Include line if we're inside one of the requested tags
            if (activeTagStack.any { it in tags }) {
                result.add(line)
            }
        }

        return result.joinToString("\n")
    }

    /**
     * Apply indentation to all lines of content.
     * This preserves the relative indentation of the included content
     * while adding the base indentation level from the include directive.
     *
     * @param content The content to indent
     * @param indentLevel The number of spaces to add to each line
     * @return The indented content
     */
    private fun applyIndentation(content: String, indentLevel: Int): String {
        if (indentLevel <= 0) return content

        val indent = " ".repeat(indentLevel)
        val lines = content.lines()

        return lines.joinToString("\n") { line ->
            if (line.isBlank()) {
                line // Don't indent blank lines
            } else {
                indent + line
            }
        }
    }

    /**
     * Merge document attributes from an included file into the main attribute map.
     * Implements first-definition-wins conflict resolution strategy.
     *
     * @param target The target attribute map (will be modified in place)
     * @param source The source attributes from the included document
     * @param sourceFile The path of the included file (for warning messages)
     * @param warnings List to collect warnings about attribute conflicts
     */
    private fun mergeDocumentAttributes(
        target: MutableMap<String, String>,
        source: Map<String, String>,
        sourceFile: String,
        warnings: MutableList<AssemblerWarning>
    ) {
        for ((key, value) in source) {
            if (target.containsKey(key)) {
                // Attribute already exists - first definition wins
                // Only emit a warning if the values are different
                if (target[key] != value) {
                    warnings.add(
                        AssemblerWarning(
                            message = "Attribute '$key' already defined with value '${target[key]}', ignoring value '$value' from included file",
                            filePath = sourceFile,
                            lineNumber = null
                        )
                    )
                }
            } else {
                // New attribute - add it
                target[key] = value
            }
        }
    }

    /**
     * Build a registry of all anchors in the document tree.
     * This allows us to track which anchors exist and where they came from.
     */
    private fun buildAnchorRegistry(blocks: List<Block>, sourceFile: String) {
        for (block in blocks) {
            // Check if block has an anchor ID in its metadata
            val anchorId = metadataOf(block)?.id
            if (anchorId != null && !anchorRegistry.containsKey(anchorId)) {
                anchorRegistry[anchorId] = AnchorInfo(
                    anchorId = anchorId,
                    sourceFile = sourceFile,
                    element = block
                )
            }

            // Recursively process nested blocks
            when (block) {
                is SectionBlock -> buildAnchorRegistry(block.blocks, sourceFile)
                is ParentBlock -> buildAnchorRegistry(block.blocks, sourceFile)
                is ConditionalBlock -> {
                    buildAnchorRegistry(block.blocks, sourceFile)
                    buildAnchorRegistry(block.elseBlocks, sourceFile)
                }
                is ListBlock -> block.items.forEach { buildAnchorRegistry(it.blocks, sourceFile) }
                is DListBlock -> block.items.forEach { buildAnchorRegistry(it.blocks, sourceFile) }
                else -> {
                    // Other block types don't have children
                }
            }
        }
    }

    /**
     * Resolve cross-references in a list of blocks.
     * This converts Antora xref syntax to simple anchor references and validates references.
     */
    private fun resolveXrefsInBlocks(
        blocks: List<Block>,
        warnings: MutableList<AssemblerWarning>
    ): List<Block> {
        return blocks.map { resolveXrefsInBlock(it, warnings) }
    }

    /**
     * Resolve cross-references in a single block.
     */
    private fun resolveXrefsInBlock(
        block: Block,
        warnings: MutableList<AssemblerWarning>
    ): Block {
        fun resolveInlines(inlines: List<Inline>): List<Inline> =
            inlines.map { resolveXrefsInInline(it, warnings) }

        return when (block) {
            is SectionBlock -> block.copy(
                title = resolveInlines(block.title),
                blocks = resolveXrefsInBlocks(block.blocks, warnings)
            )
            is LeafBlock -> block.copy(
                inlines = resolveInlines(block.inlines)
            )
            is DiscreteHeading -> block.copy(
                title = resolveInlines(block.title)
            )
            is ListBlock -> block.copy(
                items = block.items.map { item ->
                    item.copy(
                        principal = resolveInlines(item.principal),
                        blocks = resolveXrefsInBlocks(item.blocks, warnings)
                    )
                }
            )
            is DListBlock -> block.copy(
                items = block.items.map { item ->
                    item.copy(
                        terms = item.terms.map { resolveInlines(it) },
                        principal = resolveInlines(item.principal),
                        blocks = resolveXrefsInBlocks(item.blocks, warnings)
                    )
                }
            )
            is ParentBlock -> block.copy(
                blocks = resolveXrefsInBlocks(block.blocks, warnings)
            )
            is ConditionalBlock -> block.copy(
                blocks = resolveXrefsInBlocks(block.blocks, warnings),
                elseBlocks = resolveXrefsInBlocks(block.elseBlocks, warnings)
            )
            else -> block
        }
    }

    /**
     * Resolve cross-references in an inline node.
     * Handles xref nodes by validating them against the anchor registry.
     */
    private fun resolveXrefsInInline(
        inline: Inline,
        warnings: MutableList<AssemblerWarning>
    ): Inline {
        return when (inline) {
            is InlineRef -> when (inline.variant) {
                RefVariant.XREF -> {
                    val resolvedTarget = validateXrefTarget(inline.target, inline.location?.start?.line, warnings)
                    if (resolvedTarget != inline.target) {
                        inline.copy(target = resolvedTarget)
                    } else {
                        inline
                    }
                }
                RefVariant.LINK -> inline.copy(
                    inlines = inline.inlines.map { resolveXrefsInInline(it, warnings) }
                )
            }
            // The parser may surface xrefs as generic inline macros before they
            // are claimed downstream; resolve their target the same way.
            is InlineMacro -> if (inline.name == "xref") {
                val resolvedTarget = validateXrefTarget(inline.target, inline.location?.start?.line, warnings)
                if (resolvedTarget != inline.target) {
                    inline.copy(target = resolvedTarget)
                } else {
                    inline
                }
            } else {
                inline
            }
            is InlineSpan -> inline.copy(
                inlines = inline.inlines.map { resolveXrefsInInline(it, warnings) }
            )
            else -> inline
        }
    }

    /**
     * Resolve an xref target against the anchor registry, warning when the
     * resolved anchor does not exist in the assembled document.
     */
    private fun validateXrefTarget(
        target: String,
        line: Int?,
        warnings: MutableList<AssemblerWarning>
    ): String {
        // Parse the target ID to check if it's an Antora xref
        val resolvedTargetId = resolveAntoraXref(target)

        // Check if the anchor exists in the registry
        if (!anchorRegistry.containsKey(resolvedTargetId)) {
            warnings.add(
                AssemblerWarning(
                    message = "Cross-reference target '$resolvedTargetId' not found in assembled document",
                    filePath = null,
                    lineNumber = line
                )
            )
        }

        return resolvedTargetId
    }

    /**
     * Resolve Antora xref syntax to simple anchor references.
     * Antora xrefs can have formats like:
     * - page$filename.adoc#anchor
     * - module:page$filename.adoc#anchor
     * - #anchor (simple anchor reference)
     *
     * This method extracts the anchor part and returns it as a simple reference.
     */
    private fun resolveAntoraXref(targetId: String): String {
        // If it's already a simple anchor reference (starts with #), return it without the #
        if (targetId.startsWith("#")) {
            return targetId.substring(1)
        }

        // Check if it contains an Antora coordinate (contains $ or :)
        if (targetId.contains("$") || targetId.contains(":")) {
            // Extract the anchor part after #
            val anchorIndex = targetId.indexOf("#")
            if (anchorIndex >= 0) {
                return targetId.substring(anchorIndex + 1)
            }
            // If no anchor specified, use the filename as the anchor
            // This handles cases like page$filename.adoc
            val dollarIndex = targetId.lastIndexOf("$")
            if (dollarIndex >= 0) {
                val filename = targetId.substring(dollarIndex + 1)
                // Remove .adoc extension if present
                return filename.removeSuffix(".adoc")
            }
        }

        // Otherwise, return as-is (it's already a simple anchor reference)
        return targetId
    }
}

/**
 * Information about an anchor in the assembled document.
 */
data class AnchorInfo(
    val anchorId: String,
    val sourceFile: String,
    val element: AsgNode
)

data class MergeResult(
    val document: AsgDocument? = null,
    val warnings: List<AssemblerWarning>,
    val errors: List<AssemblerError> = emptyList()
)
