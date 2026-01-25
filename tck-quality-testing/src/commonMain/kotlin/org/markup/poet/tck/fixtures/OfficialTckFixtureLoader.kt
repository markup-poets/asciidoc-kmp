package org.markup.poet.tck.fixtures

import kotlinx.serialization.json.Json
import org.markup.poet.tck.platformFileExists
import org.markup.poet.tck.platformFindFiles
import org.markup.poet.tck.platformReadFile

/**
 * Loader for official Eclipse AsciiDoc TCK test fixtures.
 * 
 * This loader reads test cases from the official TCK repository structure:
 * ```
 * official-tck/repository/tests/
 * ├── block/
 * │   ├── paragraph/
 * │   │   ├── simple-paragraph-input.adoc
 * │   │   ├── simple-paragraph-output.json
 * │   │   ├── multiline-paragraph-input.adoc
 * │   │   └── multiline-paragraph-output.json
 * │   └── heading/
 * │       ├── level1-input.adoc
 * │       └── level1-output.json
 * └── inline/
 *     └── bold/
 *         ├── simple-bold-input.adoc
 *         └── simple-bold-output.json
 * ```
 * 
 * **Test Format:**
 * - Each test consists of two files: `{name}-input.adoc` and `{name}-output.json`
 * - The input file contains AsciiDoc source
 * - The output file contains the expected AST in JSON format
 * 
 * **Usage:**
 * ```kotlin
 * val loader = OfficialTckFixtureLoader("tck-quality-testing/official-tck/repository")
 * val allTests = loader.loadAllFixtures()
 * val paragraphTests = loader.loadFixturesByCategory(FixtureCategory.BLOCK_PARAGRAPH)
 * ```
 */
