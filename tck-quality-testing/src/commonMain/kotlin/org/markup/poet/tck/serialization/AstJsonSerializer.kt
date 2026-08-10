package org.markup.poet.tck.serialization

import kotlinx.serialization.json.*
import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.BibliographyEntryBlock
import org.markup.poet.asciidoc.asg.Block
import org.markup.poet.asciidoc.asg.BlockMacro
import org.markup.poet.asciidoc.asg.BlockMacroName
import org.markup.poet.asciidoc.asg.BreakBlock
import org.markup.poet.asciidoc.asg.CommentBlock
import org.markup.poet.asciidoc.asg.ConditionalBlock
import org.markup.poet.asciidoc.asg.CustomBlockMacro
import org.markup.poet.asciidoc.asg.DListBlock
import org.markup.poet.asciidoc.asg.DiscreteHeading
import org.markup.poet.asciidoc.asg.IncludeBlock
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
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.LeafBlockName
import org.markup.poet.asciidoc.asg.ListBlock
import org.markup.poet.asciidoc.asg.ListVariant
import org.markup.poet.asciidoc.asg.Location
import org.markup.poet.asciidoc.asg.ParentBlock
import org.markup.poet.asciidoc.asg.ParentBlockName
import org.markup.poet.asciidoc.asg.RawBlock
import org.markup.poet.asciidoc.asg.RefVariant
import org.markup.poet.asciidoc.asg.SectionBlock
import org.markup.poet.asciidoc.asg.TableBlock
import org.markup.poet.asciidoc.asg.SpanVariant
import org.markup.poet.asciidoc.asg.builtInBlockStyles
import org.markup.poet.asciidoc.asg.plainText

