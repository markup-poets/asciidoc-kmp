# Implementation Plan: HTML Renderer Module

## Overview

This plan implements a cross-platform HTML renderer for AsciiDoc AST nodes. The implementation follows a visitor pattern with separate components for escaping, building HTML, and rendering different node types. All code will be in `commonMain` for cross-platform compatibility, with comprehensive property-based and unit testing.

## Tasks

- [x] 1. Set up module structure and core interfaces
  - Create `html-renderer` module directory structure following KMP conventions
  - Add module to `settings.gradle.kts`
  - Create `build.gradle.kts` with KMP configuration (Android, JVM, iOS, Linux targets)
  - Add dependency on `asciidoc-parser` module
  - Add `kotlin-test` dependency for testing
  - Create package structure: `org.markup.poet.asciidoc.render`
  - _Requirements: 9.1, 9.3_

- [x] 2. Implement HTML escaping and security
  - [x] 2.1 Create `HtmlEscaper` interface and `DefaultHtmlEscaper` implementation
    - Implement `escapeHtml()` method for content escaping (&, <, >, etc.)
    - Implement `escapeAttribute()` method for attribute value escaping (includes quotes)
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.7_
  
  - [ ]* 2.2 Write property test for HTML escaping
    - **Property 5: HTML Escaping Completeness**
    - **Validates: Requirements 4.1, 4.2, 4.3, 4.4, 4.5, 4.7**
  
  - [ ]* 2.3 Write unit tests for escaping edge cases
    - Test empty strings, strings with no special chars, strings with all special chars
    - Test attribute escaping with quotes and apostrophes
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [x] 3. Implement HTML builder
  - [x] 3.1 Create `HtmlBuilder` interface and `DefaultHtmlBuilder` implementation
    - Implement `build()` method with DSL-style block
    - Implement `openTag()` with attributes support
    - Implement `closeTag()` method
    - Implement `text()` method
    - Integrate with `HtmlEscaper` for automatic escaping
    - _Requirements: 1.1, 1.2, 1.3, 1.4_
  
  - [ ]* 3.2 Write unit tests for HTML builder
    - Test tag generation with and without attributes
    - Test proper nesting and closing
    - Test text content escaping
    - _Requirements: 1.4_

- [x] 4. Implement configuration and theming
  - [x] 4.1 Create configuration data classes
    - Create `RenderConfig` with default values
    - Create `OutputOptions` with standalone/fragment modes
    - Create `CssMode` enum (NONE, INLINE, EXTERNAL)
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7_
  
  - [x] 4.2 Create `Theme` interface and `DefaultTheme` implementation
    - Implement methods for CSS class generation (headings, paragraphs, code, tables, etc.)
    - Implement `getCss()` method with default minimal styles
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_
  
  - [ ]* 4.3 Write unit tests for configuration
    - Test default configuration values
    - Test configuration validation
    - Test theme CSS class generation
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 6.5_

- [x] 5. Implement render context
  - [x] 5.1 Create `RenderContext` class
    - Implement ID generation with collision handling
    - Implement warning collection
    - Store reference to theme and configuration
    - _Requirements: 7.5, 8.3, 12.1_
  
  - [ ]* 5.2 Write unit tests for render context
    - Test ID generation for duplicate text
    - Test warning collection
    - _Requirements: 8.3_

