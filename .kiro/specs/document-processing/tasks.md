# Implementation Plan: Document Processing

## Overview

This implementation plan breaks down the document processing module into discrete, incremental tasks. The module will be implemented as a separate Gradle submodule that depends on the core parser library. Tasks are organized to build foundational components first, then layer on more complex functionality, with testing integrated throughout.

The plan includes both core document processing features (tasks 1-14, completed) and advanced features (tasks 15-22, pending):

**Core Features (Completed):**
- Include directive resolution
- Attribute substitution
- Cross-reference resolution
- Table of contents generation
- Document validation
- Macro expansion
- Processing pipeline
- Platform-specific implementations

**Advanced Features (Pending):**
- Conditional content processing (ifdef/ifndef/ifeval)
- Document fragment processing (tagged includes)
- Admonition block processing
- Bibliography and footnote management
- Source code callout processing
- Extension system for custom processors

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

- [x] 15. Implement Conditional Content Processor
  - [x] 15.1 Create ConditionalProcessor interface and configuration
    - _Requirements: 9.1, 9.2, 9.3_
  
  - [x] 15.2 Implement ifdef directive evaluation
    - Check attribute presence and include/exclude content
    - _Requirements: 9.1_
  
  - [x] 15.3 Implement ifndef directive evaluation
    - Check attribute absence and include/exclude content
    - _Requirements: 9.2_
  
  - [x] 15.4 Implement ifeval expression evaluation
    - Parse and evaluate conditional expressions
    - Support comparison operators and boolean logic
    - _Requirements: 9.3_
  
  - [x] 15.5 Implement nested conditional handling
    - Track nesting depth and evaluate nested conditions
    - _Requirements: 9.4_
  
  - [x] 15.6 Implement unclosed conditional detection
    - Validate matching endif directives
    - _Requirements: 9.5_
  
  - [x] 15.7 Implement multi-attribute conditional logic
    - Support AND/OR operators for multiple attributes
    - _Requirements: 9.6_
  
  - [ ]* 15.8 Write property test for ifdef evaluation
    - **Property 34: Ifdef Conditional Evaluation**
    - **Validates: Requirements 9.1**
  
  - [ ]* 15.9 Write property test for ifndef evaluation
    - **Property 35: Ifndef Conditional Evaluation**
    - **Validates: Requirements 9.2**
  
  - [ ]* 15.10 Write property test for ifeval evaluation
    - **Property 36: Ifeval Expression Evaluation**
    - **Validates: Requirements 9.3**
  
  - [ ]* 15.11 Write property test for nested conditionals
    - **Property 37: Nested Conditional Processing**
    - **Validates: Requirements 9.4**
  
  - [ ]* 15.12 Write property test for unclosed conditional detection
    - **Property 38: Unclosed Conditional Detection**
    - **Validates: Requirements 9.5**
  
  - [ ]* 15.13 Write property test for multi-attribute logic
    - **Property 39: Multi-Attribute Conditional Logic**
    - **Validates: Requirements 9.6**
  
  - [ ]* 15.14 Write unit tests for conditional processor edge cases
    - Test empty conditionals, complex expressions
    - _Requirements: 9.3, 9.4_

- [x] 16. Implement Document Fragment Processor
  - [x] 16.1 Create FragmentProcessor interface and configuration
    - _Requirements: 10.1_
  
  - [x] 16.2 Implement tag marker parsing
    - Parse tag:: markers in included content
    - _Requirements: 10.1_
  
  - [x] 16.3 Implement tagged content extraction
    - Extract content between matching tag markers
    - _Requirements: 10.1_
  
  - [x] 16.4 Implement multiple tag selection
    - Support comma-separated tag lists
    - _Requirements: 10.2_
  
  - [x] 16.5 Implement tag validation and error reporting
    - Report missing or malformed tags
    - _Requirements: 10.3, 10.4_
  
  - [x] 16.6 Implement tag nesting support
    - Handle nested tags according to AsciiDoc rules
    - _Requirements: 10.5_
  
  - [x] 16.7 Implement tag and line range combination
    - Apply both filters in correct order
    - _Requirements: 10.6_
  
  - [ ]* 16.8 Write property test for tagged fragment extraction
    - **Property 40: Tagged Fragment Extraction**
    - **Validates: Requirements 10.1**
  
  - [ ]* 16.9 Write property test for multiple tag selection
    - **Property 41: Multiple Tag Selection**
    - **Validates: Requirements 10.2**
  
  - [ ]* 16.10 Write property test for missing tag warning
    - **Property 42: Missing Tag Warning**
    - **Validates: Requirements 10.3**
  
  - [ ]* 16.11 Write property test for tag marker validation
    - **Property 43: Tag Marker Validation**
    - **Validates: Requirements 10.4**
  
  - [ ]* 16.12 Write property test for tag and line range combination
    - **Property 44: Tag and Line Range Combination**
    - **Validates: Requirements 10.6**
  
  - [ ]* 16.13 Write unit tests for fragment processor edge cases
    - Test overlapping tags, empty tags
    - _Requirements: 10.1, 10.3_

