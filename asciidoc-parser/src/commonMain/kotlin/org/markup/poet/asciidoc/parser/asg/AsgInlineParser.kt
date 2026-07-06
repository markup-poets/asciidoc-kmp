package org.markup.poet.asciidoc.parser.asg

import org.markup.poet.asciidoc.asg.Inline
import org.markup.poet.asciidoc.asg.InlineSpan
import org.markup.poet.asciidoc.asg.InlineText
import org.markup.poet.asciidoc.asg.Location
import org.markup.poet.asciidoc.asg.Position
import org.markup.poet.asciidoc.asg.SpanForm
import org.markup.poet.asciidoc.asg.SpanVariant

/**
 * Maps offsets in a logical inline string (block lines joined with `\n`) back
 * to 1-based, end-inclusive source positions.
 *
 * @param lineStarts logical offset at which each source line begins
 * @param sourceLines the source line numbers corresponding to [lineStarts]
 * @param startCols the 1-based source column of each line's first logical char
 */
class SegmentMap(
    private val lineStarts: List<Int>,
    private val sourceLines: List<Int>,
    private val startCols: List<Int>,
) {
    fun position(offset: Int): Position {
        var index = lineStarts.indexOfLast { it <= offset }
        if (index < 0) index = 0
        return Position(
            line = sourceLines[index],
            col = startCols[index] + (offset - lineStarts[index]),
        )
    }

    companion object {
        /** Map for a run of whole source lines starting at [firstSourceLine], each starting at column [startCol]. */
        fun ofLines(lines: List<String>, firstSourceLine: Int, startCol: Int = 1): SegmentMap {
            val lineStarts = ArrayList<Int>(lines.size)
            val sourceLines = ArrayList<Int>(lines.size)
            val startCols = ArrayList<Int>(lines.size)
            var offset = 0
            lines.forEachIndexed { i, line ->
                lineStarts.add(offset)
                sourceLines.add(firstSourceLine + i)
                startCols.add(startCol)
                offset += line.length + 1 // the joining '\n'
            }
            return SegmentMap(lineStarts, sourceLines, startCols)
        }
    }
}

/**
 * Parses inline markup within one logical string (a paragraph's lines joined
 * with `\n`), producing spans with locations that include their delimiters.
 * Plain runs coalesce into single [InlineText] nodes, `\n` included.
 */
class AsgInlineParser {

    private val spanDelimiters = mapOf(
        '*' to SpanVariant.STRONG,
        '_' to SpanVariant.EMPHASIS,
        '`' to SpanVariant.CODE,
        '#' to SpanVariant.MARK,
    )

    fun parse(text: String, map: SegmentMap): List<Inline> = parseRange(text, 0, text.length, map)

    private fun parseRange(text: String, from: Int, to: Int, map: SegmentMap): List<Inline> {
        val inlines = mutableListOf<Inline>()
        val plain = StringBuilder()
        var plainStart = -1
        var i = from

        fun flushPlain() {
            if (plain.isNotEmpty()) {
                inlines += InlineText(
                    value = plain.toString(),
                    location = Location(map.position(plainStart), map.position(i - 1)),
                )
                plain.clear()
                plainStart = -1
            }
        }

        while (i < to) {
            val c = text[i]
            if (c == '\\' && i + 1 < to && spanDelimiters.containsKey(text[i + 1])) {
                if (plain.isEmpty()) plainStart = i
                plain.append(text[i + 1])
                i += 2
                continue
            }
            val variant = spanDelimiters[c]
            if (variant != null) {
                val span = tryParseSpan(text, i, to, c, variant, map)
                if (span != null) {
                    flushPlain()
                    inlines += span.first
                    i = span.second
                    continue
                }
            }
            if (plain.isEmpty()) plainStart = i
            plain.append(c)
            i++
        }
        flushPlain()
        return inlines
    }

    /**
     * Attempts a span starting at [start]. Returns the span and the offset just
     * after its closing delimiter, or null if no valid span starts here.
     */
    private fun tryParseSpan(
        text: String,
        start: Int,
        to: Int,
        delimiter: Char,
        variant: SpanVariant,
        map: SegmentMap,
    ): Pair<InlineSpan, Int>? {
        val double = start + 1 < to && text[start + 1] == delimiter
        return if (double) {
            tryUnconstrained(text, start, to, delimiter, variant, map)
                ?: tryConstrained(text, start, to, delimiter, variant, map)
        } else {
            tryConstrained(text, start, to, delimiter, variant, map)
        }
    }

    private fun tryUnconstrained(
        text: String,
        start: Int,
        to: Int,
        delimiter: Char,
        variant: SpanVariant,
        map: SegmentMap,
    ): Pair<InlineSpan, Int>? {
        val contentStart = start + 2
        var j = contentStart
        while (j + 1 < to) {
            if (text[j] == delimiter && text[j + 1] == delimiter) {
                if (j == contentStart) return null // empty span
                val span = InlineSpan(
                    variant = variant,
                    form = SpanForm.UNCONSTRAINED,
                    inlines = parseRange(text, contentStart, j, map),
                    location = Location(map.position(start), map.position(j + 1)),
                )
                return span to (j + 2)
            }
            j++
        }
        return null
    }

    private fun tryConstrained(
        text: String,
        start: Int,
        to: Int,
        delimiter: Char,
        variant: SpanVariant,
        map: SegmentMap,
    ): Pair<InlineSpan, Int>? {
        // Opening: not preceded by a word char; followed by non-space, non-delimiter.
        val prev = if (start > 0) text[start - 1] else null
        if (prev != null && prev.isLetterOrDigit()) return null
        val contentStart = start + 1
        if (contentStart >= to) return null
        val first = text[contentStart]
        if (first == ' ' || first == '\n' || first == delimiter) return null

        var j = contentStart
        while (j < to) {
            if (text[j] == delimiter) {
                // Closing: preceded by non-space; followed by end or non-word char.
                val before = text[j - 1]
                val after = if (j + 1 < to) text[j + 1] else null
                if (before != ' ' && before != '\n' && (after == null || !after.isLetterOrDigit())) {
                    val span = InlineSpan(
                        variant = variant,
                        form = SpanForm.CONSTRAINED,
                        inlines = parseRange(text, contentStart, j, map),
                        location = Location(map.position(start), map.position(j)),
                    )
                    return span to (j + 1)
                }
            }
            j++
        }
        return null
    }
}
