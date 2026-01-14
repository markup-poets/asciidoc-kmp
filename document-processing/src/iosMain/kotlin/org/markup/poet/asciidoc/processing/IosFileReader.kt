package org.markup.poet.asciidoc.processing

import kotlinx.cinterop.*
import platform.posix.*

/**
 * iOS implementation of FileReader using POSIX file I/O.
 */
@OptIn(ExperimentalForeignApi::class)
class IosFileReader : FileReader {
    override fun readFile(path: String): FileReadResult {
        return try {
            // Check if file exists
            if (access(path, F_OK) != 0) {
                return FileReadResult.Error("File not found: $path")
            }
            
            // Check if file is readable
            if (access(path, R_OK) != 0) {
                return FileReadResult.Error("File is not readable: $path")
            }
            
            // Open file
            val file = fopen(path, "r")
            if (file == null) {
                return FileReadResult.Error("Failed to open file: $path")
            }
            
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
                memScoped {
                    val buffer = allocArray<ByteVar>(size.toInt() + 1)
                    val bytesRead = fread(buffer, 1u, size.toULong(), file)
                    
                    if (bytesRead.toLong() != size) {
                        return FileReadResult.Error("Failed to read complete file: $path")
                    }
                    
                    buffer[size.toInt()] = 0.toByte()
                    val content = buffer.toKString()
                    FileReadResult.Success(content)
                }
            } finally {
                fclose(file)
            }
        } catch (e: Exception) {
            FileReadResult.Error("Unexpected error reading file '$path': ${e.message}")
        }
    }
}
