#!/bin/bash

# Build native macOS ARM64 CLI executables
# Creates true native binaries using Kotlin/Native

set -e

echo "=========================================="
echo "Building Native macOS ARM64 CLI Tools"
echo "=========================================="
echo ""

# Create output directory
mkdir -p build/native-cli

# Build html-renderer (native ARM64)
echo "Building html-renderer (native ARM64)..."
./gradlew :html-cli:linkReleaseExecutableMacosArm64
cp html-cli/build/bin/macosArm64/releaseExecutable/html-renderer.kexe build/native-cli/html-renderer
chmod +x build/native-cli/html-renderer

echo "✓ html-renderer built (native ARM64)"
echo ""

# TODO: Build asciidoc-assembler (requires native support in antora modules)
# For now, we'll note that it needs additional work
echo "Note: asciidoc-assembler requires native support in antora-resolution module"
echo "      This will be added in a future update"
echo ""

echo "=========================================="
echo "✓ Build Complete!"
echo "=========================================="
echo ""
echo "Native executables created in: build/native-cli/"
echo ""
echo "File info:"
file build/native-cli/html-renderer
echo ""
echo "To install system-wide:"
echo "  sudo cp build/native-cli/html-renderer /usr/local/bin/"
echo ""
echo "Or add to PATH:"
echo "  export PATH=\"\$PATH:$(pwd)/build/native-cli\""
echo ""
echo "Test it:"
echo "  build/native-cli/html-renderer --help"
