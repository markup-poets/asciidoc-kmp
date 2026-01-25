package org.markup.poet.tck.config

import org.markup.poet.tck.platformReadFile
import org.markup.poet.tck.platformWriteFile
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Loads and saves TCK configuration.
 * 
 * **Usage:**
 * ```kotlin
 * val loader = JsonConfigLoader(fileOperations)
 * 
 * // Load configuration
 * val config = loader.loadConfig()
 * 
 * // Save configuration
 * loader.saveConfig(config)
 * ```
 */
interface ConfigLoader {
    /**
     * Load configuration from file.
     * 
     * @param path Configuration file path
     * @return Loaded configuration, or default if file doesn't exist
     */
    fun loadConfig(path: String = "tck-quality-testing/tck-config.json"): TckConfig
    
    /**
     * Save configuration to file.
     * 
     * @param config Configuration to save
     * @param path Configuration file path
     */
    fun saveConfig(config: TckConfig, path: String = "tck-quality-testing/tck-config.json")
    
    /**
     * Validate configuration.
     * 
     * @param config Configuration to validate
     * @return Validation result
     */
    fun validateConfig(config: TckConfig): ConfigValidationResult
}

/**
 * JSON-based configuration loader.
 */
class JsonConfigLoader(
    private val fileOperations: ConfigFileOperations
) : ConfigLoader {
    
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }
    
    override fun loadConfig(path: String): TckConfig {
        return try {
            val content = fileOperations.readFile(path)
            
            if (content == null) {
                // File doesn't exist, return default config
                return TckConfig()
            }
            
            json.decodeFromString<TckConfig>(content)
        } catch (e: SerializationException) {
            throw ConfigLoadException("Failed to parse configuration file: ${e.message}", e)
        } catch (e: Exception) {
            throw ConfigLoadException("Failed to load configuration: ${e.message}", e)
        }
    }
    
    override fun saveConfig(config: TckConfig, path: String) {
        try {
            // Validate before saving
            val validation = validateConfig(config)
            if (!validation.isValid) {
                throw ConfigValidationException(
                    "Invalid configuration: ${validation.errors.joinToString(", ")}"
                )
            }
            
            val content = json.encodeToString(config)
            fileOperations.writeFile(path, content)
        } catch (e: ConfigValidationException) {
            throw e
        } catch (e: Exception) {
            throw ConfigSaveException("Failed to save configuration: ${e.message}", e)
        }
    }
    
    override fun validateConfig(config: TckConfig): ConfigValidationResult {
        val errors = mutableListOf<String>()
        
        // Validate sync config
        if (config.sync.repositoryUrl.isBlank()) {
            errors.add("Repository URL cannot be blank")
        }
        
        if (!config.sync.repositoryUrl.startsWith("http://") && 
            !config.sync.repositoryUrl.startsWith("https://") &&
            !config.sync.repositoryUrl.startsWith("git@")) {
            errors.add("Repository URL must be a valid HTTP(S) or SSH URL")
        }
        
        if (config.sync.branch.isBlank()) {
            errors.add("Branch cannot be blank")
        }
        
        if (config.sync.localPath.isBlank()) {
            errors.add("Local path cannot be blank")
        }
        
        if (config.sync.syncTimeoutSeconds <= 0) {
            errors.add("Sync timeout must be positive")
        }
        
        // Validate execution config
        if (config.execution.testTimeoutSeconds <= 0) {
            errors.add("Test timeout must be positive")
        }
        
        if (!config.execution.enableOfficialTests && !config.execution.enableCustomTests) {
            errors.add("At least one test source must be enabled")
        }
        
        // Validate reporting config
        if (config.reporting.outputDirectory.isBlank()) {
            errors.add("Output directory cannot be blank")
        }
        
        if (!config.reporting.generateJson && 
            !config.reporting.generateHtml && 
            !config.reporting.generateMarkdown) {
            errors.add("At least one report format must be enabled")
        }
        
        if (config.reporting.maxDiffLength < 0) {
            errors.add("Max diff length cannot be negative")
        }
        
        return ConfigValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }
}

/**
 * Result of configuration validation.
 */
data class ConfigValidationResult(
    /**
     * Whether the configuration is valid.
     */
    val isValid: Boolean,
    
    /**
     * Validation errors (empty if valid).
     */
    val errors: List<String>
)

/**
 * File operations for configuration loading/saving.
 */
interface ConfigFileOperations {
    /**
     * Read a file's content.
     * 
     * @param path File path
     * @return File content, or null if file doesn't exist
     */
    fun readFile(path: String): String?
    
    /**
     * Write content to a file.
     * 
     * @param path File path
     * @param content Content to write
     */
    fun writeFile(path: String, content: String)
}

/**
 * Exception thrown when configuration loading fails.
 */
class ConfigLoadException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Exception thrown when configuration saving fails.
 */
class ConfigSaveException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Exception thrown when configuration validation fails.
 */
class ConfigValidationException(message: String) : Exception(message)
