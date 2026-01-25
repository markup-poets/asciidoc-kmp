package org.markup.poet.tck.publisher

import org.markup.poet.asciidoc.parser.AsciidocParser
import org.markup.poet.tck.execution.AggregatedResults
import kotlin.time.measureTimedValue
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Default implementation of [TckResultsPublishWorkflow] that orchestrates the complete
 * TCK results publishing pipeline.
 *
 * This workflow coordinates all stages:
 * 1. Export test results to AsciiDoc
 * 2. Parse AsciiDoc using our own parser (dogfooding!)
 * 3. Render AST to HTML with Kotlin theme
 * 4. Publish HTML to GitHub Pages
 *
 * ## Critical Dogfooding Component
 * This workflow is a critical dogfooding component - we use our own AsciiDoc parser
 * to parse the results we generate. If the parser cannot parse our own output, it
 * indicates a **CRITICAL BUG** that must be fixed immediately.
 *
 * ## Parse Error Handling
 * Parse errors are treated with the highest severity:
 * - All parse errors are logged with line/column information
 * - The generated AsciiDoc content is preserved for debugging
 * - The workflow stops immediately and returns a failure
 * - Parse failures should trigger alerts and be treated as critical bugs
 *
 * ## Progress Logging
 * The workflow logs progress at each stage to provide visibility into execution
 * and help with debugging.
 *
 * ## Validation
 * The workflow validates output at each stage to ensure dogfooding integrity:
 * - AsciiDoc validation: Ensures exported document has expected structure
 * - AST validation: Ensures parsed document has reasonable structure
 * - HTML validation: Ensures rendered HTML is valid HTML5 with expected content
 * - Publication validation: Ensures published results are accessible
 *
 * @property exporter Component that exports test results to AsciiDoc format
 * @property parser Our AsciiDoc parser (dogfooding!)
 * @property renderer HTML renderer wrapper for TCK results
 * @property publisher GitHub Pages publisher (optional - if null, skips publishing)
 * @property config Publishing configuration (optional - required if publisher is provided)
 * @property validator Validator for workflow stage outputs
 */
