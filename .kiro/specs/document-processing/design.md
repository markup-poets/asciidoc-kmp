# Design Document: Document Processing

## Overview

The Document Processing module extends the AsciiDoc parser by providing post-parse transformation and enhancement capabilities. It operates on the parsed AST to resolve includes, substitute attributes, resolve cross-references, generate tables of contents, validate document structure, and expand macros. This module bridges the gap between raw parsing and final document conversion.

The design follows a pipeline architecture where multiple processors can be chained together, each performing a specific transformation on the AST. The module maintains platform independence and provides comprehensive error reporting with source location tracking.

## Architecture

The document processing system follows a pipeline architecture with configurable processors:

```
┌──────────────────────────────────────────┐
│         Document Processor               │
│                                          │
│  ┌────────────────────────────────────┐ │
│  │   Processing Pipeline              │ │
│  │                                    │ │
│  │  1. Include Resolver               │ │
│  │  2. Attribute Substitutor          │ │
│  │  3. Cross-Reference Resolver       │ │
│  │  4. TOC Generator                  │ │
│  │  5. Document Validator             │ │
│  │  6. Macro Expander                 │ │
│  └────────────────────────────────────┘ │
│                                          │
│  ┌────────────────────────────────────┐ │
│  │   Configuration & Error Handling   │ │
│  └────────────────────────────────────┘ │
└──────────────────────────────────────────┘
         ↓                    ↑
    Document AST         Processed AST
```

### Key Architectural Principles

1. **Pipeline Pattern**: Each processor is independent and composable
2. **Immutability**: Processors return new AST instances rather than mutating
3. **Error Accumulation**: Errors are collected throughout the pipeline
4. **Configuration-Driven**: Processing behavior is controlled by configuration
5. **Platform Neutrality**: No platform-specific file I/O in core logic

## Components and Interfaces

### Document Processor Facade

The main entry point that orchestrates the processing pipeline:

```kotlin
interface DocumentProcessor {
    fun process(document: Document, config: ProcessingConfig): ProcessingResult
}

data class ProcessingResult(
    val document: Document,
    val errors: List<ProcessingError>,
    val warnings: List<ProcessingWarning>
)

data class ProcessingConfig(
    val enableIncludes: Boolean = true,
    val maxIncludeDepth: Int = 10,
    val enableAttributeSubstitution: Boolean = true,
    val attributeDefaults: Map<String, String> = emptyMap(),
    val enableCrossReferences: Boolean = true,
    val enableTocGeneration: Boolean = false,
    val tocDepth: Int = 3,
    val validationStrictness: ValidationLevel = ValidationLevel.NORMAL,
    val enableMacroExpansion: Boolean = true,
    val customMacros: Map<String, MacroProcessor> = emptyMap()
)

enum class ValidationLevel { PERMISSIVE, NORMAL, STRICT }
```

### Include Resolver

Resolves include directives and embeds external content:

```kotlin
interface IncludeResolver {
    fun resolve(document: Document, config: IncludeConfig): IncludeResult
}

data class IncludeConfig(
    val maxDepth: Int = 10,
    val basePath: String = "",
    val fileReader: FileReader
)

interface FileReader {
    fun readFile(path: String): FileReadResult
}

sealed class FileReadResult {
    data class Success(val content: String) : FileReadResult()
    data class Error(val message: String) : FileReadResult()
}

data class IncludeResult(
    val document: Document,
    val errors: List<ProcessingError>,
    val includedFiles: Set<String>
)
```

The include resolver will:
- Traverse the AST looking for include directives (represented as special block elements)
- Resolve file paths relative to the including document
- Parse included content and insert it into the AST
- Track include depth to prevent infinite recursion
- Detect circular dependencies using a visited file set

### Attribute Substitutor

Performs attribute reference substitution throughout the document:

```kotlin
interface AttributeSubstitutor {
    fun substitute(document: Document, config: AttributeConfig): SubstitutionResult
}

data class AttributeConfig(
    val defaults: Map<String, String> = emptyMap(),
    val maxRecursionDepth: Int = 10,
    val undefinedBehavior: UndefinedAttributeBehavior = UndefinedAttributeBehavior.PRESERVE
)

enum class UndefinedAttributeBehavior {
    PRESERVE,  // Keep {attr} as-is
    REMOVE,    // Remove {attr} entirely
    DEFAULT    // Use default value if provided
}

data class SubstitutionResult(
    val document: Document,
    val errors: List<ProcessingError>,
    val substitutedAttributes: Set<String>
)
```

