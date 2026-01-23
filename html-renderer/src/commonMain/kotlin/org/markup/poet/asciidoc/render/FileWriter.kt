package org.markup.poet.asciidoc.render

/**
 * Platform-agnostic file writing interface.
 * 
 * Implementations handle platform-specific file I/O operations.
 * This interface provides a common abstraction for writing file contents
 * across all supported platforms (JVM, Android, iOS, Linux).
 */
interface FileWriter {
    /**
     * Writes content to a file.
     * 
     * Creates parent directories if they don't exist. Overwrites the file
     * if it already exists. Supports both absolute and relative file paths.
     * Relative paths are resolved relative to the current working directory.
     * 
     * @param path File path (relative or absolute)
     * @param content Content to write to the file
     * @return Result indicating success or containing an exception on failure
     */
    fun writeFile(path: String, content: String): Result<Unit>
}

/**
 * Platform-specific implementation of FileWriter.
 * 
 * This expect class provides platform-specific file writing capabilities.
 * Each platform (JVM, Android, iOS, Linux) provides its own actual implementation
 * using the appropriate platform APIs.
 * 
 * Usage:
 * ```kotlin
 * val fileWriter = PlatformFileWriter()
 * val result = fileWriter.writeFile("path/to/file.css", cssContent)
 * result.onSuccess {
 *     // File written successfully
 * }.onFailure { error ->
 *     // Handle error
 * }
 * ```
 */
expect class PlatformFileWriter() : FileWriter {
    override fun writeFile(path: String, content: String): Result<Unit>
}
