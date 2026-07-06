package org.markup.poet.plugin.engine

import org.markup.poet.plugin.api.PluginInvocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PluginEngineTest {

    private fun fixtureBytes(): ByteArray =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/shout.wasm")) {
            "fixture /fixtures/shout.wasm missing — build it with examples/plugins/shout-rust/build.sh"
        }.readBytes()

    @Test
    fun loadsPluginAndReadsDescriptor() {
        val engine = PluginEngine()
        val plugin = engine.loadPlugin(fixtureBytes(), "shout.wasm")
        assertEquals("shout-plugin", plugin.id)
        assertEquals(1, plugin.descriptor.abiVersion)
        val capability = plugin.descriptor.capabilities.single()
        assertEquals("block", capability.type)
        assertEquals("shout", capability.name)
        assertNotNull(engine.forCapability("block", "shout"))
        assertNull(engine.forCapability("block", "unknown"))
        engine.unloadAll()
    }

    @Test
    fun processReturnsHtmlReplacement() {
        val engine = PluginEngine()
        val plugin = engine.loadPlugin(fixtureBytes(), "shout.wasm")
        val response = plugin.process(
            PluginInvocation(
                extensionPoint = "block",
                name = "shout",
                content = "hello <world>",
            ),
        )
        assertTrue(response.ok)
        val replacement = assertNotNull(response.replacement)
        assertEquals("html", replacement.contentType)
        assertEquals("<div class=\"shout\">HELLO &lt;WORLD&gt;!</div>", replacement.value)
        engine.unloadAll()
    }

    @Test
    fun unsupportedCapabilityYieldsOkFalse() {
        val engine = PluginEngine()
        val plugin = engine.loadPlugin(fixtureBytes(), "shout.wasm")
        val response = plugin.process(
            PluginInvocation(extensionPoint = "inlineMacro", name = "issue", content = "123"),
        )
        assertEquals(false, response.ok)
        assertNotNull(response.error)
        engine.unloadAll()
    }

    @Test
    fun garbageBytesFailWithModuleDecodeError() {
        val engine = PluginEngine()
        assertFailsWith<PluginException.ModuleDecodeError> {
            engine.loadPlugin(byteArrayOf(1, 2, 3, 4), "garbage.wasm")
        }
    }

    @Test
    fun duplicateLoadFailsWithDuplicatePluginError() {
        val engine = PluginEngine()
        engine.loadPlugin(fixtureBytes(), "shout.wasm")
        assertFailsWith<PluginException.DuplicatePluginError> {
            engine.loadPlugin(fixtureBytes(), "shout-again.wasm")
        }
        engine.unloadAll()
    }

    @Test
    fun oversizedPayloadIsRejectedBeforeReachingThePlugin() {
        val engine = PluginEngine(PluginLimits(maxPayloadBytes = 128))
        val plugin = engine.loadPlugin(fixtureBytes(), "shout.wasm")
        assertFailsWith<PluginException.PayloadTooLargeError> {
            plugin.process(
                PluginInvocation(extensionPoint = "block", name = "shout", content = "x".repeat(500)),
            )
        }
        engine.unloadAll()
    }

    @Test
    fun unloadedPluginIsGone() {
        val engine = PluginEngine()
        val plugin = engine.loadPlugin(fixtureBytes(), "shout.wasm")
        assertTrue(engine.unloadPlugin(plugin.id))
        assertNull(engine.plugin(plugin.id))
        assertNull(engine.forCapability("block", "shout"))
        assertEquals(false, engine.unloadPlugin(plugin.id))
    }
}
