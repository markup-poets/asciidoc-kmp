package org.markup.poet.tck.memory

/**
 * Memory usage metrics for an operation.
 */
data class MemoryMetrics(
    val operationName: String,
    val before: MemorySnapshot,
    val after: MemorySnapshot,
    val peak: Long, // bytes
    val allocated: Long, // bytes
    val leakDetected: Boolean
)
