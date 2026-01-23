package org.markup.poet.asciidoc.render

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.refTo
import kotlinx.cinterop.toKString
import platform.posix.errno
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite
import platform.posix.mkdir
import platform.posix.stat
import platform.posix.strerror

/**
 * Linux implementation of FileWriter using POSIX APIs.
 * 
 * Uses standard C file I/O functions (fopen, fwrite, etc.) via Kotlin/Native interop.
 * Creates parent directories if they don't exist.
 * Supports both absolute and relative file paths.
 */
@OptIn(ExperimentalForeignApi::class)
actual class PlatformFileWriter : FileWriter {
    actual override fun writeFile(path: String, content: String): Result<Unit> {
        return try {
            // Create parent directories if needed
            val lastSlashIndex = path.lastIndexOf('/')
            if (lastSlashIndex > 0) {
                val parentDir = path.substring(0, lastSlashIndex)
                createDirectories(parentDir)
            }
            
            // Open file for writing
            val file = fopen(path, "w")
            
            if (file == null) {
                val errorMsg = strerror(errno)?.toKString() ?: "Unknown error"
                return Result.failure(
                    CssException.LoadingFailure(
                        reason = "Failed to open file for writing '$path': $errorMsg"
                    )
                )
            }
            
            try {
                // Write content to file
                val bytes = content.encodeToByteArray()
                val written = fwrite(bytes.refTo(0), 1u, bytes.size.toULong(), file)
                
                if (written.toInt() != bytes.size) {
                    val errorMsg = strerror(errno)?.toKString() ?: "Unknown error"
                    return Result.failure(
                        CssException.LoadingFailure(
                            reason = "Failed to write complete content to '$path': $errorMsg"
                        )
                    )
                }
                
                Result.success(Unit)
            } finally {
                fclose(file)
            }
        } catch (e: Exception) {
            Result.failure(
                CssException.LoadingFailure(
                    reason = "Failed to write file '$path': ${e.message}"
                )
            )
        }
    }
    
    /**
     * Creates directories recursively.
     * 
     * @param path Directory path to create
     */
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
                    mkdir(currentPath, 0x1FFu) // 0777 permissions
                }
            }
        }
    }
}
