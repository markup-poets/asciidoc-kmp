package org.markup.poet.tck.fixtures

/**
 * iOS implementation of ResourceLoader.
 * 
 * Note: This is a basic implementation. For production use, this should
 * use NSBundle to load resources from the app bundle.
 */
internal actual object ResourceLoader {
    
    actual fun readResource(path: String): String? {
        // TODO: Implement using NSBundle.mainBundle().pathForResource()
        // For now, return null to indicate resource not found
        return null
    }
    
    actual fun listResources(path: String): List<String> {
        // TODO: Implement using NSBundle.mainBundle().pathsForResourcesOfType()
        // For now, return empty list
        return emptyList()
    }
}
