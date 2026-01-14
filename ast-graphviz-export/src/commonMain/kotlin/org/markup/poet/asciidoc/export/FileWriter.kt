package org.markup.poet.asciidoc.export

/**
 * Result type for file operations
 */
sealed class FileWriteResult {
    data class Success(val path: String) : FileWriteResult()
    data class Error(val path: String, val message: String, val cause: Throwable? = null) : FileWriteResult()
}

/**
 * Platform-specific file writing functionality.
 * Implementations should handle parent directory creation and error handling.
 */
internal expect class FileWriter() {
    /**
     * Writes content to a file at the specified path.
     * Creates parent directories if they don't exist.
     * 
     * @param path The file path to write to
     * @param content The content to write
     * @return FileWriteResult indicating success or failure
     */
    fun writeToFile(path: String, content: String): FileWriteResult
}
