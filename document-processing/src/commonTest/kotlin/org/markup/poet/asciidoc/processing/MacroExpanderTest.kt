package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.ast.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MacroExpanderTest {
    
    @Test
    fun `should expand macro with custom processor`() {
        // Create a simple macro processor that replaces macro with text
        val processor = object : MacroProcessor {
            override fun process(
                macroName: String,
                parameters: Map<String, String>,
                context: MacroContext
            ): MacroExpansionResult {
                val text = parameters["text"] ?: "default"
                return MacroExpansionResult.Success(
                    listOf(
                        Text(
                            content = "Expanded: $text",
                            attributes = emptyMap(),
                            sourceLocation = context.sourceLocation
                        )
                    )
                )
            }
        }
        
        // Create a document with a macro invocation
        val macro = MacroInvocation(
            macroName = "test",
            parameters = mapOf("text" to "hello"),
            isBlock = false,
            attributes = emptyMap(),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val paragraph = Paragraph(
            content = listOf(macro),
            attributes = emptyMap(),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val document = Document(
            title = "Test",
            children = listOf(paragraph),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = SourceLocation(0, 0)
        )
        
        // Expand macros
        val expander = DefaultMacroExpander()
        val config = MacroConfig(customMacros = mapOf("test" to processor))
        val result = expander.expand(document, config)
        
        // Verify expansion
        assertTrue(result.errors.isEmpty(), "Should have no errors")
        val processedParagraph = result.document.children.first() as Paragraph
        val expandedText = processedParagraph.content.first() as Text
        assertEquals("Expanded: hello", expandedText.content)
    }
    
    @Test
    fun `should report error for unknown macro`() {
        // Create a document with an unknown macro
        val macro = MacroInvocation(
            macroName = "unknown",
            parameters = emptyMap(),
            isBlock = false,
            attributes = emptyMap(),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val paragraph = Paragraph(
            content = listOf(macro),
            attributes = emptyMap(),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val document = Document(
            title = "Test",
            children = listOf(paragraph),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = SourceLocation(0, 0)
        )
        
        // Expand macros
        val expander = DefaultMacroExpander()
        val config = MacroConfig(customMacros = emptyMap())
        val result = expander.expand(document, config)
        
        // Verify error reporting
        assertEquals(1, result.errors.size)
        assertEquals(ProcessingErrorType.MACRO_EXPANSION_FAILED, result.errors.first().errorType)
        assertTrue(result.errors.first().message.contains("unknown"))
    }
    
    @Test
    fun `should report error when macro processor fails`() {
        // Create a macro processor that fails
        val processor = object : MacroProcessor {
            override fun process(
                macroName: String,
                parameters: Map<String, String>,
                context: MacroContext
            ): MacroExpansionResult {
                return MacroExpansionResult.Error("Processing failed")
            }
        }
        
        // Create a document with a macro
        val macro = MacroInvocation(
            macroName = "failing",
            parameters = emptyMap(),
            isBlock = false,
            attributes = emptyMap(),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val paragraph = Paragraph(
            content = listOf(macro),
            attributes = emptyMap(),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val document = Document(
            title = "Test",
            children = listOf(paragraph),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = SourceLocation(0, 0)
        )
        
        // Expand macros
        val expander = DefaultMacroExpander()
        val config = MacroConfig(customMacros = mapOf("failing" to processor))
        val result = expander.expand(document, config)
        
        // Verify error reporting
        assertEquals(1, result.errors.size)
        assertEquals(ProcessingErrorType.MACRO_EXPANSION_FAILED, result.errors.first().errorType)
        assertTrue(result.errors.first().message.contains("Processing failed"))
    }
    
    @Test
    fun `should expand nested macros in sections`() {
        // Create a macro processor
        val processor = object : MacroProcessor {
            override fun process(
                macroName: String,
                parameters: Map<String, String>,
                context: MacroContext
            ): MacroExpansionResult {
                return MacroExpansionResult.Success(
                    listOf(
                        Text(
                            content = "expanded",
                            attributes = emptyMap(),
                            sourceLocation = context.sourceLocation
                        )
                    )
                )
            }
        }
        
        // Create a document with nested structure
        val macro = MacroInvocation(
            macroName = "test",
            parameters = emptyMap(),
            isBlock = false,
            attributes = emptyMap(),
            sourceLocation = SourceLocation(2, 0)
        )
        
        val paragraph = Paragraph(
            content = listOf(macro),
            attributes = emptyMap(),
            sourceLocation = SourceLocation(2, 0)
        )
        
        val section = Section(
            level = 1,
            title = "Test Section",
            children = listOf(paragraph),
            attributes = emptyMap(),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val document = Document(
            title = "Test",
            children = listOf(section),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = SourceLocation(0, 0)
        )
        
        // Expand macros
        val expander = DefaultMacroExpander()
        val config = MacroConfig(customMacros = mapOf("test" to processor))
        val result = expander.expand(document, config)
        
        // Verify expansion in nested structure
        assertTrue(result.errors.isEmpty())
        val processedSection = result.document.children.first() as Section
        val processedParagraph = processedSection.children.first() as Paragraph
        val expandedText = processedParagraph.content.first() as Text
        assertEquals("expanded", expandedText.content)
    }
    
    @Test
    fun `should handle empty parameters`() {
        // Create a macro processor
        val processor = object : MacroProcessor {
            override fun process(
                macroName: String,
                parameters: Map<String, String>,
                context: MacroContext
            ): MacroExpansionResult {
                return MacroExpansionResult.Success(
                    listOf(
                        Text(
                            content = "no params",
                            attributes = emptyMap(),
                            sourceLocation = context.sourceLocation
                        )
                    )
                )
            }
        }
        
        // Create a document with a macro with no parameters
        val macro = MacroInvocation(
            macroName = "test",
            parameters = emptyMap(),
            isBlock = false,
            attributes = emptyMap(),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val paragraph = Paragraph(
            content = listOf(macro),
            attributes = emptyMap(),
            sourceLocation = SourceLocation(1, 0)
        )
        
        val document = Document(
            title = "Test",
            children = listOf(paragraph),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = SourceLocation(0, 0)
        )
        
        // Expand macros
        val expander = DefaultMacroExpander()
        val config = MacroConfig(customMacros = mapOf("test" to processor))
        val result = expander.expand(document, config)
        
        // Verify expansion
        assertTrue(result.errors.isEmpty())
        val processedParagraph = result.document.children.first() as Paragraph
        val expandedText = processedParagraph.content.first() as Text
        assertEquals("no params", expandedText.content)
    }
}
