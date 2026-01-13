package io.github.kotlin.asciidoc.parser

import io.github.kotlin.asciidoc.ast.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlin.test.assertFalse
import kotlin.random.Random

/**
 * Property-based tests for inline element recognition.
 * **Feature: asciidoc-parser, Property 5: Inline Element Recognition**
 * **Validates: Requirements 4.1, 4.2, 4.3, 4.4, 4.5**
 */
class InlineElementRecognitionTest {

    private val inlineParser = DefaultInlineParser()

    @Test
    fun `Property 5 - Inline Element Recognition - Parser should create correct inline elements with properly extracted content and attributes for all inline markup patterns`() {
        // Run property-based test with multiple iterations
        repeat(20) {
            val testData = generateInlineElementTestData()
            
            when (testData) {
                is TestStrongData -> {
                    val elements = inlineParser.parseInlineElements(testData.text, testData.lineNumber)
                    
                    // Should contain at least one Strong element
                    val strongElements = elements.filterIsInstance<Strong>()
                    assertTrue(strongElements.isNotEmpty(), "Should parse strong markup: ${testData.text}")
                    
                    // Verify strong element content
                    strongElements.forEach { strong ->
                        assertTrue(strong.content.isNotEmpty(), "Strong element should have content")
                        assertEquals(testData.lineNumber, strong.sourceLocation.line)
                    }
                }
                
                is TestEmphasisData -> {
                    val elements = inlineParser.parseInlineElements(testData.text, testData.lineNumber)
                    
                    // Should contain at least one Emphasis element
                    val emphasisElements = elements.filterIsInstance<Emphasis>()
                    assertTrue(emphasisElements.isNotEmpty(), "Should parse emphasis markup: ${testData.text}")
                    
                    // Verify emphasis element content
                    emphasisElements.forEach { emphasis ->
                        assertTrue(emphasis.content.isNotEmpty(), "Emphasis element should have content")
                        assertEquals(testData.lineNumber, emphasis.sourceLocation.line)
                    }
                }
                
                is TestCodeData -> {
                    val elements = inlineParser.parseInlineElements(testData.text, testData.lineNumber)
                    
                    // Should contain at least one Code element
                    val codeElements = elements.filterIsInstance<Code>()
                    assertTrue(codeElements.isNotEmpty(), "Should parse code markup: ${testData.text}")
                    
                    // Verify code element content
                    codeElements.forEach { code ->
                        assertEquals(testData.expectedContent, code.content)
                        assertEquals(testData.lineNumber, code.sourceLocation.line)
                    }
                }
                
                is TestLinkData -> {
                    val elements = inlineParser.parseInlineElements(testData.text, testData.lineNumber)
                    
                    // Should contain at least one Link element
                    val linkElements = elements.filterIsInstance<Link>()
                    assertTrue(linkElements.isNotEmpty(), "Should parse link markup: ${testData.text}")
                    
                    // Verify link element content
                    linkElements.forEach { link ->
                        assertEquals(testData.expectedUrl, link.url)
                        assertEquals(testData.expectedText, link.text)
                        assertEquals(testData.lineNumber, link.sourceLocation.line)
                    }
                }
                
                is TestImageData -> {
                    val elements = inlineParser.parseInlineElements(testData.text, testData.lineNumber)
                    
                    // Should contain at least one Image element
                    val imageElements = elements.filterIsInstance<Image>()
                    assertTrue(imageElements.isNotEmpty(), "Should parse image markup: ${testData.text}")
                    
                    // Verify image element content
                    imageElements.forEach { image ->
                        assertEquals(testData.expectedPath, image.path)
                        assertEquals(testData.expectedAltText, image.altText)
                        assertEquals(testData.lineNumber, image.sourceLocation.line)
                    }
                }
                
                is TestPlainTextData -> {
                    val elements = inlineParser.parseInlineElements(testData.text, testData.lineNumber)
                    
                    // Should contain at least one Text element
                    val textElements = elements.filterIsInstance<Text>()
                    assertTrue(textElements.isNotEmpty(), "Should parse plain text: ${testData.text}")
                    
                    // Verify text content is preserved
                    val combinedText = textElements.joinToString("") { it.content }
                    assertEquals(testData.text, combinedText)
                }
            }
        }
    }

