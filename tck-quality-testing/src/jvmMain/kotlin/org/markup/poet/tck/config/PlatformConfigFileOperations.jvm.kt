package org.markup.poet.tck.config

import java.io.File

/**
 * JVM implementation of ConfigFileOperations using java.io.File.
 */
actual class PlatformConfigFileOperations : ConfigFileOperations {
    
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
}
