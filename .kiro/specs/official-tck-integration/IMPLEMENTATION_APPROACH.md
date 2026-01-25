# Official TCK Integration: Implementation Approach

## Overview

This document clarifies our approach to integrating the official Eclipse AsciiDoc TCK into our Kotlin Multiplatform library.

## The Challenge

The official Eclipse AsciiDoc TCK is a **JavaScript-based test harness** with:
- Node.js dependencies (package.json)
- JavaScript test runner (harness/lib/)
- JavaScript test framework
- npm-based tooling

However, our project has a strict guideline: **No Ruby or JavaScript tools for Kotlin Multiplatform implementation.**

## Our Solution: Data-Only Integration

We integrate with the official TCK by **extracting test data** while **implementing our own Kotlin-based execution infrastructure**.

### What We Extract from Official TCK

```
official-tck/repository/tests/
├── block/
│   ├── paragraph/
│   │   ├── single-line-input.adoc          ← We use this
│   │   ├── single-line-output.json         ← We use this
│   │   ├── multiple-lines-input.adoc       ← We use this
│   │   └── multiple-lines-output.json      ← We use this
│   └── section/
│       └── ...
└── inline/
    └── ...
```

**Test Data Files:**
- `*-input.adoc`: The AsciiDoc input to parse
- `*-output.json`: The expected AST output in JSON format

**Directory Structure:**
- `tests/block/paragraph/` → Maps to `FixtureCategory.BLOCK_PARAGRAPH`
- `tests/inline/span/` → Maps to `FixtureCategory.INLINE_*`

### What We DON'T Use from Official TCK

```
official-tck/repository/
├── harness/                    ← We DON'T use this
│   ├── lib/                    ← JavaScript test runner
│   └── bin/                    ← JavaScript CLI
├── package.json                ← We DON'T use this
├── package-lock.json           ← We DON'T use this
└── node_modules/               ← We DON'T use this
```

## Our Kotlin-Based Implementation

### 1. Repository Sync (Pure Kotlin/Java)

```kotlin
// JVM: Use JGit (pure Java)
class JvmGitOperations : PlatformGitOperations {
    override suspend fun clone(url: String, destination: String) {
        Git.cloneRepository()
            .setURI(url)
            .setDirectory(File(destination))
            .call()
    }
}

// Native: Use system git command
class NativeGitOperations : PlatformGitOperations {
    override suspend fun clone(url: String, destination: String) {
        ProcessBuilder("git", "clone", url, destination)
            .start()
            .waitFor()
    }
}
```

### 2. Test Data Loading (Pure Kotlin)

```kotlin
class OfficialTckFixtureLoader(
    private val tckPath: String
) : FixtureLoader {
    override fun loadFixture(id: String): TestFixture {
        // Read .adoc file (pure Kotlin file I/O)
        val input = File("$tckPath/$id-input.adoc").readText()
        
        // Read .json file (kotlinx.serialization)
        val outputJson = File("$tckPath/$id-output.json").readText()
        val expectedOutput = Json.decodeFromString<AstNode>(outputJson)
        
        // Convert to internal format
        return TestFixture(
            id = id,
            category = mapCategory(id),
            input = input,
            expectedOutput = expectedOutput.toString()
        )
    }
}
```

### 3. Test Execution (Pure Kotlin)

```kotlin
class OfficialCompatibilityTest : CompatibilityTest() {
    @Test
    fun `official test - block paragraph single line`() {
        // Load test data (no JavaScript)
        val fixture = fixtureLoader.loadFixture("block/paragraph/single-line")
        
        // Execute with our Kotlin parser
        val ast = AsciiDocParser().parse(fixture.input)
        
        // Render with our Kotlin renderer
        val output = HtmlRenderer().render(ast)
        
        // Validate (pure Kotlin comparison)
        assertEquals(fixture.expectedOutput, output)
    }
}
```

### 4. Conformance Reporting (Pure Kotlin)

```kotlin
class ConformanceReportGenerator {
    fun generateReport(results: List<TestResult>): ConformanceReport {
        // Pure Kotlin data processing
        return ConformanceReport(
            totalTests = results.size,
            passed = results.count { it.passed },
            failed = results.count { !it.passed },
            // ... more statistics
        )
    }
    
    fun exportToJson(report: ConformanceReport): String {
        // kotlinx.serialization (no JavaScript)
        return Json.encodeToString(report)
    }
}
```

## Benefits of This Approach

### ✅ Advantages

1. **Pure Kotlin Multiplatform**: Works on JVM, Android, iOS, Linux without JavaScript runtime
2. **No External Dependencies**: No Node.js, npm, or JavaScript tooling required
3. **Official Test Data**: Uses canonical test cases from Eclipse Foundation
4. **Certification Path**: Can demonstrate conformance using official test cases
5. **Platform Native**: Uses platform-appropriate tools (JGit on JVM, native git on iOS/Linux)
6. **Maintainable**: All code is Kotlin, consistent with rest of project

### ⚠️ Considerations

1. **Output Format Differences**: Official TCK expects JSON AST, we may produce HTML
   - **Solution**: Implement FormatTranslator to convert between formats
2. **Test Harness Features**: Official harness may have features we need to replicate
   - **Solution**: Implement only what's needed for test execution and reporting
3. **Sync Overhead**: Must sync entire repository to get test data
   - **Solution**: Use shallow clone, cache between runs

## Validation Strategy

### How We Validate Conformance

```
Official Test Data → Our Parser → Our Renderer → Compare with Expected
     (input.adoc)   (Kotlin)      (Kotlin)         (output.json)
```

**Example:**

```kotlin
// Input from official TCK
val input = "A paragraph that consists of a single line."

// Expected output from official TCK (JSON AST)
val expected = """
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
          "value": "A paragraph that consists of a single line."
        }
      ]
    }
  ]
}
"""

// Our execution (pure Kotlin)
val ast = parser.parse(input)
val actualJson = ast.toJson()

// Validation (pure Kotlin)
assertEquals(expected, actualJson)
```

## Summary

| Aspect | Official TCK | Our Implementation |
|--------|-------------|-------------------|
| **Test Data** | ✅ Use input.adoc and output.json files | ✅ Extract and parse with Kotlin |
| **Test Harness** | ❌ JavaScript-based | ✅ Kotlin-based |
| **Execution** | ❌ Node.js runtime | ✅ Kotlin Multiplatform |
| **Dependencies** | ❌ npm packages | ✅ Kotlin/Java libraries only |
| **Platforms** | ❌ Requires Node.js | ✅ JVM, Android, iOS, Linux |
| **Conformance** | ✅ Official test cases | ✅ Same test data, Kotlin execution |

## Conclusion

We achieve **official TCK conformance validation** while maintaining a **pure Kotlin Multiplatform implementation** by:

1. Extracting test data from the official TCK repository
2. Implementing our own Kotlin-based test execution infrastructure
3. Validating our parser/renderer output against official expected outputs
4. Generating conformance reports that demonstrate spec compliance

This approach respects both the project's "no JavaScript" guideline and the need for official specification conformance.
