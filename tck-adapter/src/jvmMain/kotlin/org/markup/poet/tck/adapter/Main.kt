package org.markup.poet.tck.adapter

import kotlin.system.exitProcess

/**
 * Official AsciiDoc TCK adapter entry point.
 *
 * The harness spawns one process per test, writes a JSON request to stdin,
 * and parses the entire stdout as the ASG JSON. Therefore stdout must contain
 * nothing but the response; all diagnostics go to stderr.
 */
fun main() {
    try {
        val requestJson = System.`in`.readBytes().decodeToString()
        val emitLocations = System.getenv("TCK_ADAPTER_LOCATIONS") != "false"
        print(Adapter(emitLocations).handle(requestJson))
        System.out.flush()
        exitProcess(0)
    } catch (e: Throwable) {
        System.err.println("tck-adapter failed: ${e.message}")
        e.printStackTrace()
        exitProcess(1)
    }
}
