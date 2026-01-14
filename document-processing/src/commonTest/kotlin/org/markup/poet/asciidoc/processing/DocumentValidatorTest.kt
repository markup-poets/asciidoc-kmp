package org.markup.poet.asciidoc.processing

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.markup.poet.asciidoc.ast.*
import kotlin.test.Test

class DocumentValidatorTest {
    
    private val validator = DefaultDocumentValidator()
    
    @Test
    fun `should validate document with proper section hierarchy`() {
        // Arrange
        val document = Document(
            title = "Test Document",
            children = listOf(
                Section(
                    level = 1,
                    title = "Section 1",
                    children = listOf(
                        Section(
                            level = 2,
                            title = "Section 1.1",
                            children = emptyList(),
                            sourceLocation = SourceLocation(3, 0)
                        )
                    ),
                    sourceLocation = SourceLocation(1, 0)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(0, 0)
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
        val document = Document(
            title = "Test Document",
            children = listOf(
                Section(
                    level = 1,
                    title = "Section 1",
                    children = listOf(
                        Section(
                            level = 3, // Skips level 2
                            title = "Section 1.1",
                            children = emptyList(),
                            sourceLocation = SourceLocation(3, 0)
                        )
                    ),
                    sourceLocation = SourceLocation(1, 0)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(0, 0)
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
        val document = Document(
            title = "Test Document",
            children = listOf(
                Section(
                    level = 1,
                    title = "Section 1",
                    children = emptyList(),
                    attributes = mapOf("id" to "duplicate"),
                    sourceLocation = SourceLocation(1, 0)
                ),
                Section(
                    level = 1,
                    title = "Section 2",
                    children = emptyList(),
                    attributes = mapOf("id" to "duplicate"),
                    sourceLocation = SourceLocation(5, 0)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(0, 0)
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
        val document = Document(
            title = "Test Document",
            children = listOf(
                Paragraph(
                    content = listOf(
                        Text("Hello ", sourceLocation = SourceLocation(1, 0)),
                        AttributeReference("undefined", sourceLocation = SourceLocation(1, 6)),
                        Text("!", sourceLocation = SourceLocation(1, 18))
                    ),
                    sourceLocation = SourceLocation(1, 0)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(0, 0)
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
        val document = Document(
            title = "Test Document",
            children = listOf(
                Paragraph(
                    content = listOf(
                        Text("Hello ", sourceLocation = SourceLocation(1, 0)),
                        AttributeReference("author", sourceLocation = SourceLocation(1, 6)),
                        Text("!", sourceLocation = SourceLocation(1, 14))
                    ),
                    sourceLocation = SourceLocation(1, 0)
                )
            ),
            documentAttributes = mapOf("author" to "John Doe"),
            sourceLocation = SourceLocation(0, 0)
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
        val document = Document(
            title = "Test Document",
            children = listOf(
                Section(
                    level = 1,
                    title = " Section with leading space",
                    children = emptyList(),
                    sourceLocation = SourceLocation(1, 0)
                ),
                Section(
                    level = 1,
                    title = "Section with trailing space ",
                    children = emptyList(),
                    sourceLocation = SourceLocation(3, 0)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(0, 0)
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
        val document = Document(
            title = "Test Document",
            children = listOf(
                Paragraph(
                    content = listOf(
                        Text("Hello  world", sourceLocation = SourceLocation(1, 0))
                    ),
                    sourceLocation = SourceLocation(1, 0)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(0, 0)
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
        val document = Document(
            title = "Test Document",
            children = listOf(
                Section(
                    level = 1,
                    title = "Section 1",
                    children = listOf(
                        Section(
                            level = 3, // Skips level 2
                            title = "Section 1.1",
                            children = emptyList(),
                            sourceLocation = SourceLocation(3, 0)
                        )
                    ),
                    sourceLocation = SourceLocation(1, 0)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(0, 0)
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
