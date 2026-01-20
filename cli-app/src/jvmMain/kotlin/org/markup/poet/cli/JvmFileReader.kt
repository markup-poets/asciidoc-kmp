package org.markup.poet.cli

import org.markup.poet.asciidoc.processing.FileReader
import org.markup.poet.asciidoc.processing.FileReadResult
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/**
 * JVM implementation of FileReader using Java File I/O.
 * 
 * This implementation handles file reading operations for the JVM platform,
 * providing descriptive error messages for common failure scenarios.
 */
class JvmFileReader : FileReader {
    /**
     * Read the content of a file at the specified path.
     * 
     * @param path The file path to read (can be relative or absolute)
     * @return FileReadResult.Success with content if successful, FileReadResult.Error with message if failed
     */
    override fun readFile(path: String): FileReadResult {
        return try {
            val file = File(path)
            
            // Check if file exists
            if (!file.exists()) {
                return FileReadResult.Error("File not found: $path")
            }
            
            // Check if it's a file (not a directory)
            if (!file.isFile) {
                return FileReadResult.Error("Path is not a file: $path")
            }
            
            // Check if file is readable
            if (!file.canRead()) {
                return FileReadResult.Error("File is not readable: $path")
            }
            
            // Read file content
            val content = file.readText()
            FileReadResult.Success(content)
            
        } catch (e: FileNotFoundException) {
            // This shouldn't happen due to exists() check, but handle it anyway
            FileReadResult.Error("File not found: $path")
        } catch (e: IOException) {
            // Handle I/O errors with descriptive message
            FileReadResult.Error("Failed to read file '$path': ${e.message ?: "I/O error"}")
        } catch (e: SecurityException) {
            // Handle security/permission errors
            FileReadResult.Error("Permission denied reading file: $path")
        } catch (e: Exception) {
            // Catch any other unexpected errors
            FileReadResult.Error("Unexpected error reading file '$path': ${e.message ?: "Unknown error"}")
        }
    }
}
