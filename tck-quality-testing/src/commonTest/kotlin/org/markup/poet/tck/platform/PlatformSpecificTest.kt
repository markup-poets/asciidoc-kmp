package org.markup.poet.tck.platform

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.plainText
import org.markup.poet.asciidoc.parser.DefaultAsciidocParser
import org.markup.poet.tck.compatibility.CompatibilityTest
import org.markup.poet.tck.fixtures.FixtureLoader
import org.markup.poet.tck.fixtures.ResourceFixtureLoader
import org.markup.poet.tck.validation.DefaultOutputValidator
import org.markup.poet.tck.validation.OutputValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Platform-specific validation tests for encoding and line-ending handling.
 *
 * File-system-dependent tests (file I/O, include path resolution) live in the
 * JVM source set (`PlatformFileSystemTest`), since they need a real file system.
 *
 * Requirements: 7.1, 7.2, 7.3
 */
class PlatformSpecificTest : CompatibilityTest() {

    override val fixtureLoader: FixtureLoader = ResourceFixtureLoader()
    override val validator: OutputValidator = DefaultOutputValidator()

    private val parser = DefaultAsciidocParser()

    private fun parse(source: String): AsgDocument = parser.parse(source).document

    /** All plain text of the document body, concatenated. */
    private fun bodyText(document: AsgDocument): String =
        document.blocks.filterIsInstance<LeafBlock>().joinToString("\n") { plainText(it.inlines) }

    // Line Ending Tests

    @Test
    fun `should handle different line endings across platforms`() {
        val lf = "= Title\n\nFirst line.\nSecond line.\n\nSecond paragraph."
        val crlf = lf.replace("\n", "\r\n")

        val lfDoc = parse(lf)
        val crlfDoc = parse(crlf)

        // CRLF input is normalized: both variants produce the same document structure.
        assertEquals(lfDoc.blocks.size, crlfDoc.blocks.size)
        assertEquals(bodyText(lfDoc), bodyText(crlfDoc))
        assertEquals("First line.\nSecond line.", plainText((lfDoc.blocks[0] as LeafBlock).inlines))
    }

    // Encoding Tests

    @Test
    fun `should handle basic UTF-8 characters`() {
        val fixture = fixtureLoader.loadFixture("platform-encoding-utf8-basic")
        assertTrue(fixture.input.contains("café"))
        assertTrue(fixture.input.contains("naïve"))
        assertTrue(fixture.input.contains("résumé"))

        val document = parse(fixture.input)
        val body = bodyText(document)
        assertTrue(body.contains("café"))
        assertTrue(body.contains("naïve"))
        assertTrue(body.contains("résumé"))
    }

    @Test
    fun `should handle emoji and special symbols`() {
        val fixture = fixtureLoader.loadFixture("platform-encoding-utf8-emoji")
        assertTrue(fixture.input.contains("📚"))
        assertTrue(fixture.input.contains("🎉"))
        assertTrue(fixture.input.contains("©"))

        val document = parse(fixture.input)
        val header = assertNotNull(document.header)
        assertTrue(plainText(header.title).contains("📚"))
        val body = bodyText(document)
        assertTrue(body.contains("🎉"))
        assertTrue(body.contains("©"))
    }

    @Test
    fun `should handle multilingual content`() {
        val fixture = fixtureLoader.loadFixture("platform-encoding-utf8-multilingual")
        val document = parse(fixture.input)
        val body = bodyText(document)
        assertTrue(body.contains("こんにちは世界")) // Japanese
        assertTrue(body.contains("你好世界")) // Chinese
        assertTrue(body.contains("Привет мир")) // Russian
        assertTrue(body.contains("مرحبا بالعالم")) // Arabic
    }

    @Test
    fun `should handle special typographic characters`() {
        val fixture = fixtureLoader.loadFixture("platform-encoding-special-chars")
        val document = parse(fixture.input)
        val body = bodyText(document)
        assertTrue(body.contains("–")) // en-dash
        assertTrue(body.contains("—")) // em-dash
        assertTrue(body.contains("∑")) // summation
        assertTrue(body.contains("«guillemets»"))
    }

    @Test
    fun `should handle zero-width and combining characters`() {
        val zeroWidthJoiner = "\u200D"
        val zeroWidthNonJoiner = "\u200C"
        val fixture = fixtureLoader.loadFixture("platform-encoding-zero-width")
        assertTrue(fixture.input.contains(zeroWidthJoiner))
        assertTrue(fixture.input.contains(zeroWidthNonJoiner))

        val document = parse(fixture.input)
        val body = bodyText(document)
        // Zero-width and combining characters survive parsing unchanged.
        assertTrue(body.contains(zeroWidthJoiner))
        assertTrue(body.contains(zeroWidthNonJoiner))
        assertTrue(body.contains("combining acute"))
    }
}
