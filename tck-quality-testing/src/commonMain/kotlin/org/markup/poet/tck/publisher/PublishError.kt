package org.markup.poet.tck.publisher

/**
 * Sealed class hierarchy representing errors that can occur during the TCK results publishing workflow.
 *
 * Each error type captures specific failure scenarios with relevant context for debugging and recovery.
 */
sealed class PublishError {
    
    /**
     * Error during AsciiDoc export generation.
     *
     * @property message Human-readable error description
     * @property cause Original exception that caused the error (if any)
     */
    data class ExportError(
        val message: String,
        val cause: Throwable? = null
    ) : PublishError()
    
    /**
     * Error during AsciiDoc parsing (CRITICAL - indicates bug in exporter or parser).
     *
     * This error type is particularly critical because it indicates that our parser
     * cannot parse the AsciiDoc document we generated ourselves, which violates
     * the dogfooding principle and suggests a bug in either the exporter or parser.
     *
     * @property message Human-readable error description
     * @property parseErrors List of specific parsing errors with line/column information
     * @property asciidocContent The AsciiDoc content that failed to parse (for debugging)
     */
    data class ParseError(
        val message: String,
        val parseErrors: List<String>,
        val asciidocContent: String? = null
    ) : PublishError()
    
    /**
     * Error during HTML rendering.
     *
     * @property message Human-readable error description
     * @property cause Original exception that caused the error (if any)
     */
    data class RenderError(
        val message: String,
        val cause: Throwable? = null
    ) : PublishError()
    
    /**
     * Error during GitHub Pages publication.
     *
     * @property message Human-readable error description
     * @property cause Original exception that caused the error (if any)
     */
    data class PublicationError(
        val message: String,
        val cause: Throwable? = null
    ) : PublishError()
    
    /**
     * Network-related error (Git operations, GitHub API, etc.).
     *
     * @property message Human-readable error description
     * @property cause Original exception that caused the error (if any)
     * @property retryable Whether this error is potentially recoverable with retry
     */
    data class NetworkError(
        val message: String,
        val cause: Throwable? = null,
        val retryable: Boolean = true
    ) : PublishError()
    
    /**
     * Validation error (invalid configuration, malformed data, etc.).
     *
     * @property message Human-readable error description
     * @property validationErrors List of specific validation failures
     */
    data class ValidationError(
        val message: String,
        val validationErrors: List<String>
    ) : PublishError()
}
