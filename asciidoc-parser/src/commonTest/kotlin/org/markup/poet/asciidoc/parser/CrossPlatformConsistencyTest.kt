package org.markup.poet.asciidoc.parser

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import org.markup.poet.asciidoc.ast.*

/**
 * Property-based tests for cross-platform consistency.
 * **Feature: asciidoc-parser, Property 14: Cross-Platform Consistency**
 * **Validates: Requirements 8.3, 8.4**
 */
class CrossPlatformConsistencyTest : StringSpec({

    val parser = DefaultAsciidocParser()

    "simple test" {
        val result = parser.parse("= Test\n\nHello world")
        result.document shouldNotBe null
    }

    "Property 14: Cross-Platform Consistency - Parser should produce identical AST results across all platforms using platform-neutral operations" {
        checkAll(10, asciidocDocumentGenerator()) { document ->
            // Parse the same document multiple times to ensure consistency
            val result1 = parser.parse(document)
            val result2 = parser.parse(document)
            val result3 = parser.parse(document.lines())
            
            // All parsing results should be identical
            result1.document shouldBe result2.document
            result1.document shouldBe result3.document
            result1.errors shouldBe result2.errors
            result1.errors shouldBe result3.errors
            result1.warnings shouldBe result2.warnings
            result1.warnings shouldBe result3.warnings
        }
    }

})

// Generators for cross-platform testing
private fun asciidocDocumentGenerator(): Arb<String> = arbitrary { rs ->
    val sections = Arb.list(sectionGenerator(), 0..2).bind()
    val paragraphs = Arb.list(paragraphLineGenerator(), 0..3).bind()
    
    (sections + paragraphs).joinToString("\n")
}

private fun sectionGenerator(): Arb<String> = arbitrary { rs ->
    val level = Arb.int(1..2).bind()
    val title = Arb.string(5..20).filter { it.isNotBlank() }.bind()
    "${"=".repeat(level)} $title"
}

private fun paragraphLineGenerator(): Arb<String> = 
    Arb.string(10..50).filter { it.isNotBlank() && !it.startsWith("=") && !it.startsWith("*") && !it.startsWith("-") }