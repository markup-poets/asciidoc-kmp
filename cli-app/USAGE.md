# CLI Usage Guide

## Overview

The AsciiDoc to Graphviz CLI converts AsciiDoc documents into DOT format, allowing you to visualize the document's Abstract Syntax Tree (AST) structure.

## Basic Usage

### Option 1: Wrapper Script (Recommended)

```bash
./asciidoc2dot.sh input.adoc [output.dot]
```

Examples:
```bash
# Convert document.adoc to document.dot
./asciidoc2dot.sh document.adoc

# Convert with custom output name
./asciidoc2dot.sh document.adoc my-graph.dot
```

### Option 2: Direct Gradle Execution

```bash
./gradlew :cli-app:jvmRun --args="input.adoc [output.dot]"
```

Examples:
```bash
# Basic conversion
./gradlew :cli-app:jvmRun --args="document.adoc"

# With custom output
./gradlew :cli-app:jvmRun --args="document.adoc output.dot"
```

## Workflow

1. **Convert AsciiDoc to DOT**
   ```bash
   ./asciidoc2dot.sh document.adoc
   ```

2. **Generate Visualization**
   ```bash
   # PNG image
   dot -Tpng document.dot -o document.png
   
   # SVG (scalable)
   dot -Tsvg document.dot -o document.svg
   
   # PDF
   dot -Tpdf document.dot -o document.pdf
   ```

3. **View the Result**
   - Open the generated image file
   - Or use `xdot document.dot` for interactive viewing (if installed)

## Example

Try the included example:

```bash
# Convert example
./asciidoc2dot.sh cli-app/example.adoc

# Visualize
dot -Tpng cli-app/example.dot -o example.png
open example.png  # macOS
```

## Output Format

The generated DOT file contains:
- **Nodes**: Represent AST elements (Document, Section, Paragraph, etc.)
- **Edges**: Show parent-child relationships
- **Colors**: Different node types have distinct colors
- **Labels**: Show node type and content preview

## Node Color Scheme

- **Blue** - Document root
- **Green** - Sections/headers
- **Yellow** - Paragraphs
- **Coral** - Lists
- **Gray** - Code blocks
- **White** - Text content
- **Gold** - Bold text
- **Lavender** - Italic text
- **Cyan** - Links
- **Steel Blue** - Images

## Troubleshooting

### "Input file not found"
- Ensure the file path is correct
- Use absolute paths if relative paths don't work
- Check current working directory

### "Graphviz not installed"
Install Graphviz:
```bash
# macOS
brew install graphviz

# Ubuntu/Debian
sudo apt-get install graphviz

# Windows
# Download from https://graphviz.org/download/
```

### Parse Errors
The CLI will report parsing errors but still generate output. Check the console output for details about any issues in your AsciiDoc file.

## Testing

Run the CLI tests:
```bash
./gradlew :cli-app:jvmTest
```

## Building

Build the CLI module:
```bash
./gradlew :cli-app:build
```
