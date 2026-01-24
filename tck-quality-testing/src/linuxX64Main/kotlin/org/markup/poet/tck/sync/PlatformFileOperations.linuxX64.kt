package org.markup.poet.tck.sync

import kotlinx.cinterop.*
import platform.posix.*

/**
 * Linux implementation of platform-specific file operations.
 */

@OptIn(ExperimentalForeignApi::class)
actual fun platformFileExists(path: String): Boolean {
    return access(path, F_OK) == 0
}

@OptIn(ExperimentalForeignApi::class)
actual fun platformIsReadable(path: String): Boolean {
    return access(path, R_OK) == 0
}

@OptIn(ExperimentalForeignApi::class)
actual fun platformFindFiles(directory: String, suffix: String): List<String> {
    val command = "find $directory -type f -name '*$suffix' 2>/dev/null"
    val results = mutableListOf<String>()
    
    val fp = popen(command, "r") ?: return emptyList()
    
    try {
        val buffer = ByteArray(4096)
        while (true) {
            val line = fgets(buffer.refTo(0), buffer.size, fp)?.toKString()
            if (line == null) break
            val trimmed = line.trim()
            if (trimmed.isNotEmpty()) {
                results.add(trimmed)
            }
        }
    } finally {
        pclose(fp)
    }
    
    return results
}

@OptIn(ExperimentalForeignApi::class)
actual fun platformReadFile(path: String): String {
    val file = fopen(path, "r") ?: throw Exception("Cannot open file: $path")
    
    try {
        val buffer = StringBuilder()
        val chunk = ByteArray(4096)
        
        while (true) {
            val bytesRead = fread(chunk.refTo(0), 1.toULong(), chunk.size.toULong(), file)
            if (bytesRead == 0.toULong()) break
            
            buffer.append(chunk.decodeToString(0, bytesRead.toInt()))
        }
        
        return buffer.toString()
    } finally {
        fclose(file)
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun platformWriteFile(path: String, content: String) {
    // Create parent directories
    val lastSlash = path.lastIndexOf('/')
    if (lastSlash > 0) {
        val parentDir = path.substring(0, lastSlash)
        val mkdirCommand = "mkdir -p $parentDir"
        popen(mkdirCommand, "r")?.let { pclose(it) }
    }
    
    val file = fopen(path, "w") ?: throw Exception("Cannot open file for writing: $path")
    
    try {
        val bytes = content.encodeToByteArray()
        fwrite(bytes.refTo(0), 1.toULong(), bytes.size.toULong(), file)
    } finally {
        fclose(file)
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun platformDeleteDirectory(path: String) {
    val command = "rm -rf $path"
    popen(command, "r")?.let { pclose(it) }
}

@OptIn(ExperimentalForeignApi::class)
actual fun currentTimeMillis(): Long {
    memScoped {
        val tv = alloc<timeval>()
        gettimeofday(tv.ptr, null)
        return tv.tv_sec * 1000L + tv.tv_usec / 1000L
    }
}
