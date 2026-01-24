package org.markup.poet.tck.official

import org.markup.poet.tck.TckIntegration
import org.markup.poet.tck.execution.CategoryFilter
import org.markup.poet.tck.execution.SourceFilter
import org.markup.poet.tck.fixtures.FixtureCategory
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

/**
 * Sample test demonstrating how to run official TCK tests.
 * 
 * This test class shows the complete workflow for:
 * 1. Initializing the TCK system
 * 2. Running official tests
 * 3. Generating conformance reports
 * 4. Checking certification readiness
 * 
 * **Note**: These tests will only pass when:
 * - Official TCK repository has been synced
 * - A real parser and renderer are provided
 * - The implementation passes the official tests
 * 
 * For now, these serve as examples and integration tests.
 */
class SampleOfficialTest {
    
    /**
     * Example: Initialize TCK system and verify components.
     */
    @Test
    fun `should initialize TCK system successfully`() {
        val context = TckIntegration.initialize()
        
        // Verify all components are available
        assertTrue(context.config.sync.repositoryUrl.isNotEmpty())
        assertTrue(context.config.execution.enableOfficialTests || context.config.execution.enableCustomTests)
        assertNotNull(context.syncService)
        assertNotNull(context.fixtureLoader)
        assertNotNull(context.testRunner)
    }
    
    /**
     * Example: Run all available tests (custom and official).
     * 
     * This demonstrates the simplest usage pattern.
     */
    @Test
    fun `example - run all tests`() {
        val context = TckIntegration.initialize()
        
        // Run all tests
        val results = TckIntegration.runTests(context)
        
        // Results will be empty if no fixtures are loaded
        // In a real scenario with synced TCK and fixtures:
        // - results.totalTests would be > 0
        // - results.passed would show passing tests
        // - results.failed would show failing tests
        
        println("Total tests: ${results.totalTests}")
        println("Passed: ${results.passed}")
        println("Failed: ${results.failed}")
        println("Pending: ${results.pending}")
    }
    
    /**
     * Example: Run only official TCK tests.
     * 
     * This demonstrates filtering by source.
     */
    @Test
    fun `example - run only official tests`() {
        val context = TckIntegration.initialize()
        
        // Filter to only official tests (disable custom tests)
        val officialFilter = SourceFilter(allowCustom = false, allowOfficial = true)
        val results = TckIntegration.runTests(context, officialFilter)
        
        println("Official tests: ${results.totalTests}")
        println("Pass rate: ${if (results.totalTests > 0) results.passed * 100.0 / results.totalTests else 0.0}%")
    }
    
    /**
     * Example: Run tests for a specific category.
     * 
     * This demonstrates filtering by category.
     */
    @Test
    fun `example - run paragraph tests only`() {
        val context = TckIntegration.initialize()
        
        // Filter to only paragraph tests
        val paragraphFilter = CategoryFilter(setOf(FixtureCategory.BLOCK_PARAGRAPH))
        val results = TckIntegration.runTests(context, paragraphFilter)
        
        println("Paragraph tests: ${results.totalTests}")
    }
    
    /**
     * Example: Generate conformance report.
     * 
     * This demonstrates report generation.
     */
    @Test
    fun `example - generate conformance report`() {
        val context = TckIntegration.initialize()
        val results = TckIntegration.runTests(context)
        
        // Generate report
        val report = TckIntegration.generateReport(context, results)
        
        // Report contains comprehensive information
        println("Report generated at: ${report.metadata.generatedAt}")
        println("Spec version: ${report.metadata.specVersion}")
        println("TCK commit: ${report.metadata.tckCommitHash}")
        println("Library version: ${report.metadata.libraryVersion}")
        println("Platforms: ${report.metadata.platforms}")
        
        // Summary statistics
        println("\nSummary:")
        println("  Total: ${report.summary.totalTests}")
        println("  Passed: ${report.summary.passed}")
        println("  Failed: ${report.summary.failed}")
        
        // Certification status
        println("\nCertification:")
        println("  Ready: ${report.certificationStatus.isReady}")
        println("  Progress: ${report.certificationStatus.overallProgress}")
        println("  Blocking issues: ${report.certificationStatus.blockingIssues.size}")
    }
    
    /**
     * Example: Check certification readiness.
     * 
     * This demonstrates certification checking.
     */
    @Test
    fun `example - check certification readiness`() {
        val context = TckIntegration.initialize()
        val results = TckIntegration.runTests(context)
        
        // Check certification status
        val status = TckIntegration.checkCertification(context, results)
        
        println("Certification Status:")
        println("  Ready: ${status.isReady}")
        println("  Progress: ${status.overallProgress}%")
        
        if (!status.isReady) {
            println("\nBlocking Issues:")
            status.blockingIssues.forEach { issue ->
                println("  - [${issue.severity}] ${issue.description}")
            }
            
            println("\nRecommendations:")
            status.recommendations.forEach { recommendation ->
                println("  - $recommendation")
            }
        }
    }
    
    /**
     * Example: Complete workflow.
     * 
     * This demonstrates the end-to-end workflow in one call.
     * 
     * **Note**: This is a suspend function and requires coroutine support.
     * In a real test, you would use runBlocking or similar.
     */
    @Test
    fun `example - complete workflow`() {
        // This would be:
        // runBlocking {
        //     val context = TckIntegration.initialize()
        //     val report = TckIntegration.runCompleteWorkflow(context)
        //     
        //     println("Complete workflow executed")
        //     println("Total tests: ${report.summary.totalTests}")
        //     println("Pass rate: ${report.summary.passRate}")
        //     println("Certification ready: ${report.certificationStatus.isReady}")
        // }
        
        // For now, just demonstrate the API exists
        val context = TckIntegration.initialize()
        assertTrue(context.config.sync.repositoryUrl.isNotEmpty())
    }
    
    /**
     * Example: Custom configuration.
     * 
     * This demonstrates using custom configuration.
     */
    @Test
    fun `example - custom configuration`() {
        // Initialize with default config
        val context = TckIntegration.initialize()
        
        // Modify configuration
        val customConfig = context.config.copy(
            execution = context.config.execution.copy(
                enableOfficialTests = true,
                enableCustomTests = false,
                parallelExecution = true
            )
        )
        
        // Create new context with custom config
        val customContext = context.withConfig(customConfig)
        
        // Run tests with custom config
        val results = TckIntegration.runTests(customContext)
        
        println("Tests run with custom configuration")
        println("Official tests only: ${customConfig.execution.enableOfficialTests && !customConfig.execution.enableCustomTests}")
    }
    
    /**
     * Example: Platform-specific results.
     * 
     * This demonstrates accessing platform-specific results.
     */
    @Test
    fun `example - platform specific results`() {
        val context = TckIntegration.initialize()
        val results = TckIntegration.runTests(context)
        
        // Access platform-specific results
        results.byPlatform.forEach { (platform, platformResults) ->
            println("Platform: $platform")
            println("  Total: ${platformResults.total}")
            println("  Passed: ${platformResults.passed}")
            println("  Failed: ${platformResults.failed}")
        }
    }
    
    /**
     * Example: Category-specific results.
     * 
     * This demonstrates accessing category-specific results.
     */
    @Test
    fun `example - category specific results`() {
        val context = TckIntegration.initialize()
        val results = TckIntegration.runTests(context)
        
        // Access category-specific results
        results.byCategory.forEach { (category, categoryResults) ->
            println("Category: ${category.name}")
            println("  Total: ${categoryResults.total}")
            println("  Passed: ${categoryResults.passed}")
        }
    }
}
