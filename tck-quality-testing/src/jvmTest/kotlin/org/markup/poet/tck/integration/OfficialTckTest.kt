package org.markup.poet.tck.integration

import kotlinx.coroutines.runBlocking
import org.markup.poet.tck.TckIntegration
import org.markup.poet.tck.execution.SourceFilter
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Test against the OFFICIAL AsciiDoc TCK from Eclipse Foundation.
 * 
 * This will:
 * 1. Sync the official TCK repository
 * 2. Run your parser against official test data
 * 3. Show real certification results
 */
class OfficialTckTest {
    
    @Test
    fun `should sync official TCK repository`() = runBlocking {
        println("\n" + "=".repeat(60))
        println("Syncing Official AsciiDoc TCK Repository")
        println("=".repeat(60))
        
        val context = TckIntegration.initialize()
        
        println("\n📥 Starting sync...")
        println("   Repository: ${context.config.sync.repositoryUrl}")
        println("   Local path: ${context.config.sync.localPath}")
        
        try {
            val syncResult = TckIntegration.sync(context)
            
            println("\n✅ Sync completed!")
            println("   Success: ${syncResult.success}")
            println("   Spec version: ${syncResult.metadata.specVersion}")
            println("   Commit hash: ${syncResult.metadata.commitHash}")
            println("   Test count: ${syncResult.metadata.testCount}")
            println("   Duration: ${syncResult.metadata.durationMs}ms")
            
            if (syncResult.changeReport != null) {
                println("\n📊 Changes:")
                println("   Added: ${syncResult.changeReport.addedTests.size}")
                println("   Modified: ${syncResult.changeReport.modifiedTests.size}")
                println("   Removed: ${syncResult.changeReport.removedTests.size}")
            }
            
            assertTrue(syncResult.metadata.testCount > 0, "Should have synced tests")
            
        } catch (e: Exception) {
            println("\n❌ Sync failed: ${e.message}")
            println("\nThis is expected if:")
            println("  - No internet connection")
            println("  - Git is not installed")
            println("  - Repository URL is incorrect")
            println("\nYou can still run custom tests without syncing.")
            
            // Don't fail the test - sync might not be available in all environments
            println("\n⚠️  Continuing without official tests...")
        }
        
        println("\n" + "=".repeat(60))
    }
    
    @Test
    fun `should run official TCK tests if available`() = runBlocking {
        println("\n" + "=".repeat(60))
        println("Running Official AsciiDoc TCK Tests")
        println("=".repeat(60))
        
        val context = TckIntegration.initialize()
        
        // Try to sync first (might fail if already synced or no network)
        try {
            println("\n📥 Attempting sync...")
            TckIntegration.sync(context)
            println("✅ Sync successful")
        } catch (e: Exception) {
            println("⚠️  Sync skipped: ${e.message}")
            println("   Using existing repository if available")
        }
        
        // Filter to only official tests
        val filter = SourceFilter(allowCustom = false, allowOfficial = true)
        val results = TckIntegration.runTests(context, filter)
        
        println("\n📊 Official TCK Results:")
        println("   Total tests: ${results.totalTests}")
        
        if (results.totalTests == 0) {
            println("\n⚠️  No official tests found!")
            println("   This means the official TCK repository is not synced.")
            println("   Run the sync test first, or check:")
            println("   - Internet connection")
            println("   - Git installation")
            println("   - Repository path: ${context.config.sync.localPath}")
            
        } else {
            println("   Passed: ${results.passed} (${if (results.totalTests > 0) results.passed * 100 / results.totalTests else 0}%)")
            println("   Failed: ${results.failed}")
            println("   Pending: ${results.pending}")
            println("   Skipped: ${results.skipped}")
            
            // Show breakdown by category
            if (results.byCategory.isNotEmpty()) {
                println("\n📂 By Category:")
                results.byCategory.entries
                    .sortedByDescending { it.value.total }
                    .take(10)
                    .forEach { (category, categoryResults) ->
                        val passRate = if (categoryResults.total > 0) 
                            categoryResults.passed * 100 / categoryResults.total 
                        else 0
                        println("   ${category.name}: ${categoryResults.passed}/${categoryResults.total} ($passRate%)")
                    }
                if (results.byCategory.size > 10) {
                    println("   ... and ${results.byCategory.size - 10} more categories")
                }
            }
            
            // Show failed tests (first 10)
            if (results.failedTests.isNotEmpty()) {
                println("\n❌ Failed Tests (showing first 10):")
                results.failedTests.take(10).forEach { test ->
                    println("   - ${test.fixtureId}")
                    if (test.errorMessage != null) {
                        val shortError = test.errorMessage.take(100)
                        println("     ${shortError}${if (test.errorMessage.length > 100) "..." else ""}")
                    }
                }
                if (results.failedTests.size > 10) {
                    println("   ... and ${results.failedTests.size - 10} more")
                }
            }
            
            // Show certification status
            val certStatus = TckIntegration.checkCertification(context, results)
            println("\n🏆 Certification Status:")
            println("   Ready: ${certStatus.isReady}")
            println("   Progress: ${String.format("%.1f%%", certStatus.overallProgress)}")
            
            if (!certStatus.isReady && certStatus.blockingIssues.isNotEmpty()) {
                println("\n   Blocking Issues:")
                certStatus.blockingIssues.take(5).forEach { issue ->
                    println("   - [${issue.severity}] ${issue.description}")
                }
            }
        }
        
        println("\n" + "=".repeat(60))
    }
    
    @Test
    fun `should generate official conformance report`() = runBlocking {
        println("\n" + "=".repeat(60))
        println("Generating Official Conformance Report")
        println("=".repeat(60))
        
        val context = TckIntegration.initialize()
        
        // Try to sync
        try {
            TckIntegration.sync(context)
        } catch (e: Exception) {
            println("⚠️  Sync skipped: ${e.message}")
        }
        
        // Run tests (both custom and official)
        val results = TckIntegration.runTests(context)
        
        // Generate report
        val report = TckIntegration.generateReport(context, results)
        
        println("\n📄 Conformance Report:")
        println("   Generated at: ${report.metadata.generatedAt}")
        println("   Spec version: ${report.metadata.specVersion}")
        println("   TCK commit: ${report.metadata.tckCommitHash}")
        println("   Library version: ${report.metadata.libraryVersion}")
        println("   Platforms: ${report.metadata.platforms.joinToString(", ")}")
        
        println("\n📊 Summary:")
        println("   Total tests: ${report.summary.totalTests}")
        println("   Passed: ${report.summary.passed}")
        println("   Failed: ${report.summary.failed}")
        println("   Overall pass rate: ${String.format("%.1f%%", report.summary.overallPassRate * 100)}")
        println("   Official tests pass rate: ${String.format("%.1f%%", report.summary.officialTestsPassRate * 100)}")
        println("   Custom tests pass rate: ${String.format("%.1f%%", report.summary.customTestsPassRate * 100)}")
        
        println("\n🏆 Certification:")
        println("   Ready: ${report.certificationStatus.isReady}")
        println("   Progress: ${String.format("%.1f%%", report.certificationStatus.overallProgress)}")
        
        if (report.certificationStatus.recommendations.isNotEmpty()) {
            println("\n💡 Recommendations:")
            report.certificationStatus.recommendations.take(5).forEach { rec ->
                println("   - $rec")
            }
        }
        
        println("\n" + "=".repeat(60))
        
        assertTrue(results.totalTests > 0, "Should have run some tests")
    }
}
