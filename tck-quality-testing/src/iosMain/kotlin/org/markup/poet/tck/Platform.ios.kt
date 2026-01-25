package org.markup.poet.tck

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.posix.*
import org.markup.poet.tck.config.ConfigFileOperations
import org.markup.poet.tck.sync.GitOperations
import org.markup.poet.tck.sync.GitResult
import org.markup.poet.tck.version.VersionFileOperations

actual fun getPlatformName(): String = "iOS"

actual fun currentTimeMillis(): Long {
    return (NSDate().timeIntervalSince1970 * 1000).toLong()
}

@OptIn(ExperimentalForeignApi::class)
actual fun platformWriteFile(path: String, content: String) {
    val nsPath = path as NSString
    val nsContent = content as NSString
    nsContent.writeToFile(path, true, NSUTF8StringEncoding, null)
}

@OptIn(ExperimentalForeignApi::class)
actual fun platformDeleteDirectory(path: String) {
    NSFileManager.defaultManager.removeItemAtPath(path, null)
}

@OptIn(ExperimentalForeignApi::class)
actual fun platformFileExists(path: String): Boolean = NSFileManager.defaultManager.fileExistsAtPath(path)

@OptIn(ExperimentalForeignApi::class)
actual fun platformIsReadable(path: String): Boolean = NSFileManager.defaultManager.isReadableFileAtPath(path)

@OptIn(ExperimentalForeignApi::class)
actual fun platformFindFiles(directory: String, suffix: String): List<String> = emptyList()

@OptIn(ExperimentalForeignApi::class)
actual fun platformReadFile(path: String): String {
    return NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null) ?: ""
}

@OptIn(ExperimentalForeignApi::class)
actual fun platformDeleteFile(path: String) {
    NSFileManager.defaultManager.removeItemAtPath(path, null)
}

actual class PlatformConfigFileOperations actual constructor() : ConfigFileOperations {
    actual override fun readFile(path: String): String? = try { platformReadFile(path) } catch(e: Exception) { null }
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
    actual override fun readFile(path: String): String? = try { platformReadFile(path) } catch(e: Exception) { null }
    actual override fun writeFile(path: String, content: String) { platformWriteFile(path, content) }
    actual override fun deleteFile(path: String) { platformDeleteFile(path) }
    actual override fun fileExists(path: String): Boolean = platformFileExists(path)
}
