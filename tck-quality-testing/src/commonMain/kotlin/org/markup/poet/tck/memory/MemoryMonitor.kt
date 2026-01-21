package org.markup.poet.tck.memory

/**
 * Monitors memory usage during operations.
 */
interface MemoryMonitor {
    /**
     * Take a memory snapshot.
     */
    fun snapshot(): MemorySnapshot
    
    /**
     * Monitor memory usage during an operation.
     */
    fun monitor(name: String, operation: () -> Unit): MemoryMetrics
    
    /**
     * Force garbage collection (platform-specific).
     */
    fun forceGC()
}

/**
 * Platform-specific memory monitoring.
 */
expect class PlatformMemoryMonitor() : MemoryMonitor {
    override fun snapshot(): MemorySnapshot
    override fun monitor(name: String, operation: () -> Unit): MemoryMetrics
    override fun forceGC()
}
