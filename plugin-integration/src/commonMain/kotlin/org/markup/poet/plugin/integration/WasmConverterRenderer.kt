package org.markup.poet.plugin.integration

import org.markup.poet.asciidoc.asg.AsgNode
import org.markup.poet.asciidoc.asg.Block
import org.markup.poet.asciidoc.asg.Inline
import org.markup.poet.asciidoc.asg.serialization.AsgDocumentJsonSerializer
import org.markup.poet.asciidoc.render.CustomRenderer
import org.markup.poet.asciidoc.render.RenderContext
import org.markup.poet.plugin.api.PluginDispatch
import org.markup.poet.plugin.api.PluginHandle
import org.markup.poet.plugin.api.PluginInvocation
import org.markup.poet.plugin.api.SourcePoint
import org.markup.poet.plugin.engine.PluginException

/**
 * Adapts a WASM plugin's `converter` capability to the html-renderer
 * [CustomRenderer] SPI. Register it in `RenderConfig.customRenderers` under
 * the capability name — the name of the block style or ASG node kind it
 * renders (see [CustomRenderer] for the dispatch rules), most conveniently
 * via [converterRenderers].
 *
 * The plugin receives the node it claimed as **official ASG node JSON**
 * ([AsgDocumentJsonSerializer]'s node-level encoding) in `content`, plus the
 * resolved document attributes, and must answer `{contentType: "html", value}`;
 * the value is emitted verbatim. Anything else — a failed invocation, an
 * `ok: false` response, an unsupported content type, or a node without an
 * official ASG form — renders as nothing plus a rendering warning.
 */
class WasmConverterRenderer(
    private val plugin: PluginHandle,
    private val capabilityName: String,
    private val documentAttributes: Map<String, String> = emptyMap(),
) : CustomRenderer {
    private val serializer = AsgDocumentJsonSerializer()

    override fun render(node: AsgNode, context: RenderContext): String {
        val nodeJson = try {
            when (node) {
                is Block -> serializer.blockToJsonString(node)
                is Inline -> serializer.inlineToJsonString(node)
                else -> {
                    context.logWarning(
                        "converter plugin '${plugin.id}' cannot render ${node::class.simpleName}: not a block or inline node",
                    )
                    return ""
                }
            }
        } catch (e: IllegalStateException) {
            context.logWarning("converter plugin '${plugin.id}' cannot render node: ${e.message}")
            return ""
        }

        val response = try {
            plugin.process(
                PluginInvocation(
                    extensionPoint = "converter",
                    name = capabilityName,
                    content = nodeJson,
                    documentAttributes = documentAttributes,
                    location = SourcePoint(
                        node.location?.start?.line ?: 1,
                        node.location?.start?.col ?: 1,
                    ),
                ),
            )
        } catch (e: PluginException) {
            context.logWarning("converter plugin '${plugin.id}' failed on '$capabilityName' node: ${e.message}")
            return ""
        }

        response.warnings.forEach { context.logWarning("plugin '${plugin.id}': $it") }
        val replacement = response.replacement
        if (!response.ok || replacement == null) {
            context.logWarning(
                "converter plugin '${plugin.id}' rejected '$capabilityName' node: ${response.error ?: "no replacement"}",
            )
            return ""
        }
        if (replacement.contentType != "html") {
            context.logWarning(
                "converter plugin '${plugin.id}' returned unsupported contentType '${replacement.contentType}' (converters must return html)",
            )
            return ""
        }
        return replacement.value
    }
}

/**
 * The custom-renderer registrations for every `converter` capability of the
 * plugins loaded in [engine], keyed by capability name — ready to merge into
 * `RenderConfig.customRenderers`.
 */
fun converterRenderers(
    engine: PluginDispatch,
    documentAttributes: Map<String, String> = emptyMap(),
): Map<String, CustomRenderer> = buildMap {
    engine.descriptors()
        .flatMap { it.capabilities }
        .filter { it.type == "converter" }
        .forEach { capability ->
            engine.forCapability("converter", capability.name)?.let { plugin ->
                put(capability.name, WasmConverterRenderer(plugin, capability.name, documentAttributes))
            }
        }
}
