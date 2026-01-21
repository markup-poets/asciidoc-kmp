package org.markup.poet.tck.memory

/**
 * Android implementation of memory monitoring (reuses JVM Runtime).
 */
actual class PlatformMemoryMonitor actual constructor() : MemoryMonitor {
    actual override fun snapshot(): MemorySnapshot {
        val runtime = Runtime.getRuntime()
        return MemorySnapshot(
            timestamp = System.currentTimeMillis(),
            usedMemory = runtime.totalMemory() - runtime.freeMemory(),
            totalMemory = runtime.totalMemory(),
            freeMemory = runtime.freeMemory()
        )
    }
    
    actual override fun monitor(name: String, operation: () -> Unit): MemoryMetrics {
        forceGC()
        val before = snapshot()
        
        operation()
        
        forceGC()
        val after = snapshot()
        
        val allocated = after.usedMemory - before.usedMemory
        val peak = maxOf(before.usedMemory, after.usedMemory)
        val leakDetected = allocated > 0 && allocated > (before.usedMemory * 0.1) // Simple heuristic
        
        return MemoryMetrics(
            operationName = name,
            before = before,
            after = after,
            peak = peak,
            allocated = allocated,
            leakDetected = leakDetected
        )
    }
    
    actual override fun forceGC() {
        System.gc()
        Thread.sleep(100) // Give GC time to run
    }
}
