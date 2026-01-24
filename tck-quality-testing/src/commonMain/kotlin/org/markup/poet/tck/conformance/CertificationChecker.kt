package org.markup.poet.tck.conformance

import org.markup.poet.tck.execution.AggregatedResults

/**
 * Checks certification readiness and identifies blocking issues.
 * 
 * The CertificationChecker analyzes test results to determine if the
 * implementation is ready for official AsciiDoc processor certification.
 * 
 * **Usage:**
 * ```kotlin
 * val checker = DefaultCertificationChecker()
 * val status = checker.checkStatus(aggregatedResults)
 * 
 * if (status.isReady) {
 *     println("Ready for certification!")
 * } else {
 *     println("Blocking issues: ${status.blockingIssues.size}")
 * }
 * ```
 */
interface CertificationChecker {
    /**
     * Check if the implementation is ready for certification.
     * 
     * @param results Aggregated test execution results
     * @return Certification status with blocking issues and recommendations
     */
    fun checkStatus(results: AggregatedResults): CertificationStatus
    
    /**
     * Get all certification requirements.
     * 
     * @return List of certification requirements
     */
    fun getRequirements(): List<CertificationRequirement>
}

/**
 * Default implementation of CertificationChecker.
 * 
 * Certification criteria:
 * 1. 100% of official TCK tests must pass
 * 2. Tests must pass on all supported platforms
 * 3. All required spec sections must be implemented
 * 4. No critical or high-severity blocking issues
 */
class DefaultCertificationChecker : CertificationChecker {
    
    companion object {
        /**
         * Minimum pass rate for official tests (100%).
         */
        private const val REQUIRED_OFFICIAL_PASS_RATE = 1.0
        
        /**
         * Minimum pass rate for overall tests (95%).
         */
        private const val REQUIRED_OVERALL_PASS_RATE = 0.95
    }
    
    override fun checkStatus(results: AggregatedResults): CertificationStatus {
        val blockingIssues = identifyBlockingIssues(results)
        val progress = calculateProgress(results)
        val isReady = blockingIssues.none { it.severity == IssueSeverity.CRITICAL || it.severity == IssueSeverity.HIGH }
                      && progress >= 100.0
        val recommendations = generateRecommendations(results, blockingIssues)
        
        return CertificationStatus(
            isReady = isReady,
            overallProgress = progress,
            blockingIssues = blockingIssues,
            recommendations = recommendations
        )
    }
    
    override fun getRequirements(): List<CertificationRequirement> {
        return listOf(
            CertificationRequirement(
                id = "official-tests-100",
                description = "100% of official TCK tests must pass",
                required = true,
                met = false,
                notes = "All tests from the official Eclipse AsciiDoc TCK must pass"
            ),
            CertificationRequirement(
                id = "all-platforms",
                description = "Tests must pass on all supported platforms",
                required = true,
                met = false,
                notes = "JVM, iOS, Linux, and Android platforms must all pass"
            ),
            CertificationRequirement(
                id = "spec-compliance",
                description = "All required spec sections must be implemented",
                required = true,
                met = false,
                notes = "Core AsciiDoc specification features must be fully implemented"
            ),
            CertificationRequirement(
                id = "no-critical-issues",
                description = "No critical or high-severity blocking issues",
                required = true,
                met = false,
                notes = "All critical and high-severity issues must be resolved"
            )
        )
    }
    
