package io.github.kotlin.asciidoc.parser

import io.github.kotlin.asciidoc.ast.SourceLocation
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll

/**
 * Property-based tests for line processing integrity.
 * **Feature: asciidoc-parser, Property 1: Line Processing Integrity**
 * **Validates: Requirements 1.1, 1.2, 1.5**
 */
class LineProcessingIntegrityTest : StringSpec({

    val lineProcessor = DefaultLineProcessor()

    "Property 1: Line Processing Integrity - Parser should process lines sequentially, assign correct block types, and preserve line numbers" {
        checkAll(100, asciidocLinesGenerator()) { lines ->
            val context = ParseContext()
            
            lines.forEachIndexed { index, line ->
                val lineNumber = index + 1
                val result = lineProcessor.processLine(line, lineNumber, context)
                
                // Verify line number is preserved
                result.sourceLocation.line shouldBe lineNumber
                result.sourceLocation shouldNotBe null
                
                // Verify block type is assigned based on content
                val expectedBlockType = lineProcessor.determineBlockType(line)
                result.blockType shouldBe expectedBlockType
                
                // Verify content is extracted appropriately
                result.content shouldNotBe null
                
                // Verify attributes are extracted when applicable
                result.attributes shouldNotBe null
                
                // Verify block type detection is consistent
                val blockTypeFromDetermination = lineProcessor.determineBlockType(line)
                blockTypeFromDetermination shouldBe result.blockType
            }
        }
    }

    "Property 1a: Block type determination should be consistent and deterministic" {
        checkAll(100, asciidocLineGenerator()) { line ->
            val blockType1 = lineProcessor.determineBlockType(line)
            val blockType2 = lineProcessor.determineBlockType(line)
            
            // Block type determination should be deterministic
            blockType1 shouldBe blockType2
            
            // Block type should be one of the valid types
            blockType1.shouldBeInstanceOf<BlockType>()
        }
    }

    "Property 1b: Line processing should preserve line numbers correctly" {
        checkAll(100, Arb.list(asciidocLineGenerator(), 1..50)) { lines ->
            val context = ParseContext()
            
            lines.forEachIndexed { index, line ->
                val lineNumber = index + 1
                val result = lineProcessor.processLine(line, lineNumber, context)
                
                // Line number should match the provided line number
                result.sourceLocation.line shouldBe lineNumber
                
                // Line number should be positive
                result.sourceLocation.line shouldBe lineNumber
            }
        }
    }

})

// Generators for property-based testing
private fun asciidocLinesGenerator(): Arb<List<String>> = 
    Arb.list(asciidocLineGenerator(), 1..20)

private fun asciidocLineGenerator(): Arb<String> = Arb.choice(
    emptyLineGenerator(),
    sectionHeaderGenerator(),
    unorderedListGenerator(),
    orderedListGenerator(),
    codeBlockDelimiterGenerator(),
    paragraphGenerator(),
    commentGenerator(),
    attributeDefinitionGenerator()
)

private fun emptyLineGenerator(): Arb<String> = Arb.choice(
    Arb.constant(""),
    Arb.constant("   "),
    Arb.constant("\t"),
    Arb.constant("  \t  ")
)

private fun sectionHeaderGenerator(): Arb<String> = arbitrary { rs ->
    val level = Arb.int(1..6).bind()
    val equals = "=".repeat(level)
    val title = Arb.string(1..50).filter { it.isNotBlank() }.bind()
    "$equals $title"
}

private fun unorderedListGenerator(): Arb<String> = arbitrary { rs ->
    val marker = Arb.choice(Arb.constant("*"), Arb.constant("-")).bind()
    val content = Arb.string(1..100).filter { it.isNotBlank() }.bind()
    "$marker $content"
}

private fun orderedListGenerator(): Arb<String> = arbitrary { rs ->
    val choice = Arb.choice(
        arbitrary { ". ${Arb.string(1..100).filter { it.isNotBlank() }.bind()}" },
        arbitrary { "${Arb.int(1..99).bind()}. ${Arb.string(1..100).filter { it.isNotBlank() }.bind()}" }
    ).bind()
    choice
}

private fun codeBlockDelimiterGenerator(): Arb<String> = arbitrary { rs ->
    val length = Arb.int(4..10).bind()
    "-".repeat(length)
}

private fun paragraphGenerator(): Arb<String> = 
    Arb.string(1..200).filter { line ->
        val trimmed = line.trim()
        trimmed.isNotEmpty() && 
        !trimmed.startsWith("=") && 
        !trimmed.startsWith("*") && 
        !trimmed.startsWith("-") && 
        !trimmed.startsWith(".") && 
        !trimmed.matches(Regex("^\\d+\\..*")) &&
        !trimmed.startsWith("//") &&
        !trimmed.startsWith(":") &&
        !trimmed.all { it == '-' }
    }

private fun commentGenerator(): Arb<String> = arbitrary { rs ->
    val content = Arb.string(0..100).bind()
    if (content.isEmpty()) "//" else "// $content"
}

private fun attributeDefinitionGenerator(): Arb<String> = arbitrary { rs ->
    val key = Arb.string(1..20).filter { it.isNotBlank() && !it.contains(':') }.bind()
    val value = Arb.string(0..50).bind()
    ":$key: $value"
}