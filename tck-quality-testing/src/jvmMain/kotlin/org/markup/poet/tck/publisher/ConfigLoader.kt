package org.markup.poet.tck.publisher

import java.io.File

/**
 * Loader for publishing configuration from files or environment variables.
 *
 * The configuration can be loaded from:
 * 1. A JSON configuration file
 * 2. Environment variables
 * 3. Gradle project properties
 *
 * ## Configuration File Format
 * ```json
 * {
 *   "repositoryUrl": "github.com/user/repo.git",
 *   "branch": "gh-pages",
 *   "baseUrl": "https://user.github.io/repo",
 *   "authorName": "TCK Bot",
 *   "authorEmail": "tck-bot@example.com",
 *   "commitMessage": "Update TCK results"
 * }
 * ```
 *
 * **Note**: The GitHub token should NEVER be in the configuration file.
 * It must be provided via the `GITHUB_TOKEN` environment variable.
 *
 * ## Environment Variables
 * - `GITHUB_TOKEN`: GitHub personal access token (required)
 * - `GITHUB_REPOSITORY`: Repository in format "owner/repo" (optional)
 * - `GITHUB_PAGES_BRANCH`: Target branch (default: "gh-pages")
 *
 * ## Example Usage
 * ```kotlin
 * // Load from file
 * val config = ConfigLoader.loadFromFile("publish-config.json")
 *
 * // Load from environment
 * val config = ConfigLoader.loadFromEnvironment()
 *
 * // Load with defaults
 * val config = ConfigLoader.loadWithDefaults(
 *     repositoryUrl = "github.com/user/repo.git"
 * )
 * ```
 */
object ConfigLoader {
    
    /**
     * Load configuration from a JSON file.
     *
     * The GitHub token is always loaded from the GITHUB_TOKEN environment variable,
     * never from the file (for security).
     *
     * @param filePath Path to the JSON configuration file
     * @return PublishConfig loaded from the file
     * @throws IllegalArgumentException if the file doesn't exist or is invalid
     */
    fun loadFromFile(filePath: String): PublishConfig {
        val file = File(filePath)
        if (!file.exists()) {
            throw IllegalArgumentException("Configuration file not found: $filePath")
        }
        
        val json = file.readText()
        return parseConfig(json)
    }
    
    /**
     * Load configuration from environment variables.
     *
     * Required environment variables:
     * - GITHUB_TOKEN: GitHub personal access token
     * - GITHUB_REPOSITORY: Repository in format "owner/repo"
     *
     * Optional environment variables:
     * - GITHUB_PAGES_BRANCH: Target branch (default: "gh-pages")
     * - GITHUB_PAGES_BASE_URL: Base URL (auto-generated if not provided)
     *
     * @return PublishConfig loaded from environment variables
     * @throws IllegalArgumentException if required variables are missing
     */
    fun loadFromEnvironment(): PublishConfig {
        val token = System.getenv("GITHUB_TOKEN")
            ?: throw IllegalArgumentException("GITHUB_TOKEN environment variable is required")
        
        val repository = System.getenv("GITHUB_REPOSITORY")
            ?: throw IllegalArgumentException("GITHUB_REPOSITORY environment variable is required (format: owner/repo)")
        
        val branch = System.getenv("GITHUB_PAGES_BRANCH") ?: "gh-pages"
        
        // Parse repository to extract owner and repo name
        val parts = repository.split("/")
        if (parts.size != 2) {
            throw IllegalArgumentException("GITHUB_REPOSITORY must be in format 'owner/repo', got: $repository")
        }
        
        val (owner, repo) = parts
        val repositoryUrl = "github.com/$owner/$repo.git"
        val baseUrl = System.getenv("GITHUB_PAGES_BASE_URL") ?: "https://$owner.github.io/$repo"
        
        return PublishConfig(
            githubToken = token,
            repositoryUrl = repositoryUrl,
            branch = branch,
            baseUrl = baseUrl,
            authorName = "TCK Bot",
            authorEmail = "tck-bot@github.com",
            commitMessage = "Update TCK results"
        )
    }
    