    /**
     * Identify blocking issues from test results.
     */
    private fun identifyBlockingIssues(results: AggregatedResults): List<BlockingIssue> {
        val issues = mutableListOf<BlockingIssue>()
        
        // Check official test pass rate
        val officialResults = results.bySource["official-tck"]
        if (officialResults != null && officialResults.passRate < REQUIRED_OFFICIAL_PASS_RATE) {
            issues.add(BlockingIssue(
                severity = IssueSeverity.CRITICAL,
                description = "Official TCK tests not passing at 100%",
                affectedTests = results.failedTests
                    .filter { it.source == "official-tck" }
                    .map { it.fixtureId },
                resolution = "Fix all failing official TCK tests to achieve 100% pass rate"
            ))
        }
        
        // Check overall pass rate
        if (results.passRate() < REQUIRED_OVERALL_PASS_RATE) {
            issues.add(BlockingIssue(
                severity = IssueSeverity.HIGH,
                description = "Overall pass rate below 95%",
                affectedTests = results.failedTests.map { it.fixtureId },
                resolution = "Improve overall test pass rate to at least 95%"
            ))
        }
        
        // Check for platform-specific failures
        results.byPlatform.forEach { (platform, platformResults) ->
            if (platformResults.passRate < 0.95) {
                issues.add(BlockingIssue(
                    severity = IssueSeverity.HIGH,
                    description = "Platform $platform has pass rate below 95%",
                    affectedTests = results.failedTests
                        .filter { it.platform == platform }
                        .map { it.fixtureId },
                    resolution = "Fix platform-specific issues on $platform"
                ))
            }
        }
        
        // Check for high number of pending tests
        if (results.pending > results.totalTests * 0.1) {
            issues.add(BlockingIssue(
                severity = IssueSeverity.MEDIUM,
                description = "More than 10% of tests are pending implementation",
                affectedTests = results.pendingTests.map { it.fixtureId },
                resolution = "Implement pending features to reduce pending test count"
            ))
        }
        
        return issues
    }
    
    /**
     * Calculate overall progress toward certification.
     * 
     * Progress is based on:
     * - Official test pass rate (50% weight)
     * - Overall test pass rate (30% weight)
     * - Platform consistency (20% weight)
     */
    private fun calculateProgress(results: AggregatedResults): Double {
        val officialResults = results.bySource["official-tck"]
        val officialPassRate = officialResults?.passRate ?: 0.0
        val overallPassRate = results.passRate()
        
        // Calculate platform consistency (how similar pass rates are across platforms)
        val platformPassRates = results.byPlatform.values.map { it.passRate }
        val platformConsistency = if (platformPassRates.isNotEmpty()) {
            val avgPassRate = platformPassRates.average()
            val variance = platformPassRates.map { (it - avgPassRate) * (it - avgPassRate) }.average()
            1.0 - variance.coerceIn(0.0, 1.0)
        } else {
            1.0
        }
        
        // Weighted average
        val progress = (officialPassRate * 0.5 + overallPassRate * 0.3 + platformConsistency * 0.2) * 100.0
        
        return progress.coerceIn(0.0, 100.0)
    }
    
    /**
     * Generate actionable recommendations.
     */
    private fun generateRecommendations(
        results: AggregatedResults,
        blockingIssues: List<BlockingIssue>
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        // Recommendations based on blocking issues
        if (blockingIssues.any { it.severity == IssueSeverity.CRITICAL }) {
            recommendations.add("Address all critical blocking issues immediately")
        }
        
        if (blockingIssues.any { it.severity == IssueSeverity.HIGH }) {
            recommendations.add("Resolve high-severity issues before certification submission")
        }
        
        // Recommendations based on test results
        val officialResults = results.bySource["official-tck"]
        if (officialResults != null && officialResults.failed > 0) {
            recommendations.add("Focus on fixing ${officialResults.failed} failing official TCK tests")
        }
        
        if (results.pending > 0) {
            recommendations.add("Implement ${results.pending} pending features")
        }
        
        // Platform-specific recommendations
        results.byPlatform.forEach { (platform, platformResults) ->
            if (platformResults.failed > 0) {
                recommendations.add("Fix ${platformResults.failed} failing tests on $platform")
            }
        }
        
        // Category-specific recommendations
        val worstCategory = results.byCategory.values.minByOrNull { it.passRate }
        if (worstCategory != null && worstCategory.passRate < 0.8) {
            recommendations.add("Improve ${worstCategory.category} implementation (currently ${(worstCategory.passRate * 100).toInt()}% pass rate)")
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("Implementation is ready for certification submission")
        }
        
        return recommendations
    }
}