The attribute substitutor will:
- Traverse all text content in the AST
- Find attribute references in `{key}` format
- Replace them with defined values from document attributes or defaults
- Handle recursive substitution (attributes referencing other attributes)
- Detect circular references and report errors

### Cross-Reference Resolver

Resolves internal document cross-references:

```kotlin
interface CrossReferenceResolver {
    fun resolve(document: Document): CrossReferenceResult
}

data class CrossReferenceResult(
    val document: Document,
    val errors: List<ProcessingError>,
    val warnings: List<ProcessingWarning>,
    val resolvedReferences: Map<String, AnchorTarget>
)

data class AnchorTarget(
    val anchorId: String,
    val targetNode: AstNode,
    val generatedText: String
)
```

The cross-reference resolver will:
- Build an index of all anchors in the document
- Find all cross-reference elements in the AST
- Match references to their targets
- Generate appropriate link text based on target type
- Report warnings for unresolved references
- Report errors for duplicate anchor IDs

### Table of Contents Generator

Generates a hierarchical table of contents:

```kotlin
interface TocGenerator {
    fun generate(document: Document, config: TocConfig): TocResult
}

data class TocConfig(
    val maxDepth: Int = 3,
    val includeTitle: Boolean = true
)

data class TocResult(
    val tocNode: AsciiDocList?,
    val errors: List<ProcessingError>
)
```

The TOC generator will:
- Traverse the document to find all sections
- Build a hierarchical list structure representing the TOC
- Respect the configured depth limit
- Create cross-references to each section
- Return null if no sections are found

### Document Validator

Validates document structure and reports issues:

```kotlin
interface DocumentValidator {
    fun validate(document: Document, config: ValidationConfig): ValidationResult
}

data class ValidationConfig(
    val strictness: ValidationLevel = ValidationLevel.NORMAL,
    val checkSectionHierarchy: Boolean = true,
    val checkDuplicateAnchors: Boolean = true,
    val checkInvalidReferences: Boolean = true
)

data class ValidationResult(
    val errors: List<ProcessingError>,
    val warnings: List<ProcessingWarning>,
    val isValid: Boolean
)
```

The validator will check:
- Section level hierarchy (no skipped levels like 1 → 3)
- Duplicate anchor IDs
- Invalid attribute references
- Structural consistency
- Whitespace normalization

### Macro Expander

Expands macros into AST content:

```kotlin
interface MacroExpander {
    fun expand(document: Document, config: MacroConfig): MacroResult
}

data class MacroConfig(
    val customMacros: Map<String, MacroProcessor> = emptyMap(),
    val enableBuiltinMacros: Boolean = true
)

interface MacroProcessor {
    fun process(macroName: String, parameters: Map<String, String>, context: MacroContext): MacroExpansionResult
}

data class MacroContext(
    val document: Document,
    val sourceLocation: SourceLocation
)

sealed class MacroExpansionResult {
    data class Success(val nodes: List<AstNode>) : MacroExpansionResult()
    data class Error(val message: String) : MacroExpansionResult()
}

data class MacroResult(
    val document: Document,
    val errors: List<ProcessingError>
)
```

The macro expander will:
- Find macro invocations in the AST
- Parse macro parameters
- Invoke the appropriate macro processor
- Insert generated nodes into the AST
- Validate generated content

## Data Models

### Processing Errors and Warnings

```kotlin
data class ProcessingError(
    val message: String,
    val location: SourceLocation,
    val errorType: ProcessingErrorType,
    val severity: ErrorSeverity = ErrorSeverity.ERROR
)

enum class ProcessingErrorType {
    INCLUDE_NOT_FOUND,
    INCLUDE_CIRCULAR_DEPENDENCY,
    INCLUDE_MAX_DEPTH_EXCEEDED,
    ATTRIBUTE_CIRCULAR_REFERENCE,
    ATTRIBUTE_UNDEFINED,
    CROSS_REFERENCE_UNRESOLVED,
    CROSS_REFERENCE_DUPLICATE_ANCHOR,
    VALIDATION_SECTION_HIERARCHY,
    VALIDATION_DUPLICATE_ANCHOR,
    MACRO_EXPANSION_FAILED,
    MACRO_INVALID_PARAMETERS,
    CONFIGURATION_INVALID
}

data class ProcessingWarning(
    val message: String,
    val location: SourceLocation,
    val warningType: ProcessingWarningType
)

enum class ProcessingWarningType {
    ATTRIBUTE_UNDEFINED,
    CROSS_REFERENCE_UNRESOLVED,
    SECTION_HIERARCHY_VIOLATION,
    WHITESPACE_NORMALIZATION
}

enum class ErrorSeverity { WARNING, ERROR, FATAL }
```

