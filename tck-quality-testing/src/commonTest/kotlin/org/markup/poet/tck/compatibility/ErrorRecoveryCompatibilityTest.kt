package org.markup.poet.tck.compatibility

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.InlineSpan
import org.markup.poet.asciidoc.asg.InlineText
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.LeafBlockName
import org.markup.poet.asciidoc.asg.ParentBlock
import org.markup.poet.asciidoc.asg.ParentBlockName
import org.markup.poet.asciidoc.asg.SpanVariant
import org.markup.poet.asciidoc.asg.plainText
import org.markup.poet.asciidoc.parser.DefaultAsciidocParser
import org.markup.poet.asciidoc.processing.AttributeConfig
import org.markup.poet.asciidoc.processing.DefaultAttributeSubstitutor
import org.markup.poet.asciidoc.processing.DefaultCrossReferenceResolver
import org.markup.poet.asciidoc.processing.DefaultIncludeResolver
import org.markup.poet.asciidoc.processing.ErrorSeverity
import org.markup.poet.asciidoc.processing.FileReadResult
import org.markup.poet.asciidoc.processing.FileReader
import org.markup.poet.asciidoc.processing.IncludeConfig
import org.markup.poet.asciidoc.processing.ProcessingErrorType
import org.markup.poet.asciidoc.processing.ProcessingWarningType
import org.markup.poet.asciidoc.processing.UndefinedAttributeBehavior
import org.markup.poet.asciidoc.render.DefaultBlockRenderer
import org.markup.poet.asciidoc.render.DefaultHtmlBuilder
import org.markup.poet.asciidoc.render.DefaultHtmlEscaper
import org.markup.poet.asciidoc.render.DefaultHtmlRenderer
import org.markup.poet.asciidoc.render.DefaultInlineRenderer
import org.markup.poet.asciidoc.render.CssMode
import org.markup.poet.asciidoc.render.OutputOptions
import org.markup.poet.asciidoc.render.RenderConfig
import org.markup.poet.tck.fixtures.FixtureLoader
import org.markup.poet.tck.fixtures.ResourceFixtureLoader
import org.markup.poet.tck.validation.DefaultOutputValidator
import org.markup.poet.tck.validation.OutputValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Compatibility tests for error recovery in AsciiDoc parsing.
 *
 * These tests validate that the parser handles malformed input gracefully
 * and consistently across all platforms, without crashing.
 *
 * Requirements: 5.8, 5.10
 */
class ErrorRecoveryCompatibilityTest : CompatibilityTest() {
    override val fixtureLoader: FixtureLoader = ResourceFixtureLoader()
    override val validator: OutputValidator = DefaultOutputValidator()

    private val parser = DefaultAsciidocParser()

    private fun parse(source: String): AsgDocument {
        val result = parser.parse(source)
        assertTrue(result.errors.none { it.severity == org.markup.poet.asciidoc.error.ErrorSeverity.FATAL })
        return result.document
    }

    /** A platform-clean in-memory file reader for include-error tests. */
    private class MapFileReader(private val files: Map<String, String>) : FileReader {
        override fun readFile(path: String): FileReadResult =
            files[path]?.let { FileReadResult.Success(it) }
                ?: FileReadResult.Error("File not found: $path")
    }

    private fun resolveIncludes(source: String, files: Map<String, String> = emptyMap()) =
        DefaultIncludeResolver(parser).resolve(
            parser.parse(source).document,
            IncludeConfig(fileReader = MapFileReader(files)),
        )

    // Malformed Block Tests

    @Test
    fun `should handle unclosed block delimiter`() {
        // Fixture: malformed-block-unclosed
        val document = parse("====\nThis is a sidebar block.\n\nBut it's never closed.")
        // The unclosed block extends to the end of input instead of crashing.
        val block = assertIs<ParentBlock>(document.blocks.single())
        assertEquals(ParentBlockName.EXAMPLE, block.name)
        assertEquals(2, block.blocks.filterIsInstance<LeafBlock>().size)
    }

    @Test
    fun `should handle invalid block delimiter`() {
        // A delimiter-looking run of characters that is no valid AsciiDoc delimiter
        // degrades to plain paragraph content.
        val document = parse("~~~~\nNot a real block.\n~~~~\n\nAfter paragraph.")
        assertTrue(document.blocks.isNotEmpty())
        assertTrue(document.blocks.all { it is LeafBlock })
        val last = assertIs<LeafBlock>(document.blocks.last())
        assertEquals("After paragraph.", plainText(last.inlines))
    }

    @Test
    fun `should handle mismatched block delimiters`() {
        // Fixture: malformed-block-invalid-delimiter
        val document = parse("====\nThis starts as a sidebar.\n----\nBut closes with a different delimiter.")
        val outer = assertIs<ParentBlock>(document.blocks.single())
        assertEquals(ParentBlockName.EXAMPLE, outer.name)
        // The mismatched `----` opens a nested (unclosed) listing; nothing is lost.
        val listing = outer.blocks.filterIsInstance<LeafBlock>().single { it.name == LeafBlockName.LISTING }
        assertEquals("But closes with a different delimiter.", plainText(listing.inlines))
    }

