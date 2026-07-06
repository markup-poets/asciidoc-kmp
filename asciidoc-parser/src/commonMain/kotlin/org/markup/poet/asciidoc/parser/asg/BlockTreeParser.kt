package org.markup.poet.asciidoc.parser.asg

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.Block
import org.markup.poet.asciidoc.asg.BlockMetadata
import org.markup.poet.asciidoc.asg.Header
import org.markup.poet.asciidoc.asg.Inline
import org.markup.poet.asciidoc.asg.InlineText
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.LeafBlockForm
import org.markup.poet.asciidoc.asg.LeafBlockName
import org.markup.poet.asciidoc.asg.ListBlock
import org.markup.poet.asciidoc.asg.ListItem
import org.markup.poet.asciidoc.asg.ListVariant
import org.markup.poet.asciidoc.asg.Location
import org.markup.poet.asciidoc.asg.ParentBlock
import org.markup.poet.asciidoc.asg.ParentBlockName
import org.markup.poet.asciidoc.asg.Position
import org.markup.poet.asciidoc.asg.SectionBlock

/**
 * Line-based recursive-descent parser producing the ASG block tree.
 *
 * A document is a header phase followed by a body of blocks. Parent (delimited)
 * blocks recurse with their closing delimiter; sections nest by heading level.
 * All locations are 1-based with end-inclusive columns (column 0 = empty line).
 */
