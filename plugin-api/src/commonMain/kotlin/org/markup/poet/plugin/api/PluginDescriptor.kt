package org.markup.poet.plugin.api

import kotlinx.serialization.Serializable

/** Current plugin ABI version. Hosts refuse plugins declaring another version. */
const val PLUGIN_ABI_VERSION: Int = 1

/**
 * One capability a plugin offers.
 *
 * @param type extension point: `block`, `blockMacro`, `inlineMacro`, or `converter`
 * @param name the block style / macro name the plugin claims (e.g. `shout`,
 *   `plantuml`, `issue`); for `converter` the ASG node name it renders
 */
@Serializable
data class PluginCapability(
    val type: String,
    val name: String,
)

/**
 * Self-description a plugin returns from its `plugin_info()` export as
 * length-prefixed JSON.
 */
@Serializable
data class PluginDescriptor(
    val abiVersion: Int,
    val id: String,
    val name: String,
    val version: String,
    val description: String = "",
    val capabilities: List<PluginCapability> = emptyList(),
    val metadata: Map<String, String> = emptyMap(),
)
