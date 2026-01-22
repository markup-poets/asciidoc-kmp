package org.markup.poet.antora

/**
 * Default implementation of AntoraResolver.
 * Resolves Antora resource coordinates to file system paths according to Antora conventions.
 */
class DefaultAntoraResolver(
    private val fileSystem: FileSystemAccess
) : AntoraResolver {
    
    override fun resolve(
        coordinate: ResourceCoordinate,
        context: ResolutionContext
    ): ResolutionResult {
        return when (coordinate.type) {
            ResourceType.PARTIAL -> resolvePartial(coordinate, context)
            ResourceType.EXAMPLE -> resolveExample(coordinate, context)
            ResourceType.PAGE -> resolvePage(coordinate, context)
            ResourceType.IMAGE -> resolveImage(coordinate, context)
            ResourceType.ATTACHMENT -> resolveAttachment(coordinate, context)
            ResourceType.RELATIVE -> resolveRelative(coordinate, context)
        }
    }
    
    override fun resolveInclude(
        path: String,
        context: ResolutionContext
    ): ResolutionResult {
        // Try parsing as a coordinate first
        val coordinate = ResourceCoordinate.parse(path)
        
        return if (coordinate != null) {
            resolve(coordinate, context)
        } else {
            // If parsing fails, treat as relative path
            resolveRelative(
                ResourceCoordinate(
                    type = ResourceType.RELATIVE,
                    path = path
                ),
                context
            )
        }
    }
    
    private fun resolvePartial(
        coordinate: ResourceCoordinate,
        context: ResolutionContext
    ): ResolutionResult {
        return resolveResourceType(coordinate, context, "partials")
    }
    
    private fun resolveExample(
        coordinate: ResourceCoordinate,
        context: ResolutionContext
    ): ResolutionResult {
        return resolveResourceType(coordinate, context, "examples")
    }
    
    private fun resolvePage(
        coordinate: ResourceCoordinate,
        context: ResolutionContext
    ): ResolutionResult {
        return resolveResourceType(coordinate, context, "pages")
    }
    
    private fun resolveImage(
        coordinate: ResourceCoordinate,
        context: ResolutionContext
    ): ResolutionResult {
        return resolveResourceType(coordinate, context, "images")
    }
    
    private fun resolveAttachment(
        coordinate: ResourceCoordinate,
        context: ResolutionContext
    ): ResolutionResult {
        return resolveResourceType(coordinate, context, "attachments")
    }
    
    /**
     * Resolve a resource type (partial, example, page, image, attachment) to a file path.
     * Handles module-qualified and component-qualified coordinates.
     */
    private fun resolveResourceType(
        coordinate: ResourceCoordinate,
        context: ResolutionContext,
        typeDirectory: String
    ): ResolutionResult {
        // Determine the target module (use coordinate's module if specified, otherwise current module)
        val targetModule = coordinate.module ?: context.currentModule
        
        // Determine the component root (use coordinate's component if specified)
        val componentRoot = if (coordinate.component != null) {
            // Component-qualified: need to resolve to a different component
            // For now, we assume components are siblings in the same parent directory
            val parentDir = getParentDirectory(context.componentRoot)
            joinPaths(parentDir, coordinate.component)
        } else {
            context.componentRoot
        }
        
        // Build the full path: componentRoot/modules/{module}/{typeDirectory}/{path}
        val fullPath = joinPaths(
            componentRoot,
            "modules",
            targetModule,
            typeDirectory,
            coordinate.path
        )
        
        // Check if the file exists
        return if (fileSystem.exists(fullPath)) {
            ResolutionResult.Success(fullPath)
        } else {
            // Check if the module directory exists to provide better error messages
            val modulePath = joinPaths(componentRoot, "modules", targetModule)
            if (!fileSystem.exists(modulePath)) {
                ResolutionResult.Error(
                    "Module '$targetModule' not found at: $modulePath",
                    ResolutionErrorType.MODULE_NOT_FOUND
                )
            } else {
                ResolutionResult.Error(
                    "File not found: $fullPath",
                    ResolutionErrorType.FILE_NOT_FOUND
                )
            }
        }
    }
    
    /**
     * Resolve a relative path (no coordinate prefix).
     * Resolves relative to the current file's directory.
     */
    private fun resolveRelative(
        coordinate: ResourceCoordinate,
        context: ResolutionContext
    ): ResolutionResult {
        val currentFilePath = context.currentFilePath
        
        if (currentFilePath == null) {
            return ResolutionResult.Error(
                "Cannot resolve relative path without current file context",
                ResolutionErrorType.INVALID_PATH
            )
        }
        
        // Get the directory of the current file
        val currentDirectory = getParentDirectory(currentFilePath)
        
        // Resolve the relative path
        val resolvedPath = resolvePath(currentDirectory, coordinate.path)
        
        // Normalize the path (handle .. and .)
        val normalizedPath = normalizePath(resolvedPath)
        
        // Check if the file exists
        return if (fileSystem.exists(normalizedPath)) {
            ResolutionResult.Success(normalizedPath)
        } else {
            ResolutionResult.Error(
                "File not found: $normalizedPath",
                ResolutionErrorType.FILE_NOT_FOUND
            )
        }
    }
    
    /**
     * Get the parent directory of a path.
     */
    private fun getParentDirectory(path: String): String {
        val normalized = path.replace('\\', '/')
        val lastSlash = normalized.lastIndexOf('/')
        return when {
            lastSlash > 0 -> normalized.substring(0, lastSlash)
            lastSlash == 0 -> "/" // Root directory
            else -> "." // No slash found, current directory
        }
    }
    
    /**
     * Join path segments with the appropriate separator.
     */
    private fun joinPaths(vararg segments: String): String {
        return segments
            .filter { it.isNotEmpty() && it != "." }
            .joinToString("/")
            .replace("//", "/")
    }
    
    /**
     * Resolve a relative path against a base directory.
     */
    private fun resolvePath(baseDirectory: String, relativePath: String): String {
        return if (relativePath.startsWith("/")) {
            // Absolute path
            relativePath
        } else {
            joinPaths(baseDirectory, relativePath)
        }
    }
    
    /**
     * Normalize a path by resolving . and .. segments.
     */
    private fun normalizePath(path: String): String {
        val normalized = path.replace('\\', '/')
        val segments = normalized.split('/')
        val stack = mutableListOf<String>()
        
        for (segment in segments) {
            when (segment) {
                "", "." -> {
                    // Skip empty and current directory segments
                    if (stack.isEmpty() && segment == "") {
                        // Preserve leading slash for absolute paths
                        stack.add("")
                    }
                }
                ".." -> {
                    // Go up one directory
                    if (stack.isNotEmpty() && stack.last() != "" && stack.last() != "..") {
                        stack.removeAt(stack.size - 1)
                    } else if (stack.isEmpty() || stack.last() != "..") {
                        // For relative paths, keep the ..
                        if (stack.isEmpty() || stack.last() != "") {
                            stack.add("..")
                        }
                    }
                }
                else -> {
                    stack.add(segment)
                }
            }
        }
        
        return if (stack.isEmpty()) {
            "."
        } else {
            stack.joinToString("/")
        }
    }
}
