package org.markup.poet.tck.integration

import org.markup.poet.tck.TckIntegration
import kotlin.test.Test

/**
 * Debug test to check if test runner hangs on second test.
 */
class DebugTestRunnerTest {
    
    @Test
    fun `run test runner on second fixture`() {
        println("\n🔍 Testing test runner on second fixture")
        
        val context = TckIntegration.initialize()
        val allFixtures = context.fixtureLoader.loadAllFixtures()
        val officialFixtures = allFixtures.filter { it.metadata["source"] == "official" }

        if (officialFixtures.size < 2) {
            // The official TCK clone is gitignored and synced on demand; without it
            // there is nothing to run here. Conformance is gated by the dedicated
            // official-tck CI job (Node harness), not this debug test.
            println("⚠️  Official TCK repository not synced — skipping")
            return
        }

        val secondFixture = officialFixtures[1]  // The strong test
        
        println("Test: ${secondFixture.id}")
        println("Input: '${secondFixture.input}'")
        println("Input length: ${secondFixture.input.length}")
        
        println("\nCalling test runner...")
        
        try {
            val result = context.testRunner.runTest(secondFixture)
            
            println("✅ Test runner completed!")
            println("Status: ${result.status}")
            println("Duration: ${result.durationMs}ms")
            
            if (result.errorMessage != null) {
                println("Error: ${result.errorMessage}")
            }
        } catch (e: Exception) {
            println("💥 Exception: ${e.message}")
            e.printStackTrace()
        }
        
        println("\n✅ Test complete")
    }
}
