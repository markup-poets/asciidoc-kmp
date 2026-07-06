package org.markup.poet.tck.adapter

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.markup.poet.asciidoc.asg.serialization.AsgDocumentJsonSerializer
import org.markup.poet.asciidoc.parser.asg.BlockTreeParser

/**
 * The message the official TCK harness writes to the adapter's stdin.
 * See asciidoc-tck `harness/lib/adapter-manager.js` (AdapterCliManager).
 */
@Serializable
data class AdapterRequest(
    val contents: String,
    val path: String? = null,
    val type: String,
)

/**
 * Handles one TCK adapter request: parses the AsciiDoc contents and returns
 * the ASG as a JSON string (which the caller must print to stdout, unpolluted).
 */
class Adapter(
    private val emitLocations: Boolean = false,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun handle(requestJson: String): String {
        val request = json.decodeFromString(AdapterRequest.serializer(), requestJson)
        val parser = BlockTreeParser()
        val serializer = AsgDocumentJsonSerializer(emitLocations)
        return when (request.type) {
            "inline" -> serializer.serializeInlines(parser.parseInline(request.contents))
            else -> serializer.serializeDocument(parser.parseDocument(request.contents))
        }
    }
}
