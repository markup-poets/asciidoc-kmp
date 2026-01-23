package org.markup.poet.asciidoc.render

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.writeToFile

/**
 * iOS implementation of FileWriter using Foundation APIs.
 * 
 * Uses NSFileManager and NSString for file operations.
 * Creates parent directories if they don't exist.
 * Supports both absolute and relative file paths.
 */
@OptIn(ExperimentalForeignApi::class)
actual class PlatformFileWriter : FileWriter {
    actual override fun writeFile(path: String, content: String): Result<Unit> {
        return try {
            val fileManager = NSFileManager.defaultManager
            
            // Extract parent directory path
            val lastSlashIndex = path.lastIndexOf('/')
            if (lastSlashIndex > 0) {
                val parentDir = path.substring(0, lastSlashIndex)
                
                // Create parent directories if they don't exist
                if (!fileManager.fileExistsAtPath(parentDir)) {
                    val created = fileManager.createDirectoryAtPath(
                        path = parentDir,
                        withIntermediateDirectories = true,
                        attributes = null,
                        error = null
                    )
                    
                    if (!created) {
                        return Result.failure(
                            CssException.LoadingFailure(
                                reason = "Failed to create parent directory: $parentDir"
                            )
                        )
                    }
                }
            }
            
            // Write content to file
            val nsString = NSString.create(string = content)
            val success = nsString.writeToFile(
                path = path,
                atomically = true,
                encoding = NSUTF8StringEncoding,
                error = null
            )
            
            if (!success) {
                return Result.failure(
                    CssException.LoadingFailure(
                        reason = "Failed to write file: $path"
                    )
                )
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                CssException.LoadingFailure(
                    reason = "Failed to write file '$path': ${e.message}"
                )
            )
        }
    }
}
