@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.markup.poet.plugin.engine

import kotlin.js.Promise
import org.markup.poet.plugin.api.PLUGIN_ABI_VERSION
import org.markup.poet.plugin.api.PluginDescriptor
import org.markup.poet.plugin.api.PluginDispatch
import org.markup.poet.plugin.api.PluginHandle
import org.markup.poet.plugin.api.PluginInvocation
import org.markup.poet.plugin.api.PluginJson
import org.markup.poet.plugin.api.PluginResponse

/**
 * Loads and manages WASM plugins in a browser, on the browser's own
 * WebAssembly engine (Chasm, which runs plugins on the other targets, has no
 * browser build). The plugin ABI, the limits and the sandbox rule are the
 * same as for `PluginEngine`: a module may import only the inert WASI
 * functions, which do no I/O, and anything else is refused.
 *
 * One difference follows from the platform: browsers compile modules of this
 * size asynchronously, so loading returns a [Promise]. Once a plugin is
 * loaded, invocations are synchronous, and the engine plugs into
 * `WasmExtensions` like any other [PluginDispatch].
 */
class BrowserPluginEngine(
    private val limits: PluginLimits = PluginLimits(),
) : PluginDispatch {
    private val plugins = LinkedHashMap<String, BrowserPlugin>()
    private val capabilities = LinkedHashMap<Pair<String, String>, BrowserPlugin>()

    /**
     * Loads a plugin from [wasm], a JavaScript `ArrayBuffer` or typed array
     * holding the module. The promise resolves with the plugin id once the
     * plugin is registered and rejects with an `Error` when it is refused.
     */
    fun loadPlugin(wasm: JsAny, sourceName: String): Promise<JsString> =
        instantiatePlugin(wasm).then { instance -> register(instance, sourceName).toJsString() }

    /**
     * Fetches the module at [url] (resolved against the page like any
     * `fetch`) and loads it. Rejects when the response is not successful.
     */
    fun loadPluginFromUrl(url: String, sourceName: String = url): Promise<JsString> =
        // JavaScript flattens a promise returned from `then`; the cast states the type it settles to.
        fetchBytes(url).then { wasm -> loadPlugin(wasm, sourceName) }.unsafeCast<Promise<JsString>>()

    private fun register(instance: JsAny, sourceName: String): String {
        val descriptor = try {
            PluginJson.decodeFromString(PluginDescriptor.serializer(), readInfo(instance, limits.maxDescriptorBytes))
        } catch (e: Throwable) {
            throw refusal("$sourceName: cannot read the plugin descriptor: ${e.message}")
        }
        if (descriptor.abiVersion != PLUGIN_ABI_VERSION) {
            throw refusal("$sourceName: unsupported ABI version ${descriptor.abiVersion} (supported: $PLUGIN_ABI_VERSION)")
        }
        if (descriptor.id in plugins) {
            throw refusal("$sourceName: a plugin with id '${descriptor.id}' is already loaded")
        }
        for (capability in descriptor.capabilities) {
            capabilities[capability.type to capability.name]?.let { existing ->
                throw refusal(
                    "$sourceName: capability ${capability.type} '${capability.name}' is already claimed by '${existing.id}'",
                )
            }
        }

        val plugin = BrowserPlugin(descriptor, instance, limits)
        plugins[descriptor.id] = plugin
        for (capability in descriptor.capabilities) {
            capabilities[capability.type to capability.name] = plugin
        }
        return descriptor.id
    }

    fun unloadPlugin(id: String): Boolean {
        val plugin = plugins.remove(id) ?: return false
        capabilities.entries.removeAll { it.value === plugin }
        plugin.dispose()
        return true
    }

    fun unloadAll() {
        plugins.keys.toList().forEach { unloadPlugin(it) }
    }

    override fun descriptors(): List<PluginDescriptor> = plugins.values.map { it.descriptor }

    override fun forCapability(type: String, name: String): PluginHandle? = capabilities[type to name]
}

/** A plugin instantiated by the browser. */
private class BrowserPlugin(
    val descriptor: PluginDescriptor,
    private val instance: JsAny,
    private val limits: PluginLimits,
) : PluginHandle {
    override val id: String get() = descriptor.id

    private var disposed = false
    private var poisoned = false

    override fun process(invocation: PluginInvocation): PluginResponse {
        check(!disposed) { "Plugin '$id' has been disposed" }
        check(!poisoned) { "Plugin '$id' is poisoned after a previous failure" }
        val request = PluginJson.encodeToString(PluginInvocation.serializer(), invocation)
        val response = try {
            invokeProcess(instance, request, limits.maxPayloadBytes)
        } catch (e: Throwable) {
            // A trap or a broken reply leaves the instance in an unknown state.
            poisoned = true
            throw PluginException.InvocationError("process", e.message ?: "plugin call failed", e)
        }
        return PluginJson.decodeFromString(PluginResponse.serializer(), response)
    }

    fun dispose() {
        if (disposed) return
        disposed = true
        if (!poisoned) runCatching { invokeOnUnload(instance) }
    }
}

