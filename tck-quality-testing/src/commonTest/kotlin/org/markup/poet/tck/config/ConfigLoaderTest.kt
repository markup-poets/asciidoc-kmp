package org.markup.poet.tck.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ConfigLoaderTest {
    
    @Test
    fun `should load default config when file does not exist`() {
        val fileOps = MockConfigFileOperations()
        val loader = JsonConfigLoader(fileOps)
        
        val config = loader.loadConfig("nonexistent.json")
        
        assertNotNull(config)
        assertEquals("https://gitlab.eclipse.org/eclipse/asciidoc-lang/asciidoc-tck.git", config.sync.repositoryUrl)
        assertTrue(config.execution.enableOfficialTests)
        assertTrue(config.reporting.generateJson)
    }
    
    @Test
    fun `should load config from JSON file`() {
        val fileOps = MockConfigFileOperations()
        fileOps.files["test-config.json"] = """
            {
              "sync": {
                "repositoryUrl": "https://example.com/tck.git",
                "branch": "develop",
                "localPath": "custom-path",
                "autoSync": true,
                "syncFrequency": "DAILY",
                "syncTimeoutSeconds": 600
              },
              "execution": {
                "enableOfficialTests": false,
                "enableCustomTests": true,
                "parallelExecution": false,
                "testTimeoutSeconds": 60,
                "allowedCategories": [],
                "excludedCategories": [],
                "failFast": true
              },
              "reporting": {
                "outputDirectory": "reports",
                "generateJson": false,
                "generateHtml": true,
                "generateMarkdown": false,
                "includeStackTraces": false,
                "includeDiffs": false,
                "includePendingTests": false,
                "maxDiffLength": 500
              }
            }
        """.trimIndent()
        
        val loader = JsonConfigLoader(fileOps)
        val config = loader.loadConfig("test-config.json")
        
        assertEquals("https://example.com/tck.git", config.sync.repositoryUrl)
        assertEquals("develop", config.sync.branch)
        assertEquals("custom-path", config.sync.localPath)
        assertTrue(config.sync.autoSync)
        assertEquals(SyncFrequency.DAILY, config.sync.syncFrequency)
        assertEquals(600L, config.sync.syncTimeoutSeconds)
        
        assertFalse(config.execution.enableOfficialTests)
        assertTrue(config.execution.enableCustomTests)
        assertFalse(config.execution.parallelExecution)
        assertEquals(60L, config.execution.testTimeoutSeconds)
        assertTrue(config.execution.failFast)
        
        assertEquals("reports", config.reporting.outputDirectory)
        assertFalse(config.reporting.generateJson)
        assertTrue(config.reporting.generateHtml)
        assertFalse(config.reporting.generateMarkdown)
        assertFalse(config.reporting.includeStackTraces)
        assertFalse(config.reporting.includeDiffs)
        assertFalse(config.reporting.includePendingTests)
        assertEquals(500, config.reporting.maxDiffLength)
    }
    
    @Test
    fun `should save config to JSON file`() {
        val fileOps = MockConfigFileOperations()
        val loader = JsonConfigLoader(fileOps)
        
        val config = TckConfig(
            sync = SyncConfig(
                repositoryUrl = "https://example.com/tck.git",
                branch = "main"
            )
        )
        
        loader.saveConfig(config, "test-config.json")
        
        val savedContent = fileOps.files["test-config.json"]
        assertNotNull(savedContent)
        assertTrue(savedContent.contains("https://example.com/tck.git"))
        assertTrue(savedContent.contains("main"))
    }
    
    @Test
    fun `should throw exception for invalid JSON`() {
        val fileOps = MockConfigFileOperations()
        fileOps.files["invalid.json"] = "{ invalid json }"
        
        val loader = JsonConfigLoader(fileOps)
        
        assertFailsWith<ConfigLoadException> {
            loader.loadConfig("invalid.json")
        }
    }
    
    @Test
    fun `should validate config before saving`() {
        val fileOps = MockConfigFileOperations()
        val loader = JsonConfigLoader(fileOps)
        
        val invalidConfig = TckConfig(
            sync = SyncConfig(repositoryUrl = ""), // Invalid: blank URL
            execution = ExecutionConfig(testTimeoutSeconds = -1) // Invalid: negative timeout
        )
        
        assertFailsWith<ConfigValidationException> {
            loader.saveConfig(invalidConfig, "test-config.json")
        }
    }
}

class ConfigValidationTest {
    
    private val loader = JsonConfigLoader(MockConfigFileOperations())
    
    @Test
    fun `should validate valid config`() {
        val config = TckConfig()
        
        val result = loader.validateConfig(config)
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
    
    @Test
    fun `should reject blank repository URL`() {
        val config = TckConfig(
            sync = SyncConfig(repositoryUrl = "")
        )
        
        val result = loader.validateConfig(config)
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Repository URL") })
    }
    
    @Test
    fun `should reject invalid repository URL`() {
        val config = TckConfig(
            sync = SyncConfig(repositoryUrl = "invalid-url")
        )
        
        val result = loader.validateConfig(config)
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("valid HTTP") || it.contains("SSH URL") })
    }
    
    @Test
    fun `should reject blank branch`() {
        val config = TckConfig(
            sync = SyncConfig(branch = "")
        )
        
        val result = loader.validateConfig(config)
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Branch") })
    }
    
    @Test
    fun `should reject negative sync timeout`() {
        val config = TckConfig(
            sync = SyncConfig(syncTimeoutSeconds = -1)
        )
        
        val result = loader.validateConfig(config)
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Sync timeout") })
    }
    
    @Test
    fun `should reject negative test timeout`() {
        val config = TckConfig(
            execution = ExecutionConfig(testTimeoutSeconds = -1)
        )
        
        val result = loader.validateConfig(config)
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Test timeout") })
    }
    
    @Test
    fun `should reject config with no test sources enabled`() {
        val config = TckConfig(
            execution = ExecutionConfig(
                enableOfficialTests = false,
                enableCustomTests = false
            )
        )
        
        val result = loader.validateConfig(config)
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("test source") })
    }
    
    @Test
    fun `should reject blank output directory`() {
        val config = TckConfig(
            reporting = ReportingConfig(outputDirectory = "")
        )
        
        val result = loader.validateConfig(config)
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Output directory") })
    }
    
    @Test
    fun `should reject config with no report formats enabled`() {
        val config = TckConfig(
            reporting = ReportingConfig(
                generateJson = false,
                generateHtml = false,
                generateMarkdown = false
            )
        )
        
        val result = loader.validateConfig(config)
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("report format") })
    }
    
    @Test
    fun `should reject negative max diff length`() {
        val config = TckConfig(
            reporting = ReportingConfig(maxDiffLength = -1)
        )
        
        val result = loader.validateConfig(config)
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Max diff length") })
    }
    
    @Test
    fun `should accumulate multiple validation errors`() {
        val config = TckConfig(
            sync = SyncConfig(repositoryUrl = "", branch = ""),
            execution = ExecutionConfig(testTimeoutSeconds = -1),
            reporting = ReportingConfig(outputDirectory = "")
        )
        
        val result = loader.validateConfig(config)
        
        assertFalse(result.isValid)
        assertTrue(result.errors.size >= 4) // At least 4 errors
    }
}

/**
 * Mock implementation of ConfigFileOperations for testing.
 */
class MockConfigFileOperations : ConfigFileOperations {
    val files = mutableMapOf<String, String>()
    
    override fun readFile(path: String): String? {
        return files[path]
    }
    
    override fun writeFile(path: String, content: String) {
        files[path] = content
    }
}
