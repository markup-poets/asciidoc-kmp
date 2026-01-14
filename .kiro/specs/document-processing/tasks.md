# Implementation Plan: Document Processing

## Overview

This implementation plan breaks down the document processing module into discrete, incremental tasks. The module will be implemented as a separate Gradle submodule that depends on the core parser library. Tasks are organized to build foundational components first, then layer on more complex functionality, with testing integrated throughout.

## Tasks

- [x] 1. Set up document-processing module structure
  - Create new Gradle submodule `document-processing` with multiplatform configuration
  - Configure dependency on `asciidoc-parser` module
  - Set up source sets (commonMain, commonTest, platform-specific)
  - Add Kotest property testing dependency
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 3.1, 3.2, 3.3, 3.4, 3.5, 4.1, 4.2, 4.3, 4.4, 5.1, 5.2, 5.3, 5.4, 5.5, 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 8.1, 8.2, 8.3, 8.4, 8.5_

- [x] 2. Implement core data models and interfaces
  - [x] 2.1 Create ProcessingConfig data class with all configuration options
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_
  
  - [x] 2.2 Create ProcessingError and ProcessingWarning data classes
    - _Requirements: 8.1, 8.2, 8.3_
  
  - [x] 2.3 Create ProcessingResult data class
    - _Requirements: 8.4_
  
  - [x] 2.4 Create new AST node types (IncludeDirective, AttributeReference, CrossReference, MacroInvocation)
    - _Requirements: 1.1, 2.1, 3.1, 6.1_
  
  - [x] 2.5 Create DocumentProcessor interface and result types
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_
  
  - [ ]* 2.6 Write unit tests for data model creation and validation
    - Test ProcessingConfig with various option combinations
    - Test error and warning creation with location information
    - _Requirements: 7.6, 8.1_

- [x] 3. Implement Include Resolver
  - [x] 3.1 Create FileReader interface and IncludeResolver interface
    - _Requirements: 1.1, 1.2_
  
  - [x] 3.2 Implement include directive detection and path resolution
    - Handle relative and absolute paths
    - Resolve paths relative to including document location
    - _Requirements: 1.1, 1.2_
  
  - [x] 3.3 Implement file content embedding into AST
    - Parse included content and insert into document tree
    - _Requirements: 1.1_
  
  - [x] 3.4 Implement line range filtering
    - Extract specified line ranges from included files
    - _Requirements: 1.4_
  
  - [x] 3.5 Implement nested include resolution with depth tracking
    - Track current depth and enforce limit
    - _Requirements: 1.5_
  
  - [x] 3.6 Implement circular dependency detection
    - Track visited files to detect cycles
    - _Requirements: 1.6_
  
  - [x] 3.7 Implement error reporting for include failures
    - Report file not found, depth exceeded, circular dependencies
    - _Requirements: 1.3, 1.5, 1.6_
  
  - [ ]* 3.8 Write property test for include directive resolution
    - **Property 1: Include Directive Resolution**
    - **Validates: Requirements 1.1**
  
  - [ ]* 3.9 Write property test for relative path resolution
    - **Property 2: Relative Path Resolution**
    - **Validates: Requirements 1.2**
  
  - [ ]* 3.10 Write property test for include error reporting
    - **Property 3: Include Error Reporting**
    - **Validates: Requirements 1.3**
  
  - [ ]* 3.11 Write property test for line range inclusion
    - **Property 4: Line Range Inclusion**
    - **Validates: Requirements 1.4**
  
  - [ ]* 3.12 Write property test for nested include resolution
    - **Property 5: Nested Include Resolution**
    - **Validates: Requirements 1.5**
  
  - [ ]* 3.13 Write property test for circular include detection
    - **Property 6: Circular Include Detection**
    - **Validates: Requirements 1.6**
  
  - [ ]* 3.14 Write unit tests for include resolver edge cases
    - Test empty files, files with only whitespace
    - Test maximum depth boundary conditions
    - _Requirements: 1.3, 1.5_

- [x] 4. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Implement Attribute Substitutor
  - [x] 5.1 Create AttributeSubstitutor interface and configuration
    - _Requirements: 2.1, 2.2_
  
  - [x] 5.2 Implement attribute reference detection in text content
    - Find {key} patterns in all text nodes
    - _Requirements: 2.1_
  
  - [x] 5.3 Implement basic attribute substitution
    - Replace references with defined values
    - Handle undefined attributes according to configuration
    - _Requirements: 2.1, 2.2_
  
  - [x] 5.4 Implement recursive attribute resolution
    - Resolve nested attribute references
    - Track recursion depth
    - _Requirements: 2.3_
  
  - [x] 5.5 Implement attribute scope handling
    - Apply header attributes globally
    - Apply inline attributes from definition point forward
    - _Requirements: 2.4, 2.5_
  
  - [x] 5.6 Implement circular reference detection
    - Track attribute resolution chain to detect cycles
    - _Requirements: 2.6_
  
  - [ ]* 5.7 Write property test for attribute substitution
    - **Property 7: Attribute Substitution**
    - **Validates: Requirements 2.1**
  
  - [ ]* 5.8 Write property test for undefined attribute handling
    - **Property 8: Undefined Attribute Handling**
    - **Validates: Requirements 2.2**
  
  - [ ]* 5.9 Write property test for recursive attribute resolution
    - **Property 9: Recursive Attribute Resolution**
    - **Validates: Requirements 2.3**
  
  - [ ]* 5.10 Write property test for header attribute scope
    - **Property 10: Header Attribute Scope**
    - **Validates: Requirements 2.4**
  
  - [ ]* 5.11 Write property test for inline attribute scope
    - **Property 11: Inline Attribute Scope**
    - **Validates: Requirements 2.5**
  
  - [ ]* 5.12 Write property test for circular attribute detection
    - **Property 12: Circular Attribute Detection**
    - **Validates: Requirements 2.6**
  
  - [ ]* 5.13 Write unit tests for attribute substitutor edge cases
    - Test attributes with special characters
    - Test empty attribute values
    - _Requirements: 2.1, 2.2_

