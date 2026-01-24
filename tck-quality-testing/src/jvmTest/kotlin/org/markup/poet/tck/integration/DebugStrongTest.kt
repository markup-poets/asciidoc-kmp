package org.markup.poet.tck.integration

import org.markup.poet.asciidoc.parser.DefaultAsciidocParser
import kotlin.test.Test

/**
 * Debug test to reproduce the strong parsing hang.
 */
class DebugStrongTest {
    
    @Test
    fun `parse simple strong text`() {
        println("\n🔍 Debugging strong text parsing")
        
        val parser = DefaultAsciidocParser()
        val input = "*s*"
        
        println("Input: '$input'")
        println("Length: ${input.length}")
        println("Chars: ${input.toCharArray().joinToString(", ") { "'$it'" }}")
        
        println("\nParsing...")
        
        try {
            val result = parser.parse(input)
            println("✅ Parsed successfully!")
            println("Document children: ${result.document.children.size}")
            
            result.document.children.forEach { child ->
                println("  - ${child::class.simpleName}")
            }
        } catch (e: Exception) {
            println("💥 Exception: ${e.message}")
            e.printStackTrace()
        }
        
        println("\n✅ Test complete")
    }
}
