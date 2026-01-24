package org.markup.poet.tck.integration

import org.markup.poet.tck.TckIntegration
import kotlin.test.Test

/**
 * Simple test to run all 13 official TCK tests.
 */
class SimpleOfficialTest {
    
    @Test
    fun `run all official tests`() {
        println("\n" + "=".repeat(70))
        println("🚀 RUNNING ALL 13 OFFICIAL TCK TESTS")
        println("=".repeat(70))
        
        val context = TckIntegration.initialize()
        val allFixtures = context.fixtureLoader.loadAllFixtures()
        val officialFixtures = allFixtures.filter { it.metadata["source"] == "official" }
        
        println("\nFound ${officialFixtures.size} official tests")
        println("\nRunning tests (this may take a minute)...\n")
        
        var passed = 0
        var failed = 0
        var errors = 0
        
        officialFixtures.forEachIndexed { index, fixture ->
            print("[${index + 1}/${officialFixtures.size}] ${fixture.id}... ")
            
            try {
                val result = context.testRunner.runTest(fixture)
                
                when (result.status.name) {
                    "PASSED" -> {
                        passed++
                        println("✅ PASS (${result.durationMs}ms)")
                    }
                    "FAILED" -> {
                        failed++
                        println("❌ FAIL")
                        if (result.errorMessage != null) {
                            println("    ${result.errorMessage.lines().first()}")
                        }
                    }
                    else -> {
                        errors++
                        println("💥 ERROR")
                        if (result.errorMessage != null) {
                            println("    ${result.errorMessage.lines().first()}")
                        }
                    }
                }
            } catch (e: Exception) {
                errors++
                println("💥 ERROR: ${e.message}")
            }
        }
        
        println("\n" + "=".repeat(70))
        println("📊 FINAL RESULTS")
        println("=".repeat(70))
        println("Total:   ${officialFixtures.size}")
        println("Passed:  $passed ✅")
        println("Failed:  $failed ❌")
        println("Errors:  $errors 💥")
        
        val passRate = if (officialFixtures.size > 0) {
            (passed * 100.0 / officialFixtures.size)
        } else 0.0
        
        println("\nPass Rate: %.1f%%".format(passRate))
        println("=".repeat(70))
    }
}
