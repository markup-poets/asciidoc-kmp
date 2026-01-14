# Implementation Plan: AST to Graphviz Export

## Overview

This implementation plan creates a standalone Gradle module that exports AsciiDoc AST structures to Graphviz DOT format. The module follows a visitor pattern to traverse AST nodes, extract visualization data, and generate properly formatted DOT syntax with visual styling.

## Tasks

- [x] 1. Set up standalone Gradle module structure
  - Create `ast-graphviz-export` directory and module configuration
  - Configure `build.gradle.kts` with Kotlin Multiplatform setup
  - Add dependency on core library module
  - Update root `settings.gradle.kts` to include new module
  - _Requirements: 1.1, 1.2, 1.4_

- [x] 2. Implement core data models and configuration
  - [x] 2.1 Create export configuration and styling enums
    - Implement `ExportConfig`, `ColorScheme`, `NodeShape`, `GraphOrientation` classes
    - Define visual styling data classes (`NodeStyle`, `EdgeStyle`)
    - _Requirements: 5.3_

  - [ ]* 2.2 Write property test for configuration validation
    - **Property 7: Error Handling for Invalid Input**
    - **Validates: Requirements 5.4, 5.5**

  - [x] 2.3 Create graph data structures
    - Implement `NodeData`, `EdgeData`, `GraphData`, `GraphMetadata` classes
    - Create `NodeIdGenerator` for unique ID generation
    - _Requirements: 3.2_

  - [ ]* 2.4 Write property test for unique node identification
    - **Property 3: Unique Node Identification**
    - **Validates: Requirements 3.2**

- [x] 3. Implement AST visitor and traversal engine
  - [x] 3.1 Create AST visitor interface and implementation
    - Implement `AstVisitor` interface and `GraphvizAstVisitor` class
    - Add recursive traversal logic for all AST node types
    - Implement metadata extraction from nodes
    - _Requirements: 2.1, 2.2, 2.4, 2.5_

  - [ ]* 3.2 Write property test for complete AST traversal
    - **Property 1: Complete AST Traversal**
    - **Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5**

  - [x] 3.3 Add parent-child relationship tracking
    - Implement edge collection during traversal
    - Ensure proper parent-child relationship preservation
    - _Requirements: 2.3, 3.3_

  - [ ]* 3.4 Write property test for parent-child edge representation
    - **Property 4: Parent-Child Edge Representation**
    - **Validates: Requirements 3.3**

- [x] 4. Implement DOT format generation
  - [x] 4.1 Create DOT builder with syntax generation
    - Implement `DotBuilder` class with header, node, and edge generation
    - Add proper DOT syntax formatting and structure
    - _Requirements: 3.1, 3.4_

  - [ ]* 4.2 Write property test for valid DOT format generation
    - **Property 2: Valid DOT Format Generation**
    - **Validates: Requirements 3.1, 5.2**

  - [x] 4.3 Implement special character escaping
    - Add escaping logic for quotes, backslashes, newlines in labels
    - Handle attribute value escaping
    - _Requirements: 3.5_

  - [ ]* 4.4 Write property test for special character escaping
    - **Property 5: Special Character Escaping**
    - **Validates: Requirements 3.5**

- [x] 5. Checkpoint - Ensure core functionality tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. Implement visual styling and differentiation
  - [x] 6.1 Create node styling engine
    - Implement `NodeStyler` class with color scheme support
    - Add styling rules for block vs inline elements
    - Implement section level visual indicators
    - _Requirements: 4.1, 4.3, 4.5_

  - [ ]* 6.2 Write property test for visual node type differentiation
    - **Property 6: Visual Node Type Differentiation**
    - **Validates: Requirements 4.1, 4.3, 4.5**

  - [x] 6.3 Add special styling for document root and lists
    - Implement distinctive Document node styling
    - Add list grouping visual features
    - _Requirements: 4.2, 4.4_

  - [ ]* 6.4 Write unit tests for document root styling
    - Test Document node special styling
    - Test list grouping features
    - _Requirements: 4.2, 4.4_

- [x] 7. Implement main export API
  - [x] 7.1 Create primary GraphvizExporter class
    - Implement main export function accepting Document nodes
    - Wire together visitor, builder, and styler components
    - Add configuration support
    - _Requirements: 5.1, 5.2, 5.3_

  - [x] 7.2 Write unit tests for export API
    - Test main export function with various Document types
    - Test configuration option handling
    - _Requirements: 5.1, 5.2, 5.3_

  - [ ] 7.3 Add error handling for invalid inputs
    - Implement graceful handling of null/empty documents
    - Add clear error messages for invalid configurations
    - _Requirements: 5.4, 5.5_

- [x] 8. Implement file I/O functionality
  - [x] 8.1 Add file export capabilities
    - Implement `exportToFile` function with path handling
    - Add parent directory creation logic
    - Implement file system error handling
    - _Requirements: 6.1, 6.2, 6.4, 6.5_

  - [ ]* 8.2 Write property test for file path handling
    - **Property 8: File Path Handling**
    - **Validates: Requirements 6.3, 6.4**

  - [ ]* 8.3 Write unit tests for file I/O operations
    - Test successful file creation and writing
    - Test error handling for file system issues
    - Test parent directory creation
    - _Requirements: 6.1, 6.2, 6.5_

- [x] 9. Integration and comprehensive testing
  - [x] 9.1 Create integration tests with sample documents
    - Test complete export workflow with real AsciiDoc documents
    - Verify end-to-end functionality across all node types
    - _Requirements: 2.4, 3.1_

  - [ ]* 9.2 Write performance tests for large AST structures
    - Test export performance with deeply nested documents
    - Test memory usage with large document structures
    - _Requirements: 2.1_

- [ ] 10. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Property tests validate universal correctness properties using Kotest framework
- Unit tests validate specific examples and edge cases
- Integration tests ensure end-to-end functionality works correctly
- The module dependency has been corrected to use `:asciidoc-parser` instead of `:library`