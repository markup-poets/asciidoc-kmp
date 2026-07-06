package org.markup.poet.plugin.api

import kotlinx.serialization.Serializable

/**
 * What the construct is replaced with.
 *
 * @param contentType `asciidoc` (host re-parses and splices the blocks),
 *   `html` (spliced as a raw passthrough node), or `asg` (ASG node JSON;
 *   reserved, decoded by hosts that support it)
 * @param value the replacement payload
 */
@Serializable
data class PluginReplacement(
    val contentType: String,
    val value: String,
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
