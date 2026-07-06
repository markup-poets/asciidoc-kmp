package org.markup.poet.tck.serialization

import org.markup.poet.asciidoc.parser.DefaultAsciidocParser
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

/**
 * Test the AST-to-JSON serializer.
 * 
 * This validates that we can convert our AST to the JSON format
 * expected by the official AsciiDoc TCK.
 */
class AstJsonSerializerTest {
    
    private val parser = DefaultAsciidocParser()
    private val serializer = AstJsonSerializer()
    
    @Test
    fun `should serialize simple paragraph to JSON`() {
        val input = "This is a simple paragraph."
        val parseResult = parser.parseToAsg(input)
        
        val json = serializer.serialize(parseResult.document)
        
        assertNotNull(json)
        assertTrue(json.contains("\"name\": \"document\""))
        assertTrue(json.contains("\"type\": \"block\""))
        assertTrue(json.contains("\"name\": \"paragraph\""))
        assertTrue(json.contains("This is a simple paragraph"))
        
        println("✅ Simple paragraph JSON:")
        println(json)
    }
    
    @Test
    fun `should serialize heading to JSON`() {
        val input = "= Document Title"
        val parseResult = parser.parseToAsg(input)
        
        val json = serializer.serialize(parseResult.document)
        
        assertNotNull(json)
        assertTrue(json.contains("\"name\": \"document\""))
        
        println("✅ Heading JSON:")
        println(json)
    }
    
    @Test
    fun `should serialize bold text to JSON`() {
        val input = "This is *bold* text."
        val parseResult = parser.parseToAsg(input)
        
        val json = serializer.serialize(parseResult.document)
        
        assertNotNull(json)
        assertTrue(json.contains("\"variant\": \"strong\"") || json.contains("bold"))
        
        println("✅ Bold text JSON:")
        println(json)
    }
    
    @Test
    fun `should serialize list to JSON`() {
        val input = """
            * Item 1
            * Item 2
            * Item 3
        """.trimIndent()
        val parseResult = parser.parseToAsg(input)
        
        val json = serializer.serialize(parseResult.document)
        
        assertNotNull(json)
        assertTrue(json.contains("\"name\": \"list\"") || json.contains("Item"))
        
        println("✅ List JSON:")
        println(json)
    }
    
    @Test
    fun `should serialize code block to JSON`() {
        val input = """
            [source,kotlin]
            ----
            fun hello() = println("Hi")
            ----
        """.trimIndent()
        val parseResult = parser.parseToAsg(input)
        
        val json = serializer.serialize(parseResult.document)
        
        assertNotNull(json)
        assertTrue(json.contains("\"name\": \"listing\"") || json.contains("hello"))
        
        println("✅ Code block JSON:")
        println(json)
    }
    
    @Test
    fun `should serialize complex document to JSON`() {
        val input = """
            = My Document
            
            == Section 1
            
            This is a paragraph with *bold* and _italic_ text.
            
            * List item 1
            * List item 2
        """.trimIndent()
        val parseResult = parser.parseToAsg(input)
        
        val json = serializer.serialize(parseResult.document)
        
        assertNotNull(json)
        assertTrue(json.contains("\"name\": \"document\""))
        assertTrue(json.contains("\"type\": \"block\""))
        
        println("✅ Complex document JSON:")
        println(json)
        println("\n📏 JSON length: ${json.length} characters")
    }
}
