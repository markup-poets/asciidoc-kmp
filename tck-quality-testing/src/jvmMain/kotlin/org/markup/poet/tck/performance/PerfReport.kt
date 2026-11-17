package org.markup.poet.tck.performance

import org.markup.poet.asciidoc.parser.DefaultAsciidocParser
import org.markup.poet.asciidoc.render.DefaultBlockRenderer
import org.markup.poet.asciidoc.render.DefaultHtmlBuilder
import org.markup.poet.asciidoc.render.DefaultHtmlEscaper
import org.markup.poet.asciidoc.render.DefaultHtmlRenderer
import org.markup.poet.asciidoc.render.DefaultInlineRenderer
import org.markup.poet.asciidoc.render.RenderConfig
import java.io.File
import java.util.Locale

/**
 * Performance report for the parser and the HTML renderer (issue #138).
 *
 * Run with `./gradlew :tck-quality-testing:perfReport`. Measures, per corpus
 * document, parse time and parse + render time on the JVM after warm-up, and
 * writes `perf-report.json` and `perf-report.md` to the output directory.
 *
 * The numbers describe one machine and one JVM: they are for tracking this
 * project against itself over time, not for comparing with other processors
 * measured elsewhere.
 */
private const val WARMUP_ITERATIONS = 30
private const val WARMUP_MIN_NANOS = 500_000_000L
private const val MEASURED_ITERATIONS = 60

@Volatile
private var blackhole = 0

private data class Document(val name: String, val source: String)

private data class Timing(val medianMs: Double, val p95Ms: Double, val megabytesPerSecond: Double)

private data class Row(val document: Document, val bytes: Int, val parse: Timing, val parseAndRender: Timing)

fun main(args: Array<String>) {
    val outputDir = File(args.getOrElse(0) { "tck-quality-testing/build/perf-report" }).apply { mkdirs() }
    val corpus = corpus()

    val parser = DefaultAsciidocParser()
    val escaper = DefaultHtmlEscaper()
    val builder = DefaultHtmlBuilder(escaper)
    val inlineRenderer = DefaultInlineRenderer(builder)
    val renderer = DefaultHtmlRenderer(DefaultBlockRenderer(builder, inlineRenderer), inlineRenderer)

    val rows = corpus.map { document ->
        val bytes = document.source.toByteArray().size
        Row(
            document = document,
            bytes = bytes,
            parse = measure(bytes) { parser.parse(document.source) },
            parseAndRender = measure(bytes) {
                renderer.render(parser.parse(document.source).document, RenderConfig()).getOrThrow()
            },
        )
    }

    val environment = "JVM ${System.getProperty("java.version")} (${System.getProperty("java.vm.name")}), " +
        "${System.getProperty("os.name")} ${System.getProperty("os.arch")}, " +
        "${Runtime.getRuntime().availableProcessors()} cores"
    val version = System.getProperty("markup.poet.version") ?: "unknown"

    File(outputDir, "perf-report.md").writeText(markdown(rows, environment, version))
    File(outputDir, "perf-report.json").writeText(json(rows, environment, version))
    println(markdown(rows, environment, version))
    println("Written to ${outputDir.path}")
}

/**
 * The measured documents: the official TCK inputs (when the TCK is synced),
 * a kitchen-sink document exercising most constructs, and generated documents
 * of increasing size.
 */
private fun corpus(): List<Document> {
    val documents = mutableListOf<Document>()

    val tckTests = File("tck-quality-testing/official-tck/repository/tests")
    if (tckTests.isDirectory) {
        val inputs = tckTests.walkTopDown().filter { it.name.endsWith("-input.adoc") }.sortedBy { it.path }.toList()
        if (inputs.isNotEmpty()) {
            documents += Document(
                "official TCK inputs (${inputs.size} files, concatenated)",
                inputs.joinToString("\n\n") { it.readText().trimEnd() } + "\n",
            )
        }
    }

    val kitchenSink = File("tck-quality-testing/perf/kitchen-sink.adoc")
    if (kitchenSink.isFile) documents += Document("kitchen sink (issue #139 demo)", kitchenSink.readText())

    documents += Document("generated, 40 sections", generate(sections = 40))
    documents += Document("generated, 250 sections", generate(sections = 250))
    documents += Document("generated, 1000 sections", generate(sections = 1000))
    return documents
}

