package org.markup.poet.asciidoc.render

import org.markup.poet.asciidoc.asg.BibliographyEntryBlock
import org.markup.poet.asciidoc.asg.Block
import org.markup.poet.asciidoc.asg.BlockMacro
import org.markup.poet.asciidoc.asg.BlockMacroName
import org.markup.poet.asciidoc.asg.BlockMetadata
import org.markup.poet.asciidoc.asg.BreakBlock
import org.markup.poet.asciidoc.asg.BreakVariant
import org.markup.poet.asciidoc.asg.CommentBlock
import org.markup.poet.asciidoc.asg.ConditionalBlock
import org.markup.poet.asciidoc.asg.ConditionalVariant
import org.markup.poet.asciidoc.asg.CustomBlockMacro
import org.markup.poet.asciidoc.asg.DListBlock
import org.markup.poet.asciidoc.asg.DListItem
import org.markup.poet.asciidoc.asg.DiscreteHeading
import org.markup.poet.asciidoc.asg.IncludeBlock
import org.markup.poet.asciidoc.asg.Inline
import org.markup.poet.asciidoc.asg.InlineCallout
import org.markup.poet.asciidoc.asg.InlineText
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.LeafBlockForm
import org.markup.poet.asciidoc.asg.LeafBlockName
import org.markup.poet.asciidoc.asg.ListBlock
import org.markup.poet.asciidoc.asg.ListItem
import org.markup.poet.asciidoc.asg.ListVariant
import org.markup.poet.asciidoc.asg.ParentBlock
import org.markup.poet.asciidoc.asg.ParentBlockName
import org.markup.poet.asciidoc.asg.RawBlock
import org.markup.poet.asciidoc.asg.SectionBlock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Unit tests for DefaultBlockRenderer against the ASG block vocabulary.
 *
 * Covers the constructs the ASG models more richly than the legacy AST:
 * parent-block containers, verbatim variants, description lists, callout
 * lists, breaks, block macros, discrete headings, block metadata
 * (id/roles/title), and the processing-phase extension nodes.
 */
class DefaultBlockRendererTest {

    private val builder = DefaultHtmlBuilder(DefaultHtmlEscaper())
    private val renderer = DefaultBlockRenderer(builder, DefaultInlineRenderer(builder))

    private fun context() = RenderContext(RenderConfig())

    private fun text(value: String) = InlineText(value)

    private fun paragraph(content: String, metadata: BlockMetadata? = null) = LeafBlock(
        name = LeafBlockName.PARAGRAPH,
        form = LeafBlockForm.PARAGRAPH,
        inlines = listOf(text(content)),
        metadata = metadata
    )

    // ========== Sections ==========

    @Test
    fun `renders section with heading level offset and generated id`() {
        val section = SectionBlock(
            title = listOf(text("Getting Started")),
            level = 1, // `==`
            blocks = listOf(paragraph("Body text"))
        )
        val html = renderer.render(section, context())

        assertTrue(html.contains("<section id=\"getting-started-section\">"))
        assertTrue(html.contains("<h2 id=\"getting-started\" class=\"heading heading-2\">Getting Started</h2>"))
        assertTrue(html.contains("Body text"))
    }

    @Test
    fun `section uses explicit metadata id when present`() {
        val section = SectionBlock(
            title = listOf(text("Intro")),
            level = 1,
            blocks = emptyList(),
            metadata = BlockMetadata(id = "custom-id")
        )
        val html = renderer.render(section, context())

        assertTrue(html.contains("id=\"custom-id\""))
        assertTrue(html.contains("<section id=\"custom-id-section\">"))
    }

    @Test
    fun `renders nested sections recursively`() {
        val section = SectionBlock(
            title = listOf(text("Outer")),
            level = 1,
            blocks = listOf(
                SectionBlock(
                    title = listOf(text("Inner")),
                    level = 2,
                    blocks = listOf(paragraph("Deep content"))
                )
            )
        )
        val html = renderer.render(section, context())

        assertTrue(html.contains("<h2"))
        assertTrue(html.contains("<h3"))
        assertTrue(html.contains("Deep content"))
    }

