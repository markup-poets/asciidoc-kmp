---
inclusion: always
---

# Property-Based Testing in Kotlin Multiplatform

## Introduction

Property-based testing (PBT) is a powerful testing approach that complements traditional unit testing. Instead of writing individual test cases with specific inputs, you define **properties** (universal rules) that should hold true for all inputs, and the testing framework generates hundreds or thousands of test cases automatically.

**Important**: Property-based testing does NOT replace unit testing. They work together:
- **Unit tests** provide concrete examples and document expected behavior
- **Property-based tests** verify universal rules across the entire input space

Both are essential for robust, correct software.

## Why Property-Based Testing Matters

Traditional unit testing has limitations:
- You can only test the cases you think of
- Edge cases are often missed
- Test coverage doesn't guarantee correctness

Property-based testing addresses these by:
- Automatically generating diverse test inputs
- Finding edge cases you never considered
- Verifying mathematical properties and invariants
- Providing higher confidence in correctness

## The Complementary Relationship

Property-based testing (PBT) and traditional unit testing are **complementary**, not competing approaches. Both are essential for robust software.

### Unit Tests: The Foundation
Unit tests are your **concrete examples** that:
- Document expected behavior with specific cases
- Verify known edge cases and boundary conditions
- Provide clear, readable examples of how code should work
- Catch regressions on specific scenarios you've encountered
- Are easy to debug when they fail (you know exactly what input failed)

**Example:**
```kotlin
@Test
fun `should parse simple bold text`() {
    val input = "*bold*"
    val result = parseInline(input)
    assertEquals(Bold("bold"), result)
}

@Test
fun `should handle empty bold markers`() {
    val input = "**"
    val result = parseInline(input)
    assertEquals(Text("**"), result)
}
```

### Property-Based Tests: The Universal Laws
Property-based tests verify **universal properties** that should hold for **all inputs**:
- Invariants that must always be true
- Relationships between inputs and outputs
- Structural properties of your data
- Round-trip properties (serialize → deserialize → same result)
- Catch edge cases you never thought to test

**Example:**
```kotlin
@Test
fun `parsing then rendering should preserve semantic meaning`() = checkAll(
    Arb.asciidocDocument()
) { doc ->
    val parsed = parse(doc)
    val rendered = render(parsed)
    val reparsed = parse(rendered)
    
    // Semantic equivalence, not string equality
    parsed shouldBeEquivalentTo reparsed
}
```

## The Testing Strategy

### Start with Unit Tests
1. Write unit tests for basic functionality
2. Cover known edge cases
3. Document expected behavior with examples
4. Ensure the happy path works

### Add Property-Based Tests
1. Identify universal properties (invariants, relationships)
2. Write properties that should hold for all inputs
3. Let the PBT framework generate thousands of test cases
4. Discover edge cases you didn't think of

### When PBT Finds a Bug
1. PBT gives you a failing input (counterexample)
2. Add that specific case as a unit test
3. Fix the bug
4. Now you have both:
   - A unit test documenting the specific bug
   - A property test ensuring the general rule holds

## Real-World Example

### Unit Tests (Specific Cases)
```kotlin
class ParserTest {
    @Test
    fun `should parse heading level 1`() {
        assertEquals(Heading(1, "Title"), parse("= Title"))
    }
    
    @Test
    fun `should parse heading level 2`() {
        assertEquals(Heading(2, "Section"), parse("== Section"))
    }
    
    @Test
    fun `should reject heading level 7`() {
        assertFailsWith<ParseError> {
            parse("======= Invalid")
        }
    }
}
```

### Property-Based Tests (Universal Rules)
```kotlin
class ParserPropertyTest {
    @Test
    fun `heading level should match number of equals signs`() = checkAll(
        Arb.int(1..6),
        Arb.string(1..100)
    ) { level, text ->
        val input = "=".repeat(level) + " " + text
        val result = parse(input)
        
        result.shouldBeInstanceOf<Heading>()
        result.level shouldBe level
    }
    
    @Test
    fun `parsing should never crash on any input`() = checkAll(
        Arb.string()
    ) { input ->
        // Should either succeed or throw ParseError, never crash
        shouldNotThrowAny<Throwable> {
            try {
                parse(input)
            } catch (e: ParseError) {
                // Expected for invalid input
            }
        }
    }
}
```

## Benefits of Both Together

### Unit Tests Give You:
- ✅ Clear documentation of expected behavior
- ✅ Fast feedback on specific scenarios
- ✅ Easy debugging (you know the exact input)
- ✅ Regression prevention for known bugs
- ✅ Readable test names that explain intent

### Property-Based Tests Give You:
- ✅ Confidence across the entire input space
- ✅ Discovery of unexpected edge cases
- ✅ Verification of mathematical properties
- ✅ Automatic test case generation
- ✅ Higher-level correctness guarantees

## Guidelines for This Project

### Always Write Both
For each feature:
1. **Unit tests** for basic functionality and known edge cases
2. **Property-based tests** for universal properties and invariants

### Unit Test Coverage
- Basic happy path scenarios
- Known edge cases (empty input, null, boundaries)
- Error conditions
- Specific bugs you've encountered

