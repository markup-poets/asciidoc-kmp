package org.markup.poet.tck.integration

import org.markup.poet.tck.TckIntegration
import kotlin.test.Test

/**
 * Run just the first 3 official tests to identify issues.
 */
class ThreeTestsOnly {
    
    @Test
    fun `run first 3 official tests`() {
        println("\n🧪 Running first 3 official tests")
        
        val context = TckIntegration.initialize()
        println("✅ Context initialized")
        
        val allFixtures = context.fixtureLoader.loadAllFixtures()
        println("✅ Loaded ${allFixtures.size} total fixtures")
        
        val officialFixtures = allFixtures.filter { it.metadata["source"] == "official" }
        println("✅ Found ${officialFixtures.size} official fixtures")
        
        val firstThree = officialFixtures.take(3)
        println("\nRunning ${firstThree.size} tests:\n")
        
        firstThree.forEachIndexed { index, fixture ->
            println("[${index + 1}/3] Testing: ${fixture.id}")
            println("   Input: ${fixture.input.take(50)}...")
            
            try {
                val result = context.testRunner.runTest(fixture)
                println("   Result: ${result.status} (${result.durationMs}ms)")
                
                if (result.errorMessage != null) {
                    println("   Error: ${result.errorMessage.take(100)}")
                }
            } catch (e: Exception) {
                println("   Exception: ${e.message}")
                e.printStackTrace()
            }
            
            println()
        }
        
        println("✅ Test run complete")
    }
}