- [x] 6. Implement inline element rendering
  - [x] 6.1 Create `InlineRenderer` interface and `DefaultInlineRenderer` implementation
    - Implement rendering for Text (with escaping)
    - Implement rendering for Bold → `<strong>`
    - Implement rendering for Italic → `<em>`
    - Implement rendering for Code → `<code>`
    - Implement rendering for Link → `<a>` with URL sanitization
    - Implement rendering for InlineImage → `<img>` with alt text
    - Implement rendering for Subscript → `<sub>`
    - Implement rendering for Superscript → `<sup>`
    - Handle nested inline elements recursively
    - _Requirements: 1.3, 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 4.6, 8.1_
  
  - [ ]* 6.2 Write property test for inline element semantic mapping
    - **Property 3: Inline Element Semantic Mapping**
    - **Validates: Requirements 1.3, 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7**
  
  - [ ]* 6.3 Write property test for URL sanitization
    - **Property 6: URL Sanitization**
    - **Validates: Requirements 4.6**
  
  - [ ]* 6.4 Write property test for image alt text
    - **Property 15: Image Alt Text Requirement**
    - **Validates: Requirements 8.1**
  
  - [ ]* 6.5 Write unit tests for inline rendering
    - Test each inline element type with specific examples
    - Test nested inline elements (bold inside italic, etc.)
    - Test malicious URLs (javascript:, data:)
    - Test link with title attribute
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 4.6_

- [x] 7. Checkpoint - Ensure inline rendering tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 8. Implement block element rendering
  - [x] 8.1 Create `BlockRenderer` interface and `DefaultBlockRenderer` implementation
    - Implement rendering for Heading → `<h1>`-`<h6>` with ID generation
    - Implement rendering for Paragraph → `<p>`
    - Implement rendering for UnorderedList → `<ul>` with `<li>`
    - Implement rendering for OrderedList → `<ol>` with `<li>`
    - Implement rendering for CodeBlock → `<pre><code>` with language class
    - Implement rendering for Table → `<table>` with semantic structure
    - Implement rendering for Quote → `<blockquote>`
    - Implement rendering for ImageBlock → `<figure>` with `<img>` and `<figcaption>`
    - Handle unknown block types with warning
    - _Requirements: 1.2, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 6.2, 8.2, 8.5, 12.1_
  
  - [ ]* 8.2 Write property test for block element semantic mapping
    - **Property 2: Block Element Semantic Mapping**
    - **Validates: Requirements 1.2, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8**
  
  - [ ]* 8.3 Write property test for theme CSS class application
    - **Property 11: Theme CSS Class Application**
    - **Validates: Requirements 6.1, 6.2, 6.3, 6.4**
  
  - [ ]* 8.4 Write property test for code block language identification
    - **Property 18: Code Block Language Identification**
    - **Validates: Requirements 8.5**
  
  - [ ]* 8.5 Write property test for table header semantics
    - **Property 16: Table Header Semantics**
    - **Validates: Requirements 8.2**
  
  - [ ]* 8.6 Write unit tests for block rendering
    - Test each block element type with specific examples
    - Test heading ID generation and collision handling
    - Test code block with and without language
    - Test table with and without header
    - Test quote with and without attribution
    - Test image block with and without caption
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8_

- [x] 9. Implement AST visitor
  - [x] 9.1 Create `AstVisitor` class
    - Implement `visit()` method with node type dispatch
    - Handle Document nodes by visiting all blocks
    - Delegate to BlockRenderer for block elements
    - Delegate to InlineRenderer for inline elements
    - Log warnings for unknown node types
    - _Requirements: 1.1, 1.2, 1.3, 12.1_
  
  - [ ]* 9.2 Write property test for HTML nesting preservation
    - **Property 4: HTML Nesting Preservation**
    - **Validates: Requirements 1.4, 3.8**
  
  - [ ]* 9.3 Write property test for unknown node warning
    - **Property 27: Unknown Node Warning**
    - **Validates: Requirements 12.1**
  
  - [ ]* 9.4 Write unit tests for AST visitor
    - Test document traversal
    - Test unknown node type handling
    - _Requirements: 1.1, 12.1_

