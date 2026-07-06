package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.Block
import org.markup.poet.asciidoc.asg.BlockMacro
import org.markup.poet.asciidoc.asg.BlockMacroName
import org.markup.poet.asciidoc.asg.InlineRef
import org.markup.poet.asciidoc.asg.InlineText
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.LeafBlockForm
import org.markup.poet.asciidoc.asg.LeafBlockName
import org.markup.poet.asciidoc.asg.ListBlock
import org.markup.poet.asciidoc.asg.ParentBlock
import org.markup.poet.asciidoc.asg.ParentBlockName
import org.markup.poet.asciidoc.asg.SectionBlock
import org.markup.poet.asciidoc.asg.visitBlocks
import org.markup.poet.asciidoc.parser.DefaultAsciidocParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for TOC insertion in [DefaultDocumentProcessor]: each `toc` attribute
 * placement mode, the config-driven path, and idempotency across repeated
 * processing runs.
 */
class TocInsertionTest {

    private val processor = DefaultDocumentProcessor(
        includeResolver = DefaultIncludeResolver(DefaultAsciidocParser()),
        fragmentProcessor = DefaultFragmentProcessor(),
        conditionalProcessor = DefaultConditionalProcessor(),
        attributeSubstitutor = DefaultAttributeSubstitutor(),
        macroExpander = DefaultMacroExpander(),
        admonitionProcessor = DefaultAdmonitionProcessor(),
        calloutProcessor = DefaultCalloutProcessor(),
        bibliographyManager = DefaultBibliographyManager(),
        crossReferenceResolver = DefaultCrossReferenceResolver(),
        tocGenerator = DefaultTocGenerator(),
        documentValidator = DefaultDocumentValidator(),
    )

    private val config = ProcessingConfig(enableIncludes = false)

    private fun paragraph(text: String) = LeafBlock(
        name = LeafBlockName.PARAGRAPH,
        form = LeafBlockForm.PARAGRAPH,
        inlines = listOf(InlineText(text)),
    )

    private fun section(title: String, blocks: List<Block> = emptyList()) = SectionBlock(
        title = listOf(InlineText(title)),
        level = 1,
        blocks = blocks,
    )

    private fun tocMacro() = BlockMacro(name = BlockMacroName.TOC, target = null)

    /** All TOC lists (id `toc`) anywhere in the document. */
    private fun tocLists(document: AsgDocument): List<ListBlock> {
        val found = mutableListOf<ListBlock>()
        visitBlocks(document.blocks) { block ->
            if (block is ListBlock && block.metadata?.id == "toc") found.add(block)
        }
        return found
    }

    private fun xrefTargets(toc: ListBlock): List<String> =
        toc.items.map { (it.principal.single() as InlineRef).target }

    @Test
    fun `toc attribute inserts TOC before all body blocks`() {
        val document = AsgDocument(
            attributes = mapOf("toc" to ""),
            blocks = listOf(paragraph("preamble"), section("First"), section("Second")),
        )

        val result = processor.process(document, config)

        val toc = assertIs<ListBlock>(result.document.blocks.first())
        assertEquals("toc", toc.metadata?.id)
        assertTrue("toc" in toc.metadata?.roles.orEmpty())
        assertEquals(listOf("first", "second"), xrefTargets(toc))
        assertEquals(4, result.document.blocks.size)
    }

    @Test
    fun `toc auto value behaves like empty value`() {
        val document = AsgDocument(
            attributes = mapOf("toc" to "auto"),
            blocks = listOf(section("Only")),
        )

        val result = processor.process(document, config)

        assertIs<ListBlock>(result.document.blocks.first())
    }

    @Test
    fun `toc preamble inserts TOC before the first section`() {
        val document = AsgDocument(
            attributes = mapOf("toc" to "preamble"),
            blocks = listOf(paragraph("first para"), paragraph("second para"), section("First")),
        )

        val result = processor.process(document, config)

        val blocks = result.document.blocks
        assertEquals(4, blocks.size)
        assertIs<LeafBlock>(blocks[0])
        assertIs<LeafBlock>(blocks[1])
        val toc = assertIs<ListBlock>(blocks[2])
        assertEquals("toc", toc.metadata?.id)
        assertIs<SectionBlock>(blocks[3])
    }

