package org.markup.poet.asciidoc.render

import java.io.File

/**
 * Android implementation of FileWriter using java.io.File APIs.
 * 
 * Android uses the same JVM file APIs, but we use File.writeText() for better
 * compatibility with older Android versions (Files.writeString requires API 26+).
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
     * Writes content to a file using Android/JVM file I/O APIs.
     * 
     * @param path File path (relative or absolute)
     * @param content Content to write to the file
     * @return Result indicating success or containing exception on failure
     */
    actual override fun writeFile(path: String, content: String): Result<Unit> {
        return try {
            val file = File(path)
            
            // Create parent directories if they don't exist
            file.parentFile?.mkdirs()
            
            // Write content to file (overwrites if exists)
            file.writeText(content, Charsets.UTF_8)
            
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