/** The JavaScript `Error` a load promise rejects with. */
private fun refusal(message: String): Throwable = PluginLoadRefused(message)

private class PluginLoadRefused(message: String) : RuntimeException(message)

// ---------------------------------------------------------------------------
// JavaScript side. Payloads cross as strings: the browser encodes and decodes
// UTF-8 and copies to and from the plugin's memory, so no bytes are copied
// one by one through Kotlin.
// ---------------------------------------------------------------------------

/**
 * Compiles and instantiates a plugin module. Imports are limited to the inert
 * `wasi_snapshot_preview1` stubs (same table and errno values as `WasiStubs`
 * on the Chasm targets): output is discarded, reads and lookups fail with
 * EBADF (8), clock and randomness with ENOSYS (52).
 */
private fun instantiatePlugin(wasm: JsAny): Promise<JsAny> = js(
    """(async () => {
        const stubs = {
            fd_write: () => 0, fd_close: () => 0,
            fd_read: () => 8, fd_seek: () => 8, fd_fdstat_get: () => 8,
            fd_prestat_get: () => 8, fd_prestat_dir_name: () => 8,
            environ_sizes_get: () => 0, environ_get: () => 0,
            args_sizes_get: () => 0, args_get: () => 0,
            random_get: () => 52, clock_time_get: () => 52,
            proc_exit: () => {},
        };
        const module = await WebAssembly.compile(wasm);
        const imports = {};
        for (const wanted of WebAssembly.Module.imports(module)) {
            const allowed = wanted.module === 'wasi_snapshot_preview1' && wanted.kind === 'function' &&
                Object.prototype.hasOwnProperty.call(stubs, wanted.name);
            if (!allowed) {
                throw new Error('import ' + wanted.module + '.' + wanted.name + ' is not available to plugins');
            }
            (imports[wanted.module] = imports[wanted.module] || {})[wanted.name] = stubs[wanted.name];
        }
        const instance = await WebAssembly.instantiate(module, imports);
        const exports = instance.exports;
        for (const name of ['memory', 'plugin_alloc', 'plugin_dealloc', 'plugin_info', 'process']) {
            if (!(name in exports)) throw new Error('plugin does not export ' + name);
        }
        if (exports._initialize) exports._initialize();
        if (exports.on_load) exports.on_load();
        return instance;
    })()"""
)

/** Reads the length-prefixed descriptor JSON returned by `plugin_info`. */
private fun readInfo(instance: JsAny, maxBytes: Int): String = js(
    """(() => {
        const exports = instance.exports;
        const ptr = exports.plugin_info();
        const length = new DataView(exports.memory.buffer).getInt32(ptr, true);
        if (ptr === 0 || length <= 0 || length > maxBytes) throw new Error('invalid descriptor (length ' + length + ')');
        return new TextDecoder().decode(new Uint8Array(exports.memory.buffer, ptr + 4, length));
    })()"""
)

/**
 * One `process` round trip: writes [request] into plugin memory, calls
 * `process`, and returns the length-prefixed JSON reply. The memory buffer is
 * looked up again after every call into the plugin, because growing the
 * memory replaces it.
 */
private fun invokeProcess(instance: JsAny, request: String, maxBytes: Int): String = js(
    """(() => {
        const exports = instance.exports;
        const bytes = new TextEncoder().encode(request);
        if (bytes.length > maxBytes) {
            throw new Error('payload of ' + bytes.length + ' bytes exceeds the limit of ' + maxBytes);
        }
        const ptr = exports.plugin_alloc(bytes.length);
        new Uint8Array(exports.memory.buffer).set(bytes, ptr);
        const result = exports.process(ptr, bytes.length);
        if (result === 0) throw new Error('plugin signalled internal failure (returned 0)');
        const length = new DataView(exports.memory.buffer).getInt32(result, true);
        if (length <= 0 || length > maxBytes) throw new Error('invalid response length ' + length);
        const reply = new TextDecoder().decode(new Uint8Array(exports.memory.buffer, result + 4, length));
        try {
            exports.plugin_dealloc(ptr, bytes.length);
            exports.plugin_dealloc(result, 4 + length);
        } catch (ignored) {}
        return reply;
    })()"""
)

private fun invokeOnUnload(instance: JsAny): Unit = js("{ if (instance.exports.on_unload) instance.exports.on_unload(); }")

private fun fetchBytes(url: String): Promise<JsAny> = js(
    """fetch(url).then((response) => {
        if (!response.ok) throw new Error('cannot fetch plugin ' + url + ': HTTP ' + response.status);
        return response.arrayBuffer();
    })"""
)
