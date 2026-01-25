package org.markup.poet.tck.publisher

import org.markup.poet.asciidoc.render.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for RenderConfigFactory.
 *
 * Validates: Requirements 3.2, 3.3, 3.4, 3.5
 */
class RenderConfigFactoryTest {
    
    @Test
    fun `should create config with standalone mode enabled`() {
        // Arrange & Act
        val config = RenderConfigFactory.createTckResultsConfig()
        
        // Assert
        assertTrue(config.outputOptions.standalone, "Standalone mode should be enabled")
    }
    
    @Test
    fun `should create config with inline CSS mode`() {
        // Arrange & Act
        val config = RenderConfigFactory.createTckResultsConfig()
        
        // Assert
        assertEquals(CssMode.INLINE, config.outputOptions.cssMode, "CSS mode should be INLINE")
    }
    
    @Test
    fun `should create config with KotlinTheme`() {
        // Arrange & Act
        val config = RenderConfigFactory.createTckResultsConfig()
        
        // Assert
        assertTrue(config.theme is KotlinTheme, "Theme should be KotlinTheme")
    }
    
    @Test
    fun `should create config with table of contents enabled`() {
        // Arrange & Act
        val config = RenderConfigFactory.createTckResultsConfig()
        
        // Assert
        assertTrue(config.outputOptions.includeToc, "Table of contents should be enabled")
    }
    
    @Test
    fun `should create config with metadata enabled`() {
        // Arrange & Act
        val config = RenderConfigFactory.createTckResultsConfig()
        
        // Assert
        assertTrue(config.outputOptions.includeMetadata, "Metadata should be enabled")
    }
    
    @Test
    fun `should create config with default document title`() {
        // Arrange & Act
        val config = RenderConfigFactory.createTckResultsConfig()
        
        // Assert
        assertEquals(
            "AsciiDoc Konvert - TCK Results",
            config.outputOptions.documentTitle,
            "Document title should be set to default"
        )
    }
    
    @Test
    fun `should create config with custom document title`() {
        // Arrange
        val customTitle = "Custom TCK Results - 2026-01-24"
        
        // Act
        val config = RenderConfigFactory.createTckResultsConfig(documentTitle = customTitle)
        
        // Assert
        assertEquals(
            customTitle,
            config.outputOptions.documentTitle,
            "Document title should be set to custom value"
        )
    }
    
    @Test
    fun `should create config with English language`() {
        // Arrange & Act
        val config = RenderConfigFactory.createTckResultsConfig()
        
        // Assert
        assertEquals("en", config.outputOptions.language, "Language should be English")
    }
    
    @Test
    fun `should create config with custom HTML attributes`() {
        // Arrange & Act
        val config = RenderConfigFactory.createTckResultsConfig()
        
        // Assert
        assertEquals(
            "kotlin",
            config.outputOptions.customAttributes["data-theme"],
            "Should have data-theme attribute"
        )
        assertEquals(
            "tck-results",
            config.outputOptions.customAttributes["data-purpose"],
            "Should have data-purpose attribute"
        )
    }
    
    @Test
    fun `should create config with custom CSS variables for test results`() {
        // Arrange & Act
        val config = RenderConfigFactory.createTckResultsConfig()
        
        // Assert
        val cssOptions = config.cssOptions
        assertEquals(
            "#10b981",
            cssOptions.cssVariables["--mp-color-success"],
            "Should have success color variable"
        )
        assertEquals(
            "#ef4444",
            cssOptions.cssVariables["--mp-color-error"],
            "Should have error color variable"
        )
        assertEquals(
            "#f59e0b",
            cssOptions.cssVariables["--mp-color-warning"],
            "Should have warning color variable"
        )
    }
    
    @Test
    fun `should create config with custom CSS content for test styling`() {
        // Arrange & Act
        val config = RenderConfigFactory.createTckResultsConfig()
        
        // Assert
        val customCss = config.cssOptions.customCssContent
        assertNotNull(customCss, "Custom CSS content should not be null")
        
        // Verify key CSS classes are present
        assertTrue(customCss.contains(".test-passed"), "Should contain .test-passed class")
        assertTrue(customCss.contains(".test-failed"), "Should contain .test-failed class")
        assertTrue(customCss.contains(".test-error"), "Should contain .test-error class")
        assertTrue(customCss.contains(".pass-rate-high"), "Should contain .pass-rate-high class")
        assertTrue(customCss.contains(".pass-rate-medium"), "Should contain .pass-rate-medium class")
        assertTrue(customCss.contains(".pass-rate-low"), "Should contain .pass-rate-low class")
        assertTrue(customCss.contains(".failed-test-details"), "Should contain .failed-test-details class")
        assertTrue(customCss.contains(".metadata-section"), "Should contain .metadata-section class")
        assertTrue(customCss.contains(".certification-badge"), "Should contain .certification-badge class")
    }
    
    @Test
    fun `should create config with default CSS included`() {
        // Arrange & Act
        val config = RenderConfigFactory.createTckResultsConfig()
        
        // Assert
        assertTrue(
            config.cssOptions.includeDefaultCss,
            "Default CSS should be included"
        )
    }
    
    @Test
    fun `should create config with empty built-in theme to use RenderConfig theme`() {
        // Arrange & Act
        val config = RenderConfigFactory.createTckResultsConfig()
        
        // Assert
        assertEquals(
            "",
            config.cssOptions.builtInTheme,
            "Built-in theme should be empty to use RenderConfig theme"
        )
    }
    
