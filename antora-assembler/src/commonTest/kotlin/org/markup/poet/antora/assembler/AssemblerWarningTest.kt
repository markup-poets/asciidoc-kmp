package org.markup.poet.antora.assembler

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AssemblerWarningTest {
    
    @Test
    fun `should create warning with all fields`() {
        val warning = AssemblerWarning(
            message = "Image file not found",
            filePath = "docs/page.adoc",
            lineNumber = 30
        )
        
        assertEquals("Image file not found", warning.message)
        assertEquals("docs/page.adoc", warning.filePath)
        assertEquals(30, warning.lineNumber)
    }
    
    @Test
    fun `should create warning with null filePath`() {
        val warning = AssemblerWarning(
            message = "General warning",
            filePath = null,
            lineNumber = null
        )
        
        assertEquals("General warning", warning.message)
        assertNull(warning.filePath)
        assertNull(warning.lineNumber)
    }
    
    @Test
    fun `should create warning with filePath but no line number`() {
        val warning = AssemblerWarning(
            message = "Deprecated syntax used",
            filePath = "docs/old-page.adoc",
            lineNumber = null
        )
        
        assertEquals("Deprecated syntax used", warning.message)
        assertEquals("docs/old-page.adoc", warning.filePath)
        assertNull(warning.lineNumber)
    }
    
    @Test
    fun `should create warning for missing image`() {
        val warning = AssemblerWarning(
            message = "Image 'diagram.png' not found",
            filePath = "docs/modules/ROOT/pages/architecture.adoc",
            lineNumber = 45
        )
        
        assertEquals("Image 'diagram.png' not found", warning.message)
        assertEquals("docs/modules/ROOT/pages/architecture.adoc", warning.filePath)
        assertEquals(45, warning.lineNumber)
    }
    
    @Test
    fun `should create warning for unresolved cross-reference`() {
        val warning = AssemblerWarning(
            message = "Cross-reference target not found: #missing-anchor",
            filePath = "docs/page.adoc",
            lineNumber = 12
        )
        
        assertEquals("Cross-reference target not found: #missing-anchor", warning.message)
        assertEquals(12, warning.lineNumber)
    }
}