    @Test
    fun `toc macro replaces the toc block macro in place`() {
        val document = AsgDocument(
            attributes = mapOf("toc" to "macro"),
            blocks = listOf(paragraph("intro"), tocMacro(), section("First")),
        )

        val result = processor.process(document, config)

        val blocks = result.document.blocks
        assertEquals(3, blocks.size)
        assertIs<LeafBlock>(blocks[0])
        val toc = assertIs<ListBlock>(blocks[1])
        assertEquals("toc", toc.metadata?.id)
        assertTrue(blocks.none { it is BlockMacro })
    }

    @Test
    fun `toc macro is replaced inside nested containers`() {
        val document = AsgDocument(
            attributes = mapOf("toc" to "macro"),
            blocks = listOf(
                section(
                    "First",
                    blocks = listOf(
                        ParentBlock(
                            name = ParentBlockName.OPEN,
                            blocks = listOf(tocMacro()),
                        )
                    ),
                ),
                section("Second"),
            ),
        )

        val result = processor.process(document, config)

        val outer = assertIs<SectionBlock>(result.document.blocks[0])
        val container = assertIs<ParentBlock>(outer.blocks[0])
        val toc = assertIs<ListBlock>(container.blocks[0])
        assertEquals("toc", toc.metadata?.id)
        assertEquals(listOf("first", "second"), xrefTargets(toc))
    }

    @Test
    fun `toc macro without a macro in the document inserts nothing`() {
        val document = AsgDocument(
            attributes = mapOf("toc" to "macro"),
            blocks = listOf(paragraph("intro"), section("First")),
        )

        val result = processor.process(document, config)

        assertTrue(tocLists(result.document).isEmpty())
        assertEquals(2, result.document.blocks.size)
    }

    @Test
    fun `no sections means no insertion`() {
        val document = AsgDocument(
            attributes = mapOf("toc" to ""),
            blocks = listOf(paragraph("just text")),
        )

        val result = processor.process(document, config)

        assertTrue(tocLists(result.document).isEmpty())
        assertEquals(1, result.document.blocks.size)
    }

    @Test
    fun `disabled toc leaves a toc block macro untouched`() {
        val document = AsgDocument(
            blocks = listOf(tocMacro(), section("First")),
        )

        val result = processor.process(document, config)

        val macro = assertIs<BlockMacro>(result.document.blocks[0])
        assertEquals(BlockMacroName.TOC, macro.name)
        assertTrue(tocLists(result.document).isEmpty())
    }

    @Test
    fun `config flag drives generation without any toc attribute`() {
        val document = AsgDocument(
            blocks = listOf(section("First"), section("Second")),
        )

        val result = processor.process(document, config.copy(enableTocGeneration = true))

        val toc = assertIs<ListBlock>(result.document.blocks.first())
        assertEquals(listOf("first", "second"), xrefTargets(toc))
    }

    @Test
    fun `insertion is idempotent across repeated processing runs`() {
        val document = AsgDocument(
            attributes = mapOf("toc" to ""),
            blocks = listOf(section("First"), section("Second")),
        )

        val once = processor.process(document, config).document
        val twice = processor.process(once, config).document

        assertEquals(1, tocLists(twice).size)
        assertEquals(once.blocks.size, twice.blocks.size)
    }

    @Test
    fun `toclevels attribute overrides configured depth`() {
        val document = AsgDocument(
            attributes = mapOf("toc" to "", "toclevels" to "1"),
            blocks = listOf(
                section("Top", blocks = listOf(
                    SectionBlock(
                        title = listOf(InlineText("Nested")),
                        level = 2,
                        blocks = emptyList(),
                    )
                )),
            ),
        )

        val result = processor.process(document, config)

        val toc = assertNotNull(tocLists(result.document).firstOrNull())
        assertEquals(listOf("top"), xrefTargets(toc))
        assertNull(toc.items.single().blocks.firstOrNull())
    }
}
