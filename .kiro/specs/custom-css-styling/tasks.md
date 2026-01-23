# Implementation Plan: Custom CSS Styling

## Overview

This implementation plan breaks down the custom CSS styling feature into incremental coding tasks. Each task builds on previous work, starting with core data models and interfaces, then implementing CSS loading and merging logic, adding built-in themes, enhancing the CLI tool, and finally integrating everything with comprehensive testing.

The implementation follows Kotlin Multiplatform conventions with common code in `commonMain` and platform-specific implementations using expect/actual declarations.

## Tasks

- [x] 1. Create core CSS configuration data models
  - Create `CssOptions` data class in `html-renderer/src/commonMain/kotlin/org/markup/poet/asciidoc/render/CssOptions.kt`
  - Create `CssException` sealed class in `html-renderer/src/commonMain/kotlin/org/markup/poet/asciidoc/render/CssException.kt`
  - Update `RenderConfig` to include `cssOptions: CssOptions` parameter with default value
  - _Requirements: 1.5, 2.5, 3.5_

- [ ]* 1.1 Write unit tests for CssOptions data class
  - Test default values
  - Test various configuration combinations
  - _Requirements: 1.5, 2.5, 3.5_

- [x] 2. Implement FileReader interface and platform-specific implementations
  - [x] 2.1 Create `FileReader` interface in `html-renderer/src/commonMain/kotlin/org/markup/poet/asciidoc/render/FileReader.kt`
    - Define `readFile(path: String): Result<String>` method
    - _Requirements: 2.1, 2.4_
  
  - [x] 2.2 Create expect declaration for `PlatformFileReader` in commonMain
    - Add expect class declaration
    - _Requirements: 2.1_
  
  - [x] 2.3 Implement `PlatformFileReader` for JVM in `html-renderer/src/jvmMain/kotlin/org/markup/poet/asciidoc/render/PlatformFileReader.kt`
    - Use `java.io.File` and `Files.readString()`
    - Handle absolute and relative paths
    - Return Result.failure for file not found or read errors
    - _Requirements: 2.1, 2.2, 2.3, 2.4_
  
  - [x] 2.4 Implement `PlatformFileReader` for Android in `html-renderer/src/androidMain/kotlin/org/markup/poet/asciidoc/render/PlatformFileReader.kt`
    - Use same JVM implementation (Android uses JVM file APIs)
    - _Requirements: 2.1, 2.2, 2.3, 2.4_

- [ ]* 2.5 Write property test for FileReader
  - **Property 3: CSS File Loading**
  - **Validates: Requirements 2.1, 2.4**
  - Generate temporary files with random CSS content
  - Verify content is read correctly
  - Test both absolute and relative paths

- [ ]* 2.6 Write property test for file not found error handling
  - **Property 4: File Not Found Error Handling**
  - **Validates: Requirements 2.2, 8.1**
  - Generate random non-existent file paths
  - Verify error contains the file path

- [x] 3. Implement CssProvider interface and default implementation
  - [x] 3.1 Create `CssProvider` interface in `html-renderer/src/commonMain/kotlin/org/markup/poet/asciidoc/render/CssProvider.kt`
    - Define `provideCss(cssOptions: CssOptions, theme: Theme): Result<String>` method
    - _Requirements: 1.1, 2.1, 3.1_
  
  - [x] 3.2 Implement `DefaultCssProvider` class
    - Implement CSS variable override generation (`:root` block)
    - Implement default theme CSS inclusion logic
    - Implement custom CSS loading (content takes precedence over path)
    - Implement CSS merging with correct order (variables → default → custom)
    - Handle all error cases with appropriate CssException types
    - _Requirements: 1.1, 1.2, 2.1, 2.2, 3.1, 3.2, 3.4, 6.2, 8.1, 8.2_

- [ ]* 3.3 Write property test for CSS merge order
  - **Property 5: CSS Merge Order**
  - **Validates: Requirements 3.1, 3.4**
  - Generate random default and custom CSS
  - Verify custom appears after default in output

- [ ]* 3.4 Write property test for custom CSS only mode
  - **Property 6: Custom CSS Only Mode**
  - **Validates: Requirements 3.2**
  - Generate random custom CSS
  - Disable default CSS
  - Verify only custom CSS present in output

