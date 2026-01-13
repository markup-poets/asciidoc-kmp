# Design Document: AsciiDoc Core Parser

## Overview

The AsciiDoc Core Parser is designed as a multi-phase, line-based parsing system that transforms AsciiDoc source text into a structured Abstract Syntax Tree (AST). The parser follows a clean separation of concerns with distinct phases for lexical analysis, syntactic parsing, and AST construction.

The design emphasizes platform independence, maintainability, and extensibility while providing the foundation for a TCK-compliant AsciiDoc processor. The parser uses state machines to handle complex multi-line structures and provides comprehensive error reporting.

## Architecture

The parser follows a layered architecture with clear separation between different parsing concerns:

```
┌─────────────────────────────────────┐
│           Parser Facade             │
├─────────────────────────────────────┤
│         Line Processor              │
├─────────────────────────────────────┤
│    Block Parser    │ Inline Parser  │
├─────────────────────────────────────┤
│         State Machine               │
├─────────────────────────────────────┤
│           AST Builder               │
├─────────────────────────────────────┤
│         Document Model              │
└─────────────────────────────────────┘
```

### Key Architectural Principles

1. **Single Responsibility**: Each component has a focused, well-defined purpose
2. **State Management**: Explicit state machines handle complex parsing contexts
3. **Error Isolation**: Errors are captured and reported without stopping the entire process
4. **Platform Neutrality**: No platform-specific dependencies in core parsing logic
5. **Extensibility**: Plugin points for custom block and inline processors

## Components and Interfaces

### Parser Facade

The main entry point that orchestrates the parsing process:

```kotlin
interface AsciidocParser {
    fun parse(source: String): ParseResult
    fun parse(lines: List<String>): ParseResult
}

data class ParseResult(
    val document: Document,
    val errors: List<ParseError>,
    val warnings: List<ParseWarning>
)
```

### Line Processor

Handles line-by-line processing and determines parsing context:

```kotlin
interface LineProcessor {
    fun processLine(line: String, lineNumber: Int, context: ParseContext): LineResult
    fun isBlockDelimiter(line: String): Boolean
    fun determineBlockType(line: String): BlockType
}

data class LineResult(
    val blockType: BlockType,
    val content: String,
    val attributes: Map<String, String> = emptyMap()
)
```

### Block Parser

Processes document-level structural elements:

```kotlin
interface BlockParser {
    fun parseSection(line: String): Section
    fun parseParagraph(lines: List<String>): Paragraph  
    fun parseList(lines: List<String>): List
    fun parseCodeBlock(lines: List<String>): CodeBlock
    fun parseComment(line: String): Comment?
}
```

### Inline Parser

Handles text-level markup within blocks:

```kotlin
interface InlineParser {
    fun parseInlineElements(text: String): List<InlineElement>
    fun parseStrong(text: String, startIndex: Int): ParsedInline?
    fun parseEmphasis(text: String, startIndex: Int): ParsedInline?
    fun parseCode(text: String, startIndex: Int): ParsedInline?
    fun parseLink(text: String, startIndex: Int): ParsedInline?
    fun parseImage(text: String, startIndex: Int): ParsedInline?
}

data class ParsedInline(
    val element: InlineElement,
    val endIndex: Int
)
```

### State Machine

Manages parsing context and state transitions:

```kotlin
interface ParseStateMachine {
    fun getCurrentState(): ParseState
    fun transition(trigger: StateTrigger): StateTransition
    fun canTransition(trigger: StateTrigger): Boolean
    fun reset()
}

enum class ParseState {
    DOCUMENT_START,
    IN_PARAGRAPH,
    IN_LIST,
    IN_CODE_BLOCK,
    IN_SECTION
}

sealed class StateTrigger {
    object EmptyLine : StateTrigger()
    data class BlockDelimiter(val type: String) : StateTrigger()
    data class ListMarker(val type: ListType, val level: Int) : StateTrigger()
    data class SectionHeader(val level: Int) : StateTrigger()
}
```

## Data Models

### Document Model Hierarchy

The AST follows a hierarchical structure with typed nodes:

