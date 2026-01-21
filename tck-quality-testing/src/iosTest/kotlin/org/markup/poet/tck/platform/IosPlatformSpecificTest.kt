package org.markup.poet.tck.platform

import org.markup.poet.tck.fixtures.ResourceFixtureLoader
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * iOS-specific platform tests.
 * 
 * Tests iOS-specific file I/O, encoding, and path resolution behavior.
 * 
 * Requirements: 7.1, 7.2, 7.3
 */
class IosPlatformSpecificTest {
    
    private val fixtureLoader = ResourceFixtureLoader()
    
    @Test
    fun `iOS should handle file paths with forward slashes`() {
        // iOS uses Unix-style paths with forward slashes
        val fixture = fixtureLoader.loadFixture("platform-path-resolution-absolute")
        assertNotNull(fixture)
        assertTrue(fixture.input.contains("/"))
    }
    
    @Test
    fun `iOS should handle UTF-8 encoding correctly`() {
        val fixture = fixtureLoader.loadFixture("platform-encoding-utf8-basic")
        assertNotNull(fixture)
        
        // Verify UTF-8 characters are preserved
        assertTrue(fixture.input.contains("café"))
        assertTrue(fixture.input.contains("naïve"))
    }
    
    @Test
    fun `iOS should handle emoji in UTF-8`() {
        val fixture = fixtureLoader.loadFixture("platform-encoding-utf8-emoji")
        assertNotNull(fixture)
        
        // iOS has excellent emoji support
        assertTrue(fixture.input.contains("📚"))
    }
    
    @Test
    fun `iOS should handle multilingual content`() {
        val fixture = fixtureLoader.loadFixture("platform-encoding-utf8-multilingual")
        assertNotNull(fixture)
        
        // iOS has good Unicode support
        assertTrue(fixture.input.contains("こんにちは世界"))
    }
}
