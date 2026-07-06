package org.markup.poet.asciidoc.asg.serialization

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.markup.poet.asciidoc.ast.AsciiDocList
import org.markup.poet.asciidoc.ast.BlockElement
import org.markup.poet.asciidoc.ast.Code
import org.markup.poet.asciidoc.ast.CodeBlock
import org.markup.poet.asciidoc.ast.Comment
import org.markup.poet.asciidoc.ast.Document
import org.markup.poet.asciidoc.ast.Emphasis
import org.markup.poet.asciidoc.ast.InlineElement
import org.markup.poet.asciidoc.ast.Link
import org.markup.poet.asciidoc.ast.ListItem
import org.markup.poet.asciidoc.ast.ListType
import org.markup.poet.asciidoc.ast.Paragraph
import org.markup.poet.asciidoc.ast.Section
import org.markup.poet.asciidoc.ast.SourceLocation
import org.markup.poet.asciidoc.ast.Strong
import org.markup.poet.asciidoc.ast.Text

/**
 * Serializes the parsed document into the official AsciiDoc ASG JSON format
 * (see asciidoc-lang `asg/schema.json` and the TCK's `tests/` `-output.json` fixtures).
 *
 * The official TCK has two output formats:
 * - inline tests: a bare JSON array of inline nodes
 * - block tests: the full document object
 *
 * Locations in the ASG are 1-based with end-inclusive columns. The TCK harness
 * skips location comparison entirely when the output contains no `location` key,
 * so location emission is opt-in until position tracking is exact.
 */
