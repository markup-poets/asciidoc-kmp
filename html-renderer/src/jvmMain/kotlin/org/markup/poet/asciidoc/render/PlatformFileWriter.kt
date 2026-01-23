package org.markup.poet.asciidoc.render

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

/**
 * JVM implementation of FileWriter using java.io.File and java.nio.file APIs.
 * 
 * This implementation:
 * - Supports both absolute and relative file paths
 * - Resolves relative paths relative to the current working directory
 * - Creates parent directories if they don't exist
 * - Overwrites existing files
 * - Returns Result.failure for write errors (permissions, I/O errors, etc.)
 */
actual class PlatformFileWriter : FileWriter {
    
    /**
     * Writes content to a file using JVM file I/O APIs.
     * 
     * @param path File path (relative or absolute)
     * @param content Content to write to the file
     * @return Result indicating success or containing exception on failure
     */
    actual override fun writeFile(path: String, content: String): Result<Unit> {
        return try {
            // Convert path string to Path object
            val filePath = Paths.get(path)
            
            // Create parent directories if they don't exist
            filePath.parent?.let { parent ->
                Files.createDirectories(parent)
            }
            
            // Write content to file (overwrites if exists)
            Files.writeString(
                filePath,
                content,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            )
            
            Result.success(Unit)
            
        } catch (e: SecurityException) {
            Result.failure(
                CssException.LoadingFailure(
                    reason = "Permission denied writing file '$path': ${e.message}"
                )
            )
        } catch (e: Exception) {
            Result.failure(
                CssException.LoadingFailure(
                    reason = "Failed to write file '$path': ${e.message}"
                )
            )
        }
    }
}