    @Test
    fun `renders discrete heading without section wrapper`() {
        val heading = DiscreteHeading(
            title = listOf(text("Standalone")),
            level = 2
        )
        val html = renderer.render(heading, context())

        assertTrue(html.contains("<h3"))
        assertTrue(html.contains("discrete"))
        assertTrue(html.contains("Standalone"))
        assertFalse(html.contains("<section"))
    }

    // ========== Paragraphs and metadata ==========

    @Test
    fun `renders paragraph with roles and id from metadata`() {
        val block = paragraph("Styled", BlockMetadata(id = "para-1", roles = listOf("lead", "wide")))
        val html = renderer.render(block, context())

        assertEquals("<p class=\"paragraph lead wide\" id=\"para-1\">Styled</p>", html)
    }

    // ========== Verbatim blocks ==========

    @Test
    fun `renders source listing as pre code with language class`() {
        val block = LeafBlock(
            name = LeafBlockName.LISTING,
            form = LeafBlockForm.DELIMITED,
            delimiter = "----",
            inlines = listOf(text("val x = 1 < 2")),
            metadata = BlockMetadata(positional = listOf("source", "kotlin"))
        )
        val html = renderer.render(block, context())

        assertTrue(html.contains("<pre class=\"code-block\">"))
        assertTrue(html.contains("<code class=\"language-kotlin\">"))
        assertTrue(html.contains("val x = 1 &lt; 2"))
    }

    @Test
    fun `renders literal block as pre code`() {
        val block = LeafBlock(
            name = LeafBlockName.LITERAL,
            form = LeafBlockForm.DELIMITED,
            delimiter = "....",
            inlines = listOf(text("plain <text>"))
        )
        val html = renderer.render(block, context())

        assertTrue(html.contains("<pre class=\"code-block\"><code>plain &lt;text&gt;</code></pre>"))
    }

    @Test
    fun `renders callout markers inside listings`() {
        val block = LeafBlock(
            name = LeafBlockName.LISTING,
            form = LeafBlockForm.DELIMITED,
            delimiter = "----",
            inlines = listOf(text("code line "), InlineCallout(1)),
            metadata = BlockMetadata(positional = listOf("source", "kotlin"))
        )
        val html = renderer.render(block, context())

        assertTrue(html.contains("code line <span class=\"callout\">&lt;1&gt;</span>"))
    }

    @Test
    fun `renders pass block content verbatim`() {
        val block = LeafBlock(
            name = LeafBlockName.PASS,
            form = LeafBlockForm.DELIMITED,
            delimiter = "++++",
            inlines = listOf(text("<video controls></video>"))
        )
        val html = renderer.render(block, context())

        assertEquals("<video controls></video>", html)
    }

    @Test
    fun `renders stem block like a literal`() {
        val block = LeafBlock(
            name = LeafBlockName.STEM,
            form = LeafBlockForm.DELIMITED,
            delimiter = "++++",
            inlines = listOf(text("sqrt(4) = 2"))
        )
        val html = renderer.render(block, context())

        assertTrue(html.contains("<pre class=\"code-block\"><code>sqrt(4) = 2</code></pre>"))
    }

    @Test
    fun `renders verse block as verseblock pre`() {
        val block = LeafBlock(
            name = LeafBlockName.VERSE,
            form = LeafBlockForm.DELIMITED,
            delimiter = "____",
            inlines = listOf(text("Roses are red\nviolets are blue")),
            metadata = BlockMetadata(positional = listOf("verse"))
        )
        val html = renderer.render(block, context())

        assertTrue(html.contains("<pre class=\"verseblock\">Roses are red\nviolets are blue</pre>"))
    }