- [x] 17. Implement Admonition Processor
  - [x] 17.1 Create AdmonitionProcessor interface and AdmonitionBlock AST node
    - _Requirements: 11.1, 11.2_
  
  - [x] 17.2 Implement admonition type recognition
    - Identify NOTE, TIP, WARNING, CAUTION, IMPORTANT
    - _Requirements: 11.1_
  
  - [x] 17.3 Implement admonition content extraction
    - Extract and preserve admonition content
    - _Requirements: 11.2_
  
  - [x] 17.4 Implement custom title handling
    - Parse and associate custom titles
    - _Requirements: 11.3_
  
  - [x] 17.5 Implement admonition structure validation
    - Validate nesting and structural relationships
    - _Requirements: 11.4_
  
  - [x] 17.6 Implement invalid admonition handling
    - Report warnings for invalid types
    - _Requirements: 11.5_
  
  - [ ]* 17.7 Write property test for admonition type recognition
    - **Property 45: Admonition Type Recognition**
    - **Validates: Requirements 11.1, 11.2**
  
  - [ ]* 17.8 Write property test for title preservation
    - **Property 46: Admonition Title Preservation**
    - **Validates: Requirements 11.3**
  
  - [ ]* 17.9 Write property test for structure validation
    - **Property 47: Admonition Structure Validation**
    - **Validates: Requirements 11.4**
  
  - [ ]* 17.10 Write unit tests for admonition processor edge cases
    - Test nested admonitions, empty content
    - _Requirements: 11.2, 11.4_

- [x] 18. Implement Bibliography and Footnote Manager
  - [x] 18.1 Create BibliographyManager interface and data models
    - _Requirements: 12.1, 12.3_
  
  - [x] 18.2 Implement footnote collection and numbering
    - Collect footnotes and assign sequential numbers
    - _Requirements: 12.1_
  
  - [x] 18.3 Implement footnote list generation
    - Generate ordered list of all footnotes
    - _Requirements: 12.2_
  
  - [x] 18.4 Implement bibliography entry indexing
    - Index entries with unique identifiers
    - _Requirements: 12.3_
  
  - [x] 18.5 Implement bibliography reference resolution
    - Resolve references to indexed entries
    - _Requirements: 12.4_
  
  - [x] 18.6 Implement unresolved reference warnings
    - Report warnings for missing references
    - _Requirements: 12.5_
  
  - [x] 18.7 Implement consistent footnote numbering
    - Handle multiple references to same footnote
    - _Requirements: 12.6_
  
  - [ ]* 18.8 Write property test for footnote collection
    - **Property 48: Footnote Collection and Numbering**
    - **Validates: Requirements 12.1**
  
  - [ ]* 18.9 Write property test for footnote list generation
    - **Property 49: Footnote List Generation**
    - **Validates: Requirements 12.2**
  
  - [ ]* 18.10 Write property test for bibliography indexing
    - **Property 50: Bibliography Entry Indexing**
    - **Validates: Requirements 12.3**
  
  - [ ]* 18.11 Write property test for bibliography resolution
    - **Property 51: Bibliography Reference Resolution**
    - **Validates: Requirements 12.4**
  
  - [ ]* 18.12 Write property test for unresolved citation warning
    - **Property 52: Unresolved Citation Warning**
    - **Validates: Requirements 12.5**
  
  - [ ]* 18.13 Write property test for consistent numbering
    - **Property 53: Consistent Footnote Numbering**
    - **Validates: Requirements 12.6**
  
  - [ ]* 18.14 Write unit tests for bibliography manager edge cases
    - Test duplicate entries, empty footnotes
    - _Requirements: 12.1, 12.3_

