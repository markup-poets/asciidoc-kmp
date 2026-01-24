package org.markup.poet.tck.integration

import org.markup.poet.tck.TckIntegration
import org.markup.poet.tck.execution.SourceFilter
import kotlin.test.Test

/**
 * Quick test to run official TCK with immediate progress feedback.
 */
class QuickOfficialTest {
    
    @Test
    fun `run official TCK with progress`() {
        println("\n" + "=".repeat(70))
        println("🚀 OFFICIAL TCK TEST EXECUTION")
        println("=".repeat(70))
        
        // Initialize
        print("\n[1/4] Initializing TCK context... ")
        val context = TckIntegration.initialize()
        println("✅ DONE")
        
        // Load fixtures
        print("[2/4] Loading official fixtures... ")
        val allFixtures = context.fixtureLoader.loadAllFixtures()
        val officialFixtures = allFixtures.filter { it.metadata["source"] == "official" }
        println("✅ DONE (${officialFixtures.size} tests)")
        
        // Show what we're testing
        println("\n[3/4] Official tests to run:")
        officialFixtures.forEachIndexed { index, fixture ->
            println("   ${index + 1}. ${fixture.id}")
        }
        
        // Run tests
        println("\n[4/4] Running tests...")
        println("=".repeat(70))
        
        val filter = SourceFilter(allowCustom = false, allowOfficial = true)
        val results = TckIntegration.runTests(context, filter)
        
        // Final summary
        println("\n" + "=".repeat(70))
        println("📊 FINAL RESULTS")
        println("=".repeat(70))
        println("Total:   ${results.totalTests}")
        println("Passed:  ${results.passed} ✅")
        println("Failed:  ${results.failed} ❌")
        println("Errors:  ${results.errors} 💥")
        println("Pending: ${results.pending} ⏸️")
        
        val passRate = if (results.totalTests > 0) {
            (results.passed * 100.0 / results.totalTests)
        } else 0.0
        
        println("\nPass Rate: %.1f%%".format(passRate))
        
        // Show failed tests
        if (results.failedTests.isNotEmpty()) {
            println("\n❌ Failed Tests:")
            results.failedTests.forEach { test ->
                println("\n   Test: ${test.fixtureId}")
                if (test.errorMessage != null) {
                    val lines = test.errorMessage.lines()
                    lines.take(3).forEach { line ->
                        println("   > $line")
                    }
                    if (lines.size > 3) {
                        println("   > ... (${lines.size - 3} more lines)")
                    }
                }
            }
        }
        
        println("\n" + "=".repeat(70))
    }
}
