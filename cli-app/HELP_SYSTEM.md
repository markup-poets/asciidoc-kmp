# Help System Implementation

## Overview

The CLI Document Processor includes a comprehensive help system that provides users with usage information and command documentation.

## Features

### 1. Usage Information (Requirement 4.2)

When the user runs the application without arguments, the help system displays:
- Tool name and description
- Usage syntax
- List of available commands with descriptions
- Options documentation
- Guidance on getting command-specific help

**Example:**
```bash
asciidoc-tool
```

**Output:**
```
AsciiDoc CLI Tool

Usage:
  asciidoc-tool <command> [arguments]
  asciidoc-tool <input.adoc> [output.dot]  (defaults to 'convert' for backward compatibility)

Commands:
  convert         Convert AsciiDoc to Graphviz DOT format
  process         Process AsciiDoc document with include resolution

Options:
  -h, --help     Show this help message

Use '<command> --help' for more information about a specific command
```

### 2. Detailed Help with --help Flag (Requirement 4.3)

The help system supports both long (`--help`) and short (`-h`) forms of the help flag:

**Example:**
```bash
asciidoc-tool --help
asciidoc-tool -h
```

Both commands display the same comprehensive help information.

### 3. Unified Command List (Requirement 7.5)

The help message displays all available commands in a unified, sorted list:
- Commands are sorted alphabetically for consistency
- Each command shows its name and description
- The format is consistent across all commands

### 4. Command-Specific Help

Users can get detailed help for individual commands:

**Example:**
```bash
asciidoc-tool convert --help
```

This calls the command's `printHelp()` method, which displays:
- Command description
- Usage syntax
- Arguments and options
- Examples

## Implementation

The help system is implemented in the `CommandRouter` class:

### Key Methods

- `showHelp()`: Displays help and returns success result
- `getHelpText()`: Generates formatted help text
- Command routing logic checks for help flags before executing commands

### Help Display Logic

1. **No arguments**: Show help
2. **--help or -h at top level**: Show help
3. **command --help**: Show command-specific help
4. **Unknown command**: Show error with help text

## Testing

The help system is thoroughly tested in:
- `CommandRouterTest.kt`: Tests routing and help display logic
- `HelpSystemTest.kt`: Comprehensive tests for all help requirements

All tests validate:
- ✅ Help display when no arguments provided
- ✅ Help display with --help flag
- ✅ Help display with -h flag
- ✅ Command-specific help
- ✅ Unified command list
- ✅ Sorted command display
- ✅ Both short and long option forms documented

## Requirements Coverage

| Requirement | Description | Status |
|-------------|-------------|--------|
| 4.2 | Display usage when no arguments provided | ✅ Implemented |
| 4.3 | Display detailed help with --help flag | ✅ Implemented |
| 7.5 | Display unified help message showing all commands | ✅ Implemented |

## Usage Examples

### Get general help
```bash
asciidoc-tool
asciidoc-tool --help
asciidoc-tool -h
```

### Get command-specific help
```bash
asciidoc-tool convert --help
asciidoc-tool process --help
```

### Use commands
```bash
asciidoc-tool convert input.adoc output.dot
asciidoc-tool process input.adoc output.adoc
```

## Future Enhancements

Potential improvements for the help system:
- Add color coding for better readability
- Include more detailed examples in help text
- Add man page generation
- Support for help topics (e.g., `asciidoc-tool help includes`)