```kotlin
sealed class AstNode {
    abstract val attributes: Map<String, String>
    abstract val sourceLocation: SourceLocation
}

data class Document(
    val title: String?,
    val children: List<BlockElement>,
    val documentAttributes: Map<String, String>,
    override val attributes: Map<String, String> = emptyMap(),
    override val sourceLocation: SourceLocation
) : AstNode()

sealed class BlockElement : AstNode()

data class Section(
    val level: Int,
    val title: String,
    val children: List<BlockElement>,
    override val attributes: Map<String, String> = emptyMap(),
    override val sourceLocation: SourceLocation
) : BlockElement()

data class Paragraph(
    val content: List<InlineElement>,
    override val attributes: Map<String, String> = emptyMap(),
    override val sourceLocation: SourceLocation
) : BlockElement()

data class List(
    val type: ListType,
    val items: kotlin.collections.List<ListItem>,
    override val attributes: Map<String, String> = emptyMap(),
    override val sourceLocation: SourceLocation
) : BlockElement()

data class ListItem(
    val marker: String,
    val content: List<InlineElement>,
    val nestedList: List? = null,
    override val attributes: Map<String, String> = emptyMap(),
    override val sourceLocation: SourceLocation
) : AstNode()

data class CodeBlock(
    val language: String?,
    val content: String,
    override val attributes: Map<String, String> = emptyMap(),
    override val sourceLocation: SourceLocation
) : BlockElement()

sealed class InlineElement : AstNode()

data class Text(
    val content: String,
    override val attributes: Map<String, String> = emptyMap(),
    override val sourceLocation: SourceLocation
) : InlineElement()

data class Strong(
    val content: List<InlineElement>,
    override val attributes: Map<String, String> = emptyMap(),
    override val sourceLocation: SourceLocation
) : InlineElement()

data class Emphasis(
    val content: List<InlineElement>,
    override val attributes: Map<String, String> = emptyMap(),
    override val sourceLocation: SourceLocation
) : InlineElement()

data class Code(
    val content: String,
    override val attributes: Map<String, String> = emptyMap(),
    override val sourceLocation: SourceLocation
) : InlineElement()

data class Link(
    val url: String,
    val text: String,
    override val attributes: Map<String, String> = emptyMap(),
    override val sourceLocation: SourceLocation
) : InlineElement()

data class Image(
    val path: String,
    val altText: String,
    override val attributes: Map<String, String> = emptyMap(),
    override val sourceLocation: SourceLocation
) : InlineElement()

enum class ListType { UNORDERED, ORDERED, DEFINITION }

data class SourceLocation(
    val line: Int,
    val column: Int = 0
)
```

### Error and Warning Models

