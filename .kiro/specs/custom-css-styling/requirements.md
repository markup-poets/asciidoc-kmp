# Requirements Document

## Introduction

This document specifies requirements for enhancing the HTML renderer with custom CSS styling capabilities. The feature will allow users to provide their own CSS styles when rendering AsciiDoc to HTML, both through the library API and the CLI tool. This builds upon the existing CSS infrastructure (CssMode, Theme interface, and default CSS) to provide flexible styling options while maintaining backward compatibility.

## Glossary

- **HTML_Renderer**: The component that converts AsciiDoc AST to HTML output
- **CSS_Provider**: Component responsible for supplying CSS content to the renderer
- **Theme**: An interface that generates CSS class names and provides CSS content
- **RenderConfig**: Configuration object for HTML rendering options
- **CLI_Tool**: Command-line interface for the HTML renderer (html-cli module)
- **Custom_CSS**: User-provided CSS content, either as a string or loaded from a file
- **Default_CSS**: The built-in CSS styles provided by the library
- **CSS_Mode**: Enum controlling how CSS is included (NONE, INLINE, EXTERNAL)
- **Merged_CSS**: Combination of default theme CSS and custom user CSS

## Requirements

### Requirement 1: Custom CSS Content Provision

**User Story:** As a library user, I want to provide custom CSS content directly as a string, so that I can style HTML output without creating external files.

#### Acceptance Criteria

1. WHEN a user provides custom CSS as a string, THE HTML_Renderer SHALL include that CSS in the output
2. WHEN custom CSS is provided with INLINE mode, THE HTML_Renderer SHALL embed the custom CSS in a `<style>` tag
3. WHEN custom CSS is provided with EXTERNAL mode, THE HTML_Renderer SHALL write the custom CSS to the specified external file
4. WHEN custom CSS is provided with NONE mode, THE HTML_Renderer SHALL ignore the custom CSS
5. THE RenderConfig SHALL accept an optional custom CSS string parameter

### Requirement 2: CSS File Loading

**User Story:** As a library user, I want to load custom CSS from a file path, so that I can maintain my styles in separate files.

#### Acceptance Criteria

1. WHEN a user provides a CSS file path, THE CSS_Provider SHALL read the file contents
2. WHEN the CSS file does not exist, THE CSS_Provider SHALL return a descriptive error
3. WHEN the CSS file cannot be read, THE CSS_Provider SHALL return a descriptive error
4. THE CSS_Provider SHALL support both absolute and relative file paths
5. THE RenderConfig SHALL accept an optional CSS file path parameter

### Requirement 3: CSS Merging Strategy

**User Story:** As a library user, I want to merge my custom CSS with the default theme CSS, so that I can override specific styles while keeping the base styling.

#### Acceptance Criteria

1. WHEN both default theme CSS and custom CSS are provided, THE CSS_Provider SHALL merge them with custom CSS taking precedence
2. WHEN only custom CSS is provided, THE CSS_Provider SHALL use only the custom CSS
3. WHEN only default theme CSS is provided, THE CSS_Provider SHALL use only the default CSS
4. THE CSS_Provider SHALL append custom CSS after default CSS to ensure proper cascade order
5. THE RenderConfig SHALL provide an option to disable default CSS when using custom CSS

### Requirement 4: CLI Custom CSS Support

**User Story:** As a CLI user, I want to specify custom CSS files via command-line flags, so that I can style HTML output without writing code.

#### Acceptance Criteria

1. WHEN a user provides `--css-file <path>` flag, THE CLI_Tool SHALL load CSS from the specified file
2. WHEN a user provides `--no-default-css` flag, THE CLI_Tool SHALL disable default theme CSS
3. WHEN both `--css-file` and default CSS are enabled, THE CLI_Tool SHALL merge them appropriately
4. WHEN the specified CSS file does not exist, THE CLI_Tool SHALL display an error message and exit
5. THE CLI_Tool SHALL display help text documenting the CSS-related flags

### Requirement 5: Built-in Theme Selection

**User Story:** As a user, I want to choose from multiple built-in themes, so that I can quickly apply different visual styles without writing CSS.

#### Acceptance Criteria

1. THE HTML_Renderer SHALL provide at least three built-in themes (default, minimal, dark)
2. WHEN a user selects a built-in theme, THE HTML_Renderer SHALL use that theme's CSS
3. THE RenderConfig SHALL accept a theme selection parameter
4. THE CLI_Tool SHALL provide a `--theme <name>` flag for theme selection
5. WHEN an invalid theme name is provided, THE HTML_Renderer SHALL return a descriptive error

### Requirement 6: CSS Variables Support

**User Story:** As a user, I want to customize theme colors and spacing using CSS variables, so that I can easily adjust the visual appearance without writing complete stylesheets.

#### Acceptance Criteria

1. THE Default_CSS SHALL define CSS variables for primary colors, fonts, and spacing
2. WHEN a user provides custom CSS with variable overrides, THE HTML_Renderer SHALL apply those overrides
3. THE HTML_Renderer SHALL document all available CSS variables
4. THE CSS variables SHALL follow a consistent naming convention (e.g., `--mp-color-primary`)
5. THE built-in themes SHALL use CSS variables for all customizable properties

### Requirement 7: Backward Compatibility

**User Story:** As an existing library user, I want the new CSS features to work without breaking my current code, so that I can upgrade without modifications.

#### Acceptance Criteria

1. WHEN no custom CSS is provided, THE HTML_Renderer SHALL behave identically to the previous version
2. THE existing RenderConfig constructor SHALL remain functional with default values
3. THE existing Theme interface SHALL continue to work without modification
4. WHEN using the existing API, THE HTML_Renderer SHALL produce the same output as before
5. THE CLI_Tool SHALL maintain all existing command-line flags and behavior

### Requirement 8: CSS Validation and Error Handling

**User Story:** As a user, I want clear error messages when CSS loading or merging fails, so that I can quickly identify and fix configuration issues.

#### Acceptance Criteria

1. WHEN CSS file loading fails, THE CSS_Provider SHALL return an error with the file path and reason
2. WHEN CSS content is invalid, THE HTML_Renderer SHALL include the CSS as-is without validation
3. WHEN CSS merging encounters conflicts, THE CSS_Provider SHALL log a warning but continue processing
4. THE error messages SHALL include actionable information for resolving the issue
5. THE HTML_Renderer SHALL never crash due to CSS-related errors
