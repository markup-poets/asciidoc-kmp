package org.markup.poet.tck.version

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChangeDetectorTest {
    
    private val detector = DefaultChangeDetector()
    
    @Test
    fun `should detect no changes for same version`() {
        val version = TckVersion("1.0.0", "abc123", 1000L, 100)
        
        val changes = detector.detectChanges(version, version)
        
        assertFalse(changes.hasChanges())
        assertEquals(0, changes.totalChanges())
    }
    
    @Test
    fun `should detect added tests`() {
        val oldVersion = TckVersion("1.0.0", "abc123", 1000L, 100)
        val newVersion = TckVersion("1.0.0", "def456", 2000L, 110)
        
        val changes = detector.detectChanges(oldVersion, newVersion)
        
        assertTrue(changes.hasChanges())
        assertEquals(10, changes.addedTests.size)
        assertEquals(0, changes.removedTests.size)
    }
    
    @Test
    fun `should detect removed tests`() {
        val oldVersion = TckVersion("1.0.0", "abc123", 1000L, 110)
        val newVersion = TckVersion("1.0.0", "def456", 2000L, 100)
        
        val changes = detector.detectChanges(oldVersion, newVersion)
        
        assertTrue(changes.hasChanges())
        assertEquals(0, changes.addedTests.size)
        assertEquals(10, changes.removedTests.size)
    }
    
    @Test
    fun `should detect version change`() {
        val oldVersion = TckVersion("1.0.0", "abc123", 1000L, 100)
        val newVersion = TckVersion("1.5.0", "def456", 2000L, 100)
        
        val changes = detector.detectChanges(oldVersion, newVersion)
        
        assertNotNull(changes.versionChange)
        assertEquals("1.0.0", changes.versionChange?.from)
        assertEquals("1.5.0", changes.versionChange?.to)
    }
    
    @Test
    fun `should not detect version change for same spec version`() {
        val oldVersion = TckVersion("1.0.0", "abc123", 1000L, 100)
        val newVersion = TckVersion("1.0.0", "def456", 2000L, 110)
        
        val changes = detector.detectChanges(oldVersion, newVersion)
        
        assertNull(changes.versionChange)
    }
    
    @Test
    fun `should check if version is outdated`() {
        val localVersion = TckVersion("1.0.0", "abc123", 1000L, 100)
        
        assertTrue(detector.isOutdated(localVersion, "def456"))
        assertFalse(detector.isOutdated(localVersion, "abc123"))
    }
    
    @Test
    fun `should generate change summary`() {
        val oldVersion = TckVersion("1.0.0", "abc123", 1000L, 100)
        val newVersion = TckVersion("1.5.0", "def456", 2000L, 110)
        
        val changes = detector.detectChanges(oldVersion, newVersion)
        val summary = changes.summary()
        
        assertTrue(summary.contains("abc123"))
        assertTrue(summary.contains("def456"))
        assertTrue(summary.contains("Added: 10"))
        assertTrue(summary.contains("1.0.0 → 1.5.0"))
    }
}

class DetailedChangeDetectorTest {
    
    @Test
    fun `should detect added tests with test IDs`() {
        val oldTestIds = listOf("test1", "test2", "test3")
        val newTestIds = listOf("test1", "test2", "test3", "test4", "test5")
        
        val detector = DetailedChangeDetector { version ->
            if (version.commitHash == "old") oldTestIds else newTestIds
        }
        
        val oldVersion = TckVersion("1.0.0", "old", 1000L, 3)
        val newVersion = TckVersion("1.0.0", "new", 2000L, 5)
        
        val changes = detector.detectChanges(oldVersion, newVersion)
        
        assertEquals(2, changes.addedTests.size)
        assertTrue(changes.addedTests.contains("test4"))
        assertTrue(changes.addedTests.contains("test5"))
    }
    
    @Test
    fun `should detect removed tests with test IDs`() {
        val oldTestIds = listOf("test1", "test2", "test3", "test4", "test5")
        val newTestIds = listOf("test1", "test2", "test3")
        
        val detector = DetailedChangeDetector { version ->
            if (version.commitHash == "old") oldTestIds else newTestIds
        }
        
        val oldVersion = TckVersion("1.0.0", "old", 1000L, 5)
        val newVersion = TckVersion("1.0.0", "new", 2000L, 3)
        
        val changes = detector.detectChanges(oldVersion, newVersion)
        
        assertEquals(2, changes.removedTests.size)
        assertTrue(changes.removedTests.contains("test4"))
        assertTrue(changes.removedTests.contains("test5"))
    }
    
    @Test
    fun `should handle errors when loading test IDs`() {
        val detector = DetailedChangeDetector { _ ->
            throw Exception("Failed to load tests")
        }
        
        val oldVersion = TckVersion("1.0.0", "old", 1000L, 100)
        val newVersion = TckVersion("1.0.0", "new", 2000L, 110)
        
        val changes = detector.detectChanges(oldVersion, newVersion)
        
        // Should not crash, returns empty lists
        assertEquals(0, changes.addedTests.size)
        assertEquals(0, changes.removedTests.size)
    }
}

class VersionChangeTest {
    
    @Test
    fun `should detect major version change`() {
        val change = VersionChange("1.5.0", "2.0.0")
        
        assertTrue(change.isMajorChange())
        assertFalse(change.isMinorChange())
    }
    
    @Test
    fun `should detect minor version change`() {
        val change = VersionChange("1.0.0", "1.5.0")
        
        assertFalse(change.isMajorChange())
        assertTrue(change.isMinorChange())
    }
    
    @Test
    fun `should not detect change for patch version`() {
        val change = VersionChange("1.0.0", "1.0.5")
        
        assertFalse(change.isMajorChange())
        assertFalse(change.isMinorChange())
    }
    
    @Test
    fun `should handle invalid version strings`() {
        val change = VersionChange("invalid", "also-invalid")
        
        // Should not crash - both parse to 0.0.0
        assertFalse(change.isMajorChange())
        assertFalse(change.isMinorChange())
    }
}
