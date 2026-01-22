package org.markup.poet.antora

import kotlinx.cinterop.*
import platform.posix.*

/**
 * iOS implementation of FileSystemAccess using POSIX APIs.
 */
@OptIn(ExperimentalForeignApi::class)
actual class DefaultFileSystemAccess actual constructor() : FileSystemAccess {
    
    actual override fun exists(path: String): Boolean {
        return access(path, F_OK) == 0
    }
    
    actual override fun isDirectory(path: String): Boolean {
        memScoped {
            val statBuf = alloc<stat>()
            if (stat(path, statBuf.ptr) != 0) {
                return false
            }
            // Check if the mode indicates a directory
            val mode = statBuf.st_mode.toUInt()
            return (mode and S_IFMT.toUInt()) == S_IFDIR.toUInt()
        }
    }
    
    actual override fun readFile(path: String): FileReadResult {
        val file = fopen(path, "r") ?: return FileReadResult.Error("File not found: $path")
        
        try {
            // Get file size
            fseek(file, 0, SEEK_END)
            val size = ftell(file)
            fseek(file, 0, SEEK_SET)
            
            if (size < 0) {
                return FileReadResult.Error("Failed to determine file size: $path")
            }
            
            if (size == 0L) {
                return FileReadResult.Success("")
            }
            
            // Read file content
            return memScoped {
                val buffer = allocArray<ByteVar>(size.toInt() + 1)
                val bytesRead = fread(buffer, 1u, size.toULong(), file)
                
                if (bytesRead.toLong() != size) {
                    return FileReadResult.Error("Failed to read complete file: $path")
                }
                
                buffer[size.toInt()] = 0 // Null terminate
                val content = buffer.toKString()
                FileReadResult.Success(content)
            }
        } finally {
            fclose(file)
        }
    }
    
    actual override fun listDirectory(path: String): List<String> {
        val dir = opendir(path) ?: return emptyList()
        
        try {
            val entries = mutableListOf<String>()
            
            while (true) {
                val entry = readdir(dir) ?: break
                val name = entry.pointed.d_name.toKString()
                
                // Skip . and ..
                if (name != "." && name != "..") {
                    entries.add("$path/$name")
                }
            }
            
            return entries
        } finally {
            closedir(dir)
        }
    }
    
    actual override fun writeFile(path: String, content: String): FileWriteResult {
        // Create parent directories if needed
        val lastSlash = path.lastIndexOf('/')
        if (lastSlash > 0) {
            val parentDir = path.substring(0, lastSlash)
            createDirectories(parentDir)
        }
        
        val file = fopen(path, "w") ?: return FileWriteResult.Error("Failed to open file for writing: $path")
        
        try {
            val bytes = content.encodeToByteArray()
            val written = fwrite(bytes.refTo(0), 1u, bytes.size.toULong(), file)
            
            if (written.toInt() != bytes.size) {
                return FileWriteResult.Error("Failed to write complete file: $path")
            }
            
            return FileWriteResult.Success
        } finally {
            fclose(file)
        }
    }
    
    private fun createDirectories(path: String) {
        val parts = path.split('/')
        var currentPath = if (path.startsWith('/')) "/" else ""
        
        for (part in parts) {
            if (part.isEmpty()) continue
            
            currentPath += if (currentPath.endsWith('/')) part else "/$part"
            
            if (!exists(currentPath)) {
                mkdir(currentPath, 0x1FFu) // 0777 permissions
            }
        }
    }
}
