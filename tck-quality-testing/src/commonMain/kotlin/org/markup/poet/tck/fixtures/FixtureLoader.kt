package org.markup.poet.tck.fixtures

/**
 * Loads and manages test fixtures.
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
}