### Include Directive Representation

Include directives will be represented as special block elements in the AST:

```kotlin
data class IncludeDirective(
    val path: String,
    val lineRange: IntRange? = null,
    override val attributes: Map<String, String> = emptyMap(),
    override val sourceLocation: SourceLocation
) : BlockElement()
```

### Attribute Reference Representation

Attribute references will be represented as inline elements:

```kotlin
data class AttributeReference(
    val key: String,
    override val attributes: Map<String, String> = emptyMap(),
    override val sourceLocation: SourceLocation
) : InlineElement()
```

### Cross-Reference Representation

Cross-references will be represented as inline elements:

```kotlin
data class CrossReference(
    val targetId: String,
    val customText: String? = null,
    override val attributes: Map<String, String> = emptyMap(),
    override val sourceLocation: SourceLocation
) : InlineElement()
```

### Macro Invocation Representation

Macros will be represented as block or inline elements:

```kotlin
data class MacroInvocation(
    val macroName: String,
    val parameters: Map<String, String>,
    val isBlock: Boolean,
    override val attributes: Map<String, String> = emptyMap(),
    override val sourceLocation: SourceLocation
) : AstNode()
```

## Processing Pipeline Implementation

The default processing pipeline executes processors in this order:

1. **Include Resolver**: Embeds external content first so subsequent processors see the complete document
2. **Attribute Substitutor**: Resolves attribute references so other processors see final values
3. **Macro Expander**: Expands macros which may generate content needing further processing
4. **Cross-Reference Resolver**: Resolves references after all content is present
5. **TOC Generator**: Generates TOC after all sections are finalized
6. **Document Validator**: Validates the final document structure

Each processor:
- Receives the current document state
- Returns a new document with transformations applied
- Accumulates errors and warnings
- Can be skipped based on configuration

### Pipeline Execution

```kotlin
class DefaultDocumentProcessor : DocumentProcessor {
    override fun process(document: Document, config: ProcessingConfig): ProcessingResult {
        val errors = mutableListOf<ProcessingError>()
        val warnings = mutableListOf<ProcessingWarning>()
        var currentDoc = document
        
        // 1. Include resolution
        if (config.enableIncludes) {
            val includeResult = includeResolver.resolve(currentDoc, includeConfig)
            currentDoc = includeResult.document
            errors.addAll(includeResult.errors)
        }
        
        // 2. Attribute substitution
        if (config.enableAttributeSubstitution) {
            val subResult = attributeSubstitutor.substitute(currentDoc, attributeConfig)
            currentDoc = subResult.document
            errors.addAll(subResult.errors)
        }
        
        // 3. Macro expansion
        if (config.enableMacroExpansion) {
            val macroResult = macroExpander.expand(currentDoc, macroConfig)
            currentDoc = macroResult.document
            errors.addAll(macroResult.errors)
        }
        
        // 4. Cross-reference resolution
        if (config.enableCrossReferences) {
            val xrefResult = crossReferenceResolver.resolve(currentDoc)
            currentDoc = xrefResult.document
            errors.addAll(xrefResult.errors)
            warnings.addAll(xrefResult.warnings)
        }
        
        // 5. TOC generation
        if (config.enableTocGeneration) {
            val tocResult = tocGenerator.generate(currentDoc, tocConfig)
            if (tocResult.tocNode != null) {
                currentDoc = insertToc(currentDoc, tocResult.tocNode)
            }
            errors.addAll(tocResult.errors)
        }
        
        // 6. Validation
        val validationResult = validator.validate(currentDoc, validationConfig)
        errors.addAll(validationResult.errors)
        warnings.addAll(validationResult.warnings)
        
        return ProcessingResult(currentDoc, errors, warnings)
    }
}
```

## Error Handling

The document processing module implements comprehensive error handling:

### Error Categories

1. **Include Errors**: File not found, circular dependencies, depth exceeded
2. **Attribute Errors**: Circular references, undefined attributes
3. **Cross-Reference Errors**: Unresolved references, duplicate anchors
4. **Validation Errors**: Section hierarchy violations, structural issues
5. **Macro Errors**: Expansion failures, invalid parameters
6. **Configuration Errors**: Invalid settings

### Error Recovery Strategies

