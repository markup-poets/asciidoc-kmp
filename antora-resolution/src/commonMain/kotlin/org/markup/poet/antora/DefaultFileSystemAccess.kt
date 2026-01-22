package org.markup.poet.antora

/**
 * Default platform-specific implementation of FileSystemAccess.
 * Uses expect/actual declarations for platform-specific file operations.
 */
expect class DefaultFileSystemAccess() : FileSystemAccess {
    override fun exists(path: String): Boolean
    override fun isDirectory(path: String): Boolean
    override fun readFile(path: String): FileReadResult
    override fun listDirectory(path: String): List<String>
    override fun writeFile(path: String, content: String): FileWriteResult
}
