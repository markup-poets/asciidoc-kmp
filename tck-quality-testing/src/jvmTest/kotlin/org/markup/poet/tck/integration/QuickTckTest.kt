package org.markup.poet.tck.integration

import org.markup.poet.tck.TckIntegration
import org.markup.poet.tck.execution.CategoryFilter
import org.markup.poet.tck.fixtures.FixtureCategory
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Quick test to run a small subset of TCK tests and see results.
 */
class QuickTckTest {
    
    @Test
    fun `should run paragraph tests only`() {
        println("\n" + "=".repeat(60))
        println("Running Paragraph Tests with Real Implementation")
        println("=".repeat(60))
        
        val context = TckIntegration.initialize()
        
        // Filter to only paragraph tests
        val filter = CategoryFilter(setOf(FixtureCategory.BLOCK_PARAGRAPH))
        val results = TckIntegration.runTests(context, filter)
        
        println("\n📊 Paragraph Test Results:")
        println("   Total: ${results.totalTests}")
        println("   Passed: ${results.passed}")
        println("   Failed: ${results.failed}")
        println("   Pending: ${results.pending}")
        
        if (results.failed > 0) {
            println("\n❌ Failed Tests:")
            results.failedTests.take(5).forEach { test ->
                println("   - ${test.fixtureId}")
                if (test.errorMessage != null) {
                    println("     ${test.errorMessage}")
                }
            }
        }
        
        println("\n" + "=".repeat(60))
        
        assertTrue(results.totalTests > 0, "Should have paragraph tests")
    }
    
    @Test
    fun `should run heading tests only`() {
        println("\n" + "=".repeat(60))
        println("Running Heading Tests with Real Implementation")
        println("=".repeat(60))
        
        val context = TckIntegration.initialize()
        
        // Filter to only heading tests
        val filter = CategoryFilter(setOf(FixtureCategory.BLOCK_HEADING))
        val results = TckIntegration.runTests(context, filter)
        
        println("\n📊 Heading Test Results:")
        println("   Total: ${results.totalTests}")
        println("   Passed: ${results.passed}")
        println("   Failed: ${results.failed}")
        println("   Pending: ${results.pending}")
        
        if (results.failed > 0) {
            println("\n❌ Failed Tests:")
            results.failedTests.take(5).forEach { test ->
                println("   - ${test.fixtureId}")
                if (test.errorMessage != null) {
                    println("     ${test.errorMessage}")
                }
            }
        }
        
        println("\n" + "=".repeat(60))
        
        assertTrue(results.totalTests > 0, "Should have heading tests")
    }
    
    @Test
    fun `should show overall statistics`() {
        println("\n" + "=".repeat(60))
        println("Overall TCK Statistics")
        println("=".repeat(60))
        
        val context = TckIntegration.initialize()
        val results = TckIntegration.runTests(context)
        
        println("\n📊 Overall Results:")
        println("   Total tests: ${results.totalTests}")
        println("   Passed: ${results.passed} (${if (results.totalTests > 0) results.passed * 100 / results.totalTests else 0}%)")
        println("   Failed: ${results.failed}")
        println("   Pending: ${results.pending}")
        println("   Skipped: ${results.skipped}")
        
        if (results.byCategory.isNotEmpty()) {
            println("\n📂 By Category:")
            results.byCategory.entries
                .sortedByDescending { it.value.total }
                .forEach { (category, categoryResults) ->
                    val passRate = if (categoryResults.total > 0) 
                        categoryResults.passed * 100 / categoryResults.total 
                    else 0
                    println("   ${category.name}: ${categoryResults.passed}/${categoryResults.total} ($passRate%)")
                }
        }
        
        println("\n" + "=".repeat(60))
        
        assertTrue(results.totalTests > 0, "Should have loaded tests")
    }
}
