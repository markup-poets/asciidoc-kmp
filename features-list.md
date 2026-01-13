Based on your comprehensive AsciiDoc converter specification, here are the key features to implement, organized by priority and complexity:

## Core Parser Features
- Line-based document parsing with state machines
- AST node types (Document, Section, Paragraph, List, etc.)
- Block element recognition (headers, paragraphs, lists, code blocks)
- Inline markup parsing (bold, italic, code spans, links)
- Attribute parsing and storage
- Comment handling

## Document Processing Features
- Include directive resolution (`include::file[]`)
- Attribute substitution (`{version}`, `{author}`)
- Cross-reference resolution
- Table of contents generation
- Document validation and normalization
- Macro expansion system

## HTML Converter Features
- Document structure to HTML mapping
- Section hierarchy with proper heading levels
- List rendering (ordered, unordered, definition lists)
- Table generation
- Code block syntax highlighting hooks
- Link and image processing
- CSS class generation for styling

## Extension System
- Plugin architecture for custom processors
- Custom block and inline element support
- Treeprocessor interface for AST manipulation
- Custom converter registration
- Hook system for processing phases

## CLI and Platform Features
- Command-line interface with file I/O
- Batch processing capabilities
- Configuration file support
- Error reporting and diagnostics
- Progress indicators for large documents

## Advanced AsciiDoc Features
- Conditional content (`ifdef`, `ifndef`)
- Document fragments and partial includes
- Bibliography and footnote support
- Admonition blocks (NOTE, TIP, WARNING, etc.)
- Source code callouts
- Mathematical expressions

## Quality and Testing
- TCK compatibility test suite
- Performance benchmarking
- Memory usage optimization
- Error recovery mechanisms
- Comprehensive unit test coverage

Would you like me to help you start implementing any of these features, or would you prefer to begin with setting up the basic project structure for the Kotlin Multiplatform module?