package org.markup.poet.tck.validation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultOutputValidatorTest {
    private val validator = DefaultOutputValidator()
    
    @Test
    fun `should return success when strings match exactly`() {
        val expected = "Hello, World!"
        val actual = "Hello, World!"
        
        val result = validator.validate(expected, actual)
        
        assertTrue(result is ValidationResult.Success)
    }
    
    @Test
    fun `should return failure when strings differ`() {
        val expected = "Hello, World!"
        val actual = "Hello, Kotlin!"
        
        val result = validator.validate(expected, actual)
        
        assertTrue(result is ValidationResult.Failure)
        val failure = result as ValidationResult.Failure
        assertEquals(expected, failure.expected)
        assertEquals(actual, failure.actual)
        assertTrue(failure.diff != null)
    }
    
    @Test
    fun `should return success for empty strings`() {
        val result = validator.validate("", "")
        
        assertTrue(result is ValidationResult.Success)
    }
    
    @Test
    fun `should return failure when one string is empty`() {
        val result = validator.validate("content", "")
        
        assertTrue(result is ValidationResult.Failure)
    }
    
    @Test
    fun `should normalize whitespace when validating with whitespace ignore`() {
        val expected = "Hello    World"
        val actual = "Hello World"
        
        val result = validator.validateIgnoringWhitespace(expected, actual)
        
        assertTrue(result is ValidationResult.Success)
    }
    
    @Test
    fun `should normalize leading and trailing whitespace`() {
        val expected = "  Hello World  "
        val actual = "Hello World"
        
        val result = validator.validateIgnoringWhitespace(expected, actual)
        
        assertTrue(result is ValidationResult.Success)
    }
    
    @Test
    fun `should normalize multiple spaces to single space`() {
        val expected = "Hello     World     Test"
        val actual = "Hello World Test"
        
        val result = validator.validateIgnoringWhitespace(expected, actual)
        
        assertTrue(result is ValidationResult.Success)
    }
    
    @Test
    fun `should generate diff showing added lines`() {
        val expected = "Line 1"
        val actual = "Line 1\nLine 2"
        
        val result = validator.validate(expected, actual)
        
        assertTrue(result is ValidationResult.Failure)
        val failure = result as ValidationResult.Failure
        assertTrue(failure.diff!!.contains("+ Line 2"))
    }
    
    @Test
    fun `should generate diff showing removed lines`() {
        val expected = "Line 1\nLine 2"
        val actual = "Line 1"
        
        val result = validator.validate(expected, actual)
        
        assertTrue(result is ValidationResult.Failure)
        val failure = result as ValidationResult.Failure
        assertTrue(failure.diff!!.contains("- Line 2"))
    }
    
    @Test
    fun `should generate diff showing changed lines`() {
        val expected = "Line 1\nLine 2"
        val actual = "Line 1\nLine 3"
        
        val result = validator.validate(expected, actual)
        
        assertTrue(result is ValidationResult.Failure)
        val failure = result as ValidationResult.Failure
        assertTrue(failure.diff!!.contains("- Line 2"))
        assertTrue(failure.diff!!.contains("+ Line 3"))
    }
}