class AsgJsonSerializer(
    private val emitLocations: Boolean = false,
) {

    enum class Mode {
        /** Full document structure (block tests). */
        FULL_DOCUMENT,

        /** Bare array of the first paragraph's inline nodes (inline tests). */
        INLINE_ONLY,
    }

    private val json = Json {
        prettyPrint = false
    }

    fun serialize(document: Document, mode: Mode = Mode.FULL_DOCUMENT): String {
        val element: JsonElement = when (mode) {
            Mode.FULL_DOCUMENT -> serializeDocument(document)
            Mode.INLINE_ONLY -> serializeInlineOnly(document)
        }
        return json.encodeToString(JsonElement.serializer(), element)
    }

    private fun serializeInlineOnly(document: Document): JsonArray {
        val firstParagraph = document.children.filterIsInstance<Paragraph>().firstOrNull()
        return buildJsonArray {
            firstParagraph?.content?.forEach { add(serializeInline(it)) }
        }
    }

    private fun serializeDocument(document: Document): JsonObject {
        val hasHeader = document.title != null
        return buildJsonObject {
            put("name", "document")
            put("type", "block")
            if (hasHeader) {
                // The header requires the (possibly empty) resolved attributes map.
                put("attributes", buildJsonObject {
                    document.documentAttributes.forEach { (key, value) -> put(key, value) }
                })
                put("header", buildJsonObject {
                    putJsonArray("title") {
                        add(buildJsonObject {
                            put("name", "text")
                            put("type", "string")
                            put("value", document.title!!)
                        })
                    }
                })
            }
            if (document.children.isNotEmpty()) {
                putJsonArray("blocks") {
                    document.children.forEach { add(serializeBlock(it)) }
                }
            }
        }
    }

    private fun serializeBlock(element: BlockElement): JsonObject = when (element) {
        is Document -> serializeDocument(element)
        is Section -> serializeSection(element)
        is Paragraph -> serializeParagraph(element)
        is AsciiDocList -> serializeList(element)
        is CodeBlock -> serializeListing(element)
        is ListItem -> serializeListItem(element)
        is Comment -> serializeComment(element)
        else -> error("ASG serialization not implemented for ${element::class.simpleName}")
    }

    private fun serializeSection(section: Section): JsonObject = buildJsonObject {
        put("name", "section")
        put("type", "block")
        putJsonArray("title") {
            add(buildJsonObject {
                put("name", "text")
                put("type", "string")
                put("value", section.title)
            })
        }
        put("level", section.level)
        if (section.children.isNotEmpty()) {
            putJsonArray("blocks") {
                section.children.forEach { add(serializeBlock(it)) }
            }
        }
        addLocation(section.sourceLocation)
    }

    private fun serializeParagraph(paragraph: Paragraph): JsonObject = buildJsonObject {
        put("name", "paragraph")
        put("type", "block")
        putJsonArray("inlines") {
            paragraph.content.forEach { add(serializeInline(it)) }
        }
        addLocation(paragraph.sourceLocation)
    }

    private fun serializeList(list: AsciiDocList): JsonObject = buildJsonObject {
        put("name", "list")
        put("type", "block")
        put("variant", when (list.type) {
            ListType.UNORDERED -> "unordered"
            ListType.ORDERED -> "ordered"
            ListType.DEFINITION -> "definition"
        })
        list.items.firstOrNull()?.let { put("marker", it.marker) }
        putJsonArray("items") {
            list.items.forEach { add(serializeListItem(it)) }
        }
        addLocation(list.sourceLocation)
    }

    private fun serializeListItem(item: ListItem): JsonObject = buildJsonObject {
        put("name", "listItem")
        put("type", "block")
        put("marker", item.marker)
        putJsonArray("principal") {
            item.content.forEach { add(serializeInline(it)) }
        }
        item.nestedList?.let { nested ->
            putJsonArray("blocks") { add(serializeList(nested)) }
        }
        addLocation(item.sourceLocation)
    }

    private fun serializeListing(codeBlock: CodeBlock): JsonObject = buildJsonObject {
        put("name", "listing")
        put("type", "block")
        put("form", "delimited")
        put("delimiter", "----")
        putJsonArray("inlines") {
            add(buildJsonObject {
                put("name", "text")
                put("type", "string")
                put("value", codeBlock.content)
            })
        }
        addLocation(codeBlock.sourceLocation)
    }

    private fun serializeComment(comment: Comment): JsonObject = buildJsonObject {
        put("name", "comment")
        put("type", "block")
        put("value", comment.content)
        addLocation(comment.sourceLocation)
    }

    private fun serializeInline(element: InlineElement): JsonObject = when (element) {
        is Text -> buildJsonObject {
            put("name", "text")
            put("type", "string")
            put("value", element.content)
            addLocation(element.sourceLocation)
        }
        is Strong -> serializeSpan("strong", element.content, element.sourceLocation)
        is Emphasis -> serializeSpan("emphasis", element.content, element.sourceLocation)
        is Code -> buildJsonObject {
            put("name", "span")
            put("type", "inline")
            put("variant", "code")
            put("form", "constrained")
            putJsonArray("inlines") {
                add(buildJsonObject {
                    put("name", "text")
                    put("type", "string")
                    put("value", element.content)
                })
            }
            addLocation(element.sourceLocation)
        }
        is Link -> buildJsonObject {
            put("name", "ref")
            put("type", "inline")
            put("variant", "link")
            put("target", element.url)
            putJsonArray("inlines") {
                add(buildJsonObject {
                    put("name", "text")
                    put("type", "string")
                    put("value", element.text)
                })
            }
            addLocation(element.sourceLocation)
        }
        else -> error("ASG serialization not implemented for ${element::class.simpleName}")
    }

    private fun serializeSpan(
        variant: String,
        content: List<InlineElement>,
        location: SourceLocation,
    ): JsonObject = buildJsonObject {
        put("name", "span")
        put("type", "inline")
        put("variant", variant)
        put("form", "constrained")
        putJsonArray("inlines") {
            content.forEach { add(serializeInline(it)) }
        }
        addLocation(location)
    }

    private fun JsonObjectBuilder.addLocation(location: SourceLocation) {
        if (!emitLocations) return
        putJsonArray("location") {
            add(buildJsonObject {
                put("line", location.line)
                put("col", location.column)
            })
            add(buildJsonObject {
                put("line", location.endLine)
                put("col", location.endColumn)
            })
        }
    }
}
