package org.markup.poet.asciidoc.parser.asg

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.Block
import org.markup.poet.asciidoc.asg.BlockMacro
import org.markup.poet.asciidoc.asg.BlockMacroName
import org.markup.poet.asciidoc.asg.BlockMetadata
import org.markup.poet.asciidoc.asg.BreakBlock
import org.markup.poet.asciidoc.asg.BreakVariant
import org.markup.poet.asciidoc.asg.ConditionalBlock
import org.markup.poet.asciidoc.asg.ConditionalVariant
import org.markup.poet.asciidoc.asg.CustomBlockMacro
import org.markup.poet.asciidoc.asg.DListBlock
import org.markup.poet.asciidoc.asg.DListItem
import org.markup.poet.asciidoc.asg.DiscreteHeading
import org.markup.poet.asciidoc.asg.Header
import org.markup.poet.asciidoc.asg.IncludeBlock
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
import org.markup.poet.asciidoc.asg.TableBlock
import org.markup.poet.asciidoc.asg.TableCell
import org.markup.poet.asciidoc.asg.TableColumn
import org.markup.poet.asciidoc.asg.TableColumnAlignment
import org.markup.poet.asciidoc.asg.TableRow

/**
 * Line-based recursive-descent parser producing the ASG block tree.
 *
 * A document is a header phase followed by a body of blocks. Parent (delimited)
 * blocks recurse with their closing delimiter; sections nest by heading level;
 * lists nest by marker depth. All locations are 1-based with end-inclusive
 * columns (column 0 = empty line). Attribute references (`{name}`) to header
 * attributes are substituted during inline parsing.
 */
class BlockTreeParser {

    fun parseDocument(source: String): AsgDocument {
        val reader = Reader(source)
        val headerRun = ParseRun(reader, AsgInlineParser())
        val (header, attributes) = headerRun.parseHeader()
        val run = ParseRun(reader, AsgInlineParser(attributes))
        val blocks = run.parseBlocks(stopHeadingLevel = null, closingDelimiter = null)
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
        return AsgInlineParser().parse(logical, SegmentMap.ofLines(lines, firstSourceLine = 1))
    }

    // -----------------------------------------------------------------------
    // Infrastructure
    // -----------------------------------------------------------------------

    private class Reader(source: String) {
        val lines: List<String> = source.replace("\r\n", "\n").split("\n")
        var index = 0

        val eof: Boolean get() = index >= lines.size
        fun peek(): String = lines[index]
        fun peekAt(offset: Int): String? = lines.getOrNull(index + offset)
        fun next(): String = lines[index++]

        /** Inclusive end column of a line: its length (0 for an empty line). */
        fun endCol(lineIndex: Int): Int = lines[lineIndex].length
    }

    private companion object {
        val headingRegex = Regex("""^(=+) (.+)$""")

        /**
         * The deepest heading marker that still opens a section: `======`
         * (document title `=` + section levels 1-5, matching asciidoctor's cap).
         * Lines with 7+ markers are not headings and fall through to paragraphs.
         */
        const val MAX_HEADING_MARKERS = 6
        val attributeEntryRegex = Regex("""^:([A-Za-z0-9_][A-Za-z0-9_-]*):(?: (.*))?$""")
        val listItemRegex = Regex("""^(\*+|\.+|-|\d+\.) (.+)$""")
        val dlistItemRegex = Regex("""^([^\s:].*?)(::)(?: (.+))?$""")
        val blockAttributeRegex = Regex("""^\[([^\[\]]*)\]$""")
        val blockTitleRegex = Regex("""^\.([^\s.].*)$""")
        val thematicBreakRegex = Regex("""^'{3,}$""")
        val blockMacroRegex = Regex("""^([a-z][a-z0-9_-]*)::([^\[\s]*)\[([^\]]*)\]$""")
        const val PAGE_BREAK = "<<<"

        // Processing directives (block level). These share the block-macro shape
        // (`name::target[attrs]`) and must be recognized before the generic
        // blockMacro/dlist checks so they are not eaten by them.
        val includeRegex = Regex("""^include::([^\[\s]+)\[(.*)\]$""")
        val conditionalRegex = Regex("""^(ifdef|ifndef)::([^\[\s]+)\[(.*)\]$""")
        val ifevalRegex = Regex("""^ifeval::\[(.*)\]$""")
        val endifRegex = Regex("""^endif::([^\[\s]*)\[\]$""")
        val includeLineRangeRegex = Regex("""^(\d+)\.\.(\d+)$""")

        /** A callout-list line: `<1> text` or `<.> text`. */
        val calloutItemRegex = Regex("""^(<(?:\d+|\.)>) (.+)$""")

        /** `|===` (3+ `=`) opens and closes a table. */
        val tableDelimiterRegex = Regex("""^\|={3,}$""")

        /** A table line that starts cells: optional `N+` span spec, then `|`. */
        val tableRowStartRegex = Regex("""^(\d+\+)?\|""")

        /** A `N+` column-span spec at the end of the text preceding a `|` boundary. */
        val tableCellSpanSpecRegex = Regex("""(?:^|\s)(\d+)\+$""")

        /** One item of a `cols` attribute: `[repeat*][alignment][width]`. */
        val tableColsItemRegex = Regex("""^(?:(\d+)\*)?([<^>])?(\d+)?$""")

        /**
         * The author line directly below the document title: 2+ capitalized
         * words, or 1+ words plus an `<email>`. The shape gate (capitalization,
         * email) keeps ordinary body text from being mistaken for an author.
         */
        val authorLineRegex = Regex("""^([A-Z][A-Za-z0-9.'-]*(?: [A-Z][A-Za-z0-9.'-]*)*)(?: <([^<>@\s]+@[^<>\s]+)>)?$""")

        /** A list-continuation line: `+` alone attaches the next block to the item. */
        const val LIST_CONTINUATION = "+"

        val admonitionParagraphRegex = Regex("""^(NOTE|TIP|IMPORTANT|WARNING|CAUTION): (.+)$""")
        val admonitionNames = setOf("NOTE", "TIP", "IMPORTANT", "WARNING", "CAUTION")
        val blockMacroNames = mapOf(
            "audio" to BlockMacroName.AUDIO,
            "video" to BlockMacroName.VIDEO,
            "image" to BlockMacroName.IMAGE,
            "toc" to BlockMacroName.TOC,
        )

        /**
         * Names that share the block-macro shape but are processing directives,
         * never (custom) block macros. Their well-formed occurrences are matched
         * by the directive regexes before the block-macro check; malformed ones
         * (e.g. `include::[]` with an empty target) keep falling through to
         * paragraph parsing.
         */
        val processingDirectiveNames = setOf("include", "ifdef", "ifndef", "ifeval", "endif")

        val verbatimDelimiters = mapOf(
            '-' to LeafBlockName.LISTING,
            '.' to LeafBlockName.LITERAL,
            '+' to LeafBlockName.PASS,
        )
        val parentDelimiters = mapOf(
            '*' to ParentBlockName.SIDEBAR,
            '=' to ParentBlockName.EXAMPLE,
            '_' to ParentBlockName.QUOTE,
        )
    }

