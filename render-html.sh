#!/bin/bash

# HTML Renderer Script
# Converts AsciiDoc files to HTML using the built-in Kotlin renderer

set -e

# Check if input file is provided
if [ -z "$1" ]; then
    echo "Usage: $0 <input-file.adoc> [output-file.html]"
    echo ""
    echo "Examples:"
    echo "  $0 document.adoc"
    echo "  $0 document.adoc output.html"
    echo "  $0 article1/assembled.adoc article1/index.html"
    exit 1
fi

INPUT_FILE="$1"
OUTPUT_FILE="${2:-${INPUT_FILE%.adoc}.html}"

# Convert to absolute paths
INPUT_ABS="$(cd "$(dirname "$INPUT_FILE")" && pwd)/$(basename "$INPUT_FILE")"
OUTPUT_ABS="$(cd "$(dirname "$OUTPUT_FILE")" && pwd)/$(basename "$OUTPUT_FILE")"

echo "Converting AsciiDoc to HTML..."
echo "Input:  $INPUT_FILE"
echo "Output: $OUTPUT_FILE"
echo ""

# Run the HTML renderer using absolute paths
./gradlew -q :html-cli:jvmRun --args="$INPUT_ABS $OUTPUT_ABS"

echo ""
echo "Done! Open $OUTPUT_FILE in your browser to view the result."