1. **Continue Processing**: Most errors don't stop the pipeline
2. **Partial Results**: Return partially processed documents with error reports
3. **Graceful Degradation**: Skip problematic elements when possible
4. **Detailed Reporting**: Include source locations and context for all errors

### Error Reporting

All errors include:
- Precise source location (line and column)
- Error type classification
- Clear, actionable error message
- Severity level (WARNING, ERROR, FATAL)
- Context information where applicable


## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Include Directive Resolution
*For any* include directive with a valid file path, the Document_Processor should resolve the path and embed the referenced content into the AST at the directive's location
**Validates: Requirements 1.1**

### Property 2: Relative Path Resolution
*For any* include directive with a relative path, the Document_Processor should resolve it relative to the including document's location, producing the correct absolute path
**Validates: Requirements 1.2**

### Property 3: Include Error Reporting
*For any* include directive referencing a non-existent file, the Document_Processor should report an error containing the file path and source location
**Validates: Requirements 1.3**

### Property 4: Line Range Inclusion
*For any* include directive with line range attributes, the Document_Processor should include only the specified lines from the target file, excluding all other lines
**Validates: Requirements 1.4**

### Property 5: Nested Include Resolution
*For any* nested include structure, the Document_Processor should resolve includes recursively up to the configured depth limit, correctly embedding all content within the limit
**Validates: Requirements 1.5**

### Property 6: Circular Include Detection
*For any* circular include dependency graph, the Document_Processor should detect the cycle and report an error without entering infinite recursion
**Validates: Requirements 1.6**

### Property 7: Attribute Substitution
*For any* attribute reference in the document, the Document_Processor should substitute it with the attribute's defined value from the document attributes or configuration defaults
**Validates: Requirements 2.1**

### Property 8: Undefined Attribute Handling
*For any* undefined attribute reference, the Document_Processor should handle it according to configuration (preserve, remove, or use default), consistently applying the configured behavior
**Validates: Requirements 2.2**

### Property 9: Recursive Attribute Resolution
*For any* attribute value containing other attribute references, the Document_Processor should resolve them recursively until all references are replaced with final values
**Validates: Requirements 2.3**

### Property 10: Header Attribute Scope
*For any* attribute defined in the document header, the Document_Processor should make it available for substitution throughout the entire document, from beginning to end
**Validates: Requirements 2.4**

### Property 11: Inline Attribute Scope
*For any* attribute defined inline at a specific location, the Document_Processor should apply it from that point forward in the document, but not before the definition
**Validates: Requirements 2.5**

### Property 12: Circular Attribute Detection
*For any* circular attribute reference chain, the Document_Processor should detect the cycle and report an error without entering infinite recursion
**Validates: Requirements 2.6**

### Property 13: Cross-Reference Resolution
*For any* cross-reference with a valid target anchor ID, the Document_Processor should resolve it to the target element and create a proper link
**Validates: Requirements 3.1**

### Property 14: Unresolved Reference Warning
*For any* cross-reference with a non-existent target anchor ID, the Document_Processor should report a warning containing the unresolved reference ID
**Validates: Requirements 3.2**

### Property 15: Link Text Generation
*For any* resolved cross-reference, the Document_Processor should generate appropriate link text based on the target element's type (section title, list item, etc.)
**Validates: Requirements 3.3**

### Property 16: Duplicate Anchor Detection
*For any* document containing multiple elements with the same anchor ID, the Document_Processor should report an error identifying all duplicate occurrences with their locations
**Validates: Requirements 3.4, 5.3**

### Property 17: Custom Link Text Preservation
*For any* cross-reference with custom link text specified, the Document_Processor should use the provided text instead of generating text from the target element
**Validates: Requirements 3.5**

### Property 18: TOC Hierarchical Structure
*For any* document with sections, the Document_Processor should generate a table of contents as a hierarchical list that correctly represents the section structure
**Validates: Requirements 4.1**

### Property 19: TOC Depth Limiting
*For any* configured TOC depth limit, the Document_Processor should include only sections up to that depth level, excluding deeper sections from the table of contents
**Validates: Requirements 4.2**

### Property 20: Untitled Section Exclusion
*For any* section without a title, the Document_Processor should exclude it from the generated table of contents
**Validates: Requirements 4.3**

### Property 21: TOC Cross-Reference Creation
*For any* section included in the table of contents, the Document_Processor should create a valid cross-reference linking to that section
**Validates: Requirements 4.4**

