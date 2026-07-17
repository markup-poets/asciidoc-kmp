package org.markup.poet.tck

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.posix.gettimeofday
import platform.posix.timeval
import org.markup.poet.tck.sync.GitOperations
import org.markup.poet.tck.sync.GitResult

actual fun getPlatformName(): String = "Linux"

@OptIn(ExperimentalForeignApi::class)
actual fun currentTimeMillis(): Long {
    memScoped {
        val tv = alloc<timeval>()
        gettimeofday(tv.ptr, null)
        return tv.tv_sec * 1000L + tv.tv_usec / 1000L
    }
}

/** No git on Linux native; sync is a JVM-side operation. */
actual class PlatformGitOperations actual constructor() : GitOperations {
    actual override suspend fun clone(url: String, destination: String, branch: String?): GitResult =
        GitResult.Failure(UNSUPPORTED)

    actual override suspend fun pull(repositoryPath: String): GitResult = GitResult.Failure(UNSUPPORTED)
    actual override fun getCurrentCommitHash(repositoryPath: String): String? = null
    actual override fun getCurrentRef(repositoryPath: String): String? = null
    actual override fun isValidRepository(repositoryPath: String): Boolean = false
    actual override fun getRemoteUrl(repositoryPath: String, remoteName: String): String? = null

    private companion object {
        const val UNSUPPORTED = "Git operations are not supported on Linux native"
    }
}
