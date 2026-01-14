package org.markup.poet.asciidoc.parser

import org.markup.poet.asciidoc.ast.CodeBlock
import org.markup.poet.asciidoc.ast.Document
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CodeBlockParsingTest {
    private val parser = DefaultAsciidocParser()

    @Test
    fun `should parse code block with source attribute correctly`() {
        val adoc = """
[source,kotlin]
----
interface Converter<Output> {
    fun convert(document: Document): Output
}
----
        """.trimIndent()

        val result = parser.parse(adoc)
        val document = result.document
        
        assertTrue(result.errors.isEmpty(), "Should have no errors, but found: ${result.errors}")
        assertEquals(1, document.children.size, "Document should have one child")
        
        val codeBlock = document.children[0] as? CodeBlock
        assertTrue(codeBlock != null, "First child should be a CodeBlock, but was ${document.children[0]::class}")
        
        assertEquals("kotlin", codeBlock.language)
        val expectedContent = """
interface Converter<Output> {
    fun convert(document: Document): Output
}
        """.trimIndent()
        assertEquals(expectedContent, codeBlock.content)
    }
}