- [x] 10. Checkpoint - Ensure core rendering tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 11. Implement main HTML renderer
  - [x] 11.1 Create `HtmlRenderer` interface and `DefaultHtmlRenderer` implementation
    - Implement `render()` method returning `Result<String>`
    - Validate configuration before rendering
    - Create RenderContext and AstVisitor
    - Handle standalone vs fragment mode
    - Generate document structure with `<html>`, `<head>`, `<body>` for standalone
    - Include metadata tags (title, author, description, keywords)
    - Include CSS based on CssMode configuration
    - Apply custom attributes to root element
    - Collect and return warnings
    - _Requirements: 1.1, 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 7.1, 7.2, 7.3, 7.4, 7.6, 12.4_
  
  - [ ]* 11.2 Write property test for document structure completeness
    - **Property 1: Document Structure Completeness**
    - **Validates: Requirements 1.1, 5.1**
  
  - [ ]* 11.3 Write property test for fragment mode exclusion
    - **Property 7: Fragment Mode Exclusion**
    - **Validates: Requirements 5.2**
  
  - [ ]* 11.4 Write property test for CSS mode correspondence
    - **Property 8: CSS Mode Correspondence**
    - **Validates: Requirements 5.3, 5.4, 5.5**
  
  - [ ]* 11.5 Write property test for custom attributes application
    - **Property 9: Custom Attributes Application**
    - **Validates: Requirements 5.6**
  
  - [ ]* 11.6 Write property test for title tag inclusion
    - **Property 10: Title Tag Inclusion**
    - **Validates: Requirements 5.7, 7.4**
  
  - [ ]* 11.7 Write property test for metadata tag generation
    - **Property 12: Metadata Tag Generation**
    - **Validates: Requirements 7.1, 7.2, 7.3**
  
  - [ ]* 11.8 Write property test for language attribute presence
    - **Property 14: Language Attribute Presence**
    - **Validates: Requirements 7.6**
  
  - [ ]* 11.9 Write property test for invalid configuration error reporting
    - **Property 29: Invalid Configuration Error Reporting**
    - **Validates: Requirements 12.4**
  
  - [ ]* 11.10 Write unit tests for main renderer
    - Test standalone document generation
    - Test fragment generation
    - Test CSS inclusion modes
    - Test metadata tag generation
    - Test custom attributes
    - Test configuration validation errors
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 7.1, 7.2, 7.3, 7.6, 12.4_

- [x] 12. Implement advanced features
  - [x] 12.1 Add table of contents rendering support
    - Implement TOC node rendering as `<nav>` with nested lists
    - Generate links to heading anchors
    - Add proper ARIA attributes for accessibility
    - _Requirements: 7.5, 11.4_
  
  - [x] 12.2 Add heading hierarchy validation
    - Track heading levels during rendering
    - Detect and warn about skipped levels
    - _Requirements: 8.3_
  
  - [ ]* 12.3 Write property test for table of contents structure
    - **Property 13: Table of Contents Structure**
    - **Validates: Requirements 7.5, 11.4**
  
  - [ ]* 12.4 Write property test for heading hierarchy preservation
    - **Property 17: Heading Hierarchy Preservation**
    - **Validates: Requirements 8.3**
  
  - [ ]* 12.5 Write unit tests for advanced features
    - Test TOC rendering with multiple heading levels
    - Test heading hierarchy validation
    - _Requirements: 7.5, 8.3_

- [x] 13. Implement extensibility features
  - [x] 13.1 Add custom renderer support
    - Create `CustomRenderer` interface
    - Add custom renderer registry to RenderConfig
    - Modify AstVisitor to check for custom renderers
    - Implement fallback to default renderers
    - _Requirements: 10.1, 10.2_
  
  - [x] 13.2 Add attribute handler support
    - Create `AttributeHandler` interface
    - Add attribute handler registry to RenderConfig
    - Invoke handlers during rendering
    - _Requirements: 10.4, 11.1_
  
  - [x] 13.3 Add custom template support
    - Create template interface for document structure
    - Allow template override in configuration
    - _Requirements: 10.5_
  
  - [ ]* 13.4 Write property test for custom renderer invocation
    - **Property 19: Custom Renderer Invocation**
    - **Validates: Requirements 10.1**
  
  - [ ]* 13.5 Write property test for default renderer fallback
    - **Property 20: Default Renderer Fallback**
    - **Validates: Requirements 10.2**
  
  - [ ]* 13.6 Write property test for attribute handler application
    - **Property 21: Attribute Handler Application**
    - **Validates: Requirements 10.4**
  
  - [ ]* 13.7 Write property test for custom template usage
    - **Property 22: Custom Template Usage**
    - **Validates: Requirements 10.5**
  
  - [ ]* 13.8 Write unit tests for extensibility
    - Test custom renderer registration and invocation
    - Test attribute handler registration and invocation
    - Test custom template usage
    - _Requirements: 10.1, 10.2, 10.4, 10.5_

