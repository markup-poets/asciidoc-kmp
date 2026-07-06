package org.markup.poet.tck

import kotlinx.cinterop.*
import platform.posix.*
import org.markup.poet.tck.config.ConfigFileOperations
import org.markup.poet.tck.sync.GitOperations
import org.markup.poet.tck.sync.GitResult
import org.markup.poet.tck.version.VersionFileOperations

actual fun getPlatformName(): String = "Linux"

@OptIn(ExperimentalForeignApi::class)
actual fun currentTimeMillis(): Long {
    memScoped {
        val tv = alloc<timeval>()
        gettimeofday(tv.ptr, null)
        return tv.tv_sec * 1000L + tv.tv_usec / 1000L
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun platformWriteFile(path: String, content: String) {
    val file = fopen(path, "w") ?: return
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
    val fp = popen(command, "r")
    if (fp != null) pclose(fp)
}

@OptIn(ExperimentalForeignApi::class)
actual fun platformFileExists(path: String): Boolean = access(path, F_OK) == 0

@OptIn(ExperimentalForeignApi::class)
actual fun platformIsReadable(path: String): Boolean = access(path, R_OK) == 0

@OptIn(ExperimentalForeignApi::class)
actual fun platformFindFiles(directory: String, suffix: String): List<String> = emptyList() // Stub for simplicity

@OptIn(ExperimentalForeignApi::class)
actual fun platformReadFile(path: String): String {
    val file = fopen(path, "r") ?: return ""
    try {
        val sb = StringBuilder()
        val buffer = ByteArray(4096)
        while (true) {
            val read = fread(buffer.refTo(0), 1.toULong(), buffer.size.toULong(), file)
            if (read == 0.toULong()) break
            sb.append(buffer.decodeToString(0, read.toInt()))
        }
        return sb.toString()
    } finally {
        fclose(file)
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun platformDeleteFile(path: String) {
    remove(path)
}

actual class PlatformConfigFileOperations actual constructor() : ConfigFileOperations {
    // platformReadFile returns "" for unreadable files, but the ConfigFileOperations
    // contract (and the JVM actual) requires null for a missing file so the caller
    // can fall back to defaults instead of failing to parse an empty document.
    actual override fun readFile(path: String): String? =
        if (platformFileExists(path)) platformReadFile(path) else null
    actual override fun writeFile(path: String, content: String) { platformWriteFile(path, content) }
}

actual class PlatformGitOperations actual constructor() : GitOperations {
    actual override suspend fun clone(url: String, destination: String, branch: String?): GitResult = GitResult.Failure("Stub")
    actual override suspend fun pull(repositoryPath: String): GitResult = GitResult.Failure("Stub")
    actual override fun getCurrentCommitHash(repositoryPath: String): String? = null
    actual override fun getCurrentRef(repositoryPath: String): String? = null
    actual override fun isValidRepository(repositoryPath: String): Boolean = false
    actual override fun getRemoteUrl(repositoryPath: String, remoteName: String): String? = null
}

actual class PlatformVersionFileOperations actual constructor() : VersionFileOperations {
    // See PlatformConfigFileOperations.readFile: null (not "") for missing files.
    actual override fun readFile(path: String): String? =
        if (platformFileExists(path)) platformReadFile(path) else null
    actual override fun writeFile(path: String, content: String) { platformWriteFile(path, content) }
    actual override fun deleteFile(path: String) { platformDeleteFile(path) }
    actual override fun fileExists(path: String): Boolean = platformFileExists(path)
}
