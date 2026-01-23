package org.markup.poet.asciidoc.render

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fseek
import platform.posix.ftell
import platform.posix.rewind
import platform.posix.SEEK_END

/**
 * Linux implementation of FileReader using POSIX APIs.
 * 
 * Uses standard C file I/O functions (fopen, fread, etc.) via Kotlin/Native interop.
 * Supports both absolute and relative file paths.
 */
@OptIn(ExperimentalForeignApi::class)
actual class PlatformFileReader : FileReader {
    actual override fun readFile(path: String): Result<String> {
        return try {
            val file = fopen(path, "r")
            
            if (file == null) {
                return Result.failure(
                    Exception("File not found: $path")
                )
            }
            
            try {
                // Get file size
                fseek(file, 0, SEEK_END)
                val size = ftell(file)
                rewind(file)
                
                if (size < 0) {
                    return Result.failure(
                        Exception("Failed to determine file size: $path")
                    )
                }
                
                // Read file content
                memScoped {
                    val buffer = allocArray<ByteVar>(size.toInt() + 1)
                    val bytesRead = fread(buffer, 1u, size.toULong(), file)
                    
                    if (bytesRead.toInt() != size.toInt()) {
                        return Result.failure(
                            Exception("Failed to read complete file: $path")
                        )
                    }
                    
                    Result.success(buffer.toKString())
                }
            } finally {
                fclose(file)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