### Property 22: Section Hierarchy Validation
*For any* document, the Document_Processor should validate that section levels follow proper hierarchy (no skipped levels like 1 → 3), reporting violations as warnings
**Validates: Requirements 5.1, 5.2**

### Property 23: Whitespace Normalization
*For any* document, the Document_Processor should normalize whitespace according to AsciiDoc conventions, ensuring consistent formatting
**Validates: Requirements 5.4**

### Property 24: Invalid Attribute Reference Collection
*For any* document with invalid attribute references, the Document_Processor should collect all issues and report them together in a single validation report
**Validates: Requirements 5.5**

### Property 25: Macro Expansion
*For any* macro invocation, the Document_Processor should expand it according to the macro's definition, replacing the invocation with the generated content
**Validates: Requirements 6.1**

### Property 26: Macro Parameter Parsing
*For any* macro with parameters, the Document_Processor should parse the parameters and pass them correctly to the macro processor
**Validates: Requirements 6.2**

### Property 27: Macro Expansion Error Reporting
*For any* macro expansion that fails, the Document_Processor should report an error containing the macro name and source location
**Validates: Requirements 6.3**

### Property 28: Custom Macro Registration
*For any* custom macro registered in the configuration, the Document_Processor should make it available for invocation in documents
**Validates: Requirements 6.4**

### Property 29: Macro AST Integration
*For any* macro that generates AST nodes, the Document_Processor should integrate them into the document tree at the macro's location, maintaining proper parent-child relationships
**Validates: Requirements 6.5**

### Property 30: Macro Output Validation
*For any* macro expansion that creates invalid content, the Document_Processor should validate the result and report errors
**Validates: Requirements 6.6**

### Property 31: Invalid Configuration Detection
*For any* invalid configuration provided to the processor, the Document_Processor should report configuration errors before beginning document processing
**Validates: Requirements 7.6**

### Property 32: Comprehensive Error Reporting
*For any* processing errors or warnings, the Document_Processor should report them with complete location information (file path, line number, column number), distinguish between errors and warnings, collect all issues, and provide a summary upon completion
**Validates: Requirements 8.1, 8.2, 8.3, 8.4**

### Property 33: Graceful Error Handling
*For any* fatal error that prevents further processing, the Document_Processor should report the error with full details and halt gracefully without crashing
**Validates: Requirements 8.5**

## Testing Strategy

The document processing module will be validated using a dual testing approach combining unit tests and property-based tests to ensure comprehensive coverage and correctness.

### Property-Based Testing

Property-based tests will validate universal correctness properties using **Kotest Property Testing** framework with minimum 100 iterations per test. Each property test will be tagged with the format: **Feature: document-processing, Property {number}: {property_text}**

Property tests will focus on:
- **Include Resolution**: Testing file path resolution, nesting, circular dependency detection
- **Attribute Substitution**: Testing substitution logic, recursion, circular reference detection
- **Cross-Reference Resolution**: Testing reference resolution, link text generation, duplicate detection
- **TOC Generation**: Testing hierarchical structure, depth limiting, cross-reference creation
- **Validation**: Testing section hierarchy, whitespace normalization, error collection
- **Macro Expansion**: Testing expansion logic, parameter parsing, AST integration
- **Error Reporting**: Testing error collection, location tracking, severity classification

### Unit Testing

Unit tests will complement property tests by focusing on:
- **Specific Examples**: Known document patterns with expected processing results
- **Edge Cases**: Empty documents, documents with no sections, maximum nesting depths
- **Integration Points**: Pipeline execution, processor chaining, configuration handling
- **Error Scenarios**: Specific malformed patterns and their expected error messages
- **Configuration Examples**: Testing different configuration combinations (7.1-7.5)

### Test Data Generation

Property tests will use intelligent generators that:
- Generate valid AST structures with various node types
- Create nested include structures with controlled depth
- Generate attribute definitions and references with various patterns
- Create cross-reference networks with valid and invalid targets
- Generate section hierarchies with valid and invalid nesting
- Include edge cases like empty content, maximum recursion, circular dependencies
- Ensure platform-neutral test execution

### Mocking Strategy

For platform-specific operations (file I/O), tests will use:
- **FileReader interface mocking**: Provide controlled file content for testing
- **In-memory file systems**: Simulate file structures without actual I/O
- **Deterministic generators**: Ensure reproducible test results

The testing strategy ensures that both successful processing (correct transformations) and error conditions (malformed input, circular dependencies) are thoroughly validated across all supported platforms.
