package org.markup.poet.tck.publisher

import org.markup.poet.asciidoc.ast.Document

/**
 * Validator for TCK results publishing workflow stages.
 *
 * This validator ensures that each stage of the publishing workflow produces valid output:
 * - AsciiDoc documents are non-empty and contain expected sections
 * - AST documents have the expected structure
 * - HTML documents are valid HTML5 and contain expected content
 * - Publications are accessible at the expected URL
 *
 * ## Validation Philosophy
 * Validation is critical for dogfooding integrity. We must ensure that:
 * 1. Our exporter generates valid AsciiDoc
 * 2. Our parser can parse what we generate
 * 3. Our renderer produces valid HTML
 * 4. The published results are accessible
 *
 * Validation failures indicate bugs in our implementation and should be treated seriously.
 *
 * @see WorkflowResult
 */
interface WorkflowValidator {
    /**
     * Validate that the exported AsciiDoc document is well-formed and contains expected sections.
     *
     * Checks:
     * - Document is non-empty
     * - Contains document title
     * - Contains summary section
     * - Contains test results sections
     * - Contains metadata section
     *
     * @param asciidoc The AsciiDoc document to validate
     * @return ValidationResult indicating success or failure with error details
     */
    fun validateAsciidoc(asciidoc: String): ValidationResult
    
    /**
     * Validate that the parsed AST has the expected structure.
     *
     * Checks:
     * - Document has a title
     * - Document has sections (summary, results, metadata)
     * - Document structure is reasonable (not empty, not malformed)
     *
     * @param ast The Document AST to validate
     * @return ValidationResult indicating success or failure with error details
     */
    fun validateAst(ast: Document): ValidationResult
    
    /**
     * Validate that the rendered HTML is valid HTML5 and contains expected content.
     *
     * Checks:
     * - HTML is non-empty
     * - Contains DOCTYPE declaration
     * - Contains expected structural elements (html, head, body)
     * - Contains expected content sections (summary, results, metadata)
     * - Contains CSS styling (inline or external)
     *
     * @param html The HTML document to validate
     * @return ValidationResult indicating success or failure with error details
     */
    fun validateHtml(html: String): ValidationResult
    
    /**
     * Validate that a publication is accessible at the given URL.
     *
     * This is a placeholder for future implementation when GitHub Pages publishing is added.
     *
     * @param url The public URL to validate
     * @return ValidationResult indicating success or failure with error details
     */
    fun validatePublication(url: String): ValidationResult
}

/**
 * Result of a validation operation.
 */
sealed class ValidationResult {
    /**
     * Validation succeeded - the content is valid.
     */
    data object Valid : ValidationResult()
    
    /**
     * Validation failed - the content has errors.
     *
     * @property errorMessages List of validation error messages
     */
    data class Invalid(val errorMessages: List<String>) : ValidationResult()
    
    /**
     * Check if this result is valid.
     */
    fun isValid(): Boolean = this is Valid
    
    /**
     * Check if this result is invalid.
     */
    fun isInvalid(): Boolean = this is Invalid
    
    /**
     * Get the error messages if invalid, or empty list if valid.
     */
    fun getErrors(): List<String> = when (this) {
        is Valid -> emptyList()
        is Invalid -> errorMessages
    }
}

/**
 * Default implementation of [WorkflowValidator] that performs structural validation
 * at each stage of the publishing workflow.
 *
 * This validator performs basic structural checks to ensure that each stage produces
 * reasonable output. It does not perform deep semantic validation, but rather checks
 * that the output has the expected structure and contains key elements.
 *
 * ## Validation Strategy
 * - **AsciiDoc**: Check for document structure and key sections
 * - **AST**: Check for document structure and reasonable content
 * - **HTML**: Check for valid HTML5 structure and expected content
 * - **Publication**: Check URL accessibility (future implementation)
 */
class DefaultWorkflowValidator : WorkflowValidator {
    
    override fun validateAsciidoc(asciidoc: String): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Check 1: Non-empty document
        if (asciidoc.isBlank()) {
            errors.add("AsciiDoc document is empty")
            return ValidationResult.Invalid(errors)
        }
        
        // Check 2: Contains document title
        if (!asciidoc.contains(Regex("^=\\s+.+", RegexOption.MULTILINE))) {
            errors.add("AsciiDoc document missing document title (= Title)")
        }
        
        // Check 3: Contains summary section
        if (!asciidoc.contains(Regex("^==\\s+Summary", RegexOption.MULTILINE))) {
            errors.add("AsciiDoc document missing Summary section")
        }
        
        // Check 4: Contains test results section
        if (!asciidoc.contains(Regex("^==\\s+Test Results", RegexOption.MULTILINE))) {
            errors.add("AsciiDoc document missing Test Results section")
        }
        
