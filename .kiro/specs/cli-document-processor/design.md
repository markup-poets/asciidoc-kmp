# Design Document: CLI Document Processor

## Overview

The CLI Document Processor extends the existing AsciiDoc CLI application with document processing capabilities. It adds a new `process` subcommand that leverages the document-processing module to resolve include directives and produce concatenated output documents. The design maintains backward compatibility with the existing `convert` command while providing a clean, intuitive interface for document processing.

The implementation follows a command pattern architecture where each subcommand (convert, process) is implemented as a separate command handler. This allows for easy extension with additional commands in the future while keeping the codebase modular and maintainable.

## Architecture

```
┌─────────────────────────────────────────────┐
│           CLI Application                   │
│                                             │
│  ┌───────────────────────────────────────┐ │
│  │      Command Router                   │ │
│  │  - Parse arguments                    │ │
│  │  - Route to command handler           │ │
│  └───────────────────────────────────────┘ │
│              │                              │
│              ├──────────────┬───────────────┤
│              │              │               │
│  ┌───────────▼────┐  ┌─────▼──────────┐   │
│  │ ConvertCommand │  │ ProcessCommand │   │
│  │ (existing)     │  │ (new)          │   │
│  └────────────────┘  └────────────────┘   │
│                              │              │
│                              ▼              │
│                   ┌──────────────────────┐ │
│                   │ Document Processor   │ │
│                   │ (from module)        │ │
│                   └──────────────────────┘ │
└─────────────────────────────────────────────┘
```

### Key Architectural Principles

1. **Command Pattern**: Each subcommand is a separate handler
2. **Backward Compatibility**: Existing convert functionality unchanged
3. **Separation of Concerns**: CLI logic separate from processing logic
4. **Testability**: Commands can be tested independently
5. **Extensibility**: Easy to add new commands

## Components and Interfaces

### Command Interface

Base interface for all CLI commands:

```kotlin
interface CliCommand {
    val name: String
    val description: String
    
    fun execute(args: CommandArgs): CommandResult
    fun printHelp()
}

data class CommandArgs(
    val positional: List<String>,
    val options: Map<String, String>,
    val flags: Set<String>
)

sealed class CommandResult {
    data class Success(val message: String? = null) : CommandResult()
    data class Error(val message: String, val exitCode: Int = 1) : CommandResult()
}
```

### Process Command

New command for document processing:

```kotlin
class ProcessCommand(
    private val documentProcessor: DocumentProcessor,
    private val fileReader: FileReader
) : CliCommand {
    override val name = "process"
    override val description = "Process AsciiDoc document with include resolution"
    
    override fun execute(args: CommandArgs): CommandResult {
        // Parse arguments
        val inputFile = args.positional.getOrNull(0) ?: return showUsageError()
        val outputFile = args.options["output"] ?: args.options["o"]
        val basePath = args.options["base-path"] ?: args.options["b"]
        val maxDepth = args.options["max-depth"]?.toIntOrNull() ?: 10
        val verbose = args.flags.contains("verbose") || args.flags.contains("v")
        val noOverwrite = args.flags.contains("no-overwrite")
        
        // Validate input file exists
        if (!File(inputFile).exists()) {
            return CommandResult.Error("Input file '$inputFile' not found")
        }
        
        // Check output file overwrite
        if (noOverwrite && outputFile != null && File(outputFile).exists()) {
            return CommandResult.Error("Output file '$outputFile' already exists (use without --no-overwrite to overwrite)")
        }
        
        // Process document
        val result = processDocument(
            inputFile = inputFile,
            outputFile = outputFile,
            basePath = basePath,
            maxDepth = maxDepth,
            verbose = verbose
        )
        
        return result
    }
    
    private fun processDocument(
        inputFile: String,
        outputFile: String?,
        basePath: String?,
        maxDepth: Int,
        verbose: Boolean
    ): CommandResult {
        // Implementation details
    }
}
```

### Convert Command

Refactored existing functionality into command:

```kotlin
class ConvertCommand(
    private val parser: AsciidocParser
) : CliCommand {
    override val name = "convert"
    override val description = "Convert AsciiDoc to Graphviz DOT format"
    
    override fun execute(args: CommandArgs): CommandResult {
        // Existing conversion logic
    }
}
```

### Command Router

Routes commands based on arguments:

