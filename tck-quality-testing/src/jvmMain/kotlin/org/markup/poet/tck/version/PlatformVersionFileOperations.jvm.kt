package org.markup.poet.tck.version

import java.io.File

/**
 * JVM implementation of VersionFileOperations using java.io.File.
 */
actual class PlatformVersionFileOperations : VersionFileOperations {
    
    override fun readFile(path: String): String? {
        return try {
            val file = File(path)
            if (file.exists() && file.isFile) {
                file.readText()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override fun writeFile(path: String, content: String) {
        val file = File(path)
        
        // Create parent directories if they don't exist
        file.parentFile?.mkdirs()
        
        // Write content
        file.writeText(content)
    }
    
    override fun deleteFile(path: String) {
        val file = File(path)
        if (file.exists()) {
            file.delete()
        }
    }
    
    override fun fileExists(path: String): Boolean {
        return File(path).exists()
    }
}
