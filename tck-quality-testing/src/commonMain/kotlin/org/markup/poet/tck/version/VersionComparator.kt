package org.markup.poet.tck.version

/**
 * Compares TCK versions for ordering and compatibility checking.
 * 
 * The VersionComparator handles semantic version comparison (e.g., "1.0.0" vs "1.5.0")
 * and compatibility checking based on major/minor version rules.
 * 
 * **Usage:**
 * ```kotlin
 * val comparator = DefaultVersionComparator()
 * 
 * // Compare versions
 * val result = comparator.compare("1.5.0", "1.0.0") // returns 1 (newer)
 * 
 * // Check compatibility
 * val compatible = comparator.isCompatible("1.5.0", "1.0.0") // true (same major)
 * ```
 */
interface VersionComparator {
    /**
     * Compare two spec versions.
     * 
     * @param v1 First version
     * @param v2 Second version
     * @return Negative if v1 < v2, zero if equal, positive if v1 > v2
     */
    fun compare(v1: String, v2: String): Int
    
    /**
     * Check if a version is compatible with a required version.
     * 
     * Compatibility rules:
     * - Same major version = compatible
     * - Different major version = incompatible
     * 
     * @param version Version to check
     * @param requiredVersion Required version
     * @return true if compatible
     */
    fun isCompatible(version: String, requiredVersion: String): Boolean
    
    /**
     * Check if a version is newer than another.
     * 
     * @param version Version to check
     * @param otherVersion Version to compare against
     * @return true if version is newer
     */
    fun isNewer(version: String, otherVersion: String): Boolean
}

/**
 * Default implementation of VersionComparator.
 * 
 * Supports semantic versioning (MAJOR.MINOR.PATCH).
 */
class DefaultVersionComparator : VersionComparator {
    
    override fun compare(v1: String, v2: String): Int {
        val parts1 = parseVersion(v1)
        val parts2 = parseVersion(v2)
        
        // Compare major version
        val majorCompare = parts1.major.compareTo(parts2.major)
        if (majorCompare != 0) return majorCompare
        
        // Compare minor version
        val minorCompare = parts1.minor.compareTo(parts2.minor)
        if (minorCompare != 0) return minorCompare
        
        // Compare patch version
        return parts1.patch.compareTo(parts2.patch)
    }
    
    override fun isCompatible(version: String, requiredVersion: String): Boolean {
        val versionParts = parseVersion(version)
        val requiredParts = parseVersion(requiredVersion)
        
        // Compatible if same major version
        return versionParts.major == requiredParts.major
    }
    
    override fun isNewer(version: String, otherVersion: String): Boolean {
        return compare(version, otherVersion) > 0
    }
    
    /**
     * Parse a version string into components.
     */
    private fun parseVersion(version: String): VersionParts {
        val parts = version.split(".")
        
        return VersionParts(
            major = parts.getOrNull(0)?.toIntOrNull() ?: 0,
            minor = parts.getOrNull(1)?.toIntOrNull() ?: 0,
            patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
        )
    }
    
    private data class VersionParts(
        val major: Int,
        val minor: Int,
        val patch: Int
    )
}
