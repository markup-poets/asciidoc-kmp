package org.markup.poet.tck.compatibility

import org.markup.poet.tck.fixtures.FixtureLoader
import org.markup.poet.tck.validation.OutputValidator

/**
 * Base class for compatibility tests.
 * 
 * Provides utilities for running tests using fixtures and validating outputs.
 */
abstract class CompatibilityTest {
    protected abstract val fixtureLoader: FixtureLoader
    protected abstract val validator: OutputValidator
    
    /**
     * Run a compatibility test using a fixture.
     */
    protected fun runCompatibilityTest(
        fixtureId: String,
        parser: (String) -> Any,
        renderer: (Any) -> String
    ) {
        val fixture = fixtureLoader.loadFixture(fixtureId)
        val parsed = parser(fixture.input)
        val rendered = renderer(parsed)
        
        fixture.expectedOutput?.let { expected ->
            val result = validator.validate(expected, rendered)
            when (result) {
                is org.markup.poet.tck.validation.ValidationResult.Success -> {
                    // Test passed
                }
                is org.markup.poet.tck.validation.ValidationResult.Failure -> {
                    throw AssertionError(
                        "Compatibility test failed for fixture $fixtureId:\n" +
                        result.message + "\n" +
                        "Diff:\n${result.diff}"
                    )
                }
            }
        }
    }
    
    /**
     * Mark a test as pending (not yet implemented).
     */
    protected fun pending(reason: String): Nothing {
        throw PendingTestException(reason)
    }
}

/**
 * Exception thrown when a test is marked as pending.
 */
class PendingTestException(message: String) : Exception(message)
