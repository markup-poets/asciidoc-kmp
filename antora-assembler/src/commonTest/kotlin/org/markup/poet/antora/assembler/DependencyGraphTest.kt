package org.markup.poet.antora.assembler

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DependencyGraphTest {
    
    @Test
    fun `should detect simple A to B to A cycle`() {
        // Arrange: A -> B -> A
        val nodes = mapOf(
            "A" to DependencyNode("A", listOf("B"), null),
            "B" to DependencyNode("B", listOf("A"), null)
        )
        val graph = DependencyGraph(nodes, "A")
        
        // Act
        val cycles = graph.detectCycles()
        
        // Assert
        assertEquals(1, cycles.size, "Should detect exactly one cycle")
        val cycle = cycles.first()
        assertEquals(3, cycle.files.size, "Cycle should have 3 elements (A -> B -> A)")
        assertEquals("A", cycle.files.first(), "Cycle should start with A")
        assertEquals("A", cycle.files.last(), "Cycle should end with A")
        assertTrue(cycle.files.contains("B"), "Cycle should contain B")
    }
    
    @Test
    fun `should detect three-node cycle A to B to C to A`() {
        // Arrange: A -> B -> C -> A
        val nodes = mapOf(
            "A" to DependencyNode("A", listOf("B"), null),
            "B" to DependencyNode("B", listOf("C"), null),
            "C" to DependencyNode("C", listOf("A"), null)
        )
        val graph = DependencyGraph(nodes, "A")
        
        // Act
        val cycles = graph.detectCycles()
        
        // Assert
        assertEquals(1, cycles.size, "Should detect exactly one cycle")
        val cycle = cycles.first()
        assertEquals(4, cycle.files.size, "Cycle should have 4 elements (A -> B -> C -> A)")
        assertEquals("A", cycle.files.first(), "Cycle should start with A")
        assertEquals("A", cycle.files.last(), "Cycle should end with A")
        assertTrue(cycle.files.contains("B"), "Cycle should contain B")
        assertTrue(cycle.files.contains("C"), "Cycle should contain C")
    }
    
    @Test
    fun `should detect multiple independent cycles`() {
        // Arrange: Two separate cycles: A -> B -> A and C -> D -> C
        val nodes = mapOf(
            "A" to DependencyNode("A", listOf("B"), null),
            "B" to DependencyNode("B", listOf("A"), null),
            "C" to DependencyNode("C", listOf("D"), null),
            "D" to DependencyNode("D", listOf("C"), null)
        )
        val graph = DependencyGraph(nodes, "A")
        
        // Act
        val cycles = graph.detectCycles()
        
        // Assert
        assertEquals(2, cycles.size, "Should detect two cycles")
    }
    
    @Test
    fun `should detect complex multi-file cycle`() {
        // Arrange: A -> B -> C -> D -> B (cycle starts at B)
        val nodes = mapOf(
            "A" to DependencyNode("A", listOf("B"), null),
            "B" to DependencyNode("B", listOf("C"), null),
            "C" to DependencyNode("C", listOf("D"), null),
            "D" to DependencyNode("D", listOf("B"), null)
        )
        val graph = DependencyGraph(nodes, "A")
        
        // Act
        val cycles = graph.detectCycles()
        
        // Assert
        assertEquals(1, cycles.size, "Should detect exactly one cycle")
        val cycle = cycles.first()
        assertTrue(cycle.files.size >= 3, "Cycle should have at least 3 elements")
        assertEquals(cycle.files.first(), cycle.files.last(), "Cycle should start and end with same node")
    }
    
    @Test
    fun `should return empty list for acyclic graph`() {
        // Arrange: A -> B -> C (no cycle)
        val nodes = mapOf(
            "A" to DependencyNode("A", listOf("B"), null),
            "B" to DependencyNode("B", listOf("C"), null),
            "C" to DependencyNode("C", emptyList(), null)
        )
        val graph = DependencyGraph(nodes, "A")
        
        // Act
        val cycles = graph.detectCycles()
        
        // Assert
        assertTrue(cycles.isEmpty(), "Should not detect any cycles in acyclic graph")
    }
    
    @Test
    fun `should return empty list for single node with no dependencies`() {
        // Arrange: A (no dependencies)
        val nodes = mapOf(
            "A" to DependencyNode("A", emptyList(), null)
        )
        val graph = DependencyGraph(nodes, "A")
        
        // Act
        val cycles = graph.detectCycles()
        
        // Assert
        assertTrue(cycles.isEmpty(), "Should not detect any cycles for single node")
    }
    
    @Test
    fun `should return empty list for empty graph`() {
        // Arrange: Empty graph
        val nodes = emptyMap<String, DependencyNode>()
        val graph = DependencyGraph(nodes, "")
        
        // Act
        val cycles = graph.detectCycles()
        
        // Assert
        assertTrue(cycles.isEmpty(), "Should not detect any cycles in empty graph")
    }
    
    @Test
    fun `topological sort should order dependencies before dependents`() {
        // Arrange: A -> B -> C (linear dependency chain)
        val nodes = mapOf(
            "A" to DependencyNode("A", listOf("B"), null),
            "B" to DependencyNode("B", listOf("C"), null),
            "C" to DependencyNode("C", emptyList(), null)
        )
        val graph = DependencyGraph(nodes, "A")
        
        // Act
        val sorted = graph.topologicalSort()
        
        // Assert
        assertEquals(3, sorted.size, "Should include all nodes")
        val indexA = sorted.indexOf("A")
        val indexB = sorted.indexOf("B")
        val indexC = sorted.indexOf("C")
        assertTrue(indexC < indexB, "C should come before B (B depends on C)")
        assertTrue(indexB < indexA, "B should come before A (A depends on B)")
    }
    
    @Test
    fun `topological sort should handle diamond dependency`() {
        // Arrange: A -> B, A -> C, B -> D, C -> D (diamond shape)
        val nodes = mapOf(
            "A" to DependencyNode("A", listOf("B", "C"), null),
            "B" to DependencyNode("B", listOf("D"), null),
            "C" to DependencyNode("C", listOf("D"), null),
            "D" to DependencyNode("D", emptyList(), null)
        )
        val graph = DependencyGraph(nodes, "A")
        
        // Act
        val sorted = graph.topologicalSort()
        
        // Assert
        assertEquals(4, sorted.size, "Should include all nodes")
        val indexA = sorted.indexOf("A")
        val indexB = sorted.indexOf("B")
        val indexC = sorted.indexOf("C")
        val indexD = sorted.indexOf("D")
        assertTrue(indexD < indexB, "D should come before B")
        assertTrue(indexD < indexC, "D should come before C")
        assertTrue(indexB < indexA, "B should come before A")
        assertTrue(indexC < indexA, "C should come before A")
    }
    
    @Test
    fun `topological sort should handle graph with multiple roots`() {
        // Arrange: A -> C, B -> C (two roots, one shared dependency)
        val nodes = mapOf(
            "A" to DependencyNode("A", listOf("C"), null),
            "B" to DependencyNode("B", listOf("C"), null),
            "C" to DependencyNode("C", emptyList(), null)
        )
        val graph = DependencyGraph(nodes, "A")
        
        // Act
        val sorted = graph.topologicalSort()
        
        // Assert
        assertEquals(3, sorted.size, "Should include all nodes")
        val indexC = sorted.indexOf("C")
        val indexA = sorted.indexOf("A")
        val indexB = sorted.indexOf("B")
        assertTrue(indexC < indexA, "C should come before A")
        assertTrue(indexC < indexB, "C should come before B")
    }
    
    @Test
    fun `topological sort should return empty list for empty graph`() {
        // Arrange: Empty graph
        val nodes = emptyMap<String, DependencyNode>()
        val graph = DependencyGraph(nodes, "")
        
        // Act
        val sorted = graph.topologicalSort()
        
        // Assert
        assertTrue(sorted.isEmpty(), "Should return empty list for empty graph")
    }
    
    @Test
    fun `topological sort should handle single node`() {
        // Arrange: A (no dependencies)
        val nodes = mapOf(
            "A" to DependencyNode("A", emptyList(), null)
        )
        val graph = DependencyGraph(nodes, "A")
        
        // Act
        val sorted = graph.topologicalSort()
        
        // Assert
        assertEquals(1, sorted.size, "Should include the single node")
        assertEquals("A", sorted.first(), "Should return the single node")
    }
    
    @Test
    fun `topological sort should handle graph with cycle gracefully`() {
        // Arrange: A -> B -> A (cycle)
        val nodes = mapOf(
            "A" to DependencyNode("A", listOf("B"), null),
            "B" to DependencyNode("B", listOf("A"), null)
        )
        val graph = DependencyGraph(nodes, "A")
        
        // Act
        val sorted = graph.topologicalSort()
        
        // Assert
        // Should still return nodes, even if there's a cycle
        assertEquals(2, sorted.size, "Should include all nodes even with cycle")
    }
    
    @Test
    fun `DependencyCycle toString should format correctly`() {
        // Arrange
        val cycle = DependencyCycle(listOf("A", "B", "C", "A"))
        
        // Act
        val result = cycle.toString()
        
        // Assert
        assertEquals("A -> B -> C -> A", result, "Should format cycle with arrows")
    }
}
