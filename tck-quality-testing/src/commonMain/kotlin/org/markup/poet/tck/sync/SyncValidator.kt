package org.markup.poet.tck.sync

/**
 * Validates the structure and integrity of the official TCK repository.
 * 
 * This validator checks:
 * - Repository structure (tests/ directory exists)
 * - Test files are present and properly formatted
 * - Required metadata files exist
 * 
 * **Note**: This validator checks the test data files only, not the JavaScript
 * test harness. We only care about the .adoc and .json test files.
 */
interface SyncValidator {
    /**
     * Validate the overall repository structure.
     * 
     * Checks for:
     * - tests/ directory exists
     * - tests/ contains subdirectories (block/, inline/, etc.)
     * - Test files follow the naming convention
     * 
     * @param repositoryPath Path to the local TCK repository
     * @return ValidationResult indicating if structure is valid
     */
    fun validateStructure(repositoryPath: String): ValidationResult
    
    /**
     * Validate individual test files.
     * 
     * Checks for:
     * - Paired files (*-input.adoc and *-output.json)
     * - Files are readable
     * - JSON files are valid JSON
     * 
     * @param repositoryPath Path to the local TCK repository
     * @return List of validation results for each test file
     */
    fun validateTestFiles(repositoryPath: String): List<TestFileValidation>
    
    /**
     * Count the number of valid test files in the repository.
     * 
     * @param repositoryPath Path to the local TCK repository
     * @return Number of valid test pairs found
     */
    fun countTests(repositoryPath: String): Int
}

/**
 * Validation result for a single test file.
 */
data class TestFileValidation(
    /**
     * Path to the test file (relative to repository root).
     */
    val filePath: String,
    
    /**
     * Whether the test file is valid.
     */
    val isValid: Boolean,
    
    /**
     * List of validation errors.
     * Empty if test is valid.
     */
    val errors: List<String> = emptyList()
)

/**
 * Default implementation of SyncValidator.
 */
class DefaultSyncValidator : SyncValidator {
    
    override fun validateStructure(repositoryPath: String): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Check if repository path exists
        if (!fileExists(repositoryPath)) {
            errors.add("Repository path does not exist: $repositoryPath")
            return ValidationResult.Invalid(errors)
        }
        
        // Check for tests/ directory
        val testsDir = "$repositoryPath/tests"
        if (!fileExists(testsDir)) {
            errors.add("tests/ directory not found in repository")
            return ValidationResult.Invalid(errors)
        }
        
        // Check for expected subdirectories
        val expectedDirs = listOf("block", "inline")
        for (dir in expectedDirs) {
            val dirPath = "$testsDir/$dir"
            if (!fileExists(dirPath)) {
                errors.add("Expected directory not found: tests/$dir")
            }
        }
        
        // If we have errors, return invalid
        if (errors.isNotEmpty()) {
            return ValidationResult.Invalid(errors)
        }
        
        // Count tests
        val testCount = countTests(repositoryPath)
        if (testCount == 0) {
            errors.add("No test files found in repository")
            return ValidationResult.Invalid(errors)
        }
        
        return ValidationResult.Valid(testCount)
    }
    
    override fun validateTestFiles(repositoryPath: String): List<TestFileValidation> {
        val validations = mutableListOf<TestFileValidation>()
        val testsDir = "$repositoryPath/tests"
        
        if (!fileExists(testsDir)) {
            return emptyList()
        }
        
        // Find all input files
        val inputFiles = findFiles(testsDir, "-input.adoc")
        
        for (inputFile in inputFiles) {
            val errors = mutableListOf<String>()
            
            // Check if corresponding output file exists
            val outputFile = inputFile.replace("-input.adoc", "-output.json")
            if (!fileExists(outputFile)) {
                errors.add("Missing corresponding output file: $outputFile")
            }
            
            // Check if input file is readable
            if (!isReadable(inputFile)) {
                errors.add("Input file is not readable: $inputFile")
            }
            
            // Check if output file is readable and valid JSON
            if (fileExists(outputFile)) {
                if (!isReadable(outputFile)) {
                    errors.add("Output file is not readable: $outputFile")
                } else if (!isValidJson(outputFile)) {
                    errors.add("Output file is not valid JSON: $outputFile")
                }
            }
            
            validations.add(
                TestFileValidation(
                    filePath = inputFile.removePrefix("$repositoryPath/"),
                    isValid = errors.isEmpty(),
                    errors = errors
                )
            )
        }
        
        return validations
    }
    
    override fun countTests(repositoryPath: String): Int {
        val testsDir = "$repositoryPath/tests"
        if (!fileExists(testsDir)) {
            return 0
        }
        
        // Count input files (each represents one test)
        return findFiles(testsDir, "-input.adoc").size
    }
    
    /**
     * Platform-specific file existence check.
     */
    private fun fileExists(path: String): Boolean {
        return platformFileExists(path)
    }
    
    /**
     * Platform-specific file readability check.
     */
    private fun isReadable(path: String): Boolean {
        return platformIsReadable(path)
    }
    
    /**
     * Platform-specific file finder.
     */
    private fun findFiles(directory: String, suffix: String): List<String> {
        return platformFindFiles(directory, suffix)
    }
    
    /**
     * Check if a file contains valid JSON.
     */
    private fun isValidJson(path: String): Boolean {
        return try {
            val content = platformReadFile(path)
            // Try to parse as JSON
            kotlinx.serialization.json.Json.parseToJsonElement(content)
            true
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Platform-specific file operations.
 * These are expect functions that will be implemented per platform.
 */
expect fun platformFileExists(path: String): Boolean
expect fun platformIsReadable(path: String): Boolean
expect fun platformFindFiles(directory: String, suffix: String): List<String>
expect fun platformReadFile(path: String): String
