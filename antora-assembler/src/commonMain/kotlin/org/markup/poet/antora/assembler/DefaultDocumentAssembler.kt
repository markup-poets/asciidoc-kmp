package org.markup.poet.antora.assembler

import org.markup.poet.antora.AntoraResolver
import org.markup.poet.antora.FileSystemAccess
import org.markup.poet.antora.FileReadResult
import org.markup.poet.antora.FileWriteResult
import org.markup.poet.antora.ResolutionContext
import org.markup.poet.asciidoc.parser.AsciidocParser
import org.markup.poet.asciidoc.ast.Document
import org.markup.poet.asciidoc.ast.IncludeDirective

/**
 * Default implementation of DocumentAssembler.
 * Assembles multiple AsciiDoc files from an Antora structure into a single document.
 */
class DefaultDocumentAssembler(
    private val parser: AsciidocParser,
    private val resolver: AntoraResolver,
    private val fileSystem: FileSystemAccess
) : DocumentAssembler {
    
    override fun assemble(config: AssemblerConfig): AssemblerResult {
        println("[ASSEMBLER] Starting assembly process")
        println("[ASSEMBLER] Index file: ${config.indexFile}")
        println("[ASSEMBLER] Output file: ${config.outputFile}")
        println("[ASSEMBLER] Component root: ${config.componentRoot}")
        println("[ASSEMBLER] Max depth: ${config.maxDepth}")
        
        val errors = mutableListOf<AssemblerError>()
        val warnings = mutableListOf<AssemblerWarning>()
        val includedFiles = mutableSetOf<String>()
        
        // Step 1: Read and parse index file
        println("[ASSEMBLER] Step 1: Reading index file...")
        val indexFileContent = when (val readResult = fileSystem.readFile(config.indexFile)) {
            is FileReadResult.Success -> {
                println("[ASSEMBLER] Successfully read index file (${readResult.content.length} bytes)")
                readResult.content
            }
            is FileReadResult.Error -> {
                println("[ASSEMBLER] ERROR: Failed to read index file: ${readResult.message}")
                errors.add(
                    AssemblerError(
                        message = "Failed to read index file '${config.indexFile}': ${readResult.message}",
                        filePath = config.indexFile,
                        lineNumber = null,
                        errorType = AssemblerErrorType.INDEX_FILE_NOT_FOUND
                    )
                )
                return AssemblerResult(
                    success = false,
                    outputPath = null,
                    errors = errors,
                    warnings = warnings,
                    includedFiles = includedFiles
                )
            }
        }
        
        // Parse the index file
        println("[ASSEMBLER] Step 2: Parsing index file...")
        val parseResult = parser.parse(indexFileContent)
        println("[ASSEMBLER] Parse complete. Errors: ${parseResult.errors.size}, Warnings: ${parseResult.warnings.size}")
        
        // Report parse errors
        parseResult.errors.forEach { parseError ->
            errors.add(
                AssemblerError(
                    message = "Parse error in index file: ${parseError.message}",
                    filePath = config.indexFile,
                    lineNumber = parseError.location.line,
                    errorType = AssemblerErrorType.PARSE_ERROR
                )
            )
        }
        
        // Report parse warnings
        parseResult.warnings.forEach { parseWarning ->
            warnings.add(
                AssemblerWarning(
                    message = "Parse warning in index file: ${parseWarning.message}",
                    filePath = config.indexFile,
                    lineNumber = parseWarning.location.line
                )
            )
        }
        
        // If there are fatal parse errors, stop here
        if (parseResult.errors.isNotEmpty()) {
            return AssemblerResult(
                success = false,
                outputPath = null,
                errors = errors,
                warnings = warnings,
                includedFiles = includedFiles
            )
        }
        
        val indexDocument = parseResult.document
        includedFiles.add(config.indexFile)
        
        // Step 2: Build dependency graph
        println("[ASSEMBLER] Step 3: Building dependency graph...")
        val context = ResolutionContext(
            componentRoot = config.componentRoot,
            currentModule = "ROOT",
            currentFilePath = config.indexFile
        )
        
        val dependencyGraph = buildDependencyGraph(
            indexDocument,
            context,
            config,
            errors,
            warnings
        )
        println("[ASSEMBLER] Dependency graph built. Nodes: ${dependencyGraph.nodes.size}")
        
        // Step 3: Detect circular dependencies
        println("[ASSEMBLER] Step 4: Checking for circular dependencies...")
        val cycles = dependencyGraph.detectCycles()
        println("[ASSEMBLER] Circular dependencies found: ${cycles.size}")
        if (cycles.isNotEmpty()) {
            cycles.forEach { cycle ->
                errors.add(
                    AssemblerError(
                        message = "Circular dependency detected: ${cycle}",
                        filePath = config.indexFile,
                        lineNumber = null,
                        errorType = AssemblerErrorType.CIRCULAR_DEPENDENCY
                    )
                )
            }
            
            if (config.failOnCircularDependencies) {
                return AssemblerResult(
                    success = false,
                    outputPath = null,
                    errors = errors,
                    warnings = warnings,
                    includedFiles = includedFiles
                )
            }
        }
        
        // Step 4: Resolve and merge includes using ContentMerger
        println("[ASSEMBLER] Step 5: Merging content...")
        val contentMerger = ContentMerger(resolver, parser, fileSystem)
        val mergeResult = contentMerger.merge(indexDocument, context, config)
        println("[ASSEMBLER] Merge complete. Errors: ${mergeResult.errors.size}, Warnings: ${mergeResult.warnings.size}")
        
        // Collect errors and warnings from merge
        errors.addAll(mergeResult.errors)
        warnings.addAll(mergeResult.warnings)
        
        // Check if there are critical errors that prevent output
        val hasCriticalErrors = errors.any { error ->
            when (error.errorType) {
                AssemblerErrorType.INDEX_FILE_NOT_FOUND,
                AssemblerErrorType.PARSE_ERROR -> true
                AssemblerErrorType.CIRCULAR_DEPENDENCY -> config.failOnCircularDependencies
                AssemblerErrorType.INCLUDE_NOT_FOUND -> config.failOnMissingIncludes
                AssemblerErrorType.MAX_DEPTH_EXCEEDED -> true
                else -> false
            }
        }
        
        if (hasCriticalErrors) {
            return AssemblerResult(
                success = false,
                outputPath = null,
                errors = errors,
                warnings = warnings,
                includedFiles = includedFiles
            )
        }
        
        // Step 5: Collect all included files from the dependency graph
        println("[ASSEMBLER] Step 6: Collecting included files...")
        includedFiles.addAll(dependencyGraph.nodes.keys)
        println("[ASSEMBLER] Total included files: ${includedFiles.size}")
        
        // Step 6: Write output file
        println("[ASSEMBLER] Step 7: Writing output file...")
        val mergedDocument = mergeResult.document
        if (mergedDocument == null) {
            println("[ASSEMBLER] ERROR: No document produced from merge")
            errors.add(
                AssemblerError(
                    message = "Failed to merge document: no document produced",
                    filePath = config.indexFile,
                    lineNumber = null,
                    errorType = AssemblerErrorType.FILE_WRITE_ERROR
                )
            )
            return AssemblerResult(
                success = false,
                outputPath = null,
                errors = errors,
                warnings = warnings,
                includedFiles = includedFiles
            )
        }
        
        println("[ASSEMBLER] Rendering document...")
        val outputContent = renderDocument(mergedDocument)
        println("[ASSEMBLER] Rendered document (${outputContent.length} bytes)")
        
        println("[ASSEMBLER] Writing to file: ${config.outputFile}")
        when (val writeResult = fileSystem.writeFile(config.outputFile, outputContent)) {
            is FileWriteResult.Success -> {
                println("[ASSEMBLER] ✓ Successfully wrote output file")
                return AssemblerResult(
                    success = true,
                    outputPath = config.outputFile,
                    errors = errors,
                    warnings = warnings,
                    includedFiles = includedFiles
                )
            }
            is FileWriteResult.Error -> {
                errors.add(
                    AssemblerError(
                        message = "Failed to write output file '${config.outputFile}': ${writeResult.message}",
                        filePath = config.outputFile,
                        lineNumber = null,
                        errorType = AssemblerErrorType.FILE_WRITE_ERROR
                    )
                )
                return AssemblerResult(
                    success = false,
                    outputPath = null,
                    errors = errors,
                    warnings = warnings,
                    includedFiles = includedFiles
                )
            }
        }
    }
    
    /**
     * Build a dependency graph by traversing the document tree and collecting all include directives.
     */
    private fun buildDependencyGraph(
        document: Document,
        context: ResolutionContext,
        config: AssemblerConfig,
        errors: MutableList<AssemblerError>,
        warnings: MutableList<AssemblerWarning>
    ): DependencyGraph {
        val nodes = mutableMapOf<String, DependencyNode>()
        val visited = mutableSetOf<String>()
        
        // Add the root document
        val rootPath = context.currentFilePath ?: config.indexFile
        
        // Build the graph recursively
        buildDependencyGraphHelper(
            document,
            context,
            config,
            nodes,
            visited,
            0,
            errors,
            warnings
        )
        
        return DependencyGraph(
            nodes = nodes,
            root = rootPath
        )
    }
    
    /**
     * Helper function to recursively build the dependency graph.
     */
    private fun buildDependencyGraphHelper(
        document: Document,
        context: ResolutionContext,
        config: AssemblerConfig,
        nodes: MutableMap<String, DependencyNode>,
        visited: MutableSet<String>,
        depth: Int,
        errors: MutableList<AssemblerError>,
        warnings: MutableList<AssemblerWarning>
    ) {
        val currentPath = context.currentFilePath ?: return
        
        println("[GRAPH] Processing: $currentPath (depth: $depth)")
        
        if (currentPath in visited) {
            println("[GRAPH] Already visited: $currentPath, skipping")
            return
        }
        
        if (depth >= config.maxDepth) {
            println("[GRAPH] Max depth reached at: $currentPath")
            return
        }
        
        visited.add(currentPath)
        
        // Collect all include directives from this document
        val includes = collectIncludes(document)
        println("[GRAPH] Found ${includes.size} includes in $currentPath")
        val dependencies = mutableListOf<String>()
        
        for (include in includes) {
            println("[GRAPH] Resolving include: ${include.path}")
            // Resolve the include path
            val resolutionResult = resolver.resolveInclude(include.path, context)
            
            when (resolutionResult) {
                is org.markup.poet.antora.ResolutionResult.Success -> {
                    val resolvedPath = resolutionResult.resolvedPath
                    println("[GRAPH] Resolved to: $resolvedPath")
                    dependencies.add(resolvedPath)
                    
                    // If we haven't visited this file yet and haven't exceeded max depth, process it
                    if (resolvedPath !in visited && depth < config.maxDepth) {
                        println("[GRAPH] Reading file: $resolvedPath")
                        // Read and parse the included file
                        when (val readResult = fileSystem.readFile(resolvedPath)) {
                            is FileReadResult.Success -> {
                                println("[GRAPH] Successfully read file (${readResult.content.length} bytes)")
                                println("[GRAPH] Parsing file: $resolvedPath")
                                val parseResult = parser.parse(readResult.content)
                                println("[GRAPH] Parse complete for: $resolvedPath")
                                
                                // Recursively build graph for included file
                                val includedContext = context.withFile(resolvedPath)
                                buildDependencyGraphHelper(
                                    parseResult.document,
                                    includedContext,
                                    config,
                                    nodes,
                                    visited,
                                    depth + 1,
                                    errors,
                                    warnings
                                )
                            }
                            is FileReadResult.Error -> {
                                println("[GRAPH] ERROR reading file: ${readResult.message}")
                                // Error will be reported during merge phase
                            }
                        }
                    } else {
                        println("[GRAPH] Skipping $resolvedPath (visited=${resolvedPath in visited}, depth=$depth, maxDepth=${config.maxDepth})")
                    }
                }
                is org.markup.poet.antora.ResolutionResult.Error -> {
                    println("[GRAPH] ERROR resolving include: ${resolutionResult.message}")
                    // Error will be reported during merge phase
                }
            }
        }
        
        println("[GRAPH] Adding node for: $currentPath with ${dependencies.size} dependencies")
        // Add node to graph
        nodes[currentPath] = DependencyNode(
            filePath = currentPath,
            dependencies = dependencies,
            sourceLocation = null
        )
    }
    
    /**
     * Collect all include directives from a document.
     */
    private fun collectIncludes(document: Document): List<IncludeDirective> {
        val includes = mutableListOf<IncludeDirective>()
        
        fun collectFromBlocks(blocks: List<org.markup.poet.asciidoc.ast.BlockElement>) {
            for (block in blocks) {
                when (block) {
                    is IncludeDirective -> includes.add(block)
                    is org.markup.poet.asciidoc.ast.Section -> collectFromBlocks(block.children)
                    is org.markup.poet.asciidoc.ast.AdmonitionBlock -> collectFromBlocks(block.content)
                    is org.markup.poet.asciidoc.ast.ConditionalDirective -> {
                        collectFromBlocks(block.content)
                        collectFromBlocks(block.elseContent)
                    }
                    else -> {}
                }
            }
        }
        
        collectFromBlocks(document.children)
        return includes
    }
    
    /**
     * Render a document back to AsciiDoc text.
     * This is a simple renderer that preserves the document structure.
     */
    private fun renderDocument(document: Document): String {
        val builder = StringBuilder()
        
        // Render document attributes
        if (document.documentAttributes.isNotEmpty()) {
            for ((key, value) in document.documentAttributes) {
                builder.append(":$key: $value\n")
            }
            builder.append("\n")
        }
        
        // Render document children
        for (child in document.children) {
            renderBlock(child, builder, 0)
        }
        
        return builder.toString()
    }
    
    /**
     * Render a block element to AsciiDoc text.
     */
    private fun renderBlock(block: org.markup.poet.asciidoc.ast.BlockElement, builder: StringBuilder, indentLevel: Int) {
        val indent = "  ".repeat(indentLevel)
        
        when (block) {
            is org.markup.poet.asciidoc.ast.Section -> {
                val prefix = "=".repeat(block.level)
                builder.append("$indent$prefix ${block.title}\n\n")
                for (child in block.children) {
                    renderBlock(child, builder, indentLevel)
                }
            }
            is org.markup.poet.asciidoc.ast.Paragraph -> {
                for (inline in block.content) {
                    renderInline(inline, builder)
                }
                builder.append("\n\n")
            }
            is org.markup.poet.asciidoc.ast.AsciiDocList -> {
                for (item in block.items) {
                    val marker = if (block.type == org.markup.poet.asciidoc.ast.ListType.ORDERED) {
                        "${item.marker}."
                    } else {
                        "*"
                    }
                    builder.append("$indent$marker ")
                    for (inline in item.content) {
                        renderInline(inline, builder)
                    }
                    builder.append("\n")
                    
                    if (item.nestedList != null) {
                        renderBlock(item.nestedList as org.markup.poet.asciidoc.ast.BlockElement, builder, indentLevel + 1)
                    }
                }
                builder.append("\n")
            }
            is org.markup.poet.asciidoc.ast.CodeBlock -> {
                builder.append("$indent----\n")
                builder.append(block.content)
                if (!block.content.endsWith("\n")) {
                    builder.append("\n")
                }
                builder.append("$indent----\n\n")
            }
            is org.markup.poet.asciidoc.ast.AdmonitionBlock -> {
                builder.append("$indent${block.type.name}: ")
                for (child in block.content) {
                    renderBlock(child, builder, indentLevel)
                }
            }
            else -> {
                // For other block types, just add a blank line
                builder.append("\n")
            }
        }
    }
    
    /**
     * Render an inline element to AsciiDoc text.
     */
    private fun renderInline(inline: org.markup.poet.asciidoc.ast.InlineElement, builder: StringBuilder) {
        when (inline) {
            is org.markup.poet.asciidoc.ast.Text -> builder.append(inline.content)
            is org.markup.poet.asciidoc.ast.Strong -> {
                builder.append("*")
                for (child in inline.content) {
                    renderInline(child, builder)
                }
                builder.append("*")
            }
            is org.markup.poet.asciidoc.ast.Emphasis -> {
                builder.append("_")
                for (child in inline.content) {
                    renderInline(child, builder)
                }
                builder.append("_")
            }
            is org.markup.poet.asciidoc.ast.Code -> {
                builder.append("`")
                builder.append(inline.content)
                builder.append("`")
            }
            is org.markup.poet.asciidoc.ast.CrossReference -> {
                builder.append("<<${inline.targetId}")
                if (inline.customText != null) {
                    builder.append(",${inline.customText}")
                }
                builder.append(">>")
            }
            is org.markup.poet.asciidoc.ast.Link -> {
                builder.append("${inline.url}[${inline.text}]")
            }
            else -> {}
        }
    }
}
