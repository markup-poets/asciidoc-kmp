# Position Tracking Issue in Nested Inline Elements

**Date:** January 24, 2026  
**Status:** Critical Bug Identified 🔴  
**Impact:** Official TCK tests failing due to incorrect position tracking

---

## Problem Summary

When parsing nested inline elements (like `*s*`), the inner text positions are calculated relative to the extracted substring, not the original source document.

### Test Results

✅ **Test 1 PASSED**: `inline-no-markup-single-word` ("hello")  
❌ **Test 2 FAILED**: `inline-span-strong-constrained-single-char` ("*s*")

### Error Message

```
Value mismatch at root[0].inlines[0].location[0].col: expected '2', got '1'
```

---

## Root Cause

### Current Implementation

In `InlineParser.parseStrong()`:

```kotlin
// Extract inner text from *s*
val innerText = text.substring(startIndex + 1, closingIndex)  // "s"

// Parse inner elements
val innerElements = parseInlineElements(innerText, lineNumber)  // ← Problem!
```

When we call `parseInlineElements("s", 1)`, we're passing:
- A NEW string "s" (not the original "*s*")
- The line number (correct)
- But NO column offset!

So `parseInlineElements` treats "s" as starting at column 1, when it should be column 2.

### Expected Behavior

For input `*s*`:
- The `*` at index 0 → column 1
- The `s` at index 1 → column 2  
- The `*` at index 2 → column 3

The inner text "s" should have `location: [{line:1, col:2}, {line:1, col:2}]`

### Actual Behavior

We're outputting `location: [{line:1, col:1}, {line:1, col:1}]` because we parse "s" as a new string.

---

## Solution Required

### Option 1: Pass Column Offset (Recommended)

Modify `parseInlineElements` to accept a column offset:

```kotlin
fun parseInlineElements(
    text: String, 
    startLineNumber: Int = 0,
    startColumnOffset: Int = 0  // NEW parameter
): List<InlineElement>
```

Then when calculating positions:
```kotlin
val startCol = currentIndex + 1 + startColumnOffset
val endCol = currentIndex + trimmedContent.length + startColumnOffset
```

Update all callers:
- `parseStrong`: `parseInlineElements(innerText, lineNumber, startIndex + 1)`
- `parseEmphasis`: `parseInlineElements(innerText, lineNumber, startIndex + 1)`
- `parseCode`: Similar adjustment
- Top-level calls: `parseInlineElements(text, lineNumber, 0)`

### Option 2: Parse on Original String

Instead of extracting substrings, parse inline elements on the original string with start/end indices:

```kotlin
fun parseInlineElements(
    text: String,
    startIndex: Int,
    endIndex: Int,
    lineNumber: Int
): List<InlineElement>
```

This is more complex but avoids substring operations.

---

## Impact Assessment

### Tests Affected

Based on the 13 official TCK tests:
- ✅ Plain text tests (no nesting): PASS
- ❌ Any test with inline formatting: FAIL
  - Strong/bold: `*text*`
  - Emphasis/italic: `_text_`
  - Code: `` `text` ``
  - Links: `link:url[text]`
  - Images: `image:path[alt]`

**Estimated failures:** 8-10 out of 13 tests

### Severity

**HIGH** - This is a fundamental issue with position tracking that affects most inline formatting.

---

## Files to Modify

1. **`asciidoc-parser/src/commonMain/kotlin/org/markup/poet/asciidoc/parser/InlineParser.kt`**
   - Add `startColumnOffset` parameter to `parseInlineElements()`
   - Update position calculations
   - Update all recursive calls

2. **`asciidoc-parser/src/commonMain/kotlin/org/markup/poet/asciidoc/parser/BlockParser.kt`**
   - Update calls to `parseInlineElements()` to pass column offset (usually 0)

3. **All inline parsing methods:**
   - `parseStrong()` - pass `startIndex + 1` as offset
   - `parseEmphasis()` - pass `startIndex + 1` as offset  
   - `parseCode()` - pass `startIndex + 1` as offset
   - `parseLink()` - calculate offset for link text
   - `parseImage()` - calculate offset for alt text

---

## Testing Strategy

### Unit Tests

Create tests for each inline element type:

```kotlin
@Test
fun `strong text should track inner positions correctly`() {
    val parser = DefaultAsciidocParser()
    val result = parser.parse("*s*")
    
    val paragraph = result.document.children[0] as Paragraph
    val strong = paragraph.content[0] as Strong
    val text = strong.content[0] as Text
    
    // The 's' is at column 2 in the source
    assertEquals(2, text.sourceLocation.column)
    assertEquals(2, text.sourceLocation.endColumn)
}
```

### TCK Tests

After fix, run all 13 official tests:
```bash
./gradlew :tck-quality-testing:jvmTest --tests "IdentifyHangingTest"
```

Expected improvement: 1/13 → 10+/13 passing

---

## Implementation Steps

1. **Add column offset parameter** to `parseInlineElements()`
2. **Update position calculations** to include offset
3. **Update all callers** to pass correct offset
4. **Test with unit tests** for each inline type
5. **Run official TCK** to verify fixes
6. **Document the change** in code comments

---

## Additional Notes

### Why This Wasn't Caught Earlier

- The first test (`inline-no-markup-single-word`) has no nesting, so it passed
- We only discovered this when testing the second official test
- Our custom tests may not have checked nested element positions

### Related Issues

- This same issue likely affects:
  - Multi-line text (line number tracking)
  - Nested formatting (e.g., `*bold _italic_*`)
  - Complex inline structures

### Performance Consideration

Adding a column offset parameter has minimal performance impact - it's just an integer addition in position calculations.

---

## Priority

**CRITICAL** - Must be fixed before official TCK certification can proceed.

**Estimated Time:** 2-3 hours
- 1 hour: Implement fix
- 1 hour: Write unit tests
- 1 hour: Test with official TCK and fix edge cases

---

## References

- Official TCK test: `inline/span/strong/constrained-single-char`
- Test input: `*s*`
- Expected output: `tck-quality-testing/official-tck/repository/tests/inline/span/strong/constrained-single-char-output.json`
- Debug test: `DebugTestRunnerTest.kt`

