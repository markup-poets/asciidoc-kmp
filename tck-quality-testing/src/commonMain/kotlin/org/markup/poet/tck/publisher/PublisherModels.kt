package org.markup.poet.tck.publisher

/**
 * Metadata for exporting TCK test results to AsciiDoc format.
 *
 * @property timestamp Unix timestamp (milliseconds) when the export was generated
 * @property specVersion AsciiDoc specification version being tested (e.g., "1.0.0")
 * @property tckCommitHash Git commit hash of the TCK test suite
 * @property libraryVersion Version of the AsciiDoc Konvert library being tested
 * @property platforms List of platforms tested (e.g., ["JVM", "iOS", "Linux"])
 * @property runId Unique identifier for this test run (e.g., UUID or timestamp-based)
 */
data class ExportMetadata(
    val timestamp: Long,
    val specVersion: String,
    val tckCommitHash: String,
    val libraryVersion: String,
    val platforms: List<String>,
    val runId: String
)

/**
 * Metadata for publishing results to GitHub Pages.
 *
 * @property runId Unique identifier for this test run
 * @property timestamp Unix timestamp (milliseconds) when the publication occurred
 * @property specVersion AsciiDoc specification version being tested
 * @property passRate Overall pass rate as a decimal (0.0 to 1.0)
 * @property totalTests Total number of tests executed
 * @property passedTests Number of tests that passed
 */
data class PublishMetadata(
    val runId: String,
    val timestamp: Long,
    val specVersion: String,
    val passRate: Double,
    val totalTests: Int,
    val passedTests: Int
)

/**
 * Result of publishing to GitHub Pages.
 *
 * @property publicUrl Public URL where the results can be viewed
 * @property commitHash Git commit hash of the publication commit
 * @property archivedPath Path to the archived result file in the repository
 */
data class PublishResult(
    val publicUrl: String,
    val commitHash: String,
    val archivedPath: String
)

/**
 * Record of a historical publication.
 *
 * @property runId Unique identifier for this test run
 * @property timestamp Unix timestamp (milliseconds) when the publication occurred
 * @property publicUrl Public URL where the results can be viewed
 * @property passRate Overall pass rate as a decimal (0.0 to 1.0)
 * @property totalTests Total number of tests executed
 * @property passedTests Number of tests that passed
 * @property specVersion AsciiDoc specification version being tested
 * @property tckCommitHash Git commit hash of the TCK test suite
 * @property libraryVersion Version of the AsciiDoc Konvert library being tested
 * @property platforms List of platforms tested
 */
data class PublicationRecord(
    val runId: String,
    val timestamp: Long,
    val publicUrl: String,
    val passRate: Double,
    val totalTests: Int,
    val passedTests: Int,
    val specVersion: String,
    val tckCommitHash: String,
    val libraryVersion: String,
    val platforms: List<String>
)

/**
 * Result of executing the complete publishing workflow.
 *
 * @property asciidocGenerated Whether the AsciiDoc document was successfully generated
 * @property parseSucceeded Whether parsing the AsciiDoc document succeeded
 * @property renderSucceeded Whether rendering to HTML succeeded
 * @property publishSucceeded Whether publishing to GitHub Pages succeeded
 * @property publicUrl Public URL where results can be viewed (null if publishing failed)
 * @property errors List of error messages encountered during the workflow
 * @property durationMs Total execution time in milliseconds
 */
data class WorkflowResult(
    val asciidocGenerated: Boolean,
    val parseSucceeded: Boolean,
    val renderSucceeded: Boolean,
    val publishSucceeded: Boolean,
    val publicUrl: String?,
    val errors: List<String>,
    val durationMs: Long
)

/**
 * Configuration for publishing results to GitHub Pages.
 *
 * @property githubToken GitHub personal access token for authentication (optional, can use SSH)
 * @property repositoryUrl Git repository URL (e.g., "github.com/user/repo.git")
 * @property branch Target branch for GitHub Pages (default: "gh-pages")
 * @property baseUrl Base URL for GitHub Pages (e.g., "https://user.github.io/repo")
 * @property authorName Git commit author name (default: "TCK Bot")
 * @property authorEmail Git commit author email (default: "tck-bot@example.com")
 * @property commitMessage Git commit message template (default: "Update TCK results")
 */
data class PublishConfig(
    val githubToken: String? = null,
    val repositoryUrl: String,
    val branch: String = "gh-pages",
    val baseUrl: String,
    val authorName: String = "TCK Bot",
    val authorEmail: String = "tck-bot@example.com",
    val commitMessage: String = "Update TCK results"
)
