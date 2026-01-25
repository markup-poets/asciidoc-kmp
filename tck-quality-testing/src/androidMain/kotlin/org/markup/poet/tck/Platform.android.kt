package org.markup.poet.tck

import org.markup.poet.tck.config.ConfigFileOperations
import org.markup.poet.tck.sync.GitOperations
import org.markup.poet.tck.sync.GitResult
import org.markup.poet.tck.version.VersionFileOperations

actual fun getPlatformName(): String = "Android"
actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun platformWriteFile(path: String, content: String) {
    java.io.File(path).writeText(content)
}

actual fun platformDeleteDirectory(path: String) {
    java.io.File(path).deleteRecursively()
}

actual fun platformFileExists(path: String): Boolean = java.io.File(path).exists()
actual fun platformIsReadable(path: String): Boolean = java.io.File(path).canRead()
actual fun platformFindFiles(directory: String, suffix: String): List<String> = emptyList()
actual fun platformReadFile(path: String): String = java.io.File(path).readText()
actual fun platformDeleteFile(path: String) { java.io.File(path).delete() }

actual class PlatformConfigFileOperations actual constructor() : ConfigFileOperations {
    actual override fun readFile(path: String): String? = java.io.File(path).let { if (it.exists()) it.readText() else null }
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
    actual override fun readFile(path: String): String? = java.io.File(path).let { if (it.exists()) it.readText() else null }
    actual override fun writeFile(path: String, content: String) { platformWriteFile(path, content) }
    actual override fun deleteFile(path: String) { platformDeleteFile(path) }
    actual override fun fileExists(path: String): Boolean = platformFileExists(path)
}
