package org.markup.poet.tck.fixtures

import org.markup.poet.tck.sync.platformFileExists

/**
 * Detects the format of test fixture files.
 * 
 * Supports two formats:
 * 1. **Custom JSON**: Single JSON file with test data
 * 2. **Official TCK**: Paired files (*-input.adoc + *-output.json)
 * 
 * **Detection Strategy:**
 * - Check file path patterns
 * - Check directory structure
 * - Inspect file content if needed
 * 
 * **Usage:**
 * ```kotlin
 * val detector = DefaultFormatDetector()
 * val format = detector.detectFormat("tests/block/paragraph/simple-input.adoc")
 * // Returns: FixtureFormat.OFFICIAL_TCK
 * ```
 */
interface FormatDetector {
    /**
     * Detect the format of a test file based on its path.
     * 
     * @param filePath Path to the test file
     * @return The detected format
     */
    fun detectFormat(filePath: String): FixtureFormat
    
    /**
     * Detect format from file content.
     * 
     * @param content The file content
     * @return The detected format
     */
    fun detectFormatFromContent(content: String): FixtureFormat
    
    /**
     * Check if a path represents an official TCK test.
     * 
     * @param filePath Path to check
     * @return true if it's an official TCK test
     */
    fun isOfficialTckTest(filePath: String): Boolean
    
    /**
     * Check if a path represents a custom JSON test.
     * 
     * @param filePath Path to check
     * @return true if it's a custom JSON test
     */
    fun isCustomJsonTest(filePath: String): Boolean
}

/**
 * Default implementation of FormatDetector.
 * 
 * Uses multiple heuristics to detect test format:
 * 1. File path patterns (official-tck/, fixtures/)
 * 2. File naming conventions (*-input.adoc, *.json)
 * 3. Directory structure
 * 4. File content inspection (as fallback)
 */
class DefaultFormatDetector : FormatDetector {
    
    override fun detectFormat(filePath: String): FixtureFormat {
        return when {
            isOfficialTckTest(filePath) -> FixtureFormat.OFFICIAL_TCK
            isCustomJsonTest(filePath) -> FixtureFormat.CUSTOM_JSON
            else -> FixtureFormat.UNKNOWN
        }
    }
    
    override fun detectFormatFromContent(content: String): FixtureFormat {
        // Try to detect from content structure
        return when {
            // Custom JSON has specific structure with id, category, description, etc.
            content.trim().startsWith("{") && 
            content.contains("\"id\"") && 
            content.contains("\"category\"") -> FixtureFormat.CUSTOM_JSON
            
            // Official TCK output JSON has "name", "type", "blocks" structure
            content.trim().startsWith("{") && 
            content.contains("\"name\"") && 
            content.contains("\"type\"") -> FixtureFormat.OFFICIAL_TCK
            
            // AsciiDoc input files
            content.contains("=") || 
            content.contains("*") || 
            content.contains("_") -> FixtureFormat.OFFICIAL_TCK
            
            else -> FixtureFormat.UNKNOWN
        }
    }
    
    override fun isOfficialTckTest(filePath: String): Boolean {
        return when {
            // Check for official-tck directory
            filePath.contains("official-tck") -> true
            
            // Check for paired file naming convention
            filePath.endsWith("-input.adoc") -> {
                // Verify corresponding output file exists
                val outputPath = filePath.replace("-input.adoc", "-output.json")
                platformFileExists(outputPath)
            }
            
            filePath.endsWith("-output.json") -> {
                // Verify corresponding input file exists
                val inputPath = filePath.replace("-output.json", "-input.adoc")
                platformFileExists(inputPath)
            }
            
            // Check for tests/ directory structure (official TCK pattern)
            filePath.contains("/tests/block/") ||
            filePath.contains("/tests/inline/") ||
            filePath.contains("/tests/conformance/") -> true
            
            else -> false
        }
    }
    
    override fun isCustomJsonTest(filePath: String): Boolean {
        return when {
            // Check for fixtures directory (custom tests)
            filePath.contains("/fixtures/") -> true
            
            // Check for custom test naming pattern
            filePath.endsWith(".json") && 
            !filePath.endsWith("-output.json") &&
            !filePath.contains("official-tck") -> true
            
            // Check for specific custom fixture directories
            filePath.contains("/fixtures/blocks/") ||
            filePath.contains("/fixtures/inline/") ||
            filePath.contains("/fixtures/conformance/") ||
            filePath.contains("/fixtures/malformed/") -> true
            
            else -> false
        }
    }
    
    /**
     * Detect format for a directory.
     * Useful for batch operations.
     * 
     * @param directoryPath Path to directory
     * @return The format used in this directory
     */
    fun detectDirectoryFormat(directoryPath: String): FixtureFormat {
        return when {
            directoryPath.contains("official-tck") -> FixtureFormat.OFFICIAL_TCK
            directoryPath.contains("fixtures") -> FixtureFormat.CUSTOM_JSON
            else -> FixtureFormat.UNKNOWN
        }
    }
    
    /**
     * Get the appropriate loader for a given path.
     * 
     * @param filePath Path to test file
     * @param customLoader Custom JSON fixture loader
     * @param officialLoader Official TCK fixture loader
     * @return The appropriate loader for this path
     */
    fun getLoaderForPath(
        filePath: String,
        customLoader: FixtureLoader,
        officialLoader: FixtureLoader
    ): FixtureLoader {
        return when (detectFormat(filePath)) {
            FixtureFormat.CUSTOM_JSON -> customLoader
            FixtureFormat.OFFICIAL_TCK -> officialLoader
            FixtureFormat.UNKNOWN -> customLoader // Default to custom
        }
    }
}

/**
 * Enum representing different test fixture formats.
 */
enum class FixtureFormat {
    /**
     * Custom JSON format used for project-specific tests.
     * Single JSON file with test data.
     */
    CUSTOM_JSON,
    
    /**
     * Official Eclipse AsciiDoc TCK format.
     * Paired files: *-input.adoc and *-output.json
     */
    OFFICIAL_TCK,
    
    /**
     * Unknown or unsupported format.
     */
    UNKNOWN
}
