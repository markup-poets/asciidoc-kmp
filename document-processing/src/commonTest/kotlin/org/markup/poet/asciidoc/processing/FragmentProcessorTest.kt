package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.InlineText
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.LeafBlockForm
import org.markup.poet.asciidoc.asg.LeafBlockName
import org.markup.poet.asciidoc.asg.Location
import org.markup.poet.asciidoc.asg.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FragmentProcessorTest {

    private val processor = DefaultFragmentProcessor()
    private val config = FragmentConfig()
    private val sourceLocation = Location(Position(1, 1), Position(1, 1))

    @Test
    fun `should extract single tagged section`() {
        val content = """
            line before
            tag::example[]
            tagged content line 1
            tagged content line 2
            end::example[]
            line after
        """.trimIndent()

        val errors = mutableListOf<ProcessingError>()
        val warnings = mutableListOf<ProcessingWarning>()

        val result = processor.extractTaggedContent(
            content = content,
            tags = listOf("example"),
            config = config,
            location = sourceLocation,
            errors = errors,
            warnings = warnings
        )

        assertTrue(errors.isEmpty(), "Should not have errors")
        assertTrue(warnings.isEmpty(), "Should not have warnings")
        assertEquals("tagged content line 1\ntagged content line 2", result)
    }

    @Test
    fun `should extract multiple tagged sections`() {
        val content = """
            tag::section1[]
            content 1
            end::section1[]
            middle content
            tag::section2[]
            content 2
            end::section2[]
        """.trimIndent()

        val errors = mutableListOf<ProcessingError>()
        val warnings = mutableListOf<ProcessingWarning>()

        val result = processor.extractTaggedContent(
            content = content,
            tags = listOf("section1", "section2"),
            config = config,
            location = sourceLocation,
            errors = errors,
            warnings = warnings
        )

        assertTrue(errors.isEmpty(), "Should not have errors")
        assertTrue(warnings.isEmpty(), "Should not have warnings")
        assertTrue(result.contains("content 1"))
        assertTrue(result.contains("content 2"))
    }

    @Test
    fun `should warn when tag not found`() {
        val content = """
            tag::existing[]
            content
            end::existing[]
        """.trimIndent()

        val errors = mutableListOf<ProcessingError>()
        val warnings = mutableListOf<ProcessingWarning>()

        processor.extractTaggedContent(
            content = content,
            tags = listOf("missing"),
            config = config,
            location = sourceLocation,
            errors = errors,
            warnings = warnings
        )

        assertTrue(errors.isEmpty(), "Should not have errors")
        assertEquals(1, warnings.size, "Should have one warning")
        assertEquals(ProcessingWarningType.FRAGMENT_TAG_NOT_FOUND, warnings[0].warningType)
        assertTrue(warnings[0].message.contains("missing"))
    }

    @Test
    fun `should report error for unclosed tag`() {
        val content = """
            tag::unclosed[]
            content
        """.trimIndent()

        val errors = mutableListOf<ProcessingError>()
        val warnings = mutableListOf<ProcessingWarning>()

        processor.extractTaggedContent(
            content = content,
            tags = listOf("unclosed"),
            config = config,
            location = sourceLocation,
            errors = errors,
            warnings = warnings
        )

        assertEquals(1, errors.size, "Should have one error")
        assertEquals(ProcessingErrorType.FRAGMENT_TAG_UNCLOSED, errors[0].errorType)
        assertTrue(errors[0].message.contains("unclosed"))
    }

    @Test
    fun `should report error for unmatched end tag`() {
        val content = """
            content
            end::nomatch[]
        """.trimIndent()

        val errors = mutableListOf<ProcessingError>()
        val warnings = mutableListOf<ProcessingWarning>()

        processor.extractTaggedContent(
            content = content,
            tags = listOf("nomatch"),
            config = config,
            location = sourceLocation,
            errors = errors,
            warnings = warnings
        )

        assertEquals(1, errors.size, "Should have one error")
        assertEquals(ProcessingErrorType.FRAGMENT_TAG_MALFORMED, errors[0].errorType)
        assertTrue(errors[0].message.contains("Unmatched end tag"))
    }

    @Test
    fun `should handle nested tags when allowed`() {
        val configWithNesting = FragmentConfig(allowNestedTags = true)
        val content = """
            tag::outer[]
            outer content
            tag::inner[]
            inner content
            end::inner[]
            more outer
            end::outer[]
        """.trimIndent()

        val errors = mutableListOf<ProcessingError>()
        val warnings = mutableListOf<ProcessingWarning>()

        val result = processor.extractTaggedContent(
            content = content,
            tags = listOf("outer"),
            config = configWithNesting,
            location = sourceLocation,
            errors = errors,
            warnings = warnings
        )

        assertTrue(errors.isEmpty(), "Should not have errors with nested tags allowed")
        assertTrue(result.contains("outer content"))
        assertTrue(result.contains("inner content"))
    }

    @Test
    fun `should report error for nested tags when not allowed`() {
        val content = """
            tag::outer[]
            outer content
            tag::inner[]
            inner content
            end::inner[]
            end::outer[]
        """.trimIndent()

        val errors = mutableListOf<ProcessingError>()
        val warnings = mutableListOf<ProcessingWarning>()

        processor.extractTaggedContent(
            content = content,
            tags = listOf("outer"),
            config = config,
            location = sourceLocation,
            errors = errors,
            warnings = warnings
        )

        assertTrue(errors.any { it.errorType == ProcessingErrorType.FRAGMENT_TAG_MALFORMED && it.message.contains("Nested tags") })
    }

    @Test
    fun `should apply tag and line range filters in correct order`() {
        val content = """
            line 1
            tag::example[]
            line 3
            line 4
            line 5
            end::example[]
            line 7
        """.trimIndent()

        val errors = mutableListOf<ProcessingError>()
        val warnings = mutableListOf<ProcessingWarning>()

        // Extract tag first (lines 3-5), then apply line range (lines 1-2 of extracted content)
        val result = processor.applyTagAndLineRangeFilters(
            content = content,
            tags = listOf("example"),
            lineRange = 1..2,
            config = config,
            location = sourceLocation,
            errors = errors,
            warnings = warnings
        )

        assertTrue(errors.isEmpty(), "Should not have errors")
        assertEquals("line 3\nline 4", result)
    }

    @Test
    fun `should handle empty tagged section`() {
        val content = """
            tag::empty[]
            end::empty[]
        """.trimIndent()

        val errors = mutableListOf<ProcessingError>()
        val warnings = mutableListOf<ProcessingWarning>()

        val result = processor.extractTaggedContent(
            content = content,
            tags = listOf("empty"),
            config = config,
            location = sourceLocation,
            errors = errors,
            warnings = warnings
        )

        assertTrue(errors.isEmpty(), "Should not have errors")
        assertEquals("", result)
    }

    @Test
    fun `should handle same tag appearing multiple times`() {
        val content = """
            tag::repeated[]
            first occurrence
            end::repeated[]
            middle
            tag::repeated[]
            second occurrence
            end::repeated[]
        """.trimIndent()

        val errors = mutableListOf<ProcessingError>()
        val warnings = mutableListOf<ProcessingWarning>()

        val result = processor.extractTaggedContent(
            content = content,
            tags = listOf("repeated"),
            config = config,
            location = sourceLocation,
            errors = errors,
            warnings = warnings
        )

        assertTrue(errors.isEmpty(), "Should not have errors")
        assertTrue(result.contains("first occurrence"))
        assertTrue(result.contains("second occurrence"))
    }

    @Test
    fun `should validate tag names`() {
        val content = """
            tag::invalid tag[]
            content
            end::invalid tag[]
        """.trimIndent()

        val errors = mutableListOf<ProcessingError>()
        val warnings = mutableListOf<ProcessingWarning>()

        processor.extractTaggedContent(
            content = content,
            tags = listOf("invalid tag"),
            config = config,
            location = sourceLocation,
            errors = errors,
            warnings = warnings
        )

        assertTrue(errors.any { it.errorType == ProcessingErrorType.FRAGMENT_TAG_MALFORMED && it.message.contains("invalid tag name") })
    }

    @Test
    fun `should process document with fragment processor`() {
        val document = AsgDocument(
            blocks = listOf(
                LeafBlock(
                    name = LeafBlockName.PARAGRAPH,
                    form = LeafBlockForm.PARAGRAPH,
                    inlines = listOf(InlineText("Test content", sourceLocation)),
                    location = sourceLocation
                )
            ),
            location = sourceLocation
        )

        val result = processor.processFragments(document, config)

        assertTrue(result.errors.isEmpty())
        assertTrue(result.warnings.isEmpty())
        assertEquals(1, result.document.blocks.size)
    }
}
