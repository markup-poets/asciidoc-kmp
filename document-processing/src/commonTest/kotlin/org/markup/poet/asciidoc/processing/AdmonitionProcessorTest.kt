package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.ast.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AdmonitionProcessorTest {
    
    private val processor = DefaultAdmonitionProcessor()
    private val testLocation = SourceLocation(1, 1)
    
    @Test
    fun `should recognize NOTE admonition with inline syntax`() {
        val paragraph = Paragraph(
            content = listOf(Text("NOTE: This is a note", emptyMap(), testLocation)),
            attributes = emptyMap(),
            sourceLocation = testLocation
        )
        val document = Document(
            title = null,
            children = listOf(paragraph),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = testLocation
        )
        
        val result = processor.process(document)
        
        assertEquals(1, result.document.children.size)
        val admonition = result.document.children.first() as AdmonitionBlock
        assertEquals(AdmonitionType.NOTE, admonition.type)
        assertEquals(1, result.admonitionCount[AdmonitionType.NOTE])
    }
    
    @Test
    fun `should recognize TIP admonition with inline syntax`() {
        val paragraph = Paragraph(
            content = listOf(Text("TIP: This is a tip", emptyMap(), testLocation)),
            attributes = emptyMap(),
            sourceLocation = testLocation
        )
        val document = Document(
            title = null,
            children = listOf(paragraph),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = testLocation
        )
        
        val result = processor.process(document)
        
        val admonition = result.document.children.first() as AdmonitionBlock
        assertEquals(AdmonitionType.TIP, admonition.type)
    }
    
    @Test
    fun `should recognize WARNING admonition with inline syntax`() {
        val paragraph = Paragraph(
            content = listOf(Text("WARNING: This is a warning", emptyMap(), testLocation)),
            attributes = emptyMap(),
            sourceLocation = testLocation
        )
        val document = Document(
            title = null,
            children = listOf(paragraph),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = testLocation
        )
        
        val result = processor.process(document)
        
        val admonition = result.document.children.first() as AdmonitionBlock
        assertEquals(AdmonitionType.WARNING, admonition.type)
    }
    
    @Test
    fun `should recognize CAUTION admonition with inline syntax`() {
        val paragraph = Paragraph(
            content = listOf(Text("CAUTION: This is a caution", emptyMap(), testLocation)),
            attributes = emptyMap(),
            sourceLocation = testLocation
        )
        val document = Document(
            title = null,
            children = listOf(paragraph),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = testLocation
        )
        
        val result = processor.process(document)
        
        val admonition = result.document.children.first() as AdmonitionBlock
        assertEquals(AdmonitionType.CAUTION, admonition.type)
    }
    
    @Test
    fun `should recognize IMPORTANT admonition with inline syntax`() {
        val paragraph = Paragraph(
            content = listOf(Text("IMPORTANT: This is important", emptyMap(), testLocation)),
            attributes = emptyMap(),
            sourceLocation = testLocation
        )
        val document = Document(
            title = null,
            children = listOf(paragraph),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = testLocation
        )
        
        val result = processor.process(document)
        
        val admonition = result.document.children.first() as AdmonitionBlock
        assertEquals(AdmonitionType.IMPORTANT, admonition.type)
    }
    
    @Test
    fun `should recognize admonition with block syntax using style attribute`() {
        val paragraph = Paragraph(
            content = listOf(Text("This is the content", emptyMap(), testLocation)),
            attributes = mapOf("style" to "NOTE"),
            sourceLocation = testLocation
        )
        val document = Document(
            title = null,
            children = listOf(paragraph),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = testLocation
        )
        
        val result = processor.process(document)
        
        val admonition = result.document.children.first() as AdmonitionBlock
        assertEquals(AdmonitionType.NOTE, admonition.type)
        assertEquals(1, admonition.content.size)
    }
    
    @Test
    fun `should handle custom title in admonition`() {
        val paragraph = Paragraph(
            content = listOf(Text("Content here", emptyMap(), testLocation)),
            attributes = mapOf("style" to "TIP", "title" to "Custom Title"),
            sourceLocation = testLocation
        )
        val document = Document(
            title = null,
            children = listOf(paragraph),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = testLocation
        )
        
        val result = processor.process(document)
        
        val admonition = result.document.children.first() as AdmonitionBlock
        assertEquals(AdmonitionType.TIP, admonition.type)
        assertEquals("Custom Title", admonition.title)
    }
    
    @Test
    fun `should report warning for invalid admonition type`() {
        val paragraph = Paragraph(
            content = listOf(Text("Content", emptyMap(), testLocation)),
            attributes = mapOf("style" to "INVALID"),
            sourceLocation = testLocation
        )
        val document = Document(
            title = null,
            children = listOf(paragraph),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = testLocation
        )
        
        val result = processor.process(document)
        
        assertEquals(1, result.warnings.size)
        assertEquals(ProcessingWarningType.ADMONITION_INVALID_TYPE, result.warnings.first().warningType)
        assertTrue(result.warnings.first().message.contains("INVALID"))
    }
    
    @Test
    fun `should count admonitions by type`() {
        val doc = Document(
            title = null,
            children = listOf(
                Paragraph(
                    content = listOf(Text("NOTE: First note", emptyMap(), testLocation)),
                    attributes = emptyMap(),
                    sourceLocation = testLocation
                ),
                Paragraph(
                    content = listOf(Text("NOTE: Second note", emptyMap(), testLocation)),
                    attributes = emptyMap(),
                    sourceLocation = testLocation
                ),
                Paragraph(
                    content = listOf(Text("TIP: A tip", emptyMap(), testLocation)),
                    attributes = emptyMap(),
                    sourceLocation = testLocation
                )
            ),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = testLocation
        )
        
        val result = processor.process(doc)
        
        assertEquals(2, result.admonitionCount[AdmonitionType.NOTE])
        assertEquals(1, result.admonitionCount[AdmonitionType.TIP])
    }
    
    @Test
    fun `should process nested admonitions in sections`() {
        val section = Section(
            level = 1,
            title = "Test Section",
            children = listOf(
                Paragraph(
                    content = listOf(Text("WARNING: Nested warning", emptyMap(), testLocation)),
                    attributes = emptyMap(),
                    sourceLocation = testLocation
                )
            ),
            attributes = emptyMap(),
            sourceLocation = testLocation
        )
        val document = Document(
            title = null,
            children = listOf(section),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = testLocation
        )
        
        val result = processor.process(document)
        
        val processedSection = result.document.children.first() as Section
        val admonition = processedSection.children.first() as AdmonitionBlock
        assertEquals(AdmonitionType.WARNING, admonition.type)
        assertEquals(1, result.admonitionCount[AdmonitionType.WARNING])
    }
    
    @Test
    fun `should preserve content after colon in inline syntax`() {
        val paragraph = Paragraph(
            content = listOf(
                Text("NOTE: This is ", emptyMap(), testLocation),
                Strong(listOf(Text("important", emptyMap(), testLocation)), emptyMap(), testLocation),
                Text(" content", emptyMap(), testLocation)
            ),
            attributes = emptyMap(),
            sourceLocation = testLocation
        )
        val document = Document(
            title = null,
            children = listOf(paragraph),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = testLocation
        )
        
        val result = processor.process(document)
        
        val admonition = result.document.children.first() as AdmonitionBlock
        assertEquals(AdmonitionType.NOTE, admonition.type)
        assertEquals(1, admonition.content.size)
        val contentParagraph = admonition.content.first() as Paragraph
        assertTrue(contentParagraph.content.size >= 2)
    }
    
    @Test
    fun `should not process regular paragraphs as admonitions`() {
        val paragraph = Paragraph(
            content = listOf(Text("This is just regular text", emptyMap(), testLocation)),
            attributes = emptyMap(),
            sourceLocation = testLocation
        )
        val document = Document(
            title = null,
            children = listOf(paragraph),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = testLocation
        )
        
        val result = processor.process(document)
        
        assertEquals(1, result.document.children.size)
        assertTrue(result.document.children.first() is Paragraph)
        assertTrue(result.admonitionCount.isEmpty())
    }
    
    @Test
    fun `should handle case-insensitive admonition type recognition`() {
        val paragraph = Paragraph(
            content = listOf(Text("note: lowercase note", emptyMap(), testLocation)),
            attributes = emptyMap(),
            sourceLocation = testLocation
        )
        val document = Document(
            title = null,
            children = listOf(paragraph),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = testLocation
        )
        
        val result = processor.process(document)
        
        val admonition = result.document.children.first() as AdmonitionBlock
        assertEquals(AdmonitionType.NOTE, admonition.type)
    }
    
    @Test
    fun `should process already existing AdmonitionBlock nodes`() {
        val existingAdmonition = AdmonitionBlock(
            type = AdmonitionType.TIP,
            title = "Existing",
            content = listOf(
                Paragraph(
                    content = listOf(Text("Content", emptyMap(), testLocation)),
                    attributes = emptyMap(),
                    sourceLocation = testLocation
                )
            ),
            attributes = emptyMap(),
            sourceLocation = testLocation
        )
        val document = Document(
            title = null,
            children = listOf(existingAdmonition),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = testLocation
        )
        
        val result = processor.process(document)
        
        assertEquals(1, result.admonitionCount[AdmonitionType.TIP])
        assertTrue(result.document.children.first() is AdmonitionBlock)
    }
}
