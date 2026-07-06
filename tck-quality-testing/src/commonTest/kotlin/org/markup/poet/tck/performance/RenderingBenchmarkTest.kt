package org.markup.poet.tck.performance

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.parser.DefaultAsciidocParser
import org.markup.poet.asciidoc.render.CssMode
import org.markup.poet.asciidoc.render.DefaultBlockRenderer
import org.markup.poet.asciidoc.render.DefaultHtmlBuilder
import org.markup.poet.asciidoc.render.DefaultHtmlEscaper
import org.markup.poet.asciidoc.render.DefaultHtmlRenderer
import org.markup.poet.asciidoc.render.DefaultInlineRenderer
import org.markup.poet.asciidoc.render.KotlinTheme
import org.markup.poet.asciidoc.render.OutputOptions
import org.markup.poet.asciidoc.render.RenderConfig
import org.markup.poet.tck.benchmark.DefaultBenchmarkRunner
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Performance benchmark tests for AsciiDoc rendering.
 *
 * These tests measure HTML rendering performance for various document
 * structures. The assertions are deliberately generous sanity ceilings
 * (seconds, not milliseconds) so they never flake on slow CI machines;
 * the printed metrics are the actual benchmark output.
 *
 * Requirements: 3.2, 3.3
 */
class RenderingBenchmarkTest {

    private val runner = DefaultBenchmarkRunner()
    private val parser = DefaultAsciidocParser()

    private fun newRenderer(): DefaultHtmlRenderer {
        val builder = DefaultHtmlBuilder(DefaultHtmlEscaper())
        val inlineRenderer = DefaultInlineRenderer(builder)
        return DefaultHtmlRenderer(DefaultBlockRenderer(builder, inlineRenderer), inlineRenderer)
    }

    private fun fragmentConfig(theme: org.markup.poet.asciidoc.render.Theme? = null): RenderConfig {
        val options = OutputOptions(standalone = false, cssMode = CssMode.NONE)
        return if (theme != null) {
            RenderConfig(outputOptions = options, theme = theme)
        } else {
            RenderConfig(outputOptions = options)
        }
    }

    private fun benchmarkRender(
        name: String,
        document: AsgDocument,
        config: RenderConfig,
        iterations: Int,
        warmupIterations: Int,
        ceilingSeconds: Int,
    ) {
        val renderer = newRenderer()
        // Sanity: rendering succeeds and produces output before we measure it.
        val html = renderer.render(document, config).getOrThrow()
        assertTrue(html.isNotBlank(), "$name: rendered HTML is empty")

        val metrics = runner.runBenchmark(name, iterations, warmupIterations) {
            renderer.render(document, config).getOrThrow()
        }

        println("$name (${html.length} chars of HTML, ${metrics.iterations} iterations):")
        println("  Mean: ${metrics.mean}  Median: ${metrics.median}  P95: ${metrics.p95}  Max: ${metrics.max}")
        println("  Throughput: ${metrics.throughput} docs/sec")

        assertTrue(metrics.throughput > 0.0, "$name: throughput must be positive")
        assertTrue(
            metrics.max < ceilingSeconds.seconds,
            "$name: slowest iteration ${metrics.max} exceeded the ${ceilingSeconds}s sanity ceiling",
        )
    }

    @Test
    fun `benchmark simple HTML rendering`() {
        benchmarkRender(
            name = "render-simple-html",
            document = parser.parse(BenchmarkDocuments.SMALL).document,
            config = fragmentConfig(),
            iterations = 50,
            warmupIterations = 10,
            ceilingSeconds = 10,
        )
    }

    @Test
    fun `benchmark complex HTML rendering`() {
        benchmarkRender(
            name = "render-complex-html",
            document = parser.parse(BenchmarkDocuments.COMPLEX_NESTED).document,
            config = fragmentConfig(),
            iterations = 10,
            warmupIterations = 2,
            ceilingSeconds = 30,
        )
    }

    @Test
    fun `benchmark rendering with custom theme`() {
        benchmarkRender(
            name = "render-custom-theme",
            document = parser.parse(BenchmarkDocuments.MEDIUM).document,
            config = fragmentConfig(theme = KotlinTheme()),
            iterations = 10,
            warmupIterations = 2,
            ceilingSeconds = 30,
        )
    }

    /**
     * Pending: syntax highlighting is not yet implemented — the renderer only
     * emits language classes for external highlighters, it performs no
     * highlighting itself. Remove the @Ignore once highlighting lands.
     */
    @Test
    @Ignore
    fun `benchmark rendering with syntax highlighting - IGNORED syntax highlighting not yet implemented`() {
    }

    /**
     * Pending: streaming rendering is not yet implemented — HtmlRenderer only
     * renders whole documents to a String. Remove the @Ignore once a streaming
     * render API exists.
     */
    @Test
    @Ignore
    fun `benchmark streaming rendering - IGNORED streaming rendering not yet implemented`() {
    }

    @Test
    fun `benchmark end-to-end parse and render`() {
        val renderer = newRenderer()
        val config = fragmentConfig()
        val source = BenchmarkDocuments.MEDIUM

        val metrics = runner.runBenchmark("end-to-end-parse-render", iterations = 10, warmupIterations = 2) {
            val document = parser.parse(source).document
            renderer.render(document, config).getOrThrow()
        }

        println("end-to-end-parse-render (${source.length} chars of AsciiDoc):")
        println("  Mean: ${metrics.mean}  Median: ${metrics.median}  P95: ${metrics.p95}  Max: ${metrics.max}")

        assertTrue(metrics.throughput > 0.0)
        assertTrue(metrics.max < 60.seconds, "end-to-end pipeline exceeded the 60s sanity ceiling")
    }

    @Test
    fun `benchmark rendering with baseline comparison`() {
        val renderer = newRenderer()
        val config = fragmentConfig()
        val document = parser.parse(BenchmarkDocuments.SMALL).document

        val baseline = runner.runBenchmark("render-baseline", iterations = 20, warmupIterations = 5) {
            renderer.render(document, config).getOrThrow()
        }
        val comparison = runner.runBenchmarkWithBaseline("render-baseline", baseline, iterations = 20) {
            renderer.render(document, config).getOrThrow()
        }

        println("render-baseline comparison:")
        println("  Baseline mean: ${comparison.baseline.mean}")
        println("  Current mean:  ${comparison.current.mean}")
        println("  Delta: ${comparison.meanDelta}%  regressionDetected=${comparison.regressionDetected}")

        // Structural sanity only: regression detection itself is timing-noise
        // sensitive, so it is reported but not asserted.
        assertEquals(baseline, comparison.baseline)
        assertEquals(20, comparison.current.iterations)
        assertTrue(comparison.meanDelta.isFinite())
    }
}
