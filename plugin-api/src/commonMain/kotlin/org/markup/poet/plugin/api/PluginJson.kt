package org.markup.poet.plugin.api

import kotlinx.serialization.json.Json

/**
 * The JSON configuration shared by hosts and the ABI: unknown keys are ignored
 * for forward compatibility, defaults are encoded so `abiVersion` is always on
 * the wire.
 */
val PluginJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}
