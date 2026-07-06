package org.markup.poet.asciidoc.processing

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.checkAll
import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.Location
import org.markup.poet.asciidoc.asg.Position
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test to verify module setup and dependencies are correctly configured.
 */
class ModuleSetupTest {

    @Test
    fun `should be able to access asciidoc-parser classes`() {
        // This test verifies that we can access classes from the asciidoc-parser dependency
        val document = AsgDocument(
            location = Location(Position(1, 1), Position(1, 1))
        )
        assertEquals(0, document.blocks.size)
    }

    @Test
    fun `should be able to create ProcessingConfig`() {
        val config = ProcessingConfig(
            enableIncludes = true,
            maxIncludeDepth = 10
        )
        assertEquals(true, config.enableIncludes)
        assertEquals(10, config.maxIncludeDepth)
    }

    @Test
    fun `should be able to create ProcessingError`() {
        val error = ProcessingError(
            message = "Test error",
            location = Location(Position(1, 1), Position(1, 1)),
            errorType = ProcessingErrorType.INCLUDE_NOT_FOUND
        )
        assertEquals("Test error", error.message)
    }
}

/**
 * Test to verify Kotest property testing is available.
 */
class KotestPropertySetupTest : FunSpec({
    test("kotest property testing should work") {
        checkAll<Int, Int> { a, b ->
            (a + b) - b shouldBe a
        }
    }
})
