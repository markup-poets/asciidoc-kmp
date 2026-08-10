package org.markup.poet.tck.fixtures

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import org.markup.poet.tck.platformFileExists
import org.markup.poet.tck.platformReadFile
import platform.posix.getenv

/**
 * Native implementation of ResourceLoader, shared by every native target.
 *
 * Native test binaries have no classpath, so resources are read straight from
 * the module's source tree. The repository root is taken from the `TCK_ROOT`
 * environment variable (set by the native test tasks); if unset, paths resolve
 * against the current working directory, which those tasks also pin to the
 * repository root.
 *
 * Directory listing goes through kotlinx-io rather than per-target `opendir`
 * bindings, which is what lets Linux and Apple share one implementation instead
 * of each carrying its own — and iOS carrying a stub that returned null.
 */
internal actual object ResourceLoader {

    /** Resource roots, in classpath-like precedence order (test before main). */
    @OptIn(ExperimentalForeignApi::class)
    private val resourceRoots: List<String> by lazy {
        val root = getenv("TCK_ROOT")?.toKString()?.trimEnd('/') ?: "."
        listOf(
            "$root/tck-quality-testing/src/commonTest/resources",
            "$root/tck-quality-testing/src/commonMain/resources",
        )
    }

    actual fun readResource(path: String): String? {
        for (rootDir in resourceRoots) {
            val filePath = "$rootDir/$path"
            if (platformFileExists(filePath)) {
                return platformReadFile(filePath)
            }
        }
        return null
    }

    actual fun listResources(path: String): List<String> {
        val names = mutableSetOf<String>()
        for (rootDir in resourceRoots) {
            val directory = Path("$rootDir/$path")
            if (SystemFileSystem.metadataOrNull(directory)?.isDirectory != true) continue
            SystemFileSystem.list(directory).forEach { entry ->
                if (SystemFileSystem.metadataOrNull(entry)?.isRegularFile == true) {
                    names.add(entry.name)
                }
            }
        }
        return names.toList()
    }
}
