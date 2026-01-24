package org.markup.poet.tck.conformance

import org.markup.poet.tck.execution.AggregatedResults
import org.markup.poet.tck.execution.TestExecutionResult

/**
 * Generates conformance reports from aggregated test results.
 * 
 * The ReportGenerator transforms raw test execution results into a
 * comprehensive conformance report suitable for certification submission.
 * 
 * **Usage:**
 * ```kotlin
 * val generator = DefaultReportGenerator(certificationChecker)
 * val report = generator.generateReport(aggregatedResults, metadata)
 * ```
 */
interface ReportGenerator {
    /**
     * Generate a complete conformance report.
     * 
     * @param results Aggregated test execution results
     * @param metadata Report metadata (versions, platforms, etc.)
     * @return Complete conformance report
     */
    fun generateReport(
        results: AggregatedResults,
        metadata: ReportMetadata
    ): ConformanceReport
}

/**
 * Default implementation of ReportGenerator.
 * 
 * Builds a comprehensive conformance report with:
 * - Overall summary statistics
 * - Platform-specific breakdowns
 * - Category-specific breakdowns
 * - Spec section coverage
 * - Failed test details
 * - Pending test details
 * - Certification status
 */
class DefaultReportGenerator(
    private val certificationChecker: CertificationChecker
) : ReportGenerator {
    
    override fun generateReport(
        results: AggregatedResults,
        metadata: ReportMetadata
    ): ConformanceReport {
        val summary = buildSummary(results)
        val platformResults = buildPlatformResults(results)
        val categoryResults = buildCategoryResults(results)
        val specSectionResults = buildSpecSectionResults(results)
        val failedTests = buildFailedTestDetails(results)
        val pendingTests = buildPendingTestDetails(results)
        val certificationStatus = certificationChecker.checkStatus(results)
        
        return ConformanceReport(
            metadata = metadata,
            summary = summary,
            platformResults = platformResults,
            categoryResults = categoryResults,
            specSectionResults = specSectionResults,
            failedTests = failedTests,
            pendingTests = pendingTests,
            certificationStatus = certificationStatus
        )
    }
    
    /**
     * Build overall summary from aggregated results.
     */
    private fun buildSummary(results: AggregatedResults): ConformanceSummary {
        // Calculate pass rates by source
        val officialResults = results.bySource["official-tck"]
        val customResults = results.bySource["custom"]
        
        val officialPassRate = officialResults?.passRate ?: 0.0
        val customPassRate = customResults?.passRate ?: 0.0
        
        // Calculate total duration
        val totalDurationMs = results.failedTests.sumOf { it.durationMs } +
                              results.pendingTests.sumOf { it.durationMs } +
                              (results.totalTests - results.failedTests.size - results.pendingTests.size) * 100L
        
        return ConformanceSummary(
            totalTests = results.totalTests,
            passed = results.passed,
            failed = results.failed,
            pending = results.pending,
            skipped = results.skipped,
            overallPassRate = results.passRate(),
            officialTestsPassRate = officialPassRate,
            customTestsPassRate = customPassRate,
            totalDurationMs = totalDurationMs
        )
    }
    
    /**
     * Build platform-specific results.
     */
    private fun buildPlatformResults(results: AggregatedResults): List<PlatformConformance> {
        return results.byPlatform.map { (platform, platformResults) ->
            // Find failed test IDs for this platform
            val failedTestIds = results.failedTests
                .filter { it.platform == platform }
                .map { it.fixtureId }
            
            PlatformConformance(
                platform = platform,
                totalTests = platformResults.total,
                passed = platformResults.passed,
                failed = platformResults.failed,
                passRate = platformResults.passRate,
                failedTestIds = failedTestIds
            )
        }
    }
    
    /**
     * Build category-specific results.
     */
    private fun buildCategoryResults(results: AggregatedResults): List<CategoryConformance> {
        return results.byCategory.map { (category, categoryResults) ->
            CategoryConformance(
                category = category.name,
                totalTests = categoryResults.total,
                passed = categoryResults.passed,
                failed = categoryResults.failed,
                passRate = categoryResults.passRate,
                specSection = null // TODO: Map categories to spec sections
            )
        }
    }
    
    /**
     * Build spec section results.
     * 
     * Note: Currently returns empty list as spec section mapping is not yet implemented.
     */
    private fun buildSpecSectionResults(results: AggregatedResults): List<SpecSectionConformance> {
        // TODO: Implement spec section mapping
        // For now, return empty list
        return emptyList()
    }
    
    /**
     * Build detailed information about failed tests.
     */
    private fun buildFailedTestDetails(results: AggregatedResults): List<FailedTestDetail> {
        // Group failed tests by ID to collect all platforms where they failed
        val failedById = results.failedTests.groupBy { it.fixtureId }
        
        return failedById.map { (testId, failures) ->
            val first = failures.first()
            
            FailedTestDetail(
                testId = testId,
                description = first.metadata["description"] ?: "No description",
                category = first.category?.name ?: "UNKNOWN",
                specSection = first.metadata["spec_section"] ?: "Unknown",
                platforms = failures.map { it.platform },
                errorMessage = first.errorMessage ?: "No error message",
                expectedOutput = first.expectedOutput,
                actualOutput = first.actualOutput
            )
        }
    }
    
    /**
     * Build detailed information about pending tests.
     */
    private fun buildPendingTestDetails(results: AggregatedResults): List<PendingTestDetail> {
        // Group pending tests by ID (they may appear on multiple platforms)
        val pendingById = results.pendingTests.groupBy { it.fixtureId }
        
        return pendingById.map { (testId, pending) ->
            val first = pending.first()
            
            PendingTestDetail(
                testId = testId,
                description = first.metadata["description"] ?: "No description",
                category = first.category?.name ?: "UNKNOWN",
                specSection = first.metadata["spec_section"] ?: "Unknown",
                reason = first.errorMessage ?: "Feature not implemented"
            )
        }
    }
}
