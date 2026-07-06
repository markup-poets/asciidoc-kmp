#!/bin/bash
# Simple script to assemble multi-file AsciiDoc articles

set -e

# Check arguments
if [ $# -lt 2 ]; then
    echo "Usage: $0 <index-file> <output-file>"
    echo ""
    echo "Example:"
    echo "  $0 article1/index.adoc article1/assembled.adoc"
    exit 1
fi

INDEX_FILE="$1"
OUTPUT_FILE="$2"

# Check if index file exists
if [ ! -f "$INDEX_FILE" ]; then
    echo "Error: Index file not found: $INDEX_FILE"
    exit 1
fi

# Get absolute paths
INDEX_ABS=$(cd "$(dirname "$INDEX_FILE")" && pwd)/$(basename "$INDEX_FILE")
OUTPUT_ABS=$(cd "$(dirname "$OUTPUT_FILE")" && pwd)/$(basename "$OUTPUT_FILE")

echo "Assembling AsciiDoc document..."
echo "  Index: $INDEX_ABS"
echo "  Output: $OUTPUT_ABS"
echo ""

# Build the assembler if needed
echo "Building assembler..."
./gradlew :antora-assembler:jvmJar --quiet

# Run the assembler
echo ""
echo "Assembling document..."
./gradlew :antora-assembler:jvmRun --args="$INDEX_ABS $OUTPUT_ABS" --quiet

echo ""
echo "Done! Output written to: $OUTPUT_FILE"
