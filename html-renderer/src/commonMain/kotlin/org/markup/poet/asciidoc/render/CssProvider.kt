package org.markup.poet.asciidoc.render

/**
 * Provides CSS content for HTML rendering.
 * 
 * Handles loading CSS from various sources, merging default and custom CSS,
 * and applying CSS variable overrides. This interface abstracts the complexity
 * of CSS management and provides a clean API for the HTML renderer.
 * 
 * Implementations should:
 * - Load CSS from file paths when specified
 * - Merge default theme CSS with custom CSS in the correct order
 * - Apply CSS variable overrides
 * - Handle built-in theme selection
 * - Return descriptive errors for failure cases
 * 
 * Validates: Requirements 1.1, 2.1, 3.1
 */
interface CssProvider {
    /**
     * Loads and prepares CSS content based on configuration.
     * 
     * This method orchestrates the entire CSS preparation process:
     * 1. Generates CSS variable overrides (if any) in a :root block
     * 2. Includes default theme CSS (if enabled)
     * 3. Loads and appends custom CSS (from content or file path)
     * 
     * The order ensures proper CSS cascade: variables are defined first,
     * then default theme styles, then custom overrides.
     * 
     * Custom CSS content takes precedence over custom CSS file path.
     * If both are provided, only the content is used.
     * 
     * @param cssOptions CSS configuration options specifying what CSS to include
     * @param theme Theme instance for generating default CSS (used if includeDefaultCss is true)
     * @return Result containing the final merged CSS content as a string on success,
     *         or a CssException on failure (file not found, invalid theme, etc.)
     * 
     * @see CssOptions
     * @see CssException
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
 * 
 * The CSS is assembled in the following order to ensure proper cascade:
 * 1. CSS variable overrides in :root block (highest priority for variables)
 * 2. Default theme CSS (if enabled)
 * 3. Custom CSS (highest priority for rules)
 * 
 * This order allows custom CSS to override default theme styles while
 * CSS variables can be overridden at the :root level.
 * 
 * Validates: Requirements 1.1, 1.2, 2.1, 2.2, 3.1, 3.2, 3.4, 6.2, 8.1, 8.2
 * 
 * @param fileReader FileReader implementation for loading CSS from files
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
                // If builtInTheme is empty, use the provided theme (backward compatibility)
                // Otherwise, try to get the built-in theme by name, falling back to provided theme
                val themeToUse = if (cssOptions.builtInTheme.isEmpty()) {
                    theme
                } else {
                    getBuiltInTheme(cssOptions.builtInTheme) ?: theme
                }
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
        } catch (e: CssException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(CssException.LoadingFailure(e.message ?: "Unknown error"))
        }
    }
    
    /**
     * Loads CSS content from a file path.
     * 
     * @param path File path (relative or absolute)
     * @return CSS content as a string
     * @throws CssException.FileNotFound if the file doesn't exist
     * @throws CssException.LoadingFailure if the file cannot be read
     */
    private fun loadCssFromFile(path: String): String {
        return fileReader.readFile(path).getOrElse { error ->
            when {
                error.message?.contains("not found", ignoreCase = true) == true ||
                error.message?.contains("does not exist", ignoreCase = true) == true ->
                    throw CssException.FileNotFound(path, error.message ?: "File not found")
                else ->
                    throw CssException.LoadingFailure("Failed to read file '$path': ${error.message}")
            }
        }
    }
    
    /**
     * Returns a built-in theme by name.
     * 
     * Supported theme names (case-insensitive):
     * - "default": DefaultTheme with standard styling
     * - "minimal": MinimalTheme with minimal styling
     * - "dark": DarkTheme with dark color scheme
     * - "kotlin": KotlinTheme with Kotlin/neuroSKai design system colors
     * 
     * @param themeName Name of the built-in theme
     * @return Theme instance, or null if the theme name is not recognized
     */
    private fun getBuiltInTheme(themeName: String): Theme? {
        return when (themeName.lowercase()) {
            "default" -> DefaultTheme()
            "minimal" -> MinimalTheme()
            "dark" -> DarkTheme()
            "kotlin" -> KotlinTheme()
            else -> null
        }
    }
}