class OfficialTckFixtureLoader(
    private val tckRepositoryPath: String
) : FixtureLoader {
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    private val testsDirectory = "$tckRepositoryPath/tests"
    
    override fun loadFixture(id: String): TestFixture {
        // Find the test by ID
        val allTests = loadAllOfficialTests()
        val officialTest = allTests.find { it.testId == id }
            ?: throw FixtureNotFoundException(id)
        
        return convertToTestFixture(officialTest)
    }
    
    override fun loadFixturesByCategory(category: FixtureCategory): List<TestFixture> {
        val allTests = loadAllOfficialTests()
        return allTests
            .filter { it.getFixtureCategory() == category }
            .map { convertToTestFixture(it) }
    }
    
    override fun loadAllFixtures(): List<TestFixture> {
        val allTests = loadAllOfficialTests()
        return allTests.map { convertToTestFixture(it) }
    }
    
    override fun supports(path: String): Boolean {
        return path.startsWith(tckRepositoryPath) || path.contains("official-tck")
    }
    
    override fun getFormat(): FixtureFormat {
        return FixtureFormat.OFFICIAL_TCK
    }
    
    /**
     * Load all official test data from the TCK repository.
     * 
     * @return List of OfficialTestData instances
     */
    fun loadAllOfficialTests(): List<OfficialTestData> {
        if (!platformFileExists(testsDirectory)) {
            return emptyList()
        }
        
        val tests = mutableListOf<OfficialTestData>()
        
        // Find all input files
        val inputFiles = platformFindFiles(testsDirectory, "-input.adoc")
        
        for (inputFile in inputFiles) {
            try {
                val testData = parseTestPair(inputFile)
                tests.add(testData)
            } catch (e: Exception) {
                // Log warning but continue loading other tests
                println("Warning: Failed to load test from $inputFile: ${e.message}")
            }
        }
        
        return tests
    }
    
    /**
     * Parse a test pair (input.adoc + output.json) into OfficialTestData.
     * 
     * Handles both JSON formats:
     * - Array format (inline tests): `[{...}, {...}]`
     * - Object format (block tests): `{...}`
     * 
     * @param inputFilePath Path to the *-input.adoc file
     * @return Parsed OfficialTestData
     * @throws IllegalArgumentException if output file is missing or invalid
     */
    fun parseTestPair(inputFilePath: String): OfficialTestData {
        // Derive output file path
        val outputFilePath = inputFilePath.replace("-input.adoc", "-output.json")
        
        if (!platformFileExists(outputFilePath)) {
            throw IllegalArgumentException("Missing output file: $outputFilePath")
        }
        
        // Read input content
        val input = platformReadFile(inputFilePath)
        
        // Read output JSON (keep as string, don't parse yet)
        val outputJson = platformReadFile(outputFilePath)
        
        // Validate it's valid JSON
        try {
            Json.parseToJsonElement(outputJson)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid JSON in $outputFilePath: ${e.message}", e)
        }
        
        // Detect if it's an inline test (array) or block test (object)
        val trimmedJson = outputJson.trim()
        val isInlineTest = trimmedJson.startsWith("[")
        
        // Extract test metadata from file path
        val testId = extractTestId(inputFilePath)
        val category = extractCategory(inputFilePath)
        val description = extractDescription(inputFilePath)
        
        // Create a placeholder OfficialAstNode (we'll use the raw JSON string instead)
        val placeholderNode = OfficialAstNode(
            name = if (isInlineTest) "inline-array" else "document",
            type = if (isInlineTest) "inline" else "block"
        )
        
        return OfficialTestData(
            testId = testId,
            description = description,
            input = input,
            expectedOutput = placeholderNode,
            category = category,
            metadata = mapOf(
                "source" to "official",
                "type" to if (isInlineTest) "inline" else "block",
                "file_path" to inputFilePath,
                "input_file" to inputFilePath,
                "output_file" to outputFilePath,
                "expected_json" to outputJson  // Store raw JSON here
            )
        )
    }
    
    /**
     * Extract test ID from file path.
     * 
     * Example:
     * - Input: "tests/block/paragraph/simple-paragraph-input.adoc"
     * - Output: "block-paragraph-simple-paragraph"
     */
    private fun extractTestId(filePath: String): String {
        // Remove the tests/ prefix and -input.adoc suffix
        val relativePath = filePath
            .substringAfter("tests/")
            .substringBefore("-input.adoc")
        
        // Replace slashes with dashes
        return relativePath.replace("/", "-")
    }
    
    /**
     * Extract category from file path.
     * 
     * Example:
     * - Input: "tests/block/paragraph/simple-paragraph-input.adoc"
     * - Output: "block/paragraph"
     */
    private fun extractCategory(filePath: String): String {
        val relativePath = filePath.substringAfter("tests/")
        val parts = relativePath.split("/")
        
        // Take all parts except the filename
        return if (parts.size > 1) {
            parts.dropLast(1).joinToString("/")
        } else {
            "unknown"
        }
    }
    
    /**
     * Extract human-readable description from file path.
     * 
     * Example:
     * - Input: "tests/block/paragraph/simple-paragraph-input.adoc"
     * - Output: "Simple paragraph"
     */
    private fun extractDescription(filePath: String): String {
        val filename = filePath.substringAfterLast("/")
        val testName = filename
            .substringBefore("-input.adoc")
            .replace("-", " ")
        
        // Capitalize first letter
        return testName.replaceFirstChar { it.uppercase() }
    }
    
    /**
     * Convert OfficialTestData to internal TestFixture format.
     */
    private fun convertToTestFixture(officialTest: OfficialTestData): TestFixture {
        // Get the raw JSON from metadata
        val expectedJson = officialTest.metadata["expected_json"]
        
        return TestFixture(
            id = officialTest.testId,
            category = officialTest.getFixtureCategory(),
            description = officialTest.description,
            input = officialTest.input,
            expectedOutput = expectedJson,  // Use raw JSON string
            metadata = officialTest.metadata + mapOf(
                "official_ast" to "true",
                "spec_reference" to (officialTest.specReference ?: "")
            )
        )
    }
}

/**
 * Exception thrown when a fixture cannot be found.
 */
class FixtureNotFoundException(val fixtureId: String) : Exception("Fixture not found: $fixtureId")