    // -----------------------------------------------------------------------
    // One parse pass (holds the attribute-aware inline parser)
    // -----------------------------------------------------------------------

    private inner class ParseRun(
        private val reader: Reader,
        private val inlineParser: AsgInlineParser,
    ) {

        // -------------------------------------------------------------------
        // Header
        // -------------------------------------------------------------------

        fun parseHeader(): Pair<Header?, Map<String, String>> {
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

            // Author line: the first non-attribute line directly below the title,
            // when it has an author shape, becomes author metadata (never a paragraph).
            if (!reader.eof && attributeEntryRegex.matchEntire(reader.peek()) == null) {
                val author = authorLineRegex.matchEntire(reader.peek())
                val email = author?.groupValues?.get(2).orEmpty()
                if (author != null && (email.isNotEmpty() || author.groupValues[1].contains(' '))) {
                    attributes += authorAttributes(author.groupValues[1], email)
                    lastHeaderLineIndex = reader.index
                    reader.next()
                }
            }

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

        /** Splits an author line into the implicit author attributes. */
        private fun authorAttributes(fullName: String, email: String): Map<String, String> {
            val attributes = LinkedHashMap<String, String>()
            attributes["author"] = fullName
            if (email.isNotEmpty()) attributes["email"] = email
            val names = fullName.split(' ')
            attributes["firstname"] = names.first()
            if (names.size > 1) {
                attributes["lastname"] = names.last()
                if (names.size > 2) attributes["middlename"] = names.subList(1, names.size - 1).joinToString(" ")
            }
            attributes["authorinitials"] = names.take(3).map { it.first() }.joinToString("")
            return attributes
        }

        // -------------------------------------------------------------------
        // Blocks
        // -------------------------------------------------------------------

        /**
         * Parses blocks until EOF, [closingDelimiter], or — when
         * [stopHeadingLevel] is non-null — a heading of level <= that
         * (left unconsumed for the caller). With [stopAtEndif] an `endif::[]`
         * line also ends the run (left for the enclosing conditional).
         */
        fun parseBlocks(stopHeadingLevel: Int?, closingDelimiter: String?, stopAtEndif: Boolean = false): List<Block> {
            val blocks = mutableListOf<Block>()
            var pending: BlockMetadata? = null
            while (!reader.eof) {
                val line = reader.peek()
                if (line.isBlank()) {
                    pending = null // metadata only attaches to an adjacent block
                    reader.next()
                    continue
                }
                if (closingDelimiter != null && line == closingDelimiter) {
                    return blocks // caller consumes the delimiter
                }
                if (endifRegex.matchEntire(line) != null) {
                    if (stopAtEndif) return blocks // enclosing conditional consumes it
                    reader.next() // stray endif: drop it (lenient, parser never errors)
                    pending = null
                    continue
                }

                val blockAttribute = blockAttributeRegex.matchEntire(line)
                if (blockAttribute != null) {
                    pending = mergeMetadata(pending, parseBlockAttributes(blockAttribute.groupValues[1]))
                    reader.next()
                    continue
                }
                val blockTitle = blockTitleRegex.matchEntire(line)
                if (blockTitle != null && !isStructuralLine(line)) {
                    val titleText = blockTitle.groupValues[1]
                    val title = inlineParser.parse(
                        titleText,
                        SegmentMap.ofLines(listOf(titleText), firstSourceLine = reader.index + 1, startCol = 2),
                    )
                    pending = (pending ?: BlockMetadata()).copy(title = title)
                    reader.next()
                    continue
                }

                if (thematicBreakRegex.matches(line) || line == PAGE_BREAK) {
                    val lineIndex = reader.index
                    reader.next()
                    blocks += BreakBlock(
                        variant = if (line == PAGE_BREAK) BreakVariant.PAGE else BreakVariant.THEMATIC,
                        location = lineLocation(lineIndex),
                    )
                    pending = null
                    continue
                }

                val include = includeRegex.matchEntire(line)
                if (include != null) {
                    blocks += parseInclude(include)
                    pending = null
                    continue
                }
                val conditional = conditionalRegex.matchEntire(line)
                if (conditional != null) {
                    blocks += parseConditional(
                        variant = if (conditional.groupValues[1] == "ifdef") {
                            ConditionalVariant.IFDEF
                        } else {
                            ConditionalVariant.IFNDEF
                        },
                        condition = conditional.groupValues[2],
                        inlineContent = conditional.groupValues[3],
                        closingDelimiter = closingDelimiter,
                    )
                    pending = null
                    continue
                }
                val ifeval = ifevalRegex.matchEntire(line)
                if (ifeval != null) {
                    blocks += parseConditional(
                        variant = ConditionalVariant.IFEVAL,
                        condition = ifeval.groupValues[1],
                        inlineContent = "", // ifeval brackets hold the expression, never content
                        closingDelimiter = closingDelimiter,
                    )
                    pending = null
                    continue
                }

                // Block macros: built-in names become BlockMacro; any other
                // non-directive name becomes the CustomBlockMacro extension
                // seam claimable by blockMacro plugins. Recognized before the
                // dlist check, but the shapes cannot collide: a block macro
                // has no space before `::` and requires the trailing `[...]`,
                // while a dlist description is separated by a space (and the
                // dlist branches below explicitly skip block-macro lines).
                val blockMacro = blockMacroRegex.matchEntire(line)
                if (blockMacro != null && blockMacro.groupValues[1] !in processingDirectiveNames) {
                    val macroName = blockMacro.groupValues[1]
                    val lineIndex = reader.index
                    reader.next()
                    val target = blockMacro.groupValues[2].ifEmpty { null }
                    val macroMetadata = mergeMetadata(pending, parseBlockAttributes(blockMacro.groupValues[3]))
                    val builtIn = blockMacroNames[macroName]
                    blocks += if (builtIn != null) {
                        BlockMacro(
                            name = builtIn,
                            target = target,
                            metadata = macroMetadata,
                            location = lineLocation(lineIndex),
                        )
                    } else {
                        CustomBlockMacro(
                            name = macroName,
                            target = target,
                            metadata = macroMetadata,
                            location = lineLocation(lineIndex),
                        )
                    }
                    pending = null
                    continue
                }

                val heading = sectionHeadingMatch(line)
                if (heading != null) {
                    val level = heading.groupValues[1].length - 1
                    if (pending?.positional?.firstOrNull() == "discrete") {
                        blocks += parseDiscreteHeading(heading, pending)
                        pending = null
                        continue
                    }
                    if (stopHeadingLevel != null && level <= stopHeadingLevel) return blocks
                    blocks += parseSection(heading, pending, stopAtEndif)
                    pending = null
                    continue
                }

                if (tableDelimiterRegex.matches(line)) {
                    blocks += parseTable(pending)
                    pending = null
                    continue
                }

                val delimited = tryParseDelimitedBlock(pending, stopAtEndif)
                if (delimited != null) {
                    pending = null
                    blocks += delimited
                    continue
                }

                val admonition = admonitionParagraphRegex.matchEntire(line)
                if (admonition != null) {
                    blocks += parseAdmonitionParagraph(admonition, pending)
                    pending = null
                    continue
                }

                if (calloutItemRegex.matchEntire(line) != null) {
                    blocks += parseCalloutList(pending)
                    pending = null
                    continue
                }
                if (listItemRegex.matchEntire(line) != null) {
                    blocks += parseList(pending)
                    pending = null
                    continue
                }
                if (dlistItemRegex.matchEntire(line) != null && blockMacroRegex.matchEntire(line) == null) {
                    blocks += parseDList(pending)
                    pending = null
                    continue
                }

                blocks += parseParagraph(closingDelimiter, pending)
                pending = null
            }
            return blocks
        }

        /** The heading match for [line], or null when it is no heading (7+ markers included). */
        private fun sectionHeadingMatch(line: String): MatchResult? {
            val match = headingRegex.matchEntire(line) ?: return null
            return match.takeIf { it.groupValues[1].length <= MAX_HEADING_MARKERS }
        }

        private fun isStructuralLine(line: String): Boolean =
            isDelimiterLine(line) || sectionHeadingMatch(line) != null || listItemRegex.matches(line) ||
                thematicBreakRegex.matches(line) || line == PAGE_BREAK ||
                isDirectiveLine(line) || calloutItemRegex.matches(line) ||
                tableDelimiterRegex.matches(line)

        /** True for `include::`/`ifdef::`/`ifndef::`/`ifeval::`/`endif::` lines. */
        private fun isDirectiveLine(line: String): Boolean =
            includeRegex.matches(line) || conditionalRegex.matches(line) ||
                ifevalRegex.matches(line) || endifRegex.matches(line)

        // -------------------------------------------------------------------
        // Headings and sections
        // -------------------------------------------------------------------

        private fun parseDiscreteHeading(heading: MatchResult, metadata: BlockMetadata): DiscreteHeading {
            val lineIndex = reader.index
            reader.next()
            val markerLength = heading.groupValues[1].length
            return DiscreteHeading(
                title = headingTitle(heading, lineIndex),
                level = markerLength - 1,
                metadata = metadata.copy(positional = metadata.positional.drop(1)),
                location = lineLocation(lineIndex),
            )
        }

        private fun parseSection(
            heading: MatchResult,
            metadata: BlockMetadata?,
            stopAtEndif: Boolean = false,
        ): SectionBlock {
            val headingLineIndex = reader.index
            reader.next()
            val level = heading.groupValues[1].length - 1
            val children = parseBlocks(stopHeadingLevel = level, closingDelimiter = null, stopAtEndif = stopAtEndif)
            val end = children.lastOrNull()?.location?.end
                ?: Position(headingLineIndex + 1, reader.endCol(headingLineIndex))
            return SectionBlock(
                title = headingTitle(heading, headingLineIndex),
                level = level,
                blocks = children,
                metadata = metadata,
                location = Location(Position(headingLineIndex + 1, 1), end),
            )
        }

        private fun headingTitle(heading: MatchResult, lineIndex: Int): List<Inline> {
            val markerLength = heading.groupValues[1].length
            val titleText = heading.groupValues[2]
            return inlineParser.parse(
                titleText,
                SegmentMap.ofLines(
                    listOf(titleText),
                    firstSourceLine = lineIndex + 1,
                    startCol = markerLength + 2, // after "== "
                ),
            )
        }

        // -------------------------------------------------------------------
        // Delimited blocks
        // -------------------------------------------------------------------

        private fun isDelimiterLine(line: String): Boolean {
            if (line == "--") return true
            if (line.length < 4) return false
            val c = line[0]
            if (c !in verbatimDelimiters && c !in parentDelimiters) return false
            return line.all { it == c }
        }

        private fun tryParseDelimitedBlock(metadata: BlockMetadata?, stopAtEndif: Boolean = false): Block? {
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

            val style = metadata?.positional?.firstOrNull()
            val children = parseBlocks(stopHeadingLevel = null, closingDelimiter = line, stopAtEndif = stopAtEndif)
            var closeLineIndex = reader.lines.size - 1
            if (!reader.eof && reader.peek() == line) {
                closeLineIndex = reader.index
                reader.next()
            }
            val location = Location(
                Position(openLineIndex + 1, 1),
                Position(closeLineIndex + 1, reader.endCol(closeLineIndex)),
            )
            // An admonition style on any parent block turns it into an admonition.
            if (style != null && style.uppercase() in admonitionNames) {
                return ParentBlock(
                    name = ParentBlockName.ADMONITION,
                    variant = style.lowercase(),
                    delimiter = line,
                    blocks = children,
                    metadata = metadata.copy(positional = metadata.positional.drop(1)),
                    location = location,
                )
            }
            val parentName = if (line == "--") ParentBlockName.OPEN else parentDelimiters.getValue(line[0])
            return ParentBlock(
                name = parentName,
                delimiter = line,
                blocks = children,
                metadata = metadata,
                location = location,
            )
        }

        // -------------------------------------------------------------------
        // Tables
        // -------------------------------------------------------------------

        /**
         * A `|===` delimited table. Cells are split on unescaped `|` (a `N+`
         * prefix spans N columns); a line without a leading `|` continues the
         * previous cell. The column count comes from the `cols` attribute when
         * present, otherwise from the first row. The first row becomes the
         * header for `%header`/`options="header"` tables, or implicitly when
         * the delimiter-adjacent first line is followed by a blank line.
         */
        private fun parseTable(metadata: BlockMetadata?): TableBlock {
            val openLineIndex = reader.index
            val delimiter = reader.next()
            val contentLines = mutableListOf<Pair<Int, String>>() // 0-based line index to text
            var closeLineIndex = reader.lines.size - 1
            while (!reader.eof) {
                if (reader.peek() == delimiter) {
                    closeLineIndex = reader.index
                    reader.next()
                    break
                }
                contentLines += reader.index to reader.next()
            }

            // Tokenize into cells; remember which content-line ordinal each cell run starts on.
            val pendingCells = mutableListOf<PendingTableCell>()
            var nonBlankOrdinal = -1 // ordinal of non-blank content lines, 0 = first
            var firstLineIsAdjacent = false
            var blankAfterFirstLine = false
            var firstLineCellCount = 0
            contentLines.forEachIndexed { ordinal, (lineIndex, text) ->
                if (text.isBlank()) return@forEachIndexed
                nonBlankOrdinal++
                if (nonBlankOrdinal == 0) {
                    firstLineIsAdjacent = ordinal == 0
                    blankAfterFirstLine = contentLines.getOrNull(ordinal + 1)?.second?.isBlank() == true
                }
                if (tableRowStartRegex.containsMatchIn(text)) {
                    val lineCells = tableLineCells(text)
                    lineCells.forEach { cell ->
                        pendingCells += PendingTableCell(cell.colSpan, nonBlankOrdinal).apply {
                            addSegment(lineIndex, cell.startCol, cell.text)
                        }
                    }
                    if (nonBlankOrdinal == 0) firstLineCellCount = lineCells.size
                } else {
                    // Continuation of the previous cell's content.
                    pendingCells.lastOrNull()?.addSegment(lineIndex, 1, text)
                }
            }

            val columns = parseColumns(metadata)
                ?: List(maxOf(firstLineCellCount, 1)) { TableColumn() }
            val colCount = columns.size
            val cells = pendingCells.map { it.toCell(inlineParser) to it.startLineOrdinal }

            // Group the flat cell sequence into rows of colCount columns.
            val rows = mutableListOf<TableRow>()
            var current = mutableListOf<TableCell>()
            var width = 0
            for ((cell, _) in cells) {
                current += cell
                width += cell.colSpan
                if (width >= colCount) {
                    rows += tableRow(current)
                    current = mutableListOf()
                    width = 0
                }
            }
            if (current.isNotEmpty()) rows += tableRow(current)

            val explicitHeader = metadata?.options?.contains("header") == true ||
                metadata?.named?.get("options")?.split(',')?.map { it.trim() }?.contains("header") == true
            val implicitHeader = firstLineIsAdjacent && blankAfterFirstLine &&
                cells.any { it.second > 0 } // header only makes sense with body rows
            val hasHeader = rows.size > 1 && (explicitHeader || implicitHeader)

            return TableBlock(
                columns = columns,
                header = if (hasHeader) rows.first() else null,
                rows = if (hasHeader) rows.drop(1) else rows,
                metadata = metadata,
                location = Location(
                    Position(openLineIndex + 1, 1),
                    Position(closeLineIndex + 1, reader.endCol(closeLineIndex)),
                ),
            )
        }

        private fun tableRow(cells: List<TableCell>): TableRow = TableRow(
            cells = cells,
            location = cells.firstOrNull()?.location?.let { first ->
                Location(first.start, cells.last().location?.end ?: first.end)
            },
        )

        /** The [TableColumn]s described by a `cols` attribute, or null when absent. */
        private fun parseColumns(metadata: BlockMetadata?): List<TableColumn>? {
            val spec = metadata?.named?.get("cols")?.takeIf { it.isNotBlank() } ?: return null
            val columns = mutableListOf<TableColumn>()
            for (item in spec.split(',')) {
                val match = tableColsItemRegex.matchEntire(item.trim()) ?: continue
                val repetitions = match.groupValues[1].toIntOrNull() ?: 1
                val alignment = when (match.groupValues[2]) {
                    "^" -> TableColumnAlignment.CENTER
                    ">" -> TableColumnAlignment.RIGHT
                    else -> TableColumnAlignment.LEFT
                }
                val width = match.groupValues[3].toIntOrNull() ?: 1
                repeat(repetitions) { columns += TableColumn(alignment, width) }
            }
            return columns.ifEmpty { null }
        }

        /** Splits one cell-bearing table line into its cells. */
        private fun tableLineCells(line: String): List<TableLineCell> {
            val boundaries = mutableListOf<Int>()
            for (k in line.indices) {
                if (line[k] == '|' && (k == 0 || line[k - 1] != '\\')) boundaries += k
            }
            val cells = mutableListOf<TableLineCell>()
            boundaries.forEachIndexed { index, boundary ->
                val prefixStart = if (index == 0) 0 else boundaries[index - 1] + 1
                val prefix = line.substring(prefixStart, boundary)
                val colSpan = tableCellSpanSpecRegex.find(prefix)?.groupValues?.get(1)?.toIntOrNull() ?: 1
                val contentEnd = if (index + 1 < boundaries.size) boundaries[index + 1] else line.length
                var content = line.substring(boundary + 1, contentEnd)
                // The next cell's span spec trails this cell's content; peel it off.
                if (index + 1 < boundaries.size) {
                    tableCellSpanSpecRegex.find(content)?.let { content = content.substring(0, it.range.first) }
                }
                cells += TableLineCell(
                    colSpan = colSpan,
                    startCol = boundary + 2, // 1-based col of the first content char
                    text = content.replace("\\|", "|"),
                )
            }
            return cells
        }

        // -------------------------------------------------------------------
        // Processing directives (include, ifdef/ifndef/ifeval)
        // -------------------------------------------------------------------

        /**
         * `include::path[attrlist]` — the attrlist's named attributes are kept
         * as-is; a `lines=N..M` (or `lines=N`) attribute additionally becomes
         * the structured [IncludeBlock.lineRange].
         */
        private fun parseInclude(match: MatchResult): IncludeBlock {
            val lineIndex = reader.index
            reader.next()
            val attributes = parseBlockAttributes(match.groupValues[2]).named
            val lineRange = attributes["lines"]?.trim()?.let { spec ->
                includeLineRangeRegex.matchEntire(spec)
                    ?.let { it.groupValues[1].toInt()..it.groupValues[2].toInt() }
                    ?: spec.toIntOrNull()?.let { it..it }
            }
            return IncludeBlock(
                path = match.groupValues[1],
                lineRange = lineRange,
                attributes = attributes,
                location = lineLocation(lineIndex),
            )
        }

        /**
         * A conditional directive. `ifdef::attrs[content]` is the single-line
         * form: the bracketed content becomes one paragraph. `ifdef::attrs[]`
         * opens a region closed by `endif::[]`; regions nest (each inner
         * conditional consumes its own endif), and an unclosed region extends
         * to the end of the enclosing scope (lenient, the parser never errors).
         * The [condition] stays raw (`attr1,attr2` / `attr1+attr2` / an ifeval
         * expression) — evaluation is the document-processing phase's job.
         */
        private fun parseConditional(
            variant: ConditionalVariant,
            condition: String,
            inlineContent: String,
            closingDelimiter: String?,
        ): ConditionalBlock {
            val openLineIndex = reader.index
            val openLine = reader.next()

            if (inlineContent.isNotEmpty()) {
                val startCol = openLine.indexOf('[') + 2 // 1-based col of the first content char
                val inlines = inlineParser.parse(
                    inlineContent,
                    SegmentMap.ofLines(listOf(inlineContent), firstSourceLine = openLineIndex + 1, startCol = startCol),
                )
                val paragraph = LeafBlock(
                    name = LeafBlockName.PARAGRAPH,
                    form = LeafBlockForm.PARAGRAPH,
                    inlines = inlines,
                    location = Location(
                        Position(openLineIndex + 1, startCol),
                        Position(openLineIndex + 1, startCol + inlineContent.length - 1),
                    ),
                )
                return ConditionalBlock(
                    variant = variant,
                    condition = condition,
                    blocks = listOf(paragraph),
                    location = lineLocation(openLineIndex),
                )
            }

            val children = parseBlocks(
                stopHeadingLevel = null,
                closingDelimiter = closingDelimiter,
                stopAtEndif = true,
            )
            var endLineIndex = reader.lines.size - 1 // unclosed region: rest of input is the body
            if (!reader.eof) {
                if (endifRegex.matchEntire(reader.peek()) != null) {
                    endLineIndex = reader.index
                    reader.next()
                } else {
                    // Stopped at the enclosing delimiter: the region ends before it.
                    endLineIndex = (reader.index - 1).coerceAtLeast(openLineIndex)
                }
            }
            return ConditionalBlock(
                variant = variant,
                condition = condition,
                blocks = children,
                location = Location(
                    Position(openLineIndex + 1, 1),
                    Position(endLineIndex + 1, reader.endCol(endLineIndex)),
                ),
            )
        }

        // -------------------------------------------------------------------
        // Callout lists
        // -------------------------------------------------------------------

        /** A run of `<n> text` lines below a verbatim block forms a callout list. */
        private fun parseCalloutList(metadata: BlockMetadata?): ListBlock {
            val items = mutableListOf<ListItem>()
            while (!reader.eof) {
                val match = calloutItemRegex.matchEntire(reader.peek()) ?: break
                val lineIndex = reader.index
                reader.next()
                val marker = match.groupValues[1]
                val text = match.groupValues[2]
                val principal = inlineParser.parse(
                    text,
                    SegmentMap.ofLines(listOf(text), firstSourceLine = lineIndex + 1, startCol = marker.length + 2),
                )
                items += ListItem(
                    marker = marker,
                    principal = principal,
                    location = Location(
                        Position(lineIndex + 1, 1),
                        Position(lineIndex + 1, reader.endCol(lineIndex)),
                    ),
                )
            }
            return ListBlock(
                variant = ListVariant.CALLOUT,
                marker = items.first().marker,
                items = items,
                metadata = metadata,
                location = Location(items.first().location!!.start, items.last().location!!.end),
            )
        }

        // -------------------------------------------------------------------
        // Admonition paragraphs
        // -------------------------------------------------------------------

        private fun parseAdmonitionParagraph(match: MatchResult, metadata: BlockMetadata?): ParentBlock {
            val startLineIndex = reader.index
            reader.next()
            val label = match.groupValues[1]
            val firstLineText = match.groupValues[2]
            val continuation = mutableListOf<String>()
            while (!reader.eof && !reader.peek().isBlank() && !isStructuralLine(reader.peek())) {
                continuation.add(reader.next())
            }
            val lines = listOf(firstLineText) + continuation
            // The first logical line starts after "NOTE: "; continuations at col 1.
            val lineStarts = ArrayList<Int>(lines.size)
            val sourceLines = ArrayList<Int>(lines.size)
            val startCols = ArrayList<Int>(lines.size)
            var offset = 0
            lines.forEachIndexed { i, l ->
                lineStarts.add(offset)
                sourceLines.add(startLineIndex + 1 + i)
                startCols.add(if (i == 0) label.length + 3 else 1)
                offset += l.length + 1
            }
            val map = SegmentMap(lineStarts, sourceLines, startCols)
            val inlines = inlineParser.parse(lines.joinToString("\n"), map)
            val lastLineIndex = startLineIndex + lines.size - 1
            val location = Location(
                Position(startLineIndex + 1, 1),
                Position(lastLineIndex + 1, reader.endCol(lastLineIndex)),
            )
            val paragraph = LeafBlock(
                name = LeafBlockName.PARAGRAPH,
                form = LeafBlockForm.PARAGRAPH,
                inlines = inlines,
                location = location,
            )
            return ParentBlock(
                name = ParentBlockName.ADMONITION,
                variant = label.lowercase(),
                delimiter = null, // paragraph form: no delimiter (see ParentBlock KDoc)
                blocks = listOf(paragraph),
                metadata = metadata,
                location = location,
            )
        }

        // -------------------------------------------------------------------
        // Lists
        // -------------------------------------------------------------------

        fun parseList(metadata: BlockMetadata? = null): ListBlock {
            val items = mutableListOf<ListItem>()
            var marker: String? = null
            while (!reader.eof) {
                val line = reader.peek()
                if (line.isBlank()) {
                    // Blank lines between items continue the list.
                    var ahead = 1
                    while (reader.peekAt(ahead)?.isBlank() == true) ahead++
                    val nextLine = reader.peekAt(ahead) ?: break
                    if (listItemRegex.matchEntire(nextLine) == null) break
                    repeat(ahead) { reader.next() }
                    continue
                }
                if (line == LIST_CONTINUATION && items.isNotEmpty()) {
                    // `+` alone attaches the following block to the current item.
                    reader.next()
                    if (reader.eof || reader.peek().isBlank()) continue
                    val attached: Block = tryParseDelimitedBlock(null)
                        ?: (if (isStructuralLine(reader.peek())) null else parseParagraph(closingDelimiter = null))
                        ?: continue // vacuous continuation (next line starts a new structure)
                    val last = items.removeAt(items.size - 1)
                    items += last.copy(
                        blocks = last.blocks + attached,
                        location = last.location?.let { Location(it.start, attached.location?.end ?: it.end) },
                    )
                    continue
                }
                val match = listItemRegex.matchEntire(line) ?: break
                val itemMarker = match.groupValues[1]
                if (marker == null) marker = itemMarker
                val sameFamily = itemMarker == marker || (isNumberedMarker(itemMarker) && isNumberedMarker(marker))
                if (!sameFamily) {
                    if (isDeeperMarker(marker, itemMarker) && items.isNotEmpty()) {
                        val nested = parseList()
                        val last = items.removeAt(items.size - 1)
                        items += last.copy(
                            blocks = last.blocks + nested,
                            location = last.location?.let { Location(it.start, nested.location?.end ?: it.end) },
                        )
                        continue
                    }
                    break // sibling or shallower list: caller handles
                }
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
                variant = if (resolvedMarker.startsWith(".") || resolvedMarker.first().isDigit()) {
                    ListVariant.ORDERED
                } else {
                    ListVariant.UNORDERED
                },
                marker = resolvedMarker,
                items = items,
                metadata = metadata,
                location = Location(
                    items.first().location!!.start,
                    items.last().location!!.end,
                ),
            )
        }

        /** True when [candidate] nests under [current] (`*` → `**`, `.` → `..`). */
        private fun isDeeperMarker(current: String, candidate: String): Boolean {
            val currentChar = current.first()
            if (currentChar != '*' && currentChar != '.') return false
            return candidate.first() == currentChar && candidate.length > current.length
        }

        /** Explicitly numbered ordered markers (`1.`, `2.`) form one list family. */
        private fun isNumberedMarker(marker: String): Boolean = marker.first().isDigit()

        // -------------------------------------------------------------------
        // Description lists
        // -------------------------------------------------------------------

        private fun parseDList(metadata: BlockMetadata?): DListBlock {
            val items = mutableListOf<DListItem>()
            while (!reader.eof) {
                val match = dlistItemRegex.matchEntire(reader.peek()) ?: break
                if (blockMacroRegex.matchEntire(reader.peek()) != null) break
                val termLineIndex = reader.index
                reader.next()
                val termText = match.groupValues[1]
                val term = inlineParser.parse(
                    termText,
                    SegmentMap.ofLines(listOf(termText), firstSourceLine = termLineIndex + 1),
                )
                val sameLineDescription = match.groupValues[3]

                val principal: List<Inline>
                var lastLineIndex = termLineIndex
                if (sameLineDescription.isNotEmpty()) {
                    principal = inlineParser.parse(
                        sameLineDescription,
                        SegmentMap.ofLines(
                            listOf(sameLineDescription),
                            firstSourceLine = termLineIndex + 1,
                            startCol = termText.length + 4, // after "term:: "
                        ),
                    )
                } else {
                    // Description on the following lines until blank/structural.
                    while (!reader.eof && reader.peek().isBlank()) reader.next()
                    val descLines = mutableListOf<String>()
                    val descStartIndex = reader.index
                    while (!reader.eof) {
                        val l = reader.peek()
                        if (l.isBlank() || isStructuralLine(l) || dlistItemRegex.matches(l)) break
                        descLines.add(reader.next())
                    }
                    principal = if (descLines.isEmpty()) emptyList() else inlineParser.parse(
                        descLines.joinToString("\n"),
                        SegmentMap.ofLines(descLines, firstSourceLine = descStartIndex + 1),
                    )
                    if (descLines.isNotEmpty()) lastLineIndex = descStartIndex + descLines.size - 1
                }
                items += DListItem(
                    marker = match.groupValues[2],
                    terms = listOf(term),
                    principal = principal,
                    location = Location(
                        Position(termLineIndex + 1, 1),
                        Position(lastLineIndex + 1, reader.endCol(lastLineIndex)),
                    ),
                )
                // Blank lines between items continue the dlist.
                var ahead = 0
                while (reader.peekAt(ahead)?.isBlank() == true) ahead++
                val nextLine = reader.peekAt(ahead) ?: break
                if (dlistItemRegex.matchEntire(nextLine) == null || blockMacroRegex.matches(nextLine)) break
                repeat(ahead) { reader.next() }
            }
            return DListBlock(
                marker = "::",
                items = items,
                metadata = metadata,
                location = Location(items.first().location!!.start, items.last().location!!.end),
            )
        }

        // -------------------------------------------------------------------
        // Paragraphs
        // -------------------------------------------------------------------

        private fun parseParagraph(closingDelimiter: String?, metadata: BlockMetadata? = null): LeafBlock {
            val startLineIndex = reader.index
            val lines = mutableListOf<String>()
            while (!reader.eof) {
                val line = reader.peek()
                if (line.isBlank()) break
                if (closingDelimiter != null && line == closingDelimiter) break
                if (isStructuralLine(line)) break
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

        // -------------------------------------------------------------------
        // Metadata helpers
        // -------------------------------------------------------------------

        private fun lineLocation(lineIndex: Int): Location =
            Location(Position(lineIndex + 1, 1), Position(lineIndex + 1, reader.endCol(lineIndex)))

        private fun mergeMetadata(base: BlockMetadata?, addition: BlockMetadata): BlockMetadata {
            if (base == null) return addition
            return BlockMetadata(
                positional = if (addition.positional.isNotEmpty()) addition.positional else base.positional,
                named = base.named + addition.named,
                id = addition.id ?: base.id,
                roles = base.roles + addition.roles,
                options = base.options + addition.options,
                title = addition.title ?: base.title,
            )
        }

        /** Splits an attrlist on commas, keeping commas inside `"..."` values intact. */
        private fun splitAttributeList(raw: String): List<String> {
            val parts = mutableListOf<String>()
            val current = StringBuilder()
            var inQuotes = false
            for (ch in raw) {
                when {
                    ch == '"' -> {
                        inQuotes = !inQuotes
                        current.append(ch)
                    }
                    ch == ',' && !inQuotes -> {
                        parts += current.toString()
                        current.clear()
                    }
                    else -> current.append(ch)
                }
            }
            parts += current.toString()
            return parts
        }

        /**
         * Parses the inside of a `[...]` attribute line: positional and named
         * attributes, plus the `style#id.role%option` shorthand in the first
         * positional slot.
         */
        private fun parseBlockAttributes(raw: String): BlockMetadata {
            if (raw.isBlank()) return BlockMetadata()
            val positional = mutableListOf<String>()
            val named = LinkedHashMap<String, String>()
            splitAttributeList(raw).forEach { part ->
                val trimmed = part.trim()
                if (trimmed.isEmpty()) return@forEach
                val eq = trimmed.indexOf('=')
                if (eq > 0) {
                    named[trimmed.substring(0, eq).trim()] = trimmed.substring(eq + 1).trim().removeSurrounding("\"")
                } else {
                    positional += trimmed.removeSurrounding("\"")
                }
            }

            var id: String? = null
            val roles = mutableListOf<String>()
            val options = mutableListOf<String>()
            if (positional.isNotEmpty() && positional[0].any { it == '#' || it == '.' || it == '%' }) {
                val shorthand = positional.removeAt(0)
                val style = StringBuilder()
                val current = StringBuilder()
                var mode = 's'
                fun flush() {
                    val value = current.toString()
                    current.clear()
                    if (value.isEmpty()) return
                    when (mode) {
                        's' -> style.append(value)
                        '#' -> id = value
                        '.' -> roles += value
                        '%' -> options += value
                    }
                }
                for (ch in shorthand) {
                    if (ch == '#' || ch == '.' || ch == '%') {
                        flush()
                        mode = ch
                    } else {
                        current.append(ch)
                    }
                }
                flush()
                if (style.isNotEmpty()) positional.add(0, style.toString())
            }
            return BlockMetadata(
                positional = positional,
                named = named,
                id = id,
                roles = roles,
                options = options,
            )
        }
    }
}

/** One cell as it appears on a single table line: span spec, text start col, raw text. */
private data class TableLineCell(
    val colSpan: Int,
    val startCol: Int,
    val text: String,
)

/**
 * A table cell being accumulated across source lines (a cell's content
 * continues on following lines that do not start with `|`).
 */
private class PendingTableCell(
    val colSpan: Int,
    val startLineOrdinal: Int,
) {
    private val sourceLines = mutableListOf<Int>() // 0-based
    private val startCols = mutableListOf<Int>() // 1-based
    private val texts = mutableListOf<String>()

    fun addSegment(lineIndex: Int, startCol: Int, text: String) {
        sourceLines += lineIndex
        startCols += startCol
        texts += text
    }

    /** Trims the segments and inline-parses the joined content. */
    fun toCell(inlineParser: AsgInlineParser): TableCell {
        data class Segment(val sourceLine: Int, val startCol: Int, val text: String)

        val segments = texts.mapIndexedNotNull { index, raw ->
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) {
                null
            } else {
                val leading = raw.indexOf(trimmed.first())
                Segment(sourceLines[index] + 1, startCols[index] + leading, trimmed)
            }
        }
        if (segments.isEmpty()) {
            return TableCell(inlines = emptyList(), colSpan = colSpan)
        }
        val lineStarts = ArrayList<Int>(segments.size)
        var offset = 0
        segments.forEach { segment ->
            lineStarts.add(offset)
            offset += segment.text.length + 1 // the joining '\n'
        }
        val map = SegmentMap(lineStarts, segments.map { it.sourceLine }, segments.map { it.startCol })
        val inlines = inlineParser.parse(segments.joinToString("\n") { it.text }, map)
        val last = segments.last()
        return TableCell(
            inlines = inlines,
            colSpan = colSpan,
            location = Location(
                Position(segments.first().sourceLine, segments.first().startCol),
                Position(last.sourceLine, last.startCol + last.text.length - 1),
            ),
        )
    }
}
