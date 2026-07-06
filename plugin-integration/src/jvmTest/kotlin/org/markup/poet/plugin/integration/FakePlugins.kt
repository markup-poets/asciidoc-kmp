package org.markup.poet.plugin.integration

import org.markup.poet.plugin.api.PluginCapability
import org.markup.poet.plugin.api.PluginDescriptor
import org.markup.poet.plugin.api.PluginDispatch
import org.markup.poet.plugin.api.PluginHandle
import org.markup.poet.plugin.api.PluginInvocation
import org.markup.poet.plugin.api.PluginResponse

/**
 * Pure-Kotlin plugin doubles: exercise the integration layer's capability
 * dispatch and response splicing without compiling a WASM fixture. The wire
 * ABI itself stays covered by the shout-rust end-to-end tests.
 */
class FakePlugin(
    override val id: String,
    val capabilities: List<PluginCapability>,
    private val handler: (PluginInvocation) -> PluginResponse,
) : PluginHandle {
    val invocations = mutableListOf<PluginInvocation>()

    override fun process(invocation: PluginInvocation): PluginResponse {
        invocations += invocation
        return handler(invocation)
    }
}

class FakeDispatch(private val plugins: List<FakePlugin>) : PluginDispatch {
    override fun forCapability(type: String, name: String): PluginHandle? =
        plugins.firstOrNull { plugin -> plugin.capabilities.any { it.type == type && it.name == name } }

    override fun descriptors(): List<PluginDescriptor> = plugins.map { plugin ->
        PluginDescriptor(
            abiVersion = 1,
            id = plugin.id,
            name = plugin.id,
            version = "0.0.0-test",
            capabilities = plugin.capabilities,
        )
    }
}
