package org.markup.poet.asciidoc.render

import org.markup.poet.asciidoc.parser.DefaultAsciidocParser
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Regression test for issue #139: a small "kitchen sink" document must render
 * every construct it uses, with no macro placeholders left over.
 */
class DemoDocumentRenderingTest {

    private val source = """
= My document
Norberto Lopes <nlopes@example.com>
:date: 2025-01-15T10:00:00+00:00
:description: A quick preview
:github-icon: icon:github[set=brands]
:live-preview: *live preview*
:toc: macro
:icons: font
:stem: asciimath
:sectnums:

toc::[]

icon:arrow-right[]
This is a {live-preview} editor for https://asciidoc.org[AsciiDoc].

== Getting started

Type in the left pane and see the rendered output on the right.

This is using {github-icon} https://github.com/nlopes/acdc/tree/main/acdc-parser[`acdc-parser`], which you can find in the {github-icon} https://github.com/nlopes/acdc[`acdc`] repository.

* Unordered list item
* Another item

=== Media

.A random video
video::RvRhUHTV_8k[youtube,list=PLDitloyBcHOm49bxNhvGgg0f9NRZ5lSaP]

=== Stem

A matrix can be written as stem:[[[a,b\],[c,d\]\]((n),(k))].

And another inline formula: latexmath:[C = \alpha + \beta Y^{\gamma} + \epsilon].

[stem]
++++
sqrt(4) = 2
++++

=== Code blocks footnote:code-blocks[I love code blocks]

."Hello World" example
[%collapsible%open]
====
[source,rust]
----
fn main() {
    println!("Hello from the editor!");
}
----
====

=== Tables
.List of night agents
[%header,cols="30%,70%"]
|===
|Name|Email
|Peter Sutherland|peter.sutherland@example.com
|Adam Corrigan|adam.corrigan@example.com
|===

TIP: Try adding sections, lists, and inline formatting.
"""

    private val html: String by lazy {
        val document = DefaultAsciidocParser().parse(source.trimStart('\n')).document
        val escaper = DefaultHtmlEscaper()
        val builder = DefaultHtmlBuilder(escaper)
        val inlineRenderer = DefaultInlineRenderer(builder)
        DefaultHtmlRenderer(DefaultBlockRenderer(builder, inlineRenderer), inlineRenderer)
            .render(document, RenderConfig())
            .getOrThrow()
    }

    private fun assertRendered(fragment: String) =
        assertTrue(fragment in html, "expected <$fragment> in:\n$html")

    @Test
    fun `leaves no unexpanded macro placeholders`() {
        assertFalse("macro-placeholder" in html, html)
    }

    @Test
    fun `renders the document title and author line`() {
        assertRendered("<h1>My document</h1>")
        assertRendered("<span id=\"author\" class=\"author\">Norberto Lopes</span>")
        assertRendered("<a href=\"mailto:nlopes@example.com\">nlopes@example.com</a>")
    }

    @Test
    fun `fills the toc macro with numbered section links`() {
        assertRendered("<a href=\"#getting-started\">1. Getting started</a>")
        assertRendered("<a href=\"#code-blocks\">1.3. Code blocks</a>")
    }

    @Test
    fun `numbers section headings under sectnums`() {
        assertRendered(">1. Getting started</h2>")
        assertRendered(">1.1. Media</h3>")
    }

    @Test
    fun `renders font icons including the brands set from an attribute value`() {
        assertRendered("<i class=\"fa fa-arrow-right\"></i>")
        assertRendered("<i class=\"fa-brands fa-github\"></i>")
        assertRendered("font-awesome")
    }

    @Test
    fun `applies inline formatting inside attribute values`() {
        assertRendered("This is a <strong>live preview</strong> editor")
    }

    @Test
    fun `embeds a youtube video with its playlist and title`() {
        assertRendered("https://www.youtube.com/embed/RvRhUHTV_8k?rel=0&amp;list=PLDitloyBcHOm49bxNhvGgg0f9NRZ5lSaP")
        assertRendered("<div class=\"title\">A random video</div>")
    }

    @Test
    fun `renders inline and block math with escaped brackets intact`() {
        assertRendered("\\\$[[a,b],[c,d]]((n),(k))\\\$")
        assertRendered("\\(C = \\alpha + \\beta Y^{\\gamma} + \\epsilon\\)")
        assertRendered("<div class=\"stemblock\">")
        assertRendered("\\\$sqrt(4) = 2\\\$")
        assertRendered("mathjax")
    }

    @Test
    fun `renders a footnote in a section title without polluting the section id`() {
        assertRendered("<h3 id=\"code-blocks\"")
        assertRendered("href=\"#_footnotedef_1\"")
        assertRendered("<div class=\"footnote\" id=\"_footnotedef_1\">1. I love code blocks</div>")
    }

    @Test
    fun `renders a collapsible example block as an open details element`() {
        assertRendered("<details open>")
        assertRendered("<summary class=\"title\">\"Hello World\" example</summary>")
        assertRendered("<code class=\"language-rust\">")
    }

    @Test
    fun `renders the table with header row and percentage column widths`() {
        assertRendered("<col style=\"width: 30%\"><col style=\"width: 70%\">")
        assertRendered("<th>Name</th><th>Email</th>")
    }

    @Test
    fun `renders the admonition paragraph`() {
        assertRendered("<div class=\"admonitionblock tip\">")
    }
}
