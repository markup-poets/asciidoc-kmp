package org.markup.poet.tck.fixtures

/**
 * Linux implementation of ResourceLoader.
 * 
 * Note: This is a basic stub implementation. For production use, this should
 * use platform-specific file I/O to load resources.
 */
internal actual object ResourceLoader {
    
    actual fun readResource(path: String): String? {
        // TODO: Implement using platform-specific file I/O
        // For now, return null to indicate resource not found
        return null
    }
    
    actual fun listResources(path: String): List<String> {
        // TODO: Implement using platform-specific directory listing
        // For now, return empty list
        return emptyList()
    }
}
