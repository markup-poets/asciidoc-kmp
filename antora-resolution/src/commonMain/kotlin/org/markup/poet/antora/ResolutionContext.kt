package org.markup.poet.antora

/**
 * Context for resolving Antora resource coordinates.
 * Contains information about the current location and project structure.
 */
data class ResolutionContext(
    val componentRoot: String,
    val currentModule: String = "ROOT",
    val currentComponent: String? = null,
    val currentFilePath: String? = null
) {
    /**
     * Create a new context for a different file within the same module.
     */
    fun withFile(filePath: String): ResolutionContext {
        return copy(currentFilePath = filePath)
    }
    
    /**
     * Create a new context for a different module.
     */
    fun withModule(module: String): ResolutionContext {
        return copy(currentModule = module)
    }
}
