package org.markup.poet.asciidoc.asg.serialization

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.Block
import org.markup.poet.asciidoc.asg.BlockMacro
import org.markup.poet.asciidoc.asg.BlockMacroName
import org.markup.poet.asciidoc.asg.BlockMetadata
import org.markup.poet.asciidoc.asg.BreakBlock
import org.markup.poet.asciidoc.asg.BreakVariant
import org.markup.poet.asciidoc.asg.DListBlock
import org.markup.poet.asciidoc.asg.DListItem
import org.markup.poet.asciidoc.asg.DiscreteHeading
import org.markup.poet.asciidoc.asg.Header
import org.markup.poet.asciidoc.asg.Inline
import org.markup.poet.asciidoc.asg.InlineRef
import org.markup.poet.asciidoc.asg.InlineSpan
import org.markup.poet.asciidoc.asg.InlineText
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.LeafBlockForm
import org.markup.poet.asciidoc.asg.LeafBlockName
import org.markup.poet.asciidoc.asg.ListBlock
import org.markup.poet.asciidoc.asg.ListItem
import org.markup.poet.asciidoc.asg.ListVariant
import org.markup.poet.asciidoc.asg.Location
import org.markup.poet.asciidoc.asg.ParentBlock
import org.markup.poet.asciidoc.asg.ParentBlockName
import org.markup.poet.asciidoc.asg.Position
import org.markup.poet.asciidoc.asg.RefVariant
import org.markup.poet.asciidoc.asg.SectionBlock
import org.markup.poet.asciidoc.asg.SpanForm
import org.markup.poet.asciidoc.asg.SpanVariant

/**
 * Decodes official ASG JSON (asciidoc-lang `asg/schema.json`) back into the
 * ASG model — the inverse of [AsgDocumentJsonSerializer]. It decodes at least
 * everything the serializer emits (blocks, inlines, metadata; locations are
 * optional), so `serialize → deserialize → serialize` is byte-identical.
 *
 * Unknown node names or malformed shapes fail loudly with
 * [IllegalArgumentException]; callers splicing plugin-supplied ASG must treat
 * that as a plugin error, not a crash.
 */
class AsgDocumentJsonDeserializer {
    private val json = Json

    /** Decodes a whole `{"name": "document", ...}` object. */
    fun deserializeDocument(text: String): AsgDocument =
        documentFromJson(json.parseToJsonElement(text).jsonObject)

    /**
     * Decodes a JSON array of block nodes (a single block object is accepted
     * and wrapped).
     */
    fun deserializeBlocks(text: String): List<Block> =
        when (val element = json.parseToJsonElement(text)) {
            is JsonArray -> element.map { blockFromJson(it.jsonObject) }
            is JsonObject -> listOf(blockFromJson(element))
            else -> throw IllegalArgumentException("Expected a block node or an array of block nodes")
        }

    /**
     * Decodes a JSON array of inline nodes (a single inline object is accepted
     * and wrapped).
     */
    fun deserializeInlines(text: String): List<Inline> =
        when (val element = json.parseToJsonElement(text)) {
            is JsonArray -> element.map { inlineFromJson(it.jsonObject) }
            is JsonObject -> listOf(inlineFromJson(element))
            else -> throw IllegalArgumentException("Expected an inline node or an array of inline nodes")
        }

    // -----------------------------------------------------------------------
    // Document
    // -----------------------------------------------------------------------

    fun documentFromJson(obj: JsonObject): AsgDocument {
        require(obj.name() == "document") { "Expected a 'document' node, got '${obj.name()}'" }
        val attributes = LinkedHashMap<String, String>()
        obj["attributes"]?.jsonObject?.forEach { (key, value) ->
            attributes[key] = value.jsonPrimitive.content
        }
        val header = obj["header"]?.jsonObject?.let { headerObj ->
            Header(
                title = inlinesOf(headerObj, "title"),
                location = locationOf(headerObj),
            )
        }
        return AsgDocument(
            attributes = attributes,
            header = header,
            blocks = blocksOf(obj),
            location = locationOf(obj),
        )
    }

    // -----------------------------------------------------------------------
    // Blocks
    // -----------------------------------------------------------------------

