package org.markup.poet.asciidoc.render

import org.markup.poet.asciidoc.ast.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for InlineRenderer implementation.
 * 
 * Tests cover:
 * - Text rendering with HTML escaping
 * - Strong (bold) rendering
 * - Emphasis (italic) rendering
 * - Inline code rendering
 * - Link rendering with URL sanitization
 * - Image rendering with alt text
 * - Nested inline elements
 * - Attribute references
 * - Callouts
 * - Cross-references
 * - Macro invocations
 */
class InlineRendererTest {
    
    private val escaper = DefaultHtmlEscaper()
    private val builder = DefaultHtmlBuilder(escaper)
    private val renderer = DefaultInlineRenderer(builder)
    private val config = RenderConfig()
    private val context = RenderContext(config)
    
    // Helper to create a dummy SourceLocation
    private val dummyLocation = SourceLocation(1, 1)
    
    // ========== Text Rendering Tests ==========
    
    @Test
    fun `renders plain text`() {
        val text = Text("Hello, world!", sourceLocation = dummyLocation)
        val html = renderer.render(text, context)
        
        assertEquals("Hello, world!", html)
    }
    
    @Test
    fun `escapes HTML special characters in text`() {
        val text = Text("<script>alert('xss')</script>", sourceLocation = dummyLocation)
        val html = renderer.render(text, context)
        
        assertEquals("&lt;script&gt;alert('xss')&lt;/script&gt;", html)
    }
    
    @Test
    fun `escapes ampersands in text`() {
        val text = Text("Tom & Jerry", sourceLocation = dummyLocation)
        val html = renderer.render(text, context)
        
        assertEquals("Tom &amp; Jerry", html)
    }
    
    @Test
    fun `handles empty text`() {
        val text = Text("", sourceLocation = dummyLocation)
        val html = renderer.render(text, context)
        
        assertEquals("", html)
    }
    
    // ========== Strong (Bold) Rendering Tests ==========
    
    @Test
    fun `renders strong text with strong tag`() {
        val strong = Strong(
            content = listOf(Text("bold text", sourceLocation = dummyLocation)),
            sourceLocation = dummyLocation
        )
        val html = renderer.render(strong, context)
        
        assertEquals("<strong>bold text</strong>", html)
    }
    
    @Test
    fun `escapes HTML in strong text`() {
        val strong = Strong(
            content = listOf(Text("<b>test</b>", sourceLocation = dummyLocation)),
            sourceLocation = dummyLocation
        )
        val html = renderer.render(strong, context)
        
        assertEquals("<strong>&lt;b&gt;test&lt;/b&gt;</strong>", html)
    }
    
    @Test
    fun `renders nested inline elements in strong`() {
        val strong = Strong(
            content = listOf(
                Text("bold ", sourceLocation = dummyLocation),
                Emphasis(
                    content = listOf(Text("and italic", sourceLocation = dummyLocation)),
                    sourceLocation = dummyLocation
                )
            ),
            sourceLocation = dummyLocation
        )
        val html = renderer.render(strong, context)
        
        assertEquals("<strong>bold <em>and italic</em></strong>", html)
    }
    
    // ========== Emphasis (Italic) Rendering Tests ==========
    
    @Test
    fun `renders emphasis text with em tag`() {
        val emphasis = Emphasis(
            content = listOf(Text("italic text", sourceLocation = dummyLocation)),
            sourceLocation = dummyLocation
        )
        val html = renderer.render(emphasis, context)
        
        assertEquals("<em>italic text</em>", html)
    }
    
    @Test
    fun `renders nested strong in emphasis`() {
        val emphasis = Emphasis(
            content = listOf(
                Text("italic ", sourceLocation = dummyLocation),
                Strong(
                    content = listOf(Text("and bold", sourceLocation = dummyLocation)),
                    sourceLocation = dummyLocation
                )
            ),
            sourceLocation = dummyLocation
        )
        val html = renderer.render(emphasis, context)
        
        assertEquals("<em>italic <strong>and bold</strong></em>", html)
    }
    
    // ========== Code Rendering Tests ==========
    
    @Test
    fun `renders inline code with code tag`() {
        val code = Code("println(\"Hello\")", sourceLocation = dummyLocation)
        val html = renderer.render(code, context)
        
        assertEquals("<code>println(\"Hello\")</code>", html)
    }
    
