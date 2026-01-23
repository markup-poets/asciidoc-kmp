# Design Document: Custom CSS Styling

## Overview

This design enhances the HTML renderer with flexible custom CSS styling capabilities. Users will be able to provide their own CSS styles through multiple mechanisms: direct string content, file paths, or by selecting from built-in themes. The design extends the existing CSS infrastructure (`CssMode`, `Theme`, `RenderConfig`) while maintaining full backward compatibility.

The feature supports three primary use cases:
1. **Library API users** who want programmatic control over CSS styling
2. **CLI users** who want to specify CSS files via command-line flags
3. **Quick styling** through built-in themes and CSS variables

The implementation follows the existing architecture patterns and integrates seamlessly with the current rendering pipeline.

## Architecture

### Component Overview

```
┌─────────────────────────────────────────────────────────────┐
│                      User Interface                          │
├──────────────────────┬──────────────────────────────────────┤
│   Library API        │         CLI Tool                      │
│   - RenderConfig     │         - Command flags               │
│   - CssOptions       │         - File arguments              │
└──────────────────────┴──────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    CSS Provider                              │
│  - Loads CSS from files                                      │
│  - Merges default + custom CSS                               │
│  - Manages built-in themes                                   │
│  - Handles CSS variables                                     │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                  HTML Renderer                               │
│  - Includes CSS based on CssMode                             │
│  - Applies CSS to output                                     │
└─────────────────────────────────────────────────────────────┘
```

### Data Flow

1. **Configuration Phase**: User specifies CSS options (file path, content, theme, variables)
2. **Loading Phase**: CssProvider loads and validates CSS content
3. **Merging Phase**: CssProvider combines default theme CSS with custom CSS
4. **Rendering Phase**: HtmlRenderer includes CSS in output based on CssMode
5. **Output Phase**: Final HTML with embedded or linked CSS

## Components and Interfaces

### 1. CssOptions Data Class

New configuration class for CSS-related settings:

```kotlin
/**
 * Configuration for CSS styling in HTML output.
 * 
 * Provides multiple ways to customize CSS:
 * - Custom CSS content directly as a string
 * - Custom CSS loaded from a file path
 * - Built-in theme selection
 * - CSS variable overrides
 * - Control over default CSS inclusion
 * 
 * @param customCssContent Custom CSS content as a string (takes precedence over customCssPath)
 * @param customCssPath Path to custom CSS file (relative or absolute)
 * @param includeDefaultCss Whether to include default theme CSS (true by default)
 * @param builtInTheme Name of built-in theme to use ("default", "minimal", "dark")
 * @param cssVariables Map of CSS variable overrides (e.g., "--mp-color-primary" to "#007acc")
 */
data class CssOptions(
    val customCssContent: String? = null,
    val customCssPath: String? = null,
    val includeDefaultCss: Boolean = true,
    val builtInTheme: String = "default",
    val cssVariables: Map<String, String> = emptyMap()
) {
    companion object {
        fun default() = CssOptions()
    }
}
```

### 2. Enhanced RenderConfig

Update `RenderConfig` to include `CssOptions`:

```kotlin
data class RenderConfig(
    val outputOptions: OutputOptions = OutputOptions.default(),
    val theme: Theme = Theme.default(),
    val cssOptions: CssOptions = CssOptions.default(), // NEW
    val customRenderers: Map<String, CustomRenderer> = emptyMap(),
    val attributeHandlers: Map<String, AttributeHandler> = emptyMap(),
    val documentTemplate: DocumentTemplate? = null
) {
    companion object {
        fun default() = RenderConfig()
    }
}
```

### 3. CssProvider Interface

New component for CSS loading and merging:

