package org.markup.poet.tck.sync

import java.io.File

/**
 * JVM implementation of platform-specific file operations.
 */

actual fun platformFileExists(path: String): Boolean {
    return File(path).exists()
}

actual fun platformIsReadable(path: String): Boolean {
    val file = File(path)
    return file.exists() && file.canRead()
}

actual fun platformFindFiles(directory: String, suffix: String): List<String> {
    val dir = File(directory)
    if (!dir.exists() || !dir.isDirectory) {
        return emptyList()
    }
    
    val results = mutableListOf<String>()
    dir.walkTopDown().forEach { file ->
        if (file.isFile && file.name.endsWith(suffix)) {
            results.add(file.absolutePath)
        }
    }
    return results
}

actual fun platformReadFile(path: String): String {
    return File(path).readText()
}

actual fun platformWriteFile(path: String, content: String) {
    val file = File(path)
    file.parentFile?.mkdirs()
    file.writeText(content)
}

actual fun platformDeleteDirectory(path: String) {
    File(path).deleteRecursively()
}

actual fun currentTimeMillis(): Long {
    return System.currentTimeMillis()
}
