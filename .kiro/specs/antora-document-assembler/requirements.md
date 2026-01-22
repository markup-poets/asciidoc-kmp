# Requirements Document: Antora Document Assembler

## Introduction

The Antora Document Assembler is a tool that creates a single consolidated AsciiDoc document from multiple files organized in an Antora directory structure. This feature enables users to generate standalone documentation files from modular Antora-based documentation projects, facilitating distribution, PDF conversion, and simplified review processes.

The assembler consists of two main components:
1. **Antora Resolution Library**: A reusable library that understands Antora directory structure and conventions, resolving resource coordinates to file paths. This library can be used by other tools that need to work with Antora-structured documentation.
2. **Document Assembler**: A tool that uses the resolution library to read an index file, resolve all dependencies, and produce a single consolidated AsciiDoc file.

This modular design allows the Antora resolution logic to be reused in other contexts, such as IDE plugins, documentation validators, or custom processing tools.

## Glossary

- **Antora**: A documentation site generator that uses a specific directory structure for organizing content
- **Module**: A logical grouping of documentation content within an Antora project (e.g., ROOT, admin, api)
- **Component**: The top-level organizational unit in Antora (typically represents a product or project)
- **Index_File**: The entry point AsciiDoc file that defines the structure of the assembled document
- **Assembler**: The system that processes Antora files and creates a consolidated document
- **Include_Directive**: AsciiDoc syntax for embedding content from other files (e.g., `include::partial$file.adoc[]`)
- **Resource_Coordinate**: Antora's syntax for referencing files using prefixes like `partial$`, `example$`, `page$`, `image$`
- **Dependency_Graph**: The tree structure of all files referenced directly or indirectly from the index file
- **Circular_Dependency**: A situation where file A includes file B, which includes file A (directly or indirectly)
- **Consolidated_Document**: The single output AsciiDoc file containing all assembled content
- **Parser**: The existing AsciiDoc parser in the asciidoc-kmp project
- **File_Resolver**: Component that translates Antora coordinates to actual file paths
- **Antora_Resolution_Library**: A reusable library providing Antora directory structure understanding and resource resolution
- **Resolution_Context**: The configuration and state needed to resolve Antora coordinates (component root, current module, etc.)

## Requirements

### Requirement 1: Antora Resolution Library

**User Story:** As a tool developer, I want a reusable library for Antora resource resolution, so that I can build tools that work with Antora-structured documentation without reimplementing resolution logic.

#### Acceptance Criteria

1. THE Antora_Resolution_Library SHALL provide a public API for resolving resource coordinates to file paths
2. THE Antora_Resolution_Library SHALL be independent of the document assembler functionality
3. THE Antora_Resolution_Library SHALL accept a Resolution_Context containing component root and current module information
4. WHEN given a resource coordinate and context, THE Antora_Resolution_Library SHALL return the resolved file path or an error
5. THE Antora_Resolution_Library SHALL support all Antora resource coordinate types (partial$, example$, page$, image$)
6. THE Antora_Resolution_Library SHALL be usable from other Kotlin Multiplatform modules
7. THE Antora_Resolution_Library SHALL not depend on the AsciiDoc parser or assembler components

### Requirement 2: Index File Processing

**User Story:** As a documentation author, I want to specify an index file as the entry point, so that I can define the structure of my assembled document.

#### Acceptance Criteria

1. WHEN an index file path is provided, THE Assembler SHALL read and parse the file as valid AsciiDoc
2. WHEN the index file does not exist, THE Assembler SHALL return an error indicating the file was not found
3. WHEN the index file contains invalid AsciiDoc syntax, THE Assembler SHALL return a descriptive parse error
4. THE Assembler SHALL treat the index file as the root of the dependency graph
5. WHEN the index file contains document attributes, THE Assembler SHALL preserve them in the consolidated document

### Requirement 3: Antora Directory Structure Resolution

