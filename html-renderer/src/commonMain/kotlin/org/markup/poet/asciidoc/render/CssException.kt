package org.markup.poet.asciidoc.render

/**
 * Exception types for CSS-related errors.
 * 
 * These exceptions provide specific error information for different failure modes
 * during CSS loading, merging, and configuration.
 * 
 * Validates: Requirements 2.2, 2.3, 5.5, 8.1
 */
sealed class CssException(message: String) : Exception(message) {
    /**
     * Thrown when a CSS file cannot be found at the specified path.
     * 
     * @param path The file path that was attempted
     * @param reason Description of why the file was not found
     */
    data class FileNotFound(val path: String, val reason: String) :
        CssException("CSS file not found: $path - $reason")
    
    /**
     * Thrown when CSS loading fails for reasons other than file not found.
     * 
     * This includes I/O errors, permission issues, or other file system problems.
     * 
     * @param reason Description of the loading failure
     */
    data class LoadingFailure(val reason: String) :
        CssException("Failed to load CSS: $reason")
    
    /**
     * Thrown when an invalid theme name is provided.
     * 
     * @param themeName The invalid theme name that was provided
     */
    data class InvalidTheme(val themeName: String) :
        CssException("Invalid theme name: $themeName. Available themes: default, minimal, dark, kotlin")
}
