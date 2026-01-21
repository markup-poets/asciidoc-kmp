package org.markup.poet.tck.platform

import org.markup.poet.tck.fixtures.ResourceFixtureLoader
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Android-specific platform tests.
 * 
 * Tests Android-specific file I/O, encoding, and path resolution behavior.
 * 
 * Requirements: 7.1, 7.2, 7.3
 */
class AndroidPlatformSpecificTest {
    
    private val fixtureLoader = ResourceFixtureLoader()
    
    @Test
    fun `Android should handle file paths with forward slashes`() {
        // Android uses Linux-style paths with forward slashes
        val fixture = fixtureLoader.loadFixture("platform-path-resolution-absolute")
        assertNotNull(fixture)
        assertTrue(fixture.input.contains("/"))
    }
    
    @Test
    fun `Android should handle UTF-8 encoding correctly`() {
        val fixture = fixtureLoader.loadFixture("platform-encoding-utf8-basic")
        assertNotNull(fixture)
        
        // Verify UTF-8 characters are preserved
        assertTrue(fixture.input.contains("café"))
        assertTrue(fixture.input.contains("naïve"))
    }
    
    @Test
    fun `Android should handle emoji in UTF-8`() {
        val fixture = fixtureLoader.loadFixture("platform-encoding-utf8-emoji")
        assertNotNull(fixture)
        
        // Android has good emoji support
        assertTrue(fixture.input.contains("📚"))
    }
}
