# Implementation Plan: Antora Document Assembler

## Overview

This implementation plan breaks down the Antora Document Assembler into two main modules:
1. **antora-resolution**: Reusable library for Antora resource resolution
2. **antora-assembler**: Document assembly tool that uses the resolution library

The implementation follows an incremental approach, building the resolution library first, then the assembler, with testing integrated throughout.

## Tasks

- [x] 1. Set up project structure and module configuration
  - Create `antora-resolution` module with KMP configuration
  - Create `antora-assembler` module with KMP configuration
  - Configure module dependencies in `settings.gradle.kts`
  - Set up package structure: `org.markup.poet.antora` and `org.markup.poet.antora.assembler`
  - Configure Kotest dependencies for property-based testing
  - _Requirements: 1.1, 2.1_

- [x] 2. Implement Antora Resolution Library core types
  - [x] 2.1 Create ResourceCoordinate data class and ResourceType enum
    - Implement coordinate parsing logic for all types (partial$, example$, page$, image$, attachment$)
    - Handle module-qualified syntax (module:type$path)
    - Handle component-qualified syntax (component:module:type$path)
    - Support relative paths (no prefix)
    - _Requirements: 2.4, 2.5, 3.1_
  
  - [ ]* 2.2 Write property test for ResourceCoordinate parsing
    - **Property 1: Coordinate Resolution Completeness**
    - **Validates: Requirements 2.4, 2.5, 3.1**
  
  - [x] 2.3 Create ResolutionContext data class
    - Implement context creation and modification methods
    - Support component root, current module, current component, current file path
    - _Requirements: 2.3_
  
  - [x] 2.4 Create FileSystemAccess interface with expect/actual declarations
    - Define common interface for file operations
    - Implement JVM-specific file system access
    - Implement platform-specific implementations for Android, iOS, Linux
    - _Requirements: 2.1_

- [x] 3. Implement AntoraResolver interface and default implementation
  - [x] 3.1 Create AntoraResolver interface with ResolutionResult types
    - Define resolve() and resolveInclude() methods
    - Define ResolutionResult sealed class with Success and Error cases
    - Define ResolutionErrorType enum
    - _Requirements: 2.4_
  
  - [x] 3.2 Implement DefaultAntoraResolver
    - Implement resolution logic for each resource type
    - Handle module-qualified and component-qualified coordinates
    - Implement relative path resolution
    - Handle ROOT module as default
    - _Requirements: 3.1, 3.6, 3.8, 12.1, 12.2, 12.3, 12.4, 12.5_
  
  - [ ]* 3.3 Write property tests for AntoraResolver
    - **Property 1: Coordinate Resolution Completeness**
    - **Property 2: Relative Path Resolution**
    - **Property 3: Module-Qualified Resolution**
    - **Property 4: Current Module Default**
    - **Property 5: Component-Qualified Resolution**
    - **Validates: Requirements 2.4, 2.5, 3.1, 3.6, 3.8, 12.1, 12.2, 12.3, 12.5**
  
  - [x]* 3.4 Write unit tests for AntoraResolver edge cases
    - Test ROOT module handling
    - Test invalid coordinates
    - Test missing modules
    - Test file not found scenarios
    - _Requirements: 3.7, 12.4_

- [x] 4. Checkpoint - Ensure resolution library tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Implement Document Assembler configuration and core types
  - [x] 5.1 Create AssemblerConfig data class
    - Define configuration options (index file, output file, component root, max depth, etc.)
    - _Requirements: 1.1, 10.1, 13.4_
  
  - [x] 5.2 Create AssemblerResult, AssemblerError, and AssemblerWarning types
    - Define result structure with success flag, output path, errors, warnings, included files
    - Define error types enum
    - _Requirements: 11.1_
  
  - [x] 5.3 Create DocumentAssembler interface
    - Define assemble() method signature
    - _Requirements: 1.1_

