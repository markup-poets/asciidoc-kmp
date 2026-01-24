package org.markup.poet.tck.fixtures

/**
 * Composite fixture loader that delegates to multiple format-specific loaders.
 * 
 * This loader enables dual format support, allowing both custom JSON fixtures
 * and official TCK fixtures to be loaded and executed together.
 * 
 * **Architecture:**
 * ```
 * CompositeFixtureLoader
 * ├── ResourceFixtureLoader (custom JSON)
 * └── OfficialTckFixtureLoader (official TCK)
 * ```
 * 
 * **Usage:**
 * ```kotlin
 * val customLoader = ResourceFixtureLoader()
 * val officialLoader = OfficialTckFixtureLoader("official-tck/repository")
 * val detector = DefaultFormatDetector()
 * 
 * val composite = CompositeFixtureLoader(
 *     loaders = listOf(customLoader, officialLoader),
 *     formatDetector = detector
 * )
 * 
 * // Load from both sources
 * val allTests = composite.loadAllFixtures()
 * ```
 * 
 * **Features:**
 * - Automatic format detection
 * - Aggregates fixtures from multiple sources
 * - Preserves fixture IDs (no conflicts between custom and official)
 * - Supports filtering by source (custom vs official)
 */
class CompositeFixtureLoader(
    private val loaders: List<FixtureLoader>,
    private val formatDetector: FormatDetector = DefaultFormatDetector()
) : FixtureLoader {
    
    /**
     * Load a specific fixture by ID.
     * 
     * Tries each loader in order until one succeeds.
     * 
     * @param id The fixture ID
     * @return The loaded fixture
     * @throws FixtureNotFoundException if no loader can find the fixture
     */
    override fun loadFixture(id: String): TestFixture {
        // Try each loader until one succeeds
        for (loader in loaders) {
            try {
                return loader.loadFixture(id)
            } catch (e: FixtureNotFoundException) {
                // Continue to next loader
                continue
            } catch (e: FixtureLoadException) {
                // Continue to next loader
                continue
            }
        }
        
        throw FixtureNotFoundException(id)
    }
    
    /**
     * Load all fixtures in a category from all loaders.
     * 
     * Aggregates fixtures from all sources.
     * 
     * @param category The fixture category
     * @return List of all fixtures in the category from all sources
     */
    override fun loadFixturesByCategory(category: FixtureCategory): List<TestFixture> {
        val allFixtures = mutableListOf<TestFixture>()
        
        for (loader in loaders) {
            try {
                val fixtures = loader.loadFixturesByCategory(category)
                allFixtures.addAll(fixtures)
            } catch (e: Exception) {
                // Log warning but continue with other loaders
                println("Warning: Loader ${loader::class.simpleName} failed to load category $category: ${e.message}")
            }
        }
        
        return allFixtures
    }
    
    /**
     * Load all available fixtures from all loaders.
     * 
     * Aggregates fixtures from all sources.
     * 
     * @return List of all fixtures from all sources
     */
    override fun loadAllFixtures(): List<TestFixture> {
        val allFixtures = mutableListOf<TestFixture>()
        
        for (loader in loaders) {
            try {
                val fixtures = loader.loadAllFixtures()
                allFixtures.addAll(fixtures)
            } catch (e: Exception) {
                // Log warning but continue with other loaders
                println("Warning: Loader ${loader::class.simpleName} failed to load fixtures: ${e.message}")
            }
        }
        
        return allFixtures
    }
    
    /**
     * Check if any loader supports the given path.
     * 
     * @param path File or directory path
     * @return true if at least one loader can handle the path
     */
    override fun supports(path: String): Boolean {
        return loaders.any { it.supports(path) }
    }
    
    /**
     * Get the format for this composite loader.
     * 
     * @return FixtureFormat.UNKNOWN (since it supports multiple formats)
     */
    override fun getFormat(): FixtureFormat {
        return FixtureFormat.UNKNOWN
    }
    
    /**
     * Load fixtures from a specific source (custom or official).
     * 
     * @param source The source to load from ("custom" or "official-tck")
     * @return List of fixtures from the specified source
     */
    fun loadFixturesBySource(source: String): List<TestFixture> {
        val allFixtures = loadAllFixtures()
        return allFixtures.filter { fixture ->
            fixture.metadata["source"] == source
        }
    }
    
    /**
     * Load only custom fixtures.
     * 
     * @return List of custom fixtures
     */
    fun loadCustomFixtures(): List<TestFixture> {
        return loaders
            .filter { it.getFormat() == FixtureFormat.CUSTOM_JSON }
            .flatMap { it.loadAllFixtures() }
    }
    
    /**
     * Load only official TCK fixtures.
     * 
     * @return List of official TCK fixtures
     */
    fun loadOfficialFixtures(): List<TestFixture> {
        return loaders
            .filter { it.getFormat() == FixtureFormat.OFFICIAL_TCK }
            .flatMap { it.loadAllFixtures() }
    }
    
    /**
     * Get statistics about loaded fixtures.
     * 
     * @return FixtureStatistics with counts by source and category
     */
    fun getStatistics(): FixtureStatistics {
        val allFixtures = loadAllFixtures()
        val customCount = allFixtures.count { it.metadata["source"] != "official-tck" }
        val officialCount = allFixtures.count { it.metadata["source"] == "official-tck" }
        
        val byCategory = allFixtures.groupBy { it.category }
            .mapValues { it.value.size }
        
        return FixtureStatistics(
            totalCount = allFixtures.size,
            customCount = customCount,
            officialCount = officialCount,
            byCategory = byCategory
        )
    }
    
    /**
     * Get the appropriate loader for a given path.
     * 
     * @param path File or directory path
     * @return The loader that can handle this path, or null if none
     */
    fun getLoaderForPath(path: String): FixtureLoader? {
        return loaders.find { it.supports(path) }
    }
}

/**
 * Statistics about loaded fixtures.
 */
data class FixtureStatistics(
    /**
     * Total number of fixtures across all sources.
     */
    val totalCount: Int,
    
    /**
     * Number of custom fixtures.
     */
    val customCount: Int,
    
    /**
     * Number of official TCK fixtures.
     */
    val officialCount: Int,
    
    /**
     * Fixture counts by category.
     */
    val byCategory: Map<FixtureCategory, Int>
) {
    /**
     * Get a human-readable summary.
     */
    fun summary(): String {
        return buildString {
            appendLine("Fixture Statistics:")
            appendLine("  Total: $totalCount")
            appendLine("  Custom: $customCount")
            appendLine("  Official TCK: $officialCount")
            appendLine("  By Category:")
            byCategory.entries.sortedByDescending { it.value }.forEach { (category, count) ->
                appendLine("    ${category.name}: $count")
            }
        }
    }
}
