package org.markup.poet.tck.integration

import org.markup.poet.tck.TckIntegration
import kotlin.test.Test

/**
 * Debug test to check if fixture loading hangs.
 */
class DebugFixtureLoadingTest {
    
    @Test
    fun `load all fixtures without running`() {
        println("\n🔍 Testing fixture loading")
        
        println("1. Initializing context...")
        val context = TckIntegration.initialize()
        println("✅ Context initialized")
        
        println("\n2. Loading all fixtures...")
        val allFixtures = context.fixtureLoader.loadAllFixtures()
        println("✅ Loaded ${allFixtures.size} fixtures")
        
        println("\n3. Filtering official fixtures...")
        val officialFixtures = allFixtures.filter { it.metadata["source"] == "official" }
        println("✅ Found ${officialFixtures.size} official fixtures")
        
        println("\n4. Listing official fixtures:")
        officialFixtures.forEach { fixture ->
            println("   - ${fixture.id}")
            println("     Input length: ${fixture.input.length}")
            println("     Has expected output: ${fixture.expectedOutput != null}")
        }
        
        println("\n✅ Test complete - no hang!")
    }
}
