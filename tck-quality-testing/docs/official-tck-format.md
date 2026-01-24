# Official AsciiDoc TCK Test Format

## Overview

The official Eclipse AsciiDoc TCK uses a **paired-file format** for test cases:
- `{test-name}-input.adoc`: The AsciiDoc source to parse
- `{test-name}-output.json`: The expected AST in JSON format

This document describes the structure and conventions used in these test files.

## File Naming Convention

Test files follow a consistent naming pattern:

```
tests/
├── block/
│   ├── paragraph/
│   │   ├── single-line-input.adoc
│   │   ├── single-line-output.json
│   │   ├── multiple-lines-input.adoc
│   │   └── multiple-lines-output.json
│   └── section/
│       └── ...
└── inline/
    └── span/
        └── strong/
            ├── constrained-single-char-input.adoc
            └── constrained-single-char-output.json
```

**Pattern**: `{descriptive-name}-{input|output}.{adoc|json}`

## Directory Structure

The directory structure maps to test categories:

| Directory Path | Category | Description |
|---------------|----------|-------------|
| `tests/block/paragraph/` | Block-level paragraphs | Simple and complex paragraph tests |
| `tests/block/section/` | Block-level sections | Section headings and structure |
| `tests/block/list/` | Block-level lists | Ordered, unordered, and nested lists |
| `tests/block/listing/` | Block-level listings | Code blocks and literal blocks |
| `tests/block/header/` | Document headers | Document title and metadata |
| `tests/block/document/` | Document structure | Complete document tests |
| `tests/block/sidebar/` | Sidebar blocks | Sidebar content |
| `tests/inline/no-markup/` | Plain text | Text without formatting |
| `tests/inline/span/strong/` | Strong (bold) text | Bold formatting |
| `tests/inline/span/emphasis/` | Emphasis (italic) text | Italic formatting |
| `tests/inline/span/monospace/` | Monospace text | Code/monospace formatting |

## Input File Format (.adoc)

Input files contain raw AsciiDoc source:

```asciidoc
A paragraph that consists of a single line.
```

**Characteristics:**
- Plain text AsciiDoc markup
- No special metadata or headers
- Represents the exact input to be parsed
- May include newlines, whitespace, and special characters

## Output File Format (.json)

Output files contain the expected AST in JSON format.

### JSON AST Structure

The AST uses a recursive node structure:

```json
{
  "name": "document",
  "type": "block",
  "blocks": [
    {
      "name": "paragraph",
      "type": "block",
      "inlines": [
        {
          "name": "text",
          "type": "string",
          "value": "A paragraph that consists of a single line.",
          "location": [
            { "line": 1, "col": 1 },
            { "line": 1, "col": 43 }
          ]
        }
      ],
      "location": [
        { "line": 1, "col": 1 },
        { "line": 1, "col": 43 }
      ]
    }
  ],
  "location": [
    { "line": 1, "col": 1 },
    { "line": 1, "col": 43 }
  ]
}
```

### Node Types

#### Block Nodes

Block nodes represent block-level elements:

```json
{
  "name": "paragraph",
  "type": "block",
  "inlines": [...],
  "location": [...]
}
```

**Common block names:**
- `document`: Root document node
- `paragraph`: Paragraph block
- `section`: Section with heading
- `list`: List block (ordered or unordered)
- `listing`: Code or literal block
- `sidebar`: Sidebar block

#### Inline Nodes

Inline nodes represent inline formatting:

```json
{
  "name": "span",
  "type": "inline",
  "variant": "strong",
  "form": "constrained",
  "inlines": [...],
  "location": [...]
}
```

**Common inline names:**
- `span`: Formatted text span
- `text`: Plain text (type="string")

**Variants:**
- `strong`: Bold text
- `emphasis`: Italic text
- `monospace`: Monospace/code text
- `mark`: Highlighted text
- `subscript`: Subscript text
- `superscript`: Superscript text

**Forms:**
- `constrained`: Surrounded by word boundaries (e.g., `*bold*`)
- `unconstrained`: Not constrained by word boundaries (e.g., `**bold**`)

#### Text Nodes

Text nodes contain actual text content:

```json
{
  "name": "text",
  "type": "string",
  "value": "Hello world",
  "location": [...]
}
```

### Location Information

Every node includes location information tracking its position in the source:

```json
"location": [
  { "line": 1, "col": 1 },   // Start position
  { "line": 1, "col": 43 }   // End position
]
```

**Coordinates:**
- `line`: Line number (1-based)
- `col`: Column number (1-based)
- Array always has exactly 2 elements: [start, end]