    @Test
    fun `renders unclaimed custom-style block as visible fallback`() {
        val block = LeafBlock(
            name = LeafBlockName.LISTING,
            form = LeafBlockForm.DELIMITED,
            delimiter = "----",
            inlines = listOf(text("raw content")),
            metadata = BlockMetadata(positional = listOf("gallery"))
        )
        val html = renderer.render(block, context())

        assertTrue(html.contains("custom-block custom-block-gallery"))
        assertTrue(html.contains("raw content"))
    }

    // ========== Parent block containers ==========

    @Test
    fun `renders admonition with variant class and title cell`() {
        val block = ParentBlock(
            name = ParentBlockName.ADMONITION,
            variant = "warning",
            blocks = listOf(paragraph("Beware!"))
        )
        val html = renderer.render(block, context())

        assertTrue(html.contains("<div class=\"admonitionblock warning\">"))
        assertTrue(html.contains("<div class=\"title\">WARNING</div>"))
        assertTrue(html.contains("Beware!"))
    }

    @Test
    fun `renders admonition block title from metadata`() {
        val block = ParentBlock(
            name = ParentBlockName.ADMONITION,
            variant = "note",
            blocks = listOf(paragraph("Content")),
            metadata = BlockMetadata(title = listOf(text("Important Note")))
        )
        val html = renderer.render(block, context())

        assertTrue(html.contains("<div class=\"title\">Important Note</div>"))
    }

    @Test
    fun `renders sidebar as sidebarblock div`() {
        val block = ParentBlock(
            name = ParentBlockName.SIDEBAR,
            delimiter = "****",
            blocks = listOf(paragraph("Aside text")),
            metadata = BlockMetadata(title = listOf(text("Sidebar Title")))
        )
        val html = renderer.render(block, context())

        assertTrue(html.startsWith("<div class=\"sidebarblock\">"))
        assertTrue(html.contains("<div class=\"title\">Sidebar Title</div>"))
        assertTrue(html.contains("<div class=\"content\">"))
        assertTrue(html.contains("Aside text"))
    }

    @Test
    fun `renders example as exampleblock div`() {
        val block = ParentBlock(
            name = ParentBlockName.EXAMPLE,
            delimiter = "====",
            blocks = listOf(paragraph("Example content"))
        )
        val html = renderer.render(block, context())

        assertTrue(html.startsWith("<div class=\"exampleblock\">"))
        assertTrue(html.contains("Example content"))
    }

    @Test
    fun `renders open block as openblock div`() {
        val block = ParentBlock(
            name = ParentBlockName.OPEN,
            delimiter = "--",
            blocks = listOf(paragraph("Open content"))
        )
        val html = renderer.render(block, context())

        assertTrue(html.startsWith("<div class=\"openblock\">"))
        assertTrue(html.contains("Open content"))
    }

    @Test
    fun `renders quote as blockquote with attribution`() {
        val block = ParentBlock(
            name = ParentBlockName.QUOTE,
            delimiter = "____",
            blocks = listOf(paragraph("To be or not to be.")),
            metadata = BlockMetadata(positional = listOf("quote", "Shakespeare", "Hamlet"))
        )
        val html = renderer.render(block, context())

        assertTrue(html.contains("<div class=\"quoteblock\">"))
        assertTrue(html.contains("<blockquote class=\"quote\">"))
        assertTrue(html.contains("To be or not to be."))
        assertTrue(html.contains("Shakespeare"))
        assertTrue(html.contains("<cite>Hamlet</cite>"))
    }

    @Test
    fun `renders container roles and id from metadata`() {
        val block = ParentBlock(
            name = ParentBlockName.SIDEBAR,
            blocks = listOf(paragraph("x")),
            metadata = BlockMetadata(id = "aside-1", roles = listOf("fancy"))
        )
        val html = renderer.render(block, context())

        assertTrue(html.contains("class=\"sidebarblock fancy\""))
        assertTrue(html.contains("id=\"aside-1\""))
    }

