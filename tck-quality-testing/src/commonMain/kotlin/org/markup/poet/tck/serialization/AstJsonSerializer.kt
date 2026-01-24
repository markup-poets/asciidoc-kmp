package org.markup.poet.tck.serialization

import kotlinx.serialization.json.*
import org.markup.poet.asciidoc.ast.*

/**
 * Serializes AsciiDoc AST to JSON format matching the official TCK schema.
 * 
 * The official TCK has two output formats:
 * 1. **Inline tests** - Just an array of inline elements
 * 2. **Block tests** - Full document structure
 * 
 * See: tck-quality-testing/docs/official-tck-format.md
 */
class AstJsonSerializer {
    
    /**
     * Serialization mode for different test types.
     */
    enum class Mode {
        /** Full document structure (for block tests) */
        FULL_DOCUMENT,
        /** Just inline content array (for inline tests) */
        INLINE_ONLY
    }
    
    /**
     * Serialize a Document AST to JSON string.
     * 
     * @param document The document to serialize
     * @param mode Serialization mode (default: FULL_DOCUMENT)
     */
    fun serialize(document: Document, mode: Mode = Mode.FULL_DOCUMENT): String {
        val jsonElement = when (mode) {
            Mode.FULL_DOCUMENT -> serializeDocument(document)
            Mode.INLINE_ONLY -> serializeInlineOnly(document)
        }
        
        return Json {
            prettyPrint = true
            prettyPrintIndent = "  "
        }.encodeToString(JsonElement.serializer(), jsonElement)
    }
    
    /**
     * Serialize just the inline content (for inline tests).
     * 
     * Extracts inline elements from the first paragraph and returns them as an array.
     */
    private fun serializeInlineOnly(document: Document): JsonArray {
        // For inline tests, extract the inline content from the first paragraph
        val firstChild = document.children.firstOrNull()
        
        return when (firstChild) {
            is Paragraph -> {
                // Return array of inline elements
                buildJsonArray {
                    firstChild.content.forEach { inline ->
                        add(serializeInlineElement(inline))
                    }
                }
            }
            else -> {
                // Fallback: empty array
                buildJsonArray { }
            }
        }
    }
    
    /**
     * Serialize Document node to JSON.
     */
    private fun serializeDocument(document: Document): JsonObject {
        return buildJsonObject {
            put("name", "document")
            put("type", "block")
            
            // Serialize child blocks
            if (document.children.isNotEmpty()) {
                putJsonArray("blocks") {
                    document.children.forEach { child ->
                        add(serializeBlockElement(child))
                    }
                }
            }
            
            // Add location if available
            document.sourceLocation.let { loc ->
                putJsonArray("location") {
                    add(buildJsonObject {
                        put("line", loc.line)
                        put("col", loc.column)
                    })
                    add(buildJsonObject {
                        put("line", loc.line)
                        put("col", loc.column)
                    })
                }
            }
        }
    }
    
    /**
     * Serialize a BlockElement to JSON.
     */
    private fun serializeBlockElement(element: BlockElement): JsonObject {
        return when (element) {
            is Document -> serializeDocument(element)
            is Section -> serializeSection(element)
            is Paragraph -> serializeParagraph(element)
            is AsciiDocList -> serializeList(element)
            is CodeBlock -> serializeCodeBlock(element)
            is ListItem -> serializeListItem(element)
            is AdmonitionBlock -> serializeAdmonitionBlock(element)
            is Comment -> serializeComment(element)
            else -> buildJsonObject {
                put("name", "unknown")
                put("type", "block")
            }
        }
    }
    
    /**
     * Serialize Section (heading) to JSON.
     */
    private fun serializeSection(section: Section): JsonObject {
        return buildJsonObject {
            put("name", "section")
            put("type", "block")
            put("level", section.level)
            
            // Section title as inline content
            if (section.title.isNotEmpty()) {
                putJsonArray("inlines") {
                    add(buildJsonObject {
                        put("name", "text")
                        put("type", "string")
                        put("value", section.title)
                    })
                }
            }
            
            // Child blocks
            if (section.children.isNotEmpty()) {
                putJsonArray("blocks") {
                    section.children.forEach { child ->
                        add(serializeBlockElement(child))
                    }
                }
            }
            
            addLocation(section.sourceLocation)
        }
    }
    
    /**
     * Serialize Paragraph to JSON.
     */
    private fun serializeParagraph(paragraph: Paragraph): JsonObject {
        return buildJsonObject {
            put("name", "paragraph")
            put("type", "block")
            
            // Serialize inline content
            putJsonArray("inlines") {
                paragraph.content.forEach { inline ->
                    add(serializeInlineElement(inline))
                }
            }
            
            addLocation(paragraph.sourceLocation)
        }
    }
    
    /**
     * Serialize AsciiDocList to JSON.
     */
    private fun serializeList(list: AsciiDocList): JsonObject {
        return buildJsonObject {
            put("name", "list")
            put("type", "block")
            put("variant", when (list.type) {
                ListType.UNORDERED -> "unordered"
                ListType.ORDERED -> "ordered"
                ListType.DEFINITION -> "definition"
            })
            
            putJsonArray("blocks") {
                list.items.forEach { item ->
                    add(serializeListItem(item))
                }
            }
            
            addLocation(list.sourceLocation)
        }
    }
    
