package org.markup.poet.tck.fixtures

/**
 * Platform-specific resource loader for reading fixture files.
 */
internal expect object ResourceLoader {
    /**
     * Read a resource file as a string.
     * @param path The resource path (e.g., "fixtures/blocks/paragraph-simple.json")
     * @return The file content as a string, or null if not found
     */
    fun readResource(path: String): String?
    
    /**
     * List all resource files in a directory.
     * @param path The directory path (e.g., "fixtures/blocks")
     * @return List of file names (not full paths) in the directory
     */
    fun listResources(path: String): List<String>
}
