# Official TCK Fixture Loader Issue

**Date:** January 24, 2026  
**Status:** Critical Issue Identified 🔴  
**Impact:** Official TCK tests cannot be loaded

---

## Problem Summary

The `OfficialTckFixtureLoader` cannot load official TCK tests because it expects all output JSON files to be objects `{}`, but inline tests output arrays `[]`.

### Diagnostic Results

Running the diagnostic test revealed:
- **Total fixtures loaded:** 55
- **Official TCK fixtures:** 0 ❌
- **Custom fixtures:** 55 ✅

### Error Messages

```
Warning: Failed to load test from .../inline/no-markup/single-word-input.adoc: 
Invalid JSON in .../single-word-output.json: 
Expected start of the object '{', but had '[' instead
```

---

## Root Cause

### Current Implementation

The `OfficialTckFixtureLoader` tries to parse all output JSON as `OfficialAstNode`:

```kotlin
val expectedOutput = json.decodeFromString<OfficialAstNode>(outputJson)
```

This assumes the JSON is always an object:
```json
{
  "name": "document",
  "type": "block",
  ...
}
```

### Official TCK Format

The official TCK has **two different output formats**:

#### 1. Inline Tests (Array Format)
```json
[
  {
    "name": "text",
    "type": "string",
    "value": "hello",
    "location": [{"line": 1, "col": 1}, {"line": 1, "col": 5}]
  }
]
```

#### 2. Block Tests (Object Format)
```json
{
  "name": "document",
  "type": "block",
  "blocks": [...]
}
```

---

## Impact

### What Works ✅
- Custom test fixtures (55 tests)
- Column tracking implementation
- Progress logging
- Test execution infrastructure

### What Doesn't Work ❌
- Loading official TCK tests
- Running official TCK suite
- Certification testing

---

## Solution Required

The `OfficialTckFixtureLoader` needs to be updated to:

1. **Detect the JSON format** (array vs object)
2. **Parse accordingly:**
   - If array → inline test
   - If object → block test
3. **Store the raw JSON string** instead of parsing to a specific type
4. **Let the test runner handle the format** during comparison

### Proposed Fix

```kotlin
fun parseTestPair(inputFilePath: String): OfficialTestData {
    val outputFilePath = inputFilePath.replace("-input.adoc", "-output.json")
    
    if (!platformFileExists(outputFilePath)) {
        throw IllegalArgumentException("Missing output file: $outputFilePath")
    }
    
    val input = platformReadFile(inputFilePath)
    val outputJson = platformReadFile(outputFilePath)
    
    // Don't parse the JSON yet - just validate it's valid JSON
    try {
        Json.parseToJsonElement(outputJson)
    } catch (e: Exception) {
        throw IllegalArgumentException("Invalid JSON in $outputFilePath: ${e.message}", e)
    }
    
    // Detect format
    val isInlineTest = outputJson.trimStart().startsWith("[")
    
    val testId = extractTestId(inputFilePath)
    val category = extractCategory(inputFilePath)
    
    return OfficialTestData(
        testId = testId,
        description = extractDescription(inputFilePath),
        input = input,
        expectedOutputJson = outputJson,  // Store as string
        isInlineTest = isInlineTest,
        category = category,
        metadata = mapOf(
            "source" to "official",
            "type" to if (isInlineTest) "inline" else "block",
            "file_path" to inputFilePath
        )
    )
}
```

### Data Structure Update

```kotlin
data class OfficialTestData(
    val testId: String,
    val description: String,
    val input: String,
    val expectedOutputJson: String,  // Changed from OfficialAstNode
    val isInlineTest: Boolean,       // New field
    val category: String,
    val metadata: Map<String, String>
)
```

---

## Files That Need Updates

1. **`tck-quality-testing/src/commonMain/kotlin/org/markup/poet/tck/fixtures/OfficialTckFixtureLoader.kt`**
   - Update `parseTestPair()` to handle both formats
   - Store raw JSON string instead of parsed object
   - Add format detection

2. **`tck-quality-testing/src/commonMain/kotlin/org/markup/poet/tck/fixtures/OfficialTestData.kt`**
   - Change `expectedOutput: OfficialAstNode` to `expectedOutputJson: String`
   - Add `isInlineTest: Boolean` field

3. **`tck-quality-testing/src/commonMain/kotlin/org/markup/poet/tck/fixtures/TestFixture.kt`**
   - Update conversion from `OfficialTestData` to `TestFixture`
   - Pass raw JSON string to test fixture

---

## Workaround (Current)

Currently, only custom test fixtures work. These are the 55 tests we created ourselves in:
- `tck-quality-testing/fixtures/blocks/`
- `tck-quality-testing/fixtures/inline/`
- `tck-quality-testing/fixtures/conformance/`

These tests use our custom format which works correctly.

---

## Next Steps

### Option 1: Fix the Loader (Recommended)
1. Update `OfficialTckFixtureLoader` to handle both formats
2. Update data structures
3. Test with official TCK
4. Run full suite

**Estimated time:** 1-2 hours

### Option 2: Use Custom Tests Only
1. Continue with 55 custom tests
2. Skip official TCK for now
3. Focus on parser implementation
4. Return to official TCK later

**Pros:** Can continue testing immediately  
**Cons:** No official certification

---

## Test Results Without Fix

Without fixing the loader:
- **Custom tests:** 55 available ✅
- **Official tests:** 0 available ❌
- **Certification:** Not possible ❌

---

## Conclusion

The official TCK fixture loader has a critical bug that prevents loading any official tests. The loader assumes all output JSON is an object, but inline tests use arrays. This must be fixed before official certification testing can proceed.

**Priority:** HIGH 🔴  
**Blocking:** Official TCK Testing  
**Workaround:** Use custom tests (55 available)

---

## References

- Diagnostic test: `DiagnosticTest.kt`
- Fixture loader: `OfficialTckFixtureLoader.kt`
- Official TCK format: `tck-quality-testing/docs/official-tck-format.md`
- Test repository: `tck-quality-testing/official-tck/repository/tests/`
