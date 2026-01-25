package org.markup.poet.tck.publisher

/**
 * Interface for publishing HTML content to GitHub Pages.
 *
 * The publisher handles Git operations to commit and push generated HTML
 * to the gh-pages branch, maintaining historical archives and generating
 * an index page for navigation.
 *
 * ## Responsibilities
 * - Clone/pull the gh-pages branch
 * - Archive previous results with timestamps
 * - Write new HTML to latest.html and timestamped archive
 * - Generate/update index page with links to all results
 * - Commit and push changes
 * - Return public URL for viewing results
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
 * └── assets/                 # Additional assets if needed
 * ```
 *
 * ## Platform Support
 * This interface is designed to be platform-agnostic, but the initial
 * implementation will target JVM only due to Git operation requirements.
 * Future implementations may support other platforms using platform-specific
 * Git libraries or command-line tools.
 *
 * ## Example Usage
 * ```kotlin
 * val publisher = DefaultGitHubPagesPublisher(config)
 * val metadata = PublishMetadata(
 *     runId = "2026-01-24-103000",
 *     timestamp = System.currentTimeMillis(),
 *     specVersion = "1.0.0",
 *     passRate = 0.769,
 *     totalTests = 13,
 *     passedTests = 10
 * )
 * val result = publisher.publish(html, metadata).getOrThrow()
 * println("Published to: ${result.publicUrl}")
 * ```
 */
interface GitHubPagesPublisher {
    
    /**
     * Publish HTML content to GitHub Pages.
     *
     * This method performs the complete publication workflow:
     * 1. Clone or pull the gh-pages branch
     * 2. Archive the previous latest.html (if it exists)
     * 3. Write the new HTML to latest.html
     * 4. Write a timestamped copy to results/{runId}.html
     * 5. Update the index page with the new publication
     * 6. Commit and push changes
     * 7. Return the public URL
     *
     * @param html Rendered HTML content to publish
     * @param metadata Metadata for this publication
     * @return Result containing publication details, or an error
     */
    suspend fun publish(
        html: String,
        metadata: PublishMetadata
    ): Result<PublishResult>
    
    /**
     * Generate an index page linking to all historical results.
     *
     * Creates an HTML index page that lists all published results
     * in reverse chronological order (newest first), with metadata
     * such as pass rate, test counts, and links to each result.
     *
     * @param publications List of all published results
     * @return Result containing the index HTML, or an error
     */
    fun generateIndex(
        publications: List<PublicationRecord>
    ): Result<String>
}