    // ========== Lists ==========

    @Test
    fun `renders unordered list`() {
        val list = ListBlock(
            variant = ListVariant.UNORDERED,
            marker = "*",
            items = listOf(
                ListItem(marker = "*", principal = listOf(text("first"))),
                ListItem(marker = "*", principal = listOf(text("second")))
            )
        )
        val html = renderer.render(list, context())

        assertTrue(html.startsWith("<ul class=\"list\">"))
        assertTrue(html.contains("<li>first</li>"))
        assertTrue(html.contains("<li>second</li>"))
        assertTrue(html.endsWith("</ul>"))
    }

    @Test
    fun `renders ordered list`() {
        val list = ListBlock(
            variant = ListVariant.ORDERED,
            marker = ".",
            items = listOf(ListItem(marker = ".", principal = listOf(text("step one"))))
        )
        val html = renderer.render(list, context())

        assertTrue(html.startsWith("<ol class=\"list\">"))
        assertTrue(html.contains("<li>step one</li>"))
    }

    @Test
    fun `renders nested list from item blocks`() {
        val nested = ListBlock(
            variant = ListVariant.UNORDERED,
            marker = "**",
            items = listOf(ListItem(marker = "**", principal = listOf(text("child"))))
        )
        val list = ListBlock(
            variant = ListVariant.UNORDERED,
            marker = "*",
            items = listOf(
                ListItem(marker = "*", principal = listOf(text("parent")), blocks = listOf(nested))
            )
        )
        val html = renderer.render(list, context())

        assertTrue(html.contains("<li>parent\n<ul class=\"list\">"))
        assertTrue(html.contains("<li>child</li>"))
        // The outer list must be complete despite the nested render.
        assertTrue(html.startsWith("<ul class=\"list\">"))
        assertTrue(html.endsWith("</ul>"))
        assertEquals(2, Regex("<ul ").findAll(html).count())
        assertEquals(2, Regex("</ul>").findAll(html).count())
    }

    @Test
    fun `renders callout list with data-callout markers`() {
        val list = ListBlock(
            variant = ListVariant.CALLOUT,
            marker = "<1>",
            items = listOf(
                ListItem(marker = "<1>", principal = listOf(text("the declaration"))),
                ListItem(marker = "<2>", principal = listOf(text("the usage")))
            )
        )
        val html = renderer.render(list, context())

        assertTrue(html.startsWith("<ol class=\"callout-list\">"))
        assertTrue(html.contains("<li data-callout=\"1\">the declaration</li>"))
        assertTrue(html.contains("<li data-callout=\"2\">the usage</li>"))
    }

    // ========== Description lists ==========

    @Test
    fun `renders description list as dl dt dd`() {
        val list = DListBlock(
            marker = "::",
            items = listOf(
                DListItem(
                    marker = "::",
                    terms = listOf(listOf(text("CPU"))),
                    principal = listOf(text("central processing unit"))
                ),
                DListItem(
                    marker = "::",
                    terms = listOf(listOf(text("RAM")), listOf(text("Memory"))),
                    principal = listOf(text("random access memory"))
                )
            )
        )
        val html = renderer.render(list, context())

        assertTrue(html.startsWith("<dl class=\"list\">"))
        assertTrue(html.contains("<dt>CPU</dt>"))
        assertTrue(html.contains("<dd>central processing unit</dd>"))
        assertTrue(html.contains("<dt>RAM</dt>"))
        assertTrue(html.contains("<dt>Memory</dt>"))
        assertTrue(html.contains("<dd>random access memory</dd>"))
        assertTrue(html.endsWith("</dl>"))
    }

    @Test
    fun `renders dlist item nested blocks inside dd`() {
        val list = DListBlock(
            marker = "::",
            items = listOf(
                DListItem(
                    marker = "::",
                    terms = listOf(listOf(text("term"))),
                    principal = listOf(text("summary")),
                    blocks = listOf(paragraph("details paragraph"))
                )
            )
        )
        val html = renderer.render(list, context())

        assertTrue(html.contains("<dd>summary\n<p class=\"paragraph\">details paragraph</p></dd>"))
    }

