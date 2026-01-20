package org.markup.poet.asciidoc.render

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Basic test to verify the module setup and configuration classes.
 * 
 * This will be expanded in subsequent tasks.
 */
class HtmlRendererTest {
    
    @Test
    fun `module setup is correct`() {
        // Arrange
        val config = RenderConfig.default()
        
        // Assert
        assertNotNull(config)
        assertNotNull(config.outputOptions)
    }
    
    @Test
    fun `default RenderConfig has expected values`() {
        // Arrange
        val config = RenderConfig.default()
        
        // Assert
        assertTrue(config.outputOptions.standalone)
        assertEquals(CssMode.INLINE, config.outputOptions.cssMode)
        assertEquals("en", config.outputOptions.language)
        assertTrue(config.outputOptions.includeMetadata)
        assertTrue(config.customRenderers.isEmpty())
        assertTrue(config.attributeHandlers.isEmpty())
    }
    
    @Test
    fun `default OutputOptions has expected values`() {
        // Arrange
        val options = OutputOptions.default()
        
        // Assert
        assertTrue(options.standalone)
        assertEquals(CssMode.INLINE, options.cssMode)
        assertEquals(null, options.cssPath)
        assertTrue(options.includeMetadata)
        assertEquals(null, options.documentTitle)
        assertEquals("en", options.language)
        assertTrue(options.customAttributes.isEmpty())
    }
    
    @Test
    fun `can create custom RenderConfig`() {
        // Arrange
        val customOptions = OutputOptions(
            standalone = false,
            cssMode = CssMode.EXTERNAL,
            cssPath = "/styles/main.css",
            includeMetadata = false,
            documentTitle = "Custom Title",
            language = "fr",
            customAttributes = mapOf("data-version" to "1.0")
        )
        val config = RenderConfig(outputOptions = customOptions)
        
        // Assert
        assertEquals(false, config.outputOptions.standalone)
        assertEquals(CssMode.EXTERNAL, config.outputOptions.cssMode)
        assertEquals("/styles/main.css", config.outputOptions.cssPath)
        assertEquals(false, config.outputOptions.includeMetadata)
        assertEquals("Custom Title", config.outputOptions.documentTitle)
        assertEquals("fr", config.outputOptions.language)
        assertEquals("1.0", config.outputOptions.customAttributes["data-version"])
    }
    
    @Test
    fun `CssMode enum has all expected values`() {
        // Assert
        assertEquals(3, CssMode.entries.size)
        assertNotNull(CssMode.NONE)
        assertNotNull(CssMode.INLINE)
        assertNotNull(CssMode.EXTERNAL)
    }
}
