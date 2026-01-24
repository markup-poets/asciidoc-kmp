package org.markup.poet.tck.fixtures

/**
 * Loads and manages test fixtures.
 * 
 * This interface supports multiple test formats:
 * - Custom JSON format (single file)
 * - Official TCK format (paired input.adoc + output.json files)
 */
interface FixtureLoader {
    /**
     * Load a specific fixture by ID.
     */
    fun loadFixture(id: String): TestFixture
    
    /**
     * Load all fixtures in a category.
     */
    fun loadFixturesByCategory(category: FixtureCategory): List<TestFixture>
    
    /**
     * Load all available fixtures.
     */
    fun loadAllFixtures(): List<TestFixture>
    
    /**
     * Check if this loader supports the given file/path.
     * 
     * @param path File or directory path
     * @return true if this loader can handle the path
     */
    fun supports(path: String): Boolean
    
    /**
     * Get the format this loader handles.
     * 
     * @return The fixture format
     */
    fun getFormat(): FixtureFormat
}
