package org.markup.poet.asciidoc.render

/**
 * JS (browser) implementation of FileReader.
 *
 * The browser has no filesystem access, so any attempt to read a file
 * (e.g. a custom CSS path) fails. Inline CSS content and built-in themes
 * work as on every other platform.
 */
actual class PlatformFileReader actual constructor() : FileReader {
    actual override fun readFile(path: String): Result<String> =
        Result.failure(
            UnsupportedOperationException(
                "File reading is not available in the browser (requested: $path). " +
                    "Use inline CSS content or a built-in theme instead."
            )
        )
}