    /**
     * Serialize ListItem to JSON.
     */
    private fun serializeListItem(item: ListItem): JsonObject {
        return buildJsonObject {
            put("name", "list_item")
            put("type", "block")
            
            // Item content as inlines
            putJsonArray("inlines") {
                item.content.forEach { inline ->
                    add(serializeInlineElement(inline))
                }
            }
            
            // Nested list
            val nested = item.nestedList
            if (nested != null) {
                putJsonArray("blocks") {
                    add(serializeList(nested))
                }
            }
            
            addLocation(item.sourceLocation)
        }
    }
    
    /**
     * Serialize AdmonitionBlock to JSON.
     */
    private fun serializeAdmonitionBlock(admonition: AdmonitionBlock): JsonObject {
        return buildJsonObject {
            put("name", "admonition")
            put("type", "block")
            put("variant", admonition.type.name.lowercase())
            
            if (admonition.title != null) {
                put("title", admonition.title)
            }
            
            putJsonArray("blocks") {
                admonition.content.forEach { block ->
                    add(serializeBlockElement(block))
                }
            }
            
            addLocation(admonition.sourceLocation)
        }
    }
    
    /**
     * Serialize Comment to JSON.
     */
    private fun serializeComment(comment: Comment): JsonObject {
        return buildJsonObject {
            put("name", "comment")
            put("type", "block")
            put("value", comment.content)
            
            addLocation(comment.sourceLocation)
        }
    }
    
    /**
     * Serialize CodeBlock to JSON.
     */
    private fun serializeCodeBlock(codeBlock: CodeBlock): JsonObject {
        return buildJsonObject {
            put("name", "listing")
            put("type", "block")
            
            if (codeBlock.language != null) {
                put("language", codeBlock.language)
            }
            
            // Code content as text
            putJsonArray("inlines") {
                add(buildJsonObject {
                    put("name", "text")
                    put("type", "string")
                    put("value", codeBlock.content)
                })
            }
            
            addLocation(codeBlock.sourceLocation)
        }
    }
    
    /**
     * Serialize an InlineElement to JSON.
     */
    private fun serializeInlineElement(element: InlineElement): JsonObject {
        return when (element) {
            is Text -> serializeText(element)
            is Strong -> serializeStrong(element)
            is Emphasis -> serializeEmphasis(element)
            is Code -> serializeCode(element)
            is Link -> serializeLink(element)
            is Image -> serializeImage(element)
            else -> buildJsonObject {
                put("name", "text")
                put("type", "string")
                put("value", "")
            }
        }
    }
    
    /**
     * Serialize Text node to JSON.
     */
    private fun serializeText(text: Text): JsonObject {
        return buildJsonObject {
            put("name", "text")
            put("type", "string")
            put("value", text.content)
            addLocation(text.sourceLocation)
        }
    }
    
    /**
     * Serialize Strong (bold) to JSON.
     */
    private fun serializeStrong(strong: Strong): JsonObject {
        return buildJsonObject {
            put("name", "span")
            put("type", "inline")
            put("variant", "strong")
            put("form", "constrained")
            
            putJsonArray("inlines") {
                strong.content.forEach { inline ->
                    add(serializeInlineElement(inline))
                }
            }
            
            addLocation(strong.sourceLocation)
        }
    }
    
    /**
     * Serialize Emphasis (italic) to JSON.
     */
    private fun serializeEmphasis(emphasis: Emphasis): JsonObject {
        return buildJsonObject {
            put("name", "span")
            put("type", "inline")
            put("variant", "emphasis")
            put("form", "constrained")
            
            putJsonArray("inlines") {
                emphasis.content.forEach { inline ->
                    add(serializeInlineElement(inline))
                }
            }
            
            addLocation(emphasis.sourceLocation)
        }
    }
    
    /**
     * Serialize Code (monospace) to JSON.
     */
    private fun serializeCode(code: Code): JsonObject {
        return buildJsonObject {
            put("name", "span")
            put("type", "inline")
            put("variant", "monospace")
            put("form", "constrained")
            
            putJsonArray("inlines") {
                add(buildJsonObject {
                    put("name", "text")
                    put("type", "string")
                    put("value", code.content)
                })
            }
            
            addLocation(code.sourceLocation)
        }
    }
    
    /**
     * Serialize Link to JSON.
     */
    private fun serializeLink(link: Link): JsonObject {
        return buildJsonObject {
            put("name", "link")
            put("type", "inline")
            put("url", link.url)
            
            putJsonArray("inlines") {
                add(buildJsonObject {
                    put("name", "text")
                    put("type", "string")
                    put("value", link.text)
                })
            }
            
            addLocation(link.sourceLocation)
        }
    }
    
    /**
     * Serialize Image to JSON.
     */
    private fun serializeImage(image: Image): JsonObject {
        return buildJsonObject {
            put("name", "image")
            put("type", "inline")
            put("path", image.path)
            put("alt", image.altText)
            
            addLocation(image.sourceLocation)
        }
    }
    
    /**
     * Add location information to a JSON object builder.
     * Outputs an array with start and end positions: [{line, col}, {line, col}]
     */
    private fun JsonObjectBuilder.addLocation(location: SourceLocation) {
        putJsonArray("location") {
            // Start position
            add(buildJsonObject {
                put("line", location.line)
                put("col", location.column)
            })
            // End position
            add(buildJsonObject {
                put("line", location.endLine)
                put("col", location.endColumn)
            })
        }
    }
}
