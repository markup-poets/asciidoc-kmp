package org.markup.poet.cli

import org.markup.poet.asciidoc.ast.*

/**
 * Converts an AsciiDoc AST back to AsciiDoc text format.
 * 
 * This pretty printer traverses the AST and generates valid AsciiDoc markup,
 * preserving the document structure and formatting.
 */
class AsciiDocPrettyPrinter {
    
    /**
     * Convert a document to AsciiDoc text.
     */
    fun print(document: Document): String {
        val builder = StringBuilder()
        
        // Print document title if present
        val title = document.title
        if (title != null && title.isNotEmpty()) {
            builder.appendLine("= $title")
            builder.appendLine()
        }
        
        // Print document attributes
        document.documentAttributes.forEach { (key, value) ->
            builder.appendLine(":$key: $value")
        }
        if (document.documentAttributes.isNotEmpty()) {
            builder.appendLine()
        }
        
        // Print document body
        document.children.forEachIndexed { index, child ->
            printBlockElement(child, builder)
            // Add blank line between top-level blocks (except after last one)
            if (index < document.children.size - 1) {
                builder.appendLine()
            }
        }
        
        return builder.toString()
    }
    
    /**
     * Print a block element to the builder.
     */
    private fun printBlockElement(element: BlockElement, builder: StringBuilder, indent: String = "") {
        when (element) {
            is Section -> printSection(element, builder, indent)
            is Paragraph -> printParagraph(element, builder, indent)
            is AsciiDocList -> printList(element, builder, indent)
            is CodeBlock -> printCodeBlock(element, builder, indent)
            is Comment -> printComment(element, builder, indent)
            is CalloutList -> printCalloutList(element, builder, indent)
            is IncludeDirective -> printIncludeDirective(element, builder, indent)
            is ListItem -> {} // ListItems are handled within printList
            is CalloutListItem -> {} // CalloutListItems are handled within printCalloutList
            is Document -> {} // Document is handled separately in print()
        }
    }
    
    /**
     * Print a section with its heading and children.
     */
    private fun printSection(section: Section, builder: StringBuilder, indent: String) {
        // Print section heading
        val marker = "=".repeat(section.level + 1)
        builder.appendLine("$indent$marker ${section.title}")
        builder.appendLine()
        
        // Print section children
        section.children.forEachIndexed { index, child ->
            printBlockElement(child, builder, indent)
            // Add blank line between blocks (except after last one)
            if (index < section.children.size - 1) {
                builder.appendLine()
            }
        }
    }
    
    /**
     * Print a paragraph with its inline content.
     */
    private fun printParagraph(paragraph: Paragraph, builder: StringBuilder, indent: String) {
        val content = paragraph.content.joinToString("") { printInlineElement(it) }
        builder.appendLine("$indent$content")
    }
    
    /**
     * Print a list with its items.
     */
    private fun printList(list: AsciiDocList, builder: StringBuilder, indent: String) {
        list.items.forEach { item ->
            printListItem(item, builder, indent)
        }
    }
    
    /**
     * Print a list item with its content and optional nested list.
     */
    private fun printListItem(item: ListItem, builder: StringBuilder, indent: String) {
        val content = item.content.joinToString("") { printInlineElement(it) }
        builder.appendLine("$indent${item.marker} $content")
        
        // Print nested list if present
        val nestedList = item.nestedList
        if (nestedList != null) {
            printList(nestedList, builder, "$indent  ")
        }
    }
    
    /**
     * Print a code block with optional language specification.
     */
    private fun printCodeBlock(codeBlock: CodeBlock, builder: StringBuilder, indent: String) {
        if (codeBlock.language != null) {
            builder.appendLine("$indent[source,${codeBlock.language}]")
        }
        builder.appendLine("$indent----")
        codeBlock.content.lines().forEach { line ->
            builder.appendLine("$indent$line")
        }
        builder.appendLine("$indent----")
    }
    
    /**
     * Print a comment block.
     */
    private fun printComment(comment: Comment, builder: StringBuilder, indent: String) {
        builder.appendLine("$indent////")
        comment.content.lines().forEach { line ->
            builder.appendLine("$indent$line")
        }
        builder.appendLine("$indent////")
    }
    
    /**
     * Print a callout list with its items.
     */
    private fun printCalloutList(calloutList: CalloutList, builder: StringBuilder, indent: String) {
        calloutList.items.forEach { item ->
            val content = item.content.joinToString("") { printInlineElement(it) }
            builder.appendLine("$indent<${item.number}> $content")
        }
    }
    
    /**
     * Print an include directive.
     * Note: This should not normally appear in processed output since includes are resolved.
     */
    private fun printIncludeDirective(directive: IncludeDirective, builder: StringBuilder, indent: String) {
        val lineRange = directive.lineRange
        val lineRangeStr = if (lineRange != null) {
            ",lines=${lineRange.first}..${lineRange.last}"
        } else {
            ""
        }
        builder.appendLine("${indent}include::${directive.path}[$lineRangeStr]")
    }
    
    /**
     * Print an inline element and return its text representation.
     */
    private fun printInlineElement(element: InlineElement): String {
        return when (element) {
            is Text -> element.content
            is Strong -> {
                val content = element.content.joinToString("") { printInlineElement(it) }
                "*$content*"
            }
            is Emphasis -> {
                val content = element.content.joinToString("") { printInlineElement(it) }
                "_${content}_"
            }
            is Code -> "`${element.content}`"
            is Link -> "link:${element.url}[${element.text}]"
            is Image -> "image:${element.path}[${element.altText}]"
            is AttributeReference -> "{${element.key}}"
            is Callout -> "<${element.number}>"
            is CrossReference -> {
                if (element.customText != null) {
                    "<<${element.targetId},${element.customText}>>"
                } else {
                    "<<${element.targetId}>>"
                }
            }
            is MacroInvocation -> {
                val params = element.parameters.entries.joinToString(",") { "${it.key}=${it.value}" }
                if (element.isBlock) {
                    "${element.macroName}::[$params]"
                } else {
                    "${element.macroName}:[$params]"
                }
            }
        }
    }
}
