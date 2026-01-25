package org.markup.poet.tck.publisher

import org.markup.poet.asciidoc.ast.Document
import org.markup.poet.asciidoc.ast.Section
import org.markup.poet.asciidoc.ast.Paragraph
import org.markup.poet.asciidoc.ast.Text
import org.markup.poet.asciidoc.ast.SourceLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class WorkflowValidatorTest {
    
    private val validator = DefaultWorkflowValidator()
    
    // ========== AsciiDoc Validation Tests ==========
    
    @Test
    fun `validateAsciidoc should accept valid TCK results document`() {
        val validAsciidoc = """
            = AsciiDoc Konvert - TCK Certification Results
            
            == Summary
            
            |===
            | Metric | Value
            | Total Tests | 10
            | Passed | 8
            |===
            
            == Test Results by Category
            
            === Inline Tests
            
            * inline/test1 ✅ PASSED
            * inline/test2 ❌ FAILED
            
            == Metadata
            
            * Generated: 2026-01-24
            * Version: 1.0.0
        """.trimIndent()
        
        val result = validator.validateAsciidoc(validAsciidoc)
        
        assertTrue(result.isValid(), "Valid AsciiDoc should pass validation")
        assertEquals(0, result.getErrors().size)
    }
    
    @Test
    fun `validateAsciidoc should reject empty document`() {
        val result = validator.validateAsciidoc("")
        
        assertTrue(result.isInvalid())
        assertTrue(result.getErrors().any { it.contains("empty") })
    }
    
    @Test
    fun `validateAsciidoc should reject document without title`() {
        val asciidocWithoutTitle = """
            == Summary
            
            Some content here.
        """.trimIndent()
        
        val result = validator.validateAsciidoc(asciidocWithoutTitle)
        
        assertTrue(result.isInvalid())
        assertTrue(result.getErrors().any { it.contains("title") })
    }
    
    @Test
    fun `validateAsciidoc should reject document without Summary section`() {
        val asciidocWithoutSummary = """
            = Title
            
            == Test Results by Category
            
            Some content.
            
            == Metadata
            
            Some metadata.
        """.trimIndent()
        
        val result = validator.validateAsciidoc(asciidocWithoutSummary)
        
        assertTrue(result.isInvalid())
        assertTrue(result.getErrors().any { it.contains("Summary") })
    }
    
    @Test
    fun `validateAsciidoc should reject document without Test Results section`() {
        val asciidocWithoutResults = """
            = Title
            
            == Summary
            
            Some summary.
            
            == Metadata
            
            Some metadata.
        """.trimIndent()
        
        val result = validator.validateAsciidoc(asciidocWithoutResults)
        
        assertTrue(result.isInvalid())
        assertTrue(result.getErrors().any { it.contains("Test Results") })
    }
    
    @Test
    fun `validateAsciidoc should reject document without Metadata section`() {
        val asciidocWithoutMetadata = """
            = Title
            
            == Summary
            
            Some summary.
            
            == Test Results by Category
            
            Some results.
        """.trimIndent()
        
        val result = validator.validateAsciidoc(asciidocWithoutMetadata)
        
        assertTrue(result.isInvalid())
        assertTrue(result.getErrors().any { it.contains("Metadata") })
    }
    
    @Test
    fun `validateAsciidoc should reject document without tables`() {
        val asciidocWithoutTables = """
            = Title
            
            == Summary
            
            Some summary without tables.
            
            == Test Results by Category
            
            Some results.
            
            == Metadata
            
            Some metadata.
        """.trimIndent()
        
        val result = validator.validateAsciidoc(asciidocWithoutTables)
        
        assertTrue(result.isInvalid())
        assertTrue(result.getErrors().any { it.contains("tables") })
    }
    
    @Test
    fun `validateAsciidoc should reject document without status indicators`() {
        val asciidocWithoutStatus = """
            = Title
            
            == Summary
            
            |===
            | Metric | Value
            |===
            
            == Test Results by Category
            
            Some results without status.
            
            == Metadata
            
            Some metadata.
        """.trimIndent()
        
        val result = validator.validateAsciidoc(asciidocWithoutStatus)
        
        assertTrue(result.isInvalid())
        assertTrue(result.getErrors().any { it.contains("status indicators") })
    }
    
    @Test
    fun `validateAsciidoc should collect multiple errors`() {
        val invalidAsciidoc = """
            Some content without proper structure.
        """.trimIndent()
        
        val result = validator.validateAsciidoc(invalidAsciidoc)
        
        assertTrue(result.isInvalid())
        assertTrue(result.getErrors().size > 1, "Should collect multiple validation errors")
    }
    
    // ========== AST Validation Tests ==========
    
    @Test
    fun `validateAst should accept valid document AST`() {
        val document = Document(
            title = "AsciiDoc Konvert - TCK Results",
            children = listOf(
                Section(
                    level = 2,
                    title = "Summary",
                    children = listOf(
                        Paragraph(
                            content = listOf(
                                Text(
                                    content = "Some summary content here that makes it substantial",
                                    attributes = emptyMap(),
                                    sourceLocation = SourceLocation(1, 1, 1, 1)
                                )
                            ),
                            attributes = emptyMap(),
                            sourceLocation = SourceLocation(1, 1, 1, 1)
                        )
                    ),
                    attributes = emptyMap(),
                    sourceLocation = SourceLocation(1, 1, 1, 1)
                ),
                Section(
                    level = 2,
                    title = "Test Results",
                    children = listOf(
                        Paragraph(
                            content = listOf(
                                Text(
                                    content = "Some test results content here that makes it substantial",
                                    attributes = emptyMap(),
                                    sourceLocation = SourceLocation(1, 1, 1, 1)
                                )
                            ),
                            attributes = emptyMap(),
                            sourceLocation = SourceLocation(1, 1, 1, 1)
                        )
                    ),
                    attributes = emptyMap(),
                    sourceLocation = SourceLocation(1, 1, 1, 1)
                )
            ),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = SourceLocation(1, 1, 1, 1)
        )
        
        val result = validator.validateAst(document)
        
        assertTrue(result.isValid(), "Valid AST should pass validation")
        assertEquals(0, result.getErrors().size)
    }
    
    @Test
    fun `validateAst should reject document without title`() {
        val document = Document(
            title = null,
            children = listOf(
                Section(
                    level = 2,
                    title = "Summary",
                    children = emptyList(),
                    attributes = emptyMap(),
                    sourceLocation = SourceLocation(1, 1, 1, 1)
                )
            ),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = SourceLocation(1, 1, 1, 1)
        )
        
        val result = validator.validateAst(document)
        
        assertTrue(result.isInvalid())
        assertTrue(result.getErrors().any { it.contains("title") })
    }
    
    @Test
    fun `validateAst should reject document with empty title`() {
        val document = Document(
            title = "",
            children = listOf(
                Section(
                    level = 2,
                    title = "Summary",
                    children = emptyList(),
                    attributes = emptyMap(),
                    sourceLocation = SourceLocation(1, 1, 1, 1)
                )
            ),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = SourceLocation(1, 1, 1, 1)
        )
        
        val result = validator.validateAst(document)
        
        assertTrue(result.isInvalid())
        assertTrue(result.getErrors().any { it.contains("title") })
    }
    
    @Test
    fun `validateAst should reject document with no blocks`() {
        val document = Document(
            title = "Title",
            children = emptyList(),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = SourceLocation(1, 1, 1, 1)
        )
        
        val result = validator.validateAst(document)
        
        assertTrue(result.isInvalid())
        assertTrue(result.getErrors().any { it.contains("no content blocks") })
    }
    
    @Test
    fun `validateAst should reject document with insufficient sections`() {
        val document = Document(
            title = "Title",
            children = listOf(
                Section(
                    level = 2,
                    title = "Only One Section",
                    children = emptyList(),
                    attributes = emptyMap(),
                    sourceLocation = SourceLocation(1, 1, 1, 1)
                )
            ),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = SourceLocation(1, 1, 1, 1)
        )
        
        val result = validator.validateAst(document)
        
        assertTrue(result.isInvalid())
        assertTrue(result.getErrors().any { it.contains("insufficient sections") })
    }
    
    @Test
    fun `validateAst should reject document that appears too shallow`() {
        val document = Document(
            title = "Title",
            children = listOf(
                Section(
                    level = 2,
                    title = "S1",
                    children = emptyList(),
                    attributes = emptyMap(),
                    sourceLocation = SourceLocation(1, 1, 1, 1)
                ),
                Section(
                    level = 2,
                    title = "S2",
                    children = emptyList(),
                    attributes = emptyMap(),
                    sourceLocation = SourceLocation(1, 1, 1, 1)
                )
            ),
            documentAttributes = emptyMap(),
            attributes = emptyMap(),
            sourceLocation = SourceLocation(1, 1, 1, 1)
        )
        
        val result = validator.validateAst(document)
        
        if (result.isValid()) {
            println("Expected invalid but got valid. Document: $document")
        }
        assertTrue(result.isInvalid(), "Document should be invalid. Errors: ${result.getErrors()}")
        assertTrue(result.getErrors().any { it.contains("shallow") || it.contains("no content") }, 
            "Should have 'shallow' or 'no content' error. Errors: ${result.getErrors()}")
    }
    
    // ========== HTML Validation Tests ==========
    
    @Test
    fun `validateHtml should accept valid HTML5 document`() {
        val validHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>TCK Results</title>
                <style>
                    body { font-family: sans-serif; }
                </style>
            </head>
            <body>
                <h1>AsciiDoc Konvert - TCK Results</h1>
                <h2>Summary</h2>
                <table>
                    <tr><td>Total Tests</td><td>10</td></tr>
                </table>
                <h2>Test Results</h2>
                <p>Some test results here.</p>
            </body>
            </html>
        """.trimIndent()
        
        val result = validator.validateHtml(validHtml)
        
        if (result.isInvalid()) {
            println("Validation errors: ${result.getErrors()}")
            println("HTML length: ${validHtml.length}")
        }
        assertTrue(result.isValid(), "Valid HTML should pass validation. Errors: ${result.getErrors()}")
        assertEquals(0, result.getErrors().size)
    }
    
    @Test
    fun `validateHtml should reject empty document`() {
        val result = validator.validateHtml("")
        
        assertTrue(result.isInvalid())
        assertTrue(result.getErrors().any { it.contains("empty") })
    }
    
    @Test
    fun `validateHtml should reject document without DOCTYPE`() {
        val htmlWithoutDoctype = """
            <html>
            <head><title>Title</title></head>
            <body><h1>Content</h1></body>
            </html>
        """.trimIndent()
        
        val result = validator.validateHtml(htmlWithoutDoctype)
        
        assertTrue(result.isInvalid())
        assertTrue(result.getErrors().any { it.contains("DOCTYPE") })
    }
    
    @Test
    fun `validateHtml should reject document without html tag`() {
        val htmlWithoutHtmlTag = """
            <!DOCTYPE html>
            <head><title>Title</title></head>
            <body><h1>Content</h1></body>
        """.trimIndent()
        
        val result = validator.validateHtml(htmlWithoutHtmlTag)
        
        assertTrue(result.isInvalid())
        assertTrue(result.getErrors().any { it.contains("<html>") })
    }
    
    @Test
    fun `validateHtml should reject document without head section`() {
        val htmlWithoutHead = """
            <!DOCTYPE html>
            <html>
            <body><h1>Content</h1></body>
            </html>
        """.trimIndent()
        
        val result = validator.validateHtml(htmlWithoutHead)
        
        assertTrue(result.isInvalid())
        assertTrue(result.getErrors().any { it.contains("<head>") })
    }
    
    @Test
    fun `validateHtml should reject document without body section`() {
        val htmlWithoutBody = """
            <!DOCTYPE html>
            <html>
            <head><title>Title</title></head>
            </html>
        """.trimIndent()
        
        val result = validator.validateHtml(htmlWithoutBody)
        
        assertTrue(result.isInvalid())
        assertTrue(result.getErrors().any { it.contains("<body>") })
    }
    
    @Test
    fun `validateHtml should reject document without title`() {
        val htmlWithoutTitle = """
            <!DOCTYPE html>
            <html>
            <head></head>
            <body><h1>Content</h1></body>
            </html>
        """.trimIndent()
        
        val result = validator.validateHtml(htmlWithoutTitle)
        
        assertTrue(result.isInvalid())
        assertTrue(result.getErrors().any { it.contains("<title>") })
    }
    
    @Test
    fun `validateHtml should reject document without CSS`() {
        val htmlWithoutCss = """
            <!DOCTYPE html>
            <html>
            <head><title>Title</title></head>
            <body><h1>Content</h1></body>
            </html>
        """.trimIndent()
        
        val result = validator.validateHtml(htmlWithoutCss)
        
        assertTrue(result.isInvalid())
        assertTrue(result.getErrors().any { it.contains("CSS") })
    }
    
    @Test
    fun `validateHtml should accept document with external CSS`() {
        val htmlWithExternalCss = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Title</title>
                <link rel="stylesheet" href="styles.css">
            </head>
            <body>
                <h1>Content</h1>
                <table><tr><td>Data</td></tr></table>
            </body>
            </html>
        """.trimIndent()
        
        val result = validator.validateHtml(htmlWithExternalCss)
        
        if (result.isInvalid()) {
            println("Validation errors: ${result.getErrors()}")
            println("HTML length: ${htmlWithExternalCss.length}")
        }
        assertTrue(result.isValid(), "HTML with external CSS should pass validation. Errors: ${result.getErrors()}")
    }
    
    @Test
    fun `validateHtml should reject document without headings`() {
        val htmlWithoutHeadings = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Title</title>
                <style>body { }</style>
            </head>
            <body>
                <p>Content without headings</p>
                <table><tr><td>Data</td></tr></table>
            </body>
            </html>
        """.trimIndent()
        
        val result = validator.validateHtml(htmlWithoutHeadings)
        
        assertTrue(result.isInvalid())
        assertTrue(result.getErrors().any { it.contains("heading") })
    }
    
    @Test
    fun `validateHtml should reject document without tables`() {
        val htmlWithoutTables = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Title</title>
                <style>body { }</style>
            </head>
            <body>
                <h1>Content</h1>
                <p>No tables here</p>
            </body>
            </html>
        """.trimIndent()
        
        val result = validator.validateHtml(htmlWithoutTables)
        
        assertTrue(result.isInvalid())
        assertTrue(result.getErrors().any { it.contains("table") })
    }
    
    @Test
    fun `validateHtml should collect multiple errors`() {
        val invalidHtml = """
            <p>Just a paragraph, nothing else</p>
        """.trimIndent()
        
        val result = validator.validateHtml(invalidHtml)
        
        assertTrue(result.isInvalid())
        assertTrue(result.getErrors().size > 1, "Should collect multiple validation errors")
    }
    
    // ========== Publication Validation Tests ==========
    
    @Test
    fun `validatePublication should accept valid HTTPS URL`() {
        val result = validator.validatePublication("https://example.github.io/repo/results.html")
        
        assertTrue(result.isValid())
    }
    
    @Test
    fun `validatePublication should accept valid HTTP URL`() {
        val result = validator.validatePublication("http://example.com/results.html")
        
        assertTrue(result.isValid())
    }
    
    @Test
    fun `validatePublication should reject empty URL`() {
        val result = validator.validatePublication("")
        
        assertTrue(result.isInvalid())
        assertTrue(result.getErrors().any { it.contains("empty") })
    }
    
    @Test
    fun `validatePublication should reject URL without protocol`() {
        val result = validator.validatePublication("example.com/results.html")
        
        assertTrue(result.isInvalid())
        assertTrue(result.getErrors().any { it.contains("http") })
    }
    
    // ========== ValidationResult Tests ==========
    
    @Test
    fun `ValidationResult Valid should report as valid`() {
        val result = ValidationResult.Valid
        
        assertTrue(result.isValid())
        assertFalse(result.isInvalid())
        assertEquals(0, result.getErrors().size)
    }
    
    @Test
    fun `ValidationResult Invalid should report as invalid`() {
        val result = ValidationResult.Invalid(listOf("Error 1", "Error 2"))
        
        assertFalse(result.isValid())
        assertTrue(result.isInvalid())
        assertEquals(2, result.getErrors().size)
        assertEquals("Error 1", result.getErrors()[0])
        assertEquals("Error 2", result.getErrors()[1])
    }
}
