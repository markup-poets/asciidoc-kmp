package org.markup.poet.antora.assembler

import org.markup.poet.antora.AntoraResolver
import org.markup.poet.antora.FileReadResult
import org.markup.poet.antora.FileSystemAccess
import org.markup.poet.antora.ResolutionContext
import org.markup.poet.antora.ResolutionResult
import org.markup.poet.asciidoc.ast.BlockElement
import org.markup.poet.asciidoc.ast.Document
import org.markup.poet.asciidoc.ast.IncludeDirective
import org.markup.poet.asciidoc.ast.InlineElement
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
        document: Document,
        context: ResolutionContext,
        config: AssemblerConfig
    ): MergeResult {
        val warnings = mutableListOf<AssemblerWarning>()
        val errors = mutableListOf<AssemblerError>()
        val visited = mutableSetOf<String>()
        
        // Add the current document to visited set
        context.currentFilePath?.let { visited.add(it) }
        
        // Track merged attributes (first definition wins)
        val mergedAttributes = document.documentAttributes.toMutableMap()
        
        // Clear anchor registry for this merge operation
        anchorRegistry.clear()
        
        // Process all includes recursively
        val processedChildren = processBlockElements(
            document.children,
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
        buildAnchorRegistry(processedChildren, context.currentFilePath ?: "")
        
        // Second pass: Resolve cross-references in the processed document
        val resolvedChildren = resolveXrefsInBlocks(processedChildren, warnings)
        
        // Create the merged document with processed children and merged attributes
        val mergedDocument = document.copy(
            children = resolvedChildren,
            documentAttributes = mergedAttributes
        )
        
        return MergeResult(
            document = mergedDocument,
            warnings = warnings,
            errors = errors
        )
    }
    
    /**
     * Process a list of block elements, resolving any include directives.
     */
    private fun processBlockElements(
        elements: List<BlockElement>,
        context: ResolutionContext,
        config: AssemblerConfig,
        visited: MutableSet<String>,
        depth: Int,
        warnings: MutableList<AssemblerWarning>,
        errors: MutableList<AssemblerError>,
        mergedAttributes: MutableMap<String, String>
    ): List<BlockElement> {
        val result = mutableListOf<BlockElement>()
        
        for (element in elements) {
            when (element) {
                is IncludeDirective -> {
                    // Process the include directive
                    val includedElements = processInclude(
                        element,
                        context,
                        config,
                        visited,
                        depth,
                        warnings,
                        errors,
                        mergedAttributes
                    )
                    result.addAll(includedElements)
                }
                else -> {
                    // For other elements, recursively process their children if they have any
                    val processedElement = processBlockElement(
                        element,
                        context,
                        config,
                        visited,
                        depth,
                        warnings,
                        errors,
                        mergedAttributes
                    )
                    result.add(processedElement)
                }
            }
        }
        
        return result
    }
    
    /**
     * Process a single block element, recursively processing any nested elements.
     */
    private fun processBlockElement(
        element: BlockElement,
        context: ResolutionContext,
        config: AssemblerConfig,
        visited: MutableSet<String>,
        depth: Int,
        warnings: MutableList<AssemblerWarning>,
        errors: MutableList<AssemblerError>,
        mergedAttributes: MutableMap<String, String>
    ): BlockElement {
        return when (element) {
            is org.markup.poet.asciidoc.ast.Section -> {
                element.copy(
                    children = processBlockElements(
                        element.children,
                        context,
                        config,
                        visited,
                        depth,
                        warnings,
                        errors,
                        mergedAttributes
                    )
                )
            }
            is org.markup.poet.asciidoc.ast.AdmonitionBlock -> {
                element.copy(
                    content = processBlockElements(
                        element.content,
                        context,
                        config,
                        visited,
                        depth,
                        warnings,
                        errors,
                        mergedAttributes
                    )
                )
            }
            is org.markup.poet.asciidoc.ast.ConditionalDirective -> {
                element.copy(
                    content = processBlockElements(
                        element.content,
                        context,
                        config,
                        visited,
                        depth,
                        warnings,
                        errors,
                        mergedAttributes
                    ),
                    elseContent = processBlockElements(
                        element.elseContent,
                        context,
                        config,
                        visited,
                        depth,
                        warnings,
                        errors,
                        mergedAttributes
                    )
                )
            }
            else -> element
        }
    }
    
    /**
     * Process an include directive by resolving the path, reading the file,
     * parsing it, and recursively processing any nested includes.
     */
    private fun processInclude(
        directive: IncludeDirective,
        context: ResolutionContext,
        config: AssemblerConfig,
        visited: MutableSet<String>,
        depth: Int,
        warnings: MutableList<AssemblerWarning>,
        errors: MutableList<AssemblerError>,
        mergedAttributes: MutableMap<String, String>
    ): List<BlockElement> {
        // Check depth limit
        if (depth >= config.maxDepth) {
            errors.add(
                AssemblerError(
                    message = "Maximum include depth (${config.maxDepth}) exceeded at ${directive.path}",
                    filePath = context.currentFilePath,
                    lineNumber = directive.sourceLocation.line,
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
                    lineNumber = directive.sourceLocation.line,
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
                        lineNumber = directive.sourceLocation.line,
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
                                lineNumber = directive.sourceLocation.line,
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
                val indentLevel = directive.sourceLocation.column
                val indentedContent = if (indentLevel > 0) {
                    applyIndentation(tagFilteredContent, indentLevel)
                } else {
                    tagFilteredContent
                }
                
                // Parse the included file
                val parseResult = parser.parse(indentedContent)
                
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
                    parseResult.document.documentAttributes,
                    resolvedPath,
                    warnings
                )
                
                // Add the resolved path to visited set
                visited.add(resolvedPath)
                
                // Create a new context for the included file
                val includedContext = context.withFile(resolvedPath)
                
                // Recursively process the included document's children
                val processedChildren = processBlockElements(
                    parseResult.document.children,
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
                
                return processedChildren
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
    private fun buildAnchorRegistry(elements: List<BlockElement>, sourceFile: String) {
        for (element in elements) {
            // Check if element has an anchor ID
            val anchorId = element.attributes["id"]
            if (anchorId != null && !anchorRegistry.containsKey(anchorId)) {
                anchorRegistry[anchorId] = AnchorInfo(
                    anchorId = anchorId,
                    sourceFile = sourceFile,
                    element = element
                )
            }
            
            // Recursively process children
            when (element) {
                is org.markup.poet.asciidoc.ast.Section -> {
                    buildAnchorRegistry(element.children, sourceFile)
                }
                is org.markup.poet.asciidoc.ast.AsciiDocList -> {
                    for (item in element.items) {
                        val itemAnchorId = item.attributes["id"]
                        if (itemAnchorId != null && !anchorRegistry.containsKey(itemAnchorId)) {
                            anchorRegistry[itemAnchorId] = AnchorInfo(
                                anchorId = itemAnchorId,
                                sourceFile = sourceFile,
                                element = item
                            )
                        }
                        if (item.nestedList != null) {
                            buildAnchorRegistry(listOf(item.nestedList as BlockElement), sourceFile)
                        }
                    }
                }
                is org.markup.poet.asciidoc.ast.CalloutList -> {
                    for (item in element.items) {
                        val itemAnchorId = item.attributes["id"]
                        if (itemAnchorId != null && !anchorRegistry.containsKey(itemAnchorId)) {
                            anchorRegistry[itemAnchorId] = AnchorInfo(
                                anchorId = itemAnchorId,
                                sourceFile = sourceFile,
                                element = item
                            )
                        }
                    }
                }
                else -> {
                    // Other block types don't have children
                }
            }
        }
    }
    
    /**
     * Resolve cross-references in a list of block elements.
     * This converts Antora xref syntax to simple anchor references and validates references.
     */
    private fun resolveXrefsInBlocks(
        elements: List<BlockElement>,
        warnings: MutableList<AssemblerWarning>
    ): List<BlockElement> {
        return elements.map { resolveXrefsInBlock(it, warnings) }
    }
    
    /**
     * Resolve cross-references in a single block element.
     */
    private fun resolveXrefsInBlock(
        element: BlockElement,
        warnings: MutableList<AssemblerWarning>
    ): BlockElement {
        return when (element) {
            is org.markup.poet.asciidoc.ast.Section -> {
                element.copy(
                    children = resolveXrefsInBlocks(element.children, warnings)
                )
            }
            is org.markup.poet.asciidoc.ast.Paragraph -> {
                element.copy(
                    content = element.content.map { resolveXrefsInInline(it, warnings) }
                )
            }
            is org.markup.poet.asciidoc.ast.AsciiDocList -> {
                element.copy(
                    items = element.items.map { item ->
                        item.copy(
                            content = item.content.map { resolveXrefsInInline(it, warnings) },
                            nestedList = item.nestedList?.let {
                                resolveXrefsInBlock(it, warnings) as org.markup.poet.asciidoc.ast.AsciiDocList
                            }
                        )
                    }
                )
            }
            is org.markup.poet.asciidoc.ast.CalloutList -> {
                element.copy(
                    items = element.items.map { item ->
                        item.copy(
                            content = item.content.map { resolveXrefsInInline(it, warnings) }
                        )
                    }
                )
            }
            is org.markup.poet.asciidoc.ast.AdmonitionBlock -> {
                element.copy(
                    content = resolveXrefsInBlocks(element.content, warnings)
                )
            }
            is org.markup.poet.asciidoc.ast.ConditionalDirective -> {
                element.copy(
                    content = resolveXrefsInBlocks(element.content, warnings),
                    elseContent = resolveXrefsInBlocks(element.elseContent, warnings)
                )
            }
            else -> element
        }
    }
    
    /**
     * Resolve cross-references in an inline element.
     * Handles CrossReference elements by validating them against the anchor registry.
     */
    private fun resolveXrefsInInline(
        element: InlineElement,
        warnings: MutableList<AssemblerWarning>
    ): InlineElement {
        return when (element) {
            is org.markup.poet.asciidoc.ast.CrossReference -> {
                // Parse the target ID to check if it's an Antora xref
                val targetId = element.targetId
                val resolvedTargetId = resolveAntoraXref(targetId)
                
                // Check if the anchor exists in the registry
                if (!anchorRegistry.containsKey(resolvedTargetId)) {
                    warnings.add(
                        AssemblerWarning(
                            message = "Cross-reference target '$resolvedTargetId' not found in assembled document",
                            filePath = null,
                            lineNumber = element.sourceLocation.line
                        )
                    )
                }
                
                // Return the cross-reference with the resolved target ID
                if (resolvedTargetId != targetId) {
                    element.copy(targetId = resolvedTargetId)
                } else {
                    element
                }
            }
            is org.markup.poet.asciidoc.ast.Strong -> {
                element.copy(
                    content = element.content.map { resolveXrefsInInline(it, warnings) }
                )
            }
            is org.markup.poet.asciidoc.ast.Emphasis -> {
                element.copy(
                    content = element.content.map { resolveXrefsInInline(it, warnings) }
                )
            }
            else -> element
        }
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
    val element: org.markup.poet.asciidoc.ast.AstNode
)

data class MergeResult(
    val document: Document? = null,
    val warnings: List<AssemblerWarning>,
    val errors: List<AssemblerError> = emptyList()
)
