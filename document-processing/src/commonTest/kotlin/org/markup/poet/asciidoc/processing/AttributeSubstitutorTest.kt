package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.InlineAttributeRef
import org.markup.poet.asciidoc.asg.InlineText
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.LeafBlockForm
import org.markup.poet.asciidoc.asg.LeafBlockName
import org.markup.poet.asciidoc.asg.Location
import org.markup.poet.asciidoc.asg.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AttributeSubstitutorTest {

    private val substitutor = DefaultAttributeSubstitutor()

    private fun loc(line: Int) = Location(Position(line, 1), Position(line, 1))

    private fun paragraphOf(vararg inlines: org.markup.poet.asciidoc.asg.Inline) = LeafBlock(
        name = LeafBlockName.PARAGRAPH,
        form = LeafBlockForm.PARAGRAPH,
        inlines = inlines.toList(),
        location = loc(1)
    )

    @Test
    fun `should substitute simple attribute reference in text`() {
        // Arrange
        val document = AsgDocument(
            attributes = mapOf("name" to "World"),
            blocks = listOf(paragraphOf(InlineText("Hello {name}!", loc(1))))
        )

        val config = AttributeConfig()

        // Act
        val result = substitutor.substitute(document, config)

        // Assert
        assertEquals(0, result.errors.size)
        assertEquals(1, result.substitutedAttributes.size)
        assertTrue(result.substitutedAttributes.contains("name"))

        val paragraph = result.document.blocks[0] as LeafBlock
        val text = paragraph.inlines[0] as InlineText
        assertEquals("Hello World!", text.value)
    }

    @Test
    fun `should handle undefined attribute with PRESERVE behavior`() {
        // Arrange
        val document = AsgDocument(
            blocks = listOf(paragraphOf(InlineText("Hello {undefined}!", loc(1))))
        )

        val config = AttributeConfig(undefinedBehavior = UndefinedAttributeBehavior.PRESERVE)

        // Act
        val result = substitutor.substitute(document, config)

        // Assert
        assertEquals(1, result.errors.size)
        assertEquals(ProcessingErrorType.ATTRIBUTE_UNDEFINED, result.errors[0].errorType)

        val paragraph = result.document.blocks[0] as LeafBlock
        val text = paragraph.inlines[0] as InlineText
        assertEquals("Hello {undefined}!", text.value)
    }

    @Test
    fun `should handle undefined attribute with REMOVE behavior`() {
        // Arrange
        val document = AsgDocument(
            blocks = listOf(paragraphOf(InlineText("Hello {undefined}!", loc(1))))
        )

        val config = AttributeConfig(undefinedBehavior = UndefinedAttributeBehavior.REMOVE)

        // Act
        val result = substitutor.substitute(document, config)

        // Assert
        val paragraph = result.document.blocks[0] as LeafBlock
        val text = paragraph.inlines[0] as InlineText
        assertEquals("Hello !", text.value)
    }

    @Test
    fun `should resolve recursive attribute references`() {
        // Arrange
        val document = AsgDocument(
            attributes = mapOf(
                "greeting" to "Hello {name}!",
                "name" to "World"
            ),
            blocks = listOf(paragraphOf(InlineText("Message: {greeting}", loc(1))))
        )

        val config = AttributeConfig()

        // Act
        val result = substitutor.substitute(document, config)

        // Assert
        assertEquals(0, result.errors.size)
        assertEquals(2, result.substitutedAttributes.size)

        val paragraph = result.document.blocks[0] as LeafBlock
        val text = paragraph.inlines[0] as InlineText
        assertEquals("Message: Hello World!", text.value)
    }

    @Test
    fun `should detect circular attribute references`() {
        // Arrange
        val document = AsgDocument(
            attributes = mapOf(
                "a" to "{b}",
                "b" to "{c}",
                "c" to "{a}"
            ),
            blocks = listOf(paragraphOf(InlineText("Value: {a}", loc(1))))
        )

        val config = AttributeConfig()

        // Act
        val result = substitutor.substitute(document, config)

        // Assert
        assertTrue(result.errors.any { it.errorType == ProcessingErrorType.ATTRIBUTE_CIRCULAR_REFERENCE })

        val paragraph = result.document.blocks[0] as LeafBlock
        val text = paragraph.inlines[0] as InlineText
        // Should preserve the reference when circular dependency is detected
        assertTrue(text.value.contains("{"))
    }

    @Test
    fun `should handle InlineAttributeRef elements`() {
        // Arrange
        val document = AsgDocument(
            attributes = mapOf("name" to "World"),
            blocks = listOf(
                paragraphOf(
                    InlineText("Hello ", loc(1)),
                    InlineAttributeRef("name", loc(1)),
                    InlineText("!", loc(1))
                )
            )
        )

        val config = AttributeConfig()

        // Act
        val result = substitutor.substitute(document, config)

        // Assert
        assertEquals(0, result.errors.size)

        val paragraph = result.document.blocks[0] as LeafBlock
        assertEquals(3, paragraph.inlines.size)
        assertEquals("World", (paragraph.inlines[1] as InlineText).value)
    }

    @Test
    fun `should use default values when configured`() {
        // Arrange
        val document = AsgDocument(
            blocks = listOf(paragraphOf(InlineText("Hello {name}!", loc(1))))
        )

        val config = AttributeConfig(
            defaults = mapOf("name" to "Default"),
            undefinedBehavior = UndefinedAttributeBehavior.DEFAULT
        )

        // Act
        val result = substitutor.substitute(document, config)

        // Assert
        assertEquals(0, result.errors.size)

        val paragraph = result.document.blocks[0] as LeafBlock
        val text = paragraph.inlines[0] as InlineText
        assertEquals("Hello Default!", text.value)
    }

    @Test
    fun `should respect max recursion depth`() {
        // Arrange
        val document = AsgDocument(
            attributes = mapOf(
                "a" to "{b}",
                "b" to "{c}",
                "c" to "{d}",
                "d" to "{e}",
                "e" to "final"
            ),
            blocks = listOf(paragraphOf(InlineText("Value: {a}", loc(1))))
        )

        val config = AttributeConfig(maxRecursionDepth = 3)

        // Act
        val result = substitutor.substitute(document, config)

        // Assert
        assertTrue(result.errors.any { it.errorType == ProcessingErrorType.ATTRIBUTE_CIRCULAR_REFERENCE })
    }
}
