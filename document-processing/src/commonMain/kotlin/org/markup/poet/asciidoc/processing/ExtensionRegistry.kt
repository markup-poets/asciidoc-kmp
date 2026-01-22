package org.markup.poet.asciidoc.processing

/**
 * Registry for managing custom processors in the document processing pipeline.
 * Allows registration, unregistration, and retrieval of custom processors.
 */
interface ExtensionRegistry {
    /**
     * Register a custom processor for a specific phase.
     * If a processor with the same name already exists, it will be replaced.
     * 
     * @param processor The custom processor to register
     * @param phase The processing phase where this processor should execute
     */
    fun register(processor: CustomProcessor, phase: ProcessingPhase)
    
    /**
     * Register a custom processor for multiple phases.
     * 
     * @param processor The custom processor to register
     * @param phases The processing phases where this processor should execute
     */
    fun register(processor: CustomProcessor, phases: Set<ProcessingPhase>)
    
    /**
     * Unregister a custom processor by name.
     * Removes the processor from all phases.
     * 
     * @param name The name of the processor to unregister
     * @return true if a processor was unregistered, false if no processor with that name existed
     */
    fun unregister(name: String): Boolean
    
    /**
     * Get all processors registered for a specific phase, sorted by priority.
     * Higher priority processors appear first in the list.
     * 
     * @param phase The processing phase
     * @return List of processors for the phase, sorted by priority (highest first)
     */
    fun getProcessors(phase: ProcessingPhase): List<CustomProcessor>
    
    /**
     * Get all registered processors across all phases.
     * 
     * @return Map of phase to list of processors
     */
    fun getAllProcessors(): Map<ProcessingPhase, List<CustomProcessor>>
    
    /**
     * Check if a processor with the given name is registered.
     * 
     * @param name The processor name
     * @return true if registered, false otherwise
     */
    fun isRegistered(name: String): Boolean
    
    /**
     * Clear all registered processors.
     */
    fun clear()
}

/**
 * Default implementation of ExtensionRegistry.
 */
class DefaultExtensionRegistry : ExtensionRegistry {
    // Map of processor name to (processor, set of phases)
    private val processors = mutableMapOf<String, Pair<CustomProcessor, MutableSet<ProcessingPhase>>>()
    
    override fun register(processor: CustomProcessor, phase: ProcessingPhase) {
        // Replace existing processor completely if same name
        processors[processor.name] = processor to mutableSetOf(phase)
    }
    
    override fun register(processor: CustomProcessor, phases: Set<ProcessingPhase>) {
        // Replace existing processor completely if same name
        processors[processor.name] = processor to phases.toMutableSet()
    }
    
    override fun unregister(name: String): Boolean {
        return processors.remove(name) != null
    }
    
    override fun getProcessors(phase: ProcessingPhase): List<CustomProcessor> {
        return processors.values
            .filter { (_, phases) -> phase in phases }
            .map { (processor, _) -> processor }
            .sortedByDescending { it.priority.value }
    }
    
    override fun getAllProcessors(): Map<ProcessingPhase, List<CustomProcessor>> {
        val result = mutableMapOf<ProcessingPhase, MutableList<CustomProcessor>>()
        
        for ((processor, phases) in processors.values) {
            for (phase in phases) {
                result.getOrPut(phase) { mutableListOf() }.add(processor)
            }
        }
        
        // Sort each phase's processors by priority
        return result.mapValues { (_, procs) ->
            procs.sortedByDescending { it.priority.value }
        }
    }
    
    override fun isRegistered(name: String): Boolean {
        return processors.containsKey(name)
    }
    
    override fun clear() {
        processors.clear()
    }
}
