package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.Block
import org.markup.poet.asciidoc.asg.ConditionalBlock
import org.markup.poet.asciidoc.asg.DListBlock
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

                // Recursively resolve includes in the parsed content
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

                return processedBlocks
            }
        }
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