/**
 * Serializes the AsciiDoc ASG to JSON format matching the official TCK schema.
 *
 * The official TCK has two output formats:
 * 1. **Inline tests** - Just an array of inline elements
 * 2. **Block tests** - Full document structure
 *
 * The output is kept byte-identical to the historical legacy-AST serializer
 * (removed in the ASG migration): each ASG node is emitted exactly as its
 * legacy counterpart used to be. In particular, ASG constructs with no legacy
 * equivalent (breaks, includes, conditionals, raw blocks, ...) still serialize
 * as `{"name": "unknown"}` / empty-text placeholders, and parent containers
 * other than admonitions are spliced into their parent.
 *
 * See: docs/modules/ROOT/pages/reference/official-tck-fixture-format.adoc
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
     * Serialize an ASG document to JSON string.
     *
     * @param document The document to serialize
     * @param mode Serialization mode (default: FULL_DOCUMENT)
     */
    fun serialize(document: AsgDocument, mode: Mode = Mode.FULL_DOCUMENT): String {
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
     * Extracts the inline content of the first (legacy-view) paragraph and
     * returns it as an array.
     */
    private fun serializeInlineOnly(document: AsgDocument): JsonArray {
        val firstChild = document.blocks.asSequence()
            .flatMap { serializeBlock(it) }
            .firstOrNull()

        return if (firstChild != null &&
            firstChild["name"]?.jsonPrimitive?.content == "paragraph"
        ) {
            firstChild["inlines"]?.jsonArray ?: buildJsonArray { }
        } else {
            // Fallback: empty array
            buildJsonArray { }
        }
    }

    /**
     * Serialize the document node to JSON.
     */
    private fun serializeDocument(document: AsgDocument): JsonObject {
        val blocks = document.blocks.flatMap { serializeBlock(it) }
        return buildJsonObject {
            put("name", "document")
            put("type", "block")

            // Serialize child blocks
            if (blocks.isNotEmpty()) {
                putJsonArray("blocks") {
                    blocks.forEach { add(it) }
                }
            }

            // Document location historically emitted the start position twice.
            val start = document.location?.start
            putJsonArray("location") {
                add(buildJsonObject {
                    put("line", start?.line ?: 1)
                    put("col", start?.col ?: 1)
                })
                add(buildJsonObject {
                    put("line", start?.line ?: 1)
                    put("col", start?.col ?: 1)
                })
            }
        }
    }

    /**
     * Serialize a block to zero or more JSON objects. Containers without a
     * legacy equivalent (sidebar/example/quote/open) are spliced, i.e. they
     * contribute their children in their own position.
     */
    private fun serializeBlock(block: Block): List<JsonObject> = when (block) {
        is SectionBlock -> listOf(
            serializeSection(
                level = block.level + 1, // legacy level == number of '=' chars
                title = plainText(block.title),
                blocks = block.blocks.flatMap { serializeBlock(it) },
                location = block.location,
            )
        )

        is LeafBlock -> {
            val style = block.metadata?.positional?.firstOrNull()
            when {
                // A non-built-in style claims the block for extension processors
                // (legacy CustomBlock), which the serializer never handled.
                style != null && style !in builtInBlockStyles -> listOf(unknownBlock())
                block.name == LeafBlockName.PARAGRAPH -> listOf(
                    serializeParagraph(block.inlines.flatMap { serializeInlineElement(it) }, block.location)
                )
                // Listing/literal/pass/stem/verse all map to the legacy listing.
                else -> listOf(
                    serializeCodeBlock(
                        language = if (style == "source") {
                            block.metadata?.positional?.getOrNull(1) ?: block.metadata?.named?.get("language")
                        } else {
                            null
                        },
                        content = plainText(block.inlines),
                        location = block.location,
                    )
                )
            }
        }

        is ParentBlock -> if (block.name == ParentBlockName.ADMONITION) {
            listOf(
                serializeAdmonitionBlock(
                    variant = (block.variant ?: "note").lowercase(),
                    title = block.metadata?.title?.let { plainText(it) },
                    blocks = block.blocks.flatMap { serializeBlock(it) },
                    location = block.location,
                )
            )
        } else {
            // No legacy container equivalent: splice children into the parent.
            block.blocks.flatMap { serializeBlock(it) }
        }

        is ListBlock -> listOf(serializeList(block))

        is DListBlock -> listOf(serializeDList(block))

        is BlockMacro -> listOf(
            serializeParagraph(
                content = listOf(
                    if (block.name == BlockMacroName.IMAGE) {
                        serializeImage(
                            path = block.target ?: "",
                            alt = block.metadata?.positional?.firstOrNull() ?: "",
                            location = block.location,
                        )
                    } else {
                        // Legacy MacroInvocation was serialized as an empty text node.
                        emptyTextNode()
                    }
                ),
                location = block.location,
            )
        )

        // Discrete headings render like section titles but open no section.
        is DiscreteHeading -> listOf(
            serializeSection(
                level = block.level + 1,
                title = plainText(block.title),
                blocks = emptyList(),
                location = block.location,
            )
        )

        is CommentBlock -> listOf(serializeComment(block.text, block.location))

        // No legacy serialization existed for these constructs.
        is BreakBlock,
        is IncludeBlock,
        is ConditionalBlock,
        is BibliographyEntryBlock,
        is RawBlock,
        is CustomBlockMacro -> listOf(unknownBlock())

        // The legacy AST predates table support: report an unknown block.
        is TableBlock -> listOf(unknownBlock())
    }

    private fun unknownBlock(): JsonObject = buildJsonObject {
        put("name", "unknown")
        put("type", "block")
    }

    /**
     * Serialize a section (heading) to JSON.
     */
    private fun serializeSection(
        level: Int,
        title: String,
        blocks: List<JsonObject>,
        location: Location?,
    ): JsonObject {
        return buildJsonObject {
            put("name", "section")
            put("type", "block")
            put("level", level)

            // Section title as inline content
            if (title.isNotEmpty()) {
                putJsonArray("inlines") {
                    add(buildJsonObject {
                        put("name", "text")
                        put("type", "string")
                        put("value", title)
                    })
                }
            }

            // Child blocks
            if (blocks.isNotEmpty()) {
                putJsonArray("blocks") {
                    blocks.forEach { add(it) }
                }
            }

            addLocation(location)
        }
    }

    /**
     * Serialize a paragraph to JSON.
     */
    private fun serializeParagraph(content: List<JsonObject>, location: Location?): JsonObject {
        return buildJsonObject {
            put("name", "paragraph")
            put("type", "block")

            // Serialize inline content
            putJsonArray("inlines") {
                content.forEach { add(it) }
            }

            addLocation(location)
        }
    }

    /**
     * Serialize a list to JSON.
     */
    private fun serializeList(list: ListBlock): JsonObject {
        return buildJsonObject {
            put("name", "list")
            put("type", "block")
            put("variant", if (list.variant == ListVariant.ORDERED) "ordered" else "unordered")

            putJsonArray("blocks") {
                list.items.forEach { item ->
                    add(serializeListItem(
                        content = item.principal.flatMap { serializeInlineElement(it) },
                        nested = item.blocks.filterIsInstance<ListBlock>().firstOrNull(),
                        location = item.location,
                    ))
                }
            }

            addLocation(list.location)
        }
    }

    /**
     * Serialize a description list to JSON. Legacy definition lists were never
     * fully modeled: terms and principal are joined into the item content with
     * a `": "` separator.
     */
    private fun serializeDList(list: DListBlock): JsonObject {
        return buildJsonObject {
            put("name", "list")
            put("type", "block")
            put("variant", "definition")

            putJsonArray("blocks") {
                list.items.forEach { item ->
                    val content = mutableListOf<JsonObject>()
                    item.terms.forEach { term ->
                        content += term.flatMap { serializeInlineElement(it) }
                    }
                    if (item.principal.isNotEmpty()) {
                        content += serializeText(": ", item.location)
                        content += item.principal.flatMap { serializeInlineElement(it) }
                    }
                    add(serializeListItem(content, nested = null, location = item.location))
                }
            }

            addLocation(list.location)
        }
    }

    /**
     * Serialize a list item to JSON.
     */
    private fun serializeListItem(
        content: List<JsonObject>,
        nested: ListBlock?,
        location: Location?,
    ): JsonObject {
        return buildJsonObject {
            put("name", "list_item")
            put("type", "block")

            // Item content as inlines
            putJsonArray("inlines") {
                content.forEach { add(it) }
            }

            // Nested list
            if (nested != null) {
                putJsonArray("blocks") {
                    add(serializeList(nested))
                }
            }

            addLocation(location)
        }
    }

    /**
     * Serialize an admonition block to JSON.
     */
    private fun serializeAdmonitionBlock(
        variant: String,
        title: String?,
        blocks: List<JsonObject>,
        location: Location?,
    ): JsonObject {
        return buildJsonObject {
            put("name", "admonition")
            put("type", "block")
            put("variant", variant)

            if (title != null) {
                put("title", title)
            }

            putJsonArray("blocks") {
                blocks.forEach { add(it) }
            }

            addLocation(location)
        }
    }

    /**
     * Serialize a comment to JSON.
     */
    private fun serializeComment(content: String, location: Location?): JsonObject {
        return buildJsonObject {
            put("name", "comment")
            put("type", "block")
            put("value", content)

            addLocation(location)
        }
    }

    /**
     * Serialize a verbatim (listing) block to JSON.
     */
    private fun serializeCodeBlock(language: String?, content: String, location: Location?): JsonObject {
        return buildJsonObject {
            put("name", "listing")
            put("type", "block")

            if (language != null) {
                put("language", language)
            }

            // Code content as text
            putJsonArray("inlines") {
                add(buildJsonObject {
                    put("name", "text")
                    put("type", "string")
                    put("value", content)
                })
            }

            addLocation(location)
        }
    }

    /**
     * Serialize an inline element to zero or more JSON objects (mark spans are
     * spliced into their parent, mirroring the legacy mapping).
     */
    private fun serializeInlineElement(element: Inline): List<JsonObject> = when (element) {
        is InlineText -> listOf(serializeText(element.value, element.location))

        is InlineSpan -> when (element.variant) {
            SpanVariant.STRONG -> listOf(
                serializeSpan("strong", element.inlines.flatMap { serializeInlineElement(it) }, element.location)
            )
            SpanVariant.EMPHASIS -> listOf(
                serializeSpan("emphasis", element.inlines.flatMap { serializeInlineElement(it) }, element.location)
            )
            SpanVariant.CODE -> listOf(serializeCode(plainText(element.inlines), element.location))
            // Legacy AST had no mark/sub/sup elements: splice the inner inlines.
            SpanVariant.MARK,
            SpanVariant.SUBSCRIPT,
            SpanVariant.SUPERSCRIPT,
            -> element.inlines.flatMap { serializeInlineElement(it) }
        }

        is InlineMacro -> when (element.name) {
            "link" -> listOf(
                serializeLink(
                    url = element.target,
                    text = element.positional.firstOrNull() ?: element.target,
                    location = element.location,
                )
            )
            "image" -> listOf(
                serializeImage(
                    path = element.target,
                    alt = element.positional.firstOrNull() ?: "",
                    location = element.location,
                )
            )
            // xref and generic macros had no legacy serialization.
            else -> listOf(emptyTextNode())
        }

        is InlineRef -> when (element.variant) {
            RefVariant.LINK -> listOf(
                serializeLink(
                    url = element.target,
                    text = plainText(element.inlines),
                    location = element.location,
                )
            )
            RefVariant.XREF -> listOf(emptyTextNode())
        }

        // No legacy serialization existed for these constructs.
        is InlineAttributeRef,
        is InlineCallout,
        is InlineFootnote,
        is InlineCitation,
        is InlineRaw -> listOf(emptyTextNode())
    }

    private fun emptyTextNode(): JsonObject = buildJsonObject {
        put("name", "text")
        put("type", "string")
        put("value", "")
    }

    /**
     * Serialize a text node to JSON.
     */
    private fun serializeText(value: String, location: Location?): JsonObject {
        return buildJsonObject {
            put("name", "text")
            put("type", "string")
            put("value", value)
            addLocation(location)
        }
    }

    /**
     * Serialize a strong/emphasis span to JSON.
     */
    private fun serializeSpan(variant: String, content: List<JsonObject>, location: Location?): JsonObject {
        return buildJsonObject {
            put("name", "span")
            put("type", "inline")
            put("variant", variant)
            put("form", "constrained")

            putJsonArray("inlines") {
                content.forEach { add(it) }
            }

            addLocation(location)
        }
    }

    /**
     * Serialize a code (monospace) span to JSON.
     */
    private fun serializeCode(content: String, location: Location?): JsonObject {
        return buildJsonObject {
            put("name", "span")
            put("type", "inline")
            put("variant", "monospace")
            put("form", "constrained")

            putJsonArray("inlines") {
                add(buildJsonObject {
                    put("name", "text")
                    put("type", "string")
                    put("value", content)
                })
            }

            addLocation(location)
        }
    }

    /**
     * Serialize a link to JSON.
     */
    private fun serializeLink(url: String, text: String, location: Location?): JsonObject {
        return buildJsonObject {
            put("name", "link")
            put("type", "inline")
            put("url", url)

            putJsonArray("inlines") {
                add(buildJsonObject {
                    put("name", "text")
                    put("type", "string")
                    put("value", text)
                })
            }

            addLocation(location)
        }
    }

    /**
     * Serialize an image to JSON.
     */
    private fun serializeImage(path: String, alt: String, location: Location?): JsonObject {
        return buildJsonObject {
            put("name", "image")
            put("type", "inline")
            put("path", path)
            put("alt", alt)

            addLocation(location)
        }
    }

    /**
     * Add location information to a JSON object builder.
     * Outputs an array with start and end positions: [{line, col}, {line, col}]
     * A missing ASG location serializes as the legacy default (1,1)-(1,1).
     */
    private fun JsonObjectBuilder.addLocation(location: Location?) {
        putJsonArray("location") {
            // Start position
            add(buildJsonObject {
                put("line", location?.start?.line ?: 1)
                put("col", location?.start?.col ?: 1)
            })
            // End position
            add(buildJsonObject {
                put("line", location?.end?.line ?: 1)
                put("col", location?.end?.col ?: 1)
            })
        }
    }
}