    fun blockFromJson(obj: JsonObject): Block {
        val name = obj.name()
        leafBlockNames[name]?.let { return leafBlockFromJson(obj, it) }
        parentBlockNames[name]?.let { return parentBlockFromJson(obj, it) }
        blockMacroNames[name]?.let { macroName ->
            // Image/audio/video/toc share names with nothing else, but require
            // the macro form to distinguish future non-macro homonyms.
            require(obj.string("form") == "macro") { "Block macro '$name' must carry form=\"macro\"" }
            return BlockMacro(
                name = macroName,
                target = obj.string("target"),
                metadata = metadataFromJson(obj),
                location = locationOf(obj),
            )
        }
        return when (name) {
            "section" -> SectionBlock(
                title = inlinesOf(obj, "title"),
                level = obj.int("level"),
                blocks = blocksOf(obj),
                location = locationOf(obj),
            )
            "heading" -> DiscreteHeading(
                title = inlinesOf(obj, "title"),
                level = obj.int("level"),
                // The block-level "title" key is the heading text, so a
                // metadata title is not representable here.
                metadata = metadataFromJson(obj, includeTitle = false),
                location = locationOf(obj),
            )
            "list" -> ListBlock(
                variant = listVariants[obj.string("variant")]
                    ?: throw IllegalArgumentException("Unknown list variant '${obj.string("variant")}'"),
                marker = obj.string("marker") ?: "*",
                items = obj.getValue("items").jsonArray.map { listItemFromJson(it.jsonObject) },
                metadata = metadataFromJson(obj),
                location = locationOf(obj),
            )
            "dlist" -> DListBlock(
                marker = obj.string("marker") ?: "::",
                items = obj.getValue("items").jsonArray.map { dlistItemFromJson(it.jsonObject) },
                metadata = metadataFromJson(obj),
                location = locationOf(obj),
            )
            "break" -> BreakBlock(
                variant = breakVariants[obj.string("variant")]
                    ?: throw IllegalArgumentException("Unknown break variant '${obj.string("variant")}'"),
                location = locationOf(obj),
            )
            else -> throw IllegalArgumentException("Unknown ASG block name '$name'")
        }
    }

    private fun leafBlockFromJson(obj: JsonObject, name: LeafBlockName): LeafBlock = LeafBlock(
        name = name,
        form = when (val form = obj.string("form")) {
            null -> LeafBlockForm.PARAGRAPH
            "delimited" -> LeafBlockForm.DELIMITED
            "indented" -> LeafBlockForm.INDENTED
            else -> throw IllegalArgumentException("Unknown leaf block form '$form'")
        },
        delimiter = obj.string("delimiter"),
        inlines = inlinesOf(obj, "inlines"),
        metadata = metadataFromJson(obj),
        location = locationOf(obj),
    )

    private fun parentBlockFromJson(obj: JsonObject, name: ParentBlockName): ParentBlock = ParentBlock(
        name = name,
        variant = obj.string("variant"),
        delimiter = obj.string("delimiter"),
        blocks = blocksOf(obj),
        metadata = metadataFromJson(obj),
        location = locationOf(obj),
    )

    private fun listItemFromJson(obj: JsonObject): ListItem {
        require(obj.name() == "listItem") { "Expected a 'listItem' node, got '${obj.name()}'" }
        return ListItem(
            marker = obj.string("marker") ?: "*",
            principal = inlinesOf(obj, "principal"),
            blocks = blocksOf(obj),
            location = locationOf(obj),
        )
    }

    private fun dlistItemFromJson(obj: JsonObject): DListItem {
        require(obj.name() == "dlistItem") { "Expected a 'dlistItem' node, got '${obj.name()}'" }
        return DListItem(
            marker = obj.string("marker") ?: "::",
            terms = obj.getValue("terms").jsonArray.map { term ->
                term.jsonArray.map { inlineFromJson(it.jsonObject) }
            },
            principal = inlinesOf(obj, "principal"),
            blocks = blocksOf(obj),
            location = locationOf(obj),
        )
    }

    // -----------------------------------------------------------------------
    // Metadata
    // -----------------------------------------------------------------------

