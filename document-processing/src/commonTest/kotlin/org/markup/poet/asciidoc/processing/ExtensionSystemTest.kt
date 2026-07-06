package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.InlineText
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.LeafBlockForm
import org.markup.poet.asciidoc.asg.LeafBlockName
import org.markup.poet.asciidoc.asg.Location
import org.markup.poet.asciidoc.asg.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for the extension system including CustomProcessor and ExtensionRegistry.
 */
class ExtensionSystemTest {

    @Test
    fun `should register custom processor`() {
        val registry = DefaultExtensionRegistry()
        val processor = TestProcessor("test-processor", ProcessorPriority.NORMAL)

        registry.register(processor, ProcessingPhase.POST_VALIDATION)

        assertTrue(registry.isRegistered("test-processor"))
    }

    @Test
    fun `should unregister custom processor`() {
        val registry = DefaultExtensionRegistry()
        val processor = TestProcessor("test-processor", ProcessorPriority.NORMAL)

        registry.register(processor, ProcessingPhase.POST_VALIDATION)
        val result = registry.unregister("test-processor")

        assertTrue(result)
        assertFalse(registry.isRegistered("test-processor"))
    }

    @Test
    fun `should return false when unregistering non-existent processor`() {
        val registry = DefaultExtensionRegistry()

        val result = registry.unregister("non-existent")

        assertFalse(result)
    }

    @Test
    fun `should get processors for specific phase`() {
        val registry = DefaultExtensionRegistry()
        val processor1 = TestProcessor("processor-1", ProcessorPriority.HIGH)
        val processor2 = TestProcessor("processor-2", ProcessorPriority.LOW)

        registry.register(processor1, ProcessingPhase.POST_VALIDATION)
        registry.register(processor2, ProcessingPhase.POST_VALIDATION)

        val processors = registry.getProcessors(ProcessingPhase.POST_VALIDATION)

        assertEquals(2, processors.size)
        assertEquals("processor-1", processors[0].name) // HIGH priority first
        assertEquals("processor-2", processors[1].name) // LOW priority second
    }

    @Test
    fun `should sort processors by priority`() {
        val registry = DefaultExtensionRegistry()
        val lowest = TestProcessor("lowest", ProcessorPriority.LOWEST)
        val highest = TestProcessor("highest", ProcessorPriority.HIGHEST)
        val normal = TestProcessor("normal", ProcessorPriority.NORMAL)
        val high = TestProcessor("high", ProcessorPriority.HIGH)
        val low = TestProcessor("low", ProcessorPriority.LOW)

        // Register in random order
        registry.register(normal, ProcessingPhase.POST_VALIDATION)
        registry.register(lowest, ProcessingPhase.POST_VALIDATION)
        registry.register(high, ProcessingPhase.POST_VALIDATION)
        registry.register(highest, ProcessingPhase.POST_VALIDATION)
        registry.register(low, ProcessingPhase.POST_VALIDATION)

        val processors = registry.getProcessors(ProcessingPhase.POST_VALIDATION)

        assertEquals(5, processors.size)
        assertEquals("highest", processors[0].name)
        assertEquals("high", processors[1].name)
        assertEquals("normal", processors[2].name)
        assertEquals("low", processors[3].name)
        assertEquals("lowest", processors[4].name)
    }

    @Test
    fun `should register processor for multiple phases`() {
        val registry = DefaultExtensionRegistry()
        val processor = TestProcessor("multi-phase", ProcessorPriority.NORMAL)

        registry.register(processor, setOf(
            ProcessingPhase.PRE_INCLUDE,
            ProcessingPhase.POST_INCLUDE,
            ProcessingPhase.POST_VALIDATION
        ))

        assertTrue(registry.getProcessors(ProcessingPhase.PRE_INCLUDE).isNotEmpty())
        assertTrue(registry.getProcessors(ProcessingPhase.POST_INCLUDE).isNotEmpty())
        assertTrue(registry.getProcessors(ProcessingPhase.POST_VALIDATION).isNotEmpty())
        assertTrue(registry.getProcessors(ProcessingPhase.PRE_ATTRIBUTE).isEmpty())
    }

    @Test
    fun `should return empty list for phase with no processors`() {
        val registry = DefaultExtensionRegistry()

        val processors = registry.getProcessors(ProcessingPhase.PRE_INCLUDE)

        assertTrue(processors.isEmpty())
    }

    @Test
    fun `should get all processors across all phases`() {
        val registry = DefaultExtensionRegistry()
        val processor1 = TestProcessor("processor-1", ProcessorPriority.NORMAL)
        val processor2 = TestProcessor("processor-2", ProcessorPriority.NORMAL)

        registry.register(processor1, ProcessingPhase.PRE_INCLUDE)
        registry.register(processor2, ProcessingPhase.POST_VALIDATION)

        val allProcessors = registry.getAllProcessors()

        assertEquals(2, allProcessors.size)
        assertTrue(allProcessors.containsKey(ProcessingPhase.PRE_INCLUDE))
        assertTrue(allProcessors.containsKey(ProcessingPhase.POST_VALIDATION))
    }

