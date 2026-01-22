package org.markup.poet.antora

import java.io.File
import java.io.IOException

/**
 * JVM implementation of FileSystemAccess using java.io.File.
 */
actual class DefaultFileSystemAccess actual constructor() : FileSystemAccess {
    
    actual override fun exists(path: String): Boolean {
        return try {
            File(path).exists()
        } catch (e: SecurityException) {
            false
        }
    }
    
    actual override fun isDirectory(path: String): Boolean {
        return try {
            File(path).isDirectory
        } catch (e: SecurityException) {
            false
        }
    }
    
    actual override fun readFile(path: String): FileReadResult {
        return try {
            val file = File(path)
            if (!file.exists()) {
                return FileReadResult.Error("File not found: $path")
            }
            if (!file.isFile) {
                return FileReadResult.Error("Path is not a file: $path")
            }
            val content = file.readText(Charsets.UTF_8)
            FileReadResult.Success(content)
        } catch (e: IOException) {
            FileReadResult.Error("Failed to read file: ${e.message}")
        } catch (e: SecurityException) {
            FileReadResult.Error("Access denied: ${e.message}")
        }
    }
    
    actual override fun listDirectory(path: String): List<String> {
        return try {
            val dir = File(path)
            if (!dir.exists() || !dir.isDirectory) {
                return emptyList()
            }
            dir.listFiles()?.map { it.absolutePath } ?: emptyList()
        } catch (e: SecurityException) {
            emptyList()
        }
    }
    
    actual override fun writeFile(path: String, content: String): FileWriteResult {
        return try {
            val file = File(path)
            // Create parent directories if they don't exist
            file.parentFile?.mkdirs()
            file.writeText(content, Charsets.UTF_8)
            FileWriteResult.Success
        } catch (e: IOException) {
            FileWriteResult.Error("Failed to write file: ${e.message}")
        } catch (e: SecurityException) {
            FileWriteResult.Error("Access denied: ${e.message}")
        }
    }
}
