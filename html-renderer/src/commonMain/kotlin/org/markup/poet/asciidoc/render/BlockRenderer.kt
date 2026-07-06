package org.markup.poet.asciidoc.render

import org.markup.poet.asciidoc.ast.*

/**
 * Renders block-level elements to HTML.
 * 
 * BlockRenderer handles structural components like sections, paragraphs, lists,
 * code blocks, and tables. It processes block elements and their nested content
 * to produce semantic HTML5 output.
 * 
 * Implementations must ensure:
 * - Block elements are converted to appropriate semantic HTML5 elements
 * - Nested content (inline elements, nested lists) is rendered correctly
 * - CSS classes from the theme are applied consistently
 * - IDs are generated for elements that need them (e.g., headings)
 * - Unknown block types are handled gracefully with warnings
 */
interface BlockRenderer {
    /**
     * Renders a block element to HTML.
     * 
     * @param block The block element to render
     * @param context The rendering context containing configuration and state
     * @return The HTML string representation of the block element
     */
    fun render(block: BlockElement, context: RenderContext): String
}

/**
 * Default implementation of BlockRenderer that produces semantic HTML5 output.
 * 
 * This implementation:
 * - Converts Section nodes to `<section>` elements with `<h1>`-`<h6>` headings
 * - Converts Paragraph nodes to `<p>` elements
 * - Converts AsciiDocList nodes to `<ul>` or `<ol>` elements with `<li>` items
 * - Converts CodeBlock nodes to `<pre><code>` structures with language classes
 * - Handles nested content recursively (inline elements, nested lists)
 * - Applies theme CSS classes to all elements
 * - Generates unique IDs for sections/headings
 * - Logs warnings for unknown block types
 * 
 * Note: The design document mentions additional block types (Table, Quote, ImageBlock)
 * that are not yet present in the AST. This implementation handles the currently
 * available block types and can be extended as new types are added to the AST.
 * 
 * @param builder The HtmlBuilder to use for constructing HTML
 * @param inlineRenderer The InlineRenderer to use for rendering inline content
 */
