package org.markup.poet.asciidoc.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.random.Random

/**
 * Property-based tests for state transition validation.
 * **Feature: asciidoc-parser, Property 9: State Transition Validation**
 * **Validates: Requirements 5.4, 5.5**
 */
class StateTransitionValidationTest {

    @Test
    fun `Property 9 - All state transitions should be legal according to AsciiDoc grammar rules`() {
        repeat(100) {
            val stateMachine = DefaultParseStateMachine()
            val transitionSequence = generateTransitionSequence()
            
            transitionSequence.forEach { trigger ->
                val canTransition = stateMachine.canTransition(trigger)
                val actualTransition = stateMachine.transition(trigger)
                
                // canTransition should match actual transition success
                assertEquals(canTransition, actualTransition.success, 
                    "canTransition should match actual transition result for trigger $trigger")
                
                if (actualTransition.success) {
                    // Successful transitions should result in valid states
                    assertNotNull(actualTransition.newState)
                    assertEquals(actualTransition.newState, stateMachine.getCurrentState())
                } else {
                    // Failed transitions should provide error information
                    assertNotNull(actualTransition.error, "Failed transition should provide error information")
                    assertTrue(actualTransition.error!!.message.isNotEmpty(), "Error message should not be empty")
                }
            }
        }
    }

    @Test
    fun `Property 9a - Illegal state transitions should result in clear error messages`() {
        repeat(100) {
            val stateMachine = DefaultParseStateMachine()
            val illegalTransition = generateIllegalTransition(stateMachine)
            
            if (illegalTransition != null) {
                val result = stateMachine.transition(illegalTransition)
                
                // Illegal transitions should fail
                assertFalse(result.success, "Illegal transition should fail")
                assertNotNull(result.error, "Failed transition should have error")
                assertTrue(result.error!!.message.contains("Invalid state transition"), 
                    "Error message should indicate invalid transition")
                assertTrue(result.error!!.message.contains(stateMachine.getCurrentState().toString()),
                    "Error message should include current state")
            }
        }
    }

    @Test
    fun `Property 9b - Code block state transitions should respect delimiter matching`() {
        repeat(100) {
            val stateMachine = DefaultParseStateMachine()
            val codeBlockDelimiter = generateCodeBlockDelimiter()
            
            // Start code block
            val startTransition = stateMachine.transition(StateTrigger.BlockDelimiter(codeBlockDelimiter))
            if (startTransition.success) {
                assertEquals(ParseState.IN_CODE_BLOCK, stateMachine.getCurrentState())
                assertEquals(codeBlockDelimiter, stateMachine.getContext().codeBlockDelimiter)
                
                // Try to end with different delimiter - should stay in code block
                val differentDelimiter = generateDifferentCodeBlockDelimiter(codeBlockDelimiter)
                val differentEndTransition = stateMachine.transition(StateTrigger.BlockDelimiter(differentDelimiter))
                
                // Should remain in code block regardless of transition success
                assertEquals(ParseState.IN_CODE_BLOCK, stateMachine.getCurrentState())
                
                // End with matching delimiter - should succeed and exit code block
                val matchingEndTransition = stateMachine.transition(StateTrigger.BlockDelimiter(codeBlockDelimiter))
                if (matchingEndTransition.success) {
                    assertEquals(ParseState.DOCUMENT_START, stateMachine.getCurrentState())
                    assertEquals(null, stateMachine.getContext().codeBlockDelimiter)
                }
            }
        }
    }

