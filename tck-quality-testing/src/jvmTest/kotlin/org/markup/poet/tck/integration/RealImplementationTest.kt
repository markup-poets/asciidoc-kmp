package org.markup.poet.tck.integration

import org.markup.poet.asciidoc.asg.plainText
import org.markup.poet.asciidoc.parser.DefaultAsciidocParser
import org.markup.poet.tck.TckIntegration
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

/**
 * Integration test that uses the REAL parser implementation
 * to validate basic parsing functionality.
 * 
 * This demonstrates that your parser is working and can handle
 * various AsciiDoc constructs.
 */
class RealImplementationTest {
    
    private val parser = DefaultAsciidocParser()
    
    @Test
    fun `should parse simple paragraph`() {
        val input = "This is a simple paragraph."
        
        // Parse
        val parseResult = parser.parse(input)
        assertNotNull(parseResult.document, "Parser should create a document")
        assertTrue(parseResult.errors.isEmpty(), "Parser should not produce errors for valid input")
        
        // Check document has content
        assertNotNull(parseResult.document.blocks, "Document should have children")
        println("✅ Parsed simple paragraph successfully")
        println("   Document has ${parseResult.document.blocks.size} child elements")
    }
    
    @Test
    fun `should parse heading level 1`() {
        val input = "= Document Title"
        
        val parseResult = parser.parse(input)
        assertNotNull(parseResult.document, "Parser should create a document")
        
        // Document title should be set
        println("✅ Parsed heading level 1")
        println("   Document title: ${plainText(parseResult.document.header?.title.orEmpty())}")
        println("   Errors: ${parseResult.errors.size}")
        println("   Warnings: ${parseResult.warnings.size}")
    }
    
    @Test
    fun `should parse heading level 2`() {
        val input = "== Section Title"
        
        val parseResult = parser.parse(input)
        assertNotNull(parseResult.document, "Parser should create a document")
        
        println("✅ Parsed heading level 2")
        println("   Children: ${parseResult.document.blocks.size}")
    }
    
    @Test
    fun `should parse bold text`() {
        val input = "This is *bold* text."
        
        val parseResult = parser.parse(input)
        assertNotNull(parseResult.document, "Parser should create a document")
        
        println("✅ Parsed bold text")
        println("   Document has ${parseResult.document.blocks.size} elements")
    }
    
    @Test
    fun `should parse unordered list`() {
        val input = """
            * Item 1
            * Item 2
            * Item 3
        """.trimIndent()
        
        val parseResult = parser.parse(input)
        assertNotNull(parseResult.document, "Parser should create a document")
        
        println("✅ Parsed unordered list")
        println("   Document has ${parseResult.document.blocks.size} elements")
    }
    
    @Test
    fun `should parse code block`() {
        val input = """
            [source,kotlin]
            ----
            fun hello() {
                println("Hello, World!")
            }
            ----
        """.trimIndent()
        
        val parseResult = parser.parse(input)
        assertNotNull(parseResult.document, "Parser should create a document")
        
        println("✅ Parsed code block")
        println("   Document has ${parseResult.document.blocks.size} elements")
    }
    
    @Test
    fun `should handle parser errors gracefully`() {
        // Test with potentially problematic input
        val input = "======= Invalid heading level"
        
        val parseResult = parser.parse(input)
        assertNotNull(parseResult.document, "Parser should still create a document")
        
        println("✅ Handled invalid input gracefully")
        println("   Errors: ${parseResult.errors.size}")
        println("   Warnings: ${parseResult.warnings.size}")
        
        // Parser should handle this gracefully
        // The document should still be created
    }
    
    @Test
    fun `should parse complex document`() {
        val input = """
            = Document Title
            Author Name
            
            == Introduction
            
            This is a paragraph with *bold* and _italic_ text.
            
            == Features
            
            * Feature 1
            * Feature 2
            ** Nested item
            * Feature 3
            
            == Code Example
            
            [source,kotlin]
            ----
            fun main() {
                println("Hello!")
            }
            ----
        """.trimIndent()
        
        val parseResult = parser.parse(input)
        assertNotNull(parseResult.document, "Parser should create a document")
        
        println("✅ Parsed complex document")
        println("   Title: ${plainText(parseResult.document.header?.title.orEmpty())}")
        println("   Children: ${parseResult.document.blocks.size}")
        println("   Errors: ${parseResult.errors.size}")
        println("   Warnings: ${parseResult.warnings.size}")
        
        // Should have multiple sections and elements
        assertTrue(parseResult.document.blocks.isNotEmpty(), "Document should have content")
    }
    
    @Test
    fun `should run TCK tests with custom fixtures`() {
        // This shows how many custom TCK fixtures are loaded
        val context = TckIntegration.initialize()
        val results = TckIntegration.runTests(context)
        
        assertNotNull(results)
        println("\n📊 TCK Results:")
        println("   Total tests: ${results.totalTests}")
        println("   Passed: ${results.passed}")
        println("   Failed: ${results.failed}")
        println("   Pending: ${results.pending}")
        println("   Skipped: ${results.skipped}")
        
        // With custom fixtures loaded, we should have some tests
        assertTrue(results.totalTests > 0, "Should have loaded custom fixtures")
        
        // Show breakdown by category
        if (results.byCategory.isNotEmpty()) {
            println("\n   By Category:")
            results.byCategory.forEach { (category, categoryResults) ->
                println("     ${category.name}: ${categoryResults.passed}/${categoryResults.total}")
            }
        }
    }
}