**User Story:** As a documentation author, I want the assembler to understand Antora's directory structure, so that my includes resolve correctly without manual path adjustments.

#### Acceptance Criteria

1. WHEN resolving a resource coordinate, THE File_Resolver SHALL search in the appropriate Antora directory based on the coordinate prefix
2. WHEN a `partial$` coordinate is encountered, THE File_Resolver SHALL resolve the path relative to the `partials/` directory
3. WHEN an `example$` coordinate is encountered, THE File_Resolver SHALL resolve the path relative to the `examples/` directory
4. WHEN a `page$` coordinate is encountered, THE File_Resolver SHALL resolve the path relative to the `pages/` directory
5. WHEN an `image$` coordinate is encountered, THE File_Resolver SHALL resolve the path relative to the `images/` directory
6. WHEN no coordinate prefix is specified, THE File_Resolver SHALL resolve the path relative to the current file's directory
7. WHEN a file is in the ROOT module, THE File_Resolver SHALL search in `modules/ROOT/{type}/` directory
8. WHEN a file is in a named module, THE File_Resolver SHALL search in `modules/{module-name}/{type}/` directory

### Requirement 4: Include Directive Processing

**User Story:** As a documentation author, I want all include directives to be resolved and embedded, so that my consolidated document contains all referenced content.

#### Acceptance Criteria

1. WHEN an include directive is encountered, THE Assembler SHALL resolve the file path according to Antora conventions
2. WHEN an included file is found, THE Assembler SHALL embed its content at the include location
3. WHEN an included file contains include directives, THE Assembler SHALL recursively resolve and embed them
4. WHEN an included file is not found, THE Assembler SHALL return an error with the file path and include location
5. WHEN an include directive specifies line ranges, THE Assembler SHALL include only the specified lines
6. WHEN an include directive specifies tags, THE Assembler SHALL include only content within the specified tags
7. THE Assembler SHALL preserve the indentation level of included content relative to the include directive

### Requirement 5: Circular Dependency Detection

**User Story:** As a documentation author, I want circular dependencies to be detected and reported, so that I can fix structural issues in my documentation.

#### Acceptance Criteria

1. WHEN processing includes, THE Assembler SHALL track all files in the current inclusion chain
2. WHEN a file is included that is already in the inclusion chain, THE Assembler SHALL detect a circular dependency
3. WHEN a circular dependency is detected, THE Assembler SHALL return an error listing the complete dependency cycle
4. THE Assembler SHALL continue processing other includes after detecting a circular dependency in one branch
5. WHEN multiple circular dependencies exist, THE Assembler SHALL report all of them

### Requirement 6: Cross-Reference Handling

**User Story:** As a documentation author, I want cross-references between files to be preserved or resolved, so that my consolidated document maintains navigable links.

#### Acceptance Criteria

1. WHEN a cross-reference uses an anchor within the same file, THE Assembler SHALL preserve the reference unchanged
2. WHEN a cross-reference points to an anchor in an included file, THE Assembler SHALL preserve the anchor reference
3. WHEN a cross-reference uses Antora xref syntax with file coordinates, THE Assembler SHALL resolve it to a simple anchor reference
4. WHEN a cross-reference target cannot be resolved, THE Assembler SHALL preserve the original reference and emit a warning
5. THE Assembler SHALL maintain a registry of all anchors and IDs in the consolidated document

### Requirement 7: Image Reference Resolution

**User Story:** As a documentation author, I want image references to be updated with correct paths, so that images display correctly in the consolidated document.

#### Acceptance Criteria

1. WHEN an image directive uses Antora `image$` coordinate, THE Assembler SHALL resolve the path relative to the images directory
2. WHEN an image path is relative, THE Assembler SHALL resolve it relative to the source file's location
3. WHEN an image path is absolute, THE Assembler SHALL preserve it unchanged
4. THE Assembler SHALL update image paths in the consolidated document to be relative to the output file location
5. WHEN an image file does not exist, THE Assembler SHALL emit a warning but continue processing