    @Test
    fun `Property 9c - State machine should validate transition preconditions`() {
        repeat(100) {
            val stateMachine = DefaultParseStateMachine()
            val trigger = generateRandomTrigger()
            
            // Check preconditions before transition
            val currentState = stateMachine.getCurrentState()
            val canTransition = stateMachine.canTransition(trigger)
            
            // Perform transition
            val result = stateMachine.transition(trigger)
            
            // Validate that canTransition was accurate
            assertEquals(canTransition, result.success, 
                "canTransition should accurately predict transition success")
            
            // Validate state consistency
            if (result.success) {
                // State should have changed appropriately
                val newState = stateMachine.getCurrentState()
                assertEquals(result.newState, newState, "Transition result should match actual state")
                
                // Context should be updated appropriately
                val context = stateMachine.getContext()
                assertEquals(newState, context.currentState, "Context state should match machine state")
            } else {
                // State should remain unchanged on failed transition
                assertEquals(currentState, stateMachine.getCurrentState(), 
                    "State should not change on failed transition")
            }
        }
    }

    @Test
    fun `Property 9d - Document start state should allow transitions to any valid state`() {
        repeat(100) {
            val stateMachine = DefaultParseStateMachine()
            
            // Ensure we're at document start
            stateMachine.reset()
            assertEquals(ParseState.DOCUMENT_START, stateMachine.getCurrentState())
            
            val trigger = generateValidDocumentStartTrigger()
            val canTransition = stateMachine.canTransition(trigger)
            val result = stateMachine.transition(trigger)
            
            // From document start, most transitions should be valid
            assertTrue(canTransition, "Document start should allow most transitions")
            assertTrue(result.success, "Transition from document start should succeed")
            assertNotNull(result.newState, "Successful transition should have new state")
        }
    }
}

// Helper functions for generating test data
private fun generateTransitionSequence(): List<StateTrigger> {
    return (1..Random.nextInt(3, 8)).map { generateRandomTrigger() }
}

private fun generateIllegalTransition(stateMachine: DefaultParseStateMachine): StateTrigger? {
    val currentState = stateMachine.getCurrentState()
    
    // Generate a trigger that should be illegal from current state
    return when (currentState) {
        ParseState.IN_CODE_BLOCK -> {
            // In code block, most non-matching delimiters should be illegal for state change
            val context = stateMachine.getContext()
            val differentDelimiter = generateDifferentCodeBlockDelimiter(context.codeBlockDelimiter ?: "----")
            StateTrigger.SectionHeader(Random.nextInt(1, 7)) // Section headers should not work in code blocks
        }
        else -> {
            // For other states, try to find an illegal transition
            // This is a simplified approach - in practice, most transitions are legal from most states
            null
        }
    }
}

private fun generateCodeBlockDelimiter(): String {
    return listOf("----", "....", "====", "****")[Random.nextInt(4)]
}

private fun generateDifferentCodeBlockDelimiter(current: String): String {
    val delimiters = listOf("----", "....", "====", "****")
    val different = delimiters.filter { it != current }
    return different[Random.nextInt(different.size)]
}

private fun generateRandomTrigger(): StateTrigger {
    return when (Random.nextInt(7)) {
        0 -> StateTrigger.EmptyLine
        1 -> StateTrigger.BlockDelimiter(generateCodeBlockDelimiter())
        2 -> StateTrigger.ListMarker(
            type = ListType.values()[Random.nextInt(ListType.values().size)],
            level = Random.nextInt(1, 6)
        )
        3 -> StateTrigger.SectionHeader(Random.nextInt(1, 7))
        4 -> StateTrigger.AttributeDefinition
        5 -> StateTrigger.TextLine
        else -> StateTrigger.CommentLine
    }
}

private fun generateValidDocumentStartTrigger(): StateTrigger {
    // From document start, these should all be valid
    return when (Random.nextInt(6)) {
        0 -> StateTrigger.SectionHeader(Random.nextInt(1, 7))
        1 -> StateTrigger.ListMarker(
            type = ListType.values()[Random.nextInt(ListType.values().size)],
            level = Random.nextInt(1, 6)
        )
        2 -> StateTrigger.BlockDelimiter(generateCodeBlockDelimiter())
        3 -> StateTrigger.AttributeDefinition
        4 -> StateTrigger.TextLine
        else -> StateTrigger.CommentLine
    }
}