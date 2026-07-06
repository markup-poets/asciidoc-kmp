package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.asg.AsgDocument

/**
 * Interface for custom document processors that can be registered with the extension system.
 * Custom processors can be inserted at specific phases of the processing pipeline.
 */
interface CustomProcessor {
    /**
     * Unique name identifying this processor.
     */
    val name: String

    /**
     * Priority determining execution order within a phase.
     * Higher priority processors execute first.
     */
    val priority: ProcessorPriority

    /**
     * Process the document and return the result.
     *
     * @param document The document to process
     * @param context Processing context with configuration and shared state
     * @return Result containing the processed document and any errors/warnings
     */
    fun process(document: AsgDocument, context: ProcessingContext): ProcessorResult
}

/**
 * Priority levels for custom processors.
 * Determines execution order within a processing phase.
 */
enum class ProcessorPriority(val value: Int) {
    HIGHEST(5),
    HIGH(4),
    NORMAL(3),
    LOW(2),
    LOWEST(1)
}

/**
 * Context information available to custom processors during execution.
 */
data class ProcessingContext(
    /**
     * Configuration for the processing pipeline.
     */
    val config: ProcessingConfig,

    /**
     * Current phase of processing.
     */
    val currentPhase: ProcessingPhase,

    /**
     * Shared data that can be used to pass information between processors.
     * Processors can read and write to this map.
     */
    val sharedData: MutableMap<String, Any>
)

/**
 * Phases in the document processing pipeline where custom processors can be inserted.
 */
enum class ProcessingPhase {
    /**
     * Before include resolution.
     */
    PRE_INCLUDE,

    /**
     * After include resolution.
     */
    POST_INCLUDE,

    /**
     * Before attribute substitution.
     */
    PRE_ATTRIBUTE,

    /**
     * After attribute substitution.
     */
    POST_ATTRIBUTE,

    /**
     * Before macro expansion.
     */
    PRE_MACRO,

    /**
     * After macro expansion.
     */
    POST_MACRO,

    /**
     * Before validation.
     */
    PRE_VALIDATION,

    /**
     * After validation.
     */
    POST_VALIDATION
}

/**
 * Result of custom processor execution.
 */
data class ProcessorResult(
    /**
     * The processed document.
     */
    val document: AsgDocument,

    /**
     * Errors encountered during processing.
     */
    val errors: List<ProcessingError> = emptyList(),

    /**
     * Warnings generated during processing.
     */
    val warnings: List<ProcessingWarning> = emptyList(),

    /**
     * Whether to continue processing with remaining processors.
     * If false, the pipeline will halt after this processor.
     */
    val continueProcessing: Boolean = true
)
