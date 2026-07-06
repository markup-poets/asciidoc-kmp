package org.markup.poet.asciidoc.asg

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import org.markup.poet.asciidoc.parser.DefaultAsciidocParser

/**
 * Property-based tests for ASG structural integrity.
 * **Feature: asciidoc-parser, Property 2: AST Structural Integrity** (ported to the ASG model)
 * **Validates: Requirements 2.1, 2.7**
 *
 * Parses generated AsciiDoc source and verifies the resulting ASG tree holds
 * its structural invariants: a single document root, well-formed locations,
 * non-empty section titles, and lists that always carry at least one item.
 */
class AsgStructuralIntegrityTest : StringSpec({

    val parser = DefaultAsciidocParser()

    "Property 2: ASG Structural Integrity - parsing should yield a single well-formed document root" {
        checkAll(100, asciidocSourceGenerator()) { source ->
            val result = parser.parse(source)
            val document = result.document

            document shouldNotBe null
            document.location?.let(::checkLocation)
            document.header?.let { header ->
                header.title.isNotEmpty().shouldBeTrue()
                header.location?.let(::checkLocation)
            }

            document.blocks.forEach(::checkBlock)
        }
    }

})

// Structural invariants, checked recursively over the whole tree

private fun checkLocation(location: Location) {
    location.start.line shouldBeGreaterThanOrEqual 1
    location.start.col shouldBeGreaterThanOrEqual 0
    location.end.line shouldBeGreaterThanOrEqual location.start.line
    if (location.end.line == location.start.line) {
        location.end.col shouldBeGreaterThanOrEqual 0
    }
}

private fun checkBlock(block: Block) {
    block.location?.let(::checkLocation)
    when (block) {
        is SectionBlock -> {
            block.level shouldBeGreaterThanOrEqual 0
            block.title.isNotEmpty().shouldBeTrue()
            block.title.forEach(::checkInline)
            block.blocks.forEach(::checkBlock)
        }
        is ParentBlock -> block.blocks.forEach(::checkBlock)
        is LeafBlock -> block.inlines.forEach(::checkInline)
        is ListBlock -> {
            block.items.isNotEmpty().shouldBeTrue()
            block.items.forEach { item ->
                item.marker.isNotBlank().shouldBeTrue()
                item.location?.let(::checkLocation)
                item.principal.forEach(::checkInline)
                item.blocks.forEach(::checkBlock)
            }
        }
        is DListBlock -> {
            block.items.isNotEmpty().shouldBeTrue()
            block.items.forEach { item ->
                item.terms.isNotEmpty().shouldBeTrue()
                item.location?.let(::checkLocation)
                item.terms.forEach { term -> term.forEach(::checkInline) }
                item.principal.forEach(::checkInline)
                item.blocks.forEach(::checkBlock)
            }
        }
        else -> Unit // breaks, macros, headings, extension nodes: location already checked
    }
}

private fun checkInline(inline: Inline) {
    inline.location?.let(::checkLocation)
    when (inline) {
        is InlineSpan -> inline.inlines.forEach(::checkInline)
        is InlineRef -> inline.inlines.forEach(::checkInline)
        else -> Unit
    }
}

// Generators producing well-formed AsciiDoc source

private fun asciidocSourceGenerator(): Arb<String> = arbitrary { rs ->
    val header = Arb.choice(
        Arb.constant(emptyList()),
        titleGenerator().map { listOf("= $it", "") }
    ).bind()
    val chunks = Arb.list(chunkGenerator(), 1..6).bind()
    (header + chunks).joinToString("\n")
}

private fun chunkGenerator(): Arb<String> = Arb.choice(
    sectionChunkGenerator(),
    paragraphChunkGenerator(),
    listChunkGenerator(),
    listingChunkGenerator()
)

private fun sectionChunkGenerator(): Arb<String> = arbitrary { rs ->
    val level = Arb.int(2..4).bind()
    val title = titleGenerator().bind()
    "${"=".repeat(level)} $title\n"
}

private fun paragraphChunkGenerator(): Arb<String> = arbitrary { rs ->
    val words = Arb.list(wordGenerator(), 1..8).bind()
    val decorated = Arb.choice(
        Arb.constant(words.joinToString(" ")),
        Arb.constant("*${words.first()}* " + words.drop(1).joinToString(" ")),
        Arb.constant("_${words.first()}_ " + words.drop(1).joinToString(" "))
    ).bind()
    "$decorated\n"
}

private fun listChunkGenerator(): Arb<String> = arbitrary { rs ->
    val items = Arb.list(wordGenerator(), 1..5).bind()
    items.joinToString("\n", postfix = "\n") { "* $it" }
}

private fun listingChunkGenerator(): Arb<String> = arbitrary { rs ->
    val lines = Arb.list(wordGenerator(), 1..3).bind()
    "----\n${lines.joinToString("\n")}\n----\n"
}

private fun titleGenerator(): Arb<String> =
    Arb.list(wordGenerator(), 1..4).map { it.joinToString(" ") }

private fun wordGenerator(): Arb<String> =
    Arb.string(1..10, Codepoint.az())