```kotlin
data class ParseError(
    val message: String,
    val location: SourceLocation,
    val severity: ErrorSeverity = ErrorSeverity.ERROR
)

data class ParseWarning(
    val message: String,
    val location: SourceLocation
)

enum class ErrorSeverity { WARNING, ERROR, FATAL }
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Line Processing Integrity
*For any* AsciiDoc source text, the parser should process lines sequentially, assign correct block types based on content and state, and preserve line numbers in all AST nodes for error reporting
**Validates: Requirements 1.1, 1.2, 1.5**

### Property 2: AST Structural Integrity  
*For any* parsed AsciiDoc document, the resulting AST should have exactly one Document root node containing all other elements, with all parent-child relationships correctly established and navigable
**Validates: Requirements 2.1, 2.7**

### Property 3: Node Type Assignment Correctness
*For any* AsciiDoc syntax element, the parser should create the appropriate AST node type (Section, Paragraph, List, CodeBlock, etc.) that correctly represents the semantic meaning of the source syntax
**Validates: Requirements 2.2, 2.3, 2.4, 2.5, 2.6**

### Property 4: Block Element Recognition
*For any* block-level AsciiDoc syntax (headers, lists, code blocks, paragraphs, comments), the parser should correctly identify the block type and create the appropriate AST node with proper attributes and content
**Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6**

### Property 5: Inline Element Recognition  
*For any* text containing inline markup patterns (bold, italic, code, links, images), the parser should create the correct inline elements with properly extracted content and attributes
**Validates: Requirements 4.1, 4.2, 4.3, 4.4, 4.5**

### Property 6: Empty Line Block Separation
*For any* AsciiDoc document, empty lines (including whitespace-only lines) should properly separate block elements, ensuring that blocks are not incorrectly merged
**Validates: Requirements 1.3, 1.4**

### Property 7: Inline Markup Precedence and Escaping
*For any* text with nested, overlapping, or escaped inline markup, the parser should handle it according to AsciiDoc precedence rules, with escaped delimiters appearing as literal text
**Validates: Requirements 4.6, 4.7**

### Property 8: State Machine Context Preservation
*For any* multi-line or nested structure (lists, code blocks), the state machine should correctly track context across line boundaries, maintain nesting levels, and preserve literal content where appropriate
**Validates: Requirements 5.1, 5.2, 5.3**

### Property 9: State Transition Validation
*For any* sequence of parsing states, all state transitions should be legal according to AsciiDoc grammar rules, with illegal transitions resulting in clear error messages
**Validates: Requirements 5.4, 5.5**

### Property 10: Attribute Parsing and Storage
*For any* document containing attribute definitions, the parser should correctly extract key-value pairs, preserve values with spaces, store them in the Document node, and use the last value for duplicates
**Validates: Requirements 6.1, 6.2, 6.3, 6.5**

### Property 11: Attribute Reference Marking
*For any* text containing attribute references in "{key}" syntax, the parser should mark them for later substitution without performing the substitution during parsing
**Validates: Requirements 6.4**

### Property 12: Error Handling and Recovery
*For any* document containing syntax errors or warnings, the parser should report them with accurate line numbers and descriptions, attempt recovery when possible, and provide access to all collected errors and warnings
**Validates: Requirements 7.1, 7.2, 7.3, 7.4, 7.5**

### Property 13: Malformed Syntax Error Reporting
*For any* document with malformed block delimiters or other structural errors, the parser should report appropriate errors rather than producing incorrect AST structures
**Validates: Requirements 3.7**

### Property 14: Cross-Platform Consistency
*For any* AsciiDoc document, parsing should produce identical AST results across JVM, JavaScript, and native platforms, using only platform-neutral string operations
**Validates: Requirements 8.3, 8.4**

## Error Handling

The parser implements a multi-layered error handling strategy:

### Error Categories

1. **Lexical Errors**: Invalid characters, encoding issues
2. **Syntactic Errors**: Malformed block delimiters, invalid markup patterns  
3. **Semantic Errors**: Invalid nesting, conflicting attributes
4. **State Errors**: Illegal state transitions, context violations

### Error Recovery Strategies

1. **Skip and Continue**: For non-critical errors, skip the problematic element and continue parsing
2. **Default Substitution**: Replace invalid syntax with sensible defaults where possible
3. **Graceful Degradation**: Convert complex structures to simpler ones when parsing fails
4. **Early Termination**: Stop parsing only for critical structural errors

### Error Reporting

All errors include:
- Precise source location (line and column)
- Clear, actionable error message
- Error severity level (WARNING, ERROR, FATAL)
- Suggested fixes where applicable

## Testing Strategy

The parser will be validated using a dual testing approach combining unit tests and property-based tests to ensure comprehensive coverage and correctness.

### Property-Based Testing

Property-based tests will validate universal correctness properties using **Kotest Property Testing** framework with minimum 100 iterations per test. Each property test will be tagged with the format: **Feature: asciidoc-parser, Property {number}: {property_text}**

Property tests will focus on:
- **Invariant Properties**: AST structure integrity, node type consistency
- **Round-Trip Properties**: Parse → serialize → parse should preserve structure  
- **Metamorphic Properties**: Equivalent inputs should produce equivalent ASTs
- **Error Condition Properties**: Invalid inputs should produce appropriate errors

### Unit Testing

Unit tests will complement property tests by focusing on:
- **Specific Examples**: Known good and bad inputs with expected outputs
- **Edge Cases**: Empty documents, single-line documents, maximum nesting
- **Integration Points**: Component interactions and data flow
- **Error Scenarios**: Specific malformed syntax patterns

### Test Data Generation

Property tests will use intelligent generators that:
- Generate valid AsciiDoc syntax patterns
- Create nested and complex document structures  
- Include edge cases like empty lines, whitespace variations
- Generate both valid and invalid syntax for error testing
- Ensure cross-platform consistency testing

The testing strategy ensures that both the happy path (correct parsing) and error conditions (malformed input) are thoroughly validated across all supported platforms.