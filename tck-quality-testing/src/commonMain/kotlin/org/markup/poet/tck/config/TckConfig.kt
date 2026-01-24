package org.markup.poet.tck.config

import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Configuration for official TCK integration.
 * 
 * **Usage:**
 * ```kotlin
 * val config = TckConfig(
 *     sync = SyncConfig(autoSync = true),
 *     execution = ExecutionConfig(parallelExecution = true),
 *     reporting = ReportingConfig(generateHtml = true)
 * )
 * ```
 */
@Serializable
data class TckConfig(
    /**
     * Sync configuration.
     */
    val sync: SyncConfig = SyncConfig(),
    
    /**
     * Test execution configuration.
     */
    val execution: ExecutionConfig = ExecutionConfig(),
    
    /**
     * Reporting configuration.
     */
    val reporting: ReportingConfig = ReportingConfig()
)

/**
 * Configuration for TCK synchronization.
 */
@Serializable
data class SyncConfig(
    /**
     * Official TCK repository URL.
     */
    val repositoryUrl: String = "https://gitlab.eclipse.org/eclipse/asciidoc-lang/asciidoc-tck.git",
    
    /**
     * Branch or tag to sync.
     */
    val branch: String = "main",
    
    /**
     * Local path for cloned repository.
     */
    val localPath: String = "tck-quality-testing/official-tck/repository",
    
    /**
     * Enable automatic sync.
     */
    val autoSync: Boolean = false,
    
    /**
     * Sync frequency.
     */
    val syncFrequency: SyncFrequency = SyncFrequency.MANUAL,
    
    /**
     * Timeout for sync operations (in seconds).
     */
    val syncTimeoutSeconds: Long = 300
) {
    /**
     * Get sync timeout as Duration.
     */
    fun syncTimeout(): Duration = syncTimeoutSeconds.seconds
}

/**
 * Sync frequency options.
 */
@Serializable
enum class SyncFrequency {
    /**
     * Manual sync only (default).
     */
    MANUAL,
    
    /**
     * Sync on every build.
     */
    ON_BUILD,
    
    /**
     * Sync daily.
     */
    DAILY,
    
    /**
     * Sync weekly.
     */
    WEEKLY
}

/**
 * Configuration for test execution.
 */
@Serializable
data class ExecutionConfig(
    /**
     * Enable official TCK tests.
     */
    val enableOfficialTests: Boolean = true,
    
    /**
     * Enable custom tests.
     */
    val enableCustomTests: Boolean = true,
    
    /**
     * Enable parallel test execution.
     */
    val parallelExecution: Boolean = true,
    
    /**
     * Test timeout (in seconds).
     */
    val testTimeoutSeconds: Long = 30,
    
    /**
     * Allowed categories (empty = all).
     */
    val allowedCategories: Set<String> = emptySet(),
    
    /**
     * Excluded categories.
     */
    val excludedCategories: Set<String> = emptySet(),
    
    /**
     * Fail fast on first error.
     */
    val failFast: Boolean = false
) {
    /**
     * Get test timeout as Duration.
     */
    fun testTimeout(): Duration = testTimeoutSeconds.seconds
}

/**
 * Configuration for conformance reporting.
 */
@Serializable
data class ReportingConfig(
    /**
     * Output directory for reports.
     */
    val outputDirectory: String = "tck-quality-testing/conformance-reports",
    
    /**
     * Generate JSON reports.
     */
    val generateJson: Boolean = true,
    
    /**
     * Generate HTML reports.
     */
    val generateHtml: Boolean = true,
    
    /**
     * Generate Markdown reports.
     */
    val generateMarkdown: Boolean = true,
    
    /**
     * Include stack traces in reports.
     */
    val includeStackTraces: Boolean = true,
    
    /**
     * Include diffs in reports.
     */
    val includeDiffs: Boolean = true,
    
    /**
     * Include pending tests in reports.
     */
    val includePendingTests: Boolean = true,
    
    /**
     * Maximum diff length to include.
     */
    val maxDiffLength: Int = 1000
)
