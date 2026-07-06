package org.markup.poet.tck.performance

import org.markup.poet.asciidoc.parser.DefaultAsciidocParser
import org.markup.poet.tck.benchmark.DefaultBenchmarkRunner
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Performance benchmark tests for AsciiDoc parsing.
 *
 * These tests measure parsing performance for documents of various sizes
 * and complexity levels. The assertions are deliberately generous sanity
 * ceilings (seconds, not milliseconds) so they never flake on slow CI
 * machines; the printed metrics are the actual benchmark output.
 *
 * Requirements: 3.1, 3.3
 */
class ParsingBenchmarkTest {

    private val runner = DefaultBenchmarkRunner()
    private val parser = DefaultAsciidocParser()

    private fun benchmarkParse(
        name: String,
        source: String,
        iterations: Int,
        warmupIterations: Int,
        ceilingSeconds: Int,
    ) {
        // Sanity: the document actually parses into content before we measure it.
        val document = parser.parse(source).document
        assertTrue(document.blocks.isNotEmpty(), "$name: benchmark document did not parse")

        val metrics = runner.runBenchmark(name, iterations, warmupIterations) {
            parser.parse(source)
        }

        println("$name (${source.length} chars, ${metrics.iterations} iterations):")
        println("  Mean: ${metrics.mean}  Median: ${metrics.median}  P95: ${metrics.p95}  Max: ${metrics.max}")
        println("  Throughput: ${metrics.throughput} docs/sec")

        assertTrue(metrics.throughput > 0.0, "$name: throughput must be positive")
        assertTrue(
            metrics.max < ceilingSeconds.seconds,
            "$name: slowest iteration ${metrics.max} exceeded the ${ceilingSeconds}s sanity ceiling",
        )
    }

    @Test
    fun `benchmark small document parsing`() {
        benchmarkParse(
            name = "parse-small-document",
            source = BenchmarkDocuments.SMALL,
            iterations = 50,
            warmupIterations = 10,
            ceilingSeconds = 10,
        )
    }

    @Test
    fun `benchmark medium document parsing`() {
        benchmarkParse(
            name = "parse-medium-document",
            source = BenchmarkDocuments.MEDIUM,
            iterations = 10,
            warmupIterations = 2,
            ceilingSeconds = 30,
        )
    }

    @Test
    fun `benchmark large document parsing`() {
        benchmarkParse(
            name = "parse-large-document",
            source = BenchmarkDocuments.LARGE,
            iterations = 3,
            warmupIterations = 1,
            ceilingSeconds = 60,
        )
    }

    @Test
    fun `benchmark complex nested structure parsing`() {
        benchmarkParse(
            name = "parse-complex-nested",
            source = BenchmarkDocuments.COMPLEX_NESTED,
            iterations = 10,
            warmupIterations = 2,
            ceilingSeconds = 30,
        )
    }

    @Test
    fun `benchmark document with many inline formatting`() {
        benchmarkParse(
            name = "parse-inline-heavy",
            source = BenchmarkDocuments.INLINE_HEAVY,
            iterations = 10,
            warmupIterations = 2,
            ceilingSeconds = 30,
        )
    }

    @Test
    fun `benchmark incremental parsing performance`() {
        pending("Incremental parsing (re-parsing after small edits) not yet implemented")
    }

    private fun pending(reason: String): Nothing {
        throw PendingBenchmarkException(reason)
    }
}

/**
 * Exception thrown when a benchmark is pending implementation.
 */
class PendingBenchmarkException(message: String) : Exception(message)
