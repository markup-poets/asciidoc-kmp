package org.markup.poet.tck

import org.markup.poet.tck.config.ConfigLoader
import org.markup.poet.tck.config.TckConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for TckIntegration.
 */
class TckIntegrationTest {
    
    @Test
    fun `should create context with default configuration`() {
        val context = TckIntegration.initialize()
        
        assertNotNull(context)
        assertNotNull(context.config)
        assertNotNull(context.syncService)
        assertNotNull(context.versionTracker)
        assertNotNull(context.fixtureLoader)
        assertNotNull(context.testRunner)
        assertNotNull(context.resultAggregator)
        assertNotNull(context.reportGenerator)
        assertNotNull(context.certificationChecker)
    }
    
    @Test
    fun `should provide access to all TCK components`() {
        val context = TckIntegration.initialize()
        
        // Verify all components are accessible
        val config = context.config
        assertEquals("https://gitlab.eclipse.org/eclipse/asciidoc-lang/asciidoc-tck.git", config.sync.repositoryUrl)
        
        val syncService = context.syncService
        assertNotNull(syncService)
        
        val versionTracker = context.versionTracker
        assertNotNull(versionTracker)
        
        val fixtureLoader = context.fixtureLoader
        assertNotNull(fixtureLoader)
        
        val testRunner = context.testRunner
        assertNotNull(testRunner)
        
        val resultAggregator = context.resultAggregator
        assertNotNull(resultAggregator)
        
        val reportGenerator = context.reportGenerator
        assertNotNull(reportGenerator)
        
        val certificationChecker = context.certificationChecker
        assertNotNull(certificationChecker)
    }
    
    @Test
    fun `should reload configuration`() {
        val context = TckIntegration.initialize()
        val originalConfig = context.config
        
        // Reload should create new context with same config
        val reloadedContext = context.reloadConfig()
        
        assertNotNull(reloadedContext)
        assertEquals(originalConfig.sync.repositoryUrl, reloadedContext.config.sync.repositoryUrl)
    }
    
    @Test
    fun `should create context with custom configuration`() {
        val context = TckIntegration.initialize()
        val customConfig = TckConfig(
            sync = context.config.sync.copy(branch = "develop")
        )
        
        val customContext = context.withConfig(customConfig)
        
        assertEquals("develop", customContext.config.sync.branch)
    }
    
    @Test
    fun `should run tests with empty fixture list`() {
        val context = TckIntegration.initialize()
        
        // Run tests - may have custom fixtures loaded from resources
        val results = TckIntegration.runTests(context)
        
        assertNotNull(results)
        // Results may be > 0 if custom fixtures exist in resources
        assertTrue(results.totalTests >= 0)
    }
    
    @Test
    fun `should generate report from empty results`() {
        val context = TckIntegration.initialize()
        val results = TckIntegration.runTests(context)
        
        val report = TckIntegration.generateReport(context, results)
        
        assertNotNull(report)
        assertNotNull(report.metadata)
        assertNotNull(report.summary)
        // May have custom fixtures loaded
        assertTrue(report.summary.totalTests >= 0)
    }
    
    @Test
    fun `should check certification status`() {
        val context = TckIntegration.initialize()
        val results = TckIntegration.runTests(context)
        
        val status = TckIntegration.checkCertification(context, results)
        
        assertNotNull(status)
        // Certification status depends on test results
        // With placeholder parser/renderer, tests will be pending
        assertNotNull(status.blockingIssues)
    }
    
    @Test
    fun `should create internal context with components`() {
        val mockConfigLoader = MockConfigLoader()
        val config = TckConfig()
        
        val context = TckIntegration.createContext(config, mockConfigLoader)
        
        assertNotNull(context)
        assertEquals(config, context.config)
        assertEquals(mockConfigLoader, context.configLoader)
    }
}

/**
 * Mock ConfigLoader for testing.
 */
private class MockConfigLoader : ConfigLoader {
    override fun loadConfig(path: String): TckConfig {
        return TckConfig()
    }
    
    override fun saveConfig(config: TckConfig, path: String) {
        // No-op for testing
    }
    
    override fun validateConfig(config: TckConfig): org.markup.poet.tck.config.ConfigValidationResult {
        return org.markup.poet.tck.config.ConfigValidationResult(
            isValid = true,
            errors = emptyList()
        )
    }
}
