# Implementation Plan: AsciiDoc Core Parser

## Overview

This implementation plan breaks down the AsciiDoc Core Parser into discrete, incremental coding tasks. Each task builds upon previous work and includes testing to validate functionality early. The approach follows the layered architecture defined in the design, starting with core data models and building up to the complete parsing system.

## Tasks

- [x] 1. Set up project structure and core data models
  - Create Kotlin Multiplatform module structure
  - Define AST node hierarchy (Document, Section, Paragraph, List, etc.)
  - Implement SourceLocation and error/warning data classes
  - Set up Kotest framework for property-based testing
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 7.1, 7.4_

- [x] 1.1 Write property test for AST structural integrity
  - **Property 2: AST Structural Integrity**
  - **Validates: Requirements 2.1, 2.7**

- [x] 2. Implement line processing and block type detection
  - Create LineProcessor interface and implementation
  - Implement block type detection (sections, lists, code blocks, paragraphs)
  - Add empty line and whitespace-only line handling
  - Implement line number tracking
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [x] 2.1 Write property test for line processing integrity
  - **Property 1: Line Processing Integrity**
  - **Validates: Requirements 1.1, 1.2, 1.5**

- [x] 2.2 Write property test for empty line block separation
  - **Property 6: Empty Line Block Separation**
  - **Validates: Requirements 1.3, 1.4**

- [x] 3. Implement state machine for parsing context
  - Create ParseStateMachine interface and implementation
  - Define ParseState enum and StateTrigger sealed class
  - Implement state transition validation
  - Add context preservation for multi-line elements
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [x] 3.1 Write property test for state machine context preservation
  - **Property 8: State Machine Context Preservation**
  - **Validates: Requirements 5.1, 5.2, 5.3**

- [x] 3.2 Write property test for state transition validation
  - **Property 9: State Transition Validation**
  - **Validates: Requirements 5.4, 5.5**

- [x] 4. Implement block parser for document structure
  - Create BlockParser interface and implementation
  - Implement section header parsing with level detection
  - Add list parsing (ordered and unordered) with nesting support
  - Implement code block parsing with delimiter handling
  - Add paragraph and comment parsing
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_

- [x] 4.1 Write property test for block element recognition
  - **Property 4: Block Element Recognition**
  - **Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6**

- [x] 4.2 Write property test for node type assignment correctness
  - **Property 3: Node Type Assignment Correctness**
  - **Validates: Requirements 2.2, 2.3, 2.4, 2.5, 2.6**

- [x] 5. Checkpoint - Ensure block parsing tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. Implement inline parser for text markup
  - Create InlineParser interface and implementation
  - Implement strong (*bold*) and emphasis (_italic_) parsing
  - Add inline code (`code`) parsing
  - Implement link (link:url[text]) and image (image:path[alt]) parsing
  - Handle nested and overlapping markup with precedence rules
  - Add escape sequence handling for literal text
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7_

- [x] 6.1 Write property test for inline element recognition
  - **Property 5: Inline Element Recognition**
  - **Validates: Requirements 4.1, 4.2, 4.3, 4.4, 4.5**

- [ ] 6.2 Enable and fix property test for inline markup precedence and escaping
  - **Property 7: Inline Markup Precedence and Escaping**
  - **Validates: Requirements 4.6, 4.7**
  - Remove @Ignore annotation and fix any failing test cases

- [x] 7. Implement attribute parsing and storage
  - Add attribute definition parsing (lines starting with ":")
  - Implement key-value extraction with space preservation
  - Add attribute storage in Document node
  - Handle duplicate attributes (last value wins)
  - Implement attribute reference marking ({key} syntax)
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

- [ ] 7.1 Write property test for attribute parsing and storage
  - **Property 10: Attribute Parsing and Storage**
  - **Validates: Requirements 6.1, 6.2, 6.3, 6.5**

- [ ] 7.2 Write property test for attribute reference marking
  - **Property 11: Attribute Reference Marking**
  - **Validates: Requirements 6.4**

- [x] 8. Implement comprehensive error handling integration
  - Integrate error and warning collection throughout all parser components
  - Implement error recovery strategies in main parser facade
  - Add detailed error reporting with line numbers and descriptions
  - Handle malformed syntax with appropriate error messages
  - Ensure parsing continues after recoverable errors
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 3.7_

- [x] 8.1 Write property test for error handling and recovery
  - **Property 12: Error Handling and Recovery**
  - **Validates: Requirements 7.1, 7.2, 7.3, 7.4, 7.5**

- [x] 8.2 Write property test for malformed syntax error reporting
  - **Property 13: Malformed Syntax Error Reporting**
  - **Validates: Requirements 3.7**

- [x] 9. Implement main parser facade with component integration
  - Create DefaultAsciidocParser implementation of AsciidocParser interface
  - Integrate all components (LineProcessor, BlockParser, InlineParser, StateMachine, AttributeParser)
  - Implement parse methods for String and List<String> input
  - Return ParseResult with document, errors, and warnings
  - Wire together the complete parsing pipeline
  - Ensure platform-neutral implementation
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

- [x] 9.1 Write property test for cross-platform consistency
  - **Property 14: Cross-Platform Consistency**
  - **Validates: Requirements 8.3, 8.4**

- [ ] 10. Integration and end-to-end testing
  - Test complete parsing pipeline with complex documents
  - Validate AST structure for nested and mixed content
  - Ensure error collection works across all components
  - Test attribute parsing and reference marking integration
  - Verify state machine context preservation across complex documents
  - _Requirements: All requirements integration_

- [ ] 10.1 Write integration tests for complete parsing pipeline
  - Test end-to-end parsing with complex AsciiDoc documents
  - Validate complete AST structure and error handling
  - Test attribute substitution and reference resolution
  - _Requirements: All requirements integration_

- [ ] 11. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- All tasks are required for comprehensive implementation
- Each task references specific requirements for traceability
- Property tests validate universal correctness properties using kotlin-test framework
- Unit tests validate specific examples and edge cases
- Checkpoints ensure incremental validation throughout development
- The implementation follows platform-neutral Kotlin Multiplatform practices
- Most core components are implemented, focus is now on integration and testing