package org.markup.poet.tck.integration

import org.markup.poet.tck.TckIntegration
import kotlin.test.Test

/**
 * Run each official test individually to identify which one hangs.
 */
class IdentifyHangingTest {
    
    @Test
    fun `test 1 - inline-no-markup-single-word`() {
        runSingleTest("inline-no-markup-single-word")
    }
    
    @Test
    fun `test 2 - inline-span-strong-constrained-single-char`() {
        runSingleTest("inline-span-strong-constrained-single-char")
    }
    
    @Test
    fun `test 3 - block-sidebar-containing-unordered-list`() {
        runSingleTest("block-sidebar-containing-unordered-list")
    }
    
    @Test
    fun `test 4 - block-section-title-body`() {
        runSingleTest("block-section-title-body")
    }
    
    @Test
    fun `test 5 - block-paragraph-paragraph-empty-lines-paragraph`() {
        runSingleTest("block-paragraph-paragraph-empty-lines-paragraph")
    }
    
    @Test
    fun `test 6 - block-paragraph-single-line`() {
        runSingleTest("block-paragraph-single-line")
    }
    
    @Test
    fun `test 7 - block-paragraph-sibling-paragraphs`() {
        runSingleTest("block-paragraph-sibling-paragraphs")
    }
    
    @Test
    fun `test 8 - block-paragraph-multiple-lines`() {
        runSingleTest("block-paragraph-multiple-lines")
    }
    
    @Test
    fun `test 9 - block-document-body-only`() {
        runSingleTest("block-document-body-only")
    }
    
    @Test
    fun `test 10 - block-document-header-body`() {
        runSingleTest("block-document-header-body")
    }
    
    private fun runSingleTest(testId: String) {
        println("\n🧪 Testing: $testId")
        
        val context = TckIntegration.initialize()
        val allFixtures = context.fixtureLoader.loadAllFixtures()
        val fixture = allFixtures.find { it.id == testId }
        
        if (fixture == null) {
            println("❌ Test not found: $testId")
            return
        }
        
        println("   Input length: ${fixture.input.length} chars")
        println("   Input preview: ${fixture.input.take(80).replace("\n", "\\n")}")
        
        try {
            val result = context.testRunner.runTest(fixture)
            println("   ✅ Result: ${result.status} (${result.durationMs}ms)")
            
            if (result.errorMessage != null) {
                println("   Error: ${result.errorMessage.lines().first()}")
            }
        } catch (e: Exception) {
            println("   💥 Exception: ${e.message}")
            e.printStackTrace()
        }
    }
}
