package org.markup.poet.html.cli

import kotlinx.cinterop.*
import platform.posix.*

@OptIn(ExperimentalForeignApi::class)
actual fun readFileContent(path: String): String {
    val file = fopen(path, "r") ?: throw Exception("Cannot open file: $path")
    try {
        // Get file size
        fseek(file, 0, SEEK_END)
        val size = ftell(file)
        fseek(file, 0, SEEK_SET)
        
        // Read content
        return memScoped {
            val buffer = allocArray<ByteVar>(size.toInt() + 1)
            fread(buffer, 1.toULong(), size.toULong(), file)
            buffer[size.toInt()] = 0
            buffer.toKString()
        }
    } finally {
        fclose(file)
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun writeFileContent(path: String, content: String) {
    val file = fopen(path, "w") ?: throw Exception("Cannot create file: $path")
    try {
        content.encodeToByteArray().usePinned { pinned ->
            fwrite(pinned.addressOf(0), 1.toULong(), content.length.toULong(), file)
        }
    } finally {
        fclose(file)
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun fileExists(path: String): Boolean {
    return memScoped {
        val stat = alloc<stat>()
        platform.posix.stat(path, stat.ptr) == 0
    }
}

actual fun exitProcess(code: Int): Nothing {
    platform.posix.exit(code)
    throw Error("Should not reach here")
}
