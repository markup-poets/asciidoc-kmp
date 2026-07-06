package org.markup.poet.asciidoc.asg

/**
 * A position in the source document, matching the official ASG location boundary:
 * 1-based line, 1-based column. Column 0 is legal and used for empty lines.
 */
data class Position(val line: Int, val col: Int)

/**
 * A source span. Both boundaries are INCLUSIVE, as pinned by the official TCK
 * fixtures (e.g. `*s*` yields span location [{1,1},{1,3}] and inner text
 * location [{1,2},{1,2}]).
 */
data class Location(val start: Position, val end: Position)
