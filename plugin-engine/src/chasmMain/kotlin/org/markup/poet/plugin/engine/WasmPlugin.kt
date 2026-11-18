package org.markup.poet.plugin.engine

import io.github.charlietap.chasm.embedding.shapes.Instance
import io.github.charlietap.chasm.embedding.shapes.Memory
import io.github.charlietap.chasm.embedding.shapes.Store
import io.github.charlietap.chasm.runtime.value.NumberValue
import org.markup.poet.plugin.api.PluginDescriptor
import org.markup.poet.plugin.api.PluginHandle
import org.markup.poet.plugin.api.PluginInvocation
import org.markup.poet.plugin.api.PluginJson
import org.markup.poet.plugin.api.PluginResponse

/** A loaded WASM plugin instance. One store/instance/memory per plugin. */
class WasmPlugin internal constructor(
    val descriptor: PluginDescriptor,
    val sourceName: String,
    private val store: Store,
    private val instance: Instance,
    private val memory: Memory,
    private val hasOnUnload: Boolean,
    private val limits: PluginLimits,
) : PluginHandle {
    override val id: String get() = descriptor.id

    private var disposed = false

    /** True after a failed invocation left the instance in an unknown state. */
    var poisoned: Boolean = false
        private set

    override fun process(invocation: PluginInvocation): PluginResponse {
        check(!disposed) { "Plugin '$id' has been disposed" }
        check(!poisoned) { "Plugin '$id' is poisoned after a previous failure" }

        val inputBytes = PluginJson.encodeToString(PluginInvocation.serializer(), invocation).encodeToByteArray()
        if (inputBytes.size > limits.maxPayloadBytes) {
            throw PluginException.PayloadTooLargeError(inputBytes.size, limits.maxPayloadBytes)
        }

        try {
            val (ptr, len) = WasmMemoryOps.writeToMemory(store, memory, instance, inputBytes)
            val resultPtr = WasmMemoryOps.invokeI32(
                store, instance, "process",
                listOf(NumberValue.I32(ptr), NumberValue.I32(len)),
            )
            if (resultPtr == 0) {
                throw PluginException.InvocationError("process", "plugin signalled internal failure (returned 0)")
            }
            val resultJson = WasmMemoryOps.readLengthPrefixed(store, memory, resultPtr, limits.maxPayloadBytes)

            // Best effort: return both buffers to the plugin allocator.
            runCatching { WasmMemoryOps.pluginDealloc(store, instance, ptr, len) }
            runCatching {
                WasmMemoryOps.pluginDealloc(store, instance, resultPtr, 4 + resultJson.encodeToByteArray().size)
            }

            return PluginJson.decodeFromString(PluginResponse.serializer(), resultJson)
        } catch (e: PluginException) {
            poisoned = true
            throw e
        }
    }

    internal fun dispose() {
        if (disposed) return
        disposed = true
        if (hasOnUnload && !poisoned) {
            try {
                WasmMemoryOps.invokeVoid(store, instance, "on_unload")
            } catch (_: PluginException) {
                // best-effort
            }
        }
    }
}
