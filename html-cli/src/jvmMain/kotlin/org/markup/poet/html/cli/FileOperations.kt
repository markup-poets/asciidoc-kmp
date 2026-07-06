package org.markup.poet.html.cli

import java.io.File
import kotlin.system.exitProcess as kotlinExitProcess

actual fun readFileContent(path: String): String {
    return File(path).readText()
}

actual fun readFileBytes(path: String): ByteArray {
    return File(path).readBytes()
}

actual fun writeFileContent(path: String, content: String) {
    File(path).writeText(content)
}

actual fun fileExists(path: String): Boolean {
    return File(path).exists()
}

actual fun exitProcess(code: Int): Nothing {
    kotlinExitProcess(code)
}
