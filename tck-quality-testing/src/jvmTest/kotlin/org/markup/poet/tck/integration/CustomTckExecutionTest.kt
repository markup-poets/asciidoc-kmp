package org.markup.poet.tck.integration

import org.markup.poet.tck.TckIntegration
import org.markup.poet.tck.execution.CategoryFilter
import org.markup.poet.tck.fixtures.FixtureCategory
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

/**
 * Test your real implementation against custom TCK fixtures.
 * 
 * This runs your actual parser and serializer against the test fixtures
 * to see how many tests pass.
 */
class CustomTckExecutionTest {
    
    @Test
    fun `should run custom TCK tests with real implementation`() {
        println("\n" + "=".repeat(60))
        println("Running Custom TCK Tests with Real Implementation")
        println("=".repeat(60))
        
        // Initialize TCK with your real parser and serializer
        val context = TckIntegration.initialize()
        
        // Run all tests
        val results = TckIntegration.runTests(context)
        
        assertNotNull(results)
        
        // Print results
        println("\n📊 Overall Results:")
        println("   Total tests: ${results.totalTests}")
        println("   Passed: ${results.passed} (${if (results.totalTests > 0) results.passed * 100 / results.totalTests else 0}%)")
        println("   Failed: ${results.failed}")
        println("   Pending: ${results.pending}")
        println("   Skipped: ${results.skipped}")
        println("   Errors: ${results.errors}")
        
        // Show breakdown by category
        if (results.byCategory.isNotEmpty()) {
            println("\n📂 By Category:")
            results.byCategory.entries
                .sortedByDescending { it.value.total }
                .forEach { (category, categoryResults) ->
                    val categoryPassRate = if (categoryResults.total > 0) 
                        categoryResults.passed * 100 / categoryResults.total 
                    else 0
                    println("   ${category.name}: ${categoryResults.passed}/${categoryResults.total} ($categoryPassRate%)")
                }
        }
        
        // Show failed tests (first 10)
        if (results.failedTests.isNotEmpty()) {
            println("\n❌ Failed Tests (showing first 10):")
            results.failedTests.take(10).forEach { test ->
                println("   - ${test.fixtureId}")
                if (test.errorMessage != null) {
                    println("     Error: ${test.errorMessage}")
                }
            }
            if (results.failedTests.size > 10) {
                println("   ... and ${results.failedTests.size - 10} more")
            }
        }
        
        // Show pending tests (first 5)
        if (results.pendingTests.isNotEmpty()) {
            println("\n⏳ Pending Tests (showing first 5):")
            results.pendingTests.take(5).forEach { test ->
                println("   - ${test.fixtureId}")
            }
            if (results.pendingTests.size > 5) {
                println("   ... and ${results.pendingTests.size - 5} more")
            }
        }
        
        println("\n" + "=".repeat(60))
        
        // We expect some tests to run
        assertTrue(results.totalTests > 0, "Should have loaded test fixtures")
    }
    
    @Test
    fun `should test paragraph parsing`() {
        val context = TckIntegration.initialize()
        
        // Filter to only paragraph tests
        val filter = CategoryFilter(setOf(FixtureCategory.BLOCK_PARAGRAPH))
        val results = TckIntegration.runTests(context, filter)
        
        println("\n📝 Paragraph Tests:")
        println("   Total: ${results.totalTests}")
        println("   Passed: ${results.passed}")
        println("   Failed: ${results.failed}")
        
        if (results.failedTests.isNotEmpty()) {
            println("\n   Failed tests:")
            results.failedTests.forEach { test ->
                println("     - ${test.fixtureId}: ${test.errorMessage}")
            }
        }
    }
    
    @Test
    fun `should test heading parsing`() {
        val context = TckIntegration.initialize()
        
        // Filter to only heading tests
        val filter = CategoryFilter(setOf(FixtureCategory.BLOCK_HEADING))
        val results = TckIntegration.runTests(context, filter)
        
        println("\n📑 Heading Tests:")
        println("   Total: ${results.totalTests}")
        println("   Passed: ${results.passed}")
        println("   Failed: ${results.failed}")
    }
    
    @Test
    fun `should test list parsing`() {
        val context = TckIntegration.initialize()
        
        // Filter to list tests
        val filter = CategoryFilter(setOf(
            FixtureCategory.BLOCK_LIST
        ))
        val results = TckIntegration.runTests(context, filter)
        
        println("\n📋 List Tests:")
        println("   Total: ${results.totalTests}")
        println("   Passed: ${results.passed}")
        println("   Failed: ${results.failed}")
    }
    
    @Test
    fun `should generate conformance report`() {
        val context = TckIntegration.initialize()
        val results = TckIntegration.runTests(context)
        
        // Generate report
        val report = TckIntegration.generateReport(context, results)
        
        assertNotNull(report)
        
        println("\n📄 Conformance Report Generated:")
        println("   Generated at: ${report.metadata.generatedAt}")
        println("   Spec version: ${report.metadata.specVersion}")
        println("   Library version: ${report.metadata.libraryVersion}")
        println("   Total tests: ${report.summary.totalTests}")
        println("   Pass rate: ${String.format("%.1f%%", report.summary.overallPassRate * 100)}")
        println("   Certification ready: ${report.certificationStatus.isReady}")
        
        if (!report.certificationStatus.isReady) {
            println("\n   Blocking issues: ${report.certificationStatus.blockingIssues.size}")
            report.certificationStatus.blockingIssues.take(3).forEach { issue ->
                println("     - [${issue.severity}] ${issue.description}")
            }
        }
    }
}
