# Implementation Plan: CLI Document Processor

## Overview

This implementation plan adds document processing capabilities to the existing CLI application. The tasks build on the existing convert functionality while adding a new process command that leverages the document-processing module. Tasks are organized to refactor existing code first, then add new functionality incrementally.

## Tasks

- [x] 1. Refactor existing CLI into command pattern
  - [x] 1.1 Create CliCommand interface and CommandResult sealed class
    - Define base interface for all commands
    - Create result types for success and error cases
    - _Requirements: 7.1, 7.3_
  
  - [x] 1.2 Create CommandArgs data class and ArgumentParser
    - Implement argument parsing logic
    - Support positional args, options, and flags
    - Handle both short (-o) and long (--output) option forms
    - _Requirements: 4.4, 4.5_
  
  - [x] 1.3 Extract existing conversion logic into ConvertCommand
    - Move existing Main.kt logic into ConvertCommand class
    - Implement CliCommand interface
    - Maintain existing functionality
    - _Requirements: 7.1_
  
  - [ ]* 1.4 Write unit tests for ArgumentParser
    - Test parsing various argument combinations
    - Test short and long option equivalence
    - _Requirements: 4.4, 4.5_
  
  - [ ]* 1.5 Write unit tests for ConvertCommand
    - Test existing conversion functionality still works
    - _Requirements: 7.1_

- [x] 2. Implement command routing infrastructure
  - [x] 2.1 Create CommandRouter class
    - Implement command registration and routing
    - Handle backward compatibility (no subcommand defaults to convert)
    - _Requirements: 7.2, 7.3_
  
  - [x] 2.2 Implement help system
    - Show usage when no arguments provided
    - Show command list with descriptions
    - Support --help flag for detailed help
    - _Requirements: 4.2, 4.3, 7.5_
  
  - [x] 2.3 Update Main.kt to use CommandRouter
    - Wire up router with commands
    - Handle exit codes properly
    - _Requirements: 1.5, 3.4_
  
  - [ ]* 2.4 Write unit tests for CommandRouter
    - Test command routing logic
    - Test backward compatibility
    - Test help display
    - _Requirements: 7.2, 7.3, 4.2, 4.3_

- [x] 3. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. Implement JVM FileReader for document processing
  - [x] 4.1 Create JvmFileReader implementation
    - Implement FileReader interface using Java File I/O
    - Handle file not found errors
    - Handle read errors with descriptive messages
    - _Requirements: 1.4, 6.5_
  
  - [ ]* 4.2 Write unit tests for JvmFileReader
    - Test reading existing files
    - Test error handling for missing files
    - _Requirements: 1.4, 6.5_

- [x] 5. Implement ProcessCommand core functionality
  - [x] 5.1 Create ProcessCommand class skeleton
    - Implement CliCommand interface
    - Define command name and description
    - _Requirements: 4.1_
  
  - [x] 5.2 Implement argument parsing for process command
    - Parse input file (required positional argument)
    - Parse output file option (-o, --output)
    - Parse base path option (-b, --base-path)
    - Parse max depth option (--max-depth)
    - Parse verbose flag (-v, --verbose)
    - Parse no-overwrite flag (--no-overwrite)
    - _Requirements: 1.2, 2.1, 2.3, 3.5, 6.2_
  
  - [x] 5.3 Implement input file validation
    - Check input file exists
    - Report error with file path if not found
    - _Requirements: 1.4_
  
  - [x] 5.4 Implement output file overwrite checking
    - Check if output file exists when no-overwrite flag set
    - Report error if exists and no-overwrite is set
    - _Requirements: 6.2_
  
  - [x] 5.5 Implement base path determination
    - Use specified base path if provided
    - Default to input file directory if not specified
    - _Requirements: 2.1, 2.2_
  
  - [ ]* 5.6 Write unit tests for ProcessCommand argument parsing
    - Test various argument combinations
    - Test default values
    - _Requirements: 1.2, 2.1, 2.3_
  
  - [ ]* 5.7 Write property test for base path resolution
    - **Property 5: Base Path Resolution**
    - **Validates: Requirements 2.1, 2.2**

