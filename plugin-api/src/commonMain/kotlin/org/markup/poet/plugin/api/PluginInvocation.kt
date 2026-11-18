package org.markup.poet.plugin.api

import kotlinx.serialization.Serializable

/** Source position of the construct being processed (1-based). */
@Serializable
data class SourcePoint(
    val line: Int,
    val column: Int = 1,
)

/**
 * The JSON envelope the host writes into plugin memory for one `process` call.
 *
 * @param extensionPoint which capability is being invoked (`block`,
 *   `blockMacro`, `inlineMacro`, `converter`)
 * @param name the matched block style / macro name
 * @param attributes the construct's own attributes (positional attributes use
 *   their 1-based index as key)
 * @param content raw block body, macro target, or serialized ASG node JSON for
 *   `converter` invocations
 * @param documentAttributes a copy of the resolved document attributes
 * @param backend the output format the host is producing (`html5` for the HTML
 *   renderer). A plugin whose output only makes sense for some formats checks
 *   it and declines the rest by answering `ok` without a replacement.
 */
@Serializable
data class PluginInvocation(
    val abiVersion: Int = PLUGIN_ABI_VERSION,
    val extensionPoint: String,
    val name: String,
    val attributes: Map<String, String> = emptyMap(),
    val content: String = "",
    val documentAttributes: Map<String, String> = emptyMap(),
    val location: SourcePoint? = null,
    val backend: String = BACKEND_HTML5,
)

/** [PluginInvocation.backend] of the HTML renderer. */
const val BACKEND_HTML5: String = "html5"
