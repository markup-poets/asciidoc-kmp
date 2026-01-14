#!/bin/bash
# Simple wrapper script for the AsciiDoc to Graphviz converter

if [ $# -eq 0 ]; then
    echo "Usage: ./asciidoc2dot.sh <input.adoc> [output.dot]"
    echo ""
    echo "Examples:"
    echo "  ./asciidoc2dot.sh document.adoc"
    echo "  ./asciidoc2dot.sh document.adoc graph.dot"
    exit 1
fi

# Get absolute path of input file
INPUT_FILE="$(cd "$(dirname "$1")" && pwd)/$(basename "$1")"

# Determine output file
if [ $# -eq 2 ]; then
    OUTPUT_FILE="$(cd "$(dirname "$2")" && pwd)/$(basename "$2")"
else
    OUTPUT_DIR="$(dirname "$INPUT_FILE")"
    OUTPUT_NAME="$(basename "$INPUT_FILE" .adoc).dot"
    OUTPUT_FILE="$OUTPUT_DIR/$OUTPUT_NAME"
fi

# Build the project if needed
echo "Building CLI app..."
./gradlew :cli-app:build -q

# Run the converter
echo "Converting $(basename "$INPUT_FILE")..."
./gradlew :cli-app:jvmRun --args="$INPUT_FILE $OUTPUT_FILE" -q --console=plain

# Check if output file was created
if [ -f "$OUTPUT_FILE" ]; then
    echo ""
    echo "✓ DOT file created: $OUTPUT_FILE"
    echo ""
    echo "To visualize, run:"
    echo "  dot -Tpng $OUTPUT_FILE -o output.png"
    echo "  dot -Tsvg $OUTPUT_FILE -o output.svg"
fi
