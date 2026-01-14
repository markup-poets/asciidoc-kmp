package org.markup.poet.asciidoc.processing

/**
 * Platform-specific interface for reading file content.
 * Implementations should be provided for each target platform.
 */
interface FileReader {
    /**
     * Read the content of a file at the specified path.
     * 
     * @param path The file path to read (can be relative or absolute)
     * @return FileReadResult containing either the file content or an error message
     */
    fun readFile(path: String): FileReadResult
}

/**
 * Result of a file read operation.
 */
sealed class FileReadResult {
    /**
     * Successful file read with content.
     */
    data class Success(val content: String) : FileReadResult()
    
    /**
     * Failed file read with error message.
     */
    data class Error(val message: String) : FileReadResult()
}