- [x] 6. Implement Cross-Reference Resolver
  - [x] 6.1 Create CrossReferenceResolver interface
    - _Requirements: 3.1_
  
  - [x] 6.2 Implement anchor index building
    - Traverse document to collect all anchor IDs and their targets
    - Detect duplicate anchors
    - _Requirements: 3.1, 3.4_
  
  - [x] 6.3 Implement cross-reference resolution
    - Match references to targets using anchor index
    - _Requirements: 3.1_
  
  - [x] 6.4 Implement link text generation
    - Generate appropriate text based on target element type
    - Preserve custom link text when provided
    - _Requirements: 3.3, 3.5_
  
  - [x] 6.5 Implement error and warning reporting
    - Report unresolved references as warnings
    - Report duplicate anchors as errors
    - _Requirements: 3.2, 3.4_
  
  - [ ]* 6.6 Write property test for cross-reference resolution
    - **Property 13: Cross-Reference Resolution**
    - **Validates: Requirements 3.1**
  
  - [ ]* 6.7 Write property test for unresolved reference warning
    - **Property 14: Unresolved Reference Warning**
    - **Validates: Requirements 3.2**
  
  - [ ]* 6.8 Write property test for link text generation
    - **Property 15: Link Text Generation**
    - **Validates: Requirements 3.3**
  
  - [ ]* 6.9 Write property test for duplicate anchor detection
    - **Property 16: Duplicate Anchor Detection**
    - **Validates: Requirements 3.4, 5.3**
  
  - [ ]* 6.10 Write property test for custom link text preservation
    - **Property 17: Custom Link Text Preservation**
    - **Validates: Requirements 3.5**
  
  - [ ]* 6.11 Write unit tests for cross-reference resolver edge cases
    - Test references to different node types
    - Test empty anchor IDs
    - _Requirements: 3.1, 3.2_

- [x] 7. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 8. Implement Table of Contents Generator
  - [x] 8.1 Create TocGenerator interface and configuration
    - _Requirements: 4.1_
  
  - [x] 8.2 Implement section traversal and collection
    - Find all sections in document
    - Build hierarchical structure
    - _Requirements: 4.1_
  
  - [x] 8.3 Implement depth limiting
    - Filter sections based on configured depth
    - _Requirements: 4.2_
  
  - [x] 8.4 Implement untitled section filtering
    - Exclude sections without titles
    - _Requirements: 4.3_
  
  - [x] 8.5 Implement TOC list generation with cross-references
    - Create AsciiDocList structure
    - Add cross-references to each section
    - _Requirements: 4.1, 4.4_
  
  - [ ]* 8.6 Write property test for TOC hierarchical structure
    - **Property 18: TOC Hierarchical Structure**
    - **Validates: Requirements 4.1**
  
  - [ ]* 8.7 Write property test for TOC depth limiting
    - **Property 19: TOC Depth Limiting**
    - **Validates: Requirements 4.2**
  
  - [ ]* 8.8 Write property test for untitled section exclusion
    - **Property 20: Untitled Section Exclusion**
    - **Validates: Requirements 4.3**
  
  - [ ]* 8.9 Write property test for TOC cross-reference creation
    - **Property 21: TOC Cross-Reference Creation**
    - **Validates: Requirements 4.4**
  
  - [ ]* 8.10 Write unit tests for TOC generator edge cases
    - Test documents with no sections
    - Test documents with only deep sections
    - _Requirements: 4.1, 4.2_

