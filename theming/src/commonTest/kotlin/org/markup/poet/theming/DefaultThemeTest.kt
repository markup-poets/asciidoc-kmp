package org.markup.poet.theming

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DefaultThemeTest {
    
    @Test
    fun `should have correct theme metadata`() {
        val theme = DefaultTheme()
        
        assertEquals("default", theme.id)
        assertEquals("Default", theme.name)
        assertNotNull(theme.description)
    }
    
    @Test
    fun `should return heading classes with level`() {
        val theme = DefaultTheme()
        
        val style1 = theme.getStyle(ElementType.HEADING, StyleContext.heading(1))
        assertEquals(listOf("heading", "heading-1"), style1.classes)
        
        val style2 = theme.getStyle(ElementType.HEADING, StyleContext.heading(2))
        assertEquals(listOf("heading", "heading-2"), style2.classes)
        
        val style6 = theme.getStyle(ElementType.HEADING, StyleContext.heading(6))
        assertEquals(listOf("heading", "heading-6"), style6.classes)
    }
    
    @Test
    fun `should return paragraph classes`() {
        val theme = DefaultTheme()
        val style = theme.getStyle(ElementType.PARAGRAPH)
        
        assertEquals(listOf("paragraph"), style.classes)
    }
    
    @Test
    fun `should return code block classes`() {
        val theme = DefaultTheme()
        val style = theme.getStyle(ElementType.CODE_BLOCK)
        
        assertEquals(listOf("code-block"), style.classes)
    }
    
    @Test
    fun `should return admonition classes with type`() {
        val theme = DefaultTheme()
        
        val noteStyle = theme.getStyle(ElementType.ADMONITION, StyleContext.admonition("note"))
        assertEquals(listOf("admonition", "admonition-note"), noteStyle.classes)
        
        val warningStyle = theme.getStyle(ElementType.ADMONITION, StyleContext.admonition("warning"))
        assertEquals(listOf("admonition", "admonition-warning"), warningStyle.classes)
    }
    
    @Test
    fun `should return CSS stylesheet`() {
        val theme = DefaultTheme()
        val css = theme.getStylesheet("css")
        
        assertNotNull(css)
        assertTrue(css.contains(":root"))
        assertTrue(css.contains("--mp-color-primary"))
        assertTrue(css.contains(".heading"))
        assertTrue(css.contains(".paragraph"))
        assertTrue(css.contains(".code-block"))
        assertTrue(css.contains(".admonition"))
    }
    
    @Test
    fun `should return null for unsupported format`() {
        val theme = DefaultTheme()
        val result = theme.getStylesheet("pdf")
        
        assertEquals(null, result)
    }
    
    @Test
    fun `should handle case-insensitive format`() {
        val theme = DefaultTheme()
        
        val css1 = theme.getStylesheet("css")
        val css2 = theme.getStylesheet("CSS")
        val css3 = theme.getStylesheet("Css")
        
        assertNotNull(css1)
        assertNotNull(css2)
        assertNotNull(css3)
    }
    
    @Test
    fun `should return empty style for unsupported element types`() {
        val theme = DefaultTheme()
        val style = theme.getStyle(ElementType.DOCUMENT)
        
        assertEquals(ElementStyle.empty(), style)
    }
}
