package org.markup.poet.tck.fixtures

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Test that platform fixtures can be loaded correctly.
 */
class PlatformFixtureLoadingTest {
    
    private val loader = ResourceFixtureLoader()
    
    @Test
    fun `should load platform encoding utf8 basic fixture`() {
        val fixture = loader.loadFixture("platform-encoding-utf8-basic")
        
        assertNotNull(fixture)
        assertEquals("platform-encoding-utf8-basic", fixture.id)
        assertEquals(FixtureCategory.PLATFORM_ENCODING, fixture.category)
    }
    
    @Test
    fun `should load platform file io simple read fixture`() {
        val fixture = loader.loadFixture("platform-file-io-simple-read")
        
        assertNotNull(fixture)
        assertEquals("platform-file-io-simple-read", fixture.id)
        assertEquals(FixtureCategory.PLATFORM_FILE_IO, fixture.category)
    }
    
    @Test
    fun `should load platform path resolution absolute fixture`() {
        val fixture = loader.loadFixture("platform-path-resolution-absolute")
        
        assertNotNull(fixture)
        assertEquals("platform-path-resolution-absolute", fixture.id)
        assertEquals(FixtureCategory.PLATFORM_PATH_RESOLUTION, fixture.category)
    }
    
    @Test
    fun `should load all platform fixtures by category`() {
        val encodingFixtures = loader.loadFixturesByCategory(FixtureCategory.PLATFORM_ENCODING)
        val fileIoFixtures = loader.loadFixturesByCategory(FixtureCategory.PLATFORM_FILE_IO)
        val pathFixtures = loader.loadFixturesByCategory(FixtureCategory.PLATFORM_PATH_RESOLUTION)
        
        // We should have at least some fixtures in each category
        assertTrue(encodingFixtures.isNotEmpty(), "Should have encoding fixtures")
        assertTrue(fileIoFixtures.isNotEmpty(), "Should have file I/O fixtures")
        assertTrue(pathFixtures.isNotEmpty(), "Should have path resolution fixtures")
    }
}
