package org.markup.poet.tck.integration

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.markup.poet.asciidoc.parser.DefaultAsciidocParser
import org.markup.poet.tck.serialization.AstJsonSerializer
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verification test for column tracking implementation.
 * 
 * This test verifies that the parser correctly tracks start and end column positions
 * for inline elements, matching the official TCK expectations.
 */
class ColumnTrackingVerificationTest {
    
    private val parser = DefaultAsciidocParser()
    private val serializer = AstJsonSerializer()
    
    @Test
    fun `should track correct column positions for single word`() {
        // Input: "word" (4 characters)
        // Expected location: [{line: 1, col: 1}, {line: 1, col: 5}]
        // - Start: column 1 (first character 'w')
        // - End: column 5 (position after last character 'd')
        
        val input = "word"
        val parseResult = parser.parseToAsg(input)
        val json = serializer.serialize(parseResult.document, AstJsonSerializer.Mode.INLINE_ONLY)
        
        println("Input: '$input'")
        println("JSON Output:")
        println(json)
        
        // Parse JSON to verify structure
        val jsonElement = Json.parseToJsonElement(json)
        val array = jsonElement.jsonArray
        
        assertEquals(1, array.size, "Should have exactly one inline element")
        
        val textElement = array[0].jsonObject
        assertEquals("text", textElement["name"]?.jsonPrimitive?.content)
        assertEquals("word", textElement["value"]?.jsonPrimitive?.content)
        
        // Verify location array
        val location = textElement["location"]?.jsonArray
        assertEquals(2, location?.size, "Location should have start and end positions")
        
        val start = location?.get(0)?.jsonObject
        val end = location?.get(1)?.jsonObject
        
        // Verify start position
        assertEquals(1, start?.get("line")?.jsonPrimitive?.content?.toInt(), "Start line should be 1")
        assertEquals(1, start?.get("col")?.jsonPrimitive?.content?.toInt(), "Start column should be 1")
        
        // Verify end position
        assertEquals(1, end?.get("line")?.jsonPrimitive?.content?.toInt(), "End line should be 1")
        assertEquals(5, end?.get("col")?.jsonPrimitive?.content?.toInt(), "End column should be 5 (after 'word')")
        
        println("\n✅ Column tracking verified:")
        println("   Start: line ${start?.get("line")?.jsonPrimitive?.content}, col ${start?.get("col")?.jsonPrimitive?.content}")
        println("   End:   line ${end?.get("line")?.jsonPrimitive?.content}, col ${end?.get("col")?.jsonPrimitive?.content}")
    }
    
    @Test
    fun `should track correct column positions for bold text`() {
        // Input: "*bold*" (6 characters including markup)
        // Expected location for Strong element: [{line: 1, col: 1}, {line: 1, col: 7}]
        
        val input = "*bold*"
        val parseResult = parser.parseToAsg(input)
        val json = serializer.serialize(parseResult.document, AstJsonSerializer.Mode.INLINE_ONLY)
        
        println("\nInput: '$input'")
        println("JSON Output:")
        println(json)
        
        val jsonElement = Json.parseToJsonElement(json)
        val array = jsonElement.jsonArray
        
        assertEquals(1, array.size, "Should have exactly one inline element")
        
        val strongElement = array[0].jsonObject
        assertEquals("span", strongElement["name"]?.jsonPrimitive?.content)
        assertEquals("strong", strongElement["variant"]?.jsonPrimitive?.content)
        
        // Verify location array
        val location = strongElement["location"]?.jsonArray
        val start = location?.get(0)?.jsonObject
        val end = location?.get(1)?.jsonObject
        
        println("\n✅ Bold text column tracking:")
        println("   Start: line ${start?.get("line")?.jsonPrimitive?.content}, col ${start?.get("col")?.jsonPrimitive?.content}")
        println("   End:   line ${end?.get("line")?.jsonPrimitive?.content}, col ${end?.get("col")?.jsonPrimitive?.content}")
        
        // The strong element should span from column 1 to column 7 (including both * markers)
        assertEquals(1, start?.get("col")?.jsonPrimitive?.content?.toInt(), "Start column should be 1")
        assertEquals(7, end?.get("col")?.jsonPrimitive?.content?.toInt(), "End column should be 7")
    }
    
    @Test
    fun `should track correct column positions for multiple words`() {
        // Input: "hello world" (11 characters)
        // Expected: Single text element spanning columns 1-12
        
        val input = "hello world"
        val parseResult = parser.parseToAsg(input)
        val json = serializer.serialize(parseResult.document, AstJsonSerializer.Mode.INLINE_ONLY)
        
        println("\nInput: '$input'")
        
        val jsonElement = Json.parseToJsonElement(json)
        val array = jsonElement.jsonArray
        
        val textElement = array[0].jsonObject
        val location = textElement["location"]?.jsonArray
        val start = location?.get(0)?.jsonObject
        val end = location?.get(1)?.jsonObject
        
        println("✅ Multiple words column tracking:")
        println("   Start: line ${start?.get("line")?.jsonPrimitive?.content}, col ${start?.get("col")?.jsonPrimitive?.content}")
        println("   End:   line ${end?.get("line")?.jsonPrimitive?.content}, col ${end?.get("col")?.jsonPrimitive?.content}")
        
        assertEquals(1, start?.get("col")?.jsonPrimitive?.content?.toInt())
        assertEquals(12, end?.get("col")?.jsonPrimitive?.content?.toInt())
    }
}
