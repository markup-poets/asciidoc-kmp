package org.markup.poet.asciidoc.render

/**
 * Platform-agnostic file reading interface.
 * 
 * Implementations handle platform-specific file I/O operations.
 * This interface provides a common abstraction for reading file contents
 * across all supported platforms (JVM, Android, iOS, Linux).
 */
interface FileReader {
    /**
     * Reads file content as a string.
     * 
     * Supports both absolute and relative file paths. Relative paths are
     * resolved relative to the current working directory.
     * 
     * @param path File path (relative or absolute)
     * @return Result containing file content as a string on success,
     *         or an exception on failure (file not found, read error, etc.)
     */
    fun readFile(path: String): Result<String>
}

/**
 * Platform-specific implementation of FileReader.
 * 
 * This expect class provides platform-specific file reading capabilities.
 * Each platform (JVM, Android, iOS, Linux) provides its own actual implementation
 * using the appropriate platform APIs.
 * 
 * Usage:
 * ```kotlin
 * val fileReader = PlatformFileReader()
 * val result = fileReader.readFile("path/to/file.css")
 * result.onSuccess { content ->
 *     // Use file content
 * }.onFailure { error ->
 *     // Handle error
 * }
 * ```
 */
expect class PlatformFileReader() : FileReader {
    override fun readFile(path: String): Result<String>
}