    /**
     * Load configuration with defaults and overrides.
     *
     * This method provides sensible defaults and allows overriding specific values.
     * The GitHub token is always loaded from the GITHUB_TOKEN environment variable.
     *
     * @param repositoryUrl Repository URL (required)
     * @param branch Target branch (default: "gh-pages")
     * @param baseUrl Base URL for GitHub Pages (auto-generated if null)
     * @param authorName Git commit author name (default: "TCK Bot")
     * @param authorEmail Git commit author email (default: "tck-bot@github.com")
     * @param commitMessage Git commit message (default: "Update TCK results")
     * @return PublishConfig with the specified values
     */
    fun loadWithDefaults(
        repositoryUrl: String,
        branch: String = "gh-pages",
        baseUrl: String? = null,
        authorName: String = "TCK Bot",
        authorEmail: String = "tck-bot@github.com",
        commitMessage: String = "Update TCK results"
    ): PublishConfig {
        val token = System.getenv("GITHUB_TOKEN")
        
        // Auto-generate base URL if not provided
        val finalBaseUrl = baseUrl ?: run {
            // Extract owner/repo from repository URL
            // Format: github.com/owner/repo.git
            val urlParts = repositoryUrl.replace(".git", "").split("/")
            if (urlParts.size >= 2) {
                val owner = urlParts[urlParts.size - 2]
                val repo = urlParts[urlParts.size - 1]
                "https://$owner.github.io/$repo"
            } else {
                throw IllegalArgumentException("Cannot auto-generate base URL from repository URL: $repositoryUrl")
            }
        }
        
        return PublishConfig(
            githubToken = token,
            repositoryUrl = repositoryUrl,
            branch = branch,
            baseUrl = finalBaseUrl,
            authorName = authorName,
            authorEmail = authorEmail,
            commitMessage = commitMessage
        )
    }
    
    /**
     * Parse configuration from JSON string.
     *
     * This is a simple JSON parser for MVP. In production, use kotlinx.serialization.
     *
     * @param json JSON string containing configuration
     * @return PublishConfig parsed from JSON
     */
    private fun parseConfig(json: String): PublishConfig {
        val token = System.getenv("GITHUB_TOKEN")
        
        // Simple JSON parsing for MVP
        val config = mutableMapOf<String, String>()
        
        // Remove whitespace and braces
        val content = json.trim().removePrefix("{").removeSuffix("}")
        
        // Split by comma (simple approach)
        val pairs = content.split(",")
        
        for (pair in pairs) {
            val parts = pair.split(":", limit = 2)
            if (parts.size == 2) {
                val key = parts[0].trim().removeSurrounding("\"")
                val value = parts[1].trim().removeSurrounding("\"")
                config[key] = value
            }
        }
        
        val repositoryUrl = config["repositoryUrl"]
            ?: throw IllegalArgumentException("repositoryUrl is required in configuration")
        
        val branch = config["branch"] ?: "gh-pages"
        
        val baseUrl = config["baseUrl"] ?: run {
            // Auto-generate from repository URL
            val urlParts = repositoryUrl.replace(".git", "").split("/")
            if (urlParts.size >= 2) {
                val owner = urlParts[urlParts.size - 2]
                val repo = urlParts[urlParts.size - 1]
                "https://$owner.github.io/$repo"
            } else {
                throw IllegalArgumentException("Cannot auto-generate base URL from repository URL: $repositoryUrl")
            }
        }
        
        return PublishConfig(
            githubToken = token,
            repositoryUrl = repositoryUrl,
            branch = branch,
            baseUrl = baseUrl,
            authorName = config["authorName"] ?: "TCK Bot",
            authorEmail = config["authorEmail"] ?: "tck-bot@github.com",
            commitMessage = config["commitMessage"] ?: "Update TCK results"
        )
    }
}
