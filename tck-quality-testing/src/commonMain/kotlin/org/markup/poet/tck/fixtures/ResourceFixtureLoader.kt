package org.markup.poet.tck.fixtures

import kotlinx.serialization.json.Json
import kotlinx.serialization.SerializationException

/**
 * Default implementation that loads fixtures from embedded resources.
 * 
 * Fixtures are expected to be JSON files in the fixtures/ directory,
 * organized by category subdirectories (e.g., fixtures/blocks/, fixtures/inline/).
 */
class ResourceFixtureLoader : FixtureLoader {
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    // Cache for loaded fixtures to avoid repeated file I/O
    private val fixtureCache = mutableMapOf<String, TestFixture>()
    private var allFixturesLoaded = false
    
    /**
     * Load a specific fixture by ID.
     * 
     * @param id The fixture ID (e.g., "block-paragraph-simple")
     * @return The loaded fixture
     * @throws FixtureLoadException if the fixture cannot be found or loaded
     */
    override fun loadFixture(id: String): TestFixture {
        // Check cache first
        fixtureCache[id]?.let { return it }
        
        // Try to find the fixture by searching all category directories
        val categoryDirs = listOf(
            "blocks", "inline", "attributes", "macros", 
            "malformed", "conformance", "platform"
        )
        
        for (dir in categoryDirs) {
            val path = "fixtures/$dir/$id.json"
            val content = ResourceLoader.readResource(path)
            
            if (content != null) {
                return try {
                    val fixture = json.decodeFromString<TestFixture>(content)
                    fixtureCache[id] = fixture
                    fixture
                } catch (e: SerializationException) {
                    throw FixtureLoadException(
                        fixtureId = id,
                        path = path,
                        message = "Failed to parse JSON",
                        cause = e
                    )
                } catch (e: IllegalArgumentException) {
                    throw FixtureLoadException(
                        fixtureId = id,
                        path = path,
                        message = "Invalid fixture format",
                        cause = e
                    )
                }
            }
        }
        
        throw FixtureLoadException(
            fixtureId = id,
            path = "fixtures/*/$id.json",
            message = "Fixture not found in any category directory"
        )
    }
    
    /**
     * Load all fixtures in a category.
     * 
     * @param category The fixture category
     * @return List of all fixtures in the category (may be empty)
     */
    override fun loadFixturesByCategory(category: FixtureCategory): List<TestFixture> {
        // Ensure all fixtures are loaded
        if (!allFixturesLoaded) {
            loadAllFixtures()
        }
        
        // Filter cached fixtures by category
        return fixtureCache.values.filter { it.category == category }
    }
    
    /**
     * Load all available fixtures from all categories.
     * 
     * @return List of all fixtures
     */
    override fun loadAllFixtures(): List<TestFixture> {
        if (allFixturesLoaded) {
            return fixtureCache.values.toList()
        }
        
        val categoryDirs = mapOf(
            "blocks" to listOf(
                FixtureCategory.BLOCK_PARAGRAPH,
                FixtureCategory.BLOCK_HEADING,
                FixtureCategory.BLOCK_LIST,
                FixtureCategory.BLOCK_TABLE,
                FixtureCategory.BLOCK_CODE,
                FixtureCategory.BLOCK_QUOTE
            ),
            "inline" to listOf(
                FixtureCategory.INLINE_BOLD,
                FixtureCategory.INLINE_ITALIC,
                FixtureCategory.INLINE_MONOSPACE,
                FixtureCategory.INLINE_SUBSCRIPT,
                FixtureCategory.INLINE_SUPERSCRIPT
            ),
            "attributes" to listOf(FixtureCategory.ATTRIBUTE),
            "macros" to listOf(FixtureCategory.MACRO),
            "malformed" to listOf(
                FixtureCategory.MALFORMED_BLOCK,
                FixtureCategory.MALFORMED_INLINE,
                FixtureCategory.MALFORMED_ATTRIBUTE,
                FixtureCategory.CIRCULAR_INCLUDE,
                FixtureCategory.MISSING_INCLUDE
            ),
            "conformance" to listOf(
                FixtureCategory.CONFORMANCE,
                FixtureCategory.CROSS_REFERENCE,
                FixtureCategory.INCLUDE
            ),
            "platform" to listOf(
                FixtureCategory.PLATFORM_FILE_IO,
                FixtureCategory.PLATFORM_ENCODING,
                FixtureCategory.PLATFORM_PATH_RESOLUTION
            )
        )
        
        // Load fixtures from each directory
        for ((dir, _) in categoryDirs) {
            val path = "fixtures/$dir"
            val files = ResourceLoader.listResources(path)
            
            for (file in files) {
                if (file.endsWith(".json")) {
                    val filePath = "$path/$file"
                    val content = ResourceLoader.readResource(filePath)
                    
                    if (content != null) {
                        try {
                            val fixture = json.decodeFromString<TestFixture>(content)
                            fixtureCache[fixture.id] = fixture
                        } catch (e: Exception) {
                            // Log warning but continue loading other fixtures
                            println("Warning: Failed to load fixture from $filePath: ${e.message}")
                        }
                    }
                }
            }
        }
        
        allFixturesLoaded = true
        return fixtureCache.values.toList()
    }
}
