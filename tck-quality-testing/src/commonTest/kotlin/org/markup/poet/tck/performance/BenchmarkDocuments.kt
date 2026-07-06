package org.markup.poet.tck.performance

/**
 * Generated AsciiDoc documents for the parsing/rendering benchmarks.
 *
 * Sizes are approximate: SMALL well under 1 KB, MEDIUM in the tens of KB,
 * LARGE a few hundred KB. Content exercises the common block and inline
 * constructs the parser supports today.
 */
internal object BenchmarkDocuments {

    val SMALL: String = buildString {
        appendLine("= Small Document")
        appendLine()
        appendLine("A short paragraph with *bold*, _italic_ and `mono` text.")
        appendLine()
        appendLine("* item one")
        appendLine("* item two")
    }

    val MEDIUM: String = generateDocument(sections = 40, paragraphsPerSection = 5)

    val LARGE: String = generateDocument(sections = 250, paragraphsPerSection = 6)

    /** Deeply nested lists plus nested delimited blocks. */
    val COMPLEX_NESTED: String = buildString {
        appendLine("= Complex Nesting")
        repeat(50) { i ->
            appendLine()
            appendLine("== Section $i")
            appendLine()
            appendLine("* level 1 item $i")
            appendLine("** level 2 item")
            appendLine("*** level 3 item")
            appendLine("**** level 4 item")
            appendLine("***** level 5 item")
            appendLine()
            appendLine("====")
            appendLine("Example content $i.")
            appendLine()
            appendLine("____")
            appendLine("A nested quotation.")
            appendLine("____")
            appendLine("====")
            appendLine()
            appendLine("[source,kotlin]")
            appendLine("----")
            appendLine("fun f$i() = $i")
            appendLine("----")
        }
    }

    /** Thousands of inline formatting spans. */
    val INLINE_HEAVY: String = buildString {
        appendLine("= Inline Heavy")
        repeat(400) { i ->
            appendLine()
            appendLine(
                "Paragraph $i has *bold $i*, _italic ${i}_, `mono $i`, #mark $i#, " +
                    "a https://example.com/$i[link $i] and a <<ref-$i>> reference."
            )
        }
    }

    fun generateDocument(sections: Int, paragraphsPerSection: Int): String = buildString {
        appendLine("= Generated Benchmark Document")
        appendLine(":version: 1.0")
        for (s in 1..sections) {
            appendLine()
            appendLine("== Section $s")
            for (p in 1..paragraphsPerSection) {
                appendLine()
                appendLine(
                    "Paragraph $p of section $s contains *strong* and _emphasized_ words, " +
                        "some `code`, and enough plain filler text to give the parser real work to do " +
                        "across multiple clauses and sentences of ordinary prose."
                )
            }
            appendLine()
            appendLine("* first point of section $s")
            appendLine("* second point of section $s")
            appendLine()
            appendLine("----")
            appendLine("listing content for section $s")
            appendLine("----")
        }
    }
}
