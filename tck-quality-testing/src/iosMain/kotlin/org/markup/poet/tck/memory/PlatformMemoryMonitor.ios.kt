package org.markup.poet.tck.memory

/**
 * iOS implementation of memory monitoring (stub for now).
 * 
 * Note: Full implementation would require platform-specific APIs.
 */
actual class PlatformMemoryMonitor actual constructor() : MemoryMonitor {
    actual override fun snapshot(): MemorySnapshot {
        // Stub implementation - returns placeholder values
        return MemorySnapshot(
            timestamp = 0L, // Placeholder - would need platform-specific implementation
            usedMemory = 0L,
            totalMemory = 0L,
            freeMemory = 0L
        )
    }
    
    actual override fun monitor(name: String, operation: () -> Unit): MemoryMetrics {
        val before = snapshot()
        
        operation()
        
        val after = snapshot()
        
        return MemoryMetrics(
            operationName = name,
            before = before,
            after = after,
            peak = 0L,
            allocated = 0L,
            leakDetected = false
        )
    }
    
    actual override fun forceGC() {
        // No-op on iOS (automatic reference counting)
    }
}