    /**
     * Rebuilds [BlockMetadata] from the emitted `metadata` object plus the
     * block-level `title` inlines. The serializer only emits the block style
     * (`positional.first()`), so decoded metadata carries at most one
     * positional entry.
     */
    private fun metadataFromJson(obj: JsonObject, includeTitle: Boolean = true): BlockMetadata? {
        val meta = obj["metadata"]?.jsonObject
        val title = if (includeTitle) obj["title"]?.jsonArray?.map { inlineFromJson(it.jsonObject) } else null
        if (meta == null && title == null) return null

        var style: String? = null
        var id: String? = null
        val named = LinkedHashMap<String, String>()
        meta?.get("attributes")?.jsonObject?.forEach { (key, value) ->
            when (key) {
                "style" -> style = value.jsonPrimitive.content
                "id" -> id = value.jsonPrimitive.content
                else -> named[key] = value.jsonPrimitive.content
            }
        }
        return BlockMetadata(
            positional = listOfNotNull(style),
            named = named,
            id = id,
            roles = meta?.get("roles")?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
            options = meta?.get("options")?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
            title = title,
        )
    }

    // -----------------------------------------------------------------------
    // Inlines
    // -----------------------------------------------------------------------

    fun inlineFromJson(obj: JsonObject): Inline = when (val name = obj.name()) {
        "text" -> InlineText(
            value = obj.string("value") ?: "",
            location = locationOf(obj),
        )
        "span" -> InlineSpan(
            variant = spanVariants[obj.string("variant")]
                ?: throw IllegalArgumentException("Unknown span variant '${obj.string("variant")}'"),
            form = spanForms[obj.string("form")]
                ?: throw IllegalArgumentException("Unknown span form '${obj.string("form")}'"),
            inlines = inlinesOf(obj, "inlines"),
            location = locationOf(obj),
        )
        "ref" -> InlineRef(
            variant = refVariants[obj.string("variant")]
                ?: throw IllegalArgumentException("Unknown ref variant '${obj.string("variant")}'"),
            target = obj.string("target") ?: "",
            inlines = inlinesOf(obj, "inlines"),
            location = locationOf(obj),
        )
        else -> throw IllegalArgumentException("Unknown ASG inline name '$name'")
    }

    // -----------------------------------------------------------------------
    // Shared helpers
    // -----------------------------------------------------------------------

    private fun JsonObject.name(): String =
        string("name") ?: throw IllegalArgumentException("ASG node without a 'name' key")

    private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.content

    private fun JsonObject.int(key: String): Int =
        this[key]?.jsonPrimitive?.content?.toIntOrNull()
            ?: throw IllegalArgumentException("ASG node missing integer '$key'")

    private fun blocksOf(obj: JsonObject): List<Block> =
        obj["blocks"]?.jsonArray?.map { blockFromJson(it.jsonObject) } ?: emptyList()

    private fun inlinesOf(obj: JsonObject, key: String): List<Inline> =
        obj[key]?.jsonArray?.map { inlineFromJson(it.jsonObject) } ?: emptyList()

    private fun locationOf(obj: JsonObject): Location? = obj["location"]?.jsonArray?.let { boundaries ->
        require(boundaries.size == 2) { "A location must have exactly two boundaries" }
        Location(positionFromJson(boundaries[0].jsonObject), positionFromJson(boundaries[1].jsonObject))
    }

    private fun positionFromJson(obj: JsonObject): Position =
        Position(line = obj.int("line"), col = obj.int("col"))

    private companion object {
        val leafBlockNames = LeafBlockName.entries.associateBy { it.asgName }
        val parentBlockNames = ParentBlockName.entries.associateBy { it.asgName }
        val blockMacroNames = BlockMacroName.entries.associateBy { it.asgName }
        val listVariants = ListVariant.entries.associateBy { it.asgName }
        val breakVariants = BreakVariant.entries.associateBy { it.asgName }
        val spanVariants = SpanVariant.entries.associateBy { it.asgName }
        val spanForms = SpanForm.entries.associateBy { it.asgName }
        val refVariants = RefVariant.entries.associateBy { it.asgName }
    }
}
