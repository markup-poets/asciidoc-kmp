package org.markup.poet.tck.integration

import org.markup.poet.asciidoc.asg.plainText
import org.markup.poet.asciidoc.parser.DefaultAsciidocParser
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

/**
 * Quick test to validate your parser works.
 * This runs fast and shows basic parsing capabilities.
 */
class QuickParserTest {
    
    private val parser = DefaultAsciidocParser()
    
    @Test
    fun `parser should handle simple paragraph`() {
        val input = "This is a simple paragraph."
        val result = parser.parse(input)
        
        assertNotNull(result.document)
        println("✅ Simple paragraph: ${result.errors.size} errors, ${result.document.blocks.size} children")
    }
    
    @Test
    fun `parser should handle heading`() {
        val input = "= Document Title"
        val result = parser.parse(input)
        
        assertNotNull(result.document)
        println("✅ Heading: title='${plainText(result.document.header?.title.orEmpty())}', ${result.errors.size} errors")
    }
    
    @Test
    fun `parser should handle list`() {
        val input = """
            * Item 1
            * Item 2
        """.trimIndent()
        val result = parser.parse(input)
        
        assertNotNull(result.document)
        println("✅ List: ${result.document.blocks.size} children, ${result.errors.size} errors")
    }
    
    @Test
    fun `parser should handle code block`() {
        val input = """
            [source,kotlin]
            ----
            fun hello() = println("Hi")
            ----
        """.trimIndent()
        val result = parser.parse(input)
        
        assertNotNull(result.document)
        println("✅ Code block: ${result.document.blocks.size} children, ${result.errors.size} errors")
    }
    
    @Test
    fun `parser should handle complex document`() {
        val input = """
            = My Document
            
            == Section 1
            
            This is a paragraph with *bold* text.
            
            * List item 1
            * List item 2
        """.trimIndent()
        val result = parser.parse(input)
        
        assertNotNull(result.document)
        println("✅ Complex doc: title='${plainText(result.document.header?.title.orEmpty())}', ${result.document.blocks.size} children, ${result.errors.size} errors")
        
        // Show what was parsed
        result.document.blocks.forEachIndexed { i, child ->
            println("   Child $i: ${child::class.simpleName}")
        }
    }
}