- [x] 19. Implement Callout Processor
  - [x] 19.1 Create CalloutProcessor interface and data models
    - _Requirements: 13.1_
  
  - [x] 19.2 Implement callout marker extraction
    - Extract markers from code blocks and number them
    - _Requirements: 13.1_
  
  - [x] 19.3 Implement callout list association
    - Associate list items with markers
    - _Requirements: 13.2_
  
  - [x] 19.4 Implement callout mismatch detection
    - Validate marker-explanation matching
    - _Requirements: 13.3_
  
  - [x] 19.5 Implement callout context validation
    - Ensure callouts are used with code blocks
    - _Requirements: 13.4_
  
  - [x] 19.6 Implement callout sequence isolation
    - Maintain separate sequences per code block
    - _Requirements: 13.5_
  
  - [ ]* 19.7 Write property test for marker extraction
    - **Property 54: Callout Marker Extraction**
    - **Validates: Requirements 13.1**
  
  - [ ]* 19.8 Write property test for list association
    - **Property 55: Callout List Association**
    - **Validates: Requirements 13.2**
  
  - [ ]* 19.9 Write property test for mismatch warning
    - **Property 56: Callout Mismatch Warning**
    - **Validates: Requirements 13.3**
  
  - [ ]* 19.10 Write property test for context validation
    - **Property 57: Callout Context Validation**
    - **Validates: Requirements 13.4**
  
  - [ ]* 19.11 Write property test for sequence isolation
    - **Property 58: Callout Sequence Isolation**
    - **Validates: Requirements 13.5**
  
  - [ ]* 19.12 Write unit tests for callout processor edge cases
    - Test empty callouts, malformed markers
    - _Requirements: 13.1, 13.3_

- [x] 20. Implement Extension System
  - [x] 20.1 Create CustomProcessor interface and ExtensionRegistry
    - _Requirements: 14.1_
  
  - [x] 20.2 Implement processor registration
    - Allow registration with name and priority
    - _Requirements: 14.1_
  
  - [x] 20.3 Implement priority-based ordering
    - Sort processors by priority within phases
    - _Requirements: 14.2, 14.6_
  
  - [x] 20.4 Implement processing context
    - Provide context and shared state to processors
    - _Requirements: 14.1_
  
  - [x] 20.5 Implement custom processor execution
    - Execute processors at appropriate phases
    - _Requirements: 14.5_
  
  - [x] 20.6 Implement error isolation
    - Continue processing on custom processor failure
    - _Requirements: 14.3_
  
  - [x] 20.7 Implement output validation
    - Validate AST modifications from custom processors
    - _Requirements: 14.4_
  
  - [ ]* 20.8 Write property test for processor registration
    - **Property 59: Custom Processor Registration**
    - **Validates: Requirements 14.1**
  
  - [ ]* 20.9 Write property test for processor ordering
    - **Property 60: Custom Processor Ordering**
    - **Validates: Requirements 14.2, 14.5, 14.6**
  
  - [ ]* 20.10 Write property test for error isolation
    - **Property 61: Custom Processor Error Isolation**
    - **Validates: Requirements 14.3**
  
  - [ ]* 20.11 Write property test for output validation
    - **Property 62: Custom Processor Output Validation**
    - **Validates: Requirements 14.4**
  
  - [ ]* 20.12 Write unit tests for extension system edge cases
    - Test duplicate registrations, invalid priorities
    - _Requirements: 14.1, 14.2_

- [x] 21. Update Processing Pipeline
  - [x] 21.1 Integrate new processors into DefaultDocumentProcessor
    - Add conditional, fragment, admonition, callout, bibliography processors
    - _Requirements: 9.1, 10.1, 11.1, 12.1, 13.1_
  
  - [x] 21.2 Update ProcessingConfig with new options
    - Add configuration for new processors
    - _Requirements: 9.1, 10.1, 11.1, 12.1, 13.1, 14.1_
  
  - [x] 21.3 Implement extension system integration
    - Allow custom processors at each phase
    - _Requirements: 14.1, 14.5_
  
  - [x] 21.4 Update error types for new processors
    - Add error types for conditionals, fragments, etc.
    - _Requirements: 9.5, 10.3, 11.5, 12.5, 13.3_
  
  - [ ]* 21.5 Write integration tests for extended pipeline
    - Test full pipeline with all new features
    - _Requirements: 9.1, 10.1, 11.1, 12.1, 13.1, 14.1_

- [x] 22. Implement IncludeResolver Tests
  - [x] 22.1 Write unit tests for IncludeResolver
    - Test basic include resolution
    - Test relative path resolution
    - Test file not found errors
    - Test line range filtering
    - Test nested includes
    - Test circular dependency detection
    - Test max depth enforcement
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6_

