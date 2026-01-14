package org.markup.poet.asciidoc.export

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.*
import platform.posix.errno
import platform.posix.strerror

/**
 * iOS implementation of file writing functionality.
 * Uses Foundation framework's NSFileManager and NSString APIs.
 */
@OptIn(ExperimentalForeignApi::class)
internal actual class FileWriter {
    actual fun writeToFile(path: String, content: String): FileWriteResult {
        return try {
            val fileManager = NSFileManager.defaultManager
            val nsPath = path as NSString
            val parentPath = nsPath.stringByDeletingLastPathComponent
            
            // Create parent directories if they don't exist
            if (parentPath.isNotEmpty() && !fileManager.fileExistsAtPath(parentPath)) {
                val created = fileManager.createDirectoryAtPath(
                    path = parentPath,
                    withIntermediateDirectories = true,
                    attributes = null,
                    error = null
                )
                if (!created) {
                    return FileWriteResult.Error(path, "Failed to create parent directories")
                }
            }
            
            // Write content to file
            val nsContent = content as NSString
            val written = nsContent.writeToFile(
                path = path,
                atomically = true,
                encoding = NSUTF8StringEncoding,
                error = null
            )
            
            if (written) {
                FileWriteResult.Success(path)
            } else {
                val errorMsg = strerror(errno)?.let { 
                    it.toString() 
                } ?: "Unknown error"
                FileWriteResult.Error(path, "Failed to write file: $errorMsg")
            }
        } catch (e: Exception) {
            FileWriteResult.Error(path, "Unexpected error: ${e.message}", e)
        }
    }
}
