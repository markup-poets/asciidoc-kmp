package org.markup.poet.antora

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ResolutionContextTest {
    
    @Test
    fun `should create context with default values`() {
        val context = ResolutionContext(componentRoot = "/docs")
        
        assertEquals("/docs", context.componentRoot)
        assertEquals("ROOT", context.currentModule)
        assertNull(context.currentComponent)
        assertNull(context.currentFilePath)
    }
    
    @Test
    fun `should create context with all values`() {
        val context = ResolutionContext(
            componentRoot = "/docs",
            currentModule = "admin",
            currentComponent = "mycomp",
            currentFilePath = "/docs/modules/admin/pages/index.adoc"
        )
        
        assertEquals("/docs", context.componentRoot)
        assertEquals("admin", context.currentModule)
        assertEquals("mycomp", context.currentComponent)
        assertEquals("/docs/modules/admin/pages/index.adoc", context.currentFilePath)
    }
    
    @Test
    fun `withFile should create new context with updated file path`() {
        val original = ResolutionContext(
            componentRoot = "/docs",
            currentModule = "admin",
            currentComponent = "mycomp",
            currentFilePath = "/docs/modules/admin/pages/index.adoc"
        )
        
        val updated = original.withFile("/docs/modules/admin/pages/other.adoc")
        
        assertEquals("/docs", updated.componentRoot)
        assertEquals("admin", updated.currentModule)
        assertEquals("mycomp", updated.currentComponent)
        assertEquals("/docs/modules/admin/pages/other.adoc", updated.currentFilePath)
        
        // Original should be unchanged
        assertEquals("/docs/modules/admin/pages/index.adoc", original.currentFilePath)
    }
    
    @Test
    fun `withModule should create new context with updated module`() {
        val original = ResolutionContext(
            componentRoot = "/docs",
            currentModule = "admin",
            currentComponent = "mycomp",
            currentFilePath = "/docs/modules/admin/pages/index.adoc"
        )
        
        val updated = original.withModule("api")
        
        assertEquals("/docs", updated.componentRoot)
        assertEquals("api", updated.currentModule)
        assertEquals("mycomp", updated.currentComponent)
        assertEquals("/docs/modules/admin/pages/index.adoc", updated.currentFilePath)
        
        // Original should be unchanged
        assertEquals("admin", original.currentModule)
    }
    
    @Test
    fun `should support chaining context modifications`() {
        val context = ResolutionContext(componentRoot = "/docs")
            .withModule("admin")
            .withFile("/docs/modules/admin/pages/index.adoc")
        
        assertEquals("/docs", context.componentRoot)
        assertEquals("admin", context.currentModule)
        assertEquals("/docs/modules/admin/pages/index.adoc", context.currentFilePath)
    }
}