    @Test
    fun `escapes HTML in inline code`() {
        val code = Code("<div>test</div>", sourceLocation = dummyLocation)
        val html = renderer.render(code, context)
        
        assertEquals("<code>&lt;div&gt;test&lt;/div&gt;</code>", html)
    }
    
    @Test
    fun `handles empty code`() {
        val code = Code("", sourceLocation = dummyLocation)
        val html = renderer.render(code, context)
        
        assertEquals("<code></code>", html)
    }
    
    // ========== Link Rendering Tests ==========
    
    @Test
    fun `renders link with href and text`() {
        val link = Link(
            url = "https://example.com",
            text = "Example",
            sourceLocation = dummyLocation
        )
        val html = renderer.render(link, context)
        
        assertEquals("<a href=\"https://example.com\">Example</a>", html)
    }
    
    @Test
    fun `escapes link text`() {
        val link = Link(
            url = "https://example.com",
            text = "<script>alert('xss')</script>",
            sourceLocation = dummyLocation
        )
        val html = renderer.render(link, context)
        
        assertTrue(html.contains("&lt;script&gt;"))
        assertTrue(html.contains("&lt;/script&gt;"))
    }
    
    @Test
    fun `sanitizes javascript URL`() {
        val link = Link(
            url = "javascript:alert('xss')",
            text = "Click me",
            sourceLocation = dummyLocation
        )
        val html = renderer.render(link, context)
        
        assertEquals("<a href=\"#\">Click me</a>", html)
    }
    
    @Test
    fun `sanitizes data URL`() {
        val link = Link(
            url = "data:text/html,<script>alert('xss')</script>",
            text = "Click me",
            sourceLocation = dummyLocation
        )
        val html = renderer.render(link, context)
        
        assertEquals("<a href=\"#\">Click me</a>", html)
    }
    
    @Test
    fun `sanitizes javascript URL case insensitive`() {
        val link = Link(
            url = "JaVaScRiPt:alert('xss')",
            text = "Click me",
            sourceLocation = dummyLocation
        )
        val html = renderer.render(link, context)
        
        assertEquals("<a href=\"#\">Click me</a>", html)
    }
    
    @Test
    fun `allows safe URLs`() {
        val testCases = listOf(
            "https://example.com",
            "http://example.com",
            "/path/to/page",
            "../relative/path",
            "#anchor",
            "mailto:test@example.com",
            "ftp://files.example.com"
        )
        
        testCases.forEach { url ->
            val link = Link(url = url, text = "Link", sourceLocation = dummyLocation)
            val html = renderer.render(link, context)
            assertTrue(html.contains("href=\"$url\""), "URL $url should be allowed")
        }
    }
    
    @Test
    fun `renders link with title attribute`() {
        val link = Link(
            url = "https://example.com",
            text = "Example",
            attributes = mapOf("title" to "Example Website"),
            sourceLocation = dummyLocation
        )
        val html = renderer.render(link, context)
        
        assertTrue(html.contains("href=\"https://example.com\""))
        assertTrue(html.contains("title=\"Example Website\""))
        assertTrue(html.contains(">Example</a>"))
    }
    
    // ========== Image Rendering Tests ==========
    
    @Test
    fun `renders image with src and alt`() {
        val image = Image(
            path = "/images/photo.jpg",
            altText = "A photo",
            sourceLocation = dummyLocation
        )
        val html = renderer.render(image, context)
        
        assertEquals("<img src=\"/images/photo.jpg\" alt=\"A photo\">", html)
    }
    
    @Test
    fun `renders image with empty alt text`() {
        val image = Image(
            path = "/images/photo.jpg",
            altText = "",
            sourceLocation = dummyLocation
        )
        val html = renderer.render(image, context)
        
        assertTrue(html.contains("alt=\"\""), "Image should have alt attribute even if empty")
    }
    
    @Test
    fun `renders image with title attribute`() {
        val image = Image(
            path = "/images/photo.jpg",
            altText = "A photo",
            attributes = mapOf("title" to "Photo Title"),
            sourceLocation = dummyLocation
        )
        val html = renderer.render(image, context)
        
        assertTrue(html.contains("src=\"/images/photo.jpg\""))
        assertTrue(html.contains("alt=\"A photo\""))
        assertTrue(html.contains("title=\"Photo Title\""))
    }
    
