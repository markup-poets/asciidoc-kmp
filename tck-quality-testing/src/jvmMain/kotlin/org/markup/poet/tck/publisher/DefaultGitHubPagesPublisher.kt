package org.markup.poet.tck.publisher

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File
import java.io.IOException

/**
 * JVM implementation of GitHub Pages publisher using JGit.
 *
 * This implementation handles the complete publication workflow:
 * 1. Clone or pull the gh-pages branch
 * 2. Archive previous results with timestamps
 * 3. Write new HTML to latest.html and timestamped archive
 * 4. Generate/update index page
 * 5. Commit and push changes
 * 6. Return public URL
 *
 * ## Directory Structure
 * ```
 * gh-pages/
 * ├── index.html              # Index page with links to all results
 * ├── latest.html             # Most recent results
 * ├── results/
 * │   ├── 2026-01-24-103000.html
 * │   ├── 2026-01-23-153000.html
 * │   └── ...
 * └── .publications.json      # Metadata for all publications
 * ```
 *
 * ## Authentication
 * Supports GitHub token authentication via HTTPS. The token should be provided
 * in the PublishConfig and will be used for push operations.
 *
 * ## Example Usage
 * ```kotlin
 * val config = PublishConfig(
 *     githubToken = System.getenv("GITHUB_TOKEN"),
 *     repositoryUrl = "github.com/user/repo.git",
 *     baseUrl = "https://user.github.io/repo"
 * )
 * val publisher = DefaultGitHubPagesPublisher(config)
 * val result = publisher.publish(html, metadata).getOrThrow()
 * ```
 */
class DefaultGitHubPagesPublisher(
    private val config: PublishConfig,
    private val workingDir: String = "build/gh-pages-temp"
) : GitHubPagesPublisher {
    
    private val indexGenerator = IndexPageGenerator()
    
    override suspend fun publish(
        html: String,
        metadata: PublishMetadata
    ): Result<PublishResult> = withContext(Dispatchers.IO) {
        try {
            // Step 1: Setup repository
            val git = setupRepository()
            
            // Step 2: Load existing publications
            val publications = loadPublications()
            
            // Step 3: Write HTML files
            writeHtmlFiles(html, metadata)
            
            // Step 4: Update publications metadata
            val newPublication = createPublicationRecord(metadata)
            val updatedPublications = listOf(newPublication) + publications
            savePublications(updatedPublications)
            
            // Step 5: Generate index page
            val indexHtml = indexGenerator.generate(updatedPublications).getOrThrow()
            writeIndexPage(indexHtml)
            
            // Step 6: Commit and push
            val commitHash = commitAndPush(git, metadata)
            
            // Step 7: Return result
            val publicUrl = "${config.baseUrl}/latest.html"
            val archivedPath = "results/${metadata.runId}.html"
            
            Result.success(
                PublishResult(
                    publicUrl = publicUrl,
                    commitHash = commitHash,
                    archivedPath = archivedPath
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("Failed to publish to GitHub Pages: ${e.message}", e))
        }
    }
    
    override fun generateIndex(publications: List<PublicationRecord>): Result<String> {
        return indexGenerator.generate(publications)
    }
    
    /**
     * Setup the gh-pages repository (clone or pull).
     */
    private fun setupRepository(): Git {
        val repoDir = File(workingDir)
        
        return if (repoDir.exists() && File(repoDir, ".git").exists()) {
            // Repository exists, pull latest changes
            val git = Git.open(repoDir)
            pullLatest(git)
            git
        } else {
            // Clone the repository
            cloneRepository(repoDir)
        }
    }
    
    /**
     * Clone the gh-pages branch.
     */
    private fun cloneRepository(destDir: File): Git {
        // Clean up if directory exists but is not a git repo
        if (destDir.exists()) {
            destDir.deleteRecursively()
        }
        
        destDir.parentFile?.mkdirs()
        
        val repoUrl = if (config.githubToken != null) {
            "https://${config.githubToken}@${config.repositoryUrl}"
        } else {
            "https://${config.repositoryUrl}"
        }
        
        return try {
            Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(destDir)
                .setBranch(config.branch)
                .call()
        } catch (e: GitAPIException) {
            // Branch might not exist yet, create it
            val git = Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(destDir)
                .call()
            
            // Create and checkout gh-pages branch
            git.checkout()
                .setCreateBranch(true)
                .setName(config.branch)
                .setOrphan(true)
                .call()
            
            // Remove all files from the new branch
            destDir.listFiles()?.forEach { file ->
                if (file.name != ".git") {
                    file.deleteRecursively()
                }
            }
            
            git
        }
    }
    
    /**
     * Pull latest changes from remote.
     */
    private fun pullLatest(git: Git) {
        try {
            val credentials = if (config.githubToken != null) {
                UsernamePasswordCredentialsProvider(config.githubToken, "")
            } else {
                null
            }
            
            val pullCommand = git.pull()
            if (credentials != null) {
                pullCommand.setCredentialsProvider(credentials)
            }
            
            pullCommand.call()
        } catch (e: GitAPIException) {
            // Pull might fail if branch doesn't exist remotely yet, that's okay
            println("Warning: Pull failed (branch might not exist remotely yet): ${e.message}")
        }
    }
    
    /**
     * Load existing publications metadata.
     */
    private fun loadPublications(): List<PublicationRecord> {
        val metadataFile = File(workingDir, ".publications.json")
        
        return if (metadataFile.exists()) {
            try {
                val json = metadataFile.readText()
                parsePublications(json)
            } catch (e: Exception) {
                println("Warning: Failed to load publications metadata: ${e.message}")
                emptyList()
            }
        } else {
            emptyList()
        }
    }
    
    /**
     * Save publications metadata.
     */
    private fun savePublications(publications: List<PublicationRecord>) {
        val metadataFile = File(workingDir, ".publications.json")
        val json = serializePublications(publications)
        metadataFile.writeText(json)
    }
    
    /**
     * Write HTML files (latest.html and archived copy).
     */
    private fun writeHtmlFiles(html: String, metadata: PublishMetadata) {
        val repoDir = File(workingDir)
        
        // Write latest.html
        val latestFile = File(repoDir, "latest.html")
        latestFile.writeText(html)
        
        // Write archived copy
        val resultsDir = File(repoDir, "results")
        resultsDir.mkdirs()
        
        val archivedFile = File(resultsDir, "${metadata.runId}.html")
        archivedFile.writeText(html)
    }
    
    /**
     * Write index page.
     */
    private fun writeIndexPage(html: String) {
        val indexFile = File(workingDir, "index.html")
        indexFile.writeText(html)
    }
    
    /**
     * Commit and push changes.
     */
    private fun commitAndPush(git: Git, metadata: PublishMetadata): String {
        // Add all files
        git.add()
            .addFilepattern(".")
            .call()
        
        // Commit
        val commitMessage = "${config.commitMessage} - ${metadata.runId} (${(metadata.passRate * 100).toInt()}% pass rate)"
        val commit = git.commit()
            .setAuthor(config.authorName, config.authorEmail)
            .setMessage(commitMessage)
            .call()
        
        // Push
        val credentials = if (config.githubToken != null) {
            UsernamePasswordCredentialsProvider(config.githubToken, "")
        } else {
            null
        }
        
        val pushCommand = git.push()
        if (credentials != null) {
            pushCommand.setCredentialsProvider(credentials)
        }
        
        pushCommand.call()
        
        return commit.name
    }
    
    /**
     * Create a publication record from metadata.
     */
    private fun createPublicationRecord(metadata: PublishMetadata): PublicationRecord {
        return PublicationRecord(
            runId = metadata.runId,
            timestamp = metadata.timestamp,
            publicUrl = "${config.baseUrl}/results/${metadata.runId}.html",
            passRate = metadata.passRate,
            totalTests = metadata.totalTests,
            passedTests = metadata.passedTests,
            specVersion = metadata.specVersion,
            tckCommitHash = "", // Will be filled in by caller if needed
            libraryVersion = "", // Will be filled in by caller if needed
            platforms = emptyList() // Will be filled in by caller if needed
        )
    }
    
    /**
     * Parse publications from JSON.
     * Simple JSON parsing without external dependencies.
     */
    private fun parsePublications(json: String): List<PublicationRecord> {
        // For MVP, we'll use a simple approach
        // In production, use kotlinx.serialization
        val publications = mutableListOf<PublicationRecord>()
        
        try {
            // Very basic JSON parsing - just enough for MVP
            // This should be replaced with proper JSON parsing
            val lines = json.lines()
            var currentRecord: MutableMap<String, String>? = null
            
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed == "{") {
                    currentRecord = mutableMapOf()
                } else if (trimmed == "}" && currentRecord != null) {
                    publications.add(recordFromMap(currentRecord))
                    currentRecord = null
                } else if (currentRecord != null && trimmed.contains(":")) {
                    val parts = trimmed.split(":", limit = 2)
                    if (parts.size == 2) {
                        val key = parts[0].trim().removeSurrounding("\"")
                        val value = parts[1].trim().removeSuffix(",").removeSurrounding("\"")
                        currentRecord[key] = value
                    }
                }
            }
        } catch (e: Exception) {
            println("Warning: Failed to parse publications: ${e.message}")
        }
        
        return publications
    }
    
    /**
     * Serialize publications to JSON.
     */
    private fun serializePublications(publications: List<PublicationRecord>): String {
        // Simple JSON serialization for MVP
        // In production, use kotlinx.serialization
        return buildString {
            appendLine("[")
            publications.forEachIndexed { index, pub ->
                appendLine("  {")
                appendLine("    \"runId\": \"${pub.runId}\",")
                appendLine("    \"timestamp\": ${pub.timestamp},")
                appendLine("    \"publicUrl\": \"${pub.publicUrl}\",")
                appendLine("    \"passRate\": ${pub.passRate},")
                appendLine("    \"totalTests\": ${pub.totalTests},")
                appendLine("    \"passedTests\": ${pub.passedTests},")
                appendLine("    \"specVersion\": \"${pub.specVersion}\",")
                appendLine("    \"tckCommitHash\": \"${pub.tckCommitHash}\",")
                appendLine("    \"libraryVersion\": \"${pub.libraryVersion}\",")
                appendLine("    \"platforms\": [${pub.platforms.joinToString { "\"$it\"" }}]")
                append("  }")
                if (index < publications.size - 1) {
                    appendLine(",")
                } else {
                    appendLine()
                }
            }
            appendLine("]")
        }
    }
    
    /**
     * Create a publication record from a map.
     */
    private fun recordFromMap(map: Map<String, String>): PublicationRecord {
        return PublicationRecord(
            runId = map["runId"] ?: "",
            timestamp = map["timestamp"]?.toLongOrNull() ?: 0L,
            publicUrl = map["publicUrl"] ?: "",
            passRate = map["passRate"]?.toDoubleOrNull() ?: 0.0,
            totalTests = map["totalTests"]?.toIntOrNull() ?: 0,
            passedTests = map["passedTests"]?.toIntOrNull() ?: 0,
            specVersion = map["specVersion"] ?: "",
            tckCommitHash = map["tckCommitHash"] ?: "",
            libraryVersion = map["libraryVersion"] ?: "",
            platforms = emptyList() // Simplified for MVP
        )
    }
}
