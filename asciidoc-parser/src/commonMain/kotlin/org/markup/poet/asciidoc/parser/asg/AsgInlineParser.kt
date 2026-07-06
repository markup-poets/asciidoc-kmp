package org.markup.poet.asciidoc.parser.asg

import org.markup.poet.asciidoc.asg.Inline
import org.markup.poet.asciidoc.asg.InlineMacro
import org.markup.poet.asciidoc.asg.InlineRef
import org.markup.poet.asciidoc.asg.InlineSpan
import org.markup.poet.asciidoc.asg.InlineText
import org.markup.poet.asciidoc.asg.Location
import org.markup.poet.asciidoc.asg.Position
import org.markup.poet.asciidoc.asg.RefVariant
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
 *
 * With [attributes], `{name}` references to defined document attributes are
 * substituted (the resulting text node keeps the reference's source span);
 * undefined references stay literal text.
 */
class AsgInlineParser(
    private val attributes: Map<String, String> = emptyMap(),
) {

    private val spanDelimiters = mapOf(
        '*' to SpanVariant.STRONG,
        '_' to SpanVariant.EMPHASIS,
        '`' to SpanVariant.CODE,
        '#' to SpanVariant.MARK,
    )

    /** Bare URL schemes are autolink territory, not inline macros. */
    private val excludedMacroNames = setOf("http", "https", "ftp", "irc", "mailto")

    private val autolinkSchemes = listOf("https://", "http://", "ftp://", "irc://")

    private val attributeNameRegex = Regex("""^[A-Za-z0-9_][A-Za-z0-9_-]*$""")

    fun parse(text: String, map: SegmentMap): List<Inline> = parseRange(text, 0, text.length, map)

    private fun parseRange(
        text: String,
        from: Int,
        to: Int,
        map: SegmentMap,
        inCode: Boolean = false,
    ): List<Inline> {
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

        fun consume(parsed: Pair<Inline, Int>) {
            flushPlain()
            inlines += parsed.first
            i = parsed.second
        }

        while (i < to) {
            val c = text[i]
            if (c == '\\' && i + 1 < to && (spanDelimiters.containsKey(text[i + 1]) || text[i + 1] == '{')) {
                if (plain.isEmpty()) plainStart = i
                plain.append(text[i + 1])
                i += 2
                continue
            }
            val variant = spanDelimiters[c]
            if (variant != null) {
                val span = tryParseSpan(text, i, to, c, variant, map, inCode)
                if (span != null) {
                    consume(span)
                    continue
                }
            }
            if (c == '<' && !inCode) {
                val xref = tryParseXref(text, i, to, map)
                if (xref != null) {
                    consume(xref)
                    continue
                }
            }
            if (c == '{') {
                val reference = tryParseAttributeReference(text, i, to, map)
                if (reference != null) {
                    consume(reference)
                    continue
                }
            }
            if (c.isLetter() && (i == from || !text[i - 1].isLetterOrDigit())) {
                val autolink = tryParseAutolink(text, i, to, map, inCode)
                if (autolink != null) {
                    consume(autolink)
                    continue
                }
                val macro = tryParseInlineMacro(text, i, to, map)
                if (macro != null) {
                    consume(macro)
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
     * Substitutes `{name}` when the attribute is defined. The produced text
     * node's value is the attribute value while its location spans the
     * reference in the source.
     */
    private fun tryParseAttributeReference(
        text: String,
        start: Int,
        to: Int,
        map: SegmentMap,
    ): Pair<Inline, Int>? {
        val close = text.indexOf('}', start + 1)
        if (close < 0 || close >= to) return null
        val name = text.substring(start + 1, close)
        if (!attributeNameRegex.matches(name)) return null
        val value = attributes[name] ?: return null
        val node = InlineText(
            value = value,
            location = Location(map.position(start), map.position(close)),
        )
        return node to (close + 1)
    }

    /**
     * A bare URL (`https://...`) becomes a link ref. `url[text]` uses the
     * bracketed text as the link content; a bare URL links its own text.
     */
    private fun tryParseAutolink(
        text: String,
        start: Int,
        to: Int,
        map: SegmentMap,
        inCode: Boolean = false,
    ): Pair<Inline, Int>? {
        val scheme = autolinkSchemes.firstOrNull { text.startsWith(it, start) && start + it.length < to }
            ?: return null
        var j = start + scheme.length
        while (j < to && text[j] != ' ' && text[j] != '\n' && text[j] != '[' && text[j] != ']') j++
        if (j == start + scheme.length) return null
        // Trailing punctuation stays outside the link.
        var end = j
        while (end > start + scheme.length && text[end - 1] in ".,;:!?") end--
        val url = text.substring(start, end)

        if (end < to && text[end] == '[') {
            val close = text.indexOf(']', end + 1)
            if (close in (end + 1) until to) {
                val label = text.substring(end + 1, close)
                val labelInlines = if (label.isEmpty()) {
                    listOf(InlineText(url, Location(map.position(start), map.position(end - 1))))
                } else {
                    parseRange(text, end + 1, close, map, inCode)
                }
                val ref = InlineRef(
                    variant = RefVariant.LINK,
                    target = url,
                    inlines = labelInlines,
                    location = Location(map.position(start), map.position(close)),
                )
                return ref to (close + 1)
            }
        }

        val location = Location(map.position(start), map.position(end - 1))
        val ref = InlineRef(
            variant = RefVariant.LINK,
            target = url,
            inlines = listOf(InlineText(url, location)),
            location = location,
        )
        return ref to end
    }

    /**
     * Attempts an inline macro `name:target[attrlist]` starting at [start]
     * (which must be a letter at a word boundary). Returns the macro and the
     * offset just after the closing `]`, or null.
     */
    private fun tryParseInlineMacro(
        text: String,
        start: Int,
        to: Int,
        map: SegmentMap,
    ): Pair<InlineMacro, Int>? {
        var i = start
        while (i < to && (text[i].isLetterOrDigit() || text[i] == '_' || text[i] == '-')) i++
        if (i >= to || text[i] != ':' || i == start) return null
        val name = text.substring(start, i)
        if (name in excludedMacroNames) return null

        val targetStart = i + 1
        var j = targetStart
        while (j < to && text[j] != '[' && text[j] != ' ' && text[j] != '\n' && text[j] != ':') j++
        if (j >= to || text[j] != '[') return null
        val target = text.substring(targetStart, j)

        val attrEnd = text.indexOf(']', j + 1)
        if (attrEnd < 0 || attrEnd >= to) return null
        val attrlist = text.substring(j + 1, attrEnd)

        val positional = mutableListOf<String>()
        val named = LinkedHashMap<String, String>()
        if (attrlist.isNotBlank()) {
            attrlist.split(',').forEach { part ->
                val trimmed = part.trim()
                if (trimmed.isEmpty()) return@forEach
                val eq = trimmed.indexOf('=')
                if (eq > 0) {
                    named[trimmed.substring(0, eq).trim()] = trimmed.substring(eq + 1).trim().removeSurrounding("\"")
                } else {
                    positional += trimmed.removeSurrounding("\"")
                }
            }
        }

        val macro = InlineMacro(
            name = name,
            target = target,
            positional = positional,
            named = named,
            location = Location(map.position(start), map.position(attrEnd)),
        )
        return macro to (attrEnd + 1)
    }

    /**
     * An xref shorthand `<<id>>` or `<<id,custom text>>` starting at [start]
     * (which must be a `<`). The location includes the `<<`/`>>` delimiters.
     * `<<<` (a page-break line, handled at block level) never opens an xref.
     */
    private fun tryParseXref(
        text: String,
        start: Int,
        to: Int,
        map: SegmentMap,
    ): Pair<Inline, Int>? {
        if (start + 1 >= to || text[start + 1] != '<') return null
        // `<<<` (page break) never opens an xref, whichever `<` we start from.
        if (start + 2 < to && text[start + 2] == '<') return null
        if (start > 0 && text[start - 1] == '<') return null
        val close = text.indexOf(">>", start + 2)
        if (close < 0 || close + 1 >= to) return null
        val inner = text.substring(start + 2, close)
        if (inner.isEmpty() || inner.contains('\n') || inner.contains('<')) return null
        val comma = inner.indexOf(',')
        val target = if (comma < 0) inner else inner.substring(0, comma)
        if (target.isEmpty()) return null
        val label = if (comma < 0) "" else inner.substring(comma + 1)
        val inlines = if (label.isEmpty()) {
            emptyList()
        } else {
            listOf(
                InlineText(
                    value = label,
                    location = Location(map.position(start + 2 + comma + 1), map.position(close - 1)),
                ),
            )
        }
        val ref = InlineRef(
            variant = RefVariant.XREF,
            target = target,
            inlines = inlines,
            location = Location(map.position(start), map.position(close + 1)),
        )
        return ref to (close + 2)
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
        inCode: Boolean,
    ): Pair<InlineSpan, Int>? {
        val double = start + 1 < to && text[start + 1] == delimiter
        return if (double) {
            tryUnconstrained(text, start, to, delimiter, variant, map, inCode)
                ?: tryConstrained(text, start, to, delimiter, variant, map, inCode)
        } else {
            tryConstrained(text, start, to, delimiter, variant, map, inCode)
        }
    }

    private fun tryUnconstrained(
        text: String,
        start: Int,
        to: Int,
        delimiter: Char,
        variant: SpanVariant,
        map: SegmentMap,
        inCode: Boolean,
    ): Pair<InlineSpan, Int>? {
        val contentStart = start + 2
        var j = contentStart
        while (j + 1 < to) {
            if (text[j] == delimiter && text[j + 1] == delimiter) {
                if (j == contentStart) return null // empty span
                val span = InlineSpan(
                    variant = variant,
                    form = SpanForm.UNCONSTRAINED,
                    inlines = parseRange(text, contentStart, j, map, inCode || variant == SpanVariant.CODE),
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
        inCode: Boolean,
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
                        inlines = parseRange(text, contentStart, j, map, inCode || variant == SpanVariant.CODE),
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
