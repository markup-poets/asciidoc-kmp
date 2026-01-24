package org.markup.poet.tck.execution

/**
 * Collects test results from multiple test runs.
 * 
 * The ResultCollector provides a centralized place to accumulate test results
 * from different test runs, platforms, or test suites. This is useful for:
 * - Aggregating results across multiple test runs
 * - Collecting results from different platforms
 * - Building comprehensive test reports
 * 
 * **Usage:**
 * ```kotlin
 * val collector = InMemoryResultCollector()
 * 
 * // Run tests on JVM
 * val jvmResults = runner.runTests(fixtures)
 * collector.addResults(jvmResults)
 * 
 * // Run tests on iOS
 * val iosResults = runner.runTests(fixtures)
 * collector.addResults(iosResults)
 * 
 * // Get all results
 * val allResults = collector.getAllResults()
 * ```
 */
interface ResultCollector {
    /**
     * Add results from a test run.
     * 
     * @param results List of test execution results to add
     */
    fun addResults(results: List<TestExecutionResult>)
    
    /**
     * Add a single result.
     * 
     * @param result Test execution result to add
     */
    fun addResult(result: TestExecutionResult) {
        addResults(listOf(result))
    }
    
    /**
     * Get all collected results.
     * 
     * @return List of all test execution results
     */
    fun getAllResults(): List<TestExecutionResult>
    
    /**
     * Get results filtered by platform.
     * 
     * @param platform Platform name to filter by
     * @return List of results for the specified platform
     */
    fun getResultsByPlatform(platform: String): List<TestExecutionResult> {
        return getAllResults().filter { it.platform == platform }
    }
    
    /**
     * Get results filtered by status.
     * 
     * @param status Test status to filter by
     * @return List of results with the specified status
     */
    fun getResultsByStatus(status: TestStatus): List<TestExecutionResult> {
        return getAllResults().filter { it.status == status }
    }
    
    /**
     * Get results filtered by category.
     * 
     * @param category Fixture category to filter by
     * @return List of results for the specified category
     */
    fun getResultsByCategory(category: org.markup.poet.tck.fixtures.FixtureCategory): List<TestExecutionResult> {
        return getAllResults().filter { it.category == category }
    }
    
    /**
     * Get the total number of collected results.
     * 
     * @return Total number of results
     */
    fun size(): Int {
        return getAllResults().size
    }
    
    /**
     * Check if the collector is empty.
     * 
     * @return true if no results have been collected
     */
    fun isEmpty(): Boolean {
        return size() == 0
    }
    
    /**
     * Clear all collected results.
     */
    fun clear()
}

/**
 * In-memory implementation of ResultCollector.
 * 
 * Stores all results in memory. Suitable for most use cases where
 * the number of results is manageable.
 * 
 * **Thread Safety:** This implementation is NOT thread-safe. If you need
 * to collect results from multiple threads, use external synchronization.
 */
class InMemoryResultCollector : ResultCollector {
    private val results = mutableListOf<TestExecutionResult>()
    
    override fun addResults(results: List<TestExecutionResult>) {
        this.results.addAll(results)
    }
    
    override fun getAllResults(): List<TestExecutionResult> {
        return results.toList()
    }
    
    override fun clear() {
        results.clear()
    }
    
    /**
     * Get a summary of collected results.
     */
    fun summary(): String {
        val total = results.size
        val passed = results.count { it.status == TestStatus.PASSED }
        val failed = results.count { it.status == TestStatus.FAILED }
        val pending = results.count { it.status == TestStatus.PENDING }
        val skipped = results.count { it.status == TestStatus.SKIPPED }
        val errors = results.count { it.status == TestStatus.ERROR }
        
        return buildString {
            appendLine("Collected Results Summary:")
            appendLine("  Total: $total")
            appendLine("  Passed: $passed")
            appendLine("  Failed: $failed")
            appendLine("  Pending: $pending")
            appendLine("  Skipped: $skipped")
            appendLine("  Errors: $errors")
        }
    }
}

/**
 * Result collector that delegates to multiple collectors.
 * 
 * Useful for collecting results to multiple destinations simultaneously
 * (e.g., in-memory and file-based).
 */
class CompositeResultCollector(
    private val collectors: List<ResultCollector>
) : ResultCollector {
    
    override fun addResults(results: List<TestExecutionResult>) {
        collectors.forEach { it.addResults(results) }
    }
    
    override fun getAllResults(): List<TestExecutionResult> {
        // Return results from the first collector
        return collectors.firstOrNull()?.getAllResults() ?: emptyList()
    }
    
    override fun clear() {
        collectors.forEach { it.clear() }
    }
}
