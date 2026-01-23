package org.markup.poet.asciidoc.render

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
 * Validates: Requirements 1.5, 2.5, 3.5
 * 
 * @param customCssContent Custom CSS content as a string (takes precedence over customCssPath)
 * @param customCssPath Path to custom CSS file (relative or absolute)
 * @param includeDefaultCss Whether to include default theme CSS (true by default)
 * @param builtInTheme Name of built-in theme to use ("default", "minimal", "dark"), or empty string to use the theme from RenderConfig
 * @param cssVariables Map of CSS variable overrides (e.g., "--mp-color-primary" to "#007acc")
 */
data class CssOptions(
    val customCssContent: String? = null,
    val customCssPath: String? = null,
    val includeDefaultCss: Boolean = true,
    val builtInTheme: String = "",
    val cssVariables: Map<String, String> = emptyMap()
) {
    companion object {
        /**
         * Returns the default CSS options.
         * 
         * Default configuration:
         * - No custom CSS content
         * - No custom CSS file path
         * - Default theme CSS included
         * - Empty built-in theme (uses theme from RenderConfig)
         * - No CSS variable overrides
         * 
         * @return A CssOptions instance with default values
         */
        fun default() = CssOptions()
    }
}
