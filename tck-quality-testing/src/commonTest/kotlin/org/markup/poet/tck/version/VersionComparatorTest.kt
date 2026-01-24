package org.markup.poet.tck.version

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VersionComparatorTest {
    
    private val comparator = DefaultVersionComparator()
    
    @Test
    fun `should compare equal versions`() {
        assertEquals(0, comparator.compare("1.0.0", "1.0.0"))
        assertEquals(0, comparator.compare("2.5.3", "2.5.3"))
    }
    
    @Test
    fun `should compare major versions`() {
        assertTrue(comparator.compare("2.0.0", "1.0.0") > 0)
        assertTrue(comparator.compare("1.0.0", "2.0.0") < 0)
    }
    
    @Test
    fun `should compare minor versions`() {
        assertTrue(comparator.compare("1.5.0", "1.0.0") > 0)
        assertTrue(comparator.compare("1.0.0", "1.5.0") < 0)
    }
    
    @Test
    fun `should compare patch versions`() {
        assertTrue(comparator.compare("1.0.5", "1.0.0") > 0)
        assertTrue(comparator.compare("1.0.0", "1.0.5") < 0)
    }
    
    @Test
    fun `should handle missing version parts`() {
        assertEquals(0, comparator.compare("1", "1.0.0"))
        assertEquals(0, comparator.compare("1.0", "1.0.0"))
    }
    
    @Test
    fun `should check compatibility for same major version`() {
        assertTrue(comparator.isCompatible("1.5.0", "1.0.0"))
        assertTrue(comparator.isCompatible("1.0.0", "1.5.0"))
        assertTrue(comparator.isCompatible("1.2.3", "1.9.9"))
    }
    
    @Test
    fun `should check incompatibility for different major version`() {
        assertFalse(comparator.isCompatible("2.0.0", "1.0.0"))
        assertFalse(comparator.isCompatible("1.0.0", "2.0.0"))
    }
    
    @Test
    fun `should check if version is newer`() {
        assertTrue(comparator.isNewer("1.5.0", "1.0.0"))
        assertTrue(comparator.isNewer("2.0.0", "1.9.9"))
        assertFalse(comparator.isNewer("1.0.0", "1.5.0"))
        assertFalse(comparator.isNewer("1.0.0", "1.0.0"))
    }
    
    @Test
    fun `should handle invalid version strings gracefully`() {
        // Invalid versions default to 0.0.0
        assertEquals(0, comparator.compare("invalid", "0.0.0"))
        assertEquals(0, comparator.compare("", "0.0.0"))
    }
}