    // ========== Breaks and block macros ==========

    @Test
    fun `renders thematic break as hr`() {
        assertEquals("<hr/>", renderer.render(BreakBlock(BreakVariant.THEMATIC), context()))
    }

    @Test
    fun `renders page break as page-break div`() {
        val html = renderer.render(BreakBlock(BreakVariant.PAGE), context())
        assertEquals("<div style=\"page-break-after: always;\"></div>", html)
    }

    @Test
    fun `renders image block macro with alt from first positional attribute`() {
        val macro = BlockMacro(
            name = BlockMacroName.IMAGE,
            target = "images/diagram.png",
            metadata = BlockMetadata(positional = listOf("Architecture diagram"))
        )
        val html = renderer.render(macro, context())

        assertTrue(html.contains("<div class=\"imageblock\">"))
        assertTrue(html.contains("<img src=\"images/diagram.png\" alt=\"Architecture diagram\">"))
    }

    @Test
    fun `renders audio and video block macros`() {
        val audio = renderer.render(BlockMacro(name = BlockMacroName.AUDIO, target = "a.mp3"), context())
        val video = renderer.render(BlockMacro(name = BlockMacroName.VIDEO, target = "v.mp4"), context())

        assertTrue(audio.contains("<audio controls src=\"a.mp3\"></audio>"))
        assertTrue(video.contains("<video controls src=\"v.mp4\"></video>"))
    }

    @Test
    fun `renders toc macro as toc placeholder`() {
        val html = renderer.render(BlockMacro(name = BlockMacroName.TOC, target = null), context())
        assertEquals("<div id=\"toc\" class=\"toc\"></div>", html)
    }

    // ========== Processing extension nodes ==========

    @Test
    fun `renders raw html block verbatim and skips other formats`() {
        val html = renderer.render(RawBlock(format = "html", content = "<div class=\"x\">raw</div>"), context())
        val other = renderer.render(RawBlock(format = "docbook", content = "<para/>"), context())

        assertEquals("<div class=\"x\">raw</div>", html)
        assertEquals("", other)
    }

    @Test
    fun `renders bibliography entry with label and citation`() {
        val block = BibliographyEntryBlock(id = "knuth84", citation = "Knuth, TeXbook & more")
        val html = renderer.render(block, context())

        assertTrue(html.contains("<div class=\"bibliography-entry\" id=\"knuth84\">"))
        assertTrue(html.contains("<span class=\"bibliography-label\">[knuth84]</span>"))
        assertTrue(html.contains("Knuth, TeXbook &amp; more"))
    }

    @Test
    fun `comment blocks produce no output`() {
        assertEquals("", renderer.render(CommentBlock(text = "internal note"), context()))
    }

    @Test
    fun `unresolved include produces no output but logs warning`() {
        val context = context()
        val html = renderer.render(IncludeBlock(path = "chapter.adoc"), context)

        assertEquals("", html)
        assertTrue(context.getWarnings().any { it.contains("chapter.adoc") })
    }

    @Test
    fun `unclaimed custom block macro produces no output but logs warning`() {
        val context = context()
        val html = renderer.render(
            CustomBlockMacro(name = "gallery", target = "photos/2024"),
            context
        )

        assertEquals("", html)
        assertTrue(context.getWarnings().any { it.contains("gallery") })
    }

    @Test
    fun `unresolved conditional produces no output but logs warning`() {
        val context = context()
        val html = renderer.render(
            ConditionalBlock(
                variant = ConditionalVariant.IFDEF,
                condition = "backend-html5",
                blocks = listOf(paragraph("conditional content"))
            ),
            context
        )

        assertEquals("", html)
        assertTrue(context.getWarnings().isNotEmpty())
    }
}
