package org.markup.poet.asciidoc.render

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for RenderContext.
 * 
 * Tests ID generation with collision handling and warning collection.
 */
class RenderContextTest {
    
    @Test
    fun `should generate simple ID from text`() {
        // Arrange
        val config = RenderConfig.default()
        val context = RenderContext(config)
        
        // Act
        val id = context.generateId("Introduction")
        
        // Assert
        assertEquals("introduction", id)
    }
    
    @Test
    fun `should replace spaces with hyphens`() {
        // Arrange
        val config = RenderConfig.default()
        val context = RenderContext(config)
        
        // Act
        val id = context.generateId("Getting Started")
        
        // Assert
        assertEquals("getting-started", id)
    }
    
    @Test
    fun `should replace multiple non-alphanumeric characters with single hyphen`() {
        // Arrange
        val config = RenderConfig.default()
        val context = RenderContext(config)
        
        // Act
        val id = context.generateId("Hello,  World!")
        
        // Assert
        assertEquals("hello-world", id)
    }
    
    @Test
    fun `should trim leading and trailing hyphens`() {
        // Arrange
        val config = RenderConfig.default()
        val context = RenderContext(config)
        
        // Act
        val id = context.generateId("---Test---")
        
        // Assert
        assertEquals("test", id)
    }
    
    @Test
    fun `should handle duplicate IDs with numeric suffix`() {
        // Arrange
        val config = RenderConfig.default()
        val context = RenderContext(config)
        
        // Act
        val id1 = context.generateId("Introduction")
        val id2 = context.generateId("Introduction")
        val id3 = context.generateId("Introduction")
        
        // Assert
        assertEquals("introduction", id1)
        assertEquals("introduction-1", id2)
        assertEquals("introduction-2", id3)
    }
    
    @Test
    fun `should handle duplicate IDs with different casing`() {
        // Arrange
        val config = RenderConfig.default()
        val context = RenderContext(config)
        
        // Act
        val id1 = context.generateId("Introduction")
        val id2 = context.generateId("INTRODUCTION")
        val id3 = context.generateId("introduction")
        
        // Assert
        assertEquals("introduction", id1)
        assertEquals("introduction-1", id2)
        assertEquals("introduction-2", id3)
    }
    
    @Test
    fun `should handle text with only special characters`() {
        // Arrange
        val config = RenderConfig.default()
        val context = RenderContext(config)
        
        // Act
        val id = context.generateId("@#$%^&*()")
        
        // Assert
        assertEquals("section", id)
    }
    
    @Test
    fun `should handle empty text`() {
        // Arrange
        val config = RenderConfig.default()
        val context = RenderContext(config)
        
        // Act
        val id = context.generateId("")
        
        // Assert
        assertEquals("section", id)
    }
    
    @Test
    fun `should handle text with numbers`() {
        // Arrange
        val config = RenderConfig.default()
        val context = RenderContext(config)
        
        // Act
        val id = context.generateId("Chapter 1: Introduction")
        
        // Assert
        assertEquals("chapter-1-introduction", id)
    }
    
    @Test
    fun `should handle mixed case and special characters`() {
        // Arrange
        val config = RenderConfig.default()
        val context = RenderContext(config)
        
        // Act
        val id = context.generateId("API Reference (v2.0)")
        
        // Assert
        assertEquals("api-reference-v2-0", id)
    }
    
    @Test
    fun `should collect warnings`() {
        // Arrange
        val config = RenderConfig.default()
        val context = RenderContext(config)
        
        // Act
        context.logWarning("Unknown node type: CustomNode")
        context.logWarning("Skipped invalid content")
        
        // Assert
        val warnings = context.getWarnings()
        assertEquals(2, warnings.size)
        assertEquals("Unknown node type: CustomNode", warnings[0])
        assertEquals("Skipped invalid content", warnings[1])
    }
    
    @Test
    fun `should return empty list when no warnings logged`() {
        // Arrange
        val config = RenderConfig.default()
        val context = RenderContext(config)
        
        // Act
        val warnings = context.getWarnings()
        
        // Assert
        assertTrue(warnings.isEmpty())
    }
    
    @Test
    fun `should return immutable copy of warnings`() {
        // Arrange
        val config = RenderConfig.default()
        val context = RenderContext(config)
        context.logWarning("First warning")
        
        // Act
        val warnings1 = context.getWarnings()
        context.logWarning("Second warning")
        val warnings2 = context.getWarnings()
        
        // Assert
        assertEquals(1, warnings1.size)
        assertEquals(2, warnings2.size)
    }
    
    @Test
    fun `should provide access to theme from config`() {
        // Arrange
        val customTheme = DefaultTheme()
        val config = RenderConfig(theme = customTheme)
        val context = RenderContext(config)
        
        // Act
        val theme = context.theme
        
        // Assert
        assertEquals(customTheme, theme)
    }
    
    @Test
    fun `should provide access to config`() {
        // Arrange
        val config = RenderConfig(
            outputOptions = OutputOptions(standalone = false)
        )
        val context = RenderContext(config)
        
        // Act
        val retrievedConfig = context.config
        
        // Assert
        assertEquals(config, retrievedConfig)
        assertEquals(false, retrievedConfig.outputOptions.standalone)
    }
    
    @Test
    fun `should handle collision with default section ID`() {
        // Arrange
        val config = RenderConfig.default()
        val context = RenderContext(config)
        
        // Act
        val id1 = context.generateId("!!!") // Should become "section"
        val id2 = context.generateId("@@@") // Should become "section-1"
        val id3 = context.generateId("Section") // Should become "section-2"
        
        // Assert
        assertEquals("section", id1)
        assertEquals("section-1", id2)
        assertEquals("section-2", id3)
    }
    
    @Test
    fun `should handle very long text`() {
        // Arrange
        val config = RenderConfig.default()
        val context = RenderContext(config)
        val longText = "This is a very long heading that contains many words and should be converted to a long ID"
        
        // Act
        val id = context.generateId(longText)
        
        // Assert
        assertEquals("this-is-a-very-long-heading-that-contains-many-words-and-should-be-converted-to-a-long-id", id)
    }
    
    @Test
    fun `should handle unicode characters by removing them`() {
        // Arrange
        val config = RenderConfig.default()
        val context = RenderContext(config)
        
        // Act
        val id = context.generateId("Hello 世界 World")
        
        // Assert
        assertEquals("hello-world", id)
    }
    
    @Test
    fun `should handle consecutive hyphens in source text`() {
        // Arrange
        val config = RenderConfig.default()
        val context = RenderContext(config)
        
        // Act
        val id = context.generateId("Test--Multiple---Hyphens")
        
        // Assert
        assertEquals("test-multiple-hyphens", id)
    }
}