    @Test
    fun `should clear all processors`() {
        val registry = DefaultExtensionRegistry()
        val processor1 = TestProcessor("processor-1", ProcessorPriority.NORMAL)
        val processor2 = TestProcessor("processor-2", ProcessorPriority.NORMAL)

        registry.register(processor1, ProcessingPhase.PRE_INCLUDE)
        registry.register(processor2, ProcessingPhase.POST_VALIDATION)

        registry.clear()

        assertFalse(registry.isRegistered("processor-1"))
        assertFalse(registry.isRegistered("processor-2"))
        assertTrue(registry.getAllProcessors().isEmpty())
    }

    @Test
    fun `should replace processor with same name`() {
        val registry = DefaultExtensionRegistry()
        val processor1 = TestProcessor("same-name", ProcessorPriority.LOW)
        val processor2 = TestProcessor("same-name", ProcessorPriority.HIGH)

        registry.register(processor1, ProcessingPhase.POST_VALIDATION)
        registry.register(processor2, ProcessingPhase.POST_VALIDATION)

        val processors = registry.getProcessors(ProcessingPhase.POST_VALIDATION)

        assertEquals(1, processors.size)
        assertEquals(ProcessorPriority.HIGH, processors[0].priority)
    }

    @Test
    fun `custom processor should receive processing context`() {
        val document = createTestDocument()
        val config = ProcessingConfig()
        val sharedData = mutableMapOf<String, Any>()
        val context = ProcessingContext(
            config = config,
            currentPhase = ProcessingPhase.POST_VALIDATION,
            sharedData = sharedData
        )

        val processor = TestProcessor("test", ProcessorPriority.NORMAL)
        val result = processor.process(document, context)

        assertEquals(document, result.document)
        assertTrue(result.continueProcessing)
        assertTrue(result.errors.isEmpty())
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `custom processor can halt processing`() {
        val document = createTestDocument()
        val config = ProcessingConfig()
        val context = ProcessingContext(
            config = config,
            currentPhase = ProcessingPhase.POST_VALIDATION,
            sharedData = mutableMapOf()
        )

        val processor = HaltingProcessor("halting", ProcessorPriority.NORMAL)
        val result = processor.process(document, context)

        assertFalse(result.continueProcessing)
    }

    @Test
    fun `custom processor can add errors and warnings`() {
        val document = createTestDocument()
        val config = ProcessingConfig()
        val context = ProcessingContext(
            config = config,
            currentPhase = ProcessingPhase.POST_VALIDATION,
            sharedData = mutableMapOf()
        )

        val processor = ErrorGeneratingProcessor("error-gen", ProcessorPriority.NORMAL)
        val result = processor.process(document, context)

        assertEquals(1, result.errors.size)
        assertEquals(1, result.warnings.size)
    }

    @Test
    fun `custom processor can use shared data`() {
        val sharedData = mutableMapOf<String, Any>()
        val context = ProcessingContext(
            config = ProcessingConfig(),
            currentPhase = ProcessingPhase.POST_VALIDATION,
            sharedData = sharedData
        )

        val processor = SharedDataProcessor("shared", ProcessorPriority.NORMAL)
        processor.process(createTestDocument(), context)

        assertEquals("test-value", sharedData["test-key"])
    }

    // Helper functions and test processors

    private fun createTestDocument(): AsgDocument {
        val location = Location(Position(1, 1), Position(1, 1))
        return AsgDocument(
            blocks = listOf(
                LeafBlock(
                    name = LeafBlockName.PARAGRAPH,
                    form = LeafBlockForm.PARAGRAPH,
                    inlines = listOf(InlineText("Test content", location)),
                    location = location
                )
            ),
            location = location
        )
    }

    private class TestProcessor(
        override val name: String,
        override val priority: ProcessorPriority
    ) : CustomProcessor {
        override fun process(document: AsgDocument, context: ProcessingContext): ProcessorResult {
            return ProcessorResult(
                document = document,
                errors = emptyList(),
                warnings = emptyList(),
                continueProcessing = true
            )
        }
    }

    private class HaltingProcessor(
        override val name: String,
        override val priority: ProcessorPriority
    ) : CustomProcessor {
        override fun process(document: AsgDocument, context: ProcessingContext): ProcessorResult {
            return ProcessorResult(
                document = document,
                errors = emptyList(),
                warnings = emptyList(),
                continueProcessing = false
            )
        }
    }

    private class ErrorGeneratingProcessor(
        override val name: String,
        override val priority: ProcessorPriority
    ) : CustomProcessor {
        override fun process(document: AsgDocument, context: ProcessingContext): ProcessorResult {
            return ProcessorResult(
                document = document,
                errors = listOf(
                    ProcessingError(
                        message = "Test error",
                        location = null,
                        errorType = ProcessingErrorType.CONFIGURATION_INVALID,
                        severity = ErrorSeverity.ERROR
                    )
                ),
                warnings = listOf(
                    ProcessingWarning(
                        message = "Test warning",
                        location = null,
                        warningType = ProcessingWarningType.ATTRIBUTE_UNDEFINED
                    )
                ),
                continueProcessing = true
            )
        }
    }

    private class SharedDataProcessor(
        override val name: String,
        override val priority: ProcessorPriority
    ) : CustomProcessor {
        override fun process(document: AsgDocument, context: ProcessingContext): ProcessorResult {
            context.sharedData["test-key"] = "test-value"
            return ProcessorResult(
                document = document,
                errors = emptyList(),
                warnings = emptyList(),
                continueProcessing = true
            )
        }
    }
}
