package org.markup.poet.tck.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Property-based tests for configuration system.
 * 
 * These tests verify universal properties that should hold for all inputs.
 * Simplified implementation using kotlin.test instead of full Kotest property testing.
 */
class ConfigPropertiesTest {
    
    /**
     * Property 20: Configuration Validation
     * 
     * Verifies that invalid configs fail immediately with clear messages.
     */
    @Test
    fun `property 20 - invalid configs fail with clear messages`() {
        val fileOps = TestMockConfigFileOperations()
        val loader = JsonConfigLoader(fileOps)
        
        val invalidConfigs = listOf(
            TckConfig(sync = SyncConfig(repositoryUrl = "")), // Blank URL
            TckConfig(sync = SyncConfig(repositoryUrl = "invalid-url")), // Invalid URL
            TckConfig(sync = SyncConfig(branch = "")), // Blank branch
            TckConfig(sync = SyncConfig(syncTimeoutSeconds = -1)), // Negative timeout
            TckConfig(execution = ExecutionConfig(testTimeoutSeconds = -1)), // Negative timeout
            TckConfig(execution = ExecutionConfig(enableOfficialTests = false, enableCustomTests = false)), // No sources
            TckConfig(reporting = ReportingConfig(outputDirectory = "")), // Blank directory
            TckConfig(reporting = ReportingConfig(generateJson = false, generateHtml = false, generateMarkdown = false)), // No formats
            TckConfig(reporting = ReportingConfig(maxDiffLength = -1)) // Negative diff length
        )
        
        invalidConfigs.forEach { config ->
            val result = loader.validateConfig(config)
            
            assertFalse(result.isValid, "Config should be invalid")
            assertTrue(result.errors.isNotEmpty(), "Should have error messages")
            assertTrue(result.errors.all { it.isNotBlank() }, "Error messages should not be blank")
        }
    }
    
    /**
     * Property: Valid configs pass validation
     */
    @Test
    fun `property - valid configs pass validation`() {
        val fileOps = TestMockConfigFileOperations()
        val loader = JsonConfigLoader(fileOps)
        
        val validConfigs = listOf(
            TckConfig(), // Default config
            TckConfig(
                sync = SyncConfig(repositoryUrl = "https://example.com/repo.git"),
                execution = ExecutionConfig(enableOfficialTests = true, enableCustomTests = false),
                reporting = ReportingConfig(generateJson = true, generateHtml = false, generateMarkdown = false)
            ),
            TckConfig(
                sync = SyncConfig(repositoryUrl = "git@github.com:user/repo.git"),
                execution = ExecutionConfig(enableOfficialTests = false, enableCustomTests = true),
                reporting = ReportingConfig(generateJson = false, generateHtml = true, generateMarkdown = false)
            )
        )
        
        validConfigs.forEach { config ->
            val result = loader.validateConfig(config)
            
            assertTrue(result.isValid, "Config should be valid")
            assertTrue(result.errors.isEmpty(), "Should have no error messages")
        }
    }
    
    /**
     * Property: Config round-trip preserves data
     */
    @Test
    fun `property - config round-trip preserves data`() {
        val fileOps = TestMockConfigFileOperations()
        val loader = JsonConfigLoader(fileOps)
        
        val config = TckConfig(
            sync = SyncConfig(
                repositoryUrl = "https://example.com/tck.git",
                branch = "main",
                localPath = "test-path",
                autoSync = true,
                syncFrequency = SyncFrequency.DAILY,
                syncTimeoutSeconds = 600
            ),
            execution = ExecutionConfig(
                enableOfficialTests = true,
                enableCustomTests = false,
                parallelExecution = true,
                testTimeoutSeconds = 60,
                failFast = true
            ),
            reporting = ReportingConfig(
                outputDirectory = "reports",
                generateJson = true,
                generateHtml = false,
                generateMarkdown = true,
                includeStackTraces = false,
                includeDiffs = true,
                includePendingTests = false,
                maxDiffLength = 500
            )
        )
        
        loader.saveConfig(config, "test-config.json")
        val loaded = loader.loadConfig("test-config.json")
        
        // Verify all fields match
        assertEquals(config.sync.repositoryUrl, loaded.sync.repositoryUrl)
        assertEquals(config.sync.branch, loaded.sync.branch)
        assertEquals(config.sync.localPath, loaded.sync.localPath)
        assertEquals(config.sync.autoSync, loaded.sync.autoSync)
        assertEquals(config.sync.syncFrequency, loaded.sync.syncFrequency)
        assertEquals(config.sync.syncTimeoutSeconds, loaded.sync.syncTimeoutSeconds)
        
        assertEquals(config.execution.enableOfficialTests, loaded.execution.enableOfficialTests)
        assertEquals(config.execution.enableCustomTests, loaded.execution.enableCustomTests)
        assertEquals(config.execution.parallelExecution, loaded.execution.parallelExecution)
        assertEquals(config.execution.testTimeoutSeconds, loaded.execution.testTimeoutSeconds)
        assertEquals(config.execution.failFast, loaded.execution.failFast)
        
        assertEquals(config.reporting.outputDirectory, loaded.reporting.outputDirectory)
        assertEquals(config.reporting.generateJson, loaded.reporting.generateJson)
        assertEquals(config.reporting.generateHtml, loaded.reporting.generateHtml)
        assertEquals(config.reporting.generateMarkdown, loaded.reporting.generateMarkdown)
        assertEquals(config.reporting.includeStackTraces, loaded.reporting.includeStackTraces)
        assertEquals(config.reporting.includeDiffs, loaded.reporting.includeDiffs)
        assertEquals(config.reporting.includePendingTests, loaded.reporting.includePendingTests)
        assertEquals(config.reporting.maxDiffLength, loaded.reporting.maxDiffLength)
    }
    
    /**
     * Property: Validation is idempotent
     */
    @Test
    fun `property - validation is idempotent`() {
        val fileOps = TestMockConfigFileOperations()
        val loader = JsonConfigLoader(fileOps)
        
        val configs = listOf(
            TckConfig(), // Valid
            TckConfig(sync = SyncConfig(repositoryUrl = "")) // Invalid
        )
        
        configs.forEach { config ->
            val result1 = loader.validateConfig(config)
            val result2 = loader.validateConfig(config)
            
            assertEquals(result1.isValid, result2.isValid)
            assertEquals(result1.errors, result2.errors)
        }
    }
}

/**
 * Mock implementation of ConfigFileOperations for property-based testing.
 */
internal class TestMockConfigFileOperations : ConfigFileOperations {
    val files = mutableMapOf<String, String>()
    
    override fun readFile(path: String): String? = files[path]
    
    override fun writeFile(path: String, content: String) {
        files[path] = content
    }
}