```kotlin
/**
 * Provides CSS content for HTML rendering.
 * 
 * Handles loading CSS from various sources, merging default and custom CSS,
 * and applying CSS variable overrides.
 */
interface CssProvider {
    /**
     * Loads and prepares CSS content based on configuration.
     * 
     * @param cssOptions CSS configuration options
     * @param theme Theme for default CSS generation
     * @return Result containing final CSS content or error
     */
    fun provideCss(cssOptions: CssOptions, theme: Theme): Result<String>
}

/**
 * Default implementation of CssProvider.
 * 
 * Handles:
 * - Loading CSS from file paths
 * - Merging default theme CSS with custom CSS
 * - Applying CSS variable overrides
 * - Built-in theme selection
 */
class DefaultCssProvider(
    private val fileReader: FileReader
) : CssProvider {
    
    override fun provideCss(cssOptions: CssOptions, theme: Theme): Result<String> {
        return try {
            val cssBuilder = StringBuilder()
            
            // 1. Add CSS variable overrides if any
            if (cssOptions.cssVariables.isNotEmpty()) {
                cssBuilder.append(":root {\n")
                cssOptions.cssVariables.forEach { (variable, value) ->
                    cssBuilder.append("  $variable: $value;\n")
                }
                cssBuilder.append("}\n\n")
            }
            
            // 2. Add default theme CSS if enabled
            if (cssOptions.includeDefaultCss) {
                val themeToUse = getBuiltInTheme(cssOptions.builtInTheme) ?: theme
                cssBuilder.append(themeToUse.getCss())
                cssBuilder.append("\n\n")
            }
            
            // 3. Add custom CSS (content takes precedence over path)
            val customCss = when {
                cssOptions.customCssContent != null -> cssOptions.customCssContent
                cssOptions.customCssPath != null -> loadCssFromFile(cssOptions.customCssPath)
                else -> null
            }
            
            if (customCss != null) {
                cssBuilder.append("/* Custom CSS */\n")
                cssBuilder.append(customCss)
            }
            
            Result.success(cssBuilder.toString())
        } catch (e: Exception) {
            Result.failure(CssException.LoadingFailure(e.message ?: "Unknown error"))
        }
    }
    
    private fun loadCssFromFile(path: String): String {
        return fileReader.readFile(path).getOrElse { error ->
            throw CssException.FileNotFound(path, error.message ?: "File not found")
        }
    }
    
    private fun getBuiltInTheme(themeName: String): Theme? {
        return when (themeName.lowercase()) {
            "default" -> DefaultTheme()
            "minimal" -> MinimalTheme()
            "dark" -> DarkTheme()
            else -> null
        }
    }
}
```

### 4. FileReader Interface

Platform-agnostic file reading:

```kotlin
/**
 * Platform-agnostic file reading interface.
 * 
 * Implementations handle platform-specific file I/O.
 */
interface FileReader {
    /**
     * Reads file content as a string.
     * 
     * @param path File path (relative or absolute)
     * @return Result containing file content or error
     */
    fun readFile(path: String): Result<String>
}

/**
 * expect/actual implementation for platform-specific file reading.
 */
expect class PlatformFileReader() : FileReader
```

### 5. Built-in Themes

Three built-in themes with CSS variables:

```kotlin
/**
 * Minimal theme with basic styling and CSS variables.
 */
class MinimalTheme : Theme {
    override fun getCss(): String {
        return """
            :root {
                --mp-color-text: #333;
                --mp-color-background: #fff;
                --mp-color-border: #ddd;
                --mp-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
                --mp-font-size-base: 16px;
                --mp-line-height: 1.6;
                --mp-spacing-unit: 1em;
            }
            
            body {
                font-family: var(--mp-font-family);
                font-size: var(--mp-font-size-base);
                line-height: var(--mp-line-height);
                color: var(--mp-color-text);
                background: var(--mp-color-background);
            }
            
            /* Minimal heading styles */
            .heading { margin: var(--mp-spacing-unit) 0; }
            .heading-1 { font-size: 2em; }
            .heading-2 { font-size: 1.5em; }
            /* ... other minimal styles ... */
        """.trimIndent()
    }
    
    // ... other Theme methods ...
}

/**
 * Dark theme with dark color scheme.
 */
class DarkTheme : Theme {
    override fun getCss(): String {
        return """
            :root {
                --mp-color-text: #e0e0e0;
                --mp-color-background: #1e1e1e;
                --mp-color-border: #444;
                --mp-color-code-bg: #2d2d2d;
                --mp-color-link: #4fc3f7;
                --mp-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
            }
            
            body {
                font-family: var(--mp-font-family);
                color: var(--mp-color-text);
                background: var(--mp-color-background);
            }
            
            /* Dark theme styles */
            .code-block {
                background: var(--mp-color-code-bg);
                border-color: var(--mp-color-border);
            }
            /* ... other dark theme styles ... */
        """.trimIndent()
    }
    
    // ... other Theme methods ...
}
```

