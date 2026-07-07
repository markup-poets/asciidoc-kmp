package org.markup.poet.asciidoc.render

/**
 * JS (browser) implementation of FileWriter.
 *
 * The browser has no filesystem access; rendering to a string is the only
 * supported output mode on this platform.
 */
actual class PlatformFileWriter actual constructor() : FileWriter {
    actual override fun writeFile(path: String, content: String): Result<Unit> =
        Result.failure(
            UnsupportedOperationException(
                "File writing is not available in the browser (requested: $path)."
            )
        )
}
