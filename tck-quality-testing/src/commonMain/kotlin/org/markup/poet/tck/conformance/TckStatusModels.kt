package org.markup.poet.tck.conformance

import kotlinx.serialization.Serializable

/**
 * TCK status.
 * 
 * Indicates whether every published test of the official AsciiDoc TCK passes,
 * along with progress metrics and blocking issues. This is a measurement, not
 * a certification: no official certification program exists.
 */
@Serializable
data class TckStatus(
    /**
     * Whether the implementation is passing all published TCK tests.
     * 
     * True if all TCK status requirements are met and there are no
     * blocking issues.
     */
    val isReady: Boolean,
    
    /**
     * Overall progress toward passing all published TCK tests (0.0 to 100.0).
     * 
     * Calculated based on test pass rates and requirement completion.
     */
    val overallProgress: Double,
    
    /**
     * List of blocking issues preventing a clean TCK status.
     * 
     * Empty if passing all published TCK tests.
     */
    val blockingIssues: List<BlockingIssue>,
    
    /**
     * Recommendations for achieving a clean TCK status.
     * 
     * Actionable steps to resolve blocking issues and improve conformance.
     */
    val recommendations: List<String>
)

/**
 * A blocking issue preventing a clean TCK status.
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
     * Critical issue that must be resolved for a clean TCK status.
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
     * Medium-priority issue that affects TCK progress.
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
 * A TCK status requirements.
 */
@Serializable
data class TckRequirement(
    /**
     * Unique identifier for the requirement.
     */
    val id: String,
    
    /**
     * Human-readable description.
     */
    val description: String,
    
    /**
     * Whether this requirement is mandatory for a clean TCK status.
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