- [ ]* 3.5 Write property test for CSS variable override application
  - **Property 9: CSS Variable Override Application**
  - **Validates: Requirements 6.2**
  - Generate random variable name/value pairs
  - Verify :root declaration appears before theme CSS

- [ ]* 3.6 Write unit tests for DefaultCssProvider
  - Test CSS loading from file path
  - Test CSS loading from content string
  - Test content takes precedence over path
  - Test error handling for missing files
  - _Requirements: 1.1, 2.1, 2.2, 3.1, 3.2_

- [x] 4. Create built-in themes with CSS variables
  - [x] 4.1 Update `DefaultTheme` to use CSS variables
    - Define CSS variables in `:root` block (colors, fonts, spacing)
    - Update all CSS rules to use `var()` references
    - Maintain existing visual appearance
    - _Requirements: 6.1, 6.4, 6.5_
  
  - [x] 4.2 Create `MinimalTheme` class in `html-renderer/src/commonMain/kotlin/org/markup/poet/asciidoc/render/MinimalTheme.kt`
    - Implement Theme interface
    - Define minimal CSS variables
    - Provide basic, clean styling
    - _Requirements: 5.1, 6.4, 6.5_
  
  - [x] 4.3 Create `DarkTheme` class in `html-renderer/src/commonMain/kotlin/org/markup/poet/asciidoc/render/DarkTheme.kt`
    - Implement Theme interface
    - Define dark color scheme CSS variables
    - Provide dark mode styling
    - _Requirements: 5.1, 6.4, 6.5_
  
  - [x] 4.4 Add theme factory method to CssProvider
    - Implement `getBuiltInTheme(themeName: String): Theme?` method
    - Support "default", "minimal", "dark" theme names
    - Return null for invalid theme names
    - _Requirements: 5.1, 5.2_

- [ ]* 4.5 Write property test for built-in theme selection
  - **Property 7: Built-in Theme Selection**
  - **Validates: Requirements 5.2**
  - Test all valid theme names
  - Verify correct CSS returned for each theme

- [ ]* 4.6 Write property test for invalid theme error handling
  - **Property 8: Invalid Theme Error Handling**
  - **Validates: Requirements 5.5**
  - Generate random invalid theme names
  - Verify descriptive error message

- [ ]* 4.7 Write property test for CSS variable naming convention
  - **Property 10: CSS Variable Naming Convention**
  - **Validates: Requirements 6.4**
  - Parse all built-in theme CSS
  - Verify all variables match `--mp-{category}-{property}` pattern

- [ ]* 4.8 Write property test for theme CSS variable usage
  - **Property 11: Theme CSS Variable Usage**
  - **Validates: Requirements 6.5**
  - Parse all built-in theme CSS
  - Verify customizable properties use `var()` references

- [ ]* 4.9 Write unit tests for built-in themes
  - Test each theme produces valid CSS
  - Test theme class name methods
  - Verify CSS variables are defined
  - _Requirements: 5.1, 6.1, 6.4, 6.5_

- [x] 5. Integrate CssProvider with HtmlRenderer
  - [x] 5.1 Update `DefaultHtmlRenderer` to use CssProvider
    - Add CssProvider as constructor parameter (with default)
    - Update `wrapInDocument` method to call `cssProvider.provideCss()`
    - Handle CSS provider errors appropriately
    - Maintain backward compatibility (default behavior unchanged)
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 7.1, 7.4_
  
  - [x] 5.2 Update CSS inclusion logic for different modes
    - INLINE mode: Include CSS in `<style>` tag
    - EXTERNAL mode: Write CSS to file and add `<link>` tag
    - NONE mode: Skip CSS entirely
    - _Requirements: 1.2, 1.3, 1.4_

- [ ]* 5.3 Write property test for custom CSS inclusion
  - **Property 1: Custom CSS Inclusion**
  - **Validates: Requirements 1.1, 1.2**
  - Generate random CSS content
  - Test with INLINE and EXTERNAL modes
  - Verify CSS appears in output

- [ ]* 5.4 Write property test for CSS mode NONE exclusion
  - **Property 2: CSS Mode NONE Exclusion**
  - **Validates: Requirements 1.4**
  - Generate random CSS content
  - Set mode to NONE
  - Verify no CSS in output

- [ ]* 5.5 Write property test for invalid CSS pass-through
  - **Property 14: Invalid CSS Pass-Through**
  - **Validates: Requirements 8.2**
  - Generate syntactically invalid CSS
  - Verify it appears unchanged in output

