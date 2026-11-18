package org.markup.poet.plugin.engine

import io.github.charlietap.chasm.embedding.exports
import io.github.charlietap.chasm.embedding.instance
import io.github.charlietap.chasm.embedding.module
import io.github.charlietap.chasm.embedding.moduleInfo
import io.github.charlietap.chasm.embedding.shapes.ChasmResult
import io.github.charlietap.chasm.embedding.shapes.Memory
import io.github.charlietap.chasm.embedding.store
import org.markup.poet.plugin.api.PLUGIN_ABI_VERSION
import org.markup.poet.plugin.api.PluginDescriptor
import org.markup.poet.plugin.api.PluginDispatch
import org.markup.poet.plugin.api.PluginJson

/**
 * Loads and manages WASM plugins (Chasm runtime, pure Kotlin — works on every
 * KMP target Chasm supports).
 *
 * The sandbox guarantee: a plugin gets no host capability. The only imports a
 * module may declare are the inert WASI functions in [WasiStubs], which exist
 * so that modules built by WASI toolchains (Kotlin/Wasm, for one) can
 * instantiate; they do no I/O. A module importing anything else is rejected.
 */
class PluginEngine(
    private val limits: PluginLimits = PluginLimits(),
) : PluginDispatch {
    private val plugins = LinkedHashMap<String, WasmPlugin>()

    /** (capability type, name) → plugin, for dispatch during processing. */
    private val capabilities = LinkedHashMap<Pair<String, String>, WasmPlugin>()

    fun loadPlugin(bytes: ByteArray, sourceName: String): WasmPlugin {
        val pluginStore = store()

        val mod = when (val result = module(bytes)) {
            is ChasmResult.Success -> result.result
            is ChasmResult.Error -> throw PluginException.ModuleDecodeError(result.error.toString())
        }

        // Only the inert WASI stubs may be imported; anything else is refused.
        val requested = moduleInfo(mod).imports
        requested.firstOrNull { !WasiStubs.provides(it.moduleName, it.entityName) }?.let {
            throw PluginException.InstantiationError(
                "import ${it.moduleName}.${it.entityName} is not available to plugins"
            )
        }
        val stubs = WasiStubs.createImports(pluginStore, requested.map { it.entityName }.distinct())
        val inst = when (val result = instance(pluginStore, mod, stubs)) {
            is ChasmResult.Success -> result.result
            is ChasmResult.Error -> throw PluginException.InstantiationError(result.error.toString())
        }

        val exportList = exports(inst)
        val exportNames = exportList.map { it.name }.toSet()
        // WASI reactor modules initialise their runtime here, before any other export is called.
        if ("_initialize" in exportNames) {
            WasmMemoryOps.invokeVoid(pluginStore, inst, "_initialize")
        }
        for (name in listOf("memory", "plugin_alloc", "plugin_dealloc", "plugin_info", "process")) {
            if (name !in exportNames) throw PluginException.MissingExport(name)
        }
        val memory = exportList.first { it.value is Memory }.value as Memory

        if ("on_load" in exportNames) {
            WasmMemoryOps.invokeVoid(pluginStore, inst, "on_load")
        }

        val descriptor: PluginDescriptor = try {
            val infoPtr = WasmMemoryOps.invokeI32(pluginStore, inst, "plugin_info")
            val infoJson = WasmMemoryOps.readLengthPrefixed(pluginStore, memory, infoPtr, limits.maxDescriptorBytes)
            PluginJson.decodeFromString(PluginDescriptor.serializer(), infoJson)
        } catch (e: PluginException) {
            throw e
        } catch (e: Exception) {
            throw PluginException.DescriptorParseError(e.message ?: "unknown error", e)
        }

        if (descriptor.abiVersion != PLUGIN_ABI_VERSION) {
            throw PluginException.UnsupportedAbiVersion(descriptor.abiVersion, PLUGIN_ABI_VERSION)
        }
        if (descriptor.id in plugins) {
            throw PluginException.DuplicatePluginError(descriptor.id)
        }
        for (capability in descriptor.capabilities) {
            val key = capability.type to capability.name
            capabilities[key]?.let { existing ->
                throw PluginException.DuplicateCapabilityError(capability.type, capability.name, existing.id)
            }
        }

        val plugin = WasmPlugin(
            descriptor = descriptor,
            sourceName = sourceName,
            store = pluginStore,
            instance = inst,
            memory = memory,
            hasOnUnload = "on_unload" in exportNames,
            limits = limits,
        )
        plugins[descriptor.id] = plugin
        for (capability in descriptor.capabilities) {
            capabilities[capability.type to capability.name] = plugin
        }
        return plugin
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

    fun plugin(id: String): WasmPlugin? = plugins[id]

    override fun descriptors(): List<PluginDescriptor> = plugins.values.map { it.descriptor }

    /** The plugin claiming ([type], [name]), or null. */
    override fun forCapability(type: String, name: String): WasmPlugin? = capabilities[type to name]
}