- [ ] 23. Implement Property-Based Tests for Core Features
  - [ ] 23.1 Write property test for include directive resolution (Property 1)
    - **Validates: Requirements 1.1**
  
  - [ ] 23.2 Write property test for relative path resolution (Property 2)
    - **Validates: Requirements 1.2**
  
  - [ ] 23.3 Write property test for include error reporting (Property 3)
    - **Validates: Requirements 1.3**
  
  - [ ] 23.4 Write property test for line range inclusion (Property 4)
    - **Validates: Requirements 1.4**
  
  - [ ] 23.5 Write property test for nested include resolution (Property 5)
    - **Validates: Requirements 1.5**
  
  - [ ] 23.6 Write property test for circular include detection (Property 6)
    - **Validates: Requirements 1.6**
  
  - [ ] 23.7 Write property test for attribute substitution (Property 7)
    - **Validates: Requirements 2.1**
  
  - [ ] 23.8 Write property test for undefined attribute handling (Property 8)
    - **Validates: Requirements 2.2**
  
  - [ ] 23.9 Write property test for recursive attribute resolution (Property 9)
    - **Validates: Requirements 2.3**
  
  - [ ] 23.10 Write property test for header attribute scope (Property 10)
    - **Validates: Requirements 2.4**
  
  - [ ] 23.11 Write property test for inline attribute scope (Property 11)
    - **Validates: Requirements 2.5**
  
  - [ ] 23.12 Write property test for circular attribute detection (Property 12)
    - **Validates: Requirements 2.6**
  
  - [ ] 23.13 Write property test for cross-reference resolution (Property 13)
    - **Validates: Requirements 3.1**
  
  - [ ] 23.14 Write property test for unresolved reference warning (Property 14)
    - **Validates: Requirements 3.2**
  
  - [ ] 23.15 Write property test for link text generation (Property 15)
    - **Validates: Requirements 3.3**
  
  - [ ] 23.16 Write property test for duplicate anchor detection (Property 16)
    - **Validates: Requirements 3.4, 5.3**
  
  - [ ] 23.17 Write property test for custom link text preservation (Property 17)
    - **Validates: Requirements 3.5**
  
  - [ ] 23.18 Write property test for TOC hierarchical structure (Property 18)
    - **Validates: Requirements 4.1**
  
  - [ ] 23.19 Write property test for TOC depth limiting (Property 19)
    - **Validates: Requirements 4.2**
  
  - [ ] 23.20 Write property test for untitled section exclusion (Property 20)
    - **Validates: Requirements 4.3**
  
  - [ ] 23.21 Write property test for TOC cross-reference creation (Property 21)
    - **Validates: Requirements 4.4**
  
  - [ ] 23.22 Write property test for section hierarchy validation (Property 22)
    - **Validates: Requirements 5.1, 5.2**
  
  - [ ] 23.23 Write property test for whitespace normalization (Property 23)
    - **Validates: Requirements 5.4**
  
  - [ ] 23.24 Write property test for invalid attribute reference collection (Property 24)
    - **Validates: Requirements 5.5**
  
  - [ ] 23.25 Write property test for macro expansion (Property 25)
    - **Validates: Requirements 6.1**
  
  - [ ] 23.26 Write property test for macro parameter parsing (Property 26)
    - **Validates: Requirements 6.2**
  
  - [ ] 23.27 Write property test for macro expansion error reporting (Property 27)
    - **Validates: Requirements 6.3**
  
  - [ ] 23.28 Write property test for custom macro registration (Property 28)
    - **Validates: Requirements 6.4**
  
  - [ ] 23.29 Write property test for macro AST integration (Property 29)
    - **Validates: Requirements 6.5**
  
  - [ ] 23.30 Write property test for macro output validation (Property 30)
    - **Validates: Requirements 6.6**
  
  - [ ] 23.31 Write property test for invalid configuration detection (Property 31)
    - **Validates: Requirements 7.6**
  
  - [ ] 23.32 Write property test for comprehensive error reporting (Property 32)
    - **Validates: Requirements 8.1, 8.2, 8.3, 8.4**
  
  - [ ] 23.33 Write property test for graceful error handling (Property 33)
    - **Validates: Requirements 8.5**

