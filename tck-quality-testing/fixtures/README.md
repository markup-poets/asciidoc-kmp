# Test Fixtures

This directory contains test fixtures for the TCK (Technology Compatibility Kit).

## Directory Structure

- `blocks/` - Block-level AsciiDoc constructs (paragraphs, headings, lists, tables, code blocks, quotes)
- `inline/` - Inline formatting (bold, italic, monospace, subscript, superscript)
- `attributes/` - Document attributes and substitutions
- `macros/` - Macro processing
- `malformed/` - Malformed AsciiDoc for error recovery testing
- `conformance/` - AsciiDoc specification conformance tests
- `platform/` - Platform-specific tests (file I/O, encoding, path resolution)

## Fixture File Format

Fixtures are stored as JSON files with the following structure:

```json
{
  "id": "block-paragraph-simple",
  "category": "BLOCK_PARAGRAPH",
  "description": "Simple paragraph with plain text",
  "input": "This is a simple paragraph.",
  "expectedOutput": "<p>This is a simple paragraph.</p>",
  "metadata": {
    "spec_reference": "AsciiDoc Language Documentation - Paragraphs",
    "difficulty": "basic"
  }
}
```

## Fixture Categories

- `BLOCK_PARAGRAPH` - Paragraph blocks
- `BLOCK_HEADING` - Heading blocks (levels 1-6)
- `BLOCK_LIST` - List blocks (ordered, unordered, description)
- `BLOCK_TABLE` - Table blocks
- `BLOCK_CODE` - Code/listing blocks
- `BLOCK_QUOTE` - Quote blocks
- `INLINE_BOLD` - Bold formatting
- `INLINE_ITALIC` - Italic formatting
- `INLINE_MONOSPACE` - Monospace formatting
- `INLINE_SUBSCRIPT` - Subscript formatting
- `INLINE_SUPERSCRIPT` - Superscript formatting
- `ATTRIBUTE` - Document attributes
- `MACRO` - Macros
- `CROSS_REFERENCE` - Cross-references
- `INCLUDE` - Include directives
- `MALFORMED_BLOCK` - Malformed block syntax
- `MALFORMED_INLINE` - Malformed inline syntax
- `MALFORMED_ATTRIBUTE` - Invalid attributes
- `CIRCULAR_INCLUDE` - Circular include references
- `MISSING_INCLUDE` - Missing include files
- `CONFORMANCE` - AsciiDoc spec conformance
- `PLATFORM_FILE_IO` - Platform-specific file I/O tests
- `PLATFORM_ENCODING` - Platform-specific encoding tests
- `PLATFORM_PATH_RESOLUTION` - Platform-specific path resolution tests

## Adding New Fixtures

1. Create a JSON file in the appropriate subdirectory
2. Use the fixture ID pattern: `{category}-{description}`
3. Include all required fields: id, category, description, input
4. Add expectedOutput when the feature is implemented
5. Add metadata for documentation and organization
