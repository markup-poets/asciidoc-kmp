package org.markup.poet.tck.conformance

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Generates JSON format conformance reports.
 * 
 * The JSON reporter serializes the conformance report to JSON format
 * suitable for:
 * - Machine processing
 * - API responses
 * - Data storage
 * - Integration with other tools
 * 
 * **Usage:**
 * ```kotlin
 * val reporter = DefaultJsonReporter()
 * val json = reporter.generateJson(report)
 * 
 * // Write to file
 * File("conformance-report.json").writeText(json)
 * ```
 */
interface JsonReporter {
    /**
     * Generate JSON representation of the conformance report.
     * 
     * @param report Conformance report to serialize
     * @return JSON string
     */
    fun generateJson(report: ConformanceReport): String
}

/**
 * Default implementation of JsonReporter.
 * 
 * Uses kotlinx.serialization to produce well-formatted JSON with:
 * - Pretty printing (indentation)
 * - Consistent field ordering
 * - Proper escaping
 */
class DefaultJsonReporter(
    /**
     * Whether to pretty-print the JSON output.
     */
    private val prettyPrint: Boolean = true
) : JsonReporter {
    
    private val json = Json {
        this.prettyPrint = this@DefaultJsonReporter.prettyPrint
        encodeDefaults = true
        ignoreUnknownKeys = true
    }
    
    override fun generateJson(report: ConformanceReport): String {
        return json.encodeToString(report)
    }
}

/**
 * Compact JSON reporter without pretty printing.
 * 
 * Useful for:
 * - Network transmission
 * - Storage optimization
 * - API responses
 */
class CompactJsonReporter : JsonReporter {
    private val reporter = DefaultJsonReporter(prettyPrint = false)
    
    override fun generateJson(report: ConformanceReport): String {
        return reporter.generateJson(report)
    }
}
