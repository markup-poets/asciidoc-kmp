package org.markup.poet.tck.integration

import kotlinx.coroutines.runBlocking
import org.markup.poet.asciidoc.parser.DefaultAsciidocParser
import org.markup.poet.tck.serialization.AstJsonSerializer
import org.markup.poet.tck.execution.JsonComparator
import kotlin.test.Test
import java.io.File

/**
 * Test a single official TCK test to debug performance and see results.
 */
class SingleOfficialTest {
    
    private val parser = DefaultAsciidocParser()
    private val serializer = AstJsonSerializer()
    
    @Test
    fun `should run single official test - single word`() {
        println("\n" + "=".repeat(60))
        println("Testing Single Official TCK Test")
        println("=".repeat(60))
        
        val testPath = "tck-quality-testing/official-tck/repository/tests/inline/no-markup"
        val inputFile = File("$testPath/single-word-input.adoc")
        val outputFile = File("$testPath/single-word-output.json")
        
        if (!inputFile.exists()) {
            println("⚠️  Test files not found. Official TCK may not be synced.")
            println("   Expected: $testPath")
            return
        }
        
        println("\n📝 Test: inline/no-markup/single-word")
        println("   Input file: ${inputFile.name}")
        println("   Output file: ${outputFile.name}")
        
        // Read input and expected output
        val input = inputFile.readText()
        val expectedOutput = outputFile.readText()
        
        println("\n📥 Input:")
        println("   ${input.take(100)}")
        
        // Parse
        val startParse = System.currentTimeMillis()
        val parseResult = parser.parseToAsg(input)
        val parseDuration = System.currentTimeMillis() - startParse
        
        println("\n⚙️  Parsing:")
        println("   Duration: ${parseDuration}ms")
        println("   Errors: ${parseResult.errors.size}")
        println("   Warnings: ${parseResult.warnings.size}")
        
        // Serialize with INLINE_ONLY mode
        val startSerialize = System.currentTimeMillis()
        val actualOutput = serializer.serialize(
            parseResult.document,
            org.markup.poet.tck.serialization.AstJsonSerializer.Mode.INLINE_ONLY
        )
        val serializeDuration = System.currentTimeMillis() - startSerialize
        
        println("\n📤 Serialization:")
        println("   Mode: INLINE_ONLY")
        println("   Duration: ${serializeDuration}ms")
        println("   Output length: ${actualOutput.length} chars")
        
        // Compare
        val startCompare = System.currentTimeMillis()
        val result = JsonComparator.compare(expectedOutput, actualOutput)
        val compareDuration = System.currentTimeMillis() - startCompare
        
        println("\n🔍 Comparison:")
        println("   Duration: ${compareDuration}ms")
        
        when (result) {
            is org.markup.poet.tck.execution.ValidationResult.Success -> {
                println("   Result: ✅ PASSED")
            }
            is org.markup.poet.tck.execution.ValidationResult.Failure -> {
                println("   Result: ❌ FAILED")
                println("   Error: ${result.message}")
                
                println("\n📋 Expected Output (first 200 chars):")
                println(expectedOutput.take(200))
                
                println("\n📋 Actual Output (first 200 chars):")
                println(actualOutput.take(200))
            }
        }
        
        val totalDuration = parseDuration + serializeDuration + compareDuration
        println("\n⏱️  Total Duration: ${totalDuration}ms")
        println("=".repeat(60))
    }
    
    @Test
    fun `should run single official test - single line paragraph`() {
        println("\n" + "=".repeat(60))
        println("Testing Official TCK Test: Single Line Paragraph")
        println("=".repeat(60))
        
        val testPath = "tck-quality-testing/official-tck/repository/tests/block/paragraph"
        val inputFile = File("$testPath/single-line-input.adoc")
        val outputFile = File("$testPath/single-line-output.json")
        
        if (!inputFile.exists()) {
            println("⚠️  Test files not found.")
            return
        }
        
        println("\n📝 Test: block/paragraph/single-line")
        
        val input = inputFile.readText()
        val expectedOutput = outputFile.readText()
        
        println("📥 Input: ${input.take(50)}")
        
        // Parse
        val parseResult = parser.parseToAsg(input)
        println("⚙️  Parsed: ${parseResult.errors.size} errors")
        
        // Serialize
        val actualOutput = serializer.serialize(parseResult.document)
        println("📤 Serialized: ${actualOutput.length} chars")
        
        // Compare
        val result = JsonComparator.compare(expectedOutput, actualOutput)
        
        when (result) {
            is org.markup.poet.tck.execution.ValidationResult.Success -> {
                println("✅ PASSED")
            }
            is org.markup.poet.tck.execution.ValidationResult.Failure -> {
                println("❌ FAILED: ${result.message}")
            }
        }
        
        println("=".repeat(60))
    }
    
    @Test
    fun `should list all official tests`() {
        println("\n" + "=".repeat(60))
        println("Official TCK Test Inventory")
        println("=".repeat(60))
        
        val testsDir = File("tck-quality-testing/official-tck/repository/tests")
        
        if (!testsDir.exists()) {
            println("⚠️  Official TCK not synced")
            return
        }
        
        val inputFiles = testsDir.walkTopDown()
            .filter { it.name.endsWith("-input.adoc") }
            .toList()
        
        println("\n📊 Found ${inputFiles.size} official tests:\n")
        
        inputFiles.sortedBy { it.path }.forEach { file ->
            val relativePath = file.path.removePrefix("tck-quality-testing/official-tck/repository/tests/")
            val testName = relativePath.removeSuffix("-input.adoc")
            println("   - $testName")
        }
        
        println("\n" + "=".repeat(60))
    }
}
