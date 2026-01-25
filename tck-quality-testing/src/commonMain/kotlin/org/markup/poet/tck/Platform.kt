package org.markup.poet.tck

import org.markup.poet.tck.config.ConfigFileOperations
import org.markup.poet.tck.sync.GitOperations
import org.markup.poet.tck.sync.GitResult
import org.markup.poet.tck.version.VersionFileOperations

/**
 * Get the current platform name.
 */
expect fun getPlatformName(): String

/**
 * Get current time in milliseconds.
 */
expect fun currentTimeMillis(): Long

/**
 * Platform-specific file write operation.
 */
expect fun platformWriteFile(path: String, content: String)

/**
 * Platform-specific directory deletion.
 */
expect fun platformDeleteDirectory(path: String)

/**
 * Platform-specific check if a file exists.
 */
expect fun platformFileExists(path: String): Boolean

/**
 * Platform-specific check if a file is readable.
 */
expect fun platformIsReadable(path: String): Boolean

/**
 * Platform-specific file search by suffix.
 */
expect fun platformFindFiles(directory: String, suffix: String): List<String>

/**
 * Platform-specific file read.
 */
expect fun platformReadFile(path: String): String

/**
 * Platform-specific file delete.
 */
expect fun platformDeleteFile(path: String)

/**
 * Platform-specific implementation of ConfigFileOperations.
 */
expect class PlatformConfigFileOperations() : ConfigFileOperations {
    override fun readFile(path: String): String?
    override fun writeFile(path: String, content: String)
}

/**
 * Platform-specific implementation of GitOperations.
 */
expect class PlatformGitOperations() : GitOperations {
    override suspend fun clone(url: String, destination: String, branch: String?): GitResult
    override suspend fun pull(repositoryPath: String): GitResult
    override fun getCurrentCommitHash(repositoryPath: String): String?
    override fun getCurrentRef(repositoryPath: String): String?
    override fun isValidRepository(repositoryPath: String): Boolean
    override fun getRemoteUrl(repositoryPath: String, remoteName: String): String?
}

/**
 * Platform-specific implementation of VersionFileOperations.
 */
expect class PlatformVersionFileOperations() : VersionFileOperations {
    override fun readFile(path: String): String?
    override fun writeFile(path: String, content: String)
    override fun deleteFile(path: String)
    override fun fileExists(path: String): Boolean
}
