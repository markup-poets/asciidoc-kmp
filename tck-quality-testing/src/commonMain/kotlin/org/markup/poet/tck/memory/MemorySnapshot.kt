package org.markup.poet.tck.memory

/**
 * Memory usage snapshot.
 */
data class MemorySnapshot(
    val timestamp: Long,
    val usedMemory: Long, // bytes
    val totalMemory: Long, // bytes
    val freeMemory: Long // bytes
)