- [x] 6. Implement DependencyGraph and circular dependency detection
  - [x] 6.1 Create DependencyGraph data class
    - Implement graph structure with nodes and edges
    - Track file paths and their dependencies
    - _Requirements: 1.4, 5.1_
  
  - [x] 6.2 Implement circular dependency detection algorithm
    - Implement cycle detection using depth-first search
    - Return all cycles found in the graph
    - _Requirements: 5.2, 5.3_
  
  - [x] 6.3 Implement topological sort for dependency ordering
    - Sort files in dependency order (dependencies before dependents)
    - _Requirements: 4.3_
  
  - [ ]* 6.4 Write property tests for DependencyGraph
    - **Property 14: Circular Dependency Detection**
    - **Property 16: Multiple Cycle Detection**
    - **Validates: Requirements 5.2, 5.3, 5.5**
  
  - [x]* 6.5 Write unit tests for DependencyGraph
    - Test simple A→B→A cycle
    - Test complex multi-file cycles
    - Test acyclic graphs
    - _Requirements: 5.2, 5.3, 5.5_

- [x] 7. Implement ContentMerger for include resolution and content merging
  - [x] 7.1 Create ContentMerger class
    - Implement merge() method that processes all includes
    - Handle recursive include resolution
    - Track visited files to detect cycles
    - _Requirements: 4.1, 4.2, 4.3_
  
  - [x] 7.2 Implement include directive processing
    - Resolve include paths using AntoraResolver
    - Read and parse included files
    - Handle line range filtering
    - Handle tag filtering
    - Preserve indentation
    - _Requirements: 4.1, 4.2, 4.5, 4.6, 4.7_
  
  - [x] 7.3 Implement attribute merging logic
    - Merge document attributes from included files
    - Implement first-definition-wins conflict resolution
    - Preserve attribute references
    - _Requirements: 8.2, 8.3, 8.4, 8.5_
  
  - [x] 7.4 Write unit tests for ContentMerger
    - Test include resolution and embedding
    - Test recursive include resolution
    - Test circular dependency detection
    - Test max depth enforcement
    - Test line range filtering
    - Test tag filtering
    - Test indentation preservation
    - Test attribute merging
    - _Requirements: 4.1, 4.2, 4.3, 4.5, 4.6, 4.7, 8.2, 8.3, 8.4, 8.5_
  
  - [ ]* 7.5 Write property tests for ContentMerger
    - **Property 9: Include Resolution and Embedding**
    - **Property 10: Recursive Include Resolution**
    - **Property 11: Line Range Filtering**
    - **Property 12: Tag Filtering**
    - **Property 13: Indentation Preservation**
    - **Property 19: Attribute Merging**
    - **Property 20: Attribute Reference Preservation**
    - **Validates: Requirements 4.1, 4.2, 4.3, 4.5, 4.6, 4.7, 8.2, 8.3, 8.4, 8.5**

- [x] 8. Implement cross-reference and image path resolution
  - [x] 8.1 Implement cross-reference resolution logic
    - Preserve same-file anchor references
    - Preserve cross-file anchor references for included content
    - Convert Antora xref syntax to simple anchor references
    - Maintain anchor registry
    - _Requirements: 6.1, 6.2, 6.3, 6.5_
  
  - [ ] 8.2 Implement image path resolution and updates
    - Resolve Antora image$ coordinates
    - Handle relative image paths
    - Preserve absolute image paths
    - Update paths relative to output file location
    - _Requirements: 7.1, 7.2, 7.3, 7.4_
  
  - [ ] 8.3 Write unit tests for cross-reference and image handling
    - Test same-file anchor references
    - Test cross-file anchor references
    - Test Antora xref syntax conversion
    - Test image path resolution
    - Test relative and absolute image paths
    - _Requirements: 6.1, 6.2, 6.3, 7.2, 7.3, 7.4_
  
  - [ ]* 8.4 Write property tests for cross-reference and image handling
    - **Property 17: Cross-Reference Handling**
    - **Property 18: Image Path Resolution**
    - **Validates: Requirements 6.1, 6.2, 6.3, 7.2, 7.3, 7.4**