/** A deterministic document mixing paragraphs, inline markup, lists, listings and tables. */
private fun generate(sections: Int): String = buildString {
    appendLine("= Generated document")
    appendLine()
    for (section in 1..sections) {
        appendLine("== Section $section")
        appendLine()
        repeat(4) { paragraph ->
            appendLine(
                "Paragraph $paragraph of section $section with *strong*, _emphasis_, `code`, " +
                    "a https://example.org/$section[link] and plain text to give the inline parser work."
            )
            appendLine()
        }
        appendLine("* first item")
        appendLine("* second item with `code`")
        appendLine("** nested item")
        appendLine()
        appendLine("[source,kotlin]")
        appendLine("----")
        appendLine("fun f$section() = $section")
        appendLine("----")
        appendLine()
        if (section % 5 == 0) {
            appendLine("|===")
            appendLine("|Name |Value")
            appendLine("|a |$section")
            appendLine("|b |${section * 2}")
            appendLine("|===")
            appendLine()
        }
    }
}

private inline fun measure(bytes: Int, block: () -> Any?): Timing {
    var sink = 0
    // Warm up for a minimum number of runs AND a minimum time: tiny documents
    // need thousands of runs before the JIT settles.
    val warmupDeadline = System.nanoTime() + WARMUP_MIN_NANOS
    var warmups = 0
    while (warmups < WARMUP_ITERATIONS || System.nanoTime() < warmupDeadline) {
        sink += block().hashCode()
        warmups++
    }
    val nanos = LongArray(MEASURED_ITERATIONS)
    for (i in nanos.indices) {
        val start = System.nanoTime()
        sink += block().hashCode()
        nanos[i] = System.nanoTime() - start
    }
    blackhole = sink // keeps the results observable to the JIT
    nanos.sort()
    val median = nanos[nanos.size / 2] / 1e6
    val p95 = nanos[(nanos.size * 95 / 100).coerceAtMost(nanos.size - 1)] / 1e6
    val megabytesPerSecond = if (median > 0) bytes / 1e6 / (median / 1e3) else 0.0
    return Timing(median, p95, megabytesPerSecond)
}

private fun Double.fmt(digits: Int = 3) = String.format(Locale.ROOT, "%.${digits}f", this)

private fun markdown(rows: List<Row>, environment: String, version: String): String = buildString {
    appendLine("# Performance report")
    appendLine()
    appendLine("Markup Poet $version — $environment. Median and 95th percentile of " +
        "$MEASURED_ITERATIONS runs after warm-up (at least $WARMUP_ITERATIONS runs and 0.5 s).")
    appendLine()
    appendLine("| Document | Size (bytes) | Parse median (ms) | Parse p95 (ms) | Parse (MB/s) | Parse + HTML median (ms) | Parse + HTML p95 (ms) | Parse + HTML (MB/s) |")
    appendLine("|---|---:|---:|---:|---:|---:|---:|---:|")
    rows.forEach { row ->
        appendLine(
            "| ${row.document.name} | ${row.bytes} | ${row.parse.medianMs.fmt()} | ${row.parse.p95Ms.fmt()} | " +
                "${row.parse.megabytesPerSecond.fmt(1)} | ${row.parseAndRender.medianMs.fmt()} | " +
                "${row.parseAndRender.p95Ms.fmt()} | ${row.parseAndRender.megabytesPerSecond.fmt(1)} |"
        )
    }
    appendLine()
    appendLine("Numbers describe this machine and JVM only; they are not comparable with other " +
        "processors measured elsewhere.")
}

private fun json(rows: List<Row>, environment: String, version: String): String {
    fun String.quoted() = "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""
    fun Timing.json() =
        "{\"medianMs\": ${medianMs.fmt(4)}, \"p95Ms\": ${p95Ms.fmt(4)}, \"megabytesPerSecond\": ${megabytesPerSecond.fmt(2)}}"
    val documents = rows.joinToString(",\n") { row ->
        "    {\"name\": ${row.document.name.quoted()}, \"bytes\": ${row.bytes}, " +
            "\"parse\": ${row.parse.json()}, \"parseAndRender\": ${row.parseAndRender.json()}}"
    }
    return "{\n  \"libraryVersion\": ${version.quoted()},\n  \"environment\": ${environment.quoted()},\n" +
        "  \"warmupIterations\": $WARMUP_ITERATIONS,\n  \"measuredIterations\": $MEASURED_ITERATIONS,\n" +
        "  \"documents\": [\n$documents\n  ]\n}\n"
}