    @Test
    fun `should continue parsing after malformed block`() {
        val document = parse("====\nUnclosed example block.\n\nContent after the malformed opening is still parsed.")
        val block = assertIs<ParentBlock>(document.blocks.single())
        val paragraphs = block.blocks.filterIsInstance<LeafBlock>()
        assertEquals(2, paragraphs.size)
        assertEquals(
            "Content after the malformed opening is still parsed.",
            plainText(paragraphs.last().inlines),
        )
    }

    // Malformed Inline Tests

    @Test
    fun `should handle unclosed inline formatting`() {
        // Fixture: malformed-inline-unclosed
        val source = "This has *bold that never closes and continues to the end."
        val document = parse(source)
        val paragraph = assertIs<LeafBlock>(document.blocks.single())
        val text = assertIs<InlineText>(paragraph.inlines.single())
        assertEquals(source, text.value)
    }

    @Test
    fun `should handle nested unclosed inline formatting`() {
        // Fixture: malformed-inline-nested
        val document = parse("This has *bold with _italic that closes bold* before italic_.")
        val paragraph = assertIs<LeafBlock>(document.blocks.single())
        // The bold span closes; the overlapping italic stays literal text.
        val bold = paragraph.inlines.filterIsInstance<InlineSpan>().single()
        assertEquals(SpanVariant.STRONG, bold.variant)
        assertEquals("This has bold with _italic that closes bold before italic_.", plainText(paragraph.inlines))
    }

    @Test
    fun `should preserve raw text for malformed inline syntax`() {
        val source = "Unmatched markers: *bold _italic `mono end of line"
        val document = parse(source)
        val paragraph = assertIs<LeafBlock>(document.blocks.single())
        val text = assertIs<InlineText>(paragraph.inlines.single())
        assertEquals(source, text.value)
    }

    @Test
    fun `should continue parsing after malformed inline syntax`() {
        val document = parse("Broken *unclosed bold here\n\nNext paragraph with *good bold* works.")
        val paragraphs = document.blocks.filterIsInstance<LeafBlock>()
        assertEquals(2, paragraphs.size)
        val bold = paragraphs.last().inlines.filterIsInstance<InlineSpan>().single()
        assertEquals("good bold", plainText(bold.inlines))
    }

    // Invalid Attribute Tests

    @Test
    fun `should handle invalid attribute syntax`() {
        // Fixture: invalid-attribute-syntax
        val document = parse("= Title\n:invalid attribute without equals\n:another-attr: valid value\n\nContent here.")
        // The malformed entry never becomes an attribute; parsing continues leniently.
        assertTrue(document.attributes.keys.none { it.contains(' ') })
        assertTrue(document.blocks.isNotEmpty())
        assertTrue(
            document.blocks.filterIsInstance<LeafBlock>().any { plainText(it.inlines) == "Content here." }
        )
    }

    @Test
    fun `should handle invalid attribute value`() {
        // Fixture: invalid-attribute-value
        val document = parse("= Title\n:toc-levels: not-a-number\n:sectnums:\n\n== Section\n\nContent.")
        // Values are stored as raw strings; nothing crashes on non-numeric values.
        assertEquals("not-a-number", document.attributes["toc-levels"])
        assertEquals("", document.attributes["sectnums"])
        assertTrue(document.blocks.any { it is org.markup.poet.asciidoc.asg.SectionBlock })
    }

    @Test
    fun `should use default values for invalid attributes`() {
        val document = parse("= Title\n\nValue is {undefined-attr}.")
        val result = DefaultAttributeSubstitutor().substitute(
            document,
            AttributeConfig(
                defaults = mapOf("undefined-attr" to "fallback"),
                undefinedBehavior = UndefinedAttributeBehavior.DEFAULT,
            ),
        )
        val paragraph = assertIs<LeafBlock>(result.document.blocks.single())
        assertEquals("Value is fallback.", plainText(paragraph.inlines))
    }

    @Test
    fun `should log warnings for invalid attributes`() {
        val document = parse("= Title\n\nValue is {undefined-attr}.")
        val result = DefaultAttributeSubstitutor().substitute(document, AttributeConfig())
        val issue = result.errors.single()
        assertEquals(ProcessingErrorType.ATTRIBUTE_UNDEFINED, issue.errorType)
        assertEquals(ErrorSeverity.WARNING, issue.severity)
        assertTrue(issue.message.contains("undefined-attr"))
        // PRESERVE behavior: the reference stays in the output.
        val paragraph = assertIs<LeafBlock>(result.document.blocks.single())
        assertEquals("Value is {undefined-attr}.", plainText(paragraph.inlines))
    }

    // Include Tests

    @Test
    fun `should handle missing include file`() {
        val result = resolveIncludes("include::missing-file.adoc[]")
        val error = result.errors.single()
        assertEquals(ProcessingErrorType.INCLUDE_NOT_FOUND, error.errorType)
        assertTrue(error.message.contains("missing-file.adoc"))
    }

