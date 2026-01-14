package org.markup.poet.asciidoc.processing

import java.io.File
import java.io.IOException

/**
 * Android implementation of FileReader using Java file I/O.
 */
class AndroidFileReader : FileReader {
    override fun readFile(path: String): FileReadResult {
        return try {
            val file = File(path)
            if (!file.exists()) {
                FileReadResult.Error("File not found: $path")
            } else if (!file.isFile) {
                FileReadResult.Error("Path is not a file: $path")
            } else if (!file.canRead()) {
                FileReadResult.Error("File is not readable: $path")
            } else {
                val content = file.readText()
                FileReadResult.Success(content)
            }
        } catch (e: IOException) {
            FileReadResult.Error("Failed to read file '$path': ${e.message}")
        } catch (e: SecurityException) {
            FileReadResult.Error("Access denied to file '$path': ${e.message}")
        } catch (e: Exception) {
            FileReadResult.Error("Unexpected error reading file '$path': ${e.message}")
        }
    }
}
