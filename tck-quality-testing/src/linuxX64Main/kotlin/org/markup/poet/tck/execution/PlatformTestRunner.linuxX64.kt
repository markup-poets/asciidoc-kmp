package org.markup.poet.tck.execution

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.gettimeofday
import platform.posix.timeval

/**
 * Get the current platform name for Linux.
 */
actual fun getPlatformName(): String = "Linux"

/**
 * Get current time in milliseconds for Linux.
 */
@OptIn(ExperimentalForeignApi::class)
actual fun currentTimeMillis(): Long {
    val tv = timeval()
    gettimeofday(tv.ptr, null)
    return tv.tv_sec * 1000L + tv.tv_usec / 1000L
}