    @Test
    fun `should detect circular includes`() {
        val result = resolveIncludes(
            "include::a.adoc[]",
            files = mapOf(
                "a.adoc" to "Content A\n\ninclude::b.adoc[]",
                "b.adoc" to "Content B\n\ninclude::a.adoc[]",
            ),
        )
        assertTrue(result.errors.any { it.errorType == ProcessingErrorType.INCLUDE_CIRCULAR_DEPENDENCY })
    }

    @Test
    fun `should prevent infinite recursion in circular includes`() {
        // Resolution of a self-including file must terminate and report the cycle.
        val result = resolveIncludes(
            "include::self.adoc[]",
            files = mapOf("self.adoc" to "Self content\n\ninclude::self.adoc[]"),
        )
        assertTrue(result.errors.any { it.errorType == ProcessingErrorType.INCLUDE_CIRCULAR_DEPENDENCY })
        // The non-circular part of the content is still embedded.
        assertTrue(
            result.document.blocks.filterIsInstance<LeafBlock>().any { plainText(it.inlines) == "Self content" }
        )
    }

    @Test
    fun `should continue parsing after include error`() {
        val result = resolveIncludes("Before paragraph.\n\ninclude::missing.adoc[]\n\nAfter paragraph.")
        assertEquals(1, result.errors.size)
        val paragraphs = result.document.blocks.filterIsInstance<LeafBlock>()
        assertEquals(listOf("Before paragraph.", "After paragraph."), paragraphs.map { plainText(it.inlines) })
    }

    // Cross-Reference Tests

    @Test
    fun `should handle missing cross-reference target`() {
        val document = parse("See <<no-such-target>> for details.")
        val result = DefaultCrossReferenceResolver().resolve(document)
        // The document survives; the dangling reference is reported, not fatal.
        assertTrue(result.errors.isEmpty())
        assertEquals(1, result.warnings.size)
        assertEquals(ProcessingWarningType.CROSS_REFERENCE_UNRESOLVED, result.warnings.single().warningType)
    }

    @Test
    fun `should render placeholder for missing cross-reference`() {
        val document = parse("See <<no-such-target>> for details.")
        val builder = DefaultHtmlBuilder(DefaultHtmlEscaper())
        val inlineRenderer = DefaultInlineRenderer(builder)
        val renderer = DefaultHtmlRenderer(DefaultBlockRenderer(builder, inlineRenderer), inlineRenderer)
        val html = renderer.render(
            document,
            RenderConfig(outputOptions = OutputOptions(standalone = false, cssMode = CssMode.NONE)),
        ).getOrThrow()
        // The unresolved reference renders as a link placeholder carrying the target id.
        assertTrue(html.contains("href=\"#no-such-target\""))
        assertTrue(html.contains("no-such-target"))
    }

    @Test
    fun `should log warning for missing cross-reference`() {
        val document = parse("== Section\n\nSee <<missing-anchor>>.")
        val warning = DefaultCrossReferenceResolver().resolve(document).warnings.single()
        assertEquals(ProcessingWarningType.CROSS_REFERENCE_UNRESOLVED, warning.warningType)
        assertTrue(warning.message.contains("missing-anchor"))
        assertNotNull(warning.location)
    }

    // Error Collection Tests

    @Test
    fun `should collect all errors during parsing`() {
        val result = resolveIncludes(
            "include::first-missing.adoc[]\n\nMiddle paragraph.\n\ninclude::second-missing.adoc[]"
        )
        assertEquals(2, result.errors.size)
        assertTrue(result.errors.all { it.errorType == ProcessingErrorType.INCLUDE_NOT_FOUND })
    }

    @Test
    fun `should provide structured error information`() {
        val error = resolveIncludes("include::missing.adoc[]").errors.single()
        assertTrue(error.message.isNotBlank())
        assertEquals(ProcessingErrorType.INCLUDE_NOT_FOUND, error.errorType)
        assertEquals(ErrorSeverity.ERROR, error.severity)
        assertNotNull(error.location)
    }

    @Test
    fun `should include line and column numbers in errors`() {
        val error = resolveIncludes("First paragraph.\n\ninclude::missing.adoc[]").errors.single()
        val location = assertNotNull(error.location)
        assertEquals(3, location.start.line)
        assertEquals(1, location.start.col)
    }

    @Test
    fun `should never throw unhandled exceptions for malformed input`() {
        val nastyInputs = listOf(
            "",
            "\n\n\n",
            "====",
            "----\nunclosed",
            "====\n----\n____\n****",
            "* \n** \n*** ",
            "[",
            "[]",
            "[unclosed attribute",
            "= ",
            "======= too deep\n\n*unclosed `mixed _mess",
            "include::[]",
            "ifdef::x[",
            "endif::[]",
            "<<>>",
            "{}{}{}{",
            "|===\n|orphan table\n",
            " ",
            "text\rwith\rlone\rcarriage\rreturns",
        )
        for (input in nastyInputs) {
            // parse() must always return a document, never throw.
            val result = parser.parse(input)
            assertNotNull(result.document, "Parser returned no document for input: $input")
        }
    }
}
