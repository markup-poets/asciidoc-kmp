package org.markup.poet.tck.sync

import org.markup.poet.tck.currentTimeMillis
import org.markup.poet.tck.getPlatformName
import kotlinx.serialization.Serializable

/**
 * Platform-agnostic interface for Git operations.
 * 
 * This interface abstracts git operations to support multiple platforms:
 * - **JVM/Android**: Uses JGit (pure Java implementation)
 * - **iOS/Linux**: Uses native git command via process execution
 * 
 * All operations are designed to work with the official AsciiDoc TCK repository.
 * 
 * **Usage:**
 * ```kotlin
 * val gitOps = PlatformGitOperations()
 * val result = gitOps.clone(
 *     url = "https://gitlab.eclipse.org/eclipse/asciidoc-lang/asciidoc-tck.git",
 *     destination = "tck-quality-testing/official-tck/repository"
 * )
 * ```
 */
interface GitOperations {
    /**
     * Clone a git repository.
     * 
     * @param url The repository URL to clone
     * @param destination The local directory path for the cloned repository
     * @param branch Optional branch or tag to checkout (defaults to repository default branch)
     * @return GitResult indicating success or failure
     */
    suspend fun clone(url: String, destination: String, branch: String? = null): GitResult
    
    /**
     * Pull latest changes from the remote repository.
     * 
     * @param repositoryPath Path to the local git repository
     * @return GitResult indicating success or failure
     */
    suspend fun pull(repositoryPath: String): GitResult
    
    /**
     * Get the current commit hash (SHA) of the repository.
     * 
     * @param repositoryPath Path to the local git repository
     * @return The commit hash, or null if unable to determine
     */
    fun getCurrentCommitHash(repositoryPath: String): String?
    
    /**
     * Get the current branch or tag name.
     * 
     * @param repositoryPath Path to the local git repository
     * @return The branch/tag name, or null if unable to determine
     */
    fun getCurrentRef(repositoryPath: String): String?
    
    /**
     * Check if a directory is a valid git repository.
     * 
     * @param repositoryPath Path to check
     * @return true if the path contains a valid git repository, false otherwise
     */
    fun isValidRepository(repositoryPath: String): Boolean
    
    /**
     * Get the remote URL of the repository.
     * 
     * @param repositoryPath Path to the local git repository
     * @param remoteName Name of the remote (defaults to "origin")
     * @return The remote URL, or null if unable to determine
     */
    fun getRemoteUrl(repositoryPath: String, remoteName: String = "origin"): String?
}

/**
 * Result of a git operation.
 */
@Serializable
sealed class GitResult {
    @Serializable
    data class Success(val message: String) : GitResult()
    
    @Serializable
    data class Failure(val error: String, val exceptionMessage: String? = null) : GitResult()
}
