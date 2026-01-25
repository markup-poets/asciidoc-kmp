#!/bin/bash

# Complete Publishing Workflow
# Assembles an AsciiDoc article from multiple files and converts it to HTML

set -e

# Check if article directory is provided
if [ -z "$1" ]; then
    echo "Usage: $0 <article-directory>"
    echo ""
    echo "This script will:"
    echo "  1. Assemble all AsciiDoc files in the directory"
    echo "  2. Convert the assembled document to HTML"
    echo ""
    echo "Example:"
    echo "  $0 article1"
    echo ""
    echo "The article directory should contain:"
    echo "  - index.adoc (main file with include directives)"
    echo "  - chapter*.adoc (chapter files)"
    echo "  - images/ (optional image directory)"
    exit 1
fi

ARTICLE_DIR="$1"
INDEX_FILE="$ARTICLE_DIR/index.adoc"
ASSEMBLED_FILE="$ARTICLE_DIR/assembled.adoc"
OUTPUT_FILE="$ARTICLE_DIR/index.html"

# Check if article directory exists
if [ ! -d "$ARTICLE_DIR" ]; then
    echo "Error: Article directory not found: $ARTICLE_DIR"
    exit 1
fi

# Check if index file exists
if [ ! -f "$INDEX_FILE" ]; then
    echo "Error: Index file not found: $INDEX_FILE"
    echo "Expected: $INDEX_FILE"
    exit 1
fi

echo "=========================================="
echo "Publishing Article to HTML"
echo "=========================================="
echo ""
echo "Article: $ARTICLE_DIR"
echo ""

# Step 1: Assemble the document
echo "Step 1: Assembling document..."
echo "------------------------------------------"
./assemble-article.sh "$INDEX_FILE" "$ASSEMBLED_FILE"

if [ ! -f "$ASSEMBLED_FILE" ]; then
    echo "Error: Assembly failed - output file not created"
    exit 1
fi

echo ""
echo "✓ Assembly complete: $ASSEMBLED_FILE"
echo ""

# Step 2: Convert to HTML
echo "Step 2: Converting to HTML..."
echo "------------------------------------------"

# Convert to absolute paths
ASSEMBLED_ABS="$(cd "$(dirname "$ASSEMBLED_FILE")" && pwd)/$(basename "$ASSEMBLED_FILE")"
OUTPUT_ABS="$(cd "$(dirname "$OUTPUT_FILE")" && pwd)/$(basename "$OUTPUT_FILE")"

./gradlew -q :html-cli:jvmRun --args="$ASSEMBLED_ABS $OUTPUT_ABS"

if [ ! -f "$OUTPUT_FILE" ]; then
    echo "Error: HTML conversion failed - output file not created"
    exit 1
fi

echo ""
echo "=========================================="
echo "✓ Publishing Complete!"
echo "=========================================="
echo ""
echo "Generated files:"
echo "  - Assembled: $ASSEMBLED_FILE"
echo "  - HTML:      $OUTPUT_FILE"
echo ""
echo "Open $OUTPUT_FILE in your browser to view the result."
