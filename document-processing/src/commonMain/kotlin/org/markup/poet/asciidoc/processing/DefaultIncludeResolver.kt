package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.ast.*
import org.markup.poet.asciidoc.parser.AsciidocParser

/**
 * Default implementation of IncludeResolver.
 * Resolves include directives by reading files, parsing content, and embedding into the document tree.
 */
class DefaultIncludeResolver(
    private val parser: AsciidocParser
) : IncludeResolver {
    
    override fun resolve(document: Document, config: IncludeConfig): IncludeResult {
        val errors = mutableListOf<ProcessingError>()
        val includedFiles = mutableSetOf<String>()
        
        val processedDocument = resolveIncludes(
            document = document,
            config = config,
            currentDepth = 0,
            visitedFiles = mutableSetOf(),
            errors = errors,
            includedFiles = includedFiles,
            currentPath = config.basePath
        )
        
        return IncludeResult(
            document = processedDocument,
            errors = errors,
            includedFiles = includedFiles
        )
    }
    
    private fun resolveIncludes(
        document: Document,
        config: IncludeConfig,
        currentDepth: Int,
        visitedFiles: MutableSet<String>,
        errors: MutableList<ProcessingError>,
        includedFiles: MutableSet<String>,
        currentPath: String
    ): Document {
        val processedChildren = document.children.flatMap { child ->
            processBlockElement(
                element = child,
                config = config,
                currentDepth = currentDepth,
                visitedFiles = visitedFiles,
                errors = errors,
                includedFiles = includedFiles,
                currentPath = currentPath
            )
        }
        
        return document.copy(children = processedChildren)
    }
    
    private fun processBlockElement(
        element: BlockElement,
        config: IncludeConfig,
        currentDepth: Int,
        visitedFiles: MutableSet<String>,
        errors: MutableList<ProcessingError>,
        includedFiles: MutableSet<String>,
        currentPath: String
    ): List<BlockElement> {
        return when (element) {
            is IncludeDirective -> {
                resolveIncludeDirective(
                    directive = element,
                    config = config,
                    currentDepth = currentDepth,
                    visitedFiles = visitedFiles,
                    errors = errors,
                    includedFiles = includedFiles,
                    currentPath = currentPath
                )
            }
            is Section -> {
                val processedChildren = element.children.flatMap { child ->
                    processBlockElement(
                        element = child,
                        config = config,
                        currentDepth = currentDepth,
                        visitedFiles = visitedFiles,
                        errors = errors,
                        includedFiles = includedFiles,
                        currentPath = currentPath
                    )
                }
                listOf(element.copy(children = processedChildren))
            }
            is AsciiDocList -> {
                val processedItems = element.items.map { item ->
                    processListItem(
                        item = item,
                        config = config,
                        currentDepth = currentDepth,
                        visitedFiles = visitedFiles,
                        errors = errors,
                        includedFiles = includedFiles,
                        currentPath = currentPath
                    )
                }
                listOf(element.copy(items = processedItems))
            }
            is CalloutList -> {
                listOf(element)
            }
            else -> listOf(element)
        }
    }
    
    private fun processListItem(
        item: ListItem,
        config: IncludeConfig,
        currentDepth: Int,
        visitedFiles: MutableSet<String>,
        errors: MutableList<ProcessingError>,
        includedFiles: MutableSet<String>,
        currentPath: String
    ): ListItem {
        val processedNestedList = item.nestedList?.let { nestedList ->
            val processedItems = nestedList.items.map { nestedItem ->
                processListItem(
                    item = nestedItem,
                    config = config,
                    currentDepth = currentDepth,
                    visitedFiles = visitedFiles,
                    errors = errors,
                    includedFiles = includedFiles,
                    currentPath = currentPath
                )
            }
            nestedList.copy(items = processedItems)
        }
        
        return item.copy(nestedList = processedNestedList)
    }
    
    private fun resolveIncludeDirective(
        directive: IncludeDirective,
        config: IncludeConfig,
        currentDepth: Int,
        visitedFiles: MutableSet<String>,
        errors: MutableList<ProcessingError>,
        includedFiles: MutableSet<String>,
        currentPath: String
    ): List<BlockElement> {
        // Check depth limit
        if (currentDepth >= config.maxDepth) {
            errors.add(
                ProcessingError(
                    message = "Include depth exceeded maximum of ${config.maxDepth}",
                    location = directive.sourceLocation,
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
                    location = directive.sourceLocation,
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
                        location = directive.sourceLocation,
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
                val processedDocument = resolveIncludes(
                    document = parseResult.document,
                    config = config,
                    currentDepth = currentDepth + 1,
                    visitedFiles = visitedFiles,
                    errors = errors,
                    includedFiles = includedFiles,
                    currentPath = includedFilePath
                )
                
                // Remove from visited set to allow the same file to be included in different branches
                visitedFiles.remove(resolvedPath)
                
                return processedDocument.children
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
