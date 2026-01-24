package org.markup.poet.theming

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import kotlin.test.BeforeTest

class ThemeRegistryTest {
    
    @BeforeTest
    fun setup() {
        // Clear registry before each test (except default theme)
        ThemeRegistry.clear()
    }
    
    @Test
    fun `should have default theme registered`() {
        val theme = ThemeRegistry.get("default")
        
        assertNotNull(theme)
        assertEquals("default", theme.id)
    }
    
    @Test
    fun `should register custom theme`() {
        val customTheme = TestTheme("custom", "Custom Theme")
        ThemeRegistry.register(customTheme)
        
        val retrieved = ThemeRegistry.get("custom")
        assertNotNull(retrieved)
        assertEquals("custom", retrieved.id)
        assertEquals("Custom Theme", retrieved.name)
    }
    
    @Test
    fun `should throw exception when registering duplicate theme`() {
        val theme1 = TestTheme("duplicate", "Theme 1")
        val theme2 = TestTheme("duplicate", "Theme 2")
        
        ThemeRegistry.register(theme1)
        
        assertFailsWith<IllegalArgumentException> {
            ThemeRegistry.register(theme2)
        }
    }
    
    @Test
    fun `should replace theme with registerOrReplace`() {
        val theme1 = TestTheme("replaceable", "Theme 1")
        val theme2 = TestTheme("replaceable", "Theme 2")
        
        ThemeRegistry.register(theme1)
        ThemeRegistry.registerOrReplace(theme2)
        
        val retrieved = ThemeRegistry.get("replaceable")
        assertNotNull(retrieved)
        assertEquals("Theme 2", retrieved.name)
    }
    
    @Test
    fun `should return null for non-existent theme`() {
        val theme = ThemeRegistry.get("non-existent")
        
        assertNull(theme)
    }
    
    @Test
    fun `should return default theme for non-existent theme with getOrDefault`() {
        val theme = ThemeRegistry.getOrDefault("non-existent")
        
        assertNotNull(theme)
        assertEquals("default", theme.id)
    }
    
    @Test
    fun `should list all registered themes`() {
        ThemeRegistry.register(TestTheme("theme1", "Theme 1"))
        ThemeRegistry.register(TestTheme("theme2", "Theme 2"))
        
        val themeIds = ThemeRegistry.listThemes()
        
        assertTrue(themeIds.contains("default"))
        assertTrue(themeIds.contains("theme1"))
        assertTrue(themeIds.contains("theme2"))
    }
    
    @Test
    fun `should get all registered themes`() {
        ThemeRegistry.register(TestTheme("theme1", "Theme 1"))
        ThemeRegistry.register(TestTheme("theme2", "Theme 2"))
        
        val themes = ThemeRegistry.getAllThemes()
        
        assertTrue(themes.size >= 3) // default + 2 custom
        assertTrue(themes.any { it.id == "default" })
        assertTrue(themes.any { it.id == "theme1" })
        assertTrue(themes.any { it.id == "theme2" })
    }
    
    @Test
    fun `should unregister theme`() {
        val theme = TestTheme("removable", "Removable Theme")
        ThemeRegistry.register(theme)
        
        val removed = ThemeRegistry.unregister("removable")
        assertTrue(removed)
        
        val retrieved = ThemeRegistry.get("removable")
        assertNull(retrieved)
    }
    
    @Test
    fun `should return false when unregistering non-existent theme`() {
        val removed = ThemeRegistry.unregister("non-existent")
        
        assertEquals(false, removed)
    }
    
    @Test
    fun `should clear all themes except default`() {
        ThemeRegistry.register(TestTheme("theme1", "Theme 1"))
        ThemeRegistry.register(TestTheme("theme2", "Theme 2"))
        
        ThemeRegistry.clear()
        
        val themes = ThemeRegistry.listThemes()
        assertEquals(1, themes.size)
        assertEquals("default", themes[0])
    }
}

/**
 * Test theme implementation for testing purposes.
 */
private class TestTheme(
    override val id: String,
    override val name: String
) : Theme {
    override fun getStyle(element: ElementType, context: StyleContext): ElementStyle {
        return ElementStyle.withClasses("test-${element.name.lowercase()}")
    }
    
    override fun getStylesheet(format: String): String? {
        return if (format == "css") "/* Test CSS */" else null
    }
}
