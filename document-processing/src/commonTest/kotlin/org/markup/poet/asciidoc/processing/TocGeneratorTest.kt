package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.ast.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TocGeneratorTest {
    
    private val generator = DefaultTocGenerator()
    
    @Test
    fun `should return null TOC for document with no sections`() {
        val document = Document(
            title = "Test Document",
            children = listOf(
                Paragraph(
                    content = listOf(Text("Some content", sourceLocation = SourceLocation(1))),
                    sourceLocation = SourceLocation(1)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(0)
        )
        
        val config = TocConfig(maxDepth = 3)
        val result = generator.generate(document, config)
        
        assertNull(result.tocNode)
        assertTrue(result.errors.isEmpty())
    }
    
    @Test
    fun `should generate TOC with single section`() {
        val document = Document(
            title = "Test Document",
            children = listOf(
                Section(
                    level = 1,
                    title = "Introduction",
                    children = emptyList(),
                    attributes = mapOf("id" to "intro"),
                    sourceLocation = SourceLocation(1)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(0)
        )
        
        val config = TocConfig(maxDepth = 3)
        val result = generator.generate(document, config)
        
        assertNotNull(result.tocNode)
        assertEquals(1, result.tocNode.items.size)
        
        val item = result.tocNode.items[0]
        assertEquals(1, item.content.size)
        
        val crossRef = item.content[0] as CrossReference
        assertEquals("intro", crossRef.targetId)
        assertEquals("Introduction", crossRef.customText)
    }
    
    @Test
    fun `should generate hierarchical TOC with nested sections`() {
        val document = Document(
            title = "Test Document",
            children = listOf(
                Section(
                    level = 1,
                    title = "Chapter 1",
                    children = listOf(
                        Section(
                            level = 2,
                            title = "Section 1.1",
                            children = emptyList(),
                            attributes = mapOf("id" to "sec-1-1"),
                            sourceLocation = SourceLocation(3)
                        ),
                        Section(
                            level = 2,
                            title = "Section 1.2",
                            children = emptyList(),
                            attributes = mapOf("id" to "sec-1-2"),
                            sourceLocation = SourceLocation(5)
                        )
                    ),
                    attributes = mapOf("id" to "chap-1"),
                    sourceLocation = SourceLocation(1)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(0)
        )
        
        val config = TocConfig(maxDepth = 3)
        val result = generator.generate(document, config)
        
        assertNotNull(result.tocNode)
        assertEquals(1, result.tocNode.items.size)
        
        val chapter = result.tocNode.items[0]
        assertNotNull(chapter.nestedList)
        assertEquals(2, chapter.nestedList!!.items.size)
    }
    
    @Test
    fun `should respect depth limit`() {
        val document = Document(
            title = "Test Document",
            children = listOf(
                Section(
                    level = 1,
                    title = "Level 1",
                    children = listOf(
                        Section(
                            level = 2,
                            title = "Level 2",
                            children = listOf(
                                Section(
                                    level = 3,
                                    title = "Level 3",
                                    children = listOf(
                                        Section(
                                            level = 4,
                                            title = "Level 4",
                                            children = emptyList(),
                                            sourceLocation = SourceLocation(7)
                                        )
                                    ),
                                    sourceLocation = SourceLocation(5)
                                )
                            ),
                            sourceLocation = SourceLocation(3)
                        )
                    ),
                    sourceLocation = SourceLocation(1)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(0)
        )
        
        val config = TocConfig(maxDepth = 2)
        val result = generator.generate(document, config)
        
        assertNotNull(result.tocNode)
        assertEquals(1, result.tocNode.items.size)
        
        val level1 = result.tocNode.items[0]
        assertNotNull(level1.nestedList)
        assertEquals(1, level1.nestedList!!.items.size)
        
        val level2 = level1.nestedList!!.items[0]
        assertNull(level2.nestedList) // Level 3 and 4 should be excluded
    }
    
    @Test
    fun `should filter out untitled sections`() {
        val document = Document(
            title = "Test Document",
            children = listOf(
                Section(
                    level = 1,
                    title = "Chapter 1",
                    children = emptyList(),
                    sourceLocation = SourceLocation(1)
                ),
                Section(
                    level = 1,
                    title = "",
                    children = emptyList(),
                    sourceLocation = SourceLocation(3)
                ),
                Section(
                    level = 1,
                    title = "Chapter 2",
                    children = emptyList(),
                    sourceLocation = SourceLocation(5)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(0)
        )
        
        val config = TocConfig(maxDepth = 3)
        val result = generator.generate(document, config)
        
        assertNotNull(result.tocNode)
        assertEquals(2, result.tocNode.items.size) // Only titled sections
    }
    
    @Test
    fun `should generate anchor IDs for sections without explicit IDs`() {
        val document = Document(
            title = "Test Document",
            children = listOf(
                Section(
                    level = 1,
                    title = "Hello World",
                    children = emptyList(),
                    sourceLocation = SourceLocation(1)
                )
            ),
            documentAttributes = emptyMap(),
            sourceLocation = SourceLocation(0)
        )
        
        val config = TocConfig(maxDepth = 3)
        val result = generator.generate(document, config)
        
        assertNotNull(result.tocNode)
        assertEquals(1, result.tocNode.items.size)
        
        val item = result.tocNode.items[0]
        val crossRef = item.content[0] as CrossReference
        assertEquals("hello-world", crossRef.targetId) // Generated from title
    }
}
