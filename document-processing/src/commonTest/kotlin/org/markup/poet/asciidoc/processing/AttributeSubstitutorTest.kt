package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.ast.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AttributeSubstitutorTest {
    
    private val substitutor = DefaultAttributeSubstitutor()
    
    @Test
    fun `should substitute simple attribute reference in text`() {
        // Arrange
        val document = Document(
            title = "Test",
            children = listOf(
                Paragraph(
                    content = listOf(
                        Text("Hello {name}!", sourceLocation = SourceLocation(1))
                    ),
                    sourceLocation = SourceLocation(1)
                )
            ),
            documentAttributes = mapOf("name" to "World"),
            sourceLocation = SourceLocation(0)
        )
        
        val config = AttributeConfig()
        
        // Act
        val result = substitutor.substitute(document, config)
        
        // Assert
        assertEquals(0, result.errors.size)
        assertEquals(1, result.substitutedAttributes.size)
        assertTrue(result.substitutedAttributes.contains("name"))
        
        val paragraph = result.document.children[0] as Paragraph
        val text = paragraph.content[0] as Text
        assertEquals("Hello World!", text.content)
    }
    
    @Test
    fun `should handle undefined attribute with PRESERVE behavior`() {
        // Arrange
        val document = Document(
            title = "Test",
            children = listOf(
                Paragraph(
                    content = listOf(
                        Text("Hello {undefined}!", sourceLocation = SourceLocation(1))
                    ),
                    sourceLocation = SourceLocation(1)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(0)
        )
        
        val config = AttributeConfig(undefinedBehavior = UndefinedAttributeBehavior.PRESERVE)
        
        // Act
        val result = substitutor.substitute(document, config)
        
        // Assert
        assertEquals(1, result.errors.size)
        assertEquals(ProcessingErrorType.ATTRIBUTE_UNDEFINED, result.errors[0].errorType)
        
        val paragraph = result.document.children[0] as Paragraph
        val text = paragraph.content[0] as Text
        assertEquals("Hello {undefined}!", text.content)
    }
    
    @Test
    fun `should handle undefined attribute with REMOVE behavior`() {
        // Arrange
        val document = Document(
            title = "Test",
            children = listOf(
                Paragraph(
                    content = listOf(
                        Text("Hello {undefined}!", sourceLocation = SourceLocation(1))
                    ),
                    sourceLocation = SourceLocation(1)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(0)
        )
        
        val config = AttributeConfig(undefinedBehavior = UndefinedAttributeBehavior.REMOVE)
        
        // Act
        val result = substitutor.substitute(document, config)
        
        // Assert
        val paragraph = result.document.children[0] as Paragraph
        val text = paragraph.content[0] as Text
        assertEquals("Hello !", text.content)
    }
    
    @Test
    fun `should resolve recursive attribute references`() {
        // Arrange
        val document = Document(
            title = "Test",
            children = listOf(
                Paragraph(
                    content = listOf(
                        Text("Message: {greeting}", sourceLocation = SourceLocation(1))
                    ),
                    sourceLocation = SourceLocation(1)
                )
            ),
            documentAttributes = mapOf(
                "greeting" to "Hello {name}!",
                "name" to "World"
            ),
            sourceLocation = SourceLocation(0)
        )
        
        val config = AttributeConfig()
        
        // Act
        val result = substitutor.substitute(document, config)
        
        // Assert
        assertEquals(0, result.errors.size)
        assertEquals(2, result.substitutedAttributes.size)
        
        val paragraph = result.document.children[0] as Paragraph
        val text = paragraph.content[0] as Text
        assertEquals("Message: Hello World!", text.content)
    }
    
    @Test
    fun `should detect circular attribute references`() {
        // Arrange
        val document = Document(
            title = "Test",
            children = listOf(
                Paragraph(
                    content = listOf(
                        Text("Value: {a}", sourceLocation = SourceLocation(1))
                    ),
                    sourceLocation = SourceLocation(1)
                )
            ),
            documentAttributes = mapOf(
                "a" to "{b}",
                "b" to "{c}",
                "c" to "{a}"
            ),
            sourceLocation = SourceLocation(0)
        )
        
        val config = AttributeConfig()
        
        // Act
        val result = substitutor.substitute(document, config)
        
        // Assert
        assertTrue(result.errors.any { it.errorType == ProcessingErrorType.ATTRIBUTE_CIRCULAR_REFERENCE })
        
        val paragraph = result.document.children[0] as Paragraph
        val text = paragraph.content[0] as Text
        // Should preserve the reference when circular dependency is detected
        assertTrue(text.content.contains("{"))
    }
    
    @Test
    fun `should handle AttributeReference inline elements`() {
        // Arrange
        val document = Document(
            title = "Test",
            children = listOf(
                Paragraph(
                    content = listOf(
                        Text("Hello ", sourceLocation = SourceLocation(1)),
                        AttributeReference("name", sourceLocation = SourceLocation(1)),
                        Text("!", sourceLocation = SourceLocation(1))
                    ),
                    sourceLocation = SourceLocation(1)
                )
            ),
            documentAttributes = mapOf("name" to "World"),
            sourceLocation = SourceLocation(0)
        )
        
        val config = AttributeConfig()
        
        // Act
        val result = substitutor.substitute(document, config)
        
        // Assert
        assertEquals(0, result.errors.size)
        
        val paragraph = result.document.children[0] as Paragraph
        assertEquals(3, paragraph.content.size)
        assertEquals("World", (paragraph.content[1] as Text).content)
    }
    
    @Test
    fun `should use default values when configured`() {
        // Arrange
        val document = Document(
            title = "Test",
            children = listOf(
                Paragraph(
                    content = listOf(
                        Text("Hello {name}!", sourceLocation = SourceLocation(1))
                    ),
                    sourceLocation = SourceLocation(1)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(0)
        )
        
        val config = AttributeConfig(
            defaults = mapOf("name" to "Default"),
            undefinedBehavior = UndefinedAttributeBehavior.DEFAULT
        )
        
        // Act
        val result = substitutor.substitute(document, config)
        
        // Assert
        assertEquals(0, result.errors.size)
        
        val paragraph = result.document.children[0] as Paragraph
        val text = paragraph.content[0] as Text
        assertEquals("Hello Default!", text.content)
    }
    
    @Test
    fun `should respect max recursion depth`() {
        // Arrange
        val document = Document(
            title = "Test",
            children = listOf(
                Paragraph(
                    content = listOf(
                        Text("Value: {a}", sourceLocation = SourceLocation(1))
                    ),
                    sourceLocation = SourceLocation(1)
                )
            ),
            documentAttributes = mapOf(
                "a" to "{b}",
                "b" to "{c}",
                "c" to "{d}",
                "d" to "{e}",
                "e" to "final"
            ),
            sourceLocation = SourceLocation(0)
        )
        
        val config = AttributeConfig(maxRecursionDepth = 3)
        
        // Act
        val result = substitutor.substitute(document, config)
        
        // Assert
        assertTrue(result.errors.any { it.errorType == ProcessingErrorType.ATTRIBUTE_CIRCULAR_REFERENCE })
    }
}
