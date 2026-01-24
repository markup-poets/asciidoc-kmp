package org.markup.poet.tck.sync

/**
 * Shared mock implementations for testing sync components.
 */

/**
 * Mock implementation of GitOperations for testing.
 */
class MockGitOperations(
    private val cloneResult: GitResult = GitResult.Success("Cloned"),
    private val pullResult: GitResult = GitResult.Success("Pulled"),
    private val isValidRepo: Boolean = true,
    private val commitHash: String = "abc123",
    private val currentRef: String = "main"
) : GitOperations {
    
    var cloneCalled = false
    var pullCalled = false
    
    override suspend fun clone(url: String, destination: String, branch: String?): GitResult {
        cloneCalled = true
        return cloneResult
    }
    
    override suspend fun pull(repositoryPath: String): GitResult {
        pullCalled = true
        return pullResult
    }
    
    override fun getCurrentCommitHash(repositoryPath: String): String? {
        return commitHash
    }
    
    override fun getCurrentRef(repositoryPath: String): String? {
        return currentRef
    }
    
    override fun isValidRepository(repositoryPath: String): Boolean {
        return isValidRepo
    }
    
    override fun getRemoteUrl(repositoryPath: String, remoteName: String): String? {
        return "https://example.com/repo.git"
    }
}

/**
 * Mock implementation of SyncValidator for testing.
 */
class MockSyncValidator(
    private val structureResult: ValidationResult = ValidationResult.Valid(0),
    private val testFileValidations: List<TestFileValidation> = emptyList()
) : SyncValidator {
    
    override fun validateStructure(repositoryPath: String): ValidationResult {
        return structureResult
    }
    
    override fun validateTestFiles(repositoryPath: String): List<TestFileValidation> {
        return testFileValidations
    }
    
    override fun countTests(repositoryPath: String): Int {
        return when (structureResult) {
            is ValidationResult.Valid -> structureResult.testCount
            is ValidationResult.Invalid -> 0
        }
    }
}