### 6. Enhanced DefaultTheme

Update `DefaultTheme` to use CSS variables:

```kotlin
class DefaultTheme : Theme {
    override fun getCss(): String {
        return """
            :root {
                --mp-color-primary: #007acc;
                --mp-color-text: #333;
                --mp-color-background: #fff;
                --mp-color-border: #ddd;
                --mp-color-code-bg: #f5f5f5;
                --mp-color-note: #3498db;
                --mp-color-tip: #2ecc71;
                --mp-color-warning: #f39c12;
                --mp-color-important: #e74c3c;
                --mp-color-caution: #e67e22;
                --mp-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
                --mp-font-size-base: 16px;
                --mp-line-height: 1.6;
                --mp-spacing-unit: 1em;
            }
            
            /* Headings */
            .heading {
                margin: var(--mp-spacing-unit) 0 0.5em;
                font-weight: bold;
                line-height: 1.2;
            }
            /* ... rest of CSS using variables ... */
        """.trimIndent()
    }
    
    // ... other Theme methods unchanged ...
}
```

### 7. CLI Enhancements

Update CLI tool to support CSS flags:

```kotlin
data class CliOptions(
    val inputFile: String,
    val outputFile: String,
    val cssFile: String? = null,
    val noDefaultCss: Boolean = false,
    val theme: String = "default",
    val cssVariables: Map<String, String> = emptyMap()
)

fun parseArgs(args: Array<String>): CliOptions {
    var inputFile: String? = null
    var outputFile: String? = null
    var cssFile: String? = null
    var noDefaultCss = false
    var theme = "default"
    val cssVariables = mutableMapOf<String, String>()
    
    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--css-file" -> {
                cssFile = args.getOrNull(++i)
                    ?: throw IllegalArgumentException("--css-file requires a path argument")
            }
            "--no-default-css" -> {
                noDefaultCss = true
            }
            "--theme" -> {
                theme = args.getOrNull(++i)
                    ?: throw IllegalArgumentException("--theme requires a theme name")
            }
            "--css-var" -> {
                val varDef = args.getOrNull(++i)
                    ?: throw IllegalArgumentException("--css-var requires variable=value")
                val parts = varDef.split("=", limit = 2)
                if (parts.size != 2) {
                    throw IllegalArgumentException("--css-var format: variable=value")
                }
                cssVariables[parts[0]] = parts[1]
            }
            else -> {
                if (inputFile == null) {
                    inputFile = args[i]
                } else if (outputFile == null) {
                    outputFile = args[i]
                }
            }
        }
        i++
    }
    
    return CliOptions(
        inputFile = inputFile ?: throw IllegalArgumentException("Input file required"),
        outputFile = outputFile ?: inputFile.removeSuffix(".adoc") + ".html",
        cssFile = cssFile,
        noDefaultCss = noDefaultCss,
        theme = theme,
        cssVariables = cssVariables
    )
}
```

### 8. CSS Exception Types

```kotlin
sealed class CssException(message: String) : Exception(message) {
    data class FileNotFound(val path: String, val reason: String) :
        CssException("CSS file not found: $path - $reason")
    
    data class LoadingFailure(val reason: String) :
        CssException("Failed to load CSS: $reason")
    
    data class InvalidTheme(val themeName: String) :
        CssException("Invalid theme name: $themeName. Available: default, minimal, dark")
}
```

## Data Models

### CssOptions

