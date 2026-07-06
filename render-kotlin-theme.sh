#!/bin/bash

# Script to render AsciiDoc with Kotlin theme
# Usage: ./render-kotlin-theme.sh <input.adoc> [output.html]

set -e

INPUT_FILE="${1:-publisher.adoc}"
OUTPUT_FILE="${2:-kotlin-theme-demo.html}"

echo "🎨 Rendering with Kotlin Theme..."
echo "Input: $INPUT_FILE"
echo "Output: $OUTPUT_FILE"

# Build the project first
echo "📦 Building project..."
./gradlew :html-cli:build -q

# Run the HTML CLI with Kotlin theme using Gradle
echo "🔄 Converting to HTML..."
./gradlew :html-cli:jvmRun --args="$INPUT_FILE $OUTPUT_FILE --theme kotlin" -q

echo "✅ Done! Output written to: $OUTPUT_FILE"
echo ""
echo "🌐 Open in browser:"
echo "   open $OUTPUT_FILE"
