package org.markup.poet.asciidoc.asg.serialization

import org.markup.poet.asciidoc.asg.BlockMacro
import org.markup.poet.asciidoc.asg.BlockMacroName
import org.markup.poet.asciidoc.asg.InlineSpan
import org.markup.poet.asciidoc.asg.InlineText
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.LeafBlockName
import org.markup.poet.asciidoc.asg.SpanVariant
import org.markup.poet.asciidoc.parser.DefaultAsciidocParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

/**
 * The deserializer must decode everything [AsgDocumentJsonSerializer] emits:
 * `serialize -> deserialize -> serialize` is byte-identical for parsed
 * documents covering every construct with an official ASG form.
 */
class AsgDocumentJsonDeserializerTest {

    private val serializer = AsgDocumentJsonSerializer()
    private val deserializer = AsgDocumentJsonDeserializer()

    private val sampleDocuments = listOf(
        // Header, attributes, sections, spans, refs.
        """
        = Round Trip
        :author: Poet
        :toc: left

        == First *strong* section

        A paragraph with _emphasis_, `code`, #mark#, and
        a link https://example.com[Example] across lines.

        === Nested

        More text.
        """.trimIndent(),
        // Lists, nested lists, dlists.
        """
        * one
        * two
        ** two point one
        * three

        . first
        . second

        term one:: description one
        term two:: description two
        """.trimIndent(),
        // Verbatim blocks, admonitions, containers, quotes.
        """
        [source,kotlin]
        ----
        fun main() = println("hi")
        ----

        ....
        literal text
        ....

        NOTE: An admonition paragraph.

        [IMPORTANT]
        ====
        Delimited admonition.
        ====

        ****
        A sidebar.
        ****

        [quote, Someone, Somewhere]
        ____
        Quoted words.
        ____

        --
        Open block content.
        --
        """.trimIndent(),
        // Block macros, breaks, discrete headings, metadata shorthand.
        """
        image::diagram.svg[Diagram,width=640]

        toc::[]

        '''

        <<<

        [discrete]
        === Discrete Heading

        .Titled listing
        [#snippet.rounded%collapsible]
        ----
        content
        ----
        """.trimIndent(),
    )

    @Test
    fun serializeDeserializeSerializeIsByteIdenticalWithLocations() {
        sampleDocuments.forEachIndexed { index, source ->
            val document = DefaultAsciidocParser().parse(source).document
            val first = serializer.serializeDocument(document)
            val decoded = deserializer.deserializeDocument(first)
            val second = serializer.serializeDocument(decoded)
            assertEquals(first, second, "round trip diverged for sample #$index:\n$source")
        }
    }

    @Test
    fun serializeDeserializeSerializeIsByteIdenticalWithoutLocations() {
        val bare = AsgDocumentJsonSerializer(emitLocations = false)
        sampleDocuments.forEachIndexed { index, source ->
            val document = DefaultAsciidocParser().parse(source).document
            val first = bare.serializeDocument(document)
            val decoded = deserializer.deserializeDocument(first)
            val second = bare.serializeDocument(decoded)
            assertEquals(first, second, "location-free round trip diverged for sample #$index")
        }
    }

    @Test
    fun inlineRoundTripIsByteIdentical() {
        val inlines = DefaultAsciidocParser()
            .parse("plain *strong `code`* and https://example.com[a ref]").document
            .blocks.filterIsInstance<LeafBlock>().single().inlines
        val first = serializer.serializeInlines(inlines)
        val second = serializer.serializeInlines(deserializer.deserializeInlines(first))
        assertEquals(first, second)
    }

    @Test
    fun deserializeBlocksAcceptsArrayAndSingleObject() {
        val paragraphJson =
            """{"name":"paragraph","type":"block","inlines":[{"name":"text","type":"string","value":"hi"}]}"""
        val fromObject = deserializer.deserializeBlocks(paragraphJson).single()
        val fromArray = deserializer.deserializeBlocks("[$paragraphJson]").single()
        for (block in listOf(fromObject, fromArray)) {
            val paragraph = assertIs<LeafBlock>(block)
            assertEquals(LeafBlockName.PARAGRAPH, paragraph.name)
            assertEquals("hi", assertIs<InlineText>(paragraph.inlines.single()).value)
        }
    }

    @Test
    fun deserializeBlockMacroRequiresMacroForm() {
        assertFailsWith<IllegalArgumentException> {
            deserializer.deserializeBlocks("""{"name":"image","type":"block","target":"x.png"}""")
        }
        val macro = deserializer
            .deserializeBlocks("""{"name":"image","type":"block","form":"macro","target":"x.png"}""")
            .single()
        assertEquals(BlockMacroName.IMAGE, assertIs<BlockMacro>(macro).name)
    }

    @Test
    fun deserializeInlineSpan() {
        val span = deserializer
            .deserializeInlines(
                """{"name":"span","type":"inline","variant":"strong","form":"constrained","inlines":[
                   {"name":"text","type":"string","value":"loud"}]}""",
            )
            .single()
        assertEquals(SpanVariant.STRONG, assertIs<InlineSpan>(span).variant)
    }

    @Test
    fun unknownBlockNameFailsLoudly() {
        assertFailsWith<IllegalArgumentException> {
            deserializer.deserializeBlocks("""{"name":"martian","type":"block"}""")
        }
    }

    @Test
    fun inlineNodesInBlockContextFailLoudly() {
        assertFailsWith<IllegalArgumentException> {
            deserializer.deserializeBlocks("""[{"name":"text","type":"string","value":"hi"}]""")
        }
    }

    @Test
    fun blockNodesInInlineContextFailLoudly() {
        assertFailsWith<IllegalArgumentException> {
            deserializer.deserializeInlines("""[{"name":"paragraph","type":"block","inlines":[]}]""")
        }
    }
}
