package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.Block
import org.markup.poet.asciidoc.asg.ConditionalBlock
import org.markup.poet.asciidoc.asg.DListBlock
import org.markup.poet.asciidoc.asg.DiscreteHeading
import org.markup.poet.asciidoc.asg.IncludeBlock
import org.markup.poet.asciidoc.asg.ListBlock
import org.markup.poet.asciidoc.asg.ParentBlock
import org.markup.poet.asciidoc.asg.SectionBlock
import org.markup.poet.asciidoc.parser.AsciidocParser

/**
 * Default implementation of IncludeResolver.
 * Resolves include directives by reading files, parsing content, and embedding into the document tree.
 */
class DefaultIncludeResolver(
    private val parser: AsciidocParser
) : IncludeResolver {

    override fun resolve(document: AsgDocument, config: IncludeConfig): IncludeResult {
        val errors = mutableListOf<ProcessingError>()
        val includedFiles = mutableSetOf<String>()

        val processedBlocks = resolveInBlocks(
            blocks = document.blocks,
            config = config,
            currentDepth = 0,
            visitedFiles = mutableSetOf(),
            errors = errors,
            includedFiles = includedFiles,
            currentPath = config.basePath
        )

        return IncludeResult(
            document = document.copy(blocks = processedBlocks),
            errors = errors,
            includedFiles = includedFiles
        )
    }

    private fun resolveInBlocks(
        blocks: List<Block>,
        config: IncludeConfig,
        currentDepth: Int,
        visitedFiles: MutableSet<String>,
        errors: MutableList<ProcessingError>,
        includedFiles: MutableSet<String>,
        currentPath: String
    ): List<Block> = blocks.flatMap { block ->
        processBlock(block, config, currentDepth, visitedFiles, errors, includedFiles, currentPath)
    }

    private fun processBlock(
        block: Block,
        config: IncludeConfig,
        currentDepth: Int,
        visitedFiles: MutableSet<String>,
        errors: MutableList<ProcessingError>,
        includedFiles: MutableSet<String>,
        currentPath: String
    ): List<Block> {
        fun recurse(nested: List<Block>): List<Block> =
            resolveInBlocks(nested, config, currentDepth, visitedFiles, errors, includedFiles, currentPath)

        return when (block) {
            is IncludeBlock -> resolveIncludeDirective(
                directive = block,
                config = config,
                currentDepth = currentDepth,
                visitedFiles = visitedFiles,
                errors = errors,
                includedFiles = includedFiles,
                currentPath = currentPath
            )
            is SectionBlock -> listOf(block.copy(blocks = recurse(block.blocks)))
            is ParentBlock -> listOf(block.copy(blocks = recurse(block.blocks)))
            is ConditionalBlock -> listOf(
                block.copy(blocks = recurse(block.blocks), elseBlocks = recurse(block.elseBlocks))
            )
            is ListBlock -> listOf(
                block.copy(items = block.items.map { it.copy(blocks = recurse(it.blocks)) })
            )
            is DListBlock -> listOf(
                block.copy(items = block.items.map { it.copy(blocks = recurse(it.blocks)) })
            )
            else -> listOf(block)
        }
    }

    private fun resolveIncludeDirective(
        directive: IncludeBlock,
        config: IncludeConfig,
        currentDepth: Int,
        visitedFiles: MutableSet<String>,
        errors: MutableList<ProcessingError>,
        includedFiles: MutableSet<String>,
        currentPath: String
    ): List<Block> {
        // Check depth limit
        if (currentDepth >= config.maxDepth) {
            errors.add(
                ProcessingError(
                    message = "Include depth exceeded maximum of ${config.maxDepth}",
                    location = directive.location,
                    errorType = ProcessingErrorType.INCLUDE_MAX_DEPTH_EXCEEDED
                )
            )
            return emptyList()
        }

        // Resolve path
        val resolvedPath = resolvePath(directive.path, currentPath)

        // Check for circular dependency
        if (visitedFiles.contains(resolvedPath)) {
            errors.add(
                ProcessingError(
                    message = "Circular include dependency detected: $resolvedPath",
                    location = directive.location,
                    errorType = ProcessingErrorType.INCLUDE_CIRCULAR_DEPENDENCY
                )
            )
            return emptyList()
        }

        // Read file
        val fileResult = config.fileReader.readFile(resolvedPath)
        when (fileResult) {
            is FileReadResult.Error -> {
                errors.add(
                    ProcessingError(
                        message = "Failed to read include file '$resolvedPath': ${fileResult.message}",
                        location = directive.location,
                        errorType = ProcessingErrorType.INCLUDE_NOT_FOUND
                    )
                )
                return emptyList()
            }
            is FileReadResult.Success -> {
                // Apply line range filtering if specified
                val lineRange = directive.lineRange
                val content = if (lineRange != null) {
                    filterLineRange(fileResult.content, lineRange)
                } else {
                    fileResult.content
                }

                // Parse included content
                val parseResult = parser.parse(content)

                // Track this file as visited
                visitedFiles.add(resolvedPath)
                includedFiles.add(resolvedPath)

                // Get the directory of the included file for resolving nested includes
                val includedFilePath = getDirectoryPath(resolvedPath)

                // Recursively resolve includes in the parsed content. Nested includes resolve
                // (and, per below, get their own leveloffset applied) before this include's own
                // offset is applied to the result, so relative offsets compound correctly through
                // a chain of includes.
                val processedBlocks = resolveInBlocks(
                    blocks = parseResult.document.blocks,
                    config = config,
                    currentDepth = currentDepth + 1,
                    visitedFiles = visitedFiles,
                    errors = errors,
                    includedFiles = includedFiles,
                    currentPath = includedFilePath
                )

                // Remove from visited set to allow the same file to be included in different branches
                visitedFiles.remove(resolvedPath)

                // Apply leveloffset (if any) to the fully-resolved included content, and fold the
                // included document's own title (if any) into a section at the offset level instead
                // of silently discarding it.
                val offset = parseLevelOffset(directive.attributes)
                val shiftedBlocks = applyLevelOffset(processedBlocks, offset)
                val header = parseResult.document.header

                return if (header != null) {
                    listOf(
                        SectionBlock(
                            title = header.title,
                            level = offset.coerceAtLeast(0),
                            blocks = shiftedBlocks,
                            location = header.location,
                        )
                    )
                } else {
                    shiftedBlocks
                }
            }
        }
    }

    /**
     * Parses the `leveloffset` include attribute, e.g. `include::x.adoc[leveloffset=+1]`.
     * Both the relative form (`+N`/`-N`) and the bare absolute form (`N`) parse to the same
     * signed offset here and are applied identically (additive) -- this does not implement
     * Asciidoctor's true "absolute levels are not context-aware" semantics for nested includes
     * (where an absolute value would override rather than compound); that would need the offset
     * to be threaded top-down instead of applied bottom-up at each include's own splice point.
     * Absolute leveloffset is documented upstream as awkward/discouraged with nested includes
     * anyway, so this covers the common, well-defined relative case fully.
     */
    private fun parseLevelOffset(attributes: Map<String, String>): Int =
        attributes["leveloffset"]?.trim()?.toIntOrNull() ?: 0

    /**
     * Shifts every section/discrete-heading level in [blocks] by [offset], recursing into every
     * block type that can nest content. Mirrors [processBlock]'s own recursive-copy structure.
     * Levels are floored at 0 (a heading can't go negative); no ceiling is applied here -- an
     * offset that pushes a level out of the document's valid range is left for validation to flag,
     * matching how rendering already degrades gracefully for out-of-range levels.
     */
    private fun applyLevelOffset(blocks: List<Block>, offset: Int): List<Block> {
        if (offset == 0) return blocks
        return blocks.map { shiftBlockLevel(it, offset) }
    }

    private fun shiftBlockLevel(block: Block, offset: Int): Block = when (block) {
        is SectionBlock -> block.copy(
            level = (block.level + offset).coerceAtLeast(0),
            blocks = applyLevelOffset(block.blocks, offset),
        )
        is DiscreteHeading -> block.copy(level = (block.level + offset).coerceAtLeast(0))
        is ParentBlock -> block.copy(blocks = applyLevelOffset(block.blocks, offset))
        is ConditionalBlock -> block.copy(
            blocks = applyLevelOffset(block.blocks, offset),
            elseBlocks = applyLevelOffset(block.elseBlocks, offset),
        )
        is ListBlock -> block.copy(items = block.items.map { it.copy(blocks = applyLevelOffset(it.blocks, offset)) })
        is DListBlock -> block.copy(items = block.items.map { it.copy(blocks = applyLevelOffset(it.blocks, offset)) })
        else -> block
    }

    /**
     * Resolve a file path relative to the current path.
     * Handles both relative and absolute paths.
     */
    private fun resolvePath(path: String, currentPath: String): String {
        return if (isAbsolutePath(path)) {
            path
        } else {
            if (currentPath.isEmpty()) {
                path
            } else {
                "$currentPath/$path"
            }
        }
    }

    /**
     * Check if a path is absolute.
     * A path is considered absolute if it starts with '/' or contains a drive letter (Windows).
     */
    private fun isAbsolutePath(path: String): Boolean {
        return path.startsWith("/") ||
               (path.length >= 2 && path[1] == ':') // Windows drive letter
    }

    /**
     * Extract the directory path from a file path.
     */
    private fun getDirectoryPath(filePath: String): String {
        val lastSlash = filePath.lastIndexOf('/')
        return if (lastSlash >= 0) {
            filePath.substring(0, lastSlash)
        } else {
            ""
        }
    }

    /**
     * Filter content to include only the specified line range.
     */
    private fun filterLineRange(content: String, lineRange: IntRange): String {
        val lines = content.lines()
        val startLine = (lineRange.first - 1).coerceIn(0, lines.size)
        val endLine = (lineRange.last - 1).coerceIn(0, lines.size)

        return if (startLine <= endLine && startLine < lines.size) {
            lines.subList(startLine, minOf(endLine + 1, lines.size)).joinToString("\n")
        } else {
            ""
        }
    }
}
