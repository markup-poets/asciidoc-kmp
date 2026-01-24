package org.markup.poet.tck.config

import kotlinx.cinterop.*
import platform.posix.*

/**
 * Linux implementation of ConfigFileOperations using POSIX APIs.
 */
actual class PlatformConfigFileOperations : ConfigFileOperations {
    
    override fun readFile(path: String): String? {
        return try {
            val file = fopen(path, "r") ?: return null
            
            try {
                // Get file size
                fseek(file, 0, SEEK_END)
                val size = ftell(file)
                fseek(file, 0, SEEK_SET)
                
                if (size <= 0) return null
                
                // Read content
                memScoped {
                    val buffer = allocArray<ByteVar>(size.toInt() + 1)
                    val bytesRead = fread(buffer, 1u, size.toULong(), file)
                    
                    if (bytesRead.toInt() != size.toInt()) {
                        return null
                    }
                    
                    buffer[size.toInt()] = 0 // Null terminate
                    buffer.toKString()
                }
            } finally {
                fclose(file)
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override fun writeFile(path: String, content: String) {
        // Create parent directories if they don't exist
        val lastSlash = path.lastIndexOf('/')
        if (lastSlash > 0) {
            val parentPath = path.substring(0, lastSlash)
            createDirectories(parentPath)
        }
        
        // Write content
        val file = fopen(path, "w") ?: throw Exception("Failed to open file for writing: $path")
        
        try {
            content.encodeToByteArray().usePinned { pinned ->
                fwrite(pinned.addressOf(0), 1u, content.length.toULong(), file)
            }
        } finally {
            fclose(file)
        }
    }
    
    /**
     * Create directories recursively.
     */
    private fun createDirectories(path: String) {
        val parts = path.split('/')
        var currentPath = if (path.startsWith('/')) "/" else ""
        
        for (part in parts) {
            if (part.isEmpty()) continue
            
            currentPath += if (currentPath.endsWith('/')) part else "/$part"
            
            if (access(currentPath, F_OK) != 0) {
                mkdir(currentPath, 0x1FFu) // 0777 permissions
            }
        }
    }
}
