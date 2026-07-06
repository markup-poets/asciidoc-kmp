# Article Examples

This directory contains example articles demonstrating the document assembly workflow.

## Working Example (Recommended)

Use these files as templates for your own articles:

- **`index-working.adoc`** - Main article with includes
- **`chapter1-working.adoc`** - First chapter
- **`chapter2-working.adoc`** - Second chapter
- **`assembled.adoc`** - Assembled output (generated)

### To Assemble

From the project root:

```bash
./assemble-article.sh article1/index-working.adoc article1/assembled.adoc
```

Or using Gradle directly:

```bash
./gradlew :antora-assembler:jvmRun --args="$(pwd)/article1/index-working.adoc $(pwd)/article1/assembled.adoc"
```

## Simple Example

Minimal example for testing:

- **`index-simple.adoc`** - Minimal index
- **`chapter1-simple.adoc`** - Minimal chapter
- **`assembled-simple.adoc`** - Output

## Test Files

Various test files used during development:

- `index-test.adoc`, `index-test2.adoc` - Test cases
- `chapter1-test.adoc`, `chapter1-test2.adoc` - Test chapters

## Original Files (Don't Use)

These files contain unsupported syntax and will cause the parser to hang:

- ❌ `index.adoc` - Contains `[source,kotlin]` attributes
- ❌ `chapter1.adoc` - Contains `[source,kotlin]` attributes
- ❌ `chapter2.adoc` - Contains `[source,kotlin]` attributes

**Note:** These demonstrate the parser limitation with block attributes.

## What Works

✅ Headings (all levels)
✅ Paragraphs
✅ Lists (ordered and unordered)
✅ Code blocks (plain `----` delimiters)
✅ Include directives
✅ Document attributes

## What Doesn't Work

❌ Block attributes like `[source,kotlin]`
❌ Admonition blocks with attributes

## Converting to HTML

After assembling, convert to HTML with AsciiDoctor:

```bash
# Install AsciiDoctor (one-time)
brew install asciidoctor  # macOS
# or
gem install asciidoctor   # Any platform

# Convert
asciidoctor article1/assembled.adoc

# View
open article1/assembled.html
```

## Directory Structure

```
article1/
├── README.md                    # This file
├── index-working.adoc           # ✅ Use this as template
├── chapter1-working.adoc        # ✅ Use this as template
├── chapter2-working.adoc        # ✅ Use this as template
├── assembled.adoc               # Generated output
├── index-simple.adoc            # Minimal example
├── chapter1-simple.adoc         # Minimal example
├── assembled-simple.adoc        # Generated output
├── index.adoc                   # ❌ Don't use (has unsupported syntax)
├── chapter1.adoc                # ❌ Don't use (has unsupported syntax)
├── chapter2.adoc                # ❌ Don't use (has unsupported syntax)
└── images/                      # Place images here
```

## Tips

1. Start with `index-working.adoc` as your template
2. Create one chapter file per major section
3. Use plain code blocks without attributes
4. Test assembly after adding each chapter
5. Keep images in the `images/` subdirectory

## Getting Help

- Read `../publisher.adoc` for the complete guide
- Check `../WORKFLOW_COMPLETE.md` for technical details
- See `../PUBLISHER_DEMO.md` for the demonstration