```kotlin
class CommandRouter(
    private val commands: Map<String, CliCommand>
) {
    fun route(args: Array<String>): CommandResult {
        // Parse command line arguments
        val parsedArgs = parseArguments(args)
        
        // Determine which command to execute
        val commandName = when {
            parsedArgs.subcommand != null -> parsedArgs.subcommand
            parsedArgs.positional.isNotEmpty() -> "convert" // backward compatibility
            else -> return showHelp()
        }
        
        // Execute command
        val command = commands[commandName] ?: return CommandResult.Error("Unknown command: $commandName")
        return command.execute(parsedArgs.commandArgs)
    }
    
    private fun showHelp(): CommandResult {
        println("AsciiDoc CLI Tool")
        println()
        println("Commands:")
        commands.values.forEach { cmd ->
            println("  ${cmd.name.padEnd(15)} ${cmd.description}")
        }
        println()
        println("Use '<command> --help' for more information about a command")
        return CommandResult.Success()
    }
}
```

### Argument Parser

Parses command-line arguments:

```kotlin
data class ParsedArguments(
    val subcommand: String?,
    val commandArgs: CommandArgs
)

object ArgumentParser {
    fun parse(args: Array<String>): ParsedArguments {
        if (args.isEmpty()) {
            return ParsedArguments(null, CommandArgs(emptyList(), emptyMap(), emptySet()))
        }
        
        // Check if first argument is a subcommand
        val subcommand = if (args[0].startsWith("-")) null else args.getOrNull(0)
        val startIndex = if (subcommand != null) 1 else 0
        
        val positional = mutableListOf<String>()
        val options = mutableMapOf<String, String>()
        val flags = mutableSetOf<String>()
        
        var i = startIndex
        while (i < args.size) {
            val arg = args[i]
            when {
                arg.startsWith("--") -> {
                    val key = arg.substring(2)
                    if (i + 1 < args.size && !args[i + 1].startsWith("-")) {
                        options[key] = args[i + 1]
                        i += 2
                    } else {
                        flags.add(key)
                        i++
                    }
                }
                arg.startsWith("-") -> {
                    val key = arg.substring(1)
                    if (i + 1 < args.size && !args[i + 1].startsWith("-")) {
                        options[key] = args[i + 1]
                        i += 2
                    } else {
                        flags.add(key)
                        i++
                    }
                }
                else -> {
                    positional.add(arg)
                    i++
                }
            }
        }
        
        return ParsedArguments(subcommand, CommandArgs(positional, options, flags))
    }
}
```

## Data Models

### Processing Options

```kotlin
data class ProcessOptions(
    val inputFile: File,
    val outputFile: File?,
    val basePath: String?,
    val maxDepth: Int = 10,
    val verbose: Boolean = false,
    val noOverwrite: Boolean = false
)
```

### Output Writer

Interface for writing output (stdout or file):

```kotlin
interface OutputWriter {
    fun write(content: String)
}

class FileOutputWriter(private val file: File) : OutputWriter {
    override fun write(content: String) {
        file.parentFile?.mkdirs()
        file.writeText(content)
    }
}

class StdoutOutputWriter : OutputWriter {
    override fun write(content: String) {
        println(content)
    }
}
```

## Processing Flow

The process command follows this flow:

1. **Parse Arguments**: Extract input file, output file, and options
2. **Validate Input**: Check input file exists
3. **Check Overwrite**: If no-overwrite flag set, check output doesn't exist
4. **Determine Base Path**: Use specified base path or input file directory
5. **Create Configuration**: Build ProcessingConfig with options
6. **Read Input**: Read input file content
7. **Parse Document**: Parse AsciiDoc content to AST
8. **Process Document**: Run document processor with include resolution
9. **Handle Errors**: Report any processing errors
10. **Generate Output**: Convert processed AST back to AsciiDoc text
11. **Write Output**: Write to file or stdout
12. **Report Success**: Display success message if verbose

### Error Handling Flow