- [ ] 24. Implement Property-Based Tests for Advanced Features
  - [ ] 24.1 Write property test for ifdef evaluation (Property 34)
    - **Validates: Requirements 9.1**
  
  - [ ] 24.2 Write property test for ifndef evaluation (Property 35)
    - **Validates: Requirements 9.2**
  
  - [ ] 24.3 Write property test for ifeval evaluation (Property 36)
    - **Validates: Requirements 9.3**
  
  - [ ] 24.4 Write property test for nested conditionals (Property 37)
    - **Validates: Requirements 9.4**
  
  - [ ] 24.5 Write property test for unclosed conditional detection (Property 38)
    - **Validates: Requirements 9.5**
  
  - [ ] 24.6 Write property test for multi-attribute logic (Property 39)
    - **Validates: Requirements 9.6**
  
  - [ ] 24.7 Write property test for tagged fragment extraction (Property 40)
    - **Validates: Requirements 10.1**
  
  - [ ] 24.8 Write property test for multiple tag selection (Property 41)
    - **Validates: Requirements 10.2**
  
  - [ ] 24.9 Write property test for missing tag warning (Property 42)
    - **Validates: Requirements 10.3**
  
  - [ ] 24.10 Write property test for tag marker validation (Property 43)
    - **Validates: Requirements 10.4**
  
  - [ ] 24.11 Write property test for tag and line range combination (Property 44)
    - **Validates: Requirements 10.6**
  
  - [ ] 24.12 Write property test for admonition type recognition (Property 45)
    - **Validates: Requirements 11.1, 11.2**
  
  - [ ] 24.13 Write property test for title preservation (Property 46)
    - **Validates: Requirements 11.3**
  
  - [ ] 24.14 Write property test for structure validation (Property 47)
    - **Validates: Requirements 11.4**
  
  - [ ] 24.15 Write property test for footnote collection (Property 48)
    - **Validates: Requirements 12.1**
  
  - [ ] 24.16 Write property test for footnote list generation (Property 49)
    - **Validates: Requirements 12.2**
  
  - [ ] 24.17 Write property test for bibliography indexing (Property 50)
    - **Validates: Requirements 12.3**
  
  - [ ] 24.18 Write property test for bibliography resolution (Property 51)
    - **Validates: Requirements 12.4**
  
  - [ ] 24.19 Write property test for unresolved citation warning (Property 52)
    - **Validates: Requirements 12.5**
  
  - [ ] 24.20 Write property test for consistent numbering (Property 53)
    - **Validates: Requirements 12.6**
  
  - [ ] 24.21 Write property test for marker extraction (Property 54)
    - **Validates: Requirements 13.1**
  
  - [ ] 24.22 Write property test for list association (Property 55)
    - **Validates: Requirements 13.2**
  
  - [ ] 24.23 Write property test for mismatch warning (Property 56)
    - **Validates: Requirements 13.3**
  
  - [ ] 24.24 Write property test for context validation (Property 57)
    - **Validates: Requirements 13.4**
  
  - [ ] 24.25 Write property test for sequence isolation (Property 58)
    - **Validates: Requirements 13.5**
  
  - [ ] 24.26 Write property test for processor registration (Property 59)
    - **Validates: Requirements 14.1**
  
  - [ ] 24.27 Write property test for processor ordering (Property 60)
    - **Validates: Requirements 14.2, 14.5, 14.6**
  
  - [ ] 24.28 Write property test for error isolation (Property 61)
    - **Validates: Requirements 14.3**
  
  - [ ] 24.29 Write property test for output validation (Property 62)
    - **Validates: Requirements 14.4**

- [ ] 25. Implement Integration Tests
  - [ ] 25.1 Write integration tests with complete documents
    - Test full pipeline with realistic documents
    - Test documents with multiple features combined
    - Test cross-platform consistency
    - _Requirements: 1.1, 2.1, 3.1, 4.1, 5.1, 6.1, 9.1, 10.1, 11.1, 12.1, 13.1, 14.1_
  
  - [ ] 25.2 Write platform-specific integration tests
    - Test JVM FileReader with real files
    - Test iOS FileReader with real files
    - Test Android FileReader with real files
    - Test Linux FileReader with real files
    - _Requirements: 1.1_

- [ ] 26. Final checkpoint - Ensure all tests pass
  - Run all unit tests across all platforms
  - Run all property-based tests across all platforms
  - Run all integration tests across all platforms
  - Ensure all tests pass, ask the user if questions arise

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties
- Unit tests validate specific examples and edge cases
- The module follows the same multiplatform structure as the core parser
- FileReader interface allows platform-specific file I/O while keeping core logic platform-neutral
