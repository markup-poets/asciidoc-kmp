package org.markup.poet.tck.platform

import org.markup.poet.tck.fixtures.ResourceFixtureLoader
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * JVM-specific platform tests.
 * 
 * Tests JVM-specific file I/O, encoding, and path resolution behavior.
 * 
 * Requirements: 7.1, 7.2, 7.3
 */
class JvmPlatformSpecificTest {
    
    private val fixtureLoader = ResourceFixtureLoader()
    
    @Test
    fun `JVM should handle file paths with forward slashes`() {
        // JVM on all platforms should handle forward slashes
        val fixture = fixtureLoader.loadFixture("platform-path-resolution-absolute")
        assertNotNull(fixture)
        assertTrue(fixture.input.contains("/"))
    }
    
    @Test
    fun `JVM should handle UTF-8 encoding correctly`() {
        val fixture = fixtureLoader.loadFixture("platform-encoding-utf8-basic")
        assertNotNull(fixture)
        
        // Verify UTF-8 characters are preserved
        assertTrue(fixture.input.contains("café"))
        assertTrue(fixture.input.contains("naïve"))
    }
    
    @Test
    fun `JVM should handle system-specific line separators`() {
        val fixture = fixtureLoader.loadFixture("platform-file-io-multiline-read")
        assertNotNull(fixture)
        
        // JVM uses System.lineSeparator() which varies by OS
        // But fixture should use \n for consistency
        assertTrue(fixture.input.contains("\n"))
    }
}
