package org.markup.poet.antora

/**
 * Platform-agnostic interface for file system operations.
 * Implementations will use expect/actual for platform-specific code.
 */
interface FileSystemAccess {
    fun exists(path: String): Boolean
    fun isDirectory(path: String): Boolean
    fun readFile(path: String): FileReadResult
    fun listDirectory(path: String): List<String>
    fun writeFile(path: String, content: String): FileWriteResult
}

sealed class FileReadResult {
    data class Success(val content: String) : FileReadResult()
    data class Error(val message: String) : FileReadResult()
}

sealed class FileWriteResult {
    object Success : FileWriteResult()
    data class Error(val message: String) : FileWriteResult()
}
