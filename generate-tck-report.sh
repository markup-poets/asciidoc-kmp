#!/bin/bash

# Script to generate TCK Conformance Report and publish to HTML
# Used by GitHub Actions

set -e

echo "========================================================================"
echo "🚀 Generating TCK Conformance Report"
echo "========================================================================"

# Step 1: Run TCK and generate AsciiDoc report
echo "Step 1: Running TCK tests and generating AsciiDoc report..."
./gradlew :tck-quality-testing:jvmTest --tests "OfficialTckTest.should generate official conformance report in asciidoc" --console=plain

ADOC_REPORT="tck-quality-testing/build/tck-report/tck-conformance-report.adoc"

if [ ! -f "$ADOC_REPORT" ]; then
    echo "❌ Error: AsciiDoc report not found at $ADOC_REPORT"
    exit 1
fi

echo "✅ AsciiDoc report generated: $ADOC_REPORT"

# Step 2: Convert AsciiDoc report to HTML
echo ""
echo "Step 2: Converting report to HTML..."

OUTPUT_DIR="build/docs/tck"
mkdir -p "$OUTPUT_DIR"
HTML_REPORT="$OUTPUT_DIR/index.html"

# We use the project's own HTML converter
# Convert to absolute paths for gradle
ADOC_ABS="$(pwd)/$ADOC_REPORT"
HTML_ABS="$(pwd)/$HTML_REPORT"

# Get relative paths from html-cli directory
ADOC_REL=$(realpath --relative-to="html-cli" "$ADOC_ABS")
HTML_REL=$(realpath --relative-to="html-cli" "$HTML_ABS")

echo "Running converter with kotlin theme: :html-cli:jvmRun --args=\"$ADOC_REL $HTML_REL --theme kotlin\""
./gradlew :html-cli:jvmRun --args="$ADOC_REL $HTML_REL --theme kotlin" -q

if [ ! -f "$HTML_REPORT" ]; then
    echo "❌ Error: HTML conversion failed - output file not created"
    exit 1
fi

echo "✅ HTML report generated: $HTML_REPORT"

# Step 3: Prepare for publishing (copy assets if any)
# If there are images or CSS needed, they should be copied to $OUTPUT_DIR
# For now, it's a self-contained HTML (or should be)

echo ""
echo "========================================================================"
echo "🎉 TCK Report Generation Complete!"
echo "Report is available at: $HTML_REPORT"
echo "========================================================================"