- `customCssContent: String?` - Direct CSS content
- `customCssPath: String?` - Path to CSS file
- `includeDefaultCss: Boolean` - Whether to include default theme CSS
- `builtInTheme: String` - Name of built-in theme ("default", "minimal", "dark")
- `cssVariables: Map<String, String>` - CSS variable overrides

### CliOptions

- `inputFile: String` - Input AsciiDoc file path
- `outputFile: String` - Output HTML file path
- `cssFile: String?` - Custom CSS file path
- `noDefaultCss: Boolean` - Disable default CSS
- `theme: String` - Built-in theme name
- `cssVariables: Map<String, String>` - CSS variable overrides

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*


### Property 1: Custom CSS Inclusion

*For any* custom CSS content string and CSS mode (INLINE or EXTERNAL), when provided to the HTML renderer, the output SHALL contain that custom CSS content in the appropriate location.

**Validates: Requirements 1.1, 1.2**

### Property 2: CSS Mode NONE Exclusion

*For any* custom CSS content, when CSS mode is set to NONE, the rendered HTML output SHALL NOT contain any CSS content (neither custom nor default).

**Validates: Requirements 1.4**

### Property 3: CSS File Loading

*For any* valid file path containing CSS content, when provided to the CSS provider, the provider SHALL successfully read and return the file contents.

**Validates: Requirements 2.1, 2.4**

### Property 4: File Not Found Error Handling

*For any* non-existent file path, when provided to the CSS provider, the provider SHALL return a descriptive error containing the file path.

**Validates: Requirements 2.2, 8.1**

### Property 5: CSS Merge Order

*For any* default theme CSS and custom CSS content, when both are enabled, the final CSS output SHALL contain the custom CSS after the default CSS to ensure proper cascade precedence.

**Validates: Requirements 3.1, 3.4**

### Property 6: Custom CSS Only Mode

*For any* custom CSS content, when default CSS is disabled (includeDefaultCss = false), the final CSS output SHALL contain only the custom CSS and no default theme CSS.

**Validates: Requirements 3.2**

### Property 7: Built-in Theme Selection

*For any* valid built-in theme name ("default", "minimal", "dark"), when selected, the CSS provider SHALL return the CSS content specific to that theme.

**Validates: Requirements 5.2**

### Property 8: Invalid Theme Error Handling

*For any* invalid theme name (not "default", "minimal", or "dark"), when provided to the CSS provider, it SHALL return a descriptive error indicating the invalid theme name.

**Validates: Requirements 5.5**

### Property 9: CSS Variable Override Application

*For any* CSS variable name and value pair, when provided in cssVariables map, the final CSS output SHALL contain a :root declaration with that variable assignment appearing before any theme CSS.

**Validates: Requirements 6.2**

### Property 10: CSS Variable Naming Convention

*For all* CSS variables defined in built-in themes, the variable names SHALL follow the pattern `--mp-{category}-{property}` where category is one of (color, font, spacing, line-height).

**Validates: Requirements 6.4**

### Property 11: Theme CSS Variable Usage

*For all* built-in themes, customizable properties (colors, fonts, spacing) SHALL use CSS variable references (var(--mp-*)) rather than hardcoded values.

**Validates: Requirements 6.5**

### Property 12: CLI CSS File Loading

*For any* valid CSS file path provided via --css-file flag, the CLI tool SHALL load the CSS content and include it in the rendered HTML output.

**Validates: Requirements 4.1**

### Property 13: CLI File Not Found Error

*For any* non-existent file path provided via --css-file flag, the CLI tool SHALL display an error message containing the file path and exit with a non-zero status code.

**Validates: Requirements 4.4**

### Property 14: Invalid CSS Pass-Through

*For any* syntactically invalid CSS content, the HTML renderer SHALL include the CSS as-is in the output without validation or modification.

**Validates: Requirements 8.2**

### Property 15: CSS Error Robustness

*For any* CSS-related error condition (file not found, invalid theme, etc.), the HTML renderer SHALL return a Result.failure with a descriptive exception rather than crashing.

**Validates: Requirements 8.5**

## Error Handling

### CSS Loading Errors

