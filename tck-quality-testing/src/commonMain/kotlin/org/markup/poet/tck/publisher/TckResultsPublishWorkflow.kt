package org.markup.poet.tck.publisher

import org.markup.poet.tck.execution.AggregatedResults

/**
 * Interface for orchestrating the complete TCK results publishing workflow.
 *
 * The workflow coordinates all stages of the publishing pipeline:
 * 1. Export test results to AsciiDoc
 * 2. Parse AsciiDoc using our own parser (dogfooding!)
 * 3. Render AST to HTML with Kotlin theme
 * 4. Publish HTML to GitHub Pages
 *
 * Each stage validates the previous stage's output, creating a self-validating
 * system. If any stage fails, the workflow stops immediately and returns an error.
 *
 * ## Dogfooding Principle
 * This workflow is a critical dogfooding component - we use our own AsciiDoc
 * parser and HTML renderer to publish our test results. If the parser cannot
 * parse our own output, it indicates a critical bug that must be fixed.
 *
 * ## Error Handling
 * - Export errors: Stop workflow, save results data for debugging
 * - Parse errors: CRITICAL - treat as bug in exporter or parser
 * - Render errors: Stop workflow, save AST for debugging
 * - Publish errors: Retry with backoff, save HTML locally as fallback
 *
 * ## Progress Logging
 * The workflow logs progress at each stage to provide visibility into
 * execution and help with debugging. Each stage logs before execution:
 * - "Exporting test results to AsciiDoc..."
 * - "Parsing AsciiDoc document..."
 * - "Rendering HTML with Kotlin theme..."
 * - "Publishing to GitHub Pages..."
 *
 * ## Example Usage
 * ```kotlin
 * val workflow = DefaultTckResultsPublishWorkflow(
 *     exporter = DefaultTckResultsExporter(),
 *     parser = DefaultAsciidocParser(),
 *     renderer = DefaultHtmlRenderer(),
 *     publisher = DefaultGitHubPagesPublisher(config),
 *     config = publishConfig
 * )
 *
 * val results = TckIntegration.runTests(context)
 * val workflowResult = workflow.execute(results).getOrThrow()
 *
 * println("Published to: ${workflowResult.publicUrl}")
 * println("Duration: ${workflowResult.durationMs}ms")
 * ```
 */
interface TckResultsPublishWorkflow {
    
    /**
     * Execute the complete publishing workflow.
     *
     * Runs all stages of the pipeline in sequence:
     * 1. Export results to AsciiDoc
     * 2. Parse AsciiDoc to AST
     * 3. Render AST to HTML
     * 4. Publish HTML to GitHub Pages
     *
     * The workflow tracks execution time and logs progress at each stage.
     * If any stage fails, the workflow stops immediately and returns a
     * failure result with error details.
     *
     * @param results TCK test results to publish
     * @return Result containing workflow execution details, or an error
     */
    suspend fun execute(
        results: AggregatedResults
    ): Result<WorkflowResult>
}
