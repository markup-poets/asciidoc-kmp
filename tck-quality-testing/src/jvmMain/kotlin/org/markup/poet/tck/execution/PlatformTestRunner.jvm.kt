package org.markup.poet.tck.execution

actual fun getPlatformName(): String = "JVM"

actual fun currentTimeMillis(): Long = System.currentTimeMillis()
