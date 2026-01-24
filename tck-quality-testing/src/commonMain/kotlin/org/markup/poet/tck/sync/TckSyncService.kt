package org.markup.poet.tck.sync

/**
 * Service for synchronizing with the official Eclipse AsciiDoc TCK repository.
 * 
 * This service handles:
 * - Cloning the official TCK repository
 * - Pulling updates from the remote
 * - Validating the repository structure
 * - Tracking sync metadata and history
 * - Detecting changes between versions
 * 
 * **Usage:**
 * ```kotlin
 * val syncService = DefaultTckSyncService(
 *     repositoryUrl = "https://gitlab.eclipse.org/eclipse/asciidoc-lang/asciidoc-tck.git",
 *     localPath = "tck-quality-testing/official-tck/repository"
 * )
 * 
 * // Perform sync
 * val result = syncService.sync()
 * if (result.success) {
 *     println("Synced ${result.metadata.testCount} tests")
 * }
 * 
 * // Check status
 * val status = syncService.checkSyncStatus()
 * if (status.isOutdated) {
 *     println("TCK is outdated, please sync")
 * }
 * ```
 */
interface TckSyncService {
    /**
     * Synchronize with the official TCK repository.
     * 
     * This operation will:
     * 1. Clone the repository if it doesn't exist locally
     * 2. Pull latest changes if it already exists
     * 3. Validate the repository structure
     * 4. Store sync metadata
     * 5. Update the sync log
     * 6. Detect changes from the previous version
     * 
     * @param force If true, delete existing repository and perform fresh clone
     * @return SyncResult with metadata and any errors
     */
    suspend fun sync(force: Boolean = false): SyncResult
    
    /**
     * Check if the local TCK is up-to-date with the remote.
     * 
     * This operation will:
     * 1. Check if local repository exists
     * 2. Compare local commit hash with remote
     * 3. Determine if sync is needed
     * 
     * @return SyncStatus indicating current state
     */
    suspend fun checkSyncStatus(): SyncStatus
    
    /**
     * Validate the integrity of the local TCK repository.
     * 
     * This operation checks:
     * - Repository structure (tests/ directory exists)
     * - Test files are present and valid
     * - Required metadata files exist
     * 
     * @return ValidationResult indicating if repository is valid
     */
    fun validateRepository(): ValidationResult
    
    /**
     * Get the metadata from the last successful sync.
     * 
     * @return SyncMetadata, or null if never synced
     */
    fun getLastSyncMetadata(): SyncMetadata?
    
    /**
     * Get the complete sync history.
     * 
     * @return SyncLog with all historical sync operations
     */
    fun getSyncLog(): SyncLog
}