```
Error Occurs
    │
    ├─ File Not Found
    │   └─ Report: "Input file 'path' not found"
    │
    ├─ Include Error
    │   └─ Report: "Include error at line X: file 'path' not found"
    │
    ├─ Max Depth Exceeded
    │   └─ Report: "Max include depth exceeded: chain"
    │
    ├─ Write Error
    │   └─ Report: "Failed to write output file 'path': reason"
    │
    └─ Parse Error
        └─ Report: "Parse error at line X: message"
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Include Resolution Completeness
*For any* AsciiDoc document with include directives, processing should resolve all includes and produce output containing the included content
**Validates: Requirements 1.1, 5.2**

### Property 2: Output File Writing
*For any* valid output file path, the CLI should write the processed document to that location
**Validates: Requirements 1.2**

### Property 3: Input Validation
*For any* non-existent input file path, the CLI should report an error and exit with non-zero status
**Validates: Requirements 1.4**

### Property 4: Exit Code Correctness
*For any* execution, the CLI should exit with status 0 on success and non-zero on failure
**Validates: Requirements 1.5, 3.4**

### Property 5: Base Path Resolution
*For any* specified base path, the CLI should use it for resolving relative include paths; when no base path is specified, it should use the input file's directory
**Validates: Requirements 2.1, 2.2**

### Property 6: Depth Limiting
*For any* specified max depth value, the CLI should limit include nesting to that depth
**Validates: Requirements 2.3**

### Property 7: Depth Violation Reporting
*For any* include chain exceeding the max depth, the CLI should report an error with the include chain
**Validates: Requirements 2.5**

### Property 8: Comprehensive Error Reporting
*For any* processing errors, the CLI should report each error with file path and line number information
**Validates: Requirements 3.1, 6.4, 6.5**

### Property 9: Error Collection
*For any* document with multiple errors, the CLI should report all errors before exiting
**Validates: Requirements 3.2**

### Property 10: Warning Output
*For any* warnings generated during processing, the CLI should display them to stderr
**Validates: Requirements 3.3**

### Property 11: Verbose Output
*For any* execution with verbose mode enabled, the CLI should display detailed processing information
**Validates: Requirements 3.5**

### Property 12: Invalid Argument Handling
*For any* invalid argument combination, the CLI should display an error and usage information
**Validates: Requirements 4.4**

### Property 13: Option Name Equivalence
*For any* option, both short and long forms should produce identical behavior
**Validates: Requirements 4.5**

### Property 14: Output Validity
*For any* processed document, the output should be valid AsciiDoc content
**Validates: Requirements 5.1**

### Property 15: Formatting Preservation
*For any* document with content from multiple files, the CLI should preserve proper formatting and structure
**Validates: Requirements 5.3**

### Property 16: Line Range Filtering
*For any* include with line range specification, the CLI should include only the specified lines
**Validates: Requirements 5.4**

### Property 17: Content Preservation
*For any* document, all non-include content should be preserved exactly as it appears in the source
**Validates: Requirements 5.5**

### Property 18: No-Overwrite Protection
*For any* existing output file when no-overwrite flag is set, the CLI should report an error and exit
**Validates: Requirements 6.2**

### Property 19: Directory Creation
*For any* output path with non-existent directories, the CLI should create them
**Validates: Requirements 6.3**

### Property 20: Subcommand Routing
*For any* explicitly specified subcommand, the CLI should execute that command
**Validates: Requirements 7.3**

### Property 21: Error Handling Consistency
*For any* errors across different commands, the CLI should use consistent error reporting format
**Validates: Requirements 7.4**

## Error Handling

### Error Categories

1. **File Errors**: Input not found, output write failure, include file not found
2. **Processing Errors**: Parse errors, include resolution errors, depth exceeded
3. **Argument Errors**: Invalid arguments, missing required arguments
4. **Configuration Errors**: Invalid max depth, invalid base path

### Error Messages

All error messages follow this format:
```
Error: <description>
  File: <file-path>
  Line: <line-number> (if applicable)
```

Multiple errors are reported together:
```
Processing failed with 3 errors:

Error: Include file not found
  File: includes/missing.adoc
  Line: 5

Error: Max include depth exceeded
  File: includes/deep.adoc
  Line: 10
  Chain: main.adoc -> a.adoc -> b.adoc -> c.adoc -> deep.adoc

Error: Parse error
  File: main.adoc
  Line: 15
  Message: Unexpected token
```

## Testing Strategy

The CLI document processor will be validated using a dual testing approach combining unit tests and property-based tests.

### Property-Based Testing

Property tests will use **Kotest Property Testing** framework with minimum 100 iterations per test. Each property test will be tagged with: **Feature: cli-document-processor, Property {number}: {property_text}**

Property tests will focus on:
- **Include Resolution**: Testing with various document structures and include patterns
- **Path Resolution**: Testing with different base paths and relative paths
- **Error Handling**: Testing various error conditions and verifying reporting
- **Option Parsing**: Testing argument combinations and option equivalence
- **Output Generation**: Testing output validity and content preservation

### Unit Testing

Unit tests will complement property tests by focusing on:
- **Specific Examples**: Known command invocations with expected outputs
- **Edge Cases**: Empty documents, no includes, maximum depth boundaries
- **Integration Points**: Command routing, backward compatibility
- **Error Scenarios**: Specific error messages and exit codes
- **Help and Usage**: Testing help output and usage messages

### Test Data Generation

Property tests will use intelligent generators that:
- Generate valid AsciiDoc documents with various include patterns
- Create nested include structures with controlled depth
- Generate valid and invalid file paths
- Create various argument combinations
- Include edge cases like empty files, circular includes, missing files

### Testing Approach

Tests will be organized into:
1. **Command Tests**: Test individual commands in isolation
2. **Router Tests**: Test command routing and argument parsing
3. **Integration Tests**: Test end-to-end CLI execution
4. **Backward Compatibility Tests**: Ensure existing convert command still works

The testing strategy ensures both successful processing and error conditions are thoroughly validated.
