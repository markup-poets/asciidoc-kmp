package org.markup.poet.asciidoc.parser

import org.markup.poet.asciidoc.ast.SourceLocation
import org.markup.poet.asciidoc.error.ParseError
import org.markup.poet.asciidoc.error.ErrorSeverity

/**
 * Default implementation of ParseStateMachine that manages parsing context
 * and validates state transitions according to AsciiDoc grammar rules.
 */
class DefaultParseStateMachine : ParseStateMachine {
    private var context = StateMachineContext(ParseState.DOCUMENT_START)
    
    override fun getCurrentState(): ParseState = context.currentState
    
    override fun getContext(): StateMachineContext = context
    
    override fun transition(trigger: StateTrigger): StateTransition {
        val newState = determineNewState(trigger)
        val isValid = isValidTransition(context.currentState, newState, trigger)
        
        return if (isValid) {
            val newContext = updateContext(newState, trigger)
            context = newContext
            StateTransition(success = true, newState = newState)
        } else {
            val error = createTransitionError(context.currentState, newState, trigger)
            StateTransition(success = false, newState = context.currentState, error = error)
        }
    }
    
    override fun canTransition(trigger: StateTrigger): Boolean {
        val newState = determineNewState(trigger)
        return isValidTransition(context.currentState, newState, trigger)
    }
    
    override fun reset() {
        context = StateMachineContext(ParseState.DOCUMENT_START)
    }
    
    private fun determineNewState(trigger: StateTrigger): ParseState {
        return when (trigger) {
            is StateTrigger.EmptyLine -> {
                when (context.currentState) {
                    ParseState.IN_CODE_BLOCK -> ParseState.IN_CODE_BLOCK // Stay in code block
                    else -> ParseState.DOCUMENT_START // Return to document start
                }
            }
            is StateTrigger.BlockDelimiter -> {
                when (context.currentState) {
                    ParseState.IN_CODE_BLOCK -> {
                        if (trigger.type == context.codeBlockDelimiter) {
                            ParseState.DOCUMENT_START // End code block
                        } else if (trigger.type.startsWith("----") && context.codeBlockDelimiter?.startsWith("----") == true) {
                            ParseState.DOCUMENT_START // End code block (handling different lengths of ----)
                        } else if (trigger.type.startsWith("[") && context.currentState != ParseState.IN_CODE_BLOCK) {
                            ParseState.IN_CODE_BLOCK
                        } else if (trigger.type.startsWith("----") && context.codeBlockDelimiter?.startsWith("[") == true) {
                             ParseState.IN_CODE_BLOCK // Keep it going, the ---- is the real start
                        } else {
                            ParseState.IN_CODE_BLOCK // Stay in code block
                        }
                    }
                    else -> ParseState.IN_CODE_BLOCK // Start code block
                }
            }
            is StateTrigger.ListMarker -> ParseState.IN_LIST
            is StateTrigger.SectionHeader -> ParseState.IN_SECTION
            is StateTrigger.AttributeDefinition -> ParseState.IN_ATTRIBUTES
            is StateTrigger.TextLine -> {
                when (context.currentState) {
                    ParseState.IN_CODE_BLOCK -> ParseState.IN_CODE_BLOCK // Stay in code block
                    ParseState.IN_LIST -> ParseState.IN_LIST // Continue list
                    else -> ParseState.IN_PARAGRAPH // Start or continue paragraph
                }
            }
            is StateTrigger.CommentLine -> context.currentState // Comments don't change state
            is StateTrigger.IncludeDirective -> context.currentState // Includes don't change state
        }
    }
    
