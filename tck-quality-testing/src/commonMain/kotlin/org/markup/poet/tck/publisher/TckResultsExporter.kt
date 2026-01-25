package org.markup.poet.tck.publisher

import org.markup.poet.tck.execution.AggregatedResults

/**
 * Interface for exporting TCK test results to AsciiDoc format.
 *
 * The exporter converts structured test execution results into a human-readable
 * AsciiDoc document that can be parsed by our own parser and rendered to HTML.
 * This is a critical component of the dogfooding workflow.
 *
 * ## Responsibilities
 * - Generate well-formed AsciiDoc document structure (title, sections, tables)
 * - Format test results with appropriate status indicators (✅ ❌ 💥 ⏭️)
 * - Include summary statistics and metadata
 * - Organize tests by category
 * - Include detailed error information for failed tests
 *
 * ## Output Format
 * The generated AsciiDoc document should include:
 * - Document title and metadata
 * - Summary section with overall statistics
 * - Test results organized by category
 * - Failed tests section with error details
 * - Metadata section with version information
 *
 * ## Example Usage
 * ```kotlin
 * val exporter = DefaultTckResultsExporter()
 * val metadata = ExportMetadata(
 *     timestamp = System.currentTimeMillis(),
 *     specVersion = "1.0.0",
 *     tckCommitHash = "abc123",
 *     libraryVersion = "1.0.0",
 *     platforms = listOf("JVM", "iOS"),
 *     runId = UUID.randomUUID().toString()
 * )
 * val asciidoc = exporter.export(results, metadata).getOrThrow()
 * ```
 */
interface TckResultsExporter {
    
    /**
     * Export test results to AsciiDoc format.
     *
     * Generates a complete AsciiDoc document containing all test results,
     * summary statistics, and metadata. The generated document should be
     * parseable by our own AsciiDoc parser.
     *
     * @param results Aggregated test results from TCK execution
     * @param metadata Additional metadata (timestamp, versions, etc.)
     * @return Result containing the AsciiDoc document as a string, or an error
     */
    fun export(
        results: AggregatedResults,
        metadata: ExportMetadata
    ): Result<String>
}
