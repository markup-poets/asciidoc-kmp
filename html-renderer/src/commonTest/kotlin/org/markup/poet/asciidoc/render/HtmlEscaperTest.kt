package org.markup.poet.asciidoc.render

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class HtmlEscaperTest {
    
    private val escaper = DefaultHtmlEscaper()
    
    // Tests for escapeHtml()
    
    @Test
    fun `escapeHtml should convert ampersand to entity`() {
        val result = escaper.escapeHtml("Tom & Jerry")
        assertEquals("Tom &amp; Jerry", result)
    }
    
    @Test
    fun `escapeHtml should convert less than to entity`() {
        val result = escaper.escapeHtml("5 < 10")
        assertEquals("5 &lt; 10", result)
    }
    
    @Test
    fun `escapeHtml should convert greater than to entity`() {
        val result = escaper.escapeHtml("10 > 5")
        assertEquals("10 &gt; 5", result)
    }
    
    @Test
    fun `escapeHtml should convert all special characters`() {
        val result = escaper.escapeHtml("<div>Hello & goodbye</div>")
        assertEquals("&lt;div&gt;Hello &amp; goodbye&lt;/div&gt;", result)
    }
    
    @Test
    fun `escapeHtml should handle empty string`() {
        val result = escaper.escapeHtml("")
        assertEquals("", result)
    }
    
    @Test
    fun `escapeHtml should handle string with no special characters`() {
        val result = escaper.escapeHtml("Hello World")
        assertEquals("Hello World", result)
    }
    
    @Test
    fun `escapeHtml should not escape quotes`() {
        val result = escaper.escapeHtml("He said \"hello\"")
        assertEquals("He said \"hello\"", result)
    }
    
    @Test
    fun `escapeHtml should handle multiple ampersands`() {
        val result = escaper.escapeHtml("A & B & C")
        assertEquals("A &amp; B &amp; C", result)
    }
    
    @Test
    fun `escapeHtml should handle script tag`() {
        val result = escaper.escapeHtml("<script>alert('xss')</script>")
        assertEquals("&lt;script&gt;alert('xss')&lt;/script&gt;", result)
    }
    
    // Tests for escapeAttribute()
    
    @Test
    fun `escapeAttribute should convert ampersand to entity`() {
        val result = escaper.escapeAttribute("Tom & Jerry")
        assertEquals("Tom &amp; Jerry", result)
    }
    
    @Test
    fun `escapeAttribute should convert less than to entity`() {
        val result = escaper.escapeAttribute("5 < 10")
        assertEquals("5 &lt; 10", result)
    }
    
    @Test
    fun `escapeAttribute should convert greater than to entity`() {
        val result = escaper.escapeAttribute("10 > 5")
        assertEquals("10 &gt; 5", result)
    }
    
    @Test
    fun `escapeAttribute should convert double quote to entity`() {
        val result = escaper.escapeAttribute("He said \"hello\"")
        assertEquals("He said &quot;hello&quot;", result)
    }
    
    @Test
    fun `escapeAttribute should convert single quote to entity`() {
        val result = escaper.escapeAttribute("It's working")
        assertEquals("It&#39;s working", result)
    }
    
    @Test
    fun `escapeAttribute should convert all special characters including quotes`() {
        val result = escaper.escapeAttribute("<div class=\"test\" data-value='123'>A & B</div>")
        assertEquals("&lt;div class=&quot;test&quot; data-value=&#39;123&#39;&gt;A &amp; B&lt;/div&gt;", result)
    }
    
    @Test
    fun `escapeAttribute should handle empty string`() {
        val result = escaper.escapeAttribute("")
        assertEquals("", result)
    }
    
    @Test
    fun `escapeAttribute should handle string with no special characters`() {
        val result = escaper.escapeAttribute("Hello World")
        assertEquals("Hello World", result)
    }
    
    @Test
    fun `escapeAttribute should handle attribute with javascript protocol`() {
        val result = escaper.escapeAttribute("javascript:alert('xss')")
        // Note: This test verifies escaping, not URL sanitization (that's a different component)
        assertEquals("javascript:alert(&#39;xss&#39;)", result)
    }
    
    @Test
    fun `escapeAttribute should handle mixed quotes`() {
        val result = escaper.escapeAttribute("\"It's\" a test")
        assertEquals("&quot;It&#39;s&quot; a test", result)
    }
    
    // Edge case tests
    
    @Test
    fun `escapeHtml should handle string with only special characters`() {
        val result = escaper.escapeHtml("<>&")
        assertEquals("&lt;&gt;&amp;", result)
    }
    
    @Test
    fun `escapeAttribute should handle string with only special characters`() {
        val result = escaper.escapeAttribute("<>&\"'")
        assertEquals("&lt;&gt;&amp;&quot;&#39;", result)
    }
    
    @Test
    fun `escapeHtml should not contain unescaped angle brackets`() {
        val result = escaper.escapeHtml("<tag>content</tag>")
        assertFalse(result.contains("<"))
        assertFalse(result.contains(">"))
    }
    
    @Test
    fun `escapeAttribute should not contain unescaped quotes`() {
        val result = escaper.escapeAttribute("value=\"test\" and 'test'")
        assertFalse(result.contains("\""))
        assertFalse(result.contains("'"))
    }
}
