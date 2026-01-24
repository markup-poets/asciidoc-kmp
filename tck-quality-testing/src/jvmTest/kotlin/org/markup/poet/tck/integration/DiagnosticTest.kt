package org.markup.poet.tck.integration

import org.markup.poet.tck.TckIntegration
import org.markup.poet.tck.execution.SourceFilter
import kotlin.test.Test

/**
 * Diagnostic test to check TCK fixture loading.
 */
class DiagnosticTest {
    
    @Test
    fun `should list all available fixtures`() {
        println("\n" + "=".repeat(60))
        println("TCK Fixture Diagnostic")
        println("=".repeat(60))
        
        val context = TckIntegration.initialize()
        
        println("\n📂 Loading fixtures...")
        val allFixtures = context.fixtureLoader.loadAllFixtures()
        
        println("\n📊 Fixture Summary:")
        println("   Total fixtures: ${allFixtures.size}")
        
        // Group by source
        val bySource = allFixtures.groupBy { it.metadata["source"] ?: "unknown" }
        println("\n   By Source:")
        bySource.forEach { (source, fixtures) ->
            println("      $source: ${fixtures.size} fixtures")
        }
        
        // Group by category
        val byCategory = allFixtures.groupBy { it.category }
        println("\n   By Category:")
        byCategory.forEach { (category, fixtures) ->
            println("      ${category.name}: ${fixtures.size} fixtures")
        }
        
        // List first 20 fixtures
        println("\n📝 First 20 Fixtures:")
        allFixtures.take(20).forEach { fixture ->
            val source = fixture.metadata["source"] ?: "unknown"
            println("   [$source] ${fixture.id}")
        }
        
        if (allFixtures.size > 20) {
            println("   ... and ${allFixtures.size - 20} more")
        }
        
        // Check official TCK specifically
        val officialFixtures = allFixtures.filter { 
            it.metadata["source"] == "official" 
        }
        
        println("\n🏛️  Official TCK Fixtures:")
        println("   Count: ${officialFixtures.size}")
        
        if (officialFixtures.isEmpty()) {
            println("\n   ⚠️  WARNING: No official TCK fixtures found!")
            println("   This could mean:")
            println("      - Repository not synced")
            println("      - Wrong repository path")
            println("      - Fixture loader not configured correctly")
            println("\n   Repository path: ${context.config.sync.localPath}")
            println("   Check if directory exists and contains tests/")
        } else {
            println("\n   ✅ Official fixtures loaded successfully!")
            officialFixtures.take(10).forEach { fixture ->
                println("      - ${fixture.id}")
            }
        }
        
        println("\n" + "=".repeat(60))
    }
}