class DefaultTckResultsPublishWorkflow(
    private val exporter: TckResultsExporter,
    private val parser: AsciidocParser,
    private val renderer: TckHtmlRenderer = TckHtmlRenderer(),
    private val publisher: GitHubPagesPublisher? = null,
    private val config: PublishConfig? = null,
    private val validator: WorkflowValidator = DefaultWorkflowValidator()
) : TckResultsPublishWorkflow {
    
    override suspend fun execute(
        results: AggregatedResults
    ): Result<WorkflowResult> {
        val errors = mutableListOf<String>()
        
        // Measure total execution time
        val (workflowResult, duration) = measureTimedValue {
            executeWorkflow(results, errors)
        }
        
        return workflowResult.map { result ->
            result.copy(durationMs = duration.inWholeMilliseconds)
        }
    }
    
    /**
     * Execute the workflow stages and track progress.
     */
    private suspend fun executeWorkflow(
        results: AggregatedResults,
        errors: MutableList<String>
    ): Result<WorkflowResult> {
        // Stage 1: Export to AsciiDoc
        println("Exporting test results to AsciiDoc...")
        val exportResult = exportToAsciidoc(results, errors)
        if (exportResult.isFailure) {
            return Result.success(
                WorkflowResult(
                    asciidocGenerated = false,
                    parseSucceeded = false,
                    renderSucceeded = false,
                    publishSucceeded = false,
                    publicUrl = null,
                    errors = errors,
                    durationMs = 0 // Will be set by caller
                )
            )
        }
        
        val asciidoc = exportResult.getOrThrow()
        
        // Validate exported AsciiDoc
        println("Validating exported AsciiDoc...")
        val asciidocValidation = validator.validateAsciidoc(asciidoc)
        if (asciidocValidation.isInvalid()) {
            val validationErrors = asciidocValidation.getErrors()
            val errorMessage = buildString {
                appendLine("AsciiDoc validation failed:")
                validationErrors.forEach { error ->
                    appendLine("  - $error")
                }
            }
            errors.add(errorMessage)
            println("ERROR: $errorMessage")
            
            return Result.success(
                WorkflowResult(
                    asciidocGenerated = true,
                    parseSucceeded = false,
                    renderSucceeded = false,
                    publishSucceeded = false,
                    publicUrl = null,
                    errors = errors,
                    durationMs = 0 // Will be set by caller
                )
            )
        }
        println("✅ AsciiDoc validation passed")
        
        // Stage 2: Parse AsciiDoc (CRITICAL - dogfooding!)
        println("Parsing AsciiDoc document...")
        val parseResult = parseAsciidoc(asciidoc, errors)
        if (parseResult.isFailure) {
            return Result.success(
                WorkflowResult(
                    asciidocGenerated = true,
                    parseSucceeded = false,
                    renderSucceeded = false,
                    publishSucceeded = false,
                    publicUrl = null,
                    errors = errors,
                    durationMs = 0 // Will be set by caller
                )
            )
        }
        
        val document = parseResult.getOrThrow().document
        
        // Validate parsed AST
        println("Validating parsed AST...")
        val astValidation = validator.validateAst(document)
        if (astValidation.isInvalid()) {
            val validationErrors = astValidation.getErrors()
            val errorMessage = buildString {
                appendLine("AST validation failed:")
                validationErrors.forEach { error ->
                    appendLine("  - $error")
                }
            }
            errors.add(errorMessage)
            println("ERROR: $errorMessage")
            
            return Result.success(
                WorkflowResult(
                    asciidocGenerated = true,
                    parseSucceeded = true,
                    renderSucceeded = false,
                    publishSucceeded = false,
                    publicUrl = null,
                    errors = errors,
                    durationMs = 0 // Will be set by caller
                )
            )
        }
        println("✅ AST validation passed")
        
        // Stage 3: Render to HTML
        println("Rendering HTML with Kotlin theme...")
        val renderResult = renderToHtml(document, errors)
        if (renderResult.isFailure) {
            return Result.success(
                WorkflowResult(
                    asciidocGenerated = true,
                    parseSucceeded = true,
                    renderSucceeded = false,
                    publishSucceeded = false,
                    publicUrl = null,
                    errors = errors,
                    durationMs = 0 // Will be set by caller
                )
            )
        }
        
        val html = renderResult.getOrThrow()
        
        // Validate rendered HTML
        println("Validating rendered HTML...")
        val htmlValidation = validator.validateHtml(html)
        if (htmlValidation.isInvalid()) {
            val validationErrors = htmlValidation.getErrors()
            val errorMessage = buildString {
                appendLine("HTML validation failed:")
                validationErrors.forEach { error ->
                    appendLine("  - $error")
                }
            }
            errors.add(errorMessage)
            println("ERROR: $errorMessage")
            
            return Result.success(
                WorkflowResult(
                    asciidocGenerated = true,
                    parseSucceeded = true,
                    renderSucceeded = true,
                    publishSucceeded = false,
                    publicUrl = null,
                    errors = errors,
                    durationMs = 0 // Will be set by caller
                )
            )
        }
        println("✅ HTML validation passed")
        
        // Stage 4: Publish to GitHub Pages
        if (publisher != null && config != null) {
            println("Publishing to GitHub Pages...")
            val publishResult = publishToGitHub(html, results, errors)
            if (publishResult.isFailure) {
                return Result.success(
                    WorkflowResult(
                        asciidocGenerated = true,
                        parseSucceeded = true,
                        renderSucceeded = true,
                        publishSucceeded = false,
                        publicUrl = null,
                        errors = errors,
                        durationMs = 0 // Will be set by caller
                    )
                )
            }
            
            val publicUrl = publishResult.getOrThrow()
            println("✅ Publishing succeeded - results available at: $publicUrl")
            
            return Result.success(
                WorkflowResult(
                    asciidocGenerated = true,
                    parseSucceeded = true,
                    renderSucceeded = true,
                    publishSucceeded = true,
                    publicUrl = publicUrl,
                    errors = errors,
                    durationMs = 0 // Will be set by caller
                )
            )
        } else {
            println("⚠️  Skipping publication (no publisher configured)")
            
            // Return success after rendering (dry-run mode)
            return Result.success(
                WorkflowResult(
                    asciidocGenerated = true,
                    parseSucceeded = true,
                    renderSucceeded = true,
                    publishSucceeded = false,
                    publicUrl = null,
                    errors = errors,
                    durationMs = 0 // Will be set by caller
                )
            )
        }
    }
    
    /**
     * Export test results to AsciiDoc format.
     *
     * @param results Test results to export
     * @param errors Mutable list to collect error messages
     * @return Result containing the AsciiDoc document string
     */
    private fun exportToAsciidoc(
        results: AggregatedResults,
        errors: MutableList<String>
    ): Result<String> {
        val metadata = createExportMetadata(results)
        
        return exporter.export(results, metadata).onFailure { error ->
            val errorMessage = "Export failed: ${error.message}"
            errors.add(errorMessage)
            println("ERROR: $errorMessage")
            
            // Log the full exception for debugging
            if (error.stackTraceToString().isNotEmpty()) {
                println("Stack trace: ${error.stackTraceToString()}")
            }
        }
    }
    
    /**
     * Parse the AsciiDoc document using our own parser (CRITICAL - dogfooding!).
     *
     * This method treats parse errors with the highest severity because they indicate
     * a critical bug in either the exporter or parser. Parse errors are logged with
     * detailed line/column information for debugging.
     *
     * @param asciidoc The AsciiDoc document to parse
     * @param errors Mutable list to collect error messages
     * @return Result containing the parse result, or failure if parsing failed
     */
    private fun parseAsciidoc(
        asciidoc: String,
        errors: MutableList<String>
    ): Result<org.markup.poet.asciidoc.parser.ParseResult> {
        val parseResult = parser.parse(asciidoc)
        
        // Check for parse errors (CRITICAL!)
        if (parseResult.errors.isNotEmpty()) {
            // This is a CRITICAL BUG - our parser cannot parse our own output!
            val criticalError = buildString {
                appendLine("CRITICAL BUG: Parser failed on our own output!")
                appendLine("This indicates a bug in either the exporter or parser.")
                appendLine()
                appendLine("Parse Errors (${parseResult.errors.size}):")
                
                for ((index, error) in parseResult.errors.withIndex()) {
                    appendLine()
                    appendLine("Error ${index + 1}:")
                    appendLine("  Message: ${error.message}")
                    appendLine("  Location: Line ${error.location.line}, Column ${error.location.column}")
                    
                    if (error.location.endLine != error.location.line || 
                        error.location.endColumn != error.location.column) {
                        appendLine("  End Location: Line ${error.location.endLine}, Column ${error.location.endColumn}")
                    }
                    
                    appendLine("  Severity: ${error.severity}")
                    
                    // Extract the problematic line from the source for context
                    val lines = asciidoc.lines()
                    if (error.location.line > 0 && error.location.line <= lines.size) {
                        val lineIndex = error.location.line - 1 // Convert to 0-based
                        val sourceLine = lines[lineIndex]
                        appendLine("  Source Line: $sourceLine")
                        
                        // Add a pointer to the column position
                        if (error.location.column > 0) {
                            val pointer = " ".repeat(error.location.column - 1) + "^"
                            appendLine("               $pointer")
                        }
                    }
                }
                
                // Add context about the document
                appendLine()
                appendLine("Document Statistics:")
                appendLine("  Total Lines: ${asciidoc.lines().size}")
                appendLine("  Total Characters: ${asciidoc.length}")
                appendLine()
                appendLine("First 500 characters of generated AsciiDoc:")
                appendLine(asciidoc.take(500))
                if (asciidoc.length > 500) {
                    appendLine("... (truncated)")
                }
            }
            
            errors.add(criticalError)
            println("ERROR: $criticalError")
            
            // Return failure with detailed parse error information
            return Result.failure(
                ParseFailureException(
                    message = "Parser failed on our own output (CRITICAL BUG)",
                    parseErrors = parseResult.errors.map { error ->
                        "Line ${error.location.line}, Column ${error.location.column}: ${error.message}"
                    },
                    asciidocContent = asciidoc
                )
            )
        }
        
        // Log warnings if present (not critical, but should be investigated)
        if (parseResult.warnings.isNotEmpty()) {
            val warningMessage = buildString {
                appendLine("Parse warnings detected (${parseResult.warnings.size}):")
                for ((index, warning) in parseResult.warnings.withIndex()) {
                    appendLine("  Warning ${index + 1}: ${warning.message} " +
                            "(Line ${warning.location.line}, Column ${warning.location.column})")
                }
            }
            println("WARNING: $warningMessage")
            errors.add(warningMessage)
        }
        
        println("✅ Parsing succeeded - document structure validated")
        return Result.success(parseResult)
    }
    
    /**
     * Render the Document AST to HTML using the TCK HTML renderer.
     *
     * This method uses the TckHtmlRenderer wrapper which applies the appropriate
     * configuration (KotlinTheme, inline CSS, etc.) for TCK results publishing.
     *
     * @param document The Document AST to render
     * @param errors Mutable list to collect error messages
     * @return Result containing the HTML string, or failure if rendering failed
     */
    private fun renderToHtml(
        document: org.markup.poet.asciidoc.ast.Document,
        errors: MutableList<String>
    ): Result<String> {
        val renderResult = renderer.render(document)
        
        return renderResult.onFailure { error ->
            val errorMessage = "Render failed: ${error.message}"
            errors.add(errorMessage)
            println("ERROR: $errorMessage")
            
            // Log the full exception for debugging
            if (error.stackTraceToString().isNotEmpty()) {
                println("Stack trace: ${error.stackTraceToString()}")
            }
        }.onSuccess {
            println("✅ Rendering succeeded - HTML generated")
        }
    }
    
    /**
     * Publish the rendered HTML to GitHub Pages.
     *
     * This method uses the GitHubPagesPublisher to commit and push the HTML
     * to the gh-pages branch, maintaining historical archives.
     *
     * @param html The rendered HTML to publish
     * @param results Test results for metadata
     * @param errors Mutable list to collect error messages
     * @return Result containing the public URL, or failure if publishing failed
     */
    private suspend fun publishToGitHub(
        html: String,
        results: AggregatedResults,
        errors: MutableList<String>
    ): Result<String> {
        if (publisher == null || config == null) {
            val errorMessage = "Publisher or config is null - cannot publish"
            errors.add(errorMessage)
            return Result.failure(Exception(errorMessage))
        }
        
        val metadata = createPublishMetadata(results)
        
        return publisher.publish(html, metadata)
            .onFailure { error ->
                val errorMessage = "Publish failed: ${error.message}"
                errors.add(errorMessage)
                println("ERROR: $errorMessage")
                
                // Log the full exception for debugging
                if (error.stackTraceToString().isNotEmpty()) {
                    println("Stack trace: ${error.stackTraceToString()}")
                }
            }
            .map { publishResult ->
                publishResult.publicUrl
            }
    }
    
    /**
     * Create export metadata for the current test run.
     */
    private fun createExportMetadata(results: AggregatedResults): ExportMetadata {
        return ExportMetadata(
            timestamp = Clock.System.now().toEpochMilliseconds(),
            specVersion = "1.0.0", // TODO: Make configurable
            tckCommitHash = "unknown", // TODO: Get from Git
            libraryVersion = "1.0.0", // TODO: Get from build configuration
            platforms = results.byPlatform.keys.toList(),
            runId = generateRunId()
        )
    }
    
    /**
     * Generate a unique run ID for this test execution.
     */
    private fun generateRunId(): String {
        val now = Clock.System.now()
        val localDateTime = now.toLocalDateTime(TimeZone.UTC)
        
        // Format: YYYY-MM-DD-HHMMSS
        return with(localDateTime) {
            "${year}-${monthNumber.toString().padStart(2, '0')}-${dayOfMonth.toString().padStart(2, '0')}-" +
            "${hour.toString().padStart(2, '0')}${minute.toString().padStart(2, '0')}${second.toString().padStart(2, '0')}"
        }
    }
    
    /**
     * Create publish metadata for the current test run.
     */
    private fun createPublishMetadata(results: AggregatedResults): PublishMetadata {
        val totalTests = results.totalTests
        val passedTests = results.passed
        val passRate = if (totalTests > 0) passedTests.toDouble() / totalTests else 0.0
        
        return PublishMetadata(
            runId = generateRunId(),
            timestamp = Clock.System.now().toEpochMilliseconds(),
            specVersion = "1.0.0", // TODO: Make configurable
            passRate = passRate,
            totalTests = totalTests,
            passedTests = passedTests
        )
    }
}

/**
 * Exception thrown when parsing fails on our own generated AsciiDoc output.
 * This is a CRITICAL error that indicates a bug in either the exporter or parser.
 */
class ParseFailureException(
    message: String,
    val parseErrors: List<String>,
    val asciidocContent: String
) : Exception(message)
