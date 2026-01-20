package org.markup.poet.asciidoc.render

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HtmlBuilderTest {
    
    private val escaper = DefaultHtmlEscaper()
    private val builder = DefaultHtmlBuilder(escaper)
    
    @Test
    fun `should generate simple tag without attributes`() {
        // Arrange & Act
        val html = builder.build {
            openTag("p")
            text("Hello")
            closeTag("p")
        }
        
        // Assert
        assertEquals("<p>Hello</p>", html)
    }
    
    @Test
    fun `should generate tag with single attribute`() {
        // Arrange & Act
        val html = builder.build {
            openTag("p", mapOf("class" to "paragraph"))
            text("Hello")
            closeTag("p")
        }
        
        // Assert
        assertEquals("<p class=\"paragraph\">Hello</p>", html)
    }
    
    @Test
    fun `should generate tag with multiple attributes`() {
        // Arrange & Act
        val html = builder.build {
            openTag("a", mapOf("href" to "https://example.com", "title" to "Example"))
            text("Link")
            closeTag("a")
        }
        
        // Assert
        assertTrue(html.contains("<a"))
        assertTrue(html.contains("href=\"https://example.com\""))
        assertTrue(html.contains("title=\"Example\""))
        assertTrue(html.contains(">Link</a>"))
    }
    
    @Test
    fun `should omit attributes with empty values`() {
        // Arrange & Act
        val html = builder.build {
            openTag("p", mapOf("class" to "paragraph", "id" to ""))
            text("Hello")
            closeTag("p")
        }
        
        // Assert
        assertEquals("<p class=\"paragraph\">Hello</p>", html)
        assertTrue(!html.contains("id="))
    }
    
    @Test
    fun `should escape attribute values`() {
        // Arrange & Act
        val html = builder.build {
            openTag("a", mapOf("title" to "A & B < C > D \"quoted\" 'apostrophe'"))
            text("Link")
            closeTag("a")
        }
        
        // Assert
        assertTrue(html.contains("title=\"A &amp; B &lt; C &gt; D &quot;quoted&quot; &#39;apostrophe&#39;\""))
    }
    
    @Test
    fun `should handle nested tags with proper nesting`() {
        // Arrange & Act
        val html = builder.build {
            openTag("div")
            openTag("p")
            text("Paragraph")
            closeTag("p")
            closeTag("div")
        }
        
        // Assert
        assertEquals("<div><p>Paragraph</p></div>", html)
    }
    
    @Test
    fun `should handle multiple sibling tags`() {
        // Arrange & Act
        val html = builder.build {
            openTag("p")
            text("First")
            closeTag("p")
            openTag("p")
            text("Second")
            closeTag("p")
        }
        
        // Assert
        assertEquals("<p>First</p><p>Second</p>", html)
    }
    
    @Test
    fun `should not automatically escape text content`() {
        // Arrange & Act
        val html = builder.build {
            openTag("p")
            text("<b>Bold</b>")
            closeTag("p")
        }
        
        // Assert
        assertEquals("<p><b>Bold</b></p>", html)
    }
    
    @Test
    fun `should provide escape method for text content`() {
        // Arrange & Act
        val html = builder.build {
            openTag("p")
            text(escape("<b>Bold</b>"))
            closeTag("p")
        }
        
        // Assert
        assertEquals("<p>&lt;b&gt;Bold&lt;/b&gt;</p>", html)
    }
    
    @Test
    fun `should escape special HTML characters in text`() {
        // Arrange
        val text = "A & B < C > D"
        
        // Act
        val escaped = builder.escape(text)
        
        // Assert
        assertEquals("A &amp; B &lt; C &gt; D", escaped)
    }
    
    @Test
    fun `should escape all special characters in attributes`() {
        // Arrange
        val text = "A & B < C > D \"quoted\" 'apostrophe'"
        
        // Act
        val escaped = builder.escapeAttribute(text)
        
        // Assert
        assertEquals("A &amp; B &lt; C &gt; D &quot;quoted&quot; &#39;apostrophe&#39;", escaped)
    }
    
    @Test
    fun `should handle empty text content`() {
        // Arrange & Act
        val html = builder.build {
            openTag("p")
            text("")
            closeTag("p")
        }
        
        // Assert
        assertEquals("<p></p>", html)
    }
    
    @Test
    fun `should handle self-closing style tags`() {
        // Arrange & Act
        val html = builder.build {
            openTag("img", mapOf("src" to "image.png", "alt" to "Image"))
        }
        
        // Assert
        assertTrue(html.contains("<img"))
        assertTrue(html.contains("src=\"image.png\""))
        assertTrue(html.contains("alt=\"Image\""))
        assertTrue(html.endsWith(">"))
    }
    
    @Test
    fun `should clear buffer between builds`() {
        // Arrange & Act
        val html1 = builder.build {
            openTag("p")
            text("First")
            closeTag("p")
        }
        
        val html2 = builder.build {
            openTag("div")
            text("Second")
            closeTag("div")
        }
        
        // Assert
        assertEquals("<p>First</p>", html1)
        assertEquals("<div>Second</div>", html2)
    }
    
    @Test
    fun `should handle complex nested structure`() {
        // Arrange & Act
        val html = builder.build {
            openTag("div", mapOf("class" to "container"))
            openTag("h1", mapOf("id" to "title"))
            text("Title")
            closeTag("h1")
            openTag("p", mapOf("class" to "paragraph"))
            text("This is ")
            openTag("strong")
            text("bold")
            closeTag("strong")
            text(" text.")
            closeTag("p")
            closeTag("div")
        }
        
        // Assert
        assertEquals(
            "<div class=\"container\"><h1 id=\"title\">Title</h1>" +
            "<p class=\"paragraph\">This is <strong>bold</strong> text.</p></div>",
            html
        )
    }
    
    @Test
    fun `should handle text with newlines and whitespace`() {
        // Arrange & Act
        val html = builder.build {
            openTag("pre")
            text("Line 1\nLine 2\n  Indented")
            closeTag("pre")
        }
        
        // Assert
        assertEquals("<pre>Line 1\nLine 2\n  Indented</pre>", html)
    }
    
    @Test
    fun `should handle multiple text calls`() {
        // Arrange & Act
        val html = builder.build {
            openTag("p")
            text("Hello ")
            text("world")
            text("!")
            closeTag("p")
        }
        
        // Assert
        assertEquals("<p>Hello world!</p>", html)
    }
    
    @Test
    fun `should handle tags with no content`() {
        // Arrange & Act
        val html = builder.build {
            openTag("div")
            closeTag("div")
        }
        
        // Assert
        assertEquals("<div></div>", html)
    }
}
