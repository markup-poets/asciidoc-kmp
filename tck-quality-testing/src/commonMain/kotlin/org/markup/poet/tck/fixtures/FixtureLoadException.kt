package org.markup.poet.tck.fixtures

/**
 * Exception thrown when a fixture cannot be loaded.
 */
class FixtureLoadException(
    val fixtureId: String,
    val path: String,
    message: String,
    cause: Throwable? = null
) : Exception("Failed to load fixture '$fixtureId' from '$path': $message", cause)
