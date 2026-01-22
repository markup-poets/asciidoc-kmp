package org.markup.poet.antora

/**
 * Resolves Antora resource coordinates to file system paths.
 * This is the main entry point for the Antora resolution library.
 */
interface AntoraResolver {
    /**
     * Resolve a resource coordinate to an absolute file path.
     * Returns a Result containing either the resolved path or an error.
     */
    fun resolve(
        coordinate: ResourceCoordinate,
        context: ResolutionContext
    ): ResolutionResult
    
    /**
     * Resolve an include directive path (may be coordinate or relative path).
     */
    fun resolveInclude(
        path: String,
        context: ResolutionContext
    ): ResolutionResult
}

sealed class ResolutionResult {
    data class Success(val resolvedPath: String) : ResolutionResult()
    data class Error(val message: String, val errorType: ResolutionErrorType) : ResolutionResult()
}

enum class ResolutionErrorType {
    INVALID_COORDINATE,
    MODULE_NOT_FOUND,
    FILE_NOT_FOUND,
    INVALID_PATH
}
