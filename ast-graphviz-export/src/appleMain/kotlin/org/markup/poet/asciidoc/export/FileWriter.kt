package org.markup.poet.asciidoc.export

import kotlinx.cinterop.*
import platform.posix.*

/**
 * Apple implementation of file writing functionality.
 * Uses POSIX file APIs for file operations.
 */
@OptIn(ExperimentalForeignApi::class)
internal actual class FileWriter {
    actual fun writeToFile(path: String, content: String): FileWriteResult {
        return try {
            // Create parent directories if they don't exist
            val lastSlash = path.lastIndexOf('/')
            if (lastSlash > 0) {
                val parentPath = path.substring(0, lastSlash)
                createDirectories(parentPath)
            }
            
            // Open file for writing
            val file = fopen(path, "w")
            if (file == null) {
                val errorMsg = strerror(errno)?.toKString() ?: "Unknown error"
                return FileWriteResult.Error(path, "Failed to open file: $errorMsg")
            }
            
            try {
                // Write content to file
                val bytes = content.encodeToByteArray()
                val written = fwrite(bytes.refTo(0), 1u, bytes.size.toULong(), file)
                
                if (written.toInt() != bytes.size) {
                    val errorMsg = strerror(errno)?.toKString() ?: "Unknown error"
                    return FileWriteResult.Error(path, "Failed to write complete content: $errorMsg")
                }
                
                FileWriteResult.Success(path)
            } finally {
                fclose(file)
            }
        } catch (e: Exception) {
            FileWriteResult.Error(path, "Unexpected error: ${e.message}", e)
        }
    }
    
    private fun createDirectories(path: String) {
        val parts = path.split('/')
        var currentPath = if (path.startsWith('/')) "/" else ""
        
        for (part in parts) {
            if (part.isEmpty()) continue
            
            currentPath += if (currentPath.endsWith('/')) part else "/$part"
            
            // Check if directory exists
            memScoped {
                val statBuf = alloc<stat>()
                if (stat(currentPath, statBuf.ptr) != 0) {
                    // Directory doesn't exist, create it
                    mkdir(currentPath, 0x1FFu.convert()) // 0777 permissions
                }
            }
        }
    }
}
