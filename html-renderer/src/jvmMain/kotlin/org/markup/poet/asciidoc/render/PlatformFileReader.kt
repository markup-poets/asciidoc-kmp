package org.markup.poet.asciidoc.render

import java.io.File
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Paths
import kotlin.io.path.readText

/**
 * JVM implementation of FileReader using java.io.File and java.nio.file APIs.
 * 
 * This implementation:
 * - Supports both absolute and relative file paths
 * - Resolves relative paths relative to the current working directory
 * - Returns Result.failure for file not found errors
 * - Returns Result.failure for read errors (permissions, I/O errors, etc.)
 * - Uses Files.readString() for efficient file reading
 */
actual class PlatformFileReader : FileReader {
    
    /**
     * Reads file content as a string using JVM file I/O APIs.
     * 
     * @param path File path (relative or absolute)
     * @return Result containing file content on success, or exception on failure
     */
    actual override fun readFile(path: String): Result<String> {
        return try {
            // Convert path string to Path object
            val filePath = Paths.get(path)
            
            // Check if file exists
            if (!Files.exists(filePath)) {
                return Result.failure(
                    CssException.FileNotFound(
                        path = path,
                        reason = "File does not exist"
                    )
                )
            }
            
            // Check if it's a regular file (not a directory)
            if (!Files.isRegularFile(filePath)) {
                return Result.failure(
                    CssException.FileNotFound(
                        path = path,
                        reason = "Path is not a regular file"
                    )
                )
            }
            
            // Read file content
            val content = filePath.readText()
            Result.success(content)
            
        } catch (e: NoSuchFileException) {
            Result.failure(
                CssException.FileNotFound(
                    path = path,
                    reason = "File not found: ${e.message}"
                )
            )
        } catch (e: SecurityException) {
            Result.failure(
                CssException.LoadingFailure(
                    reason = "Permission denied reading file '$path': ${e.message}"
                )
            )
        } catch (e: Exception) {
            Result.failure(
                CssException.LoadingFailure(
                    reason = "Failed to read file '$path': ${e.message}"
                )
            )
        }
    }
}
