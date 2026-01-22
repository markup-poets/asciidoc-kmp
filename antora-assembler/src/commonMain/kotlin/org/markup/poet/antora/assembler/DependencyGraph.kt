package org.markup.poet.antora.assembler

/**
 * Represents the dependency graph of included files.
 * Tracks which files include which other files.
 */
data class DependencyGraph(
    val nodes: Map<String, DependencyNode>,
    val root: String
) {
    /**
     * Detect circular dependencies in the graph.
     * Returns a list of dependency cycles if any exist.
     */
    fun detectCycles(): List<DependencyCycle> {
        val cycles = mutableListOf<DependencyCycle>()
        val visited = mutableSetOf<String>()
        val recursionStack = mutableSetOf<String>()
        val pathStack = mutableListOf<String>()
        
        // Visit each node to find all cycles
        for (node in nodes.keys) {
            if (node !in visited) {
                detectCyclesHelper(node, visited, recursionStack, pathStack, cycles)
            }
        }
        
        return cycles
    }
    
    private fun detectCyclesHelper(
        current: String,
        visited: MutableSet<String>,
        recursionStack: MutableSet<String>,
        pathStack: MutableList<String>,
        cycles: MutableList<DependencyCycle>
    ) {
        visited.add(current)
        recursionStack.add(current)
        pathStack.add(current)
        
        val node = nodes[current]
        if (node != null) {
            for (dependency in node.dependencies) {
                if (dependency !in visited) {
                    // Continue DFS
                    detectCyclesHelper(dependency, visited, recursionStack, pathStack, cycles)
                } else if (dependency in recursionStack) {
                    // Found a cycle - extract the cycle from pathStack
                    val cycleStartIndex = pathStack.indexOf(dependency)
                    if (cycleStartIndex >= 0) {
                        val cycleFiles = pathStack.subList(cycleStartIndex, pathStack.size).toMutableList()
                        // Add the dependency again to close the cycle
                        cycleFiles.add(dependency)
                        cycles.add(DependencyCycle(cycleFiles))
                    }
                }
            }
        }
        
        // Backtrack
        recursionStack.remove(current)
        pathStack.removeAt(pathStack.size - 1)
    }
    
    /**
     * Get all files in topological order (dependencies before dependents).
     */
    fun topologicalSort(): List<String> {
        val result = mutableListOf<String>()
        val visited = mutableSetOf<String>()
        val tempMarked = mutableSetOf<String>()
        
        // Visit each node
        for (node in nodes.keys) {
            if (node !in visited) {
                topologicalSortHelper(node, visited, tempMarked, result)
            }
        }
        
        // Result is already in correct order (dependencies before dependents)
        return result
    }
    
    private fun topologicalSortHelper(
        current: String,
        visited: MutableSet<String>,
        tempMarked: MutableSet<String>,
        result: MutableList<String>
    ) {
        if (current in tempMarked) {
            // Cycle detected - skip this node
            return
        }
        
        if (current in visited) {
            return
        }
        
        tempMarked.add(current)
        
        val node = nodes[current]
        if (node != null) {
            for (dependency in node.dependencies) {
                topologicalSortHelper(dependency, visited, tempMarked, result)
            }
        }
        
        tempMarked.remove(current)
        visited.add(current)
        result.add(current)
    }
}

data class DependencyNode(
    val filePath: String,
    val dependencies: List<String>,
    val sourceLocation: SourceLocation?
)

data class SourceLocation(
    val filePath: String,
    val lineNumber: Int
)

data class DependencyCycle(
    val files: List<String>
) {
    override fun toString(): String = files.joinToString(" -> ")
}
