package org.markup.poet.asciidoc.render

import org.markup.poet.asciidoc.asg.Inline
import org.markup.poet.asciidoc.asg.InlineAttributeRef
import org.markup.poet.asciidoc.asg.InlineCallout
import org.markup.poet.asciidoc.asg.InlineCitation
import org.markup.poet.asciidoc.asg.InlineFootnote
import org.markup.poet.asciidoc.asg.InlineMacro
import org.markup.poet.asciidoc.asg.InlineRaw
import org.markup.poet.asciidoc.asg.InlineRef
import org.markup.poet.asciidoc.asg.InlineSpan
import org.markup.poet.asciidoc.asg.InlineText
import org.markup.poet.asciidoc.asg.RefVariant
import org.markup.poet.asciidoc.asg.SpanVariant
import org.markup.poet.asciidoc.asg.plainText

/**
 * Renders ASG inline nodes to HTML.
 *
 * InlineRenderer handles text-level markup like emphasis, strong text, code spans,
 * marks, links, and cross-references. It processes nested inline nodes recursively
 * to maintain proper HTML structure.
 *
 * Implementations must ensure:
 * - All text content is properly escaped to prevent XSS attacks
 * - URLs are sanitized to prevent javascript: and data: URI schemes
 * - Images include alt text for accessibility
 * - Nested inline nodes are rendered correctly
 */
interface InlineRenderer {
    /**
     * Renders an inline node to HTML.
     *
     * @param inline The inline node to render
     * @param context The rendering context containing configuration and state
     * @return The HTML string representation of the inline node
     */
    fun render(inline: Inline, context: RenderContext): String
}

/**
 * Default implementation of InlineRenderer that produces semantic HTML5 output.
 *
 * This implementation:
 * - Converts InlineText nodes to escaped text content
 * - Converts InlineSpan nodes to `<strong>`, `<em>`, `<code>`, or `<mark>` elements
 * - Converts InlineRef nodes to `<a>` elements (links with URL sanitization,
 *   xrefs as fragment anchors)
 * - Converts InlineMacro nodes: built-in `image`/`link`/`xref` names map to their
 *   HTML elements, anything else renders as a placeholder with a warning
 * - Converts the processing-phase extension nodes (attribute refs, callouts,
 *   footnotes, citations, raw inline output) to their HTML forms
 * - Handles nested inline nodes recursively
 *
 * @param builder The HtmlBuilder to use for escaping
 */
