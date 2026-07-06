package org.markup.poet.plugin.api

/**
 * Host-side handle to one loaded plugin: something that can process
 * [PluginInvocation] envelopes. Implemented by the engine's `WasmPlugin`;
 * test suites may substitute pure-Kotlin doubles.
 */
interface PluginHandle {
    /** The plugin id from its descriptor. */
    val id: String

    /** Processes one invocation envelope and returns the response envelope. */
    fun process(invocation: PluginInvocation): PluginResponse
}

/**
 * Capability-dispatch surface of a plugin host: looks up which plugin claims a
 * `(capability type, name)` pair. Implemented by the engine's `PluginEngine`.
 */
interface PluginDispatch {
    /** The plugin claiming ([type], [name]), or null. */
    fun forCapability(type: String, name: String): PluginHandle?

    /** Descriptors of every loaded plugin, in load order. */
    fun descriptors(): List<PluginDescriptor>
}