        // Check 5: Contains metadata section
        if (!asciidoc.contains(Regex("^==\\s+Metadata", RegexOption.MULTILINE))) {
            errors.add("AsciiDoc document missing Metadata section")
        }
        
        // Check 6: Contains summary table
        if (!asciidoc.contains("|===")) {
            errors.add("AsciiDoc document missing tables (no |=== found)")
        }
        
        // Check 7: Contains test status indicators (at least one)
        val hasStatusIndicators = asciidoc.contains("✅") || 
                                   asciidoc.contains("❌") || 
                                   asciidoc.contains("PASSED") || 
                                   asciidoc.contains("FAILED")
        if (!hasStatusIndicators) {
            errors.add("AsciiDoc document missing test status indicators")
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }
    
    override fun validateAst(ast: Document): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Check 1: Document has a title
        if (ast.title.isNullOrBlank()) {
            errors.add("Document AST missing title")
        }
        
        // Check 2: Document has content (children)
        if (ast.children.isEmpty()) {
            errors.add("Document AST has no content blocks")
        }
        
        // Check 3: Document has sections (at least 2 for summary and results)
        val sectionCount = ast.children.count { block ->
            block::class.simpleName == "Section"
        }
        if (sectionCount < 2) {
            errors.add("Document AST has insufficient sections (expected at least 2, found $sectionCount)")
        }
        
        // Check 4: Document structure is reasonable (has some content depth)
        // A valid TCK results document should have sections with actual content
        val hasContentInSections = ast.children.any { block ->
            // Check if it's a Section with children
            if (block::class.simpleName == "Section") {
                // Use reflection-like approach to check if section has children
                // For now, we'll check if the block's string representation suggests it has content
                val blockStr = block.toString()
                blockStr.contains("Paragraph") || blockStr.contains("Table") || blockStr.contains("List")
            } else {
                // Non-section blocks count as content
                true
            }
        }
        if (!hasContentInSections) {
            errors.add("Document AST appears too shallow (sections have no content)")
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }
    
    override fun validateHtml(html: String): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Check 1: Non-empty HTML
        if (html.isBlank()) {
            errors.add("HTML document is empty")
            return ValidationResult.Invalid(errors)
        }
        
        // Check 2: Contains DOCTYPE declaration
        if (!html.contains("<!DOCTYPE html>", ignoreCase = true)) {
            errors.add("HTML document missing DOCTYPE declaration")
        }
        
        // Check 3: Contains html tag
        if (!html.contains("<html", ignoreCase = true)) {
            errors.add("HTML document missing <html> tag")
        }
        
        // Check 4: Contains head section
        if (!html.contains("<head>", ignoreCase = true)) {
            errors.add("HTML document missing <head> section")
        }
        
        // Check 5: Contains body section
        if (!html.contains("<body>", ignoreCase = true)) {
            errors.add("HTML document missing <body> section")
        }
        
        // Check 6: Contains title
        if (!html.contains("<title>", ignoreCase = true)) {
            errors.add("HTML document missing <title> tag")
        }
        
        // Check 7: Contains CSS (inline or external)
        val hasInlineCss = html.contains("<style>", ignoreCase = true)
        val hasExternalCss = html.contains("<link", ignoreCase = true) && 
                             html.contains("stylesheet", ignoreCase = true)
        if (!hasInlineCss && !hasExternalCss) {
            errors.add("HTML document missing CSS styling (no <style> or <link rel=\"stylesheet\">)")
        }
        
        // Check 8: Contains expected content sections
        // Look for headings that indicate the structure
        val hasHeadings = html.contains("<h1>", ignoreCase = true) || 
                          html.contains("<h2>", ignoreCase = true)
        if (!hasHeadings) {
            errors.add("HTML document missing heading tags (no <h1> or <h2> found)")
        }
        
        // Check 9: Contains table elements (for test results)
        if (!html.contains("<table>", ignoreCase = true)) {
            errors.add("HTML document missing table elements (expected for test results)")
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }
    
    override fun validatePublication(url: String): ValidationResult {
        // Placeholder for future implementation
        // When GitHub Pages publishing is implemented, this will check URL accessibility
        
        val errors = mutableListOf<String>()
        
        // Basic URL format validation
        if (url.isBlank()) {
            errors.add("Publication URL is empty")
            return ValidationResult.Invalid(errors)
        }
        
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            errors.add("Publication URL must start with http:// or https://")
        }
        
        // TODO: When publishing is implemented, add:
        // - HTTP request to check URL is accessible
        // - Verify response status is 200
        // - Verify content-type is text/html
        // - Verify page contains expected content
        
        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }
}
