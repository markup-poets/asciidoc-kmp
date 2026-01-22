package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.ast.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConditionalProcessorTest {
    
    private val processor = DefaultConditionalProcessor()
    private val location = SourceLocation(1, 1)
    
    private fun createDocument(children: List<BlockElement>): Document {
        return Document(
            title = null,
            children = children,
            documentAttributes = emptyMap(),
            sourceLocation = location
        )
    }
    
    @Test
    fun `should include content when ifdef attribute is defined`() {
        val config = ConditionalConfig(definedAttributes = setOf("myattr"))
        val content = listOf(
            Paragraph(
                content = listOf(Text("Included content", sourceLocation = location)),
                sourceLocation = location
            )
        )
        val directive = ConditionalDirective(
            type = ConditionalType.IFDEF,
            condition = "myattr",
            content = content,
            sourceLocation = location
        )
        val document = createDocument(listOf(directive))
        
        val result = processor.process(document, config)
        
        assertEquals(1, result.evaluatedConditionals)
        assertEquals(1, result.document.children.size)
        assertTrue(result.document.children[0] is Paragraph)
        assertTrue(result.errors.isEmpty())
    }
    
    @Test
    fun `should exclude content when ifdef attribute is not defined`() {
        val config = ConditionalConfig(definedAttributes = emptySet())
        val content = listOf(
            Paragraph(
                content = listOf(Text("Excluded content", sourceLocation = location)),
                sourceLocation = location
            )
        )
        val directive = ConditionalDirective(
            type = ConditionalType.IFDEF,
            condition = "myattr",
            content = content,
            sourceLocation = location
        )
        val document = createDocument(listOf(directive))
        
        val result = processor.process(document, config)
        
        assertEquals(1, result.evaluatedConditionals)
        assertEquals(0, result.document.children.size)
        assertTrue(result.errors.isEmpty())
    }
    
    @Test
    fun `should include content when ifndef attribute is not defined`() {
        val config = ConditionalConfig(definedAttributes = emptySet())
        val content = listOf(
            Paragraph(
                content = listOf(Text("Included content", sourceLocation = location)),
                sourceLocation = location
            )
        )
        val directive = ConditionalDirective(
            type = ConditionalType.IFNDEF,
            condition = "myattr",
            content = content,
            sourceLocation = location
        )
        val document = createDocument(listOf(directive))
        
        val result = processor.process(document, config)
        
        assertEquals(1, result.evaluatedConditionals)
        assertEquals(1, result.document.children.size)
        assertTrue(result.document.children[0] is Paragraph)
        assertTrue(result.errors.isEmpty())
    }
    
    @Test
    fun `should exclude content when ifndef attribute is defined`() {
        val config = ConditionalConfig(definedAttributes = setOf("myattr"))
        val content = listOf(
            Paragraph(
                content = listOf(Text("Excluded content", sourceLocation = location)),
                sourceLocation = location
            )
        )
        val directive = ConditionalDirective(
            type = ConditionalType.IFNDEF,
            condition = "myattr",
            content = content,
            sourceLocation = location
        )
        val document = createDocument(listOf(directive))
        
        val result = processor.process(document, config)
        
        assertEquals(1, result.evaluatedConditionals)
        assertEquals(0, result.document.children.size)
        assertTrue(result.errors.isEmpty())
    }
    
    @Test
    fun `should evaluate ifeval with equality operator`() {
        val config = ConditionalConfig(definedAttributes = setOf("version"))
        val content = listOf(
            Paragraph(
                content = listOf(Text("Version matches", sourceLocation = location)),
                sourceLocation = location
            )
        )
        val directive = ConditionalDirective(
            type = ConditionalType.IFEVAL,
            condition = """{version} == "version"""",
            content = content,
            sourceLocation = location
        )
        val document = createDocument(listOf(directive))
        
        val result = processor.process(document, config)
        
        assertEquals(1, result.evaluatedConditionals)
        assertEquals(1, result.document.children.size)
        assertTrue(result.errors.isEmpty())
    }
    
    @Test
    fun `should handle nested conditionals`() {
        val config = ConditionalConfig(definedAttributes = setOf("outer", "inner"))
        val innerContent = listOf(
            Paragraph(
                content = listOf(Text("Inner content", sourceLocation = location)),
                sourceLocation = location
            )
        )
        val innerDirective = ConditionalDirective(
            type = ConditionalType.IFDEF,
            condition = "inner",
            content = innerContent,
            sourceLocation = location
        )
        val outerDirective = ConditionalDirective(
            type = ConditionalType.IFDEF,
            condition = "outer",
            content = listOf(innerDirective),
            sourceLocation = location
        )
        val document = createDocument(listOf(outerDirective))
        
        val result = processor.process(document, config)
        
        assertEquals(2, result.evaluatedConditionals)
        assertEquals(1, result.document.children.size)
        assertTrue(result.errors.isEmpty())
    }
    
    @Test
    fun `should report error when max nesting depth exceeded`() {
        val config = ConditionalConfig(
            definedAttributes = setOf("attr"),
            maxNestingDepth = 2
        )
        
        // Create deeply nested conditionals
        var innermost: BlockElement = Paragraph(
            content = listOf(Text("Deep content", sourceLocation = location)),
            sourceLocation = location
        )
        
        for (i in 0 until 5) {
            innermost = ConditionalDirective(
                type = ConditionalType.IFDEF,
                condition = "attr",
                content = listOf(innermost),
                sourceLocation = location
            )
        }
        
        val document = createDocument(listOf(innermost))
        
        val result = processor.process(document, config)
        
        assertTrue(result.errors.isNotEmpty())
        assertTrue(result.errors.any { it.errorType == ProcessingErrorType.CONDITIONAL_MAX_DEPTH_EXCEEDED })
    }
    
    @Test
    fun `should support OR operator with comma-separated attributes`() {
        val config = ConditionalConfig(definedAttributes = setOf("attr2"))
        val content = listOf(
            Paragraph(
                content = listOf(Text("At least one defined", sourceLocation = location)),
                sourceLocation = location
            )
        )
        val directive = ConditionalDirective(
            type = ConditionalType.IFDEF,
            condition = "attr1,attr2,attr3",
            content = content,
            sourceLocation = location
        )
        val document = createDocument(listOf(directive))
        
        val result = processor.process(document, config)
        
        assertEquals(1, result.evaluatedConditionals)
        assertEquals(1, result.document.children.size)
        assertTrue(result.errors.isEmpty())
    }
    
    @Test
    fun `should support AND operator with plus-separated attributes`() {
        val config = ConditionalConfig(definedAttributes = setOf("attr1", "attr2"))
        val content = listOf(
            Paragraph(
                content = listOf(Text("All defined", sourceLocation = location)),
                sourceLocation = location
            )
        )
        val directive = ConditionalDirective(
            type = ConditionalType.IFDEF,
            condition = "attr1+attr2",
            content = content,
            sourceLocation = location
        )
        val document = createDocument(listOf(directive))
        
        val result = processor.process(document, config)
        
        assertEquals(1, result.evaluatedConditionals)
        assertEquals(1, result.document.children.size)
        assertTrue(result.errors.isEmpty())
    }
    
    @Test
    fun `should exclude when AND condition not fully satisfied`() {
        val config = ConditionalConfig(definedAttributes = setOf("attr1"))
        val content = listOf(
            Paragraph(
                content = listOf(Text("Should be excluded", sourceLocation = location)),
                sourceLocation = location
            )
        )
        val directive = ConditionalDirective(
            type = ConditionalType.IFDEF,
            condition = "attr1+attr2",
            content = content,
            sourceLocation = location
        )
        val document = createDocument(listOf(directive))
        
        val result = processor.process(document, config)
        
        assertEquals(1, result.evaluatedConditionals)
        assertEquals(0, result.document.children.size)
        assertTrue(result.errors.isEmpty())
    }
    
    @Test
    fun `should report error for invalid ifeval expression`() {
        val config = ConditionalConfig(definedAttributes = emptySet())
        val directive = ConditionalDirective(
            type = ConditionalType.IFEVAL,
            condition = "invalid expression",
            content = emptyList(),
            sourceLocation = location
        )
        val document = createDocument(listOf(directive))
        
        val result = processor.process(document, config)
        
        assertTrue(result.errors.isNotEmpty())
        assertTrue(result.errors.any { it.errorType == ProcessingErrorType.CONDITIONAL_INVALID_EXPRESSION })
    }
    
    @Test
    fun `should use else content when condition is false`() {
        val config = ConditionalConfig(definedAttributes = emptySet())
        val ifContent = listOf(
            Paragraph(
                content = listOf(Text("If content", sourceLocation = location)),
                sourceLocation = location
            )
        )
        val elseContent = listOf(
            Paragraph(
                content = listOf(Text("Else content", sourceLocation = location)),
                sourceLocation = location
            )
        )
        val directive = ConditionalDirective(
            type = ConditionalType.IFDEF,
            condition = "undefined",
            content = ifContent,
            elseContent = elseContent,
            sourceLocation = location
        )
        val document = createDocument(listOf(directive))
        
        val result = processor.process(document, config)
        
        assertEquals(1, result.evaluatedConditionals)
        assertEquals(1, result.document.children.size)
        val paragraph = result.document.children[0] as Paragraph
        val text = paragraph.content[0] as Text
        assertEquals("Else content", text.content)
    }
}
