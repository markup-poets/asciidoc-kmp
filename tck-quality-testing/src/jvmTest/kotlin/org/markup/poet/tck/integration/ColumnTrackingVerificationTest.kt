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
 * for inline elements, matching the official TCK expectations: ASG locations are
 * 1-based with END-INCLUSIVE columns (the end position is the column of the last
 * character, not the position after it).
 */
class ColumnTrackingVerificationTest {

    private val parser = DefaultAsciidocParser()
    private val serializer = AstJsonSerializer()

    @Test
    fun `should track correct column positions for single word`() {
        // Input: "word" (4 characters)
        // Expected location: [{line: 1, col: 1}, {line: 1, col: 4}]
        // - Start: column 1 (first character 'w')
        // - End: column 4 (last character 'd', end-inclusive)

        val input = "word"
        val parseResult = parser.parse(input)
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
        assertEquals(4, end?.get("col")?.jsonPrimitive?.content?.toInt(), "End column should be 4 (last char of 'word', end-inclusive)")

        println("\nColumn tracking verified:")
        println("   Start: line ${start?.get("line")?.jsonPrimitive?.content}, col ${start?.get("col")?.jsonPrimitive?.content}")
        println("   End:   line ${end?.get("line")?.jsonPrimitive?.content}, col ${end?.get("col")?.jsonPrimitive?.content}")
    }

    @Test
    fun `should track correct column positions for bold text`() {
        // Input: "*bold*" (6 characters including markup)
        // Expected location for Strong element: [{line: 1, col: 1}, {line: 1, col: 6}]
        // (end-inclusive: column 6 is the closing '*')

        val input = "*bold*"
        val parseResult = parser.parse(input)
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

        println("\nBold text column tracking:")
        println("   Start: line ${start?.get("line")?.jsonPrimitive?.content}, col ${start?.get("col")?.jsonPrimitive?.content}")
        println("   End:   line ${end?.get("line")?.jsonPrimitive?.content}, col ${end?.get("col")?.jsonPrimitive?.content}")

        // The strong element spans columns 1 to 6, both '*' markers included (end-inclusive).
        assertEquals(1, start?.get("col")?.jsonPrimitive?.content?.toInt(), "Start column should be 1")
        assertEquals(6, end?.get("col")?.jsonPrimitive?.content?.toInt(), "End column should be 6")
    }

    @Test
    fun `should track correct column positions for multiple words`() {
        // Input: "hello world" (11 characters)
        // Expected: Single text element spanning columns 1-11 (end-inclusive)

        val input = "hello world"
        val parseResult = parser.parse(input)
        val json = serializer.serialize(parseResult.document, AstJsonSerializer.Mode.INLINE_ONLY)

        println("\nInput: '$input'")

        val jsonElement = Json.parseToJsonElement(json)
        val array = jsonElement.jsonArray

        val textElement = array[0].jsonObject
        val location = textElement["location"]?.jsonArray
        val start = location?.get(0)?.jsonObject
        val end = location?.get(1)?.jsonObject

        println("Multiple words column tracking:")
        println("   Start: line ${start?.get("line")?.jsonPrimitive?.content}, col ${start?.get("col")?.jsonPrimitive?.content}")
        println("   End:   line ${end?.get("line")?.jsonPrimitive?.content}, col ${end?.get("col")?.jsonPrimitive?.content}")

        assertEquals(1, start?.get("col")?.jsonPrimitive?.content?.toInt())
        assertEquals(11, end?.get("col")?.jsonPrimitive?.content?.toInt())
    }
}
