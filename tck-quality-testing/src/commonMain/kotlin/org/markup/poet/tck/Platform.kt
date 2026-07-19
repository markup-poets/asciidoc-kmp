package org.markup.poet.tck

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString
import org.markup.poet.tck.config.ConfigFileOperations
import org.markup.poet.tck.sync.GitOperations
import org.markup.poet.tck.sync.GitResult
import org.markup.poet.tck.version.VersionFileOperations

/*
 * File I/O lives here, once, on top of kotlinx-io's SystemFileSystem — not as
 * expect/actual per target. It used to be per-target, and three of the four
 * actuals silently returned emptyList() from platformFindFiles, so fixture
 * discovery worked on the JVM and nowhere else. A stub cannot hide in a single
 * shared implementation.
 *
 * Only genuinely platform-bound things stay expect/actual below: the platform
 * name, the clock, and git (which needs jgit on the JVM).
 */

/**
 * Get the current platform name.
 */
expect fun getPlatformName(): String

/**
 * Get current time in milliseconds.
 */
expect fun currentTimeMillis(): Long

/**
 * Writes [content] to [path], creating parent directories as needed.
 */
fun platformWriteFile(path: String, content: String) {
    val target = Path(path)
    target.parent?.let { SystemFileSystem.createDirectories(it) }
    SystemFileSystem.sink(target).buffered().use { it.writeString(content) }
}

/**
 * Deletes the directory at [path] and everything under it. A missing path is
 * not an error.
 */
fun platformDeleteDirectory(path: String) {
    val target = Path(path)
    if (!SystemFileSystem.exists(target)) return
    deleteRecursively(target)
}

private fun deleteRecursively(path: Path) {
    if (SystemFileSystem.metadataOrNull(path)?.isDirectory == true) {
        SystemFileSystem.list(path).forEach { deleteRecursively(it) }
    }
    SystemFileSystem.delete(path, mustExist = false)
}

/**
 * Whether a file or directory exists at [path].
 */
fun platformFileExists(path: String): Boolean = SystemFileSystem.exists(Path(path))

/**
 * Whether [path] can actually be read.
 *
 * kotlinx-io exposes no permission bits, so readability is established the only
 * way that is portable and honest: by opening the file. Directories report
 * readable when they can be listed.
 */
fun platformIsReadable(path: String): Boolean {
    val target = Path(path)
    val metadata = SystemFileSystem.metadataOrNull(target) ?: return false
    return try {
        if (metadata.isDirectory) SystemFileSystem.list(target) else SystemFileSystem.source(target).close()
        true
    } catch (e: Exception) {
        false
    }
}

/**
 * Every file under [directory], recursively, whose name ends with [suffix].
 *
 * Returns an empty list when [directory] does not exist or is not a directory.
 */
fun platformFindFiles(directory: String, suffix: String): List<String> {
    val root = Path(directory)
    if (SystemFileSystem.metadataOrNull(root)?.isDirectory != true) return emptyList()
    return buildList { collectFiles(root, suffix, this) }
}

private fun collectFiles(directory: Path, suffix: String, into: MutableList<String>) {
    // list() is a single level, so recursion is ours to do.
    SystemFileSystem.list(directory).sortedBy { it.name }.forEach { entry ->
        if (SystemFileSystem.metadataOrNull(entry)?.isDirectory == true) {
            collectFiles(entry, suffix, into)
        } else if (entry.name.endsWith(suffix)) {
            into.add(entry.toString())
        }
    }
}

/**
 * Reads [path] as UTF-8 text. Throws when the file cannot be read.
 *
 * Callers that want a missing file to be absent rather than fatal should check
 * [platformFileExists] first, as [PlatformConfigFileOperations] does.
 */
fun platformReadFile(path: String): String =
    SystemFileSystem.source(Path(path)).buffered().use { it.readString() }

/**
 * Deletes the file at [path]. A missing file is not an error.
 */
fun platformDeleteFile(path: String) {
    SystemFileSystem.delete(Path(path), mustExist = false)
}

/**
 * Reads and writes the config file.
 *
 * [readFile] returns null for a missing file rather than empty text, so callers
 * fall back to defaults instead of parsing an empty document.
 */
class PlatformConfigFileOperations : ConfigFileOperations {
    override fun readFile(path: String): String? =
        if (platformFileExists(path)) platformReadFile(path) else null

    override fun writeFile(path: String, content: String) {
        platformWriteFile(path, content)
    }
}

/**
 * Reads and writes the version metadata file. [readFile] returns null for a
 * missing file — see [PlatformConfigFileOperations].
 */
class PlatformVersionFileOperations : VersionFileOperations {
    override fun readFile(path: String): String? =
        if (platformFileExists(path)) platformReadFile(path) else null

    override fun writeFile(path: String, content: String) {
        platformWriteFile(path, content)
    }

    override fun deleteFile(path: String) {
        platformDeleteFile(path)
    }

    override fun fileExists(path: String): Boolean = platformFileExists(path)
}

/**
 * Platform-specific implementation of GitOperations.
 *
 * Stays expect/actual: the JVM drives jgit, and no other target has a git
 * implementation.
 */
expect class PlatformGitOperations() : GitOperations {
    override suspend fun clone(url: String, destination: String, branch: String?): GitResult
    override suspend fun pull(repositoryPath: String): GitResult
    override fun getCurrentCommitHash(repositoryPath: String): String?
    override fun getCurrentRef(repositoryPath: String): String?
    override fun isValidRepository(repositoryPath: String): Boolean
    override fun getRemoteUrl(repositoryPath: String, remoteName: String): String?
}
