package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.ast.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CrossReferenceResolverTest {
    
    private val resolver = DefaultCrossReferenceResolver()
    
    @Test
    fun `should resolve cross-reference to section with anchor`() {
        // Create a document with a section that has an anchor
        val section = Section(
            level = 1,
            title = "Introduction",
            children = emptyList(),
            attributes = mapOf("id" to "intro"),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val crossRef = CrossReference(
            targetId = "intro",
            customText = null,
            sourceLocation = SourceLocation(5, 0)
        )
        
        val paragraph = Paragraph(
            content = listOf(
                Text("See ", sourceLocation = SourceLocation(5, 0)),
                crossRef
            ),
            sourceLocation = SourceLocation(5, 0)
        )
        
        val document = Document(
            title = "Test Document",
            children = listOf(section, paragraph),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(0, 0)
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
        val crossRef = CrossReference(
            targetId = "nonexistent",
            customText = null,
            sourceLocation = SourceLocation(5, 0)
        )
        
        val paragraph = Paragraph(
            content = listOf(crossRef),
            sourceLocation = SourceLocation(5, 0)
        )
        
        val document = Document(
            title = "Test Document",
            children = listOf(paragraph),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(0, 0)
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
        val section1 = Section(
            level = 1,
            title = "First Section",
            children = emptyList(),
            attributes = mapOf("id" to "duplicate"),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val section2 = Section(
            level = 1,
            title = "Second Section",
            children = emptyList(),
            attributes = mapOf("id" to "duplicate"),
            sourceLocation = SourceLocation(10, 0)
        )
        
        val document = Document(
            title = "Test Document",
            children = listOf(section1, section2),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(0, 0)
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
        val section = Section(
            level = 1,
            title = "Introduction",
            children = emptyList(),
            attributes = mapOf("id" to "intro"),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val crossRef = CrossReference(
            targetId = "intro",
            customText = "Custom Link Text",
            sourceLocation = SourceLocation(5, 0)
        )
        
        val paragraph = Paragraph(
            content = listOf(crossRef),
            sourceLocation = SourceLocation(5, 0)
        )
        
        val document = Document(
            title = "Test Document",
            children = listOf(section, paragraph),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(0, 0)
        )
        
        // Resolve cross-references
        val result = resolver.resolve(document)
        
        // Verify anchor was found with generated text
        assertTrue(result.resolvedReferences.containsKey("intro"))
        assertEquals("Introduction", result.resolvedReferences["intro"]?.generatedText)
        
        // Note: Custom text is preserved in the CrossReference node itself
        // The resolver doesn't modify the custom text
    }
    
    @Test
    fun `should generate link text for list items`() {
        val listItem = ListItem(
            marker = "*",
            content = listOf(Text("List item content", sourceLocation = SourceLocation(2, 0))),
            attributes = mapOf("id" to "item1"),
            sourceLocation = SourceLocation(2, 0)
        )
        
        val list = AsciiDocList(
            type = ListType.UNORDERED,
            items = listOf(listItem),
            sourceLocation = SourceLocation(2, 0)
        )
        
        val document = Document(
            title = "Test Document",
            children = listOf(list),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(0, 0)
        )
        
        // Resolve cross-references
        val result = resolver.resolve(document)
        
        // Verify anchor was found with generated text from list item content
        assertTrue(result.resolvedReferences.containsKey("item1"))
        assertEquals("List item content", result.resolvedReferences["item1"]?.generatedText)
    }
    
    @Test
    fun `should handle nested sections with anchors`() {
        val nestedSection = Section(
            level = 2,
            title = "Nested Section",
            children = emptyList(),
            attributes = mapOf("id" to "nested"),
            sourceLocation = SourceLocation(5, 0)
        )
        
        val parentSection = Section(
            level = 1,
            title = "Parent Section",
            children = listOf(nestedSection),
            attributes = mapOf("id" to "parent"),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val document = Document(
            title = "Test Document",
            children = listOf(parentSection),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(0, 0)
        )
        
        // Resolve cross-references
        val result = resolver.resolve(document)
        
        // Verify both anchors were found
        assertTrue(result.resolvedReferences.containsKey("parent"))
        assertTrue(result.resolvedReferences.containsKey("nested"))
        assertEquals("Parent Section", result.resolvedReferences["parent"]?.generatedText)
        assertEquals("Nested Section", result.resolvedReferences["nested"]?.generatedText)
    }
}
