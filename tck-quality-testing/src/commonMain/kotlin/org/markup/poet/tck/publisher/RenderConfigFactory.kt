package org.markup.poet.tck.publisher

import org.markup.poet.asciidoc.render.*

/**
 * Factory for creating RenderConfig instances optimized for TCK results publishing.
 *
 * This factory configures the HTML renderer with:
 * - KotlinTheme for dark background and red accents
 * - Standalone HTML output with inline CSS
 * - Custom CSS for test result styling (pass/fail indicators)
 * - Table of contents for easy navigation
 * - Metadata inclusion for proper HTML structure
 *
 * The configuration is specifically designed for publishing TCK test results to
 * GitHub Pages, ensuring the output is self-contained, visually appealing, and
 * properly styled with the Kotlin brand.
 *
 * ## Example Usage
 * ```kotlin
 * val config = RenderConfigFactory.createTckResultsConfig()
 * val renderer = DefaultHtmlRenderer(blockRenderer, inlineRenderer)
 * val html = renderer.render(document, config).getOrThrow()
 * ```
 *
 * Validates: Requirements 3.2, 3.3, 3.4, 3.5
 */
object RenderConfigFactory {
    
    /**
     * Creates a RenderConfig optimized for TCK results publishing.
     *
     * The configuration includes:
     * - **Standalone mode**: Complete HTML document with head and body
     * - **Inline CSS**: All styles embedded for GitHub Pages compatibility
     * - **KotlinTheme**: Dark background with red accents
     * - **Custom CSS**: Test result styling with color-coded status indicators
     * - **Table of contents**: Enabled for easy navigation
     * - **Metadata**: Included for proper HTML structure
     *
     * ## Custom CSS Variables
     * The configuration defines custom CSS variables for test result styling:
     * - `--mp-color-success`: Green (#10b981) for passed tests
     * - `--mp-color-error`: Red (#ef4444) for failed tests
     * - `--mp-color-warning`: Orange (#f59e0b) for errors/pending tests
     *
     * ## Custom CSS Classes
     * The configuration includes custom CSS classes for test results:
     * - `.test-passed`: Green color for passed test indicators
     * - `.test-failed`: Red color for failed test indicators
     * - `.test-error`: Orange color for error test indicators
     * - `.pass-rate-high`: Green bold text for high pass rates (≥80%)
     * - `.pass-rate-medium`: Orange bold text for medium pass rates (50-79%)
     * - `.pass-rate-low`: Red bold text for low pass rates (<50%)
     *
     * @param documentTitle Optional custom document title (defaults to "AsciiDoc Konvert - TCK Results")
     * @return Configured RenderConfig for TCK results
     */
    fun createTckResultsConfig(
        documentTitle: String = "AsciiDoc Konvert - TCK Results"
    ): RenderConfig {
        return RenderConfig(
            outputOptions = OutputOptions(
                standalone = true,           // Complete HTML document
                cssMode = CssMode.INLINE,    // Inline CSS for GitHub Pages
                includeMetadata = true,      // Include meta tags
                includeToc = true,           // Table of contents for navigation
                documentTitle = documentTitle,
                language = "en",
                customAttributes = mapOf(
                    "data-theme" to "kotlin",
                    "data-purpose" to "tck-results"
                )
            ),
            theme = KotlinTheme(),           // Dark background, red accents
            cssOptions = CssOptions(
                cssVariables = mapOf(
                    // Custom colors for test results
                    "--mp-color-success" to "#10b981",  // Green for passed tests
                    "--mp-color-error" to "#ef4444",    // Red for failed tests
                    "--mp-color-warning" to "#f59e0b"   // Orange for errors/pending
                ),
                customCssContent = """
                    /* Custom styles for TCK test results */
                    
                    /* Test status indicators */
                    .test-passed {
                        color: var(--mp-color-success);
                        font-weight: 600;
                    }
                    
                    .test-failed {
                        color: var(--mp-color-error);
                        font-weight: 600;
                    }
                    
                    .test-error {
                        color: var(--mp-color-warning);
                        font-weight: 600;
                    }
                    
                    .test-skipped {
                        color: var(--muted-foreground);
                        font-style: italic;
                    }
                    
                    .test-pending {
                        color: var(--mp-color-warning);
                        font-style: italic;
                    }
                    
                    /* Pass rate styling */
                    .pass-rate-high {
                        color: var(--mp-color-success);
                        font-weight: bold;
                        font-size: 1.2em;
                    }
                    
                    .pass-rate-medium {
                        color: var(--mp-color-warning);
                        font-weight: bold;
                        font-size: 1.2em;
                    }
                    
                    .pass-rate-low {
                        color: var(--mp-color-error);
                        font-weight: bold;
                        font-size: 1.2em;
                    }
                    
                    /* Summary statistics table */
                    .summary-table {
                        margin: 2rem 0;
                        border-radius: 8px;
                        overflow: hidden;
                    }
                    
                    .summary-table th {
                        background-color: var(--surface-variant);
                        font-weight: 600;
                    }
                    
                    .summary-table td {
                        padding: 0.75rem 1rem;
                    }
                    
                    /* Test results table */
                    .test-results-table {
                        margin: 1.5rem 0;
                        font-size: 0.95em;
                    }
                    
                    .test-results-table th {
                        text-align: left;
                        padding: 0.75rem 1rem;
                    }
                    
                    .test-results-table td {
                        padding: 0.5rem 1rem;
                        vertical-align: top;
                    }
                    
                    .test-results-table tr:hover {
                        background-color: var(--surface-variant);
                    }
                    
                    /* Failed test details */
                    .failed-test-details {
                        background-color: var(--muted);
                        border-left: 4px solid var(--mp-color-error);
                        padding: 1rem 1.5rem;
                        margin: 1rem 0;
                        border-radius: 4px;
                    }
                    
                    .failed-test-details h4 {
                        color: var(--mp-color-error);
                        margin-top: 0;
                    }
                    
                    .failed-test-details pre {
                        background-color: var(--surface);
                        border: 1px solid var(--border);
                        padding: 1rem;
                        border-radius: 4px;
                        overflow-x: auto;
                        font-size: 0.9em;
                    }
                    
                    /* Metadata section */
                    .metadata-section {
                        background-color: var(--card);
                        border: 1px solid var(--border);
                        border-radius: 8px;
                        padding: 1.5rem;
                        margin: 2rem 0;
                    }
                    
                    .metadata-section h3 {
                        margin-top: 0;
                        color: var(--primary);
                    }
                    
                    .metadata-section ul {
                        list-style: none;
                        padding-left: 0;
                    }
                    
                    .metadata-section li {
                        padding: 0.25rem 0;
                        color: var(--foreground);
                    }
                    
                    .metadata-section li strong {
                        color: var(--primary);
                        margin-right: 0.5rem;
                    }
                    
                    /* Category sections */
                    .category-section {
                        margin: 2rem 0;
                    }
                    
                    .category-section h3 {
                        border-bottom: 2px solid var(--primary);
                        padding-bottom: 0.5rem;
                        margin-bottom: 1rem;
                    }
                    
                    /* Progress indicator */
                    .progress-bar {
                        width: 100%;
                        height: 30px;
                        background-color: var(--muted);
                        border-radius: 15px;
                        overflow: hidden;
                        margin: 1rem 0;
                        border: 1px solid var(--border);
                    }
                    
                    .progress-bar-fill {
                        height: 100%;
                        background: linear-gradient(90deg, var(--mp-color-success), var(--primary-glow));
                        transition: width 0.3s ease;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        color: white;
                        font-weight: bold;
                        font-size: 0.9em;
                    }
                    
                    /* Certification status badge */
                    .certification-badge {
                        display: inline-block;
                        padding: 0.5rem 1rem;
                        border-radius: 20px;
                        font-weight: bold;
                        font-size: 0.9em;
                        margin: 0.5rem 0;
                    }
                    
                    .certification-badge.ready {
                        background-color: var(--mp-color-success);
                        color: white;
                    }
                    
                    .certification-badge.in-progress {
                        background-color: var(--mp-color-warning);
                        color: white;
                    }
                    
                    .certification-badge.blocked {
                        background-color: var(--mp-color-error);
                        color: white;
                    }
                    
                    /* Responsive adjustments for mobile */
                    @media (max-width: 768px) {
                        .test-results-table {
                            font-size: 0.85em;
                        }
                        
                        .test-results-table th,
                        .test-results-table td {
                            padding: 0.5rem;
                        }
                        
                        .failed-test-details {
                            padding: 0.75rem 1rem;
                        }
                        
                        .metadata-section {
                            padding: 1rem;
                        }
                    }
                """.trimIndent(),
                includeDefaultCss = true,
                builtInTheme = "" // Use theme from RenderConfig (KotlinTheme)
            )
        )
    }
    
    /**
     * Creates a minimal RenderConfig for testing purposes.
     *
     * This configuration is useful for unit tests where you want to verify
     * the rendering logic without the full styling overhead.
     *
     * @return Minimal RenderConfig with basic settings
     */
    fun createMinimalConfig(): RenderConfig {
        return RenderConfig(
            outputOptions = OutputOptions(
                standalone = true,
                cssMode = CssMode.NONE,
                includeMetadata = false,
                includeToc = false
            ),
            theme = Theme.default()
        )
    }
}
