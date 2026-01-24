package org.markup.poet.tck.version

import org.markup.poet.tck.fixtures.TestFixture

/**
 * Detects changes between TCK versions.
 * 
 * The ChangeDetector compares two TCK versions to identify:
 * - Added tests
 * - Modified tests
 * - Removed tests
 * - Version changes
 * 
 * **Usage:**
 * ```kotlin
 * val detector = DefaultChangeDetector(fixtureLoader)
 * val changes = detector.detectChanges(oldVersion, newVersion)
 * 
 * println("Added: ${changes.addedTests.size}")
 * println("Modified: ${changes.modifiedTests.size}")
 * println("Removed: ${changes.removedTests.size}")
 * ```
 */
interface ChangeDetector {
    /**
     * Detect changes between two versions.
     * 
     * @param oldVersion Previous version
     * @param newVersion New version
     * @return Report of changes
     */
    fun detectChanges(oldVersion: TckVersion, newVersion: TckVersion): ChangeReport
    
    /**
     * Check if local version is outdated compared to remote.
     * 
     * @param localVersion Local version
     * @param remoteCommitHash Remote commit hash
     * @return true if local version is outdated
     */
    fun isOutdated(localVersion: TckVersion, remoteCommitHash: String): Boolean
}

/**
 * Default implementation of ChangeDetector.
 * 
 * Detects changes by comparing test counts and commit hashes.
 * For detailed change detection, requires access to test fixtures.
 */
class DefaultChangeDetector : ChangeDetector {
    
    override fun detectChanges(oldVersion: TckVersion, newVersion: TckVersion): ChangeReport {
        // Detect version change
        val versionChange = if (oldVersion.specVersion != newVersion.specVersion) {
            VersionChange(oldVersion.specVersion, newVersion.specVersion)
        } else {
            null
        }
        
        // For now, we can only detect test count changes
        // Detailed test-by-test comparison would require loading fixtures
        val testCountDiff = newVersion.testCount - oldVersion.testCount
        
        val addedTests = if (testCountDiff > 0) {
            // Placeholder: would need actual test IDs
            (1..testCountDiff).map { "test-added-$it" }
        } else {
            emptyList()
        }
        
        val removedTests = if (testCountDiff < 0) {
            // Placeholder: would need actual test IDs
            (1..(-testCountDiff)).map { "test-removed-$it" }
        } else {
            emptyList()
        }
        
        // Modified tests detection would require comparing test content
        val modifiedTests = emptyList<String>()
        
        return ChangeReport(
            oldVersion = oldVersion,
            newVersion = newVersion,
            addedTests = addedTests,
            modifiedTests = modifiedTests,
            removedTests = removedTests,
            versionChange = versionChange
        )
    }
    
    override fun isOutdated(localVersion: TckVersion, remoteCommitHash: String): Boolean {
        return localVersion.commitHash != remoteCommitHash
    }
}

/**
 * Enhanced change detector that compares actual test fixtures.
 * 
 * Provides detailed change detection by loading and comparing test fixtures.
 */
class DetailedChangeDetector(
    private val getTestIds: (TckVersion) -> List<String>
) : ChangeDetector {
    
    override fun detectChanges(oldVersion: TckVersion, newVersion: TckVersion): ChangeReport {
        // Get test IDs for both versions
        val oldTestIds = try {
            getTestIds(oldVersion).toSet()
        } catch (e: Exception) {
            emptySet()
        }
        
        val newTestIds = try {
            getTestIds(newVersion).toSet()
        } catch (e: Exception) {
            emptySet()
        }
        
        // Detect changes
        val addedTests = (newTestIds - oldTestIds).sorted()
        val removedTests = (oldTestIds - newTestIds).sorted()
        val commonTests = oldTestIds.intersect(newTestIds)
        
        // For modified tests, we would need to compare test content
        // For now, assume no modifications among common tests
        val modifiedTests = emptyList<String>()
        
        // Detect version change
        val versionChange = if (oldVersion.specVersion != newVersion.specVersion) {
            VersionChange(oldVersion.specVersion, newVersion.specVersion)
        } else {
            null
        }
        
        return ChangeReport(
            oldVersion = oldVersion,
            newVersion = newVersion,
            addedTests = addedTests,
            modifiedTests = modifiedTests,
            removedTests = removedTests,
            versionChange = versionChange
        )
    }
    
    override fun isOutdated(localVersion: TckVersion, remoteCommitHash: String): Boolean {
        return localVersion.commitHash != remoteCommitHash
    }
}
