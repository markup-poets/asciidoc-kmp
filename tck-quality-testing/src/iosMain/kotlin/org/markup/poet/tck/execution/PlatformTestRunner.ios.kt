package org.markup.poet.tck.execution

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual fun getPlatformName(): String = "iOS"

actual fun currentTimeMillis(): Long {
    return (NSDate().timeIntervalSince1970 * 1000).toLong()
}