**File Not Found**:
- Return `Result.failure(CssException.FileNotFound(path, reason))`
- Include the attempted file path in the error message
- Provide actionable guidance (e.g., "Check that the file exists and path is correct")

**File Read Errors**:
- Return `Result.failure(CssException.LoadingFailure(reason))`
- Include the underlying I/O error message
- Handle platform-specific error conditions gracefully

### Configuration Errors

**Invalid Theme Name**:
- Return `Result.failure(CssException.InvalidTheme(themeName))`
- List available theme names in the error message
- Suggest closest matching theme name if possible

**Invalid CSS Mode**:
- Validate CSS mode during configuration
- Return `Result.failure(RenderException.InvalidConfiguration(...))`
- Existing validation in DefaultHtmlRenderer handles this

### CLI Error Handling

**Missing Required Arguments**:
- Display error message to stderr
- Show usage information
- Exit with status code 1

**CSS File Not Found**:
- Display error: "Error: CSS file not found: {path}"
- Suggest checking the file path
- Exit with status code 1

**Invalid Theme Name**:
- Display error: "Error: Invalid theme '{name}'. Available themes: default, minimal, dark"
- Exit with status code 1

### Error Recovery

**Partial CSS Loading**:
- If custom CSS fails to load but default CSS is available, use default CSS only
- Log warning about custom CSS failure
- Continue rendering with available CSS

**CSS Variable Errors**:
- Invalid variable names are included as-is (CSS will ignore them)
- No validation of variable values (CSS handles invalid values gracefully)
- Continue rendering with provided variables

## Testing Strategy

### Unit Tests

Unit tests will verify specific examples and edge cases:

**CSS Loading**:
- Load CSS from a valid file path
- Handle non-existent file paths
- Handle empty CSS files
- Handle CSS files with special characters

**CSS Merging**:
- Merge default and custom CSS in correct order
- Custom CSS only (no default)
- Default CSS only (no custom)
- CSS with variable overrides

**Built-in Themes**:
- Each theme produces valid CSS
- Theme selection works correctly
- Invalid theme names produce errors

**CLI Argument Parsing**:
- Parse --css-file flag correctly
- Parse --no-default-css flag
- Parse --theme flag
- Parse --css-var flag
- Handle missing arguments
- Handle invalid flag combinations

**Backward Compatibility**:
- Default RenderConfig produces same output as before
- Existing Theme interface works unchanged
- Existing CLI usage works unchanged

### Property-Based Tests

Property-based tests will verify universal properties across all inputs using Kotest. Each test will run a minimum of 100 iterations.

**Property 1: Custom CSS Inclusion**
- Generate random CSS content strings
- Test with INLINE and EXTERNAL modes
- Verify CSS appears in output
- Tag: **Feature: custom-css-styling, Property 1: Custom CSS content appears in output**

**Property 2: CSS Mode NONE Exclusion**
- Generate random CSS content
- Set mode to NONE
- Verify no CSS in output
- Tag: **Feature: custom-css-styling, Property 2: NONE mode excludes all CSS**

**Property 3: CSS File Loading**
- Create temporary files with random CSS content
- Load via CSS provider
- Verify content matches
- Tag: **Feature: custom-css-styling, Property 3: File loading preserves content**

**Property 4: File Not Found Error Handling**
- Generate random non-existent file paths
- Verify error contains path
- Tag: **Feature: custom-css-styling, Property 4: File not found errors are descriptive**

**Property 5: CSS Merge Order**
- Generate random default and custom CSS
- Verify custom appears after default
- Tag: **Feature: custom-css-styling, Property 5: Custom CSS follows default CSS**

**Property 6: Custom CSS Only Mode**
- Generate random custom CSS
- Disable default CSS
- Verify only custom CSS present
- Tag: **Feature: custom-css-styling, Property 6: Custom-only mode excludes default**

**Property 7: Built-in Theme Selection**
- Test all valid theme names
- Verify correct CSS returned
- Tag: **Feature: custom-css-styling, Property 7: Theme selection returns correct CSS**

**Property 8: Invalid Theme Error Handling**
- Generate random invalid theme names
- Verify error message
- Tag: **Feature: custom-css-styling, Property 8: Invalid themes produce errors**

