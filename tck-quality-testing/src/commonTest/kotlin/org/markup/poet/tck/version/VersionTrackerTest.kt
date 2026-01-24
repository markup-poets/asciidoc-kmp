package org.markup.poet.tck.version

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VersionTrackerTest {
    
    @Test
    fun `should return null when no version exists`() {
        val fileOps = TestMockVersionFileOperations()
        val tracker = DefaultVersionTracker(fileOps, "test-path")
        
        assertNull(tracker.getCurrentVersion())
    }
    
    @Test
    fun `should update and retrieve version`() {
        val fileOps = TestMockVersionFileOperations()
        val tracker = DefaultVersionTracker(fileOps, "test-path")
        
        val version = TckVersion(
            specVersion = "1.0.0",
            commitHash = "abc123",
            timestamp = 1000L,
            testCount = 100
        )
        
        tracker.updateVersion(version)
        
        val retrieved = tracker.getCurrentVersion()
        assertNotNull(retrieved)
        assertEquals("1.0.0", retrieved.specVersion)
        assertEquals("abc123", retrieved.commitHash)
    }
    
    @Test
    fun `should maintain version history`() {
        val fileOps = TestMockVersionFileOperations()
        val tracker = DefaultVersionTracker(fileOps, "test-path")
        
        val version1 = TckVersion("1.0.0", "abc123", 1000L, 100)
        val version2 = TckVersion("1.1.0", "def456", 2000L, 110)
        
        tracker.updateVersion(version1)
        tracker.updateVersion(version2)
        
        val history = tracker.getVersionHistory()
        assertEquals(2, history.size)
        assertEquals("def456", history[0].commitHash) // Most recent first
        assertEquals("abc123", history[1].commitHash)
    }
    
    @Test
    fun `should not duplicate versions with same commit hash`() {
        val fileOps = TestMockVersionFileOperations()
        val tracker = DefaultVersionTracker(fileOps, "test-path")
        
        val version1 = TckVersion("1.0.0", "abc123", 1000L, 100)
        val version2 = TckVersion("1.0.0", "abc123", 2000L, 100)
        
        tracker.updateVersion(version1)
        tracker.updateVersion(version2)
        
        val history = tracker.getVersionHistory()
        assertEquals(1, history.size)
        assertEquals(2000L, history[0].timestamp) // Updated timestamp
    }
    
    @Test
    fun `should limit history to 50 versions`() {
        val fileOps = TestMockVersionFileOperations()
        val tracker = DefaultVersionTracker(fileOps, "test-path")
        
        // Add 60 versions
        repeat(60) { i ->
            val version = TckVersion("1.0.$i", "hash$i", i.toLong(), 100)
            tracker.updateVersion(version)
        }
        
        val history = tracker.getVersionHistory()
        assertEquals(50, history.size)
        assertEquals("hash59", history[0].commitHash) // Most recent
    }
    
    @Test
    fun `should clear history`() {
        val fileOps = TestMockVersionFileOperations()
        val tracker = DefaultVersionTracker(fileOps, "test-path")
        
        val version = TckVersion("1.0.0", "abc123", 1000L, 100)
        tracker.updateVersion(version)
        
        tracker.clearHistory()
        
        assertNull(tracker.getCurrentVersion())
        assertTrue(tracker.getVersionHistory().isEmpty())
    }
}