- [x] 9. Implement DefaultDocumentAssembler
  - [x] 9.1 Create DefaultDocumentAssembler class
    - Implement main assembly workflow
    - Read and parse index file
    - Build dependency graph
    - Detect circular dependencies
    - Resolve and merge includes using ContentMerger
    - Handle errors and warnings collection
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 5.2, 5.4, 11.5_
  
  - [x] 9.2 Implement error handling and recovery
    - Continue processing after non-critical errors
    - Collect multiple errors
    - Emit warnings for non-critical issues
    - _Requirements: 5.4, 11.1, 11.5_
  
  - [x] 9.3 Implement output file writing
    - Write consolidated document to output file
    - Create output directory if needed
    - Overwrite existing files
    - Preserve UTF-8 encoding
    - _Requirements: 10.1, 10.2, 10.3, 10.5_
  
  - [x] 9.4 Write unit tests for DefaultDocumentAssembler
    - Test basic assembly workflow
    - Test index file parsing
    - Test dependency graph building
    - Test circular dependency detection
    - Test error collection
    - Test output file writing
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 5.2, 5.4, 10.1, 10.2, 10.3, 11.1, 11.5_
  
  - [ ]* 9.5 Write property tests for DocumentAssembler
    - **Property 6: Index File Parsing**
    - **Property 7: Parse Error Reporting**
    - **Property 8: Attribute Preservation**
    - **Property 15: Error Recovery Continuation**
    - **Property 21: Content Structure Preservation**
    - **Property 22: Output Validity (Round-Trip)**
    - **Property 23: UTF-8 Encoding Preservation**
    - **Property 24: Error Message Completeness**
    - **Property 25: Multiple Error Collection**
    - **Property 26: File Reuse Consistency**
    - **Property 27: Depth Limit Enforcement**
    - **Validates: Requirements 1.1, 1.3, 1.4, 1.5, 5.4, 9.1, 9.2, 9.3, 9.4, 9.5, 10.4, 10.5, 11.1, 11.5, 13.3, 13.4**

- [x] 10. Checkpoint - Ensure all assembler tests pass
  - Run all unit tests for antora-assembler module
  - Verify ContentMerger tests pass
  - Verify DependencyGraph tests pass
  - Verify DefaultDocumentAssembler tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ]* 11. Implement custom property test generators (optional)
  - [ ]* 11.1 Create Arb.resourceCoordinate() generator
    - Generate valid Antora coordinates for all types
    - Include module-qualified and component-qualified variants
    - _Requirements: 2.4, 2.5_
  
  - [ ]* 11.2 Create Arb.resolutionContext() generator
    - Generate valid resolution contexts with various configurations
    - _Requirements: 2.3_
  
  - [ ]* 11.3 Create Arb.asciidocDocument() generator
    - Generate valid AsciiDoc documents with various structures
    - Include includes, attributes, cross-references, images
    - _Requirements: 1.1_
  
  - [ ]* 11.4 Create Arb.circularDependencyGraph() generator
    - Generate dependency graphs with circular references
    - _Requirements: 5.2_
  
  - [ ]* 11.5 Create Arb.utf8String() generator
    - Generate strings with UTF-8 characters including non-ASCII
    - _Requirements: 10.5_

- [ ]* 12. Add integration tests for end-to-end scenarios (optional)
  - [ ]* 12.1 Write integration test for simple single-module assembly
    - Create test Antora structure with ROOT module
    - Test basic include resolution
    - Verify output correctness
    - _Requirements: 1.1, 4.1, 4.2_
  
  - [ ]* 12.2 Write integration test for multi-module assembly
    - Create test structure with multiple modules
    - Test cross-module includes
    - Verify module resolution
    - _Requirements: 12.1, 12.2_
  
  - [ ]* 12.3 Write integration test for complex nested includes
    - Test deeply nested include chains
    - Verify depth limit enforcement
    - _Requirements: 4.3, 13.4_
  
  - [ ]* 12.4 Write integration test for error scenarios
    - Test missing files
    - Test circular dependencies
    - Test parse errors
    - Verify error collection and reporting
    - _Requirements: 4.4, 5.2, 11.1, 11.2, 11.4_

- [x] 13. Add CLI interface (optional for JVM platform)
  - [x] 13.1 Create CLI command for document assembly
    - Parse command-line arguments
    - Invoke DocumentAssembler
    - Display results and errors
    - _Requirements: 1.1, 10.1_
  
  - [x] 13.2 Write unit tests for CLI interface
    - Test argument parsing
    - Test error display
    - _Requirements: 11.1_

- [x] 14. Final checkpoint - Complete testing and documentation
  - Run all tests across all platforms
  - Verify property tests run with 100+ iterations (if implemented)
  - Review error messages for clarity
  - Verify all required functionality is implemented
  - Ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- The resolution library is built first as it's a dependency for the assembler
- Property tests validate universal correctness properties with 100+ iterations each
- Unit tests validate specific examples and edge cases
- Integration tests verify end-to-end functionality
- Each task references specific requirements for traceability
- Platform-specific implementations use expect/actual declarations
- All code uses package `org.markup.poet.antora` and `org.markup.poet.antora.assembler`
