package org.markup.poet.tck

import org.markup.poet.tck.sync.GitOperations
import org.markup.poet.tck.sync.GitResult

actual fun getPlatformName(): String = "Android"

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

/** No git on Android; sync is a JVM-side operation. */
actual class PlatformGitOperations actual constructor() : GitOperations {
    actual override suspend fun clone(url: String, destination: String, branch: String?): GitResult =
        GitResult.Failure(UNSUPPORTED)

    actual override suspend fun pull(repositoryPath: String): GitResult = GitResult.Failure(UNSUPPORTED)
    actual override fun getCurrentCommitHash(repositoryPath: String): String? = null
    actual override fun getCurrentRef(repositoryPath: String): String? = null
    actual override fun isValidRepository(repositoryPath: String): Boolean = false
    actual override fun getRemoteUrl(repositoryPath: String, remoteName: String): String? = null

    private companion object {
        const val UNSUPPORTED = "Git operations are not supported on Android"
    }
}