**Property 9: CSS Variable Override Application**
- Generate random variable name/value pairs
- Verify :root declaration in output
- Tag: **Feature: custom-css-styling, Property 9: Variable overrides appear in :root**

**Property 10: CSS Variable Naming Convention**
- Parse all built-in theme CSS
- Verify all variables match pattern
- Tag: **Feature: custom-css-styling, Property 10: Variables follow naming convention**

**Property 11: Theme CSS Variable Usage**
- Parse all built-in theme CSS
- Verify customizable properties use var()
- Tag: **Feature: custom-css-styling, Property 11: Themes use CSS variables**

**Property 12: CLI CSS File Loading**
- Generate random CSS files
- Test CLI with --css-file flag
- Verify CSS in output
- Tag: **Feature: custom-css-styling, Property 12: CLI loads CSS files correctly**

**Property 13: CLI File Not Found Error**
- Generate random non-existent paths
- Test CLI with --css-file flag
- Verify error message and exit code
- Tag: **Feature: custom-css-styling, Property 13: CLI handles missing files**

**Property 14: Invalid CSS Pass-Through**
- Generate syntactically invalid CSS
- Verify it appears unchanged in output
- Tag: **Feature: custom-css-styling, Property 14: Invalid CSS passes through**

**Property 15: CSS Error Robustness**
- Generate various error conditions
- Verify Result.failure returned (no crashes)
- Tag: **Feature: custom-css-styling, Property 15: CSS errors don't crash renderer**

### Integration Tests

Integration tests will verify end-to-end workflows:

**Library API Integration**:
- Create RenderConfig with custom CSS
- Render document
- Verify CSS in output

**CLI Integration**:
- Run CLI with various CSS flags
- Verify output files contain correct CSS
- Verify error handling

**Theme Integration**:
- Render with each built-in theme
- Verify visual consistency
- Verify CSS variable overrides work

### Test Configuration

- **Property test iterations**: Minimum 100 per test
- **Test framework**: kotlin-test for unit tests, Kotest for property-based tests
- **Platform coverage**: All tests run on commonTest (JVM, Android, iOS, Linux)
- **File I/O mocking**: Use test doubles for FileReader in unit tests
- **Temporary files**: Create/cleanup temp files for integration tests

## Implementation Notes

### Platform-Specific Considerations

**File Reading**:
- JVM: Use `java.io.File` and `Files.readString()`
- Android: Use `java.io.File` (same as JVM)
- iOS: Use `NSFileManager` and `NSString`
- Linux: Use `fopen` and `fread` via C interop

**Path Handling**:
- Support both forward and backward slashes on Windows
- Resolve relative paths relative to current working directory
- Handle home directory expansion (~/) on Unix-like systems

### Performance Considerations

**CSS Caching**:
- Cache loaded CSS file contents to avoid repeated I/O
- Cache built-in theme CSS (already in memory)
- No caching needed for custom CSS strings (already in memory)

**CSS Merging**:
- Use StringBuilder for efficient string concatenation
- Minimize string allocations during merge
- Pre-allocate buffer size based on input sizes

### Security Considerations

**File Path Validation**:
- No path traversal validation needed (user controls their own filesystem)
- Document that CSS files are read as-is without sanitization
- CSS content is not validated or sanitized (browser handles CSS security)

**CSS Content**:
- No XSS risk (CSS cannot execute JavaScript in modern browsers)
- No validation of CSS syntax (browser handles invalid CSS gracefully)
- User-provided CSS is their responsibility

### Backward Compatibility

**API Compatibility**:
- New `cssOptions` parameter in RenderConfig has default value
- Existing code continues to work without modification
- Theme interface unchanged (getCss() method already exists)

**CLI Compatibility**:
- All new flags are optional
- Existing CLI usage works unchanged
- New flags follow existing naming conventions

**Output Compatibility**:
- Default behavior produces identical output
- CSS structure unchanged (same <style> tag placement)
- Class names unchanged (Theme interface unchanged)
