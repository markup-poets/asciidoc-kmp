@file:OptIn(kotlin.wasm.unsafe.UnsafeWasmMemoryApi::class)

package org.markup.poet.plugin.sdk

import kotlin.wasm.unsafe.Pointer
import kotlin.wasm.unsafe.withScopedMemoryAllocator
import org.markup.poet.plugin.api.PluginDescriptor
import org.markup.poet.plugin.api.PluginInvocation
import org.markup.poet.plugin.api.PluginJson
import org.markup.poet.plugin.api.PluginResponse

/**
 * Guest side of the plugin ABI for a plugin written in Kotlin/Wasm.
 *
 * A plugin module creates one [PluginGuest] and forwards the four exported
 * ABI functions to it:
 *
 * ```kotlin
 * private val guest = PluginGuest(descriptor) { invocation -> ... }
 *
 * @WasmExport("plugin_alloc") fun pluginAlloc(size: Int): Int = guest.alloc(size)
 * @WasmExport("plugin_dealloc") fun pluginDealloc(ptr: Int, size: Int) = guest.dealloc(ptr, size)
 * @WasmExport("plugin_info") fun pluginInfo(): Int = guest.info()
 * @WasmExport("process") fun process(ptr: Int, len: Int): Int = guest.process(ptr, len)
 * ```
 *
 * Buffers handed to the host are valid until the next call into the plugin.
 * The host uses them strictly within one call sequence (allocate, write,
 * `process`, read), so every buffer comes from Kotlin's scoped allocator,
 * which reuses the same region on each call and grows linear memory when a
 * payload needs it. Nothing accumulates across calls.
 */
class PluginGuest(
    private val descriptor: PluginDescriptor,
    private val handler: (PluginInvocation) -> PluginResponse,
) {
    /**
     * Kotlin/Wasm keeps its objects on the GC heap, so the scoped allocator's
     * region can start at address 0. The ABI reserves a 0 pointer for
     * "failed", hence the buffer is placed [GUARD] bytes into the allocation.
     */
    fun alloc(size: Int): Int = withScopedMemoryAllocator { it.allocate(size + GUARD).address.toInt() + GUARD }

    @Suppress("UNUSED_PARAMETER")
    fun dealloc(ptr: Int, size: Int) {
        // Buffers live in the scoped allocator's region and are reclaimed by the next call.
    }

    fun info(): Int = writeLengthPrefixed(PluginJson.encodeToString(PluginDescriptor.serializer(), descriptor))

    /** Returns a pointer to the length-prefixed JSON response, or 0 when the response cannot be produced. */
    fun process(ptr: Int, len: Int): Int {
        // Copy the request out first: writing the response reuses the same region.
        val request = readBytes(ptr, len).decodeToString()
        val response = try {
            handler(PluginJson.decodeFromString(PluginInvocation.serializer(), request))
        } catch (e: Throwable) {
            PluginResponse(ok = false, error = e.message ?: e.toString())
        }
        return try {
            writeLengthPrefixed(PluginJson.encodeToString(PluginResponse.serializer(), response))
        } catch (_: Throwable) {
            0
        }
    }

    private companion object {
        const val GUARD = 8
    }

    private fun readBytes(ptr: Int, len: Int): ByteArray {
        val base = Pointer(ptr.toUInt())
        return ByteArray(len) { (base + it).loadByte() }
    }

    private fun writeLengthPrefixed(text: String): Int {
        val bytes = text.encodeToByteArray()
        val ptr = alloc(4 + bytes.size)
        val base = Pointer(ptr.toUInt())
        base.storeInt(bytes.size)
        for (i in bytes.indices) (base + 4 + i).storeByte(bytes[i])
        return ptr
    }
}
