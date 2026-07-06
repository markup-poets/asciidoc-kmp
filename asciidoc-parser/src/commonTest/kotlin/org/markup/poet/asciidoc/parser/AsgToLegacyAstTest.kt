package org.markup.poet.asciidoc.parser

import org.markup.poet.asciidoc.ast.AsciiDocList
import org.markup.poet.asciidoc.ast.Code
import org.markup.poet.asciidoc.ast.CodeBlock
import org.markup.poet.asciidoc.ast.Emphasis
import org.markup.poet.asciidoc.ast.ListType
import org.markup.poet.asciidoc.ast.Paragraph
import org.markup.poet.asciidoc.ast.Section
import org.markup.poet.asciidoc.ast.Strong
import org.markup.poet.asciidoc.ast.Text
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Behavior-level tests for the ASG -> legacy AST bridge, exercised through the
 * public [DefaultAsciidocParser] facade (the path downstream modules use).
 */
class AsgToLegacyAstTest {

    private val parser = DefaultAsciidocParser()

    @Test
    fun `document title comes from the header and is not a section child`() {
        val result = parser.parse("= Document Title\n\nSome text")
        assertEquals("Document Title", result.document.title)
        assertEquals(1, result.document.children.size)
        assertIs<Paragraph>(result.document.children[0])
    }

    @Test
    fun `header attribute entries become document attributes`() {
        val result = parser.parse("= Title\n:author: Jane\n:toc: left\n\nBody")
        assertEquals("Jane", result.document.getAttribute("author"))
        assertEquals("left", result.document.getAttribute("toc"))
    }

    @Test
    fun `section level equals the number of equals signs`() {
        val result = parser.parse("== First\n\ntext\n\n=== Nested\n\nmore")
        val section = assertIs<Section>(result.document.children[0])
        assertEquals(2, section.level)
        assertEquals("First", section.title)
        val nested = assertIs<Section>(section.children[1])
        assertEquals(3, nested.level)
        assertEquals("Nested", nested.title)
    }

    @Test
    fun `section content is nested inside the section`() {
        val result = parser.parse("== Section\n\nParagraph inside")
        val section = assertIs<Section>(result.document.children.single())
        val paragraph = assertIs<Paragraph>(section.children.single())
        assertEquals("Paragraph inside", (paragraph.content.single() as Text).content)
    }

    @Test
    fun `paragraph inlines map to legacy inline elements`() {
        val result = parser.parse("plain *bold* _italic_ `mono` end")
        val paragraph = assertIs<Paragraph>(result.document.children.single())
        val strong = paragraph.content.filterIsInstance<Strong>().single()
        assertEquals("bold", (strong.content.single() as Text).content)
        val emphasis = paragraph.content.filterIsInstance<Emphasis>().single()
        assertEquals("italic", (emphasis.content.single() as Text).content)
        val code = paragraph.content.filterIsInstance<Code>().single()
        assertEquals("mono", code.content)
    }

    @Test
    fun `mark spans are flattened to their inner inlines`() {
        val result = parser.parse("a #marked# word")
        val paragraph = assertIs<Paragraph>(result.document.children.single())
        val textContent = paragraph.content.filterIsInstance<Text>().joinToString("") { it.content }
        assertEquals("a marked word", textContent)
    }

    @Test
    fun `unordered and ordered lists map with markers preserved`() {
        val result = parser.parse("* one\n* two\n\n. first\n. second")
        val unordered = assertIs<AsciiDocList>(result.document.children[0])
        assertEquals(ListType.UNORDERED, unordered.type)
        assertEquals(listOf("*", "*"), unordered.items.map { it.marker })
        assertEquals("one", (unordered.items[0].content.single() as Text).content)
        val ordered = assertIs<AsciiDocList>(result.document.children[1])
        assertEquals(ListType.ORDERED, ordered.type)
        assertEquals("second", (ordered.items[1].content.single() as Text).content)
    }

    @Test
    fun `listing block maps to a code block without language`() {
        val result = parser.parse("----\nval x = 1\nval y = 2\n----")
        val codeBlock = assertIs<CodeBlock>(result.document.children.single())
        assertNull(codeBlock.language)
        assertEquals("val x = 1\nval y = 2", codeBlock.content)
    }

    @Test
    fun `source attribute line folds its language into the code block`() {
        val result = parser.parse("[source,kotlin]\n----\nfun main() {}\n----")
        val codeBlock = assertIs<CodeBlock>(result.document.children.single())
        assertEquals("kotlin", codeBlock.language)
        assertEquals("fun main() {}", codeBlock.content)
    }

    @Test
    fun `parent blocks are spliced into their children`() {
        val result = parser.parse("****\nsidebar text\n****\n\n====\nexample text\n====")
        assertEquals(2, result.document.children.size)
        val first = assertIs<Paragraph>(result.document.children[0])
        assertEquals("sidebar text", (first.content.single() as Text).content)
        val second = assertIs<Paragraph>(result.document.children[1])
        assertEquals("example text", (second.content.single() as Text).content)
    }

    @Test
    fun `locations carry over from the asg`() {
        val result = parser.parse("first line\n\nsecond paragraph")
        val second = assertIs<Paragraph>(result.document.children[1])
        assertEquals(3, second.sourceLocation.line)
        assertEquals(1, second.sourceLocation.column)
    }

    @Test
    fun `parser never throws and always returns a document`() {
        val nasty = listOf(
            "",
            "\n\n\n",
            "== ",
            "*unclosed strong",
            "----\nunclosed listing",
            "****\nunclosed sidebar",
            "\\*escaped* and #mark",
            "= Title\n:bad attr line\ntext",
        )
        nasty.forEach { source ->
            val result = parser.parse(source) // must not throw
            assertTrue(result.document.sourceLocation.line >= 1, "parse('$source') should return a document")
        }
    }
}
