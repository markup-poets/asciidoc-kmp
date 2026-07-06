package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.Location
import org.markup.poet.asciidoc.asg.Position

/**
 * Default implementation of FragmentProcessor.
 * Processes tagged fragments in included content, extracting specific sections.
 *
 * The document pass itself is structure-preserving: tag extraction happens on
 * raw include content, via [extractTaggedContent] and
 * [applyTagAndLineRangeFilters], which the include resolution phase calls for
 * includes carrying `tag`/`tags` attributes.
 */
class DefaultFragmentProcessor : FragmentProcessor {

    override fun processFragments(document: AsgDocument, config: FragmentConfig): FragmentResult {
        return FragmentResult(
            document = document,
            errors = emptyList(),
            warnings = emptyList(),
            extractedTags = emptyMap()
        )
    }

    /**
     * Extract tagged fragments from content.
     * This is called by IncludeResolver when processing includes with tag attributes.
     */
    fun extractTaggedContent(
        content: String,
        tags: List<String>,
        config: FragmentConfig,
        location: Location?,
        errors: MutableList<ProcessingError>,
        warnings: MutableList<ProcessingWarning>
    ): String {
        val lines = content.lines()
        val taggedSections = parseTagMarkers(lines, config, location, errors)

        // Extract content for requested tags
        val extractedLines = mutableListOf<String>()
        val foundTags = mutableSetOf<String>()

        for (tag in tags) {
            val sections = taggedSections[tag]
            if (sections == null || sections.isEmpty()) {
                warnings.add(
                    ProcessingWarning(
                        message = "Tag '$tag' not found in included content",
                        location = location,
                        warningType = ProcessingWarningType.FRAGMENT_TAG_NOT_FOUND
                    )
                )
            } else {
                foundTags.add(tag)
                for (section in sections) {
                    extractedLines.addAll(section)
                }
            }
        }

        return extractedLines.joinToString("\n")
    }

    /**
     * Parse tag markers in content and extract tagged sections.
     * Returns a map of tag names to lists of line sections.
     */
    private fun parseTagMarkers(
        lines: List<String>,
        config: FragmentConfig,
        location: Location?,
        errors: MutableList<ProcessingError>
    ): Map<String, List<List<String>>> {
        val taggedSections = mutableMapOf<String, MutableList<List<String>>>()
        val activeTagStack = mutableListOf<Pair<String, Int>>() // (tagName, startLine)
        val tagContentMap = mutableMapOf<String, MutableList<String>>()

        for ((lineIndex, line) in lines.withIndex()) {
            val trimmedLine = line.trim()

            // Check for tag start marker
            val startMatch = findTagStart(trimmedLine, config)
            if (startMatch != null) {
                val tagName = startMatch

                // Validate tag marker format
                if (!isValidTagName(tagName)) {
                    errors.add(
                        ProcessingError(
                            message = "Malformed tag marker: invalid tag name '$tagName'",
                            location = offsetLocation(location, lineIndex),
                            errorType = ProcessingErrorType.FRAGMENT_TAG_MALFORMED
                        )
                    )
                    continue
                }

                // Check for nested tags
                if (!config.allowNestedTags && activeTagStack.isNotEmpty()) {
                    errors.add(
                        ProcessingError(
                            message = "Nested tags are not allowed: tag '$tagName' inside '${activeTagStack.last().first}'",
                            location = offsetLocation(location, lineIndex),
                            errorType = ProcessingErrorType.FRAGMENT_TAG_MALFORMED
                        )
                    )
                    continue
                }

                activeTagStack.add(tagName to lineIndex)
                tagContentMap[tagName] = mutableListOf()
                continue
            }

            // Check for tag end marker
            val endMatch = findTagEnd(trimmedLine, config)
            if (endMatch != null) {
                val tagName = endMatch

                // Find matching start tag
                val matchingTagIndex = activeTagStack.indexOfLast { it.first == tagName }
                if (matchingTagIndex == -1) {
                    errors.add(
                        ProcessingError(
                            message = "Unmatched end tag marker: no start marker for tag '$tagName'",
                            location = offsetLocation(location, lineIndex),
                            errorType = ProcessingErrorType.FRAGMENT_TAG_MALFORMED
                        )
                    )
                    continue
                }

                // Close the tag
                val (closedTag, _) = activeTagStack.removeAt(matchingTagIndex)
                val content = tagContentMap.remove(closedTag) ?: mutableListOf()

                // Store the extracted section
                if (!taggedSections.containsKey(closedTag)) {
                    taggedSections[closedTag] = mutableListOf()
                }
                taggedSections[closedTag]!!.add(content)

                continue
            }

            // Add line to all active tags
            for ((tagName, _) in activeTagStack) {
                tagContentMap[tagName]?.add(line)
            }
        }

        // Check for unclosed tags
        for ((tagName, startLine) in activeTagStack) {
            errors.add(
                ProcessingError(
                    message = "Unclosed tag marker: tag '$tagName' started but never closed",
                    location = offsetLocation(location, startLine),
                    errorType = ProcessingErrorType.FRAGMENT_TAG_UNCLOSED
                )
            )
        }

        return taggedSections
    }