- [x] 6. Implement document processing logic
  - [x] 6.1 Implement document reading and parsing
    - Read input file content
    - Parse AsciiDoc to AST using DefaultAsciidocParser
    - Handle parse errors
    - _Requirements: 1.1_
  
  - [x] 6.2 Create ProcessingConfig from command options
    - Set enableIncludes to true
    - Set maxIncludeDepth from option (default 10)
    - Disable other processors (attributes, cross-refs, TOC, macros)
    - _Requirements: 2.3, 2.4_
  
  - [x] 6.3 Implement include resolution
    - Create IncludeConfig with base path and file reader
    - Call DefaultIncludeResolver to resolve includes
    - _Requirements: 1.1, 2.1_
  
  - [x] 6.4 Implement error reporting
    - Collect all processing errors
    - Format errors with file path and line number
    - Report all errors before exiting
    - Display warnings to stderr
    - _Requirements: 3.1, 3.2, 3.3_
  
  - [x] 6.5 Implement verbose output
    - Display processing steps when verbose flag set
    - Show included files
    - Show processing summary
    - _Requirements: 3.5_
  
  - [ ]* 6.6 Write property test for include resolution completeness
    - **Property 1: Include Resolution Completeness**
    - **Validates: Requirements 1.1, 5.2**
  
  - [ ]* 6.7 Write property test for depth limiting
    - **Property 6: Depth Limiting**
    - **Validates: Requirements 2.3**
  
  - [ ]* 6.8 Write property test for depth violation reporting
    - **Property 7: Depth Violation Reporting**
    - **Validates: Requirements 2.5**
  
  - [ ]* 6.9 Write property test for error collection
    - **Property 9: Error Collection**
    - **Validates: Requirements 3.2**

- [x] 7. Implement output generation
  - [x] 7.1 Create AsciiDoc pretty printer
    - Convert processed AST back to AsciiDoc text
    - Preserve formatting and structure
    - Handle all AST node types
    - _Requirements: 5.1, 5.3, 5.5_
  
  - [x] 7.2 Create OutputWriter interface and implementations
    - Create FileOutputWriter for file output
    - Create StdoutOutputWriter for stdout output
    - Handle directory creation for file output
    - _Requirements: 1.2, 1.3, 6.3_
  
  - [x] 7.3 Implement output writing logic
    - Write to file if output path specified
    - Write to stdout if no output path
    - Create parent directories if needed
    - Handle write errors with descriptive messages
    - _Requirements: 1.2, 1.3, 6.3, 6.4_
  
  - [ ]* 7.4 Write property test for output file writing
    - **Property 2: Output File Writing**
    - **Validates: Requirements 1.2**
  
  - [ ]* 7.5 Write property test for output validity
    - **Property 14: Output Validity**
    - **Validates: Requirements 5.1**
  
  - [ ]* 7.6 Write property test for content preservation
    - **Property 17: Content Preservation**
    - **Validates: Requirements 5.5**
  
  - [ ]* 7.7 Write unit tests for OutputWriter implementations
    - Test file writing
    - Test stdout writing
    - Test directory creation
    - _Requirements: 1.2, 1.3, 6.3_

- [x] 8. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 9. Implement exit code handling
  - [x] 9.1 Implement proper exit code logic
    - Return 0 on successful processing
    - Return non-zero on any errors
    - _Requirements: 1.5, 3.4_
  
  - [ ]* 9.2 Write property test for exit code correctness
    - **Property 4: Exit Code Correctness**
    - **Validates: Requirements 1.5, 3.4**

- [x] 10. Implement remaining error handling
  - [x] 10.1 Implement comprehensive error reporting format
    - Format all errors consistently
    - Include file path and line number
    - Group multiple errors together
    - _Requirements: 3.1, 3.2, 8.1_
  
  - [x] 10.2 Implement error handling consistency across commands
    - Use same error format for convert and process commands
    - _Requirements: 7.4_
  
  - [ ]* 10.3 Write property test for comprehensive error reporting
    - **Property 8: Comprehensive Error Reporting**
    - **Validates: Requirements 3.1, 6.4, 6.5**
  
  - [ ]* 10.4 Write property test for error handling consistency
    - **Property 21: Error Handling Consistency**
    - **Validates: Requirements 7.4**

- [x] 11. Integration and end-to-end testing
  - [x] 11.1 Wire ProcessCommand into CommandRouter
    - Register process command
    - Test command routing
    - _Requirements: 4.1, 7.3_
  
  - [x] 11.2 Update help and usage messages
    - Add process command to help output
    - Document all options and flags
    - _Requirements: 4.2, 4.3, 7.5_
  
  - [ ]* 11.3 Write integration tests for complete workflows
    - Test processing documents with includes
    - Test error scenarios end-to-end
    - Test backward compatibility with convert command
    - _Requirements: 1.1, 7.1, 7.2_
  
  - [ ]* 11.4 Write property test for input validation
    - **Property 3: Input Validation**
    - **Validates: Requirements 1.4**
  
  - [ ]* 11.5 Write property test for invalid argument handling
    - **Property 12: Invalid Argument Handling**
    - **Validates: Requirements 4.4**
  
  - [ ]* 11.6 Write property test for option name equivalence
    - **Property 13: Option Name Equivalence**
    - **Validates: Requirements 4.5**
  
  - [ ]* 11.7 Write property test for subcommand routing
    - **Property 20: Subcommand Routing**
    - **Validates: Requirements 7.3**

- [x] 12. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties
- Unit tests validate specific examples and edge cases
- The implementation builds on existing CLI infrastructure
- Document processing module must be complete before starting these tasks
- AsciiDoc pretty printer is needed to convert AST back to text format
