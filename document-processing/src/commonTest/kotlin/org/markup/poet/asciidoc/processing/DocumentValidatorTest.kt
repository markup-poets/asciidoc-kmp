package org.markup.poet.asciidoc.processing

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.Block
import org.markup.poet.asciidoc.asg.BlockMetadata
import org.markup.poet.asciidoc.asg.Inline
import org.markup.poet.asciidoc.asg.InlineAttributeRef
import org.markup.poet.asciidoc.asg.InlineText
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.LeafBlockForm
import org.markup.poet.asciidoc.asg.LeafBlockName
import org.markup.poet.asciidoc.asg.Location
import org.markup.poet.asciidoc.asg.Position
import org.markup.poet.asciidoc.asg.SectionBlock
import kotlin.test.Test

class DocumentValidatorTest {

    private val validator = DefaultDocumentValidator()

    private fun loc(line: Int, col: Int = 1) = Location(Position(line, col), Position(line, col))

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

    private fun paragraphOf(vararg inlines: Inline, line: Int = 1) = LeafBlock(
        name = LeafBlockName.PARAGRAPH,
        form = LeafBlockForm.PARAGRAPH,
        inlines = inlines.toList(),
        location = loc(line)
    )

    @Test
    fun `should validate document with proper section hierarchy`() {
        // Arrange
        val document = AsgDocument(
            blocks = listOf(
                section(
                    "Section 1", level = 1, line = 1,
                    blocks = listOf(section("Section 1.1", level = 2, line = 3))
                )
            ),
            location = loc(1)
        )

        val config = ValidationConfig()

        // Act
        val result = validator.validate(document, config)

        // Assert
        result.isValid shouldBe true
        result.errors.shouldBeEmpty()
        result.warnings.shouldBeEmpty()
    }

    @Test
    fun `should detect skipped section levels`() {
        // Arrange
        val document = AsgDocument(
            blocks = listOf(
                section(
                    "Section 1", level = 1, line = 1,
                    blocks = listOf(
                        section("Section 1.1", level = 3, line = 3) // Skips level 2
                    )
                )
            ),
            location = loc(1)
        )

        val config = ValidationConfig()

        // Act
        val result = validator.validate(document, config)

        // Assert
        result.isValid shouldBe true // No errors, only warnings
        result.errors.shouldBeEmpty()
        result.warnings shouldHaveSize 1
        result.warnings[0].warningType shouldBe ProcessingWarningType.SECTION_HIERARCHY_VIOLATION
        result.warnings[0].message shouldBe "Section level 3 skips from level 1 (expected level 2)"
    }

    @Test
    fun `should detect duplicate anchor IDs`() {
        // Arrange
        val document = AsgDocument(
            blocks = listOf(
                section("Section 1", level = 1, line = 1, id = "duplicate"),
                section("Section 2", level = 1, line = 5, id = "duplicate")
            ),
            location = loc(1)
        )

        val config = ValidationConfig()

        // Act
        val result = validator.validate(document, config)

        // Assert
        result.isValid shouldBe false
        result.errors shouldHaveSize 1
        result.errors[0].errorType shouldBe ProcessingErrorType.VALIDATION_DUPLICATE_ANCHOR
        result.errors[0].message shouldBe "Duplicate anchor ID 'duplicate' found at: line 1, line 5"
    }

    @Test
    fun `should detect invalid attribute references`() {
        // Arrange
        val document = AsgDocument(
            blocks = listOf(
                paragraphOf(
                    InlineText("Hello ", loc(1, 1)),
                    InlineAttributeRef("undefined", loc(1, 6)),
                    InlineText("!", loc(1, 18))
                )
            ),
            location = loc(1)
        )

        val config = ValidationConfig()

        // Act
        val result = validator.validate(document, config)

        // Assert
        result.isValid shouldBe true // No errors, only warnings
        result.errors.shouldBeEmpty()
        result.warnings shouldHaveSize 1
        result.warnings[0].warningType shouldBe ProcessingWarningType.ATTRIBUTE_UNDEFINED
        result.warnings[0].message shouldBe "Invalid attribute reference '{undefined}' - attribute is not defined"
    }

    @Test
    fun `should not report warnings for defined attributes`() {
        // Arrange
        val document = AsgDocument(
            attributes = mapOf("author" to "John Doe"),
            blocks = listOf(
                paragraphOf(
                    InlineText("Hello ", loc(1, 1)),
                    InlineAttributeRef("author", loc(1, 6)),
                    InlineText("!", loc(1, 14))
                )
            ),
            location = loc(1)
        )

        val config = ValidationConfig()

        // Act
        val result = validator.validate(document, config)

        // Assert
        result.isValid shouldBe true
        result.errors.shouldBeEmpty()
        result.warnings.shouldBeEmpty()
    }

    @Test
    fun `should detect whitespace issues in section titles`() {
        // Arrange
        val document = AsgDocument(
            blocks = listOf(
                section(" Section with leading space", level = 1, line = 1),
                section("Section with trailing space ", level = 1, line = 3)
            ),
            location = loc(1)
        )

        val config = ValidationConfig()

        // Act
        val result = validator.validate(document, config)

        // Assert
        result.isValid shouldBe true // No errors, only warnings
        result.errors.shouldBeEmpty()
        result.warnings shouldHaveSize 2
        result.warnings.all { it.warningType == ProcessingWarningType.WHITESPACE_NORMALIZATION } shouldBe true
    }

    @Test
    fun `should detect multiple consecutive spaces in text`() {
        // Arrange
        val document = AsgDocument(
            blocks = listOf(
                paragraphOf(InlineText("Hello  world", loc(1)))
            ),
            location = loc(1)
        )

        val config = ValidationConfig()

        // Act
        val result = validator.validate(document, config)

        // Assert
        result.isValid shouldBe true // No errors, only warnings
        result.errors.shouldBeEmpty()
        result.warnings shouldHaveSize 1
        result.warnings[0].warningType shouldBe ProcessingWarningType.WHITESPACE_NORMALIZATION
        result.warnings[0].message shouldBe "Text contains multiple consecutive spaces"
    }

    @Test
    fun `should respect validation configuration`() {
        // Arrange
        val document = AsgDocument(
            blocks = listOf(
                section(
                    "Section 1", level = 1, line = 1,
                    blocks = listOf(
                        section("Section 1.1", level = 3, line = 3) // Skips level 2
                    )
                )
            ),
            location = loc(1)
        )

        // Disable section hierarchy checking
        val config = ValidationConfig(checkSectionHierarchy = false)

        // Act
        val result = validator.validate(document, config)

        // Assert
        result.isValid shouldBe true
        result.errors.shouldBeEmpty()
        result.warnings.shouldBeEmpty() // No warnings because hierarchy checking is disabled
    }
}