    // ========== Attribute Reference Tests ==========
    
    @Test
    fun `renders unresolved attribute reference with warning`() {
        val ref = AttributeReference(
            key = "author",
            sourceLocation = dummyLocation
        )
        val html = renderer.render(ref, context)
        
        assertEquals("{author}", html)
        assertTrue(context.getWarnings().any { it.contains("Unresolved attribute reference") })
    }
    
    // ========== Callout Tests ==========
    
    @Test
    fun `renders callout with number`() {
        val callout = Callout(
            number = 1,
            sourceLocation = dummyLocation
        )
        val html = renderer.render(callout, context)
        
        assertEquals("<span class=\"callout\">&lt;1&gt;</span>", html)
    }
    
    @Test
    fun `renders callout with multiple digit number`() {
        val callout = Callout(
            number = 42,
            sourceLocation = dummyLocation
        )
        val html = renderer.render(callout, context)
        
        assertEquals("<span class=\"callout\">&lt;42&gt;</span>", html)
    }
    
    // ========== Cross-Reference Tests ==========
    
    @Test
    fun `renders cross-reference with target ID`() {
        val ref = CrossReference(
            targetId = "section-1",
            sourceLocation = dummyLocation
        )
        val html = renderer.render(ref, context)
        
        assertEquals("<a href=\"#section-1\">section-1</a>", html)
    }
    
    @Test
    fun `renders cross-reference with custom text`() {
        val ref = CrossReference(
            targetId = "section-1",
            customText = "See Section 1",
            sourceLocation = dummyLocation
        )
        val html = renderer.render(ref, context)
        
        assertEquals("<a href=\"#section-1\">See Section 1</a>", html)
    }
    
    @Test
    fun `escapes custom text in cross-reference`() {
        val ref = CrossReference(
            targetId = "section-1",
            customText = "<script>alert('xss')</script>",
            sourceLocation = dummyLocation
        )
        val html = renderer.render(ref, context)
        
        assertTrue(html.contains("&lt;script&gt;"))
        assertTrue(html.contains("&lt;/script&gt;"))
    }
    
    // ========== Macro Invocation Tests ==========
    
    @Test
    fun `renders unexpanded macro with warning`() {
        val macro = MacroInvocation(
            macroName = "include",
            parameters = mapOf("file" to "test.adoc"),
            isBlock = false,
            sourceLocation = dummyLocation
        )
        val html = renderer.render(macro, context)
        
        assertEquals("<span class=\"macro-placeholder\">include[]</span>", html)
        assertTrue(context.getWarnings().any { it.contains("Unexpanded macro") })
    }
    
    // ========== Complex Nesting Tests ==========
    
    @Test
    fun `renders deeply nested inline elements`() {
        val nested = Strong(
            content = listOf(
                Text("bold ", sourceLocation = dummyLocation),
                Emphasis(
                    content = listOf(
                        Text("italic ", sourceLocation = dummyLocation),
                        Code("code", sourceLocation = dummyLocation)
                    ),
                    sourceLocation = dummyLocation
                )
            ),
            sourceLocation = dummyLocation
        )
        val html = renderer.render(nested, context)
        
        assertEquals("<strong>bold <em>italic <code>code</code></em></strong>", html)
    }
    
    @Test
    fun `renders multiple inline elements in sequence`() {
        val elements = listOf(
            Text("Plain ", sourceLocation = dummyLocation),
            Strong(
                content = listOf(Text("bold", sourceLocation = dummyLocation)),
                sourceLocation = dummyLocation
            ),
            Text(" and ", sourceLocation = dummyLocation),
            Emphasis(
                content = listOf(Text("italic", sourceLocation = dummyLocation)),
                sourceLocation = dummyLocation
            ),
            Text(" text.", sourceLocation = dummyLocation)
        )
        
        val html = elements.joinToString("") { renderer.render(it, context) }
        
        assertEquals("Plain <strong>bold</strong> and <em>italic</em> text.", html)
    }
    
    @Test
    fun `handles empty nested content`() {
        val strong = Strong(
            content = emptyList(),
            sourceLocation = dummyLocation
        )
        val html = renderer.render(strong, context)
        
        assertEquals("<strong></strong>", html)
    }
}
