package org.markup.poet.tck.execution

import org.markup.poet.tck.fixtures.TestFixture
import org.markup.poet.tck.fixtures.FixtureCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

class TestRunnerTest {
    
    @Test
    fun `should execute test successfully when parser and renderer work`() {
        val fixture = TestFixture(
            id = "test-1",
            category = FixtureCategory.BLOCK_PARAGRAPH,
            description = "Simple paragraph",
            input = "Hello, world!",
            expectedOutput = "<p>Hello, world!</p>",
            metadata = emptyMap()
        )
        
        val runner = DefaultTestRunner(
            parser = { input -> input }, // Identity parser
            renderer = { ast -> "<p>$ast</p>" }, // Simple renderer
            validator = DefaultOutputValidator()
        )
        
        val result = runner.runTest(fixture)
        
        assertEquals(TestStatus.PASSED, result.status)
        assertEquals("test-1", result.fixtureId)
        assertNotNull(result.actualOutput)
        assertTrue(result.durationMs >= 0)
    }
    
    @Test
    fun `should mark test as failed when output doesn't match`() {
        val fixture = TestFixture(
            id = "test-2",
            category = FixtureCategory.BLOCK_PARAGRAPH,
            description = "Paragraph with mismatch",
            input = "Hello",
            expectedOutput = "<p>Expected</p>",
            metadata = emptyMap()
        )
        
        val runner = DefaultTestRunner(
            parser = { input -> input },
            renderer = { ast -> "<p>$ast</p>" },
            validator = DefaultOutputValidator()
        )
        
        val result = runner.runTest(fixture)
        
        assertEquals(TestStatus.FAILED, result.status)
        assertNotNull(result.errorMessage)
    }
    
    @Test
    fun `should mark test as error when parser throws exception`() {
        val fixture = TestFixture(
            id = "test-3",
            category = FixtureCategory.BLOCK_PARAGRAPH,
            description = "Parser error",
            input = "Invalid input",
            expectedOutput = null,
            metadata = emptyMap()
        )
        
        val runner = DefaultTestRunner(
            parser = { throw IllegalArgumentException("Parse error") },
            renderer = { ast -> ast.toString() },
            validator = DefaultOutputValidator()
        )
        
        val result = runner.runTest(fixture)
        
        assertEquals(TestStatus.ERROR, result.status)
        assertNotNull(result.errorMessage)
        assertTrue(result.errorMessage!!.contains("Parse error"))
    }
    
    @Test
    fun `should mark test as pending when PendingTestException is thrown`() {
        val fixture = TestFixture(
            id = "test-4",
            category = FixtureCategory.BLOCK_TABLE,
            description = "Pending feature",
            input = "Table input",
            expectedOutput = null,
            metadata = emptyMap()
        )
        
        val runner = DefaultTestRunner(
            parser = { throw PendingTestException("Tables not implemented yet") },
            renderer = { ast -> ast.toString() },
            validator = DefaultOutputValidator()
        )
        
        val result = runner.runTest(fixture)
        
        assertEquals(TestStatus.PENDING, result.status)
        assertNotNull(result.errorMessage)
        assertTrue(result.errorMessage!!.contains("not implemented"))
    }
    
    @Test
    fun `should pass test when no expected output is provided`() {
        val fixture = TestFixture(
            id = "test-5",
            category = FixtureCategory.BLOCK_PARAGRAPH,
            description = "No expected output",
            input = "Any input",
            expectedOutput = null,
            metadata = emptyMap()
        )
        
        val runner = DefaultTestRunner(
            parser = { input -> input },
            renderer = { ast -> "<p>$ast</p>" },
            validator = DefaultOutputValidator()
        )
        
        val result = runner.runTest(fixture)
        
        assertEquals(TestStatus.PASSED, result.status)
    }
    
    @Test
    fun `should run multiple tests and return all results`() {
        val fixtures = listOf(
            TestFixture("test-1", FixtureCategory.BLOCK_PARAGRAPH, "Test 1", "input1", "<p>input1</p>", emptyMap()),
            TestFixture("test-2", FixtureCategory.BLOCK_HEADING, "Test 2", "input2", "<h1>input2</h1>", emptyMap()),
            TestFixture("test-3", FixtureCategory.INLINE_BOLD, "Test 3", "input3", "<strong>input3</strong>", emptyMap())
        )
        
        val runner = DefaultTestRunner(
            parser = { input -> input },
            renderer = { ast -> 
                when {
                    ast.toString().startsWith("input1") -> "<p>$ast</p>"
                    ast.toString().startsWith("input2") -> "<h1>$ast</h1>"
                    else -> "<strong>$ast</strong>"
                }
            },
            validator = DefaultOutputValidator()
        )
        
        val results = runner.runTests(fixtures)
        
        assertEquals(3, results.size)
        assertTrue(results.all { it.status == TestStatus.PASSED })
    }
    
    @Test
    fun `should filter tests before running`() {
        val fixtures = listOf(
            TestFixture("test-1", FixtureCategory.BLOCK_PARAGRAPH, "Test 1", "input1", null, emptyMap()),
            TestFixture("test-2", FixtureCategory.BLOCK_HEADING, "Test 2", "input2", null, emptyMap()),
            TestFixture("test-3", FixtureCategory.BLOCK_PARAGRAPH, "Test 3", "input3", null, emptyMap())
        )
        
        val runner = DefaultTestRunner(
            parser = { input -> input },
            renderer = { ast -> ast.toString() },
            validator = DefaultOutputValidator()
        )
        
        val filter = CategoryFilter(setOf(FixtureCategory.BLOCK_PARAGRAPH))
        val results = runner.runTestsFiltered(fixtures, filter)
        
        assertEquals(2, results.size)
        assertTrue(results.all { it.category == FixtureCategory.BLOCK_PARAGRAPH })
    }
}

class OutputValidatorTest {
    
    @Test
    fun `should validate matching output as success`() {
        val validator = DefaultOutputValidator()
        val result = validator.validate("expected", "expected")
        
        assertTrue(result is ValidationResult.Success)
    }
    
    @Test
    fun `should validate matching output with whitespace differences as success`() {
        val validator = DefaultOutputValidator()
        val result = validator.validate("  expected  ", "expected")
        
        assertTrue(result is ValidationResult.Success)
    }
    
    @Test
    fun `should validate non-matching output as failure`() {
        val validator = DefaultOutputValidator()
        val result = validator.validate("expected", "actual")
        
        assertTrue(result is ValidationResult.Failure)
        val failure = result as ValidationResult.Failure
        assertNotNull(failure.message)
        assertNotNull(failure.diff)
    }
    
    @Test
    fun `should include diff in failure result`() {
        val validator = DefaultOutputValidator()
        val result = validator.validate("expected output", "actual output")
        
        assertTrue(result is ValidationResult.Failure)
        val failure = result as ValidationResult.Failure
        assertTrue(failure.diff!!.contains("Expected:"))
        assertTrue(failure.diff!!.contains("Actual:"))
    }
}
