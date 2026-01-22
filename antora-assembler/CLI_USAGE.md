# Antora Document Assembler CLI

A command-line tool for assembling multiple AsciiDoc files from an Antora directory structure into a single consolidated document.

## Building

Build the JAR file:

```bash
./gradlew :antora-assembler:jvmJar
```

The JAR will be created at `antora-assembler/build/libs/antora-assembler-jvm-1.0.0.jar`

## Usage

Run the CLI using the JAR:

```bash
java -cp antora-assembler/build/libs/antora-assembler-jvm-1.0.0.jar \
  org.markup.poet.antora.assembler.cli.MainKt \
  <index-file> <output-file> [options]
```

### Arguments

- `index-file` - Path to the index AsciiDoc file (entry point)
- `output-file` - Path to the output consolidated file

### Options

- `--component-root <path>` - Path to the Antora component root directory (default: current directory)
- `--max-depth <n>` - Maximum include recursion depth (default: 50)
- `--no-preserve-comments` - Do not preserve comments in the output
- `--allow-missing` - Continue processing when includes are missing (default: fail on missing includes)
- `--allow-circular` - Continue processing when circular dependencies are detected (default: fail on circular deps)
- `-h, --help` - Show help message

## Examples

### Basic Assembly

```bash
java -cp antora-assembler/build/libs/antora-assembler-jvm-1.0.0.jar \
  org.markup.poet.antora.assembler.cli.MainKt \
  docs/modules/ROOT/pages/index.adoc output.adoc
```

### Specify Component Root

```bash
java -cp antora-assembler/build/libs/antora-assembler-jvm-1.0.0.jar \
  org.markup.poet.antora.assembler.cli.MainKt \
  docs/modules/ROOT/pages/index.adoc output.adoc \
  --component-root docs
```

### Allow Missing Includes

```bash
java -cp antora-assembler/build/libs/antora-assembler-jvm-1.0.0.jar \
  org.markup.poet.antora.assembler.cli.MainKt \
  index.adoc output.adoc \
  --allow-missing
```

### Set Maximum Depth

```bash
java -cp antora-assembler/build/libs/antora-assembler-jvm-1.0.0.jar \
  org.markup.poet.antora.assembler.cli.MainKt \
  index.adoc output.adoc \
  --max-depth 100
```

## Exit Codes

- `0` - Success
- `1` - Assembly failed (errors occurred)

## Antora Directory Structure

The assembler expects an Antora directory structure:

```
docs/
└── modules/
    ├── ROOT/
    │   ├── pages/
    │   │   ├── index.adoc
    │   │   └── getting-started.adoc
    │   ├── partials/
    │   │   └── common-intro.adoc
    │   ├── examples/
    │   │   └── code-sample.java
    │   └── images/
    │       └── diagram.png
    └── admin/
        ├── pages/
        ├── partials/
        ├── examples/
        └── images/
```

## Resource Coordinates

The assembler understands Antora resource coordinates:

- `partial$filename.adoc` - Resolves to `modules/{module}/partials/filename.adoc`
- `example$filename.txt` - Resolves to `modules/{module}/examples/filename.txt`
- `page$filename.adoc` - Resolves to `modules/{module}/pages/filename.adoc`
- `image$filename.png` - Resolves to `modules/{module}/images/filename.png`
- `module:page$filename.adoc` - Cross-module reference
- `./relative.adoc` - Relative to current file

## Features

- ✅ Resolves all include directives recursively
- ✅ Handles Antora resource coordinates (partial$, example$, page$, image$)
- ✅ Detects circular dependencies
- ✅ Merges document attributes
- ✅ Preserves cross-references
- ✅ Updates image paths
- ✅ Supports line range and tag filtering in includes
- ✅ Comprehensive error reporting

## Testing

Run the CLI tests:

```bash
./gradlew :antora-assembler:jvmTest --tests "org.markup.poet.antora.assembler.cli.*"
```