### Node Fields Reference

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | string | Yes | Node name (e.g., "document", "paragraph", "text") |
| `type` | string | Yes | Node type (e.g., "block", "inline", "string") |
| `blocks` | array | No | Child block nodes (for block containers) |
| `inlines` | array | No | Child inline nodes (for blocks with inline content) |
| `value` | string | No | Text value (for type="string" nodes) |
| `variant` | string | No | Inline variant (e.g., "strong", "emphasis") |
| `form` | string | No | Inline form (e.g., "constrained", "unconstrained") |
| `location` | array | No | Source position [start, end] |
| `attributes` | object | No | Additional node-specific attributes |

## Examples

### Example 1: Simple Paragraph

**Input** (`single-line-input.adoc`):
```asciidoc
A paragraph that consists of a single line.
```

**Output** (`single-line-output.json`):
```json
{
  "name": "document",
  "type": "block",
  "blocks": [
    {
      "name": "paragraph",
      "type": "block",
      "inlines": [
        {
          "name": "text",
          "type": "string",
          "value": "A paragraph that consists of a single line.",
          "location": [{ "line": 1, "col": 1 }, { "line": 1, "col": 43 }]
        }
      ],
      "location": [{ "line": 1, "col": 1 }, { "line": 1, "col": 43 }]
    }
  ],
  "location": [{ "line": 1, "col": 1 }, { "line": 1, "col": 43 }]
}
```

### Example 2: Multi-line Paragraph

**Input** (`multiple-lines-input.adoc`):
```asciidoc
This paragraph has multiple lines that wrap after reaching the 72
character limit set by the text editor.
```

**Output** (`multiple-lines-output.json`):
```json
{
  "name": "document",
  "type": "block",
  "blocks": [
    {
      "name": "paragraph",
      "type": "block",
      "inlines": [
        {
          "name": "text",
          "type": "string",
          "value": "This paragraph has multiple lines that wrap after reaching the 72\ncharacter limit set by the text editor.",
          "location": [{ "line": 1, "col": 1 }, { "line": 2, "col": 39 }]
        }
      ],
      "location": [{ "line": 1, "col": 1 }, { "line": 2, "col": 39 }]
    }
  ],
  "location": [{ "line": 1, "col": 1 }, { "line": 2, "col": 39 }]
}
```

### Example 3: Strong (Bold) Text

**Input** (`constrained-single-char-input.adoc`):
```asciidoc
*s*
```

**Output** (`constrained-single-char-output.json`):
```json
[
  {
    "name": "span",
    "type": "inline",
    "variant": "strong",
    "form": "constrained",
    "inlines": [
      {
        "name": "text",
        "type": "string",
        "value": "s",
        "location": [{ "line": 1, "col": 2 }, { "line": 1, "col": 2 }]
      }
    ],
    "location": [{ "line": 1, "col": 1 }, { "line": 1, "col": 3 }]
  }
]
```

**Note**: Some inline tests return an array of inline nodes rather than a document node.

## Test Discovery

To discover all tests in the official TCK:

1. Scan the `tests/` directory recursively
2. Find all files matching `*-input.adoc`
3. For each input file, look for corresponding `*-output.json`
4. Extract test name by removing `-input.adoc` suffix
5. Map directory path to test category

**Example:**
```
File: tests/block/paragraph/single-line-input.adoc
Test ID: block/paragraph/single-line
Category: block/paragraph
Input: tests/block/paragraph/single-line-input.adoc
Output: tests/block/paragraph/single-line-output.json
```

## Validation Strategy

When validating our parser against official tests:

1. **Parse Input**: Parse the `.adoc` file with our AsciiDoc parser
2. **Convert to AST**: Convert our internal AST to the official JSON format
3. **Compare**: Compare our output JSON with the expected `.json` file
4. **Report Differences**: Report any structural or content differences

**Comparison Considerations:**
- Location information may differ if our parser tracks positions differently
- Whitespace in text values must match exactly (including `\n`)
- Node structure must match exactly (same nesting, same node types)
- Optional fields may be omitted if not applicable

## Integration with Kotlin Code

The Kotlin data models in `OfficialAstNode.kt` directly map to this JSON structure:

```kotlin
@Serializable
data class OfficialAstNode(
    val name: String,
    val type: String,
    val blocks: List<OfficialAstNode>? = null,
    val inlines: List<OfficialAstNode>? = null,
    val value: String? = null,
    val variant: String? = null,
    val form: String? = null,
    val location: List<SourcePosition>? = null,
    val attributes: Map<String, String>? = null
)
```

Use `kotlinx.serialization` to parse JSON files:

```kotlin
val json = Json { ignoreUnknownKeys = true }
val ast = json.decodeFromString<OfficialAstNode>(jsonString)
```

## References

- Official TCK Repository: https://gitlab.eclipse.org/eclipse/asciidoc-lang/asciidoc-tck
- AsciiDoc Language Specification: https://docs.asciidoc.org/asciidoc/latest/