- [ ]* 5.6 Write property test for CSS error robustness
  - **Property 15: CSS Error Robustness**
  - **Validates: Requirements 8.5**
  - Generate various CSS error conditions
  - Verify Result.failure returned (no crashes)

- [ ]* 5.7 Write unit tests for HtmlRenderer CSS integration
  - Test rendering with custom CSS content
  - Test rendering with custom CSS file
  - Test rendering with CSS variables
  - Test rendering with different themes
  - Test backward compatibility (no custom CSS)
  - _Requirements: 1.1, 1.2, 7.1, 7.4_

- [x] 6. Checkpoint - Ensure all tests pass
  - Run all unit and property tests
  - Verify backward compatibility
  - Ask the user if questions arise

- [x] 7. Enhance CLI tool with CSS flags
  - [x] 7.1 Create `CliOptions` data class in `html-cli/src/commonMain/kotlin/org/markup/poet/html/cli/CliOptions.kt`
    - Add fields for CSS file, theme, no-default-css flag, CSS variables
    - _Requirements: 4.1, 4.2, 4.3, 5.4_
  
  - [x] 7.2 Implement CLI argument parser
    - Parse `--css-file <path>` flag
    - Parse `--no-default-css` flag
    - Parse `--theme <name>` flag
    - Parse `--css-var <variable>=<value>` flag
    - Handle missing required arguments
    - Handle invalid flag combinations
    - _Requirements: 4.1, 4.2, 4.3, 5.4_
  
  - [x] 7.3 Update `main()` function to use new CLI options
    - Create CssOptions from CLI arguments
    - Create RenderConfig with CSS options
    - Handle CSS-related errors (file not found, invalid theme)
    - Display appropriate error messages
    - _Requirements: 4.1, 4.2, 4.3, 4.4_
  
  - [x] 7.4 Update help text with CSS flags documentation
    - Document `--css-file` flag
    - Document `--no-default-css` flag
    - Document `--theme` flag
    - Document `--css-var` flag
    - Provide usage examples
    - _Requirements: 4.5_

- [ ]* 7.5 Write property test for CLI CSS file loading
  - **Property 12: CLI CSS File Loading**
  - **Validates: Requirements 4.1**
  - Generate random CSS files
  - Test CLI with --css-file flag
  - Verify CSS in output

- [ ]* 7.6 Write property test for CLI file not found error
  - **Property 13: CLI File Not Found Error**
  - **Validates: Requirements 4.4**
  - Generate random non-existent paths
  - Test CLI with --css-file flag
  - Verify error message and exit code

- [ ]* 7.7 Write unit tests for CLI argument parsing
  - Test parsing each flag individually
  - Test parsing multiple flags together
  - Test missing required arguments
  - Test invalid flag values
  - _Requirements: 4.1, 4.2, 4.3, 5.4_

- [ ]* 7.8 Write unit tests for CLI error handling
  - Test CSS file not found error
  - Test invalid theme name error
  - Test missing CSS file path
  - Verify error messages and exit codes
  - _Requirements: 4.4, 5.5_

- [ ]* 7.9 Write integration tests for CLI
  - Test end-to-end rendering with custom CSS file
  - Test rendering with different themes
  - Test rendering with CSS variables
  - Test rendering with --no-default-css flag
  - Verify output files contain correct CSS
  - _Requirements: 4.1, 4.2, 4.3, 5.4_

- [x] 8. Backward compatibility verification
  - [x] 8.1 Write backward compatibility tests
    - Test default RenderConfig produces same output as before
    - Test existing Theme interface works unchanged
    - Test existing CLI usage works unchanged
    - Compare output with previous version
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [x] 9. Final checkpoint - Ensure all tests pass
  - Run complete test suite across all platforms
  - Verify all property tests pass (minimum 100 iterations each)
  - Verify all unit tests pass
  - Verify all integration tests pass
  - Verify backward compatibility
  - Ask the user if questions arise

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Property tests validate universal correctness properties
- Unit tests validate specific examples and edge cases
- Integration tests verify end-to-end workflows
- Checkpoints ensure incremental validation
- All code follows Kotlin Multiplatform conventions
- Platform-specific implementations use expect/actual declarations
- Package structure: `org.markup.poet.asciidoc.render` for renderer components
- Package structure: `org.markup.poet.html.cli` for CLI components
