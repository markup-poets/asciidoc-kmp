package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.ast.*

/**
 * Interface for rendering a Document AST back to AsciiDoc source text.
 */
interface AsciidocRenderer {
    /**
     * Render the given document to AsciiDoc text.
     */
    fun render(document: Document): String
}

/**
 * Default implementation of AsciidocRenderer.
 */
class DefaultAsciidocRenderer : AsciidocRenderer {

    override fun render(document: Document): String {
        val sb = StringBuilder()
        
        // Render document attributes
        document.documentAttributes.forEach { (key, value) ->
            sb.append(":").append(key).append(":").append(if (value.isNotEmpty()) " $value" else "").append("\n")
        }
        
        if (document.documentAttributes.isNotEmpty() && document.children.isNotEmpty()) {
            sb.append("\n")
        }

        // Render children
        document.children.forEachIndexed { index, block ->
            sb.append(renderBlock(block))
            if (index < document.children.size - 1) {
                sb.append("\n\n")
            }
        }
        
        return sb.toString()
    }

    private fun renderBlock(block: BlockElement): String {
        return when (block) {
            is Section -> renderSection(block)
            is Paragraph -> renderParagraph(block)
            is AsciiDocList -> renderList(block)
            is CodeBlock -> renderCodeBlock(block)
            is Comment -> renderComment(block)
            is IncludeDirective -> renderIncludeDirective(block)
            is Document -> render(block) // Should not happen for children but just in case
            else -> ""
        }
    }

    private fun renderSection(section: Section): String {
        val sb = StringBuilder()
        sb.append("=".repeat(section.level)).append(" ").append(section.title).append("\n")
        section.children.forEachIndexed { index, child ->
            sb.append(renderBlock(child))
            if (index < section.children.size - 1) {
                sb.append("\n\n")
            }
        }
        return sb.toString()
    }

    private fun renderParagraph(paragraph: Paragraph): String {
        return paragraph.content.joinToString("") { renderInline(it) }
    }

    private fun renderList(list: AsciiDocList): String {
        return list.items.joinToString("\n") { renderListItem(it, list.type) }
    }

    private fun renderListItem(item: ListItem, type: ListType): String {
        val sb = StringBuilder()
        sb.append(item.marker).append(" ")
        sb.append(item.content.joinToString("") { renderInline(it) })
        val nestedList = item.nestedList
        if (nestedList != null) {
            sb.append("\n").append(renderList(nestedList))
        }
        return sb.toString()
    }

    private fun renderCodeBlock(codeBlock: CodeBlock): String {
        val sb = StringBuilder()
        if (codeBlock.language != null) {
            sb.append("[source,").append(codeBlock.language).append("]\n")
        }
        sb.append("----\n")
        sb.append(codeBlock.content)
        if (codeBlock.content.isNotEmpty() && !codeBlock.content.endsWith("\n")) {
            sb.append("\n")
        }
        sb.append("----")
        return sb.toString()
    }

    private fun renderComment(comment: Comment): String {
        return if (comment.content.contains("\n")) {
            "////\n${comment.content}\n////"
        } else {
            "// ${comment.content}"
        }
    }

    private fun renderIncludeDirective(include: IncludeDirective): String {
        val sb = StringBuilder()
        sb.append("include::").append(include.path).append("[")
        // Attributes or line ranges could go here if needed, but for concatenation they are resolved
        sb.append("]")
        return sb.toString()
    }

    private fun renderInline(inline: InlineElement): String {
        return when (inline) {
            is Text -> inline.content
            is Strong -> "**${inline.content.joinToString("") { renderInline(it) }}**"
            is Emphasis -> "__${inline.content.joinToString("") { renderInline(it) }}__"
            is Code -> "`${inline.content}`"
            is Link -> "link:${inline.url}[${inline.text}]"
            is Image -> "image:${inline.path}[${inline.altText}]"
            is AttributeReference -> "{${inline.key}}"
            is Callout -> "<${inline.number}>"
            is CrossReference -> {
                val text = if (inline.customText != null) ",${inline.customText}" else ""
                "<<${inline.targetId}$text>>"
            }
            is MacroInvocation -> {
                val params = inline.parameters.entries.joinToString(",") { "${it.key}=${it.value}" }
                "${inline.macroName}::${params}[]"
            }
        }
    }
}
