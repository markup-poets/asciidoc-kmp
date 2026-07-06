package org.markup.poet.examples

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.markup.poet.theming.*

class KotlinThemeTest {
    
    @Test
    fun `should have correct theme metadata`() {
        val theme = KotlinTheme()
        
        assertEquals("kotlin", theme.id)
        assertEquals("Kotlin", theme.name)
        assertNotNull(theme.description)
    }
    
    @Test
    fun `should return kotlin-prefixed heading classes`() {
        val theme = KotlinTheme()
        
        val style1 = theme.getStyle(ElementType.HEADING, StyleContext.heading(1))
        assertEquals(listOf("kotlin-heading", "kotlin-heading-1"), style1.classes)
        
        val style3 = theme.getStyle(ElementType.HEADING, StyleContext.heading(3))
        assertEquals(listOf("kotlin-heading", "kotlin-heading-3"), style3.classes)
    }
    
    @Test
    fun `should return kotlin-prefixed paragraph classes`() {
        val theme = KotlinTheme()
        val style = theme.getStyle(ElementType.PARAGRAPH)
        
        assertEquals(listOf("kotlin-paragraph"), style.classes)
    }
    
    @Test
    fun `should return kotlin-prefixed code block classes`() {
        val theme = KotlinTheme()
        val style = theme.getStyle(ElementType.CODE_BLOCK)
        
        assertEquals(listOf("kotlin-code-block"), style.classes)
    }
    
    @Test
    fun `should return kotlin-prefixed admonition classes with type`() {
        val theme = KotlinTheme()
        
        val noteStyle = theme.getStyle(ElementType.ADMONITION, StyleContext.admonition("note"))
        assertEquals(listOf("kotlin-admonition", "kotlin-admonition-note"), noteStyle.classes)
        
        val tipStyle = theme.getStyle(ElementType.ADMONITION, StyleContext.admonition("tip"))
        assertEquals(listOf("kotlin-admonition", "kotlin-admonition-tip"), tipStyle.classes)
    }
    
    @Test
    fun `should return CSS stylesheet with dark theme colors`() {
        val theme = KotlinTheme()
        val css = theme.getStylesheet("css")
        
        assertNotNull(css)
        assertTrue(css.contains("--kotlin-background: #0A0B0D"))
        assertTrue(css.contains("--kotlin-primary: #DC2626"))
        assertTrue(css.contains(".kotlin-heading"))
        assertTrue(css.contains(".kotlin-paragraph"))
        assertTrue(css.contains(".kotlin-code-block"))
    }
    
    @Test
    fun `should include responsive design rules`() {
        val theme = KotlinTheme()
        val css = theme.getStylesheet("css")
        
        assertNotNull(css)
        assertTrue(css.contains("@media (max-width: 768px)"))
    }
    
    @Test
    fun `should include scrollbar styling`() {
        val theme = KotlinTheme()
        val css = theme.getStylesheet("css")
        
        assertNotNull(css)
        assertTrue(css.contains("::-webkit-scrollbar"))
    }
    
    @Test
    fun `should include selection styling`() {
        val theme = KotlinTheme()
        val css = theme.getStylesheet("css")
        
        assertNotNull(css)
        assertTrue(css.contains("::selection"))
    }
    
    @Test
    fun `should return null for unsupported format`() {
        val theme = KotlinTheme()
        val result = theme.getStylesheet("pdf")
        
        assertEquals(null, result)
    }
    
    @Test
    fun `should be registrable in ThemeRegistry`() {
        val theme = KotlinTheme()
        ThemeRegistry.registerOrReplace(theme)
        
        val retrieved = ThemeRegistry.get("kotlin")
        assertNotNull(retrieved)
        assertEquals("kotlin", retrieved.id)
    }
}
