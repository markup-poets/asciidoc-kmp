package org.markup.poet.antora.assembler

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AssemblerConfigTest {
    
    @Test
    fun `should create config with required parameters`() {
        val config = AssemblerConfig(
            indexFile = "docs/index.adoc",
            outputFile = "output/assembled.adoc",
            componentRoot = "docs"
        )
        
        assertEquals("docs/index.adoc", config.indexFile)
        assertEquals("output/assembled.adoc", config.outputFile)
        assertEquals("docs", config.componentRoot)
    }
    
    @Test
    fun `should use default values for optional parameters`() {
        val config = AssemblerConfig(
            indexFile = "index.adoc",
            outputFile = "output.adoc",
            componentRoot = "."
        )
        
        assertEquals(50, config.maxDepth)
        assertTrue(config.preserveComments)
        assertTrue(config.failOnMissingIncludes)
        assertTrue(config.failOnCircularDependencies)
    }
    
    @Test
    fun `should allow custom maxDepth`() {
        val config = AssemblerConfig(
            indexFile = "index.adoc",
            outputFile = "output.adoc",
            componentRoot = ".",
            maxDepth = 100
        )
        
        assertEquals(100, config.maxDepth)
    }
    
    @Test
    fun `should allow disabling comment preservation`() {
        val config = AssemblerConfig(
            indexFile = "index.adoc",
            outputFile = "output.adoc",
            componentRoot = ".",
            preserveComments = false
        )
        
        assertFalse(config.preserveComments)
    }
    
    @Test
    fun `should allow disabling fail on missing includes`() {
        val config = AssemblerConfig(
            indexFile = "index.adoc",
            outputFile = "output.adoc",
            componentRoot = ".",
            failOnMissingIncludes = false
        )
        
        assertFalse(config.failOnMissingIncludes)
    }
    
    @Test
    fun `should allow disabling fail on circular dependencies`() {
        val config = AssemblerConfig(
            indexFile = "index.adoc",
            outputFile = "output.adoc",
            componentRoot = ".",
            failOnCircularDependencies = false
        )
        
        assertFalse(config.failOnCircularDependencies)
    }
    
    @Test
    fun `should support all custom parameters together`() {
        val config = AssemblerConfig(
            indexFile = "docs/modules/ROOT/pages/index.adoc",
            outputFile = "build/output/assembled-doc.adoc",
            componentRoot = "docs",
            maxDepth = 25,
            preserveComments = false,
            failOnMissingIncludes = false,
            failOnCircularDependencies = false
        )
        
        assertEquals("docs/modules/ROOT/pages/index.adoc", config.indexFile)
        assertEquals("build/output/assembled-doc.adoc", config.outputFile)
        assertEquals("docs", config.componentRoot)
        assertEquals(25, config.maxDepth)
        assertFalse(config.preserveComments)
        assertFalse(config.failOnMissingIncludes)
        assertFalse(config.failOnCircularDependencies)
    }
}