class DefaultInlineRenderer(
    private val builder: HtmlBuilder
) : InlineRenderer {

    override fun render(inline: Inline, context: RenderContext): String {
        return when (inline) {
            is InlineText -> renderText(inline)
            is InlineSpan -> renderSpan(inline, context)
            is InlineRef -> renderRef(inline, context)
            is InlineMacro -> renderMacro(inline, context)
            is InlineAttributeRef -> renderAttributeRef(inline, context)
            is InlineCallout -> renderCallout(inline)
            is InlineFootnote -> renderFootnote(inline)
            is InlineCitation -> renderCitation(inline)
            is InlineRaw -> if (inline.format == "html") inline.content else ""
        }
    }

    /** Renders plain text with HTML escaping. */
    private fun renderText(text: InlineText): String {
        return builder.escape(text.value)
    }

    /**
     * Renders a formatting span.
     *
     * - STRONG -> `<strong>` with nested inlines
     * - EMPHASIS -> `<em>` with nested inlines
     * - CODE -> `<code>` holding the span's concatenated text, escaped
     * - MARK -> `<mark>` with nested inlines
     */
    private fun renderSpan(span: InlineSpan, context: RenderContext): String {
        return when (span.variant) {
            SpanVariant.STRONG -> "<strong>${renderNested(span.inlines, context)}</strong>"
            SpanVariant.EMPHASIS -> "<em>${renderNested(span.inlines, context)}</em>"
            SpanVariant.CODE -> "<code>${builder.escape(plainText(span.inlines))}</code>"
            SpanVariant.MARK -> "<mark>${renderNested(span.inlines, context)}</mark>"
        }
    }

    /**
     * Renders a reference.
     *
     * - LINK -> `<a href="...">` with URL sanitization; the link text is the
     *   nested inline content (falling back to the target)
     * - XREF -> `<a href="#target">` pointing at an in-document anchor
     */
    private fun renderRef(ref: InlineRef, context: RenderContext): String {
        return when (ref.variant) {
            RefVariant.LINK -> {
                val escapedUrl = builder.escapeAttribute(sanitizeUrl(ref.target))
                val text = if (ref.inlines.isEmpty()) {
                    builder.escape(ref.target)
                } else {
                    renderNested(ref.inlines, context)
                }
                "<a href=\"$escapedUrl\">$text</a>"
            }
            RefVariant.XREF -> {
                val escapedId = builder.escapeAttribute(ref.target)
                val text = if (ref.inlines.isEmpty()) {
                    builder.escape(ref.target)
                } else {
                    renderNested(ref.inlines, context)
                }
                "<a href=\"#$escapedId\">$text</a>"
            }
        }
    }

    /**
     * Renders a generic inline macro.
     *
     * The built-in names map to their HTML elements (`image` -> `<img>`,
     * `link` -> `<a>`, `xref` -> in-document anchor). Any other name is an
     * extension seam: by the time rendering runs, extension processors should
     * have replaced claimed macros, so a leftover macro renders as a
     * placeholder with a warning.
     */
    private fun renderMacro(macro: InlineMacro, context: RenderContext): String {
        return when (macro.name) {
            "image" -> {
                val escapedSrc = builder.escapeAttribute(sanitizeUrl(macro.target))
                val escapedAlt = builder.escapeAttribute(macro.positional.firstOrNull() ?: "")
                val titleAttr = macro.named["title"]?.let { " title=\"${builder.escapeAttribute(it)}\"" } ?: ""
                "<img src=\"$escapedSrc\" alt=\"$escapedAlt\"$titleAttr>"
            }
            "link" -> {
                val escapedUrl = builder.escapeAttribute(sanitizeUrl(macro.target))
                val text = builder.escape(macro.positional.firstOrNull() ?: macro.target)
                val titleAttr = macro.named["title"]?.let { " title=\"${builder.escapeAttribute(it)}\"" } ?: ""
                "<a href=\"$escapedUrl\"$titleAttr>$text</a>"
            }
            "xref" -> {
                val escapedId = builder.escapeAttribute(macro.target)
                val text = builder.escape(macro.positional.firstOrNull() ?: macro.target)
                "<a href=\"#$escapedId\">$text</a>"
            }
            else -> {
                context.logWarning("Unexpanded macro: ${macro.name}")
                val escapedName = builder.escape("${macro.name}[]")
                "<span class=\"macro-placeholder\">$escapedName</span>"
            }
        }
    }

    /**
     * Renders an unresolved attribute reference as its literal `{name}` text.
     *
     * In the rendering phase, attribute references should have been resolved
     * by the document-processing module; encountering one here is worth a warning.
     */
    private fun renderAttributeRef(ref: InlineAttributeRef, context: RenderContext): String {
        context.logWarning("Unresolved attribute reference: {${ref.name}}")
        return builder.escape("{${ref.name}}")
    }

    /** Renders a callout marker as a styled `<span>` (e.g. `<1>`). */
    private fun renderCallout(callout: InlineCallout): String {
        return "<span class=\"callout\">&lt;${callout.number}&gt;</span>"
    }

    /** Renders a footnote reference as a superscript anchor. */
    private fun renderFootnote(footnote: InlineFootnote): String {
        return """<sup class="footnote-ref"><a href="#fn-${footnote.id}">${footnote.id}</a></sup>"""
    }

    /** Renders a bibliography citation as an anchor to the entry. */
    private fun renderCitation(citation: InlineCitation): String {
        return """<a href="#${citation.citationId}" class="bibliography-ref">[${citation.citationId}]</a>"""
    }

    /**
     * Renders a list of nested inline nodes recursively.
     */
    private fun renderNested(content: List<Inline>, context: RenderContext): String {
        return content.joinToString("") { render(it, context) }
    }

    /**
     * Sanitizes a URL to prevent XSS attacks.
     *
     * Blocks javascript: and data: URI schemes by replacing them with "#".
     * This prevents execution of malicious scripts through link URLs.
     */
    private fun sanitizeUrl(url: String): String {
        val trimmed = url.trim()
        val lower = trimmed.lowercase()

        // Block dangerous URI schemes
        if (lower.startsWith("javascript:") || lower.startsWith("data:")) {
            return "#"
        }

        return trimmed
    }
}
