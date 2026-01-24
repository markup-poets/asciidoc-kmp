package org.markup.poet.tck

import org.markup.poet.tck.config.ConfigLoader
import org.markup.poet.tck.config.TckConfig
import org.markup.poet.tck.conformance.CertificationChecker
import org.markup.poet.tck.conformance.ReportGenerator
import org.markup.poet.tck.execution.ResultAggregator
import org.markup.poet.tck.execution.TestRunner
import org.markup.poet.tck.fixtures.FixtureLoader
import org.markup.poet.tck.sync.TckSyncService
import org.markup.poet.tck.version.VersionTracker

/**
 * Context for TCK operations providing access to all TCK components.
 * 
 * This interface serves as the central access point for all TCK functionality,
 * providing access to configuration, sync, testing, and reporting components.
 * 
 * Example usage:
 * ```kotlin
 * val context = TckIntegration.initialize()
 * 
 * // Access configuration
 * val config = context.config
 * 
 * // Sync official TCK
 * val syncResult = context.syncService.sync()
 * 
 * // Load fixtures
 * val fixtures = context.fixtureLoader.loadAllFixtures()
 * 
 * // Run tests
 * val results = context.testRunner.runTests(fixtures)
 * 
 * // Generate report
 * val report = context.reportGenerator.generateReport(results)
 * ```
 */
interface TckContext {
    /**
     * Current TCK configuration.
     */
    val config: TckConfig
    
    /**
     * Configuration loader for reading/writing config files.
     */
    val configLoader: ConfigLoader
    
    /**
     * Sync service for managing official TCK repository.
     */
    val syncService: TckSyncService
    
    /**
     * Version tracker for tracking TCK versions.
     */
    val versionTracker: VersionTracker
    
    /**
     * Fixture loader for loading test fixtures.
     */
    val fixtureLoader: FixtureLoader
    
    /**
     * Test runner for executing tests.
     */
    val testRunner: TestRunner
    
    /**
     * Result aggregator for aggregating test results.
     */
    val resultAggregator: ResultAggregator
    
    /**
     * Report generator for generating conformance reports.
     */
    val reportGenerator: ReportGenerator
    
    /**
     * Certification checker for checking certification readiness.
     */
    val certificationChecker: CertificationChecker
    
    /**
     * Reload configuration from file.
     * 
     * @param configPath Path to configuration file (default: tck-config.json)
     * @return Updated TckContext with new configuration
     */
    fun reloadConfig(configPath: String = "tck-quality-testing/tck-config.json"): TckContext
    
    /**
     * Create a copy of this context with updated configuration.
     * 
     * @param config New configuration
     * @return New TckContext with updated configuration
     */
    fun withConfig(config: TckConfig): TckContext
}
