package org.markup.poet.tck.conformance

import kotlinx.serialization.Serializable

/**
 * Certification readiness status.
 * 
 * Indicates whether the implementation is ready for official AsciiDoc
 * processor certification, along with progress metrics and blocking issues.
 */
@Serializable
data class CertificationStatus(
    /**
     * Whether the implementation is ready for certification.
     * 
     * True if all certification requirements are met and there are no
     * blocking issues.
     */
    val isReady: Boolean,
    
    /**
     * Overall progress toward certification (0.0 to 100.0).
     * 
     * Calculated based on test pass rates and requirement completion.
     */
    val overallProgress: Double,
    
    /**
     * List of blocking issues preventing certification.
     * 
     * Empty if ready for certification.
     */
    val blockingIssues: List<BlockingIssue>,
    
    /**
     * Recommendations for achieving certification.
     * 
     * Actionable steps to resolve blocking issues and improve conformance.
     */
    val recommendations: List<String>
)

/**
 * A blocking issue preventing certification.
 */
@Serializable
data class BlockingIssue(
    /**
     * Severity of the issue.
     */
    val severity: IssueSeverity,
    
    /**
     * Description of the issue.
     */
    val description: String,
    
    /**
     * Tests affected by this issue.
     */
    val affectedTests: List<String>,
    
    /**
     * Suggested resolution steps.
     */
    val resolution: String
)

/**
 * Severity level for blocking issues.
 */
@Serializable
enum class IssueSeverity {
    /**
     * Critical issue that must be resolved for certification.
     * 
     * Examples:
     * - Core spec features not implemented
     * - Major test failures across all platforms
     */
    CRITICAL,
    
    /**
     * High-priority issue that should be resolved soon.
     * 
     * Examples:
     * - Important spec features partially implemented
     * - Test failures on multiple platforms
     */
    HIGH,
    
    /**
     * Medium-priority issue that affects certification progress.
     * 
     * Examples:
     * - Optional spec features not implemented
     * - Test failures on single platform
     */
    MEDIUM,
    
    /**
     * Low-priority issue that has minimal impact.
     * 
     * Examples:
     * - Edge case handling
     * - Minor spec deviations
     */
    LOW
}

/**
 * A certification requirement.
 */
@Serializable
data class CertificationRequirement(
    /**
     * Unique identifier for the requirement.
     */
    val id: String,
    
    /**
     * Human-readable description.
     */
    val description: String,
    
    /**
     * Whether this requirement is mandatory for certification.
     */
    val required: Boolean,
    
    /**
     * Whether this requirement has been met.
     */
    val met: Boolean,
    
    /**
     * Additional details or notes about the requirement.
     */
    val notes: String? = null
)
