package org.markup.poet.tck.execution

import org.markup.poet.tck.fixtures.TestFixture
import org.markup.poet.tck.fixtures.FixtureCategory

/**
 * Filters tests based on various criteria.
 * 
 * TestFilters allow selective test execution based on:
 * - Category (e.g., only run paragraph tests)
 * - Source (custom vs official TCK)
 * - Spec section (e.g., only run tests for section 4.2)
 * - Custom predicates
 * 
 * **Usage:**
 * ```kotlin
 * // Run only paragraph and heading tests
 * val filter = CategoryFilter(setOf(
 *     FixtureCategory.BLOCK_PARAGRAPH,
 *     FixtureCategory.BLOCK_HEADING
 * ))
 * 
 * // Run only official TCK tests
 * val officialOnly = SourceFilter(
 *     allowCustom = false,
 *     allowOfficial = true
 * )
 * 
 * // Combine filters
 * val combined = CompositeFilter(
 *     listOf(filter, officialOnly),
 *     mode = CompositeFilter.FilterMode.AND
 * )
 * ```
 */
interface TestFilter {
    /**
     * Determine if a test should run.
     * 
     * @param fixture The test fixture to evaluate
     * @return true if the test should run, false otherwise
     */
    fun shouldRun(fixture: TestFixture): Boolean
}

/**
 * Filter by fixture category.
 * 
 * Only tests in the specified categories will run.
 * 
 * @param allowedCategories Set of categories to allow
 */
class CategoryFilter(
    private val allowedCategories: Set<FixtureCategory>
) : TestFilter {
    override fun shouldRun(fixture: TestFixture): Boolean {
        return fixture.category in allowedCategories
    }
}

/**
 * Filter by test source (custom vs official).
 * 
 * Controls whether custom tests, official TCK tests, or both should run.
 * 
 * @param allowCustom If true, allow custom tests
 * @param allowOfficial If true, allow official TCK tests
 */
class SourceFilter(
    private val allowCustom: Boolean = true,
    private val allowOfficial: Boolean = true
) : TestFilter {
    override fun shouldRun(fixture: TestFixture): Boolean {
        val source = fixture.metadata["source"] ?: "custom"
        return when (source) {
            "official-tck" -> allowOfficial
            "custom" -> allowCustom
            else -> true // Unknown sources are allowed by default
        }
    }
}

/**
 * Filter by spec section.
 * 
 * Only tests for the specified spec sections will run.
 * 
 * @param allowedSections Set of spec sections to allow (e.g., "4.2", "5.1")
 */
class SpecSectionFilter(
    private val allowedSections: Set<String>
) : TestFilter {
    override fun shouldRun(fixture: TestFixture): Boolean {
        val section = fixture.metadata["spec_section"] ?: return true
        return section in allowedSections
    }
}

/**
 * Composite filter that combines multiple filters.
 * 
 * Supports both AND and OR modes:
 * - AND: All filters must pass for the test to run
 * - OR: At least one filter must pass for the test to run
 * 
 * @param filters List of filters to combine
 * @param mode Combination mode (AND or OR)
 */
class CompositeFilter(
    private val filters: List<TestFilter>,
    private val mode: FilterMode = FilterMode.AND
) : TestFilter {
    
    /**
     * Filter combination mode.
     */
    enum class FilterMode {
        /**
         * All filters must pass.
         */
        AND,
        
        /**
         * At least one filter must pass.
         */
        OR
    }
    
    override fun shouldRun(fixture: TestFixture): Boolean {
        if (filters.isEmpty()) return true
        
        return when (mode) {
            FilterMode.AND -> filters.all { it.shouldRun(fixture) }
            FilterMode.OR -> filters.any { it.shouldRun(fixture) }
        }
    }
}

/**
 * Filter that always allows all tests.
 * 
 * Useful as a default or no-op filter.
 */
class AllowAllFilter : TestFilter {
    override fun shouldRun(fixture: TestFixture): Boolean = true
}

/**
 * Filter that blocks all tests.
 * 
 * Useful for testing or temporarily disabling test execution.
 */
class BlockAllFilter : TestFilter {
    override fun shouldRun(fixture: TestFixture): Boolean = false
}

/**
 * Filter based on a custom predicate.
 * 
 * Allows arbitrary filtering logic.
 * 
 * @param predicate Function that determines if a test should run
 */
class PredicateFilter(
    private val predicate: (TestFixture) -> Boolean
) : TestFilter {
    override fun shouldRun(fixture: TestFixture): Boolean {
        return predicate(fixture)
    }
}
