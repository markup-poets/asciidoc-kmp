package org.markup.poet.tck

import org.markup.poet.tck.sync.GitOperations
import org.markup.poet.tck.sync.GitResult
import java.io.File
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.storage.file.FileRepositoryBuilder

actual fun getPlatformName(): String = "JVM"

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

/** The only target with a real git implementation — sync runs here. */
actual class PlatformGitOperations actual constructor() : GitOperations {
    actual override suspend fun clone(url: String, destination: String, branch: String?): GitResult {
        return try {
            val destFile = File(destination)
            if (destFile.exists()) return GitResult.Failure("Exists: $destination")
            destFile.parentFile?.mkdirs()
            val command = Git.cloneRepository().setURI(url).setDirectory(destFile)
            if (branch != null) command.setBranch(branch)
            val git = command.call()
            val ref = git.repository.fullBranch
            git.close()
            GitResult.Success("Cloned $ref")
        } catch (e: Exception) {
            GitResult.Failure("Clone failed: ${e.message}", e.message)
        }
    }

    actual override suspend fun pull(repositoryPath: String): GitResult {
        return try {
            val repo = openRepo(repositoryPath) ?: return GitResult.Failure("No repo")
            val git = Git(repo)
            val result = git.pull().call()
            val success = result.isSuccessful
            git.close()
            repo.close()
            if (success) GitResult.Success("Pulled") else GitResult.Failure("Pull failed")
        } catch (e: Exception) {
            GitResult.Failure("Pull failed: ${e.message}", e.message)
        }
    }

    actual override fun getCurrentCommitHash(repositoryPath: String): String? =
        openRepo(repositoryPath)?.use { it.resolve(Constants.HEAD)?.name }

    actual override fun getCurrentRef(repositoryPath: String): String? =
        openRepo(repositoryPath)?.use { it.fullBranch }

    actual override fun isValidRepository(repositoryPath: String): Boolean =
        openRepo(repositoryPath)?.use { true } ?: false

    actual override fun getRemoteUrl(repositoryPath: String, remoteName: String): String? =
        openRepo(repositoryPath)?.use { it.config.getString("remote", remoteName, "url") }

    private fun openRepo(path: String) = try {
        FileRepositoryBuilder().setGitDir(File(path, ".git")).readEnvironment().findGitDir().build()
    } catch (e: Exception) {
        null
    }
}
