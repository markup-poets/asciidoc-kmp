package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.Inline
import org.markup.poet.asciidoc.asg.InlineMacro
import org.markup.poet.asciidoc.asg.InlineText
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.LeafBlockForm
import org.markup.poet.asciidoc.asg.LeafBlockName
import org.markup.poet.asciidoc.asg.Location
import org.markup.poet.asciidoc.asg.Position
import org.markup.poet.asciidoc.asg.SectionBlock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MacroExpanderTest {

    private val location = Location(Position(1, 1), Position(1, 1))

    private fun paragraphOf(vararg inlines: Inline) = LeafBlock(
        name = LeafBlockName.PARAGRAPH,
        form = LeafBlockForm.PARAGRAPH,
        inlines = inlines.toList(),
        location = location
    )

    @Test
    fun `should expand macro with custom processor`() {
        // Create a simple macro processor that replaces macro with text
        val processor = object : MacroProcessor {
            override fun process(
                macroName: String,
                parameters: Map<String, String>,
                context: MacroContext
            ): MacroExpansionResult {
                val text = parameters["text"] ?: "default"
                return MacroExpansionResult.Success(
                    listOf(InlineText(value = "Expanded: $text", location = context.location))
                )
            }
        }

        // Create a document with a macro invocation
        val macro = InlineMacro(
            name = "test",
            target = "",
            named = mapOf("text" to "hello"),
            location = location
        )

        val document = AsgDocument(blocks = listOf(paragraphOf(macro)), location = location)

        // Expand macros
        val expander = DefaultMacroExpander()
        val config = MacroConfig(customMacros = mapOf("test" to processor))
        val result = expander.expand(document, config)

        // Verify expansion
        assertTrue(result.errors.isEmpty(), "Should have no errors")
        val processedParagraph = result.document.blocks.first() as LeafBlock
        val expandedText = processedParagraph.inlines.first() as InlineText
        assertEquals("Expanded: hello", expandedText.value)
    }

    @Test
    fun `should report error for unknown macro`() {
        // Create a document with an unknown macro
        val macro = InlineMacro(
            name = "unknown",
            target = "",
            location = location
        )

        val document = AsgDocument(blocks = listOf(paragraphOf(macro)), location = location)

        // Expand macros
        val expander = DefaultMacroExpander()
        val config = MacroConfig(customMacros = emptyMap())
        val result = expander.expand(document, config)

        // Verify error reporting
        assertEquals(1, result.errors.size)
        assertEquals(ProcessingErrorType.MACRO_EXPANSION_FAILED, result.errors.first().errorType)
        assertTrue(result.errors.first().message.contains("unknown"))
    }

    @Test
    fun `should report error when macro processor fails`() {
        // Create a macro processor that fails
        val processor = object : MacroProcessor {
            override fun process(
                macroName: String,
                parameters: Map<String, String>,
                context: MacroContext
            ): MacroExpansionResult {
                return MacroExpansionResult.Error("Processing failed")
            }
        }

        // Create a document with a macro
        val macro = InlineMacro(
            name = "failing",
            target = "",
            location = location
        )

        val document = AsgDocument(blocks = listOf(paragraphOf(macro)), location = location)

        // Expand macros
        val expander = DefaultMacroExpander()
        val config = MacroConfig(customMacros = mapOf("failing" to processor))
        val result = expander.expand(document, config)

        // Verify error reporting
        assertEquals(1, result.errors.size)
        assertEquals(ProcessingErrorType.MACRO_EXPANSION_FAILED, result.errors.first().errorType)
        assertTrue(result.errors.first().message.contains("Processing failed"))
    }

    @Test
    fun `should expand nested macros in sections`() {
        // Create a macro processor
        val processor = object : MacroProcessor {
            override fun process(
                macroName: String,
                parameters: Map<String, String>,
                context: MacroContext
            ): MacroExpansionResult {
                return MacroExpansionResult.Success(
                    listOf(InlineText(value = "expanded", location = context.location))
                )
            }
        }

        // Create a document with nested structure
        val macro = InlineMacro(
            name = "test",
            target = "",
            location = location
        )

        val section = SectionBlock(
            title = listOf(InlineText("Test Section", location)),
            level = 1,
            blocks = listOf(paragraphOf(macro)),
            location = location
        )

        val document = AsgDocument(blocks = listOf(section), location = location)

        // Expand macros
        val expander = DefaultMacroExpander()
        val config = MacroConfig(customMacros = mapOf("test" to processor))
        val result = expander.expand(document, config)

        // Verify expansion in nested structure
        assertTrue(result.errors.isEmpty())
        val processedSection = result.document.blocks.first() as SectionBlock
        val processedParagraph = processedSection.blocks.first() as LeafBlock
        val expandedText = processedParagraph.inlines.first() as InlineText
        assertEquals("expanded", expandedText.value)
    }

    @Test
    fun `should handle empty parameters`() {
        // Create a macro processor
        val processor = object : MacroProcessor {
            override fun process(
                macroName: String,
                parameters: Map<String, String>,
                context: MacroContext
            ): MacroExpansionResult {
                return MacroExpansionResult.Success(
                    listOf(InlineText(value = "no params", location = context.location))
                )
            }
        }

        // Create a document with a macro with no parameters
        val macro = InlineMacro(
            name = "test",
            target = "",
            location = location
        )

        val document = AsgDocument(blocks = listOf(paragraphOf(macro)), location = location)

        // Expand macros
        val expander = DefaultMacroExpander()
        val config = MacroConfig(customMacros = mapOf("test" to processor))
        val result = expander.expand(document, config)

        // Verify expansion
        assertTrue(result.errors.isEmpty())
        val processedParagraph = result.document.blocks.first() as LeafBlock
        val expandedText = processedParagraph.inlines.first() as InlineText
        assertEquals("no params", expandedText.value)
    }

    @Test
    fun `should leave built-in macros untouched`() {
        // image is a built-in macro name and must not be claimed by the expander
        val macro = InlineMacro(
            name = "image",
            target = "logo.png",
            positional = listOf("Logo"),
            location = location
        )

        val document = AsgDocument(blocks = listOf(paragraphOf(macro)), location = location)

        val expander = DefaultMacroExpander()
        val result = expander.expand(document, MacroConfig(customMacros = emptyMap()))

        assertTrue(result.errors.isEmpty(), "Built-in macros should not produce errors")
        val processedParagraph = result.document.blocks.first() as LeafBlock
        assertEquals(macro, processedParagraph.inlines.first())
    }
}