    private fun isValidTransition(
        currentState: ParseState, 
        newState: ParseState, 
        trigger: StateTrigger
    ): Boolean {
        return when (currentState) {
            ParseState.DOCUMENT_START -> true // Can transition to any state from document start
            
            ParseState.IN_PARAGRAPH -> when (newState) {
                ParseState.DOCUMENT_START -> trigger is StateTrigger.EmptyLine
                ParseState.IN_SECTION -> trigger is StateTrigger.SectionHeader
                ParseState.IN_LIST -> trigger is StateTrigger.ListMarker
                ParseState.IN_CODE_BLOCK -> trigger is StateTrigger.BlockDelimiter
                ParseState.IN_ATTRIBUTES -> trigger is StateTrigger.AttributeDefinition
                ParseState.IN_PARAGRAPH -> trigger is StateTrigger.TextLine
            }
            
            ParseState.IN_LIST -> when (newState) {
                ParseState.DOCUMENT_START -> trigger is StateTrigger.EmptyLine
                ParseState.IN_SECTION -> trigger is StateTrigger.SectionHeader
                ParseState.IN_LIST -> trigger is StateTrigger.ListMarker || trigger is StateTrigger.TextLine
                ParseState.IN_CODE_BLOCK -> trigger is StateTrigger.BlockDelimiter
                ParseState.IN_PARAGRAPH -> trigger is StateTrigger.TextLine
                else -> false
            }
            
            ParseState.IN_CODE_BLOCK -> when (newState) {
                ParseState.DOCUMENT_START -> {
                    trigger is StateTrigger.BlockDelimiter && 
                    trigger.type == context.codeBlockDelimiter
                }
                ParseState.IN_CODE_BLOCK -> true // Any content is valid inside code block
                else -> false
            }
            
            ParseState.IN_SECTION -> when (newState) {
                ParseState.DOCUMENT_START -> trigger is StateTrigger.EmptyLine
                ParseState.IN_SECTION -> trigger is StateTrigger.SectionHeader
                ParseState.IN_PARAGRAPH -> trigger is StateTrigger.TextLine
                ParseState.IN_LIST -> trigger is StateTrigger.ListMarker
                ParseState.IN_CODE_BLOCK -> trigger is StateTrigger.BlockDelimiter
                ParseState.IN_ATTRIBUTES -> trigger is StateTrigger.AttributeDefinition
            }
            
            ParseState.IN_ATTRIBUTES -> when (newState) {
                ParseState.DOCUMENT_START -> trigger is StateTrigger.EmptyLine
                ParseState.IN_ATTRIBUTES -> trigger is StateTrigger.AttributeDefinition
                ParseState.IN_SECTION -> trigger is StateTrigger.SectionHeader
                ParseState.IN_PARAGRAPH -> trigger is StateTrigger.TextLine
                ParseState.IN_LIST -> trigger is StateTrigger.ListMarker
                ParseState.IN_CODE_BLOCK -> trigger is StateTrigger.BlockDelimiter
            }
        }
    }
    
    private fun updateContext(newState: ParseState, trigger: StateTrigger): StateMachineContext {
        return when (trigger) {
            is StateTrigger.ListMarker -> context.copy(
                currentState = newState,
                listLevel = trigger.level,
                listType = trigger.type
            )
            is StateTrigger.SectionHeader -> context.copy(
                currentState = newState,
                sectionLevel = trigger.level
            )
            is StateTrigger.BlockDelimiter -> {
                if (newState == ParseState.IN_CODE_BLOCK && context.currentState != ParseState.IN_CODE_BLOCK) {
                    // Starting a new code block
                    context.copy(
                        currentState = newState,
                        codeBlockDelimiter = trigger.type
                    )
                } else if (newState == ParseState.IN_CODE_BLOCK && context.currentState == ParseState.IN_CODE_BLOCK && context.codeBlockDelimiter?.startsWith("[") == true && trigger.type.startsWith("----")) {
                    // Transitioning from [source] to ----
                    context.copy(
                        currentState = newState,
                        codeBlockDelimiter = trigger.type
                    )
                } else if (newState == ParseState.DOCUMENT_START) {
                    // Ending a code block
                    context.copy(
                        currentState = newState,
                        codeBlockDelimiter = null
                    )
                } else {
                    // Staying in code block with different delimiter - don't change delimiter
                    context.copy(currentState = newState)
                }
            }
            else -> context.copy(currentState = newState)
        }
    }
    
    private fun createTransitionError(
        currentState: ParseState, 
        attemptedState: ParseState, 
        trigger: StateTrigger
    ): ParseError {
        val message = "Invalid state transition from $currentState to $attemptedState " +
                     "triggered by $trigger at line ${context.lineNumber}"
        
        return ParseError(
            message = message,
            location = SourceLocation(context.lineNumber),
            severity = ErrorSeverity.ERROR
        )
    }
}