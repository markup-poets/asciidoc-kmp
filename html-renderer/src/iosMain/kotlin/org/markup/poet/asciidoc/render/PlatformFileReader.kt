package org.markup.poet.asciidoc.render

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile

/**
 * iOS implementation of FileReader using Foundation APIs.
 * 
 * Uses NSFileManager and NSString for file operations.
 * Supports both absolute and relative file paths.
 */
@OptIn(ExperimentalForeignApi::class)
actual class PlatformFileReader : FileReader {
    actual override fun readFile(path: String): Result<String> {
        return try {
            val fileManager = NSFileManager.defaultManager
            
            // Check if file exists
            if (!fileManager.fileExistsAtPath(path)) {
                return Result.failure(
                    Exception("File not found: $path")
                )
            }
            
            // Read file content
            val content = NSString.stringWithContentsOfFile(
                path = path,
                encoding = NSUTF8StringEncoding,
                error = null
            )
            
            if (content == null) {
                return Result.failure(
                    Exception("Failed to read file: $path")
                )
            }
            
            Result.success(content as String)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
