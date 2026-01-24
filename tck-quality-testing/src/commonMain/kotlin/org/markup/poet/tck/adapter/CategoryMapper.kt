package org.markup.poet.tck.adapter

import org.markup.poet.tck.fixtures.FixtureCategory

/**
 * Maps official TCK test directory paths to internal FixtureCategory enum values.
 * 
 * The official TCK organizes tests by directory structure (e.g., tests/block/paragraph/),
 * while our internal system uses a FixtureCategory enum. This mapper provides the
 * translation between the two systems.
 * 
 * **Directory Structure Mapping:**
 * - `tests/block/paragraph/` → `BLOCK_PARAGRAPH`
 * - `tests/block/section/` → `BLOCK_HEADING` (sections have headings)
 * - `tests/inline/span/strong/` → `INLINE_BOLD`
 * - etc.
 * 
 * **Usage:**
 * ```kotlin
 * val mapper = DefaultCategoryMapper()
 * val category = mapper.mapCategory("block/paragraph")
 * // Returns: FixtureCategory.BLOCK_PARAGRAPH
 * ```
 */
interface CategoryMapper {
    /**
     * Map an official TCK category path to an internal FixtureCategory.
     * 
     * @param officialCategory The directory path from the official TCK (e.g., "block/paragraph")
     * @return The corresponding internal FixtureCategory
     */
    fun mapCategory(officialCategory: String): FixtureCategory
    
    /**
     * Get all category mappings.
     * 
     * @return Map of official category paths to internal FixtureCategory values
     */
    fun getAllMappings(): Map<String, FixtureCategory>
    
    /**
     * Check if a category path is supported.
     * 
     * @param officialCategory The directory path to check
     * @return true if the category can be mapped, false otherwise
     */
    fun isSupported(officialCategory: String): Boolean
}

/**
 * Default implementation of CategoryMapper with comprehensive mappings.
 */
class DefaultCategoryMapper : CategoryMapper {
    
    /**
     * Mapping from official TCK directory paths to internal categories.
     * 
     * This map is used for exact path matching. Paths should not include
     * the "tests/" prefix or trailing slashes.
     */
    private val exactMappings = mapOf(
        // Block-level elements
        "block/paragraph" to FixtureCategory.BLOCK_PARAGRAPH,
        "block/section" to FixtureCategory.BLOCK_HEADING,
        "block/header" to FixtureCategory.BLOCK_HEADING,
        "block/document" to FixtureCategory.CONFORMANCE,
        "block/list" to FixtureCategory.BLOCK_LIST,
        "block/listing" to FixtureCategory.BLOCK_CODE,
        "block/sidebar" to FixtureCategory.BLOCK_QUOTE, // Closest match
        
        // Inline elements - no markup
        "inline/no-markup" to FixtureCategory.CONFORMANCE,
        
        // Inline elements - spans
        "inline/span/strong" to FixtureCategory.INLINE_BOLD,
        "inline/span/emphasis" to FixtureCategory.INLINE_ITALIC,
        "inline/span/monospace" to FixtureCategory.INLINE_MONOSPACE,
        "inline/span/mark" to FixtureCategory.INLINE_BOLD, // Closest match
        "inline/span/subscript" to FixtureCategory.INLINE_SUBSCRIPT,
        "inline/span/superscript" to FixtureCategory.INLINE_SUPERSCRIPT,
        
        // Attributes
        "attribute" to FixtureCategory.ATTRIBUTE,
        "attributes" to FixtureCategory.ATTRIBUTE,
        
        // Macros
        "macro" to FixtureCategory.MACRO,
        "macros" to FixtureCategory.MACRO,
        
        // Cross-references
        "xref" to FixtureCategory.CROSS_REFERENCE,
        "cross-reference" to FixtureCategory.CROSS_REFERENCE,
        
        // Includes
        "include" to FixtureCategory.INCLUDE
    )
    
    /**
     * Pattern-based mappings for categories that follow predictable patterns.
     */
    private val patternMappings = listOf(
        // Block patterns
        PatternMapping(Regex("^block/.*"), FixtureCategory.CONFORMANCE),
        
        // Inline patterns
        PatternMapping(Regex("^inline/.*"), FixtureCategory.CONFORMANCE),
        
        // Malformed patterns (if official TCK has error tests)
        PatternMapping(Regex(".*malformed.*"), FixtureCategory.MALFORMED_BLOCK),
        PatternMapping(Regex(".*error.*"), FixtureCategory.MALFORMED_BLOCK)
    )
    
    override fun mapCategory(officialCategory: String): FixtureCategory {
        // Normalize the path: remove "tests/" prefix and trailing slashes
        val normalized = officialCategory
            .removePrefix("tests/")
            .removeSuffix("/")
            .trim()
        
        // Try exact match first
        exactMappings[normalized]?.let { return it }
        
        // Try pattern matching
        for (pattern in patternMappings) {
            if (pattern.regex.matches(normalized)) {
                return pattern.category
            }
        }
        
        // Default to CONFORMANCE for unknown categories
        return FixtureCategory.CONFORMANCE
    }
    
    override fun getAllMappings(): Map<String, FixtureCategory> {
        return exactMappings.toMap()
    }
    
    override fun isSupported(officialCategory: String): Boolean {
        val normalized = officialCategory
            .removePrefix("tests/")
            .removeSuffix("/")
            .trim()
        
        // Check exact mappings
        if (normalized in exactMappings) {
            return true
        }
        
        // Check pattern mappings
        return patternMappings.any { it.regex.matches(normalized) }
    }
    
    /**
     * Helper class for pattern-based category mapping.
     */
    private data class PatternMapping(
        val regex: Regex,
        val category: FixtureCategory
    )
}

/**
 * Extension function to extract category from a test file path.
 * 
 * **Example:**
 * ```kotlin
 * val path = "tests/block/paragraph/single-line-input.adoc"
 * val category = path.extractCategory()
 * // Returns: "block/paragraph"
 * ```
 */
fun String.extractCategory(): String {
    // Remove "tests/" prefix if present
    val withoutPrefix = this.removePrefix("tests/")
    
    // Split by "/" and take all but the last part (filename)
    val parts = withoutPrefix.split("/")
    if (parts.size <= 1) {
        return ""
    }
    
    // Join all parts except the filename
    return parts.dropLast(1).joinToString("/")
}

/**
 * Extension function to extract test name from a file path.
 * 
 * **Example:**
 * ```kotlin
 * val path = "tests/block/paragraph/single-line-input.adoc"
 * val testName = path.extractTestName()
 * // Returns: "single-line"
 * ```
 */
fun String.extractTestName(): String {
    // Get the filename
    val filename = this.substringAfterLast("/")
    
    // Remove -input.adoc or -output.json suffix
    return filename
        .removeSuffix("-input.adoc")
        .removeSuffix("-output.json")
}

/**
 * Extension function to build a full test ID from a file path.
 * 
 * **Example:**
 * ```kotlin
 * val path = "tests/block/paragraph/single-line-input.adoc"
 * val testId = path.buildTestId()
 * // Returns: "block/paragraph/single-line"
 * ```
 */
fun String.buildTestId(): String {
    val category = this.extractCategory()
    val testName = this.extractTestName()
    
    return if (category.isNotEmpty()) {
        "$category/$testName"
    } else {
        testName
    }
}
