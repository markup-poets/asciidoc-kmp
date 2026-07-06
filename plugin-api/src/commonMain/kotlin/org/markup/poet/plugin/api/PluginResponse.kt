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
 * The JSON envelope a plugin returns from `process` as length-prefixed JSON.
 * With `ok = false` the host leaves the original construct untouched and
 * reports [error] as a processing warning.
 */
@Serializable
data class PluginResponse(
    val ok: Boolean,
    val replacement: PluginReplacement? = null,
    val error: String? = null,
    val warnings: List<String> = emptyList(),
)
