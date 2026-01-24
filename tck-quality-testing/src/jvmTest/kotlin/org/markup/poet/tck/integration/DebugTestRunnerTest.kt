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
