package org.markup.poet.tck.fixtures

import kotlinx.serialization.Serializable

/**
 * Represents a test case from the official Eclipse AsciiDoc TCK.
 * 
 * Official TCK tests consist of paired files:
 * - `{test-name}-input.adoc`: The AsciiDoc input
 * - `{test-name}-output.json`: The expected AST output in JSON format
 * 
 * This data class represents the parsed and combined data from both files.
 * 
 * **Example:**
 * ```
 * tests/block/paragraph/simple-paragraph-input.adoc
 * tests/block/paragraph/simple-paragraph-output.json
 * ```
 * 
 * Maps to:
 * ```kotlin
 * OfficialTestData(
 *     testId = "block-paragraph-simple-paragraph",
 *     description = "Simple paragraph test",
 *     input = "This is a simple paragraph.",
 *     expectedOutput = OfficialAstNode(...),
 *     category = "block/paragraph",
 *     metadata = mapOf("source" to "official-tck")
 * )
 * ```
 */
@Serializable
data class OfficialTestData(
    /**
     * Unique identifier for the test.
     * Format: "{category}-{test-name}"
     * Example: "block-paragraph-simple-paragraph"
     */
    val testId: String,
    
    /**
     * Human-readable description of what the test validates.
     * Extracted from test file name or metadata.
     */
    val description: String,
    
    /**
     * The AsciiDoc input content from the *-input.adoc file.
     */
    val input: String,
    
    /**
     * The expected AST output from the *-output.json file.
     * This is the parsed JSON AST structure.
     */
    val expectedOutput: OfficialAstNode,
    
    /**
     * Test category based on directory structure.
     * Examples: "block/paragraph", "inline/bold", "conformance/attributes"
     */
    val category: String,
    
    /**
     * Specification section reference (if available).
     * Example: "6.1.2" for paragraph blocks
     */
    val specReference: String? = null,
    
    /**
     * Additional metadata about the test.
     * May include:
     * - "source": "official-tck"
     * - "file_path": Original file path in TCK repository
     * - "tags": Test tags or categories
     */
    val metadata: Map<String, String> = emptyMap()
) {
    /**
     * Get the test name without category prefix.
     * Example: "block-paragraph-simple-paragraph" -> "simple-paragraph"
     */
    fun getTestName(): String {
        val parts = testId.split("-")
        return if (parts.size > 2) {
            parts.drop(2).joinToString("-")
        } else {
            testId
        }
    }
    
    /**
     * Get the category as a FixtureCategory enum.
     * Uses CategoryMapper to convert directory path to enum.
     */
    fun getFixtureCategory(): FixtureCategory {
        return when {
            category.contains("block/paragraph") -> FixtureCategory.BLOCK_PARAGRAPH
            category.contains("block/heading") -> FixtureCategory.BLOCK_HEADING
            category.contains("block/list") -> FixtureCategory.BLOCK_LIST
            category.contains("block/table") -> FixtureCategory.BLOCK_TABLE
            category.contains("block/code") -> FixtureCategory.BLOCK_CODE
            category.contains("block/quote") -> FixtureCategory.BLOCK_QUOTE
            category.contains("inline/bold") || category.contains("inline/strong") -> FixtureCategory.INLINE_BOLD
            category.contains("inline/italic") || category.contains("inline/emphasis") -> FixtureCategory.INLINE_ITALIC
            category.contains("inline/monospace") || category.contains("inline/code") -> FixtureCategory.INLINE_MONOSPACE
            category.contains("attribute") -> FixtureCategory.ATTRIBUTE
            category.contains("macro") -> FixtureCategory.MACRO
            category.contains("cross-reference") || category.contains("xref") -> FixtureCategory.CROSS_REFERENCE
            category.contains("include") -> FixtureCategory.INCLUDE
            else -> FixtureCategory.CONFORMANCE
        }
    }
    
    /**
     * Check if this test is from the official TCK.
     */
    fun isOfficialTest(): Boolean {
        return metadata["source"] == "official-tck"
    }
}

/**
 * Builder for creating OfficialTestData instances.
 * Provides a fluent API for constructing test data.
 */
class OfficialTestDataBuilder {
    private var testId: String = ""
    private var description: String = ""
    private var input: String = ""
    private var expectedOutput: OfficialAstNode? = null
    private var category: String = ""
    private var specReference: String? = null
    private val metadata: MutableMap<String, String> = mutableMapOf()
    
    fun testId(id: String) = apply { this.testId = id }
    fun description(desc: String) = apply { this.description = desc }
    fun input(content: String) = apply { this.input = content }
    fun expectedOutput(output: OfficialAstNode) = apply { this.expectedOutput = output }
    fun category(cat: String) = apply { this.category = cat }
    fun specReference(ref: String?) = apply { this.specReference = ref }
    fun metadata(key: String, value: String) = apply { this.metadata[key] = value }
    fun metadata(map: Map<String, String>) = apply { this.metadata.putAll(map) }
    
    fun build(): OfficialTestData {
        require(testId.isNotEmpty()) { "testId is required" }
        require(description.isNotEmpty()) { "description is required" }
        require(input.isNotEmpty()) { "input is required" }
        require(expectedOutput != null) { "expectedOutput is required" }
        require(category.isNotEmpty()) { "category is required" }
        
        return OfficialTestData(
            testId = testId,
            description = description,
            input = input,
            expectedOutput = expectedOutput!!,
            category = category,
            specReference = specReference,
            metadata = metadata.toMap()
        )
    }
}

/**
 * DSL function for building OfficialTestData.
 */
fun officialTestData(block: OfficialTestDataBuilder.() -> Unit): OfficialTestData {
    return OfficialTestDataBuilder().apply(block).build()
}