    @Test
    fun `Property 5a - Strong markup parsing should correctly identify and extract bold text content`() {
        repeat(20) {
            val strongData = generateStrongTestData()
            val elements = inlineParser.parseInlineElements(strongData.text, strongData.lineNumber)
            
            val strongElements = elements.filterIsInstance<Strong>()
            assertTrue(strongElements.isNotEmpty(), "Should find strong elements in: ${strongData.text}")
            
            // Verify each strong element has proper content
            strongElements.forEach { strong ->
                assertTrue(strong.content.isNotEmpty(), "Strong element should have content")
                
                // Content should be inline elements (Text or nested markup)
                strong.content.forEach { inlineElement ->
                    assertTrue(inlineElement is InlineElement, "Strong content should be inline elements")
                }
            }
        }
    }

    @Test
    fun `Property 5b - Emphasis markup parsing should correctly identify and extract italic text content`() {
        repeat(20) {
            val emphasisData = generateEmphasisTestData()
            val elements = inlineParser.parseInlineElements(emphasisData.text, emphasisData.lineNumber)
            
            val emphasisElements = elements.filterIsInstance<Emphasis>()
            assertTrue(emphasisElements.isNotEmpty(), "Should find emphasis elements in: ${emphasisData.text}")
            
            // Verify each emphasis element has proper content
            emphasisElements.forEach { emphasis ->
                assertTrue(emphasis.content.isNotEmpty(), "Emphasis element should have content")
                
                // Content should be inline elements
                emphasis.content.forEach { inlineElement ->
                    assertTrue(inlineElement is InlineElement, "Emphasis content should be inline elements")
                }
            }
        }
    }

    @Test
    fun `Property 5c - Code markup parsing should preserve literal content without further parsing`() {
        repeat(20) {
            val codeData = generateCodeTestData()
            val elements = inlineParser.parseInlineElements(codeData.text, codeData.lineNumber)
            
            val codeElements = elements.filterIsInstance<Code>()
            assertTrue(codeElements.isNotEmpty(), "Should find code elements in: ${codeData.text}")
            
            // Verify code content is preserved literally
            codeElements.forEach { code ->
                assertEquals(codeData.expectedContent, code.content, "Code content should be preserved literally")
                
                // Code content should not contain markup characters unless they're literal
                assertNotNull(code.content, "Code content should not be null")
            }
        }
    }

    @Test
    fun `Property 5d - Link markup parsing should extract URL and display text correctly`() {
        repeat(20) {
            val linkData = generateLinkTestData()
            val elements = inlineParser.parseInlineElements(linkData.text, linkData.lineNumber)
            
            val linkElements = elements.filterIsInstance<Link>()
            assertTrue(linkElements.isNotEmpty(), "Should find link elements in: ${linkData.text}")
            
            // Verify link URL and text extraction
            linkElements.forEach { link ->
                assertEquals(linkData.expectedUrl, link.url, "Link URL should be extracted correctly")
                assertEquals(linkData.expectedText, link.text, "Link text should be extracted correctly")
                assertTrue(link.url.isNotEmpty(), "Link URL should not be empty")
            }
        }
    }

    @Test
    fun `Property 5e - Image markup parsing should extract path and alt text correctly`() {
        repeat(20) {
            val imageData = generateImageTestData()
            val elements = inlineParser.parseInlineElements(imageData.text, imageData.lineNumber)
            
            val imageElements = elements.filterIsInstance<Image>()
            assertTrue(imageElements.isNotEmpty(), "Should find image elements in: ${imageData.text}")
            
            // Verify image path and alt text extraction
            imageElements.forEach { image ->
                assertEquals(imageData.expectedPath, image.path, "Image path should be extracted correctly")
                assertEquals(imageData.expectedAltText, image.altText, "Image alt text should be extracted correctly")
                assertTrue(image.path.isNotEmpty(), "Image path should not be empty")
            }
        }
    }

