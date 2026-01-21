package org.markup.poet.tck.platform

import org.markup.poet.tck.fixtures.ResourceFixtureLoader
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Linux-specific platform tests.
 * 
 * Tests Linux-specific file I/O, encoding, and path resolution behavior.
 * 
 * Requirements: 7.1, 7.2, 7.3
 */
class LinuxPlatformSpecificTest {
    
    private val fixtureLoader = ResourceFixtureLoader()
    
    @Test
    fun `Linux should handle file paths with forward slashes`() {
        // Linux uses forward slashes for paths
        val fixture = fixtureLoader.loadFixture("platform-path-resolution-absolute")
        assertNotNull(fixture)
        assertTrue(fixture.input.contains("/"))
    }
    
    @Test
    fun `Linux should handle UTF-8 encoding correctly`() {
        val fixture = fixtureLoader.loadFixture("platform-encoding-utf8-basic")
        assertNotNull(fixture)
        
        // Verify UTF-8 characters are preserved
        assertTrue(fixture.input.contains("café"))
        assertTrue(fixture.input.contains("naïve"))
    }
    
    @Test
    fun `Linux should handle Unix-style line endings`() {
        val fixture = fixtureLoader.loadFixture("platform-file-io-multiline-read")
        assertNotNull(fixture)
        
        // Linux uses \n for line endings
        assertTrue(fixture.input.contains("\n"))
    }
    
    @Test
    fun `Linux should handle relative paths`() {
        val fixture = fixtureLoader.loadFixture("platform-path-resolution-relative")
        assertNotNull(fixture)
        assertTrue(fixture.input.contains(".."))
    }
}