### Property-Based Test Coverage
- Round-trip properties (parse → render → parse)
- Invariants (e.g., "parsed AST depth never exceeds input nesting")
- Relationships (e.g., "rendered output length ≤ input length + markup overhead")
- Structural properties (e.g., "all opened blocks are closed")
- Robustness (e.g., "parser never crashes, only returns error")

## Common Properties to Test

### Parser Properties
- **Robustness**: Never crash on any input
- **Completeness**: All valid syntax is accepted
- **Determinism**: Same input always produces same output
- **Structure preservation**: Nesting levels are maintained

### Renderer Properties
- **Round-trip**: parse(render(ast)) ≈ ast
- **Validity**: Rendered output is valid markup
- **Idempotence**: render(parse(render(ast))) = render(ast)

### Transformer Properties
- **Structure preservation**: AST shape is maintained
- **Reversibility**: Some transforms have inverses
- **Composition**: transform(transform(x)) behaves predictably

## When to Use Each

### Use Unit Tests When:
- You have a specific example to document
- You found a bug and want to prevent regression
- You want to explain how something works
- You're testing a specific edge case

### Use Property-Based Tests When:
- You can express a universal rule
- You want to test across many inputs
- You're testing mathematical properties
- You want to discover unknown edge cases

## Kotlin Multiplatform Considerations

### Testing Framework: Kotest
This project uses **Kotest** for property-based testing, which provides excellent KMP support:
- Works across all platforms (JVM, Android, iOS, Linux)
- Integrates seamlessly with `kotlin-test`
- Provides powerful property testing with `kotest-property`

### Dependency Setup
```kotlin
// In build.gradle.kts
kotlin {
    sourceSets {
        val commonTest by getting {
            dependencies {
                implementation(libs.kotest.framework.engine)
                implementation(libs.kotest.assertions.core)
                implementation(libs.kotest.property)
            }
        }
    }
}
```

### Source Set Organization
```
library/src/
├── commonTest/kotlin/          # Both unit AND property-based tests
│   ├── ParserTest.kt          # Unit tests (specific examples)
│   └── ParserPropertyTest.kt  # Property tests (universal rules)
├── jvmTest/kotlin/            # Platform-specific tests if needed
└── iosTest/kotlin/
```

### Running Property-Based Tests
```bash
# All tests (unit + property-based) across all platforms
./gradlew test

# Specific platform
./gradlew :library:jvmTest
./gradlew :library:iosX64Test

# Common tests only
./gradlew :library:commonTest
```

### Platform-Specific Considerations
- Property-based tests run on all configured platforms
- Generators work identically across platforms
- Some properties may need platform-specific validation
- Use `commonTest` for shared properties, platform-specific test sources for platform behaviors

## Kotest Property Testing Basics

### Essential Imports
```kotlin
import io.kotest.property.checkAll
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
```

### Basic Property Test Structure
```kotlin
import kotlin.test.Test
import io.kotest.property.checkAll
import io.kotest.property.Arb

class ParserPropertyTest {
    @Test
    fun `property name describing the universal rule`() = runTest {
        checkAll<InputType> { input ->
            // Arrange & Act
            val result = functionUnderTest(input)
            
            // Assert - verify the property holds
            result shouldSatisfy someCondition
        }
    }
}
```

### Custom Generators (Arbitraries)
```kotlin
// Define custom generators for your domain
fun Arb.Companion.asciidocHeading(): Arb<String> = arbitrary {
    val level = Arb.int(1..6).bind()
    val text = Arb.string(1..100).bind()
    "=".repeat(level) + " " + text
}

// Use in tests
@Test
fun `all valid headings should parse successfully`() = runTest {
    checkAll(Arb.asciidocHeading()) { heading ->
        val result = parse(heading)
        result.shouldBeInstanceOf<Heading>()
    }
}
```

## The Bottom Line

**You need both.** Unit tests are your foundation and documentation. Property-based tests are your safety net and edge case discoverer. Together, they give you confidence that your code works correctly across all scenarios.

Think of it this way:
- **Unit tests**: "Here are 10 specific examples that must work"
- **Property-based tests**: "Here's a rule that must hold for all 10,000 generated examples"

Both are valuable. Both catch different kinds of bugs. Use both.

## Quick Reference

### When Writing Tests for a Feature

1. **Start with unit tests** (3-5 concrete examples)
   ```kotlin
   @Test
   fun `should parse simple bold`() {
       assertEquals(Bold("text"), parse("*text*"))
   }
   ```

2. **Add property-based tests** (universal rules)
   ```kotlin
   @Test
   fun `parsing should never crash`() = runTest {
       checkAll<String> { input ->
           shouldNotThrowAny { parse(input) }
       }
   }
   ```

3. **When PBT finds a bug**:
   - Add the failing input as a unit test
   - Fix the bug
   - Verify both tests pass

### Common Kotest Patterns

```kotlin
// Test with multiple parameters
checkAll(Arb.int(1..6), Arb.string()) { level, text ->
    // test logic
}

// Configure iterations (default is 1000)
checkAll(iterations = 10000, Arb.string()) { input ->
    // test logic
}

// Use custom generators
checkAll(Arb.asciidocDocument()) { doc ->
    // test logic
}
```

This approach ensures your KMP library is thoroughly tested across all platforms with both concrete examples and universal properties verified.
