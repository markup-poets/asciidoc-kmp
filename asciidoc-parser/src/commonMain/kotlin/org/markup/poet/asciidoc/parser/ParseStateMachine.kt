package org.markup.poet.asciidoc.parser

import org.markup.poet.asciidoc.error.ParseError

/**
 * Interface for managing parsing context and state transitions during AsciiDoc parsing.
 * The state machine tracks the current parsing context (e.g., inside list, code block)
 * and validates transitions between different parsing states.
 */
interface ParseStateMachine {
    /**
     * Get the current parsing state.
     */
    fun getCurrentState(): ParseState
    
    /**
     * Attempt to transition to a new state based on the given trigger.
     * Returns the result of the transition attempt.
     */
    fun transition(trigger: StateTrigger): StateTransition
    
    /**
     * Check if a transition is valid without actually performing it.
     */
    fun canTransition(trigger: StateTrigger): Boolean
    
    /**
     * Reset the state machine to its initial state.
     */
    fun reset()
    
    /**
     * Get the current parsing context information.
     */
    fun getContext(): StateMachineContext
}

/**
 * Enumeration of possible parsing states.
 */
enum class ParseState {
    /** Initial state at the start of document parsing */
    DOCUMENT_START,
    
    /** Currently parsing a paragraph */
    IN_PARAGRAPH,
    
    /** Currently parsing a list structure */
    IN_LIST,
    
    /** Currently parsing a code block */
    IN_CODE_BLOCK,
    
    /** Currently parsing a section */
    IN_SECTION,
    
    /** Currently parsing document attributes */
    IN_ATTRIBUTES
}

/**
 * Sealed class representing triggers that can cause state transitions.
 */
sealed class StateTrigger {
    /** Empty line encountered */
    object EmptyLine : StateTrigger()
    
    /** Block delimiter encountered (e.g., ----) */
    data class BlockDelimiter(val type: String) : StateTrigger()
    
    /** List marker encountered */
    data class ListMarker(val type: ListType, val level: Int) : StateTrigger()
    
    /** Section header encountered */
    data class SectionHeader(val level: Int) : StateTrigger()
    
    /** Attribute definition encountered */
    object AttributeDefinition : StateTrigger()
    
    /** Regular text line encountered */
    object TextLine : StateTrigger()
    
    /** Comment line encountered */
    object CommentLine : StateTrigger()

    /** Include directive encountered */
    object IncludeDirective : StateTrigger()
    
    /** Block attribute encountered (e.g., [source,kotlin]) */
    data class BlockAttribute(val attribute: String) : StateTrigger()
}

/**
 * Enumeration of list types.
 */
enum class ListType { 
    UNORDERED, 
    ORDERED, 
    DEFINITION 
}

/**
 * Result of a state transition attempt.
 */
data class StateTransition(
    val success: Boolean,
    val newState: ParseState,
    val error: ParseError? = null
)

/**
 * Extended context information for state machine parsing.
 */
data class StateMachineContext(
    val currentState: ParseState,
    val listLevel: Int = 0,
    val listType: ListType? = null,
    val codeBlockDelimiter: String? = null,
    val sectionLevel: Int = 0,
    val lineNumber: Int = 0
)