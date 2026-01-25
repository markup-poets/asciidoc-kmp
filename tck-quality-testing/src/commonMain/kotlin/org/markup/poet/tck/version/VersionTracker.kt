package org.markup.poet.tck.version

import org.markup.poet.tck.platformReadFile
import org.markup.poet.tck.platformWriteFile
import org.markup.poet.tck.platformFileExists
import org.markup.poet.tck.platformDeleteFile
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Tracks official TCK version and maintains version history.
 * 
 * The VersionTracker stores version information in:
 * - `version.txt`: Current spec version
 * - `commit-hash.txt`: Current commit hash
 * - `version-history.json`: Complete version history
 * 
 * **Usage:**
 * ```kotlin
 * val tracker = DefaultVersionTracker(fileOperations)
 * 
 * // Get current version
 * val current = tracker.getCurrentVersion()
 * 
 * // Update after sync
 * tracker.updateVersion(newVersion)
 * 
 * // Get history
 * val history = tracker.getVersionHistory()
 * ```
 */
interface VersionTracker {
    /**
     * Get current local TCK version.
     * 
     * @return Current version, or null if not yet synced
     */
    fun getCurrentVersion(): TckVersion?
    
    /**
     * Update version information after sync.
     * 
     * @param version New version to record
     */
    fun updateVersion(version: TckVersion)
    
    /**
     * Get version history (most recent first).
     * 
     * @return List of all recorded versions
     */
    fun getVersionHistory(): List<TckVersion>
    
    /**
     * Clear version history.
     */
    fun clearHistory()
}

/**
 * Default implementation of VersionTracker.
 * 
 * Stores version information in files:
 * - `{basePath}/version.txt`: Current spec version
 * - `{basePath}/commit-hash.txt`: Current commit hash
 * - `{basePath}/version-history.json`: Complete history
 */
class DefaultVersionTracker(
    private val fileOperations: VersionFileOperations,
    private val basePath: String = "tck-quality-testing/official-tck"
) : VersionTracker {
    
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }
    
    override fun getCurrentVersion(): TckVersion? {
        return try {
            val versionText = fileOperations.readFile("$basePath/version.txt") ?: return null
            val commitHash = fileOperations.readFile("$basePath/commit-hash.txt") ?: return null
            
            // Try to get full version from history
            val history = getVersionHistory()
            history.firstOrNull { it.commitHash == commitHash.trim() }
                ?: TckVersion(
                    specVersion = versionText.trim(),
                    commitHash = commitHash.trim(),
                    timestamp = 0L,
                    testCount = 0
                )
        } catch (e: Exception) {
            null
        }
    }
    
    override fun updateVersion(version: TckVersion) {
        // Write version.txt
        fileOperations.writeFile("$basePath/version.txt", version.specVersion)
        
        // Write commit-hash.txt
        fileOperations.writeFile("$basePath/commit-hash.txt", version.commitHash)
        
        // Update history
        val history = getVersionHistory().toMutableList()
        
        // Remove any existing entry with same commit hash
        history.removeAll { it.commitHash == version.commitHash }
        
        // Add new version at the beginning
        history.add(0, version)
        
        // Keep only last 50 versions
        val trimmedHistory = history.take(50)
        
        // Write history
        val historyJson = json.encodeToString(trimmedHistory)
        fileOperations.writeFile("$basePath/version-history.json", historyJson)
    }
    
    override fun getVersionHistory(): List<TckVersion> {
        return try {
            val historyJson = fileOperations.readFile("$basePath/version-history.json") ?: return emptyList()
            json.decodeFromString<List<TckVersion>>(historyJson)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    override fun clearHistory() {
        fileOperations.deleteFile("$basePath/version.txt")
        fileOperations.deleteFile("$basePath/commit-hash.txt")
        fileOperations.deleteFile("$basePath/version-history.json")
    }
}

/**
 * File operations for version tracking.
 * 
 * Platform-specific implementation for reading/writing version files.
 */
interface VersionFileOperations {
    /**
     * Read a file's content.
     * 
     * @param path File path
     * @return File content, or null if file doesn't exist
     */
    fun readFile(path: String): String?
    
    /**
     * Write content to a file.
     * 
     * @param path File path
     * @param content Content to write
     */
    fun writeFile(path: String, content: String)
    
    /**
     * Delete a file.
     * 
     * @param path File path
     */
    fun deleteFile(path: String)
    
    /**
     * Check if a file exists.
     * 
     * @param path File path
     * @return true if file exists
     */
    fun fileExists(path: String): Boolean
}