- [x] 14. Checkpoint - Ensure extensibility tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 15. Implement document processing integration
  - [x] 15.1 Add support for resolved attributes
    - Use attribute values from AST in rendering decisions
    - Apply attributes to CSS classes and data attributes
    - _Requirements: 11.1_
  
  - [x] 15.2 Add support for cross-references
    - Render cross-reference nodes as anchor links
    - Use resolved target IDs for href attributes
    - _Requirements: 11.2_
  
  - [x] 15.3 Add support for includes
    - Render included content inline at include location
    - _Requirements: 11.3_
  
  - [x] 15.4 Add support for macro expansions
    - Render expanded macro content appropriately
    - _Requirements: 11.5_
  
  - [ ]* 15.5 Write property test for attribute value rendering
    - **Property 23: Attribute Value Rendering**
    - **Validates: Requirements 11.1**
  
  - [ ]* 15.6 Write property test for cross-reference link generation
    - **Property 24: Cross-Reference Link Generation**
    - **Validates: Requirements 11.2**
  
  - [ ]* 15.7 Write property test for include content inlining
    - **Property 25: Include Content Inlining**
    - **Validates: Requirements 11.3**
  
  - [ ]* 15.8 Write property test for macro expansion rendering
    - **Property 26: Macro Expansion Rendering**
    - **Validates: Requirements 11.5**
  
  - [ ]* 15.9 Write unit tests for document processing integration
    - Test attribute-based rendering
    - Test cross-reference link generation
    - Test include rendering
    - Test macro expansion rendering
    - _Requirements: 11.1, 11.2, 11.3, 11.5_

- [x] 16. Implement error handling
  - [x] 16.1 Create exception classes
    - Create `RenderException` sealed class
    - Create `InvalidAst` exception
    - Create `InvalidConfiguration` exception
    - Create `ValidationFailure` exception
    - _Requirements: 12.2, 12.3, 12.4_
  
  - [x] 16.2 Add error handling throughout renderer
    - Wrap rendering in try-catch blocks
    - Return Result.failure for critical errors
    - Collect warnings for non-critical issues
    - Provide partial results when possible
    - _Requirements: 12.1, 12.2, 12.5_
  
  - [ ]* 16.3 Write property test for malformed AST error reporting
    - **Property 28: Malformed AST Error Reporting**
    - **Validates: Requirements 12.2**
  
  - [ ]* 16.4 Write property test for graceful degradation
    - **Property 30: Graceful Degradation**
    - **Validates: Requirements 12.5**
  
  - [ ]* 16.5 Write unit tests for error handling
    - Test malformed AST handling
    - Test invalid configuration handling
    - Test partial result generation
    - Test warning collection
    - _Requirements: 12.1, 12.2, 12.4, 12.5_

- [x] 17. Create integration tests
  - [ ]* 17.1 Write end-to-end integration tests
    - Test complete document rendering with all element types
    - Test rendering with different configurations
    - Test rendering with custom renderers and themes
    - Test integration with document-processing module
    - _Requirements: 1.1, 1.2, 1.3, 5.1, 5.2, 10.1, 11.1, 11.2_

- [x] 18. Final checkpoint - Ensure all tests pass
  - Run all tests across all platforms
  - Verify property tests run with 100+ iterations
  - Ensure all 30 correctness properties are tested
  - Ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties (minimum 100 iterations each)
- Unit tests validate specific examples and edge cases
- All code goes in `commonMain` for cross-platform compatibility
- Package: `org.markup.poet.asciidoc.render`
- Module depends on `asciidoc-parser` module for AST node types
