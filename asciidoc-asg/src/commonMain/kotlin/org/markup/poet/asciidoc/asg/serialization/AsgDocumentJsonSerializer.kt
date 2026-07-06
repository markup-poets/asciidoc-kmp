package org.markup.poet.asciidoc.asg.serialization

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.Block
import org.markup.poet.asciidoc.asg.Inline
import org.markup.poet.asciidoc.asg.InlineMacro
import org.markup.poet.asciidoc.asg.InlineRef
import org.markup.poet.asciidoc.asg.InlineSpan
import org.markup.poet.asciidoc.asg.InlineText
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.LeafBlockForm
import org.markup.poet.asciidoc.asg.ListBlock
import org.markup.poet.asciidoc.asg.ListItem
import org.markup.poet.asciidoc.asg.Location
import org.markup.poet.asciidoc.asg.ParentBlock
import org.markup.poet.asciidoc.asg.SectionBlock

/**
 * Near-identity mapping from the ASG model to the official ASG JSON
 * (asciidoc-lang `asg/schema.json`). Unknown constructs fail loudly rather
 * than emitting placeholder nodes.
 */
class AsgDocumentJsonSerializer(
    private val emitLocations: Boolean = true,
) {
    private val json = Json { prettyPrint = false }

    fun serializeDocument(document: AsgDocument): String =
        json.encodeToString(JsonElement.serializer(), documentToJson(document))

    fun serializeInlines(inlines: List<Inline>): String =
        json.encodeToString(
            JsonElement.serializer(),
            buildJsonArray { inlines.forEach { add(inlineToJson(it)) } },
        )

    private fun documentToJson(document: AsgDocument): JsonObject = buildJsonObject {
        put("name", "document")
        put("type", "block")
        val header = document.header
        if (header != null) {
            // The schema requires the (possibly empty) attributes map when a header exists.
            put("attributes", buildJsonObject {
                document.attributes.forEach { (key, value) -> put(key, value) }
            })
            put("header", buildJsonObject {
                putJsonArray("title") { header.title.forEach { add(inlineToJson(it)) } }
                addLocation(header.location)
            })
        }
        if (document.blocks.isNotEmpty()) {
            putJsonArray("blocks") { document.blocks.forEach { add(blockToJson(it)) } }
        }
        addLocation(document.location)
    }

    private fun blockToJson(block: Block): JsonObject = when (block) {
        is LeafBlock -> buildJsonObject {
            put("name", block.name.asgName)
            put("type", "block")
            if (block.form != LeafBlockForm.PARAGRAPH) {
                put("form", block.form.asgName)
            }
            block.delimiter?.let { put("delimiter", it) }
            putJsonArray("inlines") { block.inlines.forEach { add(inlineToJson(it)) } }
            addLocation(block.location)
        }
        is ParentBlock -> buildJsonObject {
            put("name", block.name.asgName)
            put("type", "block")
            block.variant?.let { put("variant", it) }
            put("form", "delimited")
            put("delimiter", block.delimiter)
            if (block.blocks.isNotEmpty()) {
                putJsonArray("blocks") { block.blocks.forEach { add(blockToJson(it)) } }
            }
            addLocation(block.location)
        }
        is SectionBlock -> buildJsonObject {
            put("name", "section")
            put("type", "block")
            putJsonArray("title") { block.title.forEach { add(inlineToJson(it)) } }
            put("level", block.level)
            if (block.blocks.isNotEmpty()) {
                putJsonArray("blocks") { block.blocks.forEach { add(blockToJson(it)) } }
            }
            addLocation(block.location)
        }
        is ListBlock -> buildJsonObject {
            put("name", "list")
            put("type", "block")
            put("variant", block.variant.asgName)
            put("marker", block.marker)
            putJsonArray("items") { block.items.forEach { add(listItemToJson(it)) } }
            addLocation(block.location)
        }
    }

    private fun listItemToJson(item: ListItem): JsonObject = buildJsonObject {
        put("name", "listItem")
        put("type", "block")
        put("marker", item.marker)
        putJsonArray("principal") { item.principal.forEach { add(inlineToJson(it)) } }
        if (item.blocks.isNotEmpty()) {
            putJsonArray("blocks") { item.blocks.forEach { add(blockToJson(it)) } }
        }
        addLocation(item.location)
    }

    private fun inlineToJson(inline: Inline): JsonObject = when (inline) {
        is InlineText -> buildJsonObject {
            put("name", "text")
            put("type", "string")
            put("value", inline.value)
            addLocation(inline.location)
        }
        is InlineSpan -> buildJsonObject {
            put("name", "span")
            put("type", "inline")
            put("variant", inline.variant.asgName)
            put("form", inline.form.asgName)
            putJsonArray("inlines") { inline.inlines.forEach { add(inlineToJson(it)) } }
            addLocation(inline.location)
        }
        is InlineRef -> buildJsonObject {
            put("name", "ref")
            put("type", "inline")
            put("variant", inline.variant.asgName)
            put("target", inline.target)
            putJsonArray("inlines") { inline.inlines.forEach { add(inlineToJson(it)) } }
            addLocation(inline.location)
        }
        // Extension seam only — the official ASG schema has no generic macro node.
        is InlineMacro -> error(
            "Inline macro '${inline.name}:${inline.target}[…]' has no official ASG serialization; " +
                "macros must be expanded by an extension before serializing",
        )
    }

    private fun JsonObjectBuilder.addLocation(location: Location?) {
        if (!emitLocations || location == null) return
        putJsonArray("location") {
            add(buildJsonObject {
                put("line", location.start.line)
                put("col", location.start.col)
            })
            add(buildJsonObject {
                put("line", location.end.line)
                put("col", location.end.col)
            })
        }
    }
}
