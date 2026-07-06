package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.Block
import org.markup.poet.asciidoc.asg.BlockMetadata
import org.markup.poet.asciidoc.asg.InlineRef
import org.markup.poet.asciidoc.asg.InlineText
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.LeafBlockForm
import org.markup.poet.asciidoc.asg.LeafBlockName
import org.markup.poet.asciidoc.asg.ListBlock
import org.markup.poet.asciidoc.asg.ListItem
import org.markup.poet.asciidoc.asg.Location
import org.markup.poet.asciidoc.asg.Position
import org.markup.poet.asciidoc.asg.SectionBlock
import org.markup.poet.asciidoc.asg.plainText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TocGeneratorTest {

    private val generator = DefaultTocGenerator()

    private fun loc(line: Int) = Location(Position(line, 1), Position(line, 1))

    private fun section(
        title: String,
        level: Int,
        line: Int,
        id: String? = null,
        blocks: List<Block> = emptyList()
    ) = SectionBlock(
        title = listOf(InlineText(title, loc(line))),
        level = level,
        blocks = blocks,
        metadata = id?.let { BlockMetadata(id = it) },
        location = loc(line)
    )

    private fun nestedListOf(item: ListItem): ListBlock? =
        item.blocks.filterIsInstance<ListBlock>().firstOrNull()

    @Test
    fun `should return null TOC for document with no sections`() {
        val document = AsgDocument(
            blocks = listOf(
                LeafBlock(
                    name = LeafBlockName.PARAGRAPH,
                    form = LeafBlockForm.PARAGRAPH,
                    inlines = listOf(InlineText("Some content", loc(1))),
                    location = loc(1)
                )
            ),
            location = loc(1)
        )

        val config = TocConfig(maxDepth = 3)
        val result = generator.generate(document, config)

        assertNull(result.tocNode)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `should generate TOC with single section`() {
        val document = AsgDocument(
            blocks = listOf(section("Introduction", level = 1, line = 1, id = "intro")),
            location = loc(1)
        )

        val config = TocConfig(maxDepth = 3)
        val result = generator.generate(document, config)

        assertNotNull(result.tocNode)
        assertEquals(1, result.tocNode.items.size)

        val item = result.tocNode.items[0]
        assertEquals(1, item.principal.size)

        val crossRef = item.principal[0] as InlineRef
        assertEquals("intro", crossRef.target)
        assertEquals("Introduction", plainText(crossRef.inlines))
    }

    @Test
    fun `should generate hierarchical TOC with nested sections`() {
        val document = AsgDocument(
            blocks = listOf(
                section(
                    "Chapter 1", level = 1, line = 1, id = "chap-1",
                    blocks = listOf(
                        section("Section 1.1", level = 2, line = 3, id = "sec-1-1"),
                        section("Section 1.2", level = 2, line = 5, id = "sec-1-2")
                    )
                )
            ),
            location = loc(1)
        )

        val config = TocConfig(maxDepth = 3)
        val result = generator.generate(document, config)

        assertNotNull(result.tocNode)
        assertEquals(1, result.tocNode.items.size)

        val chapter = result.tocNode.items[0]
        val nestedList = nestedListOf(chapter)
        assertNotNull(nestedList)
        assertEquals(2, nestedList.items.size)
    }

    @Test
    fun `should respect depth limit`() {
        val document = AsgDocument(
            blocks = listOf(
                section(
                    "Level 1", level = 1, line = 1,
                    blocks = listOf(
                        section(
                            "Level 2", level = 2, line = 3,
                            blocks = listOf(
                                section(
                                    "Level 3", level = 3, line = 5,
                                    blocks = listOf(section("Level 4", level = 4, line = 7))
                                )
                            )
                        )
                    )
                )
            ),
            location = loc(1)
        )

        val config = TocConfig(maxDepth = 2)
        val result = generator.generate(document, config)

        assertNotNull(result.tocNode)
        assertEquals(1, result.tocNode.items.size)

        val level1 = result.tocNode.items[0]
        val level1Nested = nestedListOf(level1)
        assertNotNull(level1Nested)
        assertEquals(1, level1Nested.items.size)

        val level2 = level1Nested.items[0]
        assertNull(nestedListOf(level2)) // Level 3 and 4 should be excluded
    }

    @Test
    fun `should filter out untitled sections`() {
        val document = AsgDocument(
            blocks = listOf(
                section("Chapter 1", level = 1, line = 1),
                section("", level = 1, line = 3),
                section("Chapter 2", level = 1, line = 5)
            ),
            location = loc(1)
        )

        val config = TocConfig(maxDepth = 3)
        val result = generator.generate(document, config)

        assertNotNull(result.tocNode)
        assertEquals(2, result.tocNode.items.size) // Only titled sections
    }

    @Test
    fun `should generate anchor IDs for sections without explicit IDs`() {
        val document = AsgDocument(
            blocks = listOf(section("Hello World", level = 1, line = 1)),
            location = loc(1)
        )

        val config = TocConfig(maxDepth = 3)
        val result = generator.generate(document, config)

        assertNotNull(result.tocNode)
        assertEquals(1, result.tocNode.items.size)

        val item = result.tocNode.items[0]
        val crossRef = item.principal[0] as InlineRef
        assertEquals("hello-world", crossRef.target) // Generated from title
    }
}
