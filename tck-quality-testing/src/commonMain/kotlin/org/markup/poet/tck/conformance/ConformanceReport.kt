package org.markup.poet.tck.conformance

import kotlinx.serialization.Serializable

/**
 * Complete conformance report for certification.
 * 
 * This report contains all information needed for AsciiDoc processor certification:
 * - Test execution summary
 * - Platform-specific results
 * - Category-specific results
 * - Spec section coverage
 * - Failed test details
 * - Pending test details
 * - Certification readiness status
 * 
 * **Usage:**
 * ```kotlin
 * val generator = DefaultReportGenerator(certificationChecker)
 * val report = generator.generateReport(aggregatedResults, metadata)
 * 
 * // Export to JSON
 * val jsonReporter = DefaultJsonReporter()
 * val json = jsonReporter.generateJson(report)
 * ```
 */
@Serializable
data class ConformanceReport(
    /**
     * Report metadata (timestamp, versions, platforms).
     */
    val metadata: ReportMetadata,
    
    /**
     * Overall test execution summary.
     */
    val summary: ConformanceSummary,
    
    /**
     * Results broken down by platform.
     */
    val platformResults: List<PlatformConformance>,
    
    /**
     * Results broken down by category.
     */
    val categoryResults: List<CategoryConformance>,
    
    /**
     * Results broken down by spec section.
     */
    val specSectionResults: List<SpecSectionConformance>,
    
    /**
     * Detailed information about failed tests.
     */
    val failedTests: List<FailedTestDetail>,
    
    /**
     * Detailed information about pending tests.
     */
    val pendingTests: List<PendingTestDetail>,
    
    /**
     * Certification readiness status.
     */
    val certificationStatus: CertificationStatus
)

/**
 * Metadata about the conformance report.
 */
@Serializable
data class ReportMetadata(
    /**
     * Timestamp when the report was generated (milliseconds since epoch).
     */
    val generatedAt: Long,
    
    /**
     * AsciiDoc specification version.
     */
    val specVersion: String,
    
    /**
     * Official TCK commit hash.
     */
    val tckCommitHash: String,
    
    /**
     * Library version being tested.
     */
    val libraryVersion: String,
    
    /**
     * Platforms where tests were executed.
     */
    val platforms: List<String>
)

/**
 * Overall test execution summary.
 */
@Serializable
data class ConformanceSummary(
    /**
     * Total number of tests executed.
     */
    val totalTests: Int,
    
    /**
     * Number of tests that passed.
     */
    val passed: Int,
    
    /**
     * Number of tests that failed.
     */
    val failed: Int,
    
    /**
     * Number of tests that are pending implementation.
     */
    val pending: Int,
    
    /**
     * Number of tests that were skipped.
     */
    val skipped: Int,
    
    /**
     * Overall pass rate (0.0 to 1.0).
     */
    val overallPassRate: Double,
    
    /**
     * Pass rate for official TCK tests only (0.0 to 1.0).
     */
    val officialTestsPassRate: Double,
    
    /**
     * Pass rate for custom tests only (0.0 to 1.0).
     */
    val customTestsPassRate: Double,
    
    /**
     * Total test execution duration in milliseconds.
     */
    val totalDurationMs: Long
)

/**
 * Test results for a specific platform.
 */
@Serializable
data class PlatformConformance(
    /**
     * Platform name (e.g., "JVM", "iOS", "Linux").
     */
    val platform: String,
    
    /**
     * Total number of tests on this platform.
     */
    val totalTests: Int,
    
    /**
     * Number of tests that passed.
     */
    val passed: Int,
    
    /**
     * Number of tests that failed.
     */
    val failed: Int,
    
    /**
     * Pass rate for this platform (0.0 to 1.0).
     */
    val passRate: Double,
    
    /**
     * IDs of tests that failed on this platform.
     */
    val failedTestIds: List<String>
)

/**
 * Test results for a specific category.
 */
@Serializable
data class CategoryConformance(
    /**
     * Category name (e.g., "BLOCK_PARAGRAPH", "INLINE_BOLD").
     */
    val category: String,
    
    /**
     * Total number of tests in this category.
     */
    val totalTests: Int,
    
    /**
     * Number of tests that passed.
     */
    val passed: Int,
    
    /**
     * Number of tests that failed.
     */
    val failed: Int,
    
    /**
     * Pass rate for this category (0.0 to 1.0).
     */
    val passRate: Double,
    
    /**
     * Spec section this category relates to (if known).
     */
    val specSection: String?
)

/**
 * Test results for a specific spec section.
 */
@Serializable
data class SpecSectionConformance(
    /**
     * Spec section number (e.g., "4.2", "5.1").
     */
    val section: String,
    
    /**
     * Spec section title.
     */
    val title: String,
    
    /**
     * Total number of tests for this section.
     */
    val totalTests: Int,
    
    /**
     * Number of tests that passed.
     */
    val passed: Int,
    
    /**
     * Number of tests that failed.
     */
    val failed: Int,
    
    /**
     * Pass rate for this section (0.0 to 1.0).
     */
    val passRate: Double,
    
    /**
     * Whether this section is required for certification.
     */
    val requiredForCertification: Boolean
)

/**
 * Detailed information about a failed test.
 */
@Serializable
data class FailedTestDetail(
    /**
     * Test fixture ID.
     */
    val testId: String,
    
    /**
     * Test description.
     */
    val description: String,
    
    /**
     * Test category.
     */
    val category: String,
    
    /**
     * Spec section this test relates to.
     */
    val specSection: String,
    
    /**
     * Platforms where this test failed.
     */
    val platforms: List<String>,
    
    /**
     * Error message from the failure.
     */
    val errorMessage: String,
    
    /**
     * Expected output (if available).
     */
    val expectedOutput: String?,
    
    /**
     * Actual output produced.
     */
    val actualOutput: String?
)

/**
 * Detailed information about a pending test.
 */
@Serializable
data class PendingTestDetail(
    /**
     * Test fixture ID.
     */
    val testId: String,
    
    /**
     * Test description.
     */
    val description: String,
    
    /**
     * Test category.
     */
    val category: String,
    
    /**
     * Spec section this test relates to.
     */
    val specSection: String,
    
    /**
     * Reason why the test is pending.
     */
    val reason: String
)
