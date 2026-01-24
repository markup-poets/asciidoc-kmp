package org.markup.poet.tck.sync

import kotlinx.serialization.Serializable
import kotlin.time.Duration

/**
 * Result of a TCK sync operation.
 * 
 * Contains metadata about the sync operation and any errors that occurred.
 */
@Serializable
data class SyncResult(
    /**
     * Whether the sync operation was successful.
     */
    val success: Boolean,
    
    /**
     * Metadata about the synced TCK.
     */
    val metadata: SyncMetadata,
    
    /**
     * Report of changes between the previous and current version.
     * Null if this is the first sync or if change detection failed.
     */
    val changeReport: ChangeReport? = null,
    
    /**
     * List of errors that occurred during sync.
     * Empty if sync was successful.
     */
    val errors: List<SyncError> = emptyList()
)

/**
 * Metadata about a TCK sync operation.
 * 
 * This information is stored in sync-metadata.json after each successful sync.
 */
@Serializable
data class SyncMetadata(
    /**
     * Timestamp when the sync occurred (milliseconds since epoch).
     */
    val timestamp: Long,
    
    /**
     * AsciiDoc specification version from the TCK.
     */
    val specVersion: String,
    
    /**
     * Git commit hash of the synced TCK.
     */
    val commitHash: String,
    
    /**
     * URL of the TCK repository.
     */
    val repositoryUrl: String,
    
    /**
     * Number of test files found in the TCK.
     */
    val testCount: Int,
    
    /**
     * Duration of the sync operation in milliseconds.
     */
    val durationMs: Long
)

/**
 * Status of TCK synchronization.
 * 
 * Used to check if the local TCK is up-to-date with the remote.
 */
@Serializable
data class SyncStatus(
    /**
     * Whether the local TCK has been synced at least once.
     */
    val isSynced: Boolean,
    
    /**
     * Whether the local TCK is outdated compared to the remote.
     */
    val isOutdated: Boolean,
    
    /**
     * Local TCK version (spec version).
     */
    val localVersion: String?,
    
    /**
     * Remote TCK version (spec version).
     * Null if unable to determine.
     */
    val remoteVersion: String?,
    
    /**
     * Timestamp of last sync (milliseconds since epoch).
     * Null if never synced.
     */
    val lastSyncTimestamp: Long?,
    
    /**
     * Human-readable status message.
     */
    val message: String
)

/**
 * Report of changes between two TCK versions.
 * 
 * Tracks which tests were added, modified, or removed.
 */
@Serializable
data class ChangeReport(
    /**
     * Test IDs that were added in the new version.
     */
    val addedTests: List<String>,
    
    /**
     * Test IDs that were modified in the new version.
     */
    val modifiedTests: List<String>,
    
    /**
     * Test IDs that were removed in the new version.
     */
    val removedTests: List<String>,
    
    /**
     * Version change information.
     * Null if version didn't change.
     */
    val versionChange: VersionChange? = null
)

/**
 * Information about a version change.
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
)

/**
 * Error that occurred during a sync operation.
 */
@Serializable
data class SyncError(
    /**
     * Type of error.
     */
    val type: SyncErrorType,
    
    /**
     * Error message describing what went wrong.
     */
    val message: String,
    
    /**
     * Steps to resolve the error.
     */
    val resolutionSteps: List<String>,
    
    /**
     * Stack trace if available.
     * Null if no exception was thrown.
     */
    val stackTrace: String? = null
)

/**
 * Types of errors that can occur during sync.
 */
@Serializable
enum class SyncErrorType {
    /**
     * Network connectivity error (unable to reach remote repository).
     */
    NETWORK_ERROR,
    
    /**
     * Git operation error (clone, pull, etc. failed).
     */
    GIT_ERROR,
    
    /**
     * Repository validation error (invalid structure, missing files).
     */
    VALIDATION_ERROR,
    
    /**
     * File system permission error.
     */
    PERMISSION_ERROR,
    
    /**
     * Unknown or unexpected error.
     */
    UNKNOWN_ERROR
}

/**
 * Result of repository validation.
 */
sealed class ValidationResult {
    /**
     * Repository is valid.
     * 
     * @param testCount Number of tests found
     */
    data class Valid(val testCount: Int) : ValidationResult()
    
    /**
     * Repository is invalid.
     * 
     * @param errors List of validation errors
     */
    data class Invalid(val errors: List<String>) : ValidationResult()
}

/**
 * Historical log entry for a sync operation.
 * 
 * Stored in sync-log.json to track sync history.
 */
@Serializable
data class SyncLogEntry(
    /**
     * Timestamp of the sync operation.
     */
    val timestamp: Long,
    
    /**
     * Spec version after sync.
     */
    val specVersion: String,
    
    /**
     * Commit hash after sync.
     */
    val commitHash: String,
    
    /**
     * Number of tests after sync.
     */
    val testCount: Int,
    
    /**
     * Whether the sync was successful.
     */
    val success: Boolean,
    
    /**
     * Changes detected during this sync.
     */
    val changes: ChangeReport? = null,
    
    /**
     * Errors that occurred during sync.
     */
    val errors: List<SyncError> = emptyList()
)

/**
 * Complete sync log containing all historical sync operations.
 */
@Serializable
data class SyncLog(
    /**
     * List of sync operations, ordered from oldest to newest.
     */
    val entries: List<SyncLogEntry>
)
