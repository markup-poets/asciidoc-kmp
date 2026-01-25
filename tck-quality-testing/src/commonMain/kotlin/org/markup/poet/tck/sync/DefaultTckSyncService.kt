package org.markup.poet.tck.sync

import org.markup.poet.tck.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Default implementation of TckSyncService.
 * 
 * This service orchestrates the complete sync workflow:
 * 1. Clone or pull the official TCK repository
 * 2. Validate the repository structure
 * 3. Store sync metadata
 * 4. Update sync log
 * 5. Detect changes from previous version
 * 
 * **Configuration:**
 * ```kotlin
 * val syncService = DefaultTckSyncService(
 *     repositoryUrl = "https://gitlab.eclipse.org/eclipse/asciidoc-lang/asciidoc-tck.git",
 *     localPath = "tck-quality-testing/official-tck/repository",
 *     metadataPath = "tck-quality-testing/official-tck/sync-metadata.json",
 *     logPath = "tck-quality-testing/official-tck/sync-log.json"
 * )
 * ```
 */
class DefaultTckSyncService(
    private val repositoryUrl: String,
    private val localPath: String,
    private val metadataPath: String = "$localPath/../sync-metadata.json",
    private val logPath: String = "$localPath/../sync-log.json",
    private val gitOperations: GitOperations = PlatformGitOperations(),
    private val validator: SyncValidator = DefaultSyncValidator()
) : TckSyncService {
    
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    override suspend fun sync(force: Boolean): SyncResult {
        val startTime = currentTimeMillis()
        val errors = mutableListOf<SyncError>()
        
        try {
            // Step 1: Clone or pull repository
            val gitResult = if (force || !gitOperations.isValidRepository(localPath)) {
                // Delete existing directory if force is true
                if (force && platformFileExists(localPath)) {
                    deleteDirectory(localPath)
                }
                
                // Clone repository
                gitOperations.clone(repositoryUrl, localPath)
            } else {
                // Pull latest changes
                gitOperations.pull(localPath)
            }
            
            // Check if git operation succeeded
            when (gitResult) {
                is GitResult.Failure -> {
                    errors.add(
                        SyncError(
                            type = SyncErrorType.GIT_ERROR,
                            message = gitResult.error,
                            resolutionSteps = listOf(
                                "Check network connectivity",
                                "Verify repository URL is correct",
                                "Ensure git is installed and accessible",
                                "Try running with force=true to perform fresh clone"
                            ),
                            stackTrace = gitResult.exceptionMessage
                        )
                    )
                    
                    // Try to use existing data if available
                    val lastMetadata = getLastSyncMetadata()
                    return SyncResult(
                        success = false,
                        metadata = lastMetadata ?: createEmptyMetadata(startTime),
                        errors = errors
                    )
                }
                is GitResult.Success -> {
                    // Continue with validation
                }
            }
            
            // Step 2: Validate repository structure
            val validationResult = validator.validateStructure(localPath)
            when (validationResult) {
                is ValidationResult.Invalid -> {
                    errors.add(
                        SyncError(
                            type = SyncErrorType.VALIDATION_ERROR,
                            message = "Repository validation failed",
                            resolutionSteps = listOf(
                                "Check repository structure",
                                "Ensure tests/ directory exists",
                                "Try running with force=true to perform fresh clone"
                            ) + validationResult.errors
                        )
                    )
                    
                    val lastMetadata = getLastSyncMetadata()
                    return SyncResult(
                        success = false,
                        metadata = lastMetadata ?: createEmptyMetadata(startTime),
                        errors = errors
                    )
                }
                is ValidationResult.Valid -> {
                    // Continue with metadata creation
                    val endTime = currentTimeMillis()
                    val duration = endTime - startTime
                    
                    // Get git information
                    val commitHash = gitOperations.getCurrentCommitHash(localPath) ?: "unknown"
                    val specVersion = extractSpecVersion(localPath)
                    
                    // Create metadata
                    val metadata = SyncMetadata(
                        timestamp = endTime,
                        specVersion = specVersion,
                        commitHash = commitHash,
                        repositoryUrl = repositoryUrl,
                        testCount = validationResult.testCount,
                        durationMs = duration
                    )
                    
                    // Step 3: Detect changes from previous version
                    val changeReport = detectChanges(metadata)
                    
                    // Step 4: Store metadata
                    storeMetadata(metadata)
                    
                    // Step 5: Update sync log
                    updateSyncLog(metadata, changeReport, errors)
                    
                    return SyncResult(
                        success = true,
                        metadata = metadata,
                        changeReport = changeReport,
                        errors = errors
                    )
                }
            }
        } catch (e: Exception) {
            errors.add(
                SyncError(
                    type = SyncErrorType.UNKNOWN_ERROR,
                    message = "Unexpected error during sync: ${e.message}",
                    resolutionSteps = listOf(
                        "Check logs for details",
                        "Try running with force=true",
                        "Report issue if problem persists"
                    ),
                    stackTrace = e.stackTraceToString()
                )
            )
            
            val lastMetadata = getLastSyncMetadata()
            return SyncResult(
                success = false,
                metadata = lastMetadata ?: createEmptyMetadata(startTime),
                errors = errors
            )
        }
    }
    
    override suspend fun checkSyncStatus(): SyncStatus {
        // Check if local repository exists
        if (!gitOperations.isValidRepository(localPath)) {
            return SyncStatus(
                isSynced = false,
                isOutdated = true,
                localVersion = null,
                remoteVersion = null,
                lastSyncTimestamp = null,
                message = "Local TCK repository not found. Run sync() to clone."
            )
        }
        
        // Get local metadata
        val localMetadata = getLastSyncMetadata()
        if (localMetadata == null) {
            return SyncStatus(
                isSynced = false,
                isOutdated = true,
                localVersion = null,
                remoteVersion = null,
                lastSyncTimestamp = null,
                message = "No sync metadata found. Run sync() to initialize."
            )
        }
        
        // Get current commit hash
        val currentHash = gitOperations.getCurrentCommitHash(localPath)
        
        // For now, we consider it synced if we have metadata
        // A more sophisticated check would fetch remote HEAD
        return SyncStatus(
            isSynced = true,
            isOutdated = false,
            localVersion = localMetadata.specVersion,
            remoteVersion = null, // Would need to fetch from remote
            lastSyncTimestamp = localMetadata.timestamp,
            message = "Local TCK is synced (${localMetadata.testCount} tests, version ${localMetadata.specVersion})"
        )
    }
    
    override fun validateRepository(): ValidationResult {
        return validator.validateStructure(localPath)
    }
    
    override fun getLastSyncMetadata(): SyncMetadata? {
        return try {
            if (!platformFileExists(metadataPath)) {
                return null
            }
            val content = platformReadFile(metadataPath)
            json.decodeFromString<SyncMetadata>(content)
        } catch (e: Exception) {
            null
        }
    }
    
    override fun getSyncLog(): SyncLog {
        return try {
            if (!platformFileExists(logPath)) {
                return SyncLog(emptyList())
            }
            val content = platformReadFile(logPath)
            json.decodeFromString<SyncLog>(content)
        } catch (e: Exception) {
            SyncLog(emptyList())
        }
    }
    
    /**
     * Store sync metadata to file.
     */
    private fun storeMetadata(metadata: SyncMetadata) {
        try {
            val content = json.encodeToString(metadata)
            platformWriteFile(metadataPath, content)
        } catch (e: Exception) {
            // Log error but don't fail sync
            println("Warning: Failed to store sync metadata: ${e.message}")
        }
    }
    
    /**
     * Update the sync log with a new entry.
     */
    private fun updateSyncLog(
        metadata: SyncMetadata,
        changeReport: ChangeReport?,
        errors: List<SyncError>
    ) {
        try {
            val log = getSyncLog()
            val newEntry = SyncLogEntry(
                timestamp = metadata.timestamp,
                specVersion = metadata.specVersion,
                commitHash = metadata.commitHash,
                testCount = metadata.testCount,
                success = errors.isEmpty(),
                changes = changeReport,
                errors = errors
            )
            
            val updatedLog = SyncLog(log.entries + newEntry)
            val content = json.encodeToString(updatedLog)
            platformWriteFile(logPath, content)
        } catch (e: Exception) {
            // Log error but don't fail sync
            println("Warning: Failed to update sync log: ${e.message}")
        }
    }
    
    /**
     * Detect changes from the previous version.
     */
    private fun detectChanges(newMetadata: SyncMetadata): ChangeReport? {
        val oldMetadata = getLastSyncMetadata() ?: return null
        
        // If commit hash is the same, no changes
        if (oldMetadata.commitHash == newMetadata.commitHash) {
            return ChangeReport(
                addedTests = emptyList(),
                modifiedTests = emptyList(),
                removedTests = emptyList(),
                versionChange = null
            )
        }
        
        // For now, we just report test count changes
        // A more sophisticated implementation would compare actual test files
        val versionChange = if (oldMetadata.specVersion != newMetadata.specVersion) {
            VersionChange(from = oldMetadata.specVersion, to = newMetadata.specVersion)
        } else {
            null
        }
        
        return ChangeReport(
            addedTests = emptyList(), // TODO: Implement detailed change detection
            modifiedTests = emptyList(),
            removedTests = emptyList(),
            versionChange = versionChange
        )
    }
    
    /**
     * Extract spec version from repository.
     * This is a placeholder - actual implementation would read from a version file.
     */
    private fun extractSpecVersion(repositoryPath: String): String {
        // Try to read version from package.json or similar
        // For now, return a default
        return "1.0.0"
    }
    
    /**
     * Create empty metadata for error cases.
     */
    private fun createEmptyMetadata(timestamp: Long): SyncMetadata {
        return SyncMetadata(
            timestamp = timestamp,
            specVersion = "unknown",
            commitHash = "unknown",
            repositoryUrl = repositoryUrl,
            testCount = 0,
            durationMs = 0
        )
    }
    
    /**
     * Delete a directory recursively.
     */
    private fun deleteDirectory(path: String) {
        platformDeleteDirectory(path)
    }
}