### Requirement 8: Document Attribute Preservation

**User Story:** As a documentation author, I want document attributes to be preserved and merged, so that my consolidated document maintains proper configuration.

#### Acceptance Criteria

1. WHEN the index file defines document attributes, THE Assembler SHALL include them in the consolidated document header
2. WHEN included files define document attributes, THE Assembler SHALL merge them with existing attributes
3. WHEN attribute conflicts occur, THE Assembler SHALL use the value from the index file (first definition wins)
4. THE Assembler SHALL preserve attribute references throughout the document
5. WHEN an attribute is undefined, THE Assembler SHALL preserve the attribute reference syntax

### Requirement 9: Content Formatting Preservation

**User Story:** As a documentation author, I want all AsciiDoc formatting to be preserved, so that my consolidated document renders identically to the original modular structure.

#### Acceptance Criteria

1. THE Assembler SHALL preserve all block structures (paragraphs, lists, tables, code blocks)
2. THE Assembler SHALL preserve all inline formatting (bold, italic, monospace, subscript, superscript)
3. THE Assembler SHALL preserve all section headings with their levels
4. THE Assembler SHALL preserve all block attributes and options
5. THE Assembler SHALL preserve all comments in the source files

### Requirement 10: Output Generation

**User Story:** As a documentation author, I want to generate a single consolidated AsciiDoc file, so that I can distribute or process it as a standalone document.

#### Acceptance Criteria

1. WHEN assembly is successful, THE Assembler SHALL write the consolidated content to the specified output file
2. WHEN the output file already exists, THE Assembler SHALL overwrite it
3. WHEN the output directory does not exist, THE Assembler SHALL create it
4. THE Assembler SHALL write valid AsciiDoc syntax to the output file
5. THE Assembler SHALL preserve UTF-8 encoding in the output file

### Requirement 11: Error Reporting

**User Story:** As a documentation author, I want clear error messages when assembly fails, so that I can quickly identify and fix issues.

#### Acceptance Criteria

1. WHEN an error occurs, THE Assembler SHALL return an error message indicating the error type and location
2. WHEN a file is not found, THE Assembler SHALL include the file path and the include directive location
3. WHEN a circular dependency is detected, THE Assembler SHALL list all files in the dependency cycle
4. WHEN a parse error occurs, THE Assembler SHALL include the file path and line number
5. THE Assembler SHALL collect and report multiple errors when possible rather than stopping at the first error

### Requirement 12: Module and Component Support

**User Story:** As a documentation author, I want to assemble documents from multiple modules and components, so that I can create comprehensive documentation from modular sources.

#### Acceptance Criteria

1. WHEN an include references a different module, THE File_Resolver SHALL resolve the path to the target module's directory
2. WHEN an include uses module-qualified syntax (e.g., `module:page$file.adoc`), THE File_Resolver SHALL resolve to the specified module
3. WHEN no module is specified, THE File_Resolver SHALL assume the current module
4. THE Assembler SHALL support the ROOT module as the default module
5. WHEN a component is specified in an include, THE File_Resolver SHALL resolve to the target component's directory structure

### Requirement 13: Performance and Scalability

**User Story:** As a documentation author, I want the assembler to handle large documentation sets efficiently, so that I can assemble comprehensive documents without excessive wait times.

#### Acceptance Criteria

1. THE Assembler SHALL cache parsed files to avoid re-parsing the same file multiple times
2. THE Assembler SHALL process includes in a single pass through the dependency graph
3. WHEN a file is included multiple times, THE Assembler SHALL reuse the cached parsed content
4. THE Assembler SHALL limit recursion depth to prevent stack overflow on deeply nested includes
5. WHEN the recursion depth limit is exceeded, THE Assembler SHALL return an error indicating excessive nesting