    /**
     * Shifts [location] down by [lineOffset] lines to point at a line inside included content.
     */
    private fun offsetLocation(location: Location?, lineOffset: Int): Location? {
        if (location == null) return null
        val position = Position(location.start.line + lineOffset, location.start.col)
        return Location(position, position)
    }

    /**
     * Find tag start marker in a line.
     * Returns the tag name if found, null otherwise.
     */
    private fun findTagStart(line: String, config: FragmentConfig): String? {
        val pattern = "${Regex.escape(config.tagPrefix)}([^\\[]+)${Regex.escape(config.tagSuffix)}"
        val regex = Regex(pattern)
        val match = regex.find(line)
        return match?.groupValues?.getOrNull(1)?.trim()
    }

    /**
     * Find tag end marker in a line.
     * Returns the tag name if found, null otherwise.
     */
    private fun findTagEnd(line: String, config: FragmentConfig): String? {
        val pattern = "end::([^\\[]+)${Regex.escape(config.tagSuffix)}"
        val regex = Regex(pattern)
        val match = regex.find(line)
        return match?.groupValues?.getOrNull(1)?.trim()
    }

    /**
     * Validate tag name format.
     */
    private fun isValidTagName(tagName: String): Boolean {
        return tagName.isNotEmpty() &&
               tagName.all { it.isLetterOrDigit() || it == '-' || it == '_' }
    }

    /**
     * Apply both tag filtering and line range filtering.
     * Tags are applied first, then line ranges.
     */
    fun applyTagAndLineRangeFilters(
        content: String,
        tags: List<String>?,
        lineRange: IntRange?,
        config: FragmentConfig,
        location: Location?,
        errors: MutableList<ProcessingError>,
        warnings: MutableList<ProcessingWarning>
    ): String {
        var filteredContent = content

        // Apply tag filtering first
        if (tags != null && tags.isNotEmpty()) {
            filteredContent = extractTaggedContent(
                content = filteredContent,
                tags = tags,
                config = config,
                location = location,
                errors = errors,
                warnings = warnings
            )
        }

        // Apply line range filtering second
        if (lineRange != null) {
            filteredContent = filterLineRange(filteredContent, lineRange)
        }

        return filteredContent
    }

    /**
     * Filter content to include only the specified line range.
     */
    private fun filterLineRange(content: String, lineRange: IntRange): String {
        val lines = content.lines()
        val startLine = (lineRange.first - 1).coerceIn(0, lines.size)
        val endLine = (lineRange.last - 1).coerceIn(0, lines.size)

        return if (startLine <= endLine && startLine < lines.size) {
            lines.subList(startLine, minOf(endLine + 1, lines.size)).joinToString("\n")
        } else {
            ""
        }
    }
}
