package org.markup.poet.tck.integration

import org.markup.poet.tck.TckIntegration
import kotlin.test.Test

/**
 * Step-by-step test to identify where the hang occurs.
 */
class StepByStepTest {
    
    @Test
    fun `step 1 - initialize context`() {
        println("\n🔧 STEP 1: Initialize context")
        println("Starting...")
        
        val context = TckIntegration.initialize()
        
        println("✅ Context initialized successfully")
        println("   Config path: ${context.config.sync.localPath}")
        println("   Enable official: ${context.config.execution.enableOfficialTests}")
        println("   Enable custom: ${context.config.execution.enableCustomTests}")
    }
    
    @Test
    fun `step 2 - load fixtures`() {
        println("\n📂 STEP 2: Load fixtures")
        println("Initializing context...")
        
        val context = TckIntegration.initialize()
        println("✅ Context ready")
        
        println("\nLoading fixtures...")
        val fixtures = context.fixtureLoader.loadAllFixtures()
        
        println("✅ Fixtures loaded: ${fixtures.size}")
        
        val official = fixtures.filter { it.metadata["source"] == "official" }
        val custom = fixtures.filter { it.metadata["source"] != "official" }
        
        println("   Official: ${official.size}")
        println("   Custom: ${custom.size}")
    }
    
    @Test
    fun `step 3 - run single test`() {
        println("\n🧪 STEP 3: Run single test")
        println("Initializing...")
        
        val context = TckIntegration.initialize()
        println("✅ Context ready")
        
        println("\nLoading fixtures...")
        val fixtures = context.fixtureLoader.loadAllFixtures()
        val official = fixtures.filter { it.metadata["source"] == "official" }
        
        if (official.isEmpty()) {
            println("❌ No official fixtures found!")
            return
        }
        
        println("✅ Found ${official.size} official fixtures")
        
        val firstTest = official.first()
        println("\nRunning first test: ${firstTest.id}")
        println("   Input length: ${firstTest.input.length} chars")
        println("   Has expected output: ${firstTest.expectedOutput != null}")
        
        println("\nExecuting test...")
        val result = context.testRunner.runTest(firstTest)
        
        println("✅ Test completed!")
        println("   Status: ${result.status}")
        println("   Duration: ${result.durationMs}ms")
        
        if (result.errorMessage != null) {
            println("   Error: ${result.errorMessage.take(200)}")
        }
    }
}
