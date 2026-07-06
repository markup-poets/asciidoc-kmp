package org.markup.poet.plugin.engine

/**
 * Safety limits enforced by the engine.
 *
 * @param maxPayloadBytes cap on any length-prefixed payload crossing the
 *   host/plugin boundary in either direction
 * @param maxDescriptorBytes cap on the `plugin_info()` descriptor payload
 */
data class PluginLimits(
    val maxPayloadBytes: Int = 1_000_000,
    val maxDescriptorBytes: Int = 65_536,
)
