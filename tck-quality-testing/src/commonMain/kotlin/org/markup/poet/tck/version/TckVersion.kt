package org.markup.poet.tck.version

import kotlinx.serialization.Serializable

/**
 * Represents a specific version of the official AsciiDoc TCK.
 * 
 * Tracks the TCK version, commit hash, timestamp, and test count
 * to enable version comparison and change detection.
 * 
 * **Usage:**
 * ```kotlin
 * val version = TckVersion(
 *     specVersion = "1.0.0",
 *     commitHash = "abc123def456",
 *     timestamp = System.currentTimeMillis(),
 *     testCount = 150
 * )
 * ```
 */
@Serializable
data class TckVersion(
    /**
     * AsciiDoc specification version (e.g., "1.0.0", "1.5.0").
     */
    val specVersion: String,
    
    /**
     * Git commit hash of the TCK repository.
     */
    val commitHash: String,
    
    /**
     * Timestamp when this version was synced (milliseconds since epoch).
     */
    val timestamp: Long,
    
    /**
     * Number of tests in this version.
     */
    val testCount: Int,
    
    /**
     * Optional branch or tag name.
     */
    val ref: String? = null,
    
    /**
     * Optional notes about this version.
     */
    val notes: String? = null
) {
    /**
     * Get a short commit hash (first 8 characters).
     */
    fun shortCommitHash(): String = commitHash.take(8)
    
    /**
     * Check if this version is the same as another (by commit hash).
     */
    fun isSameAs(other: TckVersion): Boolean {
        return commitHash == other.commitHash
    }
    
    /**
     * Check if this version is newer than another (by timestamp).
     */
    fun isNewerThan(other: TckVersion): Boolean {
        return timestamp > other.timestamp
    }
    
    /**
     * Get a human-readable summary.
     */
    fun summary(): String {
        return buildString {
            append("TCK v$specVersion")
            append(" (${shortCommitHash()})")
            append(" - $testCount tests")
            if (ref != null) {
                append(" [$ref]")
            }
        }
    }
}

/**
 * Report of changes between two TCK versions.
 */
@Serializable
data class ChangeReport(
    /**
     * Old version (before changes).
     */
    val oldVersion: TckVersion,
    
    /**
     * New version (after changes).
     */
    val newVersion: TckVersion,
    
    /**
     * Tests that were added in the new version.
     */
    val addedTests: List<String>,
    
    /**
     * Tests that were modified in the new version.
     */
    val modifiedTests: List<String>,
    
    /**
     * Tests that were removed in the new version.
     */
    val removedTests: List<String>,
    
    /**
     * Spec version change (if any).
     */
    val versionChange: VersionChange? = null
) {
    /**
     * Total number of changes.
     */
    fun totalChanges(): Int = addedTests.size + modifiedTests.size + removedTests.size
    
    /**
     * Check if there are any changes.
     */
    fun hasChanges(): Boolean = totalChanges() > 0
    
    /**
     * Get a summary of changes.
     */
    fun summary(): String {
        return buildString {
            appendLine("Changes from ${oldVersion.shortCommitHash()} to ${newVersion.shortCommitHash()}:")
            appendLine("  Added: ${addedTests.size} tests")
            appendLine("  Modified: ${modifiedTests.size} tests")
            appendLine("  Removed: ${removedTests.size} tests")
            if (versionChange != null) {
                appendLine("  Version: ${versionChange.from} → ${versionChange.to}")
            }
        }
    }
}

/**
 * Represents a change in spec version.
 */
@Serializable
data class VersionChange(
    /**
     * Previous version.
     */
    val from: String,
    
    /**
     * New version.
     */
    val to: String
) {
    /**
     * Check if this is a major version change.
     */
    fun isMajorChange(): Boolean {
        val fromMajor = from.split(".").firstOrNull()?.toIntOrNull() ?: 0
        val toMajor = to.split(".").firstOrNull()?.toIntOrNull() ?: 0
        return toMajor > fromMajor
    }
    
    /**
     * Check if this is a minor version change.
     */
    fun isMinorChange(): Boolean {
        val fromParts = from.split(".")
        val toParts = to.split(".")
        
        if (fromParts.size < 2 || toParts.size < 2) return false
        
        val fromMajor = fromParts[0].toIntOrNull() ?: 0
        val toMajor = toParts[0].toIntOrNull() ?: 0
        val fromMinor = fromParts[1].toIntOrNull() ?: 0
        val toMinor = toParts[1].toIntOrNull() ?: 0
        
        return fromMajor == toMajor && toMinor > fromMinor
    }
}
