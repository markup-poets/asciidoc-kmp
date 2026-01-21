package org.markup.poet.tck.fixtures

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for FixtureLoadException.
 */
class FixtureLoadExceptionTest {
    
    @Test
    fun `should create exception with all fields`() {
        // Arrange & Act
        val exception = FixtureLoadException(
            fixtureId = "test-fixture-1",
            path = "/fixtures/test-fixture-1.json",
            message = "File not found"
        )
        
        // Assert
        assertEquals("test-fixture-1", exception.fixtureId)
        assertEquals("/fixtures/test-fixture-1.json", exception.path)
        assertTrue(exception.message!!.contains("test-fixture-1"))
        assertTrue(exception.message!!.contains("/fixtures/test-fixture-1.json"))
        assertTrue(exception.message!!.contains("File not found"))
        assertNull(exception.cause)
    }
    
    @Test
    fun `should create exception with cause`() {
        // Arrange
        val cause = IllegalArgumentException("Invalid JSON")
        
        // Act
        val exception = FixtureLoadException(
            fixtureId = "test-fixture-2",
            path = "/fixtures/test-fixture-2.json",
            message = "JSON parse error",
            cause = cause
        )
        
        // Assert
        assertEquals("test-fixture-2", exception.fixtureId)
        assertEquals("/fixtures/test-fixture-2.json", exception.path)
        assertNotNull(exception.cause)
        assertEquals(cause, exception.cause)
        assertTrue(exception.message!!.contains("JSON parse error"))
    }
    
    @Test
    fun `should format error message correctly`() {
        // Arrange & Act
        val exception = FixtureLoadException(
            fixtureId = "block-paragraph-simple",
            path = "/fixtures/blocks/paragraph-simple.json",
            message = "Resource not found"
        )
        
        // Assert
        val expectedMessage = "Failed to load fixture 'block-paragraph-simple' from '/fixtures/blocks/paragraph-simple.json': Resource not found"
        assertEquals(expectedMessage, exception.message)
    }
    
    @Test
    fun `should be throwable`() {
        // Arrange
        val exception = FixtureLoadException(
            fixtureId = "test-fixture",
            path = "/test/path",
            message = "Test error"
        )
        
        // Act & Assert
        try {
            throw exception
        } catch (e: FixtureLoadException) {
            assertEquals("test-fixture", e.fixtureId)
            assertEquals("/test/path", e.path)
        }
    }
    
    @Test
    fun `should preserve cause chain`() {
        // Arrange
        val rootCause = RuntimeException("Root cause")
        val intermediateCause = IllegalStateException("Intermediate", rootCause)
        
        // Act
        val exception = FixtureLoadException(
            fixtureId = "test-fixture",
            path = "/test/path",
            message = "Failed to load",
            cause = intermediateCause
        )
        
        // Assert
        assertEquals(intermediateCause, exception.cause)
        assertEquals(rootCause, exception.cause?.cause)
    }
}
