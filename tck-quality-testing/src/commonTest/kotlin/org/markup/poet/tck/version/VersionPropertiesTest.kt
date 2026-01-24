package org.markup.poet.tck.version

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.math.abs

/**
 * Property-based tests for version tracking system.
 * 
 * These tests verify universal properties that should hold for all inputs.
 * Simplified implementation using kotlin.test instead of full Kotest property testing.
 */
class VersionPropertiesTest {
    
    /**
     * Property 3: Version Tracking Consistency
     * 
     * Verifies that version.txt matches sync metadata after update.
     */
    @Test
    fun `property 3 - version tracking consistency`() {
        val fileOps = TestMockVersionFileOperations()
        val tracker = DefaultVersionTracker(fileOps, "test-path")
        
        // Test with multiple versions
        repeat(10) { i ->
            val version = TckVersion(
                specVersion = "1.$i.0",
                commitHash = "hash$i",
                timestamp = i.toLong() * 1000,
                testCount = 100 + i
            )
            
            tracker.updateVersion(version)
            val retrieved = tracker.getCurrentVersion()
            
            assertNotNull(retrieved)
            assertEquals(version.specVersion, retrieved.specVersion)
            assertEquals(version.commitHash, retrieved.commitHash)
            assertEquals(version.testCount, retrieved.testCount)
        }
    }
    
    /**
     * Property 15: Change Detection Accuracy
     * 
     * Verifies that change report correctly categorizes all tests.
     */
    @Test
    fun `property 15 - change detection accuracy`() {
        val detector = DefaultChangeDetector()
        
        // Test with various test count differences
        val testCases = listOf(
            Pair(100, 110), // Added tests
            Pair(110, 100), // Removed tests
            Pair(100, 100), // No change
            Pair(50, 75),   // Added tests
            Pair(75, 50)    // Removed tests
        )
        
        testCases.forEach { (oldCount, newCount) ->
            val oldVersion = TckVersion("1.0.0", "old", 1000L, oldCount)
            val newVersion = TckVersion("1.0.0", "new", 2000L, newCount)
            
            val changes = detector.detectChanges(oldVersion, newVersion)
            
            val expectedChanges = abs(newCount - oldCount)
            val actualChanges = changes.addedTests.size + changes.removedTests.size
            
            assertEquals(expectedChanges, actualChanges)
            
            if (oldCount == newCount) {
                assertEquals(0, changes.addedTests.size)
                assertEquals(0, changes.removedTests.size)
            }
        }
    }
    
    /**
     * Property 16: Outdated Detection
     * 
     * Verifies that outdated detection works correctly when commit hashes differ.
     */
    @Test
    fun `property 16 - outdated detection`() {
        val detector = DefaultChangeDetector()
        val localVersion = TckVersion("1.0.0", "abc123", 1000L, 100)
        
        // Same hash - not outdated
        assertFalse(detector.isOutdated(localVersion, "abc123"))
        
        // Different hashes - outdated
        assertTrue(detector.isOutdated(localVersion, "def456"))
        assertTrue(detector.isOutdated(localVersion, "xyz789"))
    }
    
    /**
     * Property: Version comparison is transitive
     */
    @Test
    fun `property - version comparison is transitive`() {
        val comparator = DefaultVersionComparator()
        
        val v1 = "1.0.0"
        val v2 = "1.5.0"
        val v3 = "2.0.0"
        
        val cmp12 = comparator.compare(v1, v2)
        val cmp23 = comparator.compare(v2, v3)
        val cmp13 = comparator.compare(v1, v3)
        
        // v1 < v2 and v2 < v3, so v1 < v3
        assertTrue(cmp12 < 0)
        assertTrue(cmp23 < 0)
        assertTrue(cmp13 < 0)
    }
    
    /**
     * Property: Version comparison is reflexive
     */
    @Test
    fun `property - version comparison is reflexive`() {
        val comparator = DefaultVersionComparator()
        
        val versions = listOf("1.0.0", "2.5.3", "0.1.0", "10.20.30")
        
        versions.forEach { version ->
            assertEquals(0, comparator.compare(version, version))
        }
    }
    
    /**
     * Property: Version comparison is antisymmetric
     */
    @Test
    fun `property - version comparison is antisymmetric`() {
        val comparator = DefaultVersionComparator()
        
        val testCases = listOf(
            Pair("1.0.0", "2.0.0"),
            Pair("1.5.0", "1.0.0"),
            Pair("2.0.0", "2.0.1")
        )
        
        testCases.forEach { (v1, v2) ->
            val cmp12 = comparator.compare(v1, v2)
            val cmp21 = comparator.compare(v2, v1)
            
            if (cmp12 < 0) {
                assertEquals(1, cmp21)
            } else if (cmp12 > 0) {
                assertEquals(-1, cmp21)
            } else {
                assertEquals(0, cmp21)
            }
        }
    }
}

/**
 * Mock implementation of VersionFileOperations for property-based testing.
 */
internal class TestMockVersionFileOperations : VersionFileOperations {
    private val files = mutableMapOf<String, String>()
    
    override fun readFile(path: String): String? = files[path]
    
    override fun writeFile(path: String, content: String) {
        files[path] = content
    }
    
    override fun deleteFile(path: String) {
        files.remove(path)
    }
    
    override fun fileExists(path: String): Boolean = files.containsKey(path)
}
