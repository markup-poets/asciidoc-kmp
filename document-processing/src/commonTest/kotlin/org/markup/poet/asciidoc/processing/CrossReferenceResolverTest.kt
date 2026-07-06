package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.Block
import org.markup.poet.asciidoc.asg.BlockMetadata
import org.markup.poet.asciidoc.asg.Inline
import org.markup.poet.asciidoc.asg.InlineRef
import org.markup.poet.asciidoc.asg.InlineText
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.LeafBlockForm
import org.markup.poet.asciidoc.asg.LeafBlockName
import org.markup.poet.asciidoc.asg.ListBlock
import org.markup.poet.asciidoc.asg.ListItem
import org.markup.poet.asciidoc.asg.ListVariant
import org.markup.poet.asciidoc.asg.Location
import org.markup.poet.asciidoc.asg.Position
import org.markup.poet.asciidoc.asg.RefVariant
import org.markup.poet.asciidoc.asg.SectionBlock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CrossReferenceResolverTest {

    private val resolver = DefaultCrossReferenceResolver()

    private fun loc(line: Int, col: Int = 1) = Location(Position(line, col), Position(line, col))

    private fun section(title: String, id: String?, line: Int, blocks: List<Block> = emptyList(), level: Int = 1) =
        SectionBlock(
            title = listOf(InlineText(title, loc(line))),
            level = level,
            blocks = blocks,
            metadata = id?.let { BlockMetadata(id = it) },
            location = loc(line)
        )

    private fun paragraphOf(vararg inlines: Inline, line: Int = 1) = LeafBlock(
        name = LeafBlockName.PARAGRAPH,
        form = LeafBlockForm.PARAGRAPH,
        inlines = inlines.toList(),
        location = loc(line)
    )

    private fun xref(target: String, text: String? = null, line: Int = 1) = InlineRef(
        variant = RefVariant.XREF,
        target = target,
        inlines = text?.let { listOf(InlineText(it, loc(line))) } ?: emptyList(),
        location = loc(line)
    )

    @Test
    fun `should resolve cross-reference to section with anchor`() {
        // Create a document with a section that has an anchor
        val document = AsgDocument(
            blocks = listOf(
                section("Introduction", id = "intro", line = 1),
                paragraphOf(InlineText("See ", loc(5)), xref("intro", line = 5), line = 5)
            ),
            location = loc(1)
        )

        // Resolve cross-references
        val result = resolver.resolve(document)

        // Verify anchor was found
        assertTrue(result.resolvedReferences.containsKey("intro"))
        assertEquals("Introduction", result.resolvedReferences["intro"]?.generatedText)

        // Verify no errors or warnings
        assertTrue(result.errors.isEmpty())
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `should report warning for unresolved cross-reference`() {
        val document = AsgDocument(
            blocks = listOf(
                paragraphOf(xref("nonexistent", line = 5), line = 5)
            ),
            location = loc(1)
        )

        // Resolve cross-references
        val result = resolver.resolve(document)

        // Verify warning was reported
        assertEquals(1, result.warnings.size)
        assertEquals(ProcessingWarningType.CROSS_REFERENCE_UNRESOLVED, result.warnings[0].warningType)
        assertTrue(result.warnings[0].message.contains("nonexistent"))
    }

    @Test
    fun `should report error for duplicate anchor IDs`() {
        val document = AsgDocument(
            blocks = listOf(
                section("First Section", id = "duplicate", line = 1),
                section("Second Section", id = "duplicate", line = 10)
            ),
            location = loc(1)
        )

        // Resolve cross-references
        val result = resolver.resolve(document)

        // Verify error was reported
        assertEquals(1, result.errors.size)
        assertEquals(ProcessingErrorType.CROSS_REFERENCE_DUPLICATE_ANCHOR, result.errors[0].errorType)
        assertTrue(result.errors[0].message.contains("duplicate"))
        assertTrue(result.errors[0].message.contains("line 1"))
        assertTrue(result.errors[0].message.contains("line 10"))
    }

    @Test
    fun `should use custom link text when provided`() {
        val document = AsgDocument(
            blocks = listOf(
                section("Introduction", id = "intro", line = 1),
                paragraphOf(xref("intro", text = "Custom Link Text", line = 5), line = 5)
            ),
            location = loc(1)
        )

        // Resolve cross-references
        val result = resolver.resolve(document)

        // Verify anchor was found with generated text
        assertTrue(result.resolvedReferences.containsKey("intro"))
        assertEquals("Introduction", result.resolvedReferences["intro"]?.generatedText)

        // Note: Custom text is preserved in the InlineRef node itself
        // The resolver doesn't modify the custom text
    }

    @Test
    fun `should generate link text for anchored lists`() {
        val list = ListBlock(
            variant = ListVariant.UNORDERED,
            marker = "*",
            items = listOf(
                ListItem(
                    marker = "*",
                    principal = listOf(InlineText("List item content", loc(2))),
                    location = loc(2)
                )
            ),
            metadata = BlockMetadata(id = "item1"),
            location = loc(2)
        )

        val document = AsgDocument(blocks = listOf(list), location = loc(1))

        // Resolve cross-references
        val result = resolver.resolve(document)

        // Verify anchor was found with generated text from the first item's content
        assertTrue(result.resolvedReferences.containsKey("item1"))
        assertEquals("List item content", result.resolvedReferences["item1"]?.generatedText)
    }

    @Test
    fun `should handle nested sections with anchors`() {
        val nestedSection = section("Nested Section", id = "nested", line = 5, level = 2)
        val parentSection = section("Parent Section", id = "parent", line = 1, blocks = listOf(nestedSection))

        val document = AsgDocument(blocks = listOf(parentSection), location = loc(1))

        // Resolve cross-references
        val result = resolver.resolve(document)

        // Verify both anchors were found
        assertTrue(result.resolvedReferences.containsKey("parent"))
        assertTrue(result.resolvedReferences.containsKey("nested"))
        assertEquals("Parent Section", result.resolvedReferences["parent"]?.generatedText)
        assertEquals("Nested Section", result.resolvedReferences["nested"]?.generatedText)
    }
}