class BlockTreeParser(
    private val inlineParser: AsgInlineParser = AsgInlineParser(),
) {

    private class Reader(source: String) {
        val lines: List<String> = source.replace("\r\n", "\n").split("\n")
        var index = 0

        val eof: Boolean get() = index >= lines.size
        fun peek(): String = lines[index]
        fun next(): String = lines[index++]

        /** Inclusive end column of a line: its length (0 for an empty line). */
        fun endCol(lineIndex: Int): Int = lines[lineIndex].length
    }

    private val headingRegex = Regex("""^(=+) (.+)$""")
    private val attributeEntryRegex = Regex("""^:([A-Za-z0-9_][A-Za-z0-9_-]*):(?: (.*))?$""")
    private val listItemRegex = Regex("""^(\*+|\.+|-) (.+)$""")
    private val blockAttributeRegex = Regex("""^\[([^\[\]]*)\]$""")

    private val verbatimDelimiters = mapOf(
        '-' to LeafBlockName.LISTING,
        '.' to LeafBlockName.LITERAL,
        '+' to LeafBlockName.PASS,
    )
    private val parentDelimiters = mapOf(
        '*' to ParentBlockName.SIDEBAR,
        '=' to ParentBlockName.EXAMPLE,
        '_' to ParentBlockName.QUOTE,
    )

    fun parseDocument(source: String): AsgDocument {
        val reader = Reader(source)
        val (header, attributes) = parseHeader(reader)
        val blocks = parseBlocks(reader, stopHeadingLevel = null, closingDelimiter = null)
        val end = blocks.lastOrNull()?.location?.end ?: header?.location?.end
        return AsgDocument(
            attributes = attributes,
            header = header,
            blocks = blocks,
            location = end?.let { Location(Position(1, 1), it) },
        )
    }

    /** Parses a bare inline snippet (the TCK's inline test mode). */
    fun parseInline(source: String): List<Inline> {
        val lines = source.replace("\r\n", "\n").split("\n").dropLastWhile { it.isEmpty() }
        val logical = lines.joinToString("\n")
        return inlineParser.parse(logical, SegmentMap.ofLines(lines, firstSourceLine = 1))
    }

    // -----------------------------------------------------------------------
    // Header
    // -----------------------------------------------------------------------

    private fun parseHeader(reader: Reader): Pair<Header?, Map<String, String>> {
        if (reader.eof) return null to emptyMap()
        val match = headingRegex.matchEntire(reader.peek()) ?: return null to emptyMap()
        if (match.groupValues[1].length != 1) return null to emptyMap() // only `=` opens a header

        val titleLineIndex = reader.index
        reader.next()
        val titleText = match.groupValues[2]
        val title = inlineParser.parse(
            titleText,
            SegmentMap.ofLines(listOf(titleText), firstSourceLine = titleLineIndex + 1, startCol = 3),
        )

        val attributes = LinkedHashMap<String, String>()
        var lastHeaderLineIndex = titleLineIndex
        while (!reader.eof) {
            val attr = attributeEntryRegex.matchEntire(reader.peek()) ?: break
            attributes[attr.groupValues[1]] = attr.groupValues[2]
            lastHeaderLineIndex = reader.index
            reader.next()
        }

        val header = Header(
            title = title,
            location = Location(
                Position(titleLineIndex + 1, 1),
                Position(lastHeaderLineIndex + 1, reader.endCol(lastHeaderLineIndex)),
            ),
        )
        return header to attributes
    }

    // -----------------------------------------------------------------------
    // Blocks
    // -----------------------------------------------------------------------

    /**
     * Parses blocks until EOF, [closingDelimiter], or — when [stopHeadingLevel]
     * is non-null — a heading of level <= that (left unconsumed for the caller).
     */
    private fun parseBlocks(reader: Reader, stopHeadingLevel: Int?, closingDelimiter: String?): List<Block> {
        val blocks = mutableListOf<Block>()
        var pendingMetadata: BlockMetadata? = null
        while (!reader.eof) {
            val line = reader.peek()
            if (line.isBlank()) {
                pendingMetadata = null // metadata only attaches to an adjacent block
                reader.next()
                continue
            }
            if (closingDelimiter != null && line == closingDelimiter) {
                return blocks // caller consumes the delimiter
            }
            val blockAttribute = blockAttributeRegex.matchEntire(line)
            if (blockAttribute != null) {
                pendingMetadata = parseBlockAttributes(blockAttribute.groupValues[1])
                reader.next()
                continue
            }
            val heading = headingRegex.matchEntire(line)
            if (heading != null) {
                val level = heading.groupValues[1].length - 1
                if (stopHeadingLevel != null && level <= stopHeadingLevel) return blocks
                pendingMetadata = null
                blocks += parseSection(reader, heading)
                continue
            }
            val delimited = tryParseDelimitedBlock(reader, pendingMetadata)
            if (delimited != null) {
                pendingMetadata = null
                blocks += delimited
                continue
            }
            if (listItemRegex.matchEntire(line) != null) {
                pendingMetadata = null
                blocks += parseList(reader)
                continue
            }
            blocks += parseParagraph(reader, closingDelimiter, pendingMetadata)
            pendingMetadata = null
        }
        return blocks
    }

    /** Parses the inside of a `[...]` attribute line into positional/named attributes. */
    private fun parseBlockAttributes(raw: String): BlockMetadata {
        if (raw.isBlank()) return BlockMetadata()
        val positional = mutableListOf<String>()
        val named = LinkedHashMap<String, String>()
        raw.split(',').forEach { part ->
            val trimmed = part.trim()
            if (trimmed.isEmpty()) return@forEach
            val eq = trimmed.indexOf('=')
            if (eq > 0) {
                named[trimmed.substring(0, eq).trim()] = trimmed.substring(eq + 1).trim().removeSurrounding("\"")
            } else {
                positional += trimmed.removeSurrounding("\"")
            }
        }
        return BlockMetadata(positional = positional, named = named)
    }

    private fun parseSection(reader: Reader, heading: MatchResult): SectionBlock {
        val headingLineIndex = reader.index
        reader.next()
        val markerLength = heading.groupValues[1].length
        val level = markerLength - 1
        val titleText = heading.groupValues[2]
        val title = inlineParser.parse(
            titleText,
            SegmentMap.ofLines(
                listOf(titleText),
                firstSourceLine = headingLineIndex + 1,
                startCol = markerLength + 2, // after "== "
            ),
        )
        val children = parseBlocks(reader, stopHeadingLevel = level, closingDelimiter = null)
        val end = children.lastOrNull()?.location?.end
            ?: Position(headingLineIndex + 1, reader.endCol(headingLineIndex))
        return SectionBlock(
            title = title,
            level = level,
            blocks = children,
            location = Location(Position(headingLineIndex + 1, 1), end),
        )
    }

    private fun isDelimiterLine(line: String): Boolean {
        if (line == "--") return true
        if (line.length < 4) return false
        val c = line[0]
        if (c !in verbatimDelimiters && c !in parentDelimiters) return false
        return line.all { it == c }
    }

    private fun tryParseDelimitedBlock(reader: Reader, metadata: BlockMetadata? = null): Block? {
        val line = reader.peek()
        if (!isDelimiterLine(line)) return null
        val openLineIndex = reader.index
        reader.next()

        val verbatim = if (line == "--") null else verbatimDelimiters[line[0]]
        if (verbatim != null) {
            val contentLines = mutableListOf<String>()
            var closeLineIndex = -1
            while (!reader.eof) {
                if (reader.peek() == line) {
                    closeLineIndex = reader.index
                    reader.next()
                    break
                }
                contentLines.add(reader.next())
            }
            val endLineIndex = if (closeLineIndex >= 0) closeLineIndex else reader.lines.size - 1
            val inlines = if (contentLines.isEmpty()) emptyList() else listOf(
                InlineText(
                    value = contentLines.joinToString("\n"),
                    location = Location(
                        Position(openLineIndex + 2, 1),
                        Position(openLineIndex + 1 + contentLines.size, contentLines.last().length),
                    ),
                ),
            )
            return LeafBlock(
                name = verbatim,
                form = LeafBlockForm.DELIMITED,
                delimiter = line,
                inlines = inlines,
                metadata = metadata,
                location = Location(
                    Position(openLineIndex + 1, 1),
                    Position(endLineIndex + 1, reader.endCol(endLineIndex)),
                ),
            )
        }

        val parentName = if (line == "--") ParentBlockName.OPEN else parentDelimiters.getValue(line[0])
        val children = parseBlocks(reader, stopHeadingLevel = null, closingDelimiter = line)
        var closeLineIndex = reader.lines.size - 1
        if (!reader.eof && reader.peek() == line) {
            closeLineIndex = reader.index
            reader.next()
        }
        return ParentBlock(
            name = parentName,
            delimiter = line,
            blocks = children,
            metadata = metadata,
            location = Location(
                Position(openLineIndex + 1, 1),
                Position(closeLineIndex + 1, reader.endCol(closeLineIndex)),
            ),
        )
    }

    private fun parseList(reader: Reader): ListBlock {
        val items = mutableListOf<ListItem>()
        var marker: String? = null
        while (!reader.eof) {
            val match = listItemRegex.matchEntire(reader.peek()) ?: break
            val itemMarker = match.groupValues[1]
            if (marker == null) marker = itemMarker
            if (itemMarker != marker) break // different marker: separate list (nesting comes later)
            val lineIndex = reader.index
            reader.next()
            val text = match.groupValues[2]
            val principal = inlineParser.parse(
                text,
                SegmentMap.ofLines(
                    listOf(text),
                    firstSourceLine = lineIndex + 1,
                    startCol = itemMarker.length + 2,
                ),
            )
            items += ListItem(
                marker = itemMarker,
                principal = principal,
                location = Location(
                    Position(lineIndex + 1, 1),
                    Position(lineIndex + 1, reader.endCol(lineIndex)),
                ),
            )
        }
        val resolvedMarker = marker ?: "*"
        return ListBlock(
            variant = if (resolvedMarker.startsWith(".")) ListVariant.ORDERED else ListVariant.UNORDERED,
            marker = resolvedMarker,
            items = items,
            location = Location(items.first().location!!.start, items.last().location!!.end),
        )
    }

    private fun parseParagraph(reader: Reader, closingDelimiter: String?, metadata: BlockMetadata? = null): LeafBlock {
        val startLineIndex = reader.index
        val lines = mutableListOf<String>()
        while (!reader.eof) {
            val line = reader.peek()
            if (line.isBlank()) break
            if (closingDelimiter != null && line == closingDelimiter) break
            if (isDelimiterLine(line)) break
            if (headingRegex.matchEntire(line) != null) break
            if (listItemRegex.matchEntire(line) != null) break
            if (blockAttributeRegex.matchEntire(line) != null) break
            lines.add(reader.next())
        }
        val logical = lines.joinToString("\n")
        val inlines = inlineParser.parse(logical, SegmentMap.ofLines(lines, firstSourceLine = startLineIndex + 1))
        val lastLineIndex = startLineIndex + lines.size - 1
        return LeafBlock(
            name = LeafBlockName.PARAGRAPH,
            form = LeafBlockForm.PARAGRAPH,
            inlines = inlines,
            metadata = metadata,
            location = Location(
                Position(startLineIndex + 1, 1),
                Position(lastLineIndex + 1, reader.endCol(lastLineIndex)),
            ),
        )
    }
}
