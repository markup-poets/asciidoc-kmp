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
import org.markup.poet.asciidoc.asg.BibliographyEntryBlock
import org.markup.poet.asciidoc.asg.Block
import org.markup.poet.asciidoc.asg.BlockMacro
import org.markup.poet.asciidoc.asg.BlockMetadata
import org.markup.poet.asciidoc.asg.BreakBlock
import org.markup.poet.asciidoc.asg.CommentBlock
import org.markup.poet.asciidoc.asg.ConditionalBlock
import org.markup.poet.asciidoc.asg.DListBlock
import org.markup.poet.asciidoc.asg.DListItem
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
import org.markup.poet.asciidoc.asg.LeafBlockForm
import org.markup.poet.asciidoc.asg.ListBlock
import org.markup.poet.asciidoc.asg.ListItem
import org.markup.poet.asciidoc.asg.Location
import org.markup.poet.asciidoc.asg.ParentBlock
import org.markup.poet.asciidoc.asg.RawBlock
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
            addMetadata(block.metadata)
            addLocation(block.location)
        }
        is ParentBlock -> buildJsonObject {
            put("name", block.name.asgName)
            put("type", "block")
            block.variant?.let { put("variant", it) }
            // Paragraph-form admonitions have no delimiter; the official schema
            // only defines the delimited form, so form/delimiter are emitted
            // only when a delimiter exists.
            block.delimiter?.let {
                put("form", "delimited")
                put("delimiter", it)
            }
            if (block.blocks.isNotEmpty()) {
                putJsonArray("blocks") { block.blocks.forEach { add(blockToJson(it)) } }
            }
            addMetadata(block.metadata)
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
            addMetadata(block.metadata)
            addLocation(block.location)
        }
        is DListBlock -> buildJsonObject {
            put("name", "dlist")
            put("type", "block")
            put("marker", block.marker)
            putJsonArray("items") { block.items.forEach { add(dlistItemToJson(it)) } }
            addMetadata(block.metadata)
            addLocation(block.location)
        }
        is BreakBlock -> buildJsonObject {
            put("name", "break")
            put("type", "block")
            put("variant", block.variant.asgName)
            addLocation(block.location)
        }
        is BlockMacro -> buildJsonObject {
            put("name", block.name.asgName)
            put("type", "block")
            put("form", "macro")
            block.target?.let { put("target", it) }
            addMetadata(block.metadata)
            addLocation(block.location)
        }
        is DiscreteHeading -> buildJsonObject {
            put("name", "heading")
            put("type", "block")
            putJsonArray("title") { block.title.forEach { add(inlineToJson(it)) } }
            put("level", block.level)
            addMetadata(block.metadata)
            addLocation(block.location)
        }
        // Processing-phase extension nodes are not part of the official schema;
        // they must be resolved by document-processing before serializing.
        is CommentBlock, is IncludeBlock, is ConditionalBlock, is BibliographyEntryBlock, is RawBlock -> error(
            "${block::class.simpleName} has no official ASG serialization; " +
                "it must be resolved by document-processing before serializing",
        )
    }

    private fun dlistItemToJson(item: DListItem): JsonObject = buildJsonObject {
        put("name", "dlistItem")
        put("type", "block")
        put("marker", item.marker)
        putJsonArray("terms") {
            item.terms.forEach { term ->
                add(buildJsonArray { term.forEach { add(inlineToJson(it)) } })
            }
        }
        if (item.principal.isNotEmpty()) {
            putJsonArray("principal") { item.principal.forEach { add(inlineToJson(it)) } }
        }
        if (item.blocks.isNotEmpty()) {
            putJsonArray("blocks") { item.blocks.forEach { add(blockToJson(it)) } }
        }
        addLocation(item.location)
    }

    /**
     * Emits block metadata and title. Field naming is best-effort against the
     * official schema (metadata: attributes/options/roles; title as inlines) —
     * no TCK fixture covers these yet; adjust when one lands.
     */
    private fun JsonObjectBuilder.addMetadata(metadata: BlockMetadata?) {
        if (metadata == null) return
        metadata.title?.let { title ->
            putJsonArray("title") { title.forEach { add(inlineToJson(it)) } }
        }
        val style = metadata.positional.firstOrNull()
        val hasAttributes = metadata.named.isNotEmpty() || metadata.id != null || style != null
        if (!hasAttributes && metadata.roles.isEmpty() && metadata.options.isEmpty()) return
        put("metadata", buildJsonObject {
            if (hasAttributes) {
                put("attributes", buildJsonObject {
                    style?.let { put("style", it) }
                    metadata.id?.let { put("id", it) }
                    metadata.named.forEach { (key, value) -> put(key, value) }
                })
            }
            if (metadata.roles.isNotEmpty()) {
                putJsonArray("roles") { metadata.roles.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } }
            }
            if (metadata.options.isNotEmpty()) {
                putJsonArray("options") { metadata.options.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } }
            }
        })
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
        // Processing-phase extension nodes are not part of the official schema.
        is InlineAttributeRef, is InlineCallout, is InlineCitation, is InlineFootnote, is InlineRaw -> error(
            "${inline::class.simpleName} has no official ASG serialization; " +
                "it must be resolved by document-processing before serializing",
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