    @Test
    fun `should create config with responsive CSS for mobile devices`() {
        // Arrange & Act
        val config = RenderConfigFactory.createTckResultsConfig()
        
        // Assert
        val customCss = config.cssOptions.customCssContent
        assertNotNull(customCss, "Custom CSS content should not be null")
        
        // Verify responsive media query is present
        assertTrue(
            customCss.contains("@media (max-width: 768px)"),
            "Should contain responsive media query for mobile"
        )
    }
    
    @Test
    fun `should create minimal config for testing`() {
        // Arrange & Act
        val config = RenderConfigFactory.createMinimalConfig()
        
        // Assert
        assertTrue(config.outputOptions.standalone, "Standalone mode should be enabled")
        assertEquals(CssMode.NONE, config.outputOptions.cssMode, "CSS mode should be NONE")
        assertTrue(!config.outputOptions.includeMetadata, "Metadata should be disabled")
        assertTrue(!config.outputOptions.includeToc, "Table of contents should be disabled")
    }
    
    @Test
    fun `should create config with CSS for test status indicators`() {
        // Arrange & Act
        val config = RenderConfigFactory.createTckResultsConfig()
        
        // Assert
        val customCss = config.cssOptions.customCssContent
        assertNotNull(customCss, "Custom CSS content should not be null")
        
        // Verify all test status classes are present
        assertTrue(customCss.contains(".test-passed"), "Should have passed status styling")
        assertTrue(customCss.contains(".test-failed"), "Should have failed status styling")
        assertTrue(customCss.contains(".test-error"), "Should have error status styling")
        assertTrue(customCss.contains(".test-skipped"), "Should have skipped status styling")
        assertTrue(customCss.contains(".test-pending"), "Should have pending status styling")
    }
    
    @Test
    fun `should create config with CSS for pass rate visualization`() {
        // Arrange & Act
        val config = RenderConfigFactory.createTckResultsConfig()
        
        // Assert
        val customCss = config.cssOptions.customCssContent
        assertNotNull(customCss, "Custom CSS content should not be null")
        
        // Verify pass rate classes with different colors
        assertTrue(customCss.contains(".pass-rate-high"), "Should have high pass rate styling")
        assertTrue(customCss.contains(".pass-rate-medium"), "Should have medium pass rate styling")
        assertTrue(customCss.contains(".pass-rate-low"), "Should have low pass rate styling")
        
        // Verify they use the custom color variables
        assertTrue(
            customCss.contains("var(--mp-color-success)"),
            "Should use success color variable"
        )
        assertTrue(
            customCss.contains("var(--mp-color-error)"),
            "Should use error color variable"
        )
        assertTrue(
            customCss.contains("var(--mp-color-warning)"),
            "Should use warning color variable"
        )
    }
    
    @Test
    fun `should create config with CSS for certification status badges`() {
        // Arrange & Act
        val config = RenderConfigFactory.createTckResultsConfig()
        
        // Assert
        val customCss = config.cssOptions.customCssContent
        assertNotNull(customCss, "Custom CSS content should not be null")
        
        // Verify certification badge classes
        assertTrue(customCss.contains(".certification-badge"), "Should have base badge styling")
        assertTrue(customCss.contains(".certification-badge.ready"), "Should have ready badge styling")
        assertTrue(customCss.contains(".certification-badge.in-progress"), "Should have in-progress badge styling")
        assertTrue(customCss.contains(".certification-badge.blocked"), "Should have blocked badge styling")
    }
    
    @Test
    fun `should create config with CSS for tables`() {
        // Arrange & Act
        val config = RenderConfigFactory.createTckResultsConfig()
        
        // Assert
        val customCss = config.cssOptions.customCssContent
        assertNotNull(customCss, "Custom CSS content should not be null")
        
        // Verify table styling classes
        assertTrue(customCss.contains(".summary-table"), "Should have summary table styling")
        assertTrue(customCss.contains(".test-results-table"), "Should have test results table styling")
    }
    
    @Test
    fun `should create config with CSS for failed test details`() {
        // Arrange & Act
        val config = RenderConfigFactory.createTckResultsConfig()
        
        // Assert
        val customCss = config.cssOptions.customCssContent
        assertNotNull(customCss, "Custom CSS content should not be null")
        
        // Verify failed test details styling
        assertTrue(
            customCss.contains(".failed-test-details"),
            "Should have failed test details styling"
        )
        assertTrue(
            customCss.contains("border-left: 4px solid var(--mp-color-error)"),
            "Failed test details should have red left border"
        )
    }
    
    @Test
    fun `should create config with CSS for metadata section`() {
        // Arrange & Act
        val config = RenderConfigFactory.createTckResultsConfig()
        
        // Assert
        val customCss = config.cssOptions.customCssContent
        assertNotNull(customCss, "Custom CSS content should not be null")
        
        // Verify metadata section styling
        assertTrue(customCss.contains(".metadata-section"), "Should have metadata section styling")
    }
    
    @Test
    fun `should create config with CSS for progress bar`() {
        // Arrange & Act
        val config = RenderConfigFactory.createTckResultsConfig()
        
        // Assert
        val customCss = config.cssOptions.customCssContent
        assertNotNull(customCss, "Custom CSS content should not be null")
        
        // Verify progress bar styling
        assertTrue(customCss.contains(".progress-bar"), "Should have progress bar styling")
        assertTrue(customCss.contains(".progress-bar-fill"), "Should have progress bar fill styling")
    }
}
