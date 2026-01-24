package org.markup.poet.tck.sync

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File
import java.io.IOException

/**
 * JVM implementation of GitOperations using JGit library.
 * 
 * JGit is a pure Java implementation of Git, which means:
 * - No external git binary required
 * - Works on any JVM platform (JVM, Android)
 * - Thread-safe and well-tested
 * 
 * **Note**: This implementation does NOT use JavaScript or Node.js.
 * It's a pure Java/Kotlin implementation.
 */
actual class PlatformGitOperations : GitOperations {
    
    override suspend fun clone(url: String, destination: String, branch: String?): GitResult {
        return try {
            val destFile = File(destination)
            
            // Check if destination already exists
            if (destFile.exists()) {
                return GitResult.Failure("Destination directory already exists: $destination")
            }
            
            // Create parent directories if needed
            destFile.parentFile?.mkdirs()
            
            // Clone the repository
            val cloneCommand = Git.cloneRepository()
                .setURI(url)
                .setDirectory(destFile)
            
            // Set branch if specified
            if (branch != null) {
                cloneCommand.setBranch(branch)
            }
            
            cloneCommand.call().use { git ->
                val ref = git.repository.fullBranch
                GitResult.Success("Successfully cloned repository to $destination (ref: $ref)")
            }
        } catch (e: GitAPIException) {
            GitResult.Failure("Git clone failed: ${e.message}", e)
        } catch (e: IOException) {
            GitResult.Failure("I/O error during clone: ${e.message}", e)
        } catch (e: Exception) {
            GitResult.Failure("Unexpected error during clone: ${e.message}", e)
        }
    }
    
    override suspend fun pull(repositoryPath: String): GitResult {
        return try {
            val repo = openRepository(repositoryPath)
                ?: return GitResult.Failure("Not a valid git repository: $repositoryPath")
            
            Git(repo).use { git ->
                val result = git.pull().call()
                
                if (result.isSuccessful) {
                    val fetchResult = result.fetchResult
                    val mergeResult = result.mergeResult
                    GitResult.Success(
                        "Successfully pulled changes. " +
                        "Fetch: ${fetchResult?.messages ?: "OK"}, " +
                        "Merge: ${mergeResult?.mergeStatus?.name ?: "OK"}"
                    )
                } else {
                    GitResult.Failure("Pull failed: ${result.mergeResult?.mergeStatus?.name ?: "Unknown error"}")
                }
            }
        } catch (e: GitAPIException) {
            GitResult.Failure("Git pull failed: ${e.message}", e)
        } catch (e: IOException) {
            GitResult.Failure("I/O error during pull: ${e.message}", e)
        } catch (e: Exception) {
            GitResult.Failure("Unexpected error during pull: ${e.message}", e)
        }
    }
    
    override fun getCurrentCommitHash(repositoryPath: String): String? {
        return try {
            val repo = openRepository(repositoryPath) ?: return null
            repo.use {
                val head = it.resolve(Constants.HEAD)
                head?.name
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override fun getCurrentRef(repositoryPath: String): String? {
        return try {
            val repo = openRepository(repositoryPath) ?: return null
            repo.use {
                it.fullBranch
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override fun isValidRepository(repositoryPath: String): Boolean {
        return try {
            val repo = openRepository(repositoryPath)
            repo?.use { true } ?: false
        } catch (e: Exception) {
            false
        }
    }
    
    override fun getRemoteUrl(repositoryPath: String, remoteName: String): String? {
        return try {
            val repo = openRepository(repositoryPath) ?: return null
            repo.use {
                val config = it.config
                config.getString("remote", remoteName, "url")
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Open a git repository from a file path.
     * 
     * @param repositoryPath Path to the repository
     * @return Repository instance, or null if not a valid repository
     */
    private fun openRepository(repositoryPath: String): Repository? {
        return try {
            val gitDir = File(repositoryPath, ".git")
            if (!gitDir.exists()) {
                // Try treating the path itself as a git directory
                val altGitDir = File(repositoryPath)
                if (!altGitDir.exists()) {
                    return null
                }
            }
            
            FileRepositoryBuilder()
                .setGitDir(if (gitDir.exists()) gitDir else File(repositoryPath))
                .readEnvironment()
                .findGitDir()
                .build()
        } catch (e: Exception) {
            null
        }
    }
}
