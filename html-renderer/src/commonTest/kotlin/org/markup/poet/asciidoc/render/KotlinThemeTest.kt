package org.markup.poet.asciidoc.render

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KotlinThemeTest {
    
    private val theme = KotlinTheme()
    
    @Test
    fun `should provide heading classes with level`() {
        assertEquals("kotlin-heading kotlin-heading-1", theme.headingClasses(1))
        assertEquals("kotlin-heading kotlin-heading-2", theme.headingClasses(2))
        assertEquals("kotlin-heading kotlin-heading-6", theme.headingClasses(6))
    }
    
    @Test
    fun `should provide paragraph classes`() {
        assertEquals("kotlin-paragraph", theme.paragraphClasses())
    }
    
    @Test
    fun `should provide code block classes`() {
        assertEquals("kotlin-code-block", theme.codeBlockClasses())
    }
    
    @Test
    fun `should provide table classes`() {
        assertEquals("kotlin-table", theme.tableClasses())
    }
    
    @Test
    fun `should provide list classes`() {
        assertEquals("kotlin-list", theme.listClasses())
    }
    
    @Test
    fun `should provide quote classes`() {
        assertEquals("kotlin-quote", theme.quoteClasses())
    }
    
    @Test
    fun `should provide admonition classes with type`() {
        assertEquals("kotlin-admonition kotlin-admonition-note", theme.admonitionClasses("note"))
        assertEquals("kotlin-admonition kotlin-admonition-warning", theme.admonitionClasses("warning"))
        assertEquals("kotlin-admonition kotlin-admonition-tip", theme.admonitionClasses("tip"))
    }
    
    @Test
    fun `should generate CSS with dark theme colors`() {
        val css = theme.getCss()
        
        // Check for key color variables
        assertTrue(css.contains("--background: #0A0B0D"))
        assertTrue(css.contains("--foreground: #F2F2F2"))
        assertTrue(css.contains("--primary: #DC2626"))
        assertTrue(css.contains("--primary-glow: #EF4444"))
    }
    
    @Test
    fun `should include typography styles in CSS`() {
        val css = theme.getCss()
        
        assertTrue(css.contains("h1"))
        assertTrue(css.contains("h2"))
        assertTrue(css.contains("font-family"))
        assertTrue(css.contains("font-size"))
    }
    
    @Test
    fun `should include code block styling in CSS`() {
        val css = theme.getCss()
        
        assertTrue(css.contains("pre"))
        assertTrue(css.contains("code"))
        assertTrue(css.contains("var(--font-mono)"))
    }
    
    @Test
    fun `should include table styling in CSS`() {
        val css = theme.getCss()
        
        assertTrue(css.contains("table"))
        assertTrue(css.contains("th"))
        assertTrue(css.contains("td"))
    }
    
    @Test
    fun `should include admonition styling in CSS`() {
        val css = theme.getCss()
        
        assertTrue(css.contains(".admonitionblock"))
        assertTrue(css.contains(".admonitionblock.note"))
        assertTrue(css.contains(".admonitionblock.warning"))
        assertTrue(css.contains(".admonitionblock.tip"))
    }
    
    @Test
    fun `should include responsive design in CSS`() {
        val css = theme.getCss()
        
        assertTrue(css.contains("@media"))
        assertTrue(css.contains("max-width: 768px"))
    }
}
