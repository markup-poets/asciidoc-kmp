package org.markup.poet.tck.integration

import org.markup.poet.asciidoc.parser.DefaultAsciidocParser
import org.markup.poet.tck.serialization.AstJsonSerializer
import kotlin.test.Test

/**
 * Debug test to check if serializer hangs on strong text.
 */
class DebugSerializerTest {
    
    @Test
    fun `serialize strong text`() {
        println("\n🔍 Debugging serializer with strong text")
        
        val parser = DefaultAsciidocParser()
        val serializer = AstJsonSerializer()
        
        val input = "*s*"
        
        println("Input: '$input'")
        println("\n1. Parsing...")
        
        val result = parser.parse(input)
        println("✅ Parsed successfully")
        
        println("\n2. Serializing with INLINE_ONLY mode...")
        
        try {
            val json = serializer.serialize(
                result.document,
                AstJsonSerializer.Mode.INLINE_ONLY
            )
            
            println("✅ Serialized successfully!")
            println("\nJSON output:")
            println(json)
        } catch (e: Exception) {
            println("💥 Exception: ${e.message}")
            e.printStackTrace()
        }
        
        println("\n✅ Test complete")
    }
}
