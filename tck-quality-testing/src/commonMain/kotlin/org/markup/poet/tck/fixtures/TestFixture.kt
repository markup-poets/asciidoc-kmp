package org.markup.poet.tck.fixtures

import kotlinx.serialization.Serializable

/**
 * Represents a test fixture with input AsciiDoc and expected output.
 */
@Serializable
data class TestFixture(
    val id: String,
    val category: FixtureCategory,
    val description: String,
    val input: String,
    val expectedOutput: String? = null,
    val metadata: Map<String, String> = emptyMap()
)
