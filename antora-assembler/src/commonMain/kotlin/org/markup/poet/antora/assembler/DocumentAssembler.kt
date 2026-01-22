package org.markup.poet.antora.assembler

/**
 * Assembles multiple AsciiDoc files from an Antora structure into a single document.
 */
interface DocumentAssembler {
    /**
     * Assemble a document from the configured index file.
     * Returns the result containing the assembled document or errors.
     */
    fun assemble(config: AssemblerConfig): AssemblerResult
}

data class AssemblerResult(
    val success: Boolean,
    val outputPath: String?,
    val errors: List<AssemblerError>,
    val warnings: List<AssemblerWarning>,
    val includedFiles: Set<String>
)

data class AssemblerError(
    val message: String,
    val filePath: String?,
    val lineNumber: Int?,
    val errorType: AssemblerErrorType
)

enum class AssemblerErrorType {
    INDEX_FILE_NOT_FOUND,
    PARSE_ERROR,
    INCLUDE_NOT_FOUND,
    CIRCULAR_DEPENDENCY,
    MAX_DEPTH_EXCEEDED,
    FILE_WRITE_ERROR,
    RESOLUTION_ERROR
}

data class AssemblerWarning(
    val message: String,
    val filePath: String?,
    val lineNumber: Int?
)
