package org.markup.poet.plugin.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * What the construct is replaced with.
 *
 * @param contentType `asciidoc` (host re-parses and splices the blocks),
 *   `html` (spliced as a raw passthrough node), or `asg` (official ASG node
 *   JSON, decoded and spliced as blocks/inlines)
 * @param value the replacement payload (for `asg` it may hold the node JSON
 *   as a string when [nodes] is absent)
 * @param nodes for `asg`: the replacement as a JSON array of official ASG
 *   nodes (or a single node object); takes precedence over [value]
 */
@Serializable
data class PluginReplacement(
    val contentType: String,
    val value: String = "",
    val nodes: JsonElement? = null,
)

/**
 * A document-level resource the plugin's output depends on, such as a
 * stylesheet link or a script.
 *
 * @param key identifies the resource; the host keeps one asset per key for a
 *   document however many responses carry it
 * @param html the markup to place, verbatim
 * @param placement where it belongs; `head` (the document `<head>`) is the
 *   only placement defined so far
 */
@Serializable
data class PluginAsset(
    val key: String,
    val html: String,
    val placement: String = PLACEMENT_HEAD,
)

/** [PluginAsset.placement] for the document `<head>`. */
const val PLACEMENT_HEAD: String = "head"

/**
 * The JSON envelope a plugin returns from `process` as length-prefixed JSON.
 * With `ok = false` the host leaves the original construct untouched and
 * reports [error] as a processing warning. `ok = true` without a
 * [replacement] declines the construct: the host keeps the original and
 * renders it as if no plugin had claimed it.
 *
 * @param assets document-level resources the replacement needs
 */
@Serializable
data class PluginResponse(
    val ok: Boolean,
    val replacement: PluginReplacement? = null,
    val error: String? = null,
    val warnings: List<String> = emptyList(),
    val assets: List<PluginAsset> = emptyList(),
)
