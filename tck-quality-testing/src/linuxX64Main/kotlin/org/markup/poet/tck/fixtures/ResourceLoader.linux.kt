package org.markup.poet.tck.fixtures

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.pointed
import kotlinx.cinterop.toKString
import org.markup.poet.tck.platformFileExists
import org.markup.poet.tck.platformReadFile
import platform.posix.DT_REG
import platform.posix.closedir
import platform.posix.getenv
import platform.posix.opendir
import platform.posix.readdir

/**
 * Linux implementation of ResourceLoader.
 *
 * Native test binaries have no classpath, so resources are read straight from
 * the module's source tree. The repository root is taken from the `TCK_ROOT`
 * environment variable (set by the `linuxX64Test` Gradle task); if unset,
 * paths resolve against the current working directory, which the test task
 * also pins to the repository root.
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

    @OptIn(ExperimentalForeignApi::class)
    actual fun listResources(path: String): List<String> {
        val names = mutableSetOf<String>()
        for (rootDir in resourceRoots) {
            val dir = opendir("$rootDir/$path") ?: continue
            try {
                while (true) {
                    val entry = readdir(dir) ?: break
                    if (entry.pointed.d_type.toInt() == DT_REG) {
                        names.add(entry.pointed.d_name.toKString())
                    }
                }
            } finally {
                closedir(dir)
            }
        }
        return names.toList()
    }
}