class DefaultBlockRenderer(
    private val builder: HtmlBuilder,
    private val inlineRenderer: InlineRenderer
) : BlockRenderer {
    
    override fun render(block: BlockElement, context: RenderContext): String {
        return when (block) {
            is Section -> renderSection(block, context)
            is Paragraph -> renderParagraph(block, context)
            is AsciiDocList -> renderList(block, context)
            is CodeBlock -> renderCodeBlock(block, context)
            is Comment -> renderComment(block, context)
            is ListItem -> renderListItem(block, context)
            is CalloutList -> renderCalloutList(block, context)
            is CalloutListItem -> renderCalloutListItem(block, context)
            is IncludeDirective -> renderIncludeDirective(block, context)
            is Document -> renderDocument(block, context)
            is AdmonitionBlock -> renderAdmonitionBlock(block, context)
            is ConditionalDirective -> renderConditionalDirective(block, context)
            is BibliographyEntry -> renderBibliographyEntry(block, context)
            is CustomBlock -> renderCustomBlock(block, context)
            is PassthroughBlock -> renderPassthroughBlock(block)
        }
    }

    /**
     * Fallback rendering for a custom block no extension processor claimed:
     * a listing-style `<pre>` carrying the block name as a CSS class, so the
     * content stays visible instead of silently disappearing.
     */
    private fun renderCustomBlock(block: CustomBlock, context: RenderContext): String {
        val preClasses = "${context.theme.codeBlockClasses()} custom-block custom-block-${block.name}"
        return builder.build {
            openTag("pre", mapOf("class" to preClasses))
            openTag("code")
            text(builder.escape(block.rawContent))
            closeTag("code")
            closeTag("pre")
        }
    }

    /** Pre-rendered extension output: emitted verbatim for HTML, skipped otherwise. */
    private fun renderPassthroughBlock(block: PassthroughBlock): String =
        if (block.format == "html") block.content else ""
    
    /**
     * Renders a section with heading and nested content.
     * 
     * Sections are rendered as `<section>` elements containing:
     * - A heading (`<h1>` through `<h6>`) with a unique ID
     * - Nested block elements (children)
     * 
     * The heading level is clamped to 1-6 to ensure valid HTML.
     * 
     * Validates: Requirements 1.2, 2.1, 6.1, 8.3
     */
    private fun renderSection(section: Section, context: RenderContext): String {
        val level = section.level.coerceIn(1, 6)
        val id = context.generateId(section.title)
        
        // Register heading for TOC and hierarchy validation
        context.registerHeading(level, id, section.title)
        
        val headingClasses = context.theme.headingClasses(level)
        
        // Process attribute handlers
        val handlerAttrs = context.processAttributeHandlers(section)
        val headingAttrs = mutableMapOf("id" to id, "class" to headingClasses)
        headingAttrs.putAll(handlerAttrs)
        
        val heading = builder.build {
            openTag("h$level", headingAttrs)
            text(builder.escape(section.title))
            closeTag("h$level")
        }
        
        val children = section.children.joinToString("\n") { render(it, context) }
        
        return builder.build {
            openTag("section", mapOf("id" to "$id-section"))
            text(heading)
            if (children.isNotEmpty()) {
                text("\n")
                text(children)
            }
            closeTag("section")
        }
    }
    
    /**
     * Renders a paragraph as `<p>` element.
     * 
     * Paragraphs contain inline elements that are rendered recursively.
     * Theme CSS classes are applied for styling.
     * 
     * Validates: Requirements 1.2, 2.2, 6.1
     */
    private fun renderParagraph(paragraph: Paragraph, context: RenderContext): String {
        val classes = context.theme.paragraphClasses()
        val content = renderInlineContent(paragraph.content, context)
        
        // Process attribute handlers
        val handlerAttrs = context.processAttributeHandlers(paragraph)
        val attrs = mutableMapOf("class" to classes)
        
        // Add role attribute as CSS class if present
        paragraph.attributes["role"]?.let { role ->
            attrs["class"] = "$classes $role"
        }
        
        // Add id attribute if present
        paragraph.attributes["id"]?.let { id ->
            attrs["id"] = id
        }
        
        // Add any data- attributes from node attributes
        paragraph.attributes.forEach { (key, value) ->
            if (key.startsWith("data-")) {
                attrs[key] = value
            }
        }
        
        attrs.putAll(handlerAttrs)
        
        return builder.build {
            openTag("p", attrs)
            text(content)
            closeTag("p")
        }
    }
    
    /**
     * Renders a list as `<ul>` or `<ol>` element.
     * 
     * The list type determines whether to use unordered (`<ul>`) or
     * ordered (`<ol>`) list elements. List items are rendered as `<li>` elements.
     * 
     * Validates: Requirements 1.2, 2.3, 2.4, 6.1
     */
    private fun renderList(list: AsciiDocList, context: RenderContext): String {
        val tagName = when (list.type) {
            ListType.UNORDERED -> "ul"
            ListType.ORDERED -> "ol"
            ListType.DEFINITION -> "dl" // Definition lists use <dl>
        }
        
        val classes = context.theme.listClasses()
        
        return builder.build {
            openTag(tagName, mapOf("class" to classes))
            list.items.forEach { item ->
                text("\n")
                text(renderListItem(item, context))
            }
            text("\n")
            closeTag(tagName)
        }
    }
    
    /**
     * Renders a list item as `<li>` element.
     * 
     * List items contain inline content and may have nested lists.
     * If a nested list is present, it is rendered after the item content.
     * 
     * Validates: Requirements 1.2, 2.3, 2.4
     */
    private fun renderListItem(item: ListItem, context: RenderContext): String {
        val content = renderInlineContent(item.content, context)
        val nestedList = item.nestedList
        
        return builder.build {
            openTag("li")
            text(content)
            
            // Render nested list if present
            if (nestedList != null) {
                text("\n")
                text(renderList(nestedList, context))
            }
            
            closeTag("li")
        }
    }
    
    /**
     * Renders a code block as `<pre><code>` structure.
     * 
     * Code blocks are rendered with:
     * - `<pre>` element with theme CSS classes
     * - `<code>` element with language-specific class if language is specified
     * - Escaped content to prevent HTML injection
     * 
     * The language class follows the format `language-{language}` for
     * compatibility with syntax highlighting libraries like Prism.js and highlight.js.
     * 
     * Validates: Requirements 1.2, 2.5, 6.2, 8.5
     */
    private fun renderCodeBlock(code: CodeBlock, context: RenderContext): String {
        val language = code.language ?: ""
        val languageClass = if (language.isNotEmpty()) "language-$language" else ""
        val preClasses = context.theme.codeBlockClasses()
        
        // Build attributes for pre element
        val preAttrs = mutableMapOf("class" to preClasses)
        
        // Add role attribute as CSS class if present
        code.attributes["role"]?.let { role ->
            preAttrs["class"] = "$preClasses $role"
        }
        
        // Add id attribute if present
        code.attributes["id"]?.let { id ->
            preAttrs["id"] = id
        }
        
        return builder.build {
            openTag("pre", preAttrs)
            openTag("code", mapOf("class" to languageClass))
            text(builder.escape(code.content))
            closeTag("code")
            closeTag("pre")
        }
    }
    
    /**
     * Renders a comment block.
     * 
     * Comments are typically not rendered in the final HTML output.
     * This implementation returns an empty string, effectively hiding comments.
     * 
     * If you want to preserve comments in the HTML (e.g., for debugging),
     * you could render them as HTML comments: `<!-- content -->`
     */
    private fun renderComment(comment: Comment, context: RenderContext): String {
        // Comments are not rendered in the output
        // Alternatively, could render as HTML comment: "<!-- ${comment.content} -->"
        return ""
    }
    
    /**
     * Renders a callout list as an ordered list with special styling.
     * 
     * Callout lists are used to annotate code blocks with numbered references.
     * They are rendered as `<ol>` elements with a special CSS class.
     * 
     * Validates: Requirements 1.2
     */
    private fun renderCalloutList(calloutList: CalloutList, context: RenderContext): String {
        return builder.build {
            openTag("ol", mapOf("class" to "callout-list"))
            calloutList.items.forEach { item ->
                text("\n")
                text(renderCalloutListItem(item, context))
            }
            text("\n")
            closeTag("ol")
        }
    }
    
    /**
     * Renders a callout list item.
     * 
     * Callout list items are rendered as `<li>` elements with the callout number
     * as a data attribute for styling purposes.
     */
    private fun renderCalloutListItem(item: CalloutListItem, context: RenderContext): String {
        val content = renderInlineContent(item.content, context)
        
        return builder.build {
            openTag("li", mapOf("data-callout" to item.number.toString()))
            text(content)
            closeTag("li")
        }
    }
    
    /**
     * Renders an include directive.
     * 
     * In the rendering phase, include directives should have been resolved
     * by the document-processing module. If we encounter one here, we render
     * a placeholder with a warning.
     * 
     * Validates: Requirements 11.3
     */
    private fun renderIncludeDirective(include: IncludeDirective, context: RenderContext): String {
        context.logWarning("Unresolved include directive: ${include.path}")
        val escapedPath = builder.escape("include::${include.path}[]")
        return "<div class=\"include-placeholder\">$escapedPath</div>"
    }
    
    /**
     * Renders a document node.
     * 
     * This method is called when a Document node is encountered as a block element.
     * It renders all children of the document.
     * 
     * Note: The main HtmlRenderer will handle document-level structure (html, head, body).
     * This method just renders the document's block content.
     * 
     * Validates: Requirements 1.1
     */
    private fun renderDocument(document: Document, context: RenderContext): String {
        return document.children.joinToString("\n") { render(it, context) }
    }
    
    /**
     * Renders a list of inline elements recursively.
     * 
     * This helper method is used to render inline content within block elements
     * like paragraphs and list items.
     * 
     * Validates: Requirements 1.3, 1.4
     */
    private fun renderInlineContent(content: List<InlineElement>, context: RenderContext): String {
        return content.joinToString("") { inlineRenderer.render(it, context) }
    }
    
    /**
     * Render an admonition block (NOTE, TIP, WARNING, etc.)
     */
    private fun renderAdmonitionBlock(block: AdmonitionBlock, context: RenderContext): String {
        val content = block.content.joinToString("\n") { render(it, context) }
        return """<div class="admonitionblock ${block.type.name.lowercase()}">
<table>
<tr>
<td class="icon"><div class="title">${block.type.name}</div></td>
<td class="content">$content</td>
</tr>
</table>
</div>"""
    }
    
    /**
     * Render a conditional directive (ifdef/ifndef)
     */
    private fun renderConditionalDirective(block: ConditionalDirective, context: RenderContext): String {
        // For now, just render the content (condition should be evaluated during processing)
        return block.content.joinToString("\n") { render(it, context) }
    }
    
    /**
     * Render a bibliography entry
     */
    private fun renderBibliographyEntry(block: BibliographyEntry, context: RenderContext): String {
        return """<div class="bibliography-entry" id="${block.id}">
<span class="bibliography-label">[${block.id}]</span>
<span class="bibliography-text">${builder.escape(block.citation)}</span>
</div>"""
    }
}