- [x] 9. Implement Document Validator
  - [x] 9.1 Create DocumentValidator interface and configuration
    - _Requirements: 5.1_
  
  - [x] 9.2 Implement section hierarchy validation
    - Check for skipped section levels
    - Report violations with location information
    - _Requirements: 5.1, 5.2_
  
  - [x] 9.3 Implement duplicate anchor detection
    - Reuse logic from cross-reference resolver
    - _Requirements: 5.3_
  
  - [x] 9.4 Implement whitespace normalization
    - Normalize according to AsciiDoc conventions
    - _Requirements: 5.4_
  
  - [x] 9.5 Implement invalid attribute reference collection
    - Collect all invalid references
    - Report in single validation report
    - _Requirements: 5.5_
  
  - [ ]* 9.6 Write property test for section hierarchy validation
    - **Property 22: Section Hierarchy Validation**
    - **Validates: Requirements 5.1, 5.2**
  
  - [ ]* 9.7 Write property test for whitespace normalization
    - **Property 23: Whitespace Normalization**
    - **Validates: Requirements 5.4**
  
  - [ ]* 9.8 Write property test for invalid attribute reference collection
    - **Property 24: Invalid Attribute Reference Collection**
    - **Validates: Requirements 5.5**
  
  - [ ]* 9.9 Write unit tests for validator edge cases
    - Test valid hierarchies
    - Test various whitespace patterns
    - _Requirements: 5.1, 5.4_

- [x] 10. Implement Macro Expander
  - [x] 10.1 Create MacroExpander interface and MacroProcessor interface
    - _Requirements: 6.1_
  
  - [x] 10.2 Implement macro invocation detection
    - Find macro invocations in AST
    - _Requirements: 6.1_
  
  - [x] 10.3 Implement macro parameter parsing
    - Extract and parse macro parameters
    - _Requirements: 6.2_
  
  - [x] 10.4 Implement macro processor invocation
    - Call appropriate processor with parameters
    - Handle custom and built-in macros
    - _Requirements: 6.1, 6.4_
  
  - [x] 10.5 Implement AST node integration
    - Insert generated nodes at macro location
    - Maintain parent-child relationships
    - _Requirements: 6.5_
  
  - [x] 10.6 Implement macro output validation
    - Validate generated content
    - _Requirements: 6.6_
  
  - [x] 10.7 Implement error reporting for macro failures
    - Report expansion failures with location
    - _Requirements: 6.3_
  
  - [ ]* 10.8 Write property test for macro expansion
    - **Property 25: Macro Expansion**
    - **Validates: Requirements 6.1**
  
  - [ ]* 10.9 Write property test for macro parameter parsing
    - **Property 26: Macro Parameter Parsing**
    - **Validates: Requirements 6.2**
  
  - [ ]* 10.10 Write property test for macro expansion error reporting
    - **Property 27: Macro Expansion Error Reporting**
    - **Validates: Requirements 6.3**
  
  - [ ]* 10.11 Write property test for custom macro registration
    - **Property 28: Custom Macro Registration**
    - **Validates: Requirements 6.4**
  
  - [ ]* 10.12 Write property test for macro AST integration
    - **Property 29: Macro AST Integration**
    - **Validates: Requirements 6.5**
  
  - [ ]* 10.13 Write property test for macro output validation
    - **Property 30: Macro Output Validation**
    - **Validates: Requirements 6.6**
  
  - [ ]* 10.14 Write unit tests for macro expander edge cases
    - Test macros with no parameters
    - Test macros generating empty content
    - _Requirements: 6.1, 6.2_

- [x] 11. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 12. Implement Processing Pipeline
  - [x] 12.1 Create DefaultDocumentProcessor implementation
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_
  
  - [x] 12.2 Implement pipeline execution logic
    - Chain processors in correct order
    - Accumulate errors and warnings
    - _Requirements: 8.2, 8.4_
  
  - [x] 12.3 Implement configuration validation
    - Validate configuration before processing
    - _Requirements: 7.6_
  
  - [x] 12.4 Implement conditional processor execution
    - Skip processors based on configuration
    - _Requirements: 7.1, 7.4_
  
  - [x] 12.5 Implement error handling and graceful halting
    - Handle fatal errors gracefully
    - _Requirements: 8.5_
  
  - [ ]* 12.6 Write property test for invalid configuration detection
    - **Property 31: Invalid Configuration Detection**
    - **Validates: Requirements 7.6**
  
  - [ ]* 12.7 Write property test for comprehensive error reporting
    - **Property 32: Comprehensive Error Reporting**
    - **Validates: Requirements 8.1, 8.2, 8.3, 8.4**
  
  - [ ]* 12.8 Write property test for graceful error handling
    - **Property 33: Graceful Error Handling**
    - **Validates: Requirements 8.5**
  
  - [ ]* 12.9 Write unit tests for pipeline execution
    - Test with various configuration combinations (Requirements 7.1-7.5)
    - Test processor ordering
    - Test error accumulation
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 8.2_

- [x] 13. Integration and platform-specific implementations
  - [x] 13.1 Implement platform-specific FileReader for JVM
    - Use Java file I/O
    - _Requirements: 1.1_
  
  - [x] 13.2 Implement platform-specific FileReader for other platforms
    - Provide appropriate implementations for iOS, Android, Linux
    - _Requirements: 1.1_
  
  - [ ]* 13.3 Write integration tests with complete documents
    - Test full pipeline with realistic documents
    - Test cross-platform consistency
    - _Requirements: 1.1, 2.1, 3.1, 4.1, 5.1, 6.1_

- [x] 14. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties
- Unit tests validate specific examples and edge cases
- The module follows the same multiplatform structure as the core parser
- FileReader interface allows platform-specific file I/O while keeping core logic platform-neutral
