package org.markup.poet.tck.fixtures

import kotlinx.serialization.Serializable

/**
 * Categories for organizing test fixtures.
 */
@Serializable
enum class FixtureCategory {
    BLOCK_PARAGRAPH,
    BLOCK_HEADING,
    BLOCK_LIST,
    BLOCK_TABLE,
    BLOCK_CODE,
    BLOCK_QUOTE,
    INLINE_BOLD,
    INLINE_ITALIC,
    INLINE_MONOSPACE,
    INLINE_SUBSCRIPT,
    INLINE_SUPERSCRIPT,
    ATTRIBUTE,
    MACRO,
    CROSS_REFERENCE,
    INCLUDE,
    MALFORMED_BLOCK,
    MALFORMED_INLINE,
    MALFORMED_ATTRIBUTE,
    CIRCULAR_INCLUDE,
    MISSING_INCLUDE,
    CONFORMANCE,
    PLATFORM_FILE_IO,
    PLATFORM_ENCODING,
    PLATFORM_PATH_RESOLUTION
}
