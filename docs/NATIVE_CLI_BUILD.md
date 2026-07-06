# Native macOS ARM64 CLI Tools

## Overview

This project now includes **true native macOS ARM64 executables** built with Kotlin/Native. These are not JVM-based JAR files - they are compiled directly to native machine code for Apple Silicon.

## Built Executables

### ✅ html-renderer (Native ARM64)

A standalone native binary that converts AsciiDoc files to HTML.

**Location:** `build/native-cli/html-renderer`

**Binary Type:** Mach-O 64-bit executable arm64

**Usage:**
```bash
# Convert AsciiDoc to HTML
build/native-cli/html-renderer document.adoc output.html

# Show help
build/native-cli/html-renderer --help
```

**Features:**
- No JVM required
- Fast startup time
- Native file I/O using POSIX APIs
- Full AsciiDoc parsing and HTML rendering
- Syntax highlighting support for code blocks

## Building Native Executables

### Build Script

Run the build script to create native binaries:

```bash
./build-native-cli.sh
```

This will:
1. Compile Kotlin code to native ARM64 machine code
2. Link all dependencies into a single executable
3. Place the binary in `build/native-cli/`

### Manual Build

You can also build manually using Gradle:

```bash
# Build html-renderer for macOS ARM64
./gradlew :html-cli:linkReleaseExecutableMacosArm64

# The binary will be at:
# html-cli/build/bin/macosArm64/releaseExecutable/html-renderer.kexe
```

## Installation

### System-Wide Installation

```bash
sudo cp build/native-cli/html-renderer /usr/local/bin/
```

### Add to PATH

```bash
export PATH="$PATH:$(pwd)/build/native-cli"
```

Add this to your `~/.zshrc` or `~/.bash_profile` to make it permanent.

## Technical Details

### Architecture

The native CLI tools use Kotlin Multiplatform with the following structure:

- **commonMain**: Shared business logic (parsing, rendering)
- **nativeMain**: Native-specific implementations (file I/O, process exit)
- **jvmMain**: JVM-specific implementations (for development/testing)

### Platform-Specific Code

File operations use `expect/actual` declarations:

```kotlin
// Common declaration
expect fun readFileContent(path: String): String
expect fun writeFileContent(path: String, content: String)
expect fun fileExists(path: String): Boolean
expect fun exitProcess(code: Int): Nothing

// Native implementation (using POSIX)
@OptIn(ExperimentalForeignApi::class)
actual fun readFileContent(path: String): String {
    val file = fopen(path, "r") ?: throw Exception("Cannot open file: $path")
    // ... POSIX file operations
}
```

### Dependencies

All library modules now support macOS targets:
- ✅ asciidoc-parser (macosArm64, macosX64)
- ✅ html-renderer (macosArm64, macosX64)
- ✅ document-processing (macosArm64, macosX64)
- ✅ ast-graphviz-export (macosArm64, macosX64)
- ✅ antora-resolution (macosArm64, macosX64)
- ✅ antora-assembler (macosArm64, macosX64)

## Future Work

### Additional Native CLIs

The following CLIs can be built as native executables with additional work:

1. **asciidoc-assembler**: Requires native file system support in antora-resolution
2. **asciidoc-parser CLI**: General-purpose AsciiDoc parser tool
3. **ast-visualizer**: GraphViz export tool

### Cross-Platform Builds

The build system supports multiple native targets:

- macosArm64 (Apple Silicon)
- macosX64 (Intel Mac)
- linuxX64 (Linux)
- iosArm64 (iOS devices)
- iosX64 (iOS simulator)

To build for other platforms:

```bash
# macOS Intel
./gradlew :html-cli:linkReleaseExecutableMacosX64

# Linux
./gradlew :html-cli:linkReleaseExecutableLinuxX64
```

## Performance

Native executables offer significant advantages:

- **Startup time**: ~10-50ms (vs ~200-500ms for JVM)
- **Memory usage**: Lower baseline memory footprint
- **Distribution**: Single binary, no JVM installation required
- **Size**: ~2-5MB per executable (vs ~50MB+ with bundled JVM)

## Testing

Test the native binary:

```bash
# Create a test file
cat > test.adoc << 'EOF'
= Test Document

This is a *test* document.

[source,kotlin]
----
fun main() {
    println("Hello, Native!")
}
----
EOF

# Convert to HTML
build/native-cli/html-renderer test.adoc test.html

# View the result
open test.html
```

## Troubleshooting

### Binary Not Executable

```bash
chmod +x build/native-cli/html-renderer
```

### File Not Found

Make sure you're running from the project root directory, or use absolute paths.

### Compilation Errors

If you encounter compilation errors, clean and rebuild:

```bash
./gradlew clean
./build-native-cli.sh
```

## Summary

✅ **Native ARM64 executable built successfully**  
✅ **No JVM required**  
✅ **Fast startup and low memory usage**  
✅ **Single binary distribution**  
✅ **Full AsciiDoc → HTML conversion**  
✅ **Code block syntax highlighting support**  
✅ **Parser fix for `[source,language]` attributes**
