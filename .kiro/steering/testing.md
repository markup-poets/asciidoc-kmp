# Unit Testing in Kotlin Multiplatform

## Testing Library
- **kotlin-test**: Official Kotlin testing library for multiplatform projects
- Provides common testing APIs that work across all platforms
- Automatically maps to platform-specific test frameworks (JUnit on JVM/Android, XCTest on iOS)

## Test Structure Organization

### Source Set Hierarchy
```
library/src/
├── commonTest/kotlin/          # Shared tests for common code
├── androidHostTest/kotlin/     # Android-specific tests (host tests)
├── jvmTest/kotlin/            # JVM-specific tests
├── iosTest/kotlin/            # iOS-specific tests
└── linuxX64Test/kotlin/       # Linux-specific tests
```

### Test Naming Conventions
- **Common tests**: `{Feature}Test.kt` (e.g., `FibiTest.kt`)
- **Platform tests**: `{Platform}{Feature}Test.kt` (e.g., `AndroidFibiTest.kt`)
- **Test methods**: Use descriptive names with backticks for readability

## Core Testing APIs

### Essential Imports
```kotlin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertFailsWith
```

### Basic Test Structure
```kotlin
class MyFeatureTest {
    
    @Test
    fun `should return expected result when given valid input`() {
        // Arrange
        val input = "test"
        
        // Act
        val result = myFunction(input)
        
        // Assert
        assertEquals("expected", result)
    }
}
```

## Common Assertion Patterns

### Value Assertions
```kotlin
@Test
fun `test basic assertions`() {
    assertEquals(expected, actual)
    assertTrue(condition)
    assertFalse(condition)
    assertNull(nullableValue)
    assertNotNull(nonNullValue)
}
```

### Exception Testing
```kotlin
@Test
fun `should throw exception for invalid input`() {
    assertFailsWith<IllegalArgumentException> {
        functionThatShouldThrow()
    }
}
```

### Collection Testing
```kotlin
@Test
fun `test collection operations`() {
    val list = listOf(1, 2, 3)
    assertEquals(3, list.size)
    assertTrue(list.contains(2))
    assertEquals(listOf(1, 2, 3), list)
}
```

## Platform-Specific Testing

### Testing expect/actual Declarations
```kotlin
// commonTest - Test the common interface
@Test
fun `should use platform-specific values`() {
    val result = generateFibi().take(3).toList()
    assertEquals(firstElement + secondElement, result[2])
}

// Platform-specific test - Test actual implementation
@Test
fun `android should use specific starting values`() {
    assertEquals(1, firstElement)
    assertEquals(2, secondElement)
}
```

### Android Host Tests vs Instrumented Tests
- **Host tests** (`androidHostTest`): Run on JVM, faster execution
- **Instrumented tests** (`androidInstrumentedTest`): Run on device/emulator, access to Android APIs
- Use host tests for business logic, instrumented tests for Android-specific functionality

## Test Organization Best Practices

### Test Class Structure
```kotlin
class FeatureTest {
    
    // Setup if needed
    private val testSubject = MyClass()
    
    @Test
    fun `should handle normal case`() { /* ... */ }
    
    @Test
    fun `should handle edge case`() { /* ... */ }
    
    @Test
    fun `should handle error case`() { /* ... */ }
}
```

### Grouping Related Tests
- One test class per feature/component
- Group related test methods within the same class
- Use descriptive test method names that explain the scenario

## Running Tests

### Gradle Commands
```bash
# All tests across all platforms
./gradlew test

# Platform-specific tests
./gradlew :library:testDebugUnitTest    # Android
./gradlew :library:jvmTest              # JVM
./gradlew :library:iosX64Test           # iOS x64
./gradlew :library:linuxX64Test         # Linux

# Common tests only
./gradlew :library:commonTest
```

### IDE Integration
- Tests can be run directly from IDE
- Use platform-specific run configurations for targeted testing
- Common tests run on all configured platforms

## Testing Guidelines

### What to Test
- **Public APIs**: All public functions and properties
- **Business logic**: Core functionality and algorithms
- **Edge cases**: Boundary conditions and error scenarios
- **Platform differences**: Verify expect/actual implementations work correctly

### Test Quality
- **Arrange-Act-Assert**: Structure tests clearly
- **Single responsibility**: One assertion per test when possible
- **Descriptive names**: Test names should explain the scenario
- **Independent tests**: Tests should not depend on each other
- **Fast execution**: Keep tests lightweight and quick

### Common Patterns
```kotlin
// Testing sequences/flows
@Test
fun `should generate correct fibonacci sequence`() {
    val result = generateFibi().take(5).toList()
    assertEquals(listOf(1, 2, 3, 5, 8), result)
}

// Testing with different inputs
@Test
fun `should handle empty input`() {
    val result = processInput("")
    assertTrue(result.isEmpty())
}

// Testing platform-specific behavior
@Test
fun `should use platform-appropriate implementation`() {
    val result = getPlatformSpecificValue()
    assertNotNull(result)
    assertTrue(result.isNotEmpty())
}
```

## Debugging Tests
- Use descriptive assertion messages: `assertEquals(expected, actual, "Custom error message")`
- Add logging or print statements for complex test scenarios
- Use IDE debugger to step through test execution
- Check platform-specific test outputs for detailed error information