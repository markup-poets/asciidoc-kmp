package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.asg.ConditionalBlock
import org.markup.poet.asciidoc.asg.IncludeBlock
import org.markup.poet.asciidoc.asg.InlineText
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.parser.DefaultAsciidocParser
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * End-to-end proof that directives parsed by the ASG parser core flow through
 * the document-processing resolvers: `include::` lines are parsed into
 * [IncludeBlock]s that [DefaultIncludeResolver] resolves against real files,
 * and `ifdef::` regions are parsed into [ConditionalBlock]s that
 * [DefaultConditionalProcessor] keeps or drops per attribute.
 */
class DirectiveEndToEndJvmTest {

    private val parser = DefaultAsciidocParser()

    private fun paragraphTexts(blocks: List<org.markup.poet.asciidoc.asg.Block>): List<String> =
        blocks.filterIsInstance<LeafBlock>().map { block ->
            block.inlines.filterIsInstance<InlineText>().joinToString("") { it.value }
        }

    @Test
    fun `include directive resolves against a real file`() {
        val dir = Files.createTempDirectory("mp-include-e2e").toFile()
        try {
            val included = dir.resolve("part.adoc")
            included.writeText("included paragraph")
            val main = "before include\n\ninclude::part.adoc[]\n\nafter include"

            val parsed = parser.parse(main)
            assertIs<IncludeBlock>(parsed.document.blocks[1]) // the parser emits the directive node

            val resolver = DefaultIncludeResolver(parser)
            val result = resolver.resolve(
                parsed.document,
                IncludeConfig(maxDepth = 10, basePath = dir.absolutePath, fileReader = JvmFileReader()),
            )

            assertTrue(result.errors.isEmpty(), "unexpected errors: ${result.errors}")
            assertEquals(setOf(included.absolutePath), result.includedFiles.map { java.io.File(it).absolutePath }.toSet())
            assertEquals(
                listOf("before include", "included paragraph", "after include"),
                paragraphTexts(result.document.blocks),
            )
            assertTrue(result.document.blocks.none { it is IncludeBlock })
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `include directive with lines attribute embeds only the requested range`() {
        val dir = Files.createTempDirectory("mp-include-lines-e2e").toFile()
        try {
            dir.resolve("long.adoc").writeText("line one\n\nline three\n\nline five")
            val parsed = parser.parse("include::long.adoc[lines=3..3]")

            val resolver = DefaultIncludeResolver(parser)
            val result = resolver.resolve(
                parsed.document,
                IncludeConfig(maxDepth = 10, basePath = dir.absolutePath, fileReader = JvmFileReader()),
            )

            assertTrue(result.errors.isEmpty(), "unexpected errors: ${result.errors}")
            assertEquals(listOf("line three"), paragraphTexts(result.document.blocks))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `ifdef region is kept when the attribute is defined`() {
        val parsed = parser.parse("ifdef::flag[]\nsecret content\nendif::[]\n\nalways visible")
        assertIs<ConditionalBlock>(parsed.document.blocks[0])

        val result = DefaultConditionalProcessor().process(
            parsed.document,
            ConditionalConfig(definedAttributes = setOf("flag")),
        )

        assertEquals(1, result.evaluatedConditionals)
        assertEquals(listOf("secret content", "always visible"), paragraphTexts(result.document.blocks))
    }

    @Test
    fun `ifdef region is dropped when the attribute is not defined`() {
        val parsed = parser.parse("ifdef::flag[]\nsecret content\nendif::[]\n\nalways visible")

        val result = DefaultConditionalProcessor().process(
            parsed.document,
            ConditionalConfig(definedAttributes = emptySet()),
        )

        assertEquals(listOf("always visible"), paragraphTexts(result.document.blocks))
    }

    @Test
    fun `ifndef region is dropped when the attribute is defined`() {
        val parsed = parser.parse("ifndef::flag[]\nfallback\nendif::[]\n\nbody")

        val defined = DefaultConditionalProcessor().process(
            parsed.document,
            ConditionalConfig(definedAttributes = setOf("flag")),
        )
        assertEquals(listOf("body"), paragraphTexts(defined.document.blocks))

        val undefined = DefaultConditionalProcessor().process(
            parsed.document,
            ConditionalConfig(definedAttributes = emptySet()),
        )
        assertEquals(listOf("fallback", "body"), paragraphTexts(undefined.document.blocks))
    }
}
