package org.markup.poet.tck.execution

import org.markup.poet.tck.fixtures.FixtureCategory

/**
 * Aggregates test results into summary statistics.
 * 
 * The ResultAggregator takes raw test execution results and produces
 * comprehensive statistics broken down by:
 * - Platform (JVM, iOS, Linux, Android)
 * - Category (paragraph, heading, list, etc.)
 * - Source (custom vs official TCK)
 * 
 * **Usage:**
 * ```kotlin
 * val aggregator = DefaultResultAggregator()
 * val results = collector.getAllResults()
 * val aggregated = aggregator.aggregate(results)
 * 
 * println(aggregated.summary())
 * ```
 */
interface ResultAggregator {
    /**
     * Aggregate results into summary statistics.
     * 
     * @param results List of test execution results
     * @return Aggregated results with statistics
     */
    fun aggregate(results: List<TestExecutionResult>): AggregatedResults
}

/**
 * Default implementation of ResultAggregator.
 * 
 * Provides comprehensive aggregation with breakdowns by platform,
 * category, and source.
 */
class DefaultResultAggregator : ResultAggregator {
    
    override fun aggregate(results: List<TestExecutionResult>): AggregatedResults {
        val totalTests = results.size
        val passed = results.count { it.status == TestStatus.PASSED }
        val failed = results.count { it.status == TestStatus.FAILED }
        val skipped = results.count { it.status == TestStatus.SKIPPED }
        val pending = results.count { it.status == TestStatus.PENDING }
        val errors = results.count { it.status == TestStatus.ERROR }
        
        val byPlatform = aggregateByPlatform(results)
        val byCategory = aggregateByCategory(results)
        val bySource = aggregateBySource(results)
        
        val failedTests = results.filter { it.status == TestStatus.FAILED }
        val pendingTests = results.filter { it.status == TestStatus.PENDING }
        
        return AggregatedResults(
            totalTests = totalTests,
            passed = passed,
            failed = failed,
            skipped = skipped,
            pending = pending,
            errors = errors,
            byPlatform = byPlatform,
            byCategory = byCategory,
            bySource = bySource,
            failedTests = failedTests,
            pendingTests = pendingTests
        )
    }
    
    /**
     * Aggregate results by platform.
     */
    private fun aggregateByPlatform(results: List<TestExecutionResult>): Map<String, PlatformResults> {
        return results.groupBy { it.platform }
            .mapValues { (platform, platformResults) ->
                val total = platformResults.size
                val passed = platformResults.count { it.status == TestStatus.PASSED }
                val failed = platformResults.count { it.status == TestStatus.FAILED }
                val passRate = if (total > 0) passed.toDouble() / total else 0.0
                
                PlatformResults(
                    platform = platform,
                    total = total,
                    passed = passed,
                    failed = failed,
                    passRate = passRate
                )
            }
    }
    
    /**
     * Aggregate results by category.
     */
    private fun aggregateByCategory(results: List<TestExecutionResult>): Map<FixtureCategory, CategoryResults> {
        return results
            .filter { it.category != null }
            .groupBy { it.category!! }
            .mapValues { (category, categoryResults) ->
                val total = categoryResults.size
                val passed = categoryResults.count { it.status == TestStatus.PASSED }
                val failed = categoryResults.count { it.status == TestStatus.FAILED }
                val passRate = if (total > 0) passed.toDouble() / total else 0.0
                
                CategoryResults(
                    category = category,
                    total = total,
                    passed = passed,
                    failed = failed,
                    passRate = passRate
                )
            }
    }
    
    /**
     * Aggregate results by source (custom vs official).
     */
    private fun aggregateBySource(results: List<TestExecutionResult>): Map<String, SourceResults> {
        return results.groupBy { it.source ?: "custom" }
            .mapValues { (source, sourceResults) ->
                val total = sourceResults.size
                val passed = sourceResults.count { it.status == TestStatus.PASSED }
                val failed = sourceResults.count { it.status == TestStatus.FAILED }
                val passRate = if (total > 0) passed.toDouble() / total else 0.0
                
                SourceResults(
                    source = source,
                    total = total,
                    passed = passed,
                    failed = failed,
                    passRate = passRate
                )
            }
    }
}

/**
 * Result aggregator that caches aggregation results.
 * 
 * Useful when aggregating the same results multiple times.
 */
class CachingResultAggregator(
    private val delegate: ResultAggregator = DefaultResultAggregator()
) : ResultAggregator {
    
    private var cachedResults: List<TestExecutionResult>? = null
    private var cachedAggregation: AggregatedResults? = null
    
    override fun aggregate(results: List<TestExecutionResult>): AggregatedResults {
        // Check if we can use cached results
        if (results === cachedResults && cachedAggregation != null) {
            return cachedAggregation!!
        }
        
        // Aggregate and cache
        val aggregated = delegate.aggregate(results)
        cachedResults = results
        cachedAggregation = aggregated
        
        return aggregated
    }
    
    /**
     * Clear the cache.
     */
    fun clearCache() {
        cachedResults = null
        cachedAggregation = null
    }
}