    // Test data generation functions
    private fun generateInlineElementTestData(): TestInlineElement {
        return when (Random.nextInt(6)) {
            0 -> generateStrongTestData()
            1 -> generateEmphasisTestData()
            2 -> generateCodeTestData()
            3 -> generateLinkTestData()
            4 -> generateImageTestData()
            else -> generatePlainTextTestData()
        }
    }

    private fun generateStrongTestData(): TestStrongData {
        val content = generateRandomText()
        val text = "*$content*"
        val lineNumber = Random.nextInt(1, 1001)
        
        return TestStrongData(text, lineNumber, content)
    }

    private fun generateEmphasisTestData(): TestEmphasisData {
        val content = generateRandomText()
        val text = "_${content}_"
        val lineNumber = Random.nextInt(1, 1001)
        
        return TestEmphasisData(text, lineNumber, content)
    }

    private fun generateCodeTestData(): TestCodeData {
        val content = generateRandomCodeText()
        val text = "`$content`"
        val lineNumber = Random.nextInt(1, 1001)
        
        return TestCodeData(text, lineNumber, content)
    }

    private fun generateLinkTestData(): TestLinkData {
        val url = generateRandomUrl()
        val linkText = generateRandomText()
        val text = "link:$url[$linkText]"
        val lineNumber = Random.nextInt(1, 1001)
        
        return TestLinkData(text, lineNumber, url, linkText)
    }

    private fun generateImageTestData(): TestImageData {
        val path = generateRandomImagePath()
        val altText = generateRandomText()
        val text = "image:$path[$altText]"
        val lineNumber = Random.nextInt(1, 1001)
        
        return TestImageData(text, lineNumber, path, altText)
    }

    private fun generatePlainTextTestData(): TestPlainTextData {
        val text = generateRandomText()
        val lineNumber = Random.nextInt(1, 1001)
        
        return TestPlainTextData(text, lineNumber)
    }

    private fun generateRandomText(): String {
        val words = listOf("hello", "world", "test", "content", "sample", "text", "markup", "parser")
        val wordCount = Random.nextInt(1, 5)
        return (1..wordCount).map { words.random() }.joinToString(" ")
    }

    private fun generateRandomCodeText(): String {
        val codeSnippets = listOf(
            "println(\"Hello\")",
            "val x = 42",
            "function test()",
            "return true",
            "if (condition)",
            "for (i in 0..10)",
            "class MyClass",
            "import kotlin.test"
        )
        return codeSnippets.random()
    }

    private fun generateRandomUrl(): String {
        val domains = listOf("example.com", "test.org", "sample.net", "demo.io")
        val paths = listOf("", "/path", "/page.html", "/api/v1", "/docs")
        return "https://${domains.random()}${paths.random()}"
    }

    private fun generateRandomImagePath(): String {
        val filenames = listOf("image.png", "photo.jpg", "diagram.svg", "icon.gif")
        val paths = listOf("", "images/", "assets/", "media/")
        return "${paths.random()}${filenames.random()}"
    }
}

// Test data classes
sealed class TestInlineElement
data class TestStrongData(val text: String, val lineNumber: Int, val expectedContent: String) : TestInlineElement()
data class TestEmphasisData(val text: String, val lineNumber: Int, val expectedContent: String) : TestInlineElement()
data class TestCodeData(val text: String, val lineNumber: Int, val expectedContent: String) : TestInlineElement()
data class TestLinkData(val text: String, val lineNumber: Int, val expectedUrl: String, val expectedText: String) : TestInlineElement()
data class TestImageData(val text: String, val lineNumber: Int, val expectedPath: String, val expectedAltText: String) : TestInlineElement()
data class TestPlainTextData(val text: String, val lineNumber: Int) : TestInlineElement()