# AsciiDoc to Graphviz CLI

A simple command-line tool to convert AsciiDoc files into Graphviz DOT format for AST visualization.

## Quick Start

```bash
# Using the wrapper script from project root
./asciidoc2dot.sh document.adoc

# Or with Gradle
./gradlew :cli-app:jvmRun --args="document.adoc"
```

## Usage

### Using Gradle

```bash
./gradlew :cli-app:jvmRun --args="path/to/input.adoc [output.dot]"
```

### Examples

Convert an AsciiDoc file (output will be `document.dot`):
```bash
./gradlew :cli-app:jvmRun --args="document.adoc"
```

Convert with custom output filename:
```bash
./gradlew :cli-app:jvmRun --args="document.adoc graph.dot"
```

Convert from project root:
```bash
./gradlew :cli-app:jvmRun --args="../minimal-asciidoc-converter-kmp.adoc"
```

## Visualizing the Output

Once you have the DOT file, you can visualize it using Graphviz:

```bash
# Generate PNG image
dot -Tpng output.dot -o output.png

# Generate SVG
dot -Tsvg output.dot -o output.svg

# Generate PDF
dot -Tpdf output.dot -o output.pdf

# Interactive view (if you have xdot installed)
xdot output.dot
```

## Installing Graphviz

### macOS
```bash
brew install graphviz
```

### Ubuntu/Debian
```bash
sudo apt-get install graphviz
```

### Windows
Download from: https://graphviz.org/download/

## Features

- Parses AsciiDoc files into an Abstract Syntax Tree (AST)
- Exports AST as Graphviz DOT format
- Color-coded nodes by type (sections, paragraphs, lists, etc.)
- Shows document structure and relationships
- Reports parsing errors and warnings

## Node Types

The visualization uses different colors and shapes for different AST node types:

- **Document** (blue double octagon) - Root document node
- **Section** (green box) - Section headers
- **Paragraph** (yellow box) - Text paragraphs
- **List** (coral folder) - Ordered/unordered lists
- **Code Block** (gray box) - Code blocks
- **Text** (white ellipse) - Plain text content
- **Strong** (gold ellipse) - Bold text
- **Emphasis** (lavender ellipse) - Italic text
- **Link** (cyan ellipse) - Hyperlinks
- **Image** (steel blue ellipse) - Images

## Building a Standalone JAR

To create a distributable JAR:

```bash
./gradlew :cli-app:jvmJar
```

The JAR will be in `cli-app/build/libs/`.
