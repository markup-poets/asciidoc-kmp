package org.markup.poet.asciidoc.parser

import org.markup.poet.asciidoc.ast.*
import org.markup.poet.asciidoc.error.ErrorSeverity
import org.markup.poet.asciidoc.error.ParseError
import org.markup.poet.asciidoc.error.ParseWarning
import org.markup.poet.asciidoc.parser.ListType as ParserListType

/**
 * Default implementation of AsciidocParser with comprehensive error handling and recovery.
 * This parser integrates error collection throughout all parsing components and implements
 * recovery strategies to continue parsing after encountering errors.
 */
class DefaultAsciidocParser(
    private val lineProcessor: LineProcessor = DefaultLineProcessor(),
    private val blockParser: BlockParser = DefaultBlockParser(),
    private val inlineParser: InlineParser = DefaultInlineParser(),
    private val stateMachine: ParseStateMachine = DefaultParseStateMachine(),
    private val attributeParser: AttributeParser = DefaultAttributeParser()
) : AsciidocParser {
    
    private val errors = mutableListOf<ParseError>()
    private val warnings = mutableListOf<ParseWarning>()
    
    override fun parse(source: String): ParseResult {
        return parse(source.lines())
    }
    
    override fun parse(lines: List<String>): ParseResult {
        // Clear previous errors and warnings
        errors.clear()
        warnings.clear()
        
        // Reset state machine
        stateMachine.reset()
        
        try {
            val document = parseDocument(lines)
            return ParseResult(document, errors.toList(), warnings.toList())
        } catch (e: Exception) {
            // Handle critical parsing failures
            addError(
                message = "Critical parsing failure: ${e.message}",
                location = SourceLocation(1),
                severity = ErrorSeverity.FATAL
            )
            
            // Return minimal document even on critical failure
            val fallbackDocument = Document(
                title = null,
                children = emptyList(),
                documentAttributes = emptyMap(),
                sourceLocation = SourceLocation(1)
            )
            
            return ParseResult(fallbackDocument, errors.toList(), warnings.toList())
        }
    }
    
    private fun parseDocument(lines: List<String>): Document {
        val documentAttributes = mutableMapOf<String, String>()
        val children = mutableListOf<BlockElement>()
        var title: String? = null
        
        var currentBlockLines = mutableListOf<String>()
        var currentBlockType: BlockType? = null
        var currentBlockStartLine = 1
        
        lines.forEachIndexed { index, line ->
            val lineNumber = index + 1
            
            try {
                val context = ParseContext(
                    inCodeBlock = stateMachine.getCurrentState() == ParseState.IN_CODE_BLOCK,
                    currentListLevel = stateMachine.getContext().listLevel,
                    listType = when (stateMachine.getContext().listType) {
                        ParserListType.UNORDERED -> BlockType.UNORDERED_LIST
                        ParserListType.ORDERED -> BlockType.ORDERED_LIST
                        ParserListType.DEFINITION -> BlockType.PARAGRAPH // Simplified
                        null -> null
                    }
                )
                
                val lineResult = lineProcessor.processLine(line, lineNumber, context)
                
                // Handle state transitions with error checking
                val trigger = createStateTrigger(lineResult)
                val transition = stateMachine.transition(trigger)
                
                if (!transition.success && transition.error != null) {
                    errors.add(transition.error)
                    // Continue parsing despite state transition error
                }
                
                // Process the line based on its type
                val inCodeBlock = stateMachine.getCurrentState() == ParseState.IN_CODE_BLOCK

                when (lineResult.blockType) {
                    BlockType.EMPTY if !inCodeBlock -> {
                        // Finalize current block if any
                        if (currentBlockLines.isNotEmpty()) {
                            processBlock(currentBlockLines, currentBlockType, currentBlockStartLine, children, documentAttributes)
                            currentBlockLines.clear()
                        }
                        currentBlockType = null
                    }
                    BlockType.ATTRIBUTE_DEFINITION if !inCodeBlock -> {
                        // Process attribute with error handling
                        processAttributeDefinition(line, lineNumber, documentAttributes)
                    }
                    else -> {
                        // Check if we need to start a new block
                        // In code block, we don't switch block type until the delimiter ends it
                        val isDelimiter = lineResult.blockType == BlockType.CODE_BLOCK_DELIMITER
                        val shouldSwitchBlock = !inCodeBlock && currentBlockType != lineResult.blockType
                        val isClosingDelimiter = inCodeBlock && isDelimiter

                        if (shouldSwitchBlock || isClosingDelimiter) {
                            // Finalize previous block
                            if (currentBlockLines.isNotEmpty()) {
                                if (isClosingDelimiter) {
                                    currentBlockLines.add(line)
                                }
                                processBlock(currentBlockLines, currentBlockType, currentBlockStartLine, children, documentAttributes)
                                currentBlockLines.clear()
                                if (isClosingDelimiter) {
                                    currentBlockType = null
                                    // NO RESET HERE
                                    return@forEachIndexed
                                }
                            }

                            currentBlockType = lineResult.blockType
                            currentBlockStartLine = lineNumber
                        } else if (currentBlockType == null) {
                            currentBlockType = lineResult.blockType
                            currentBlockStartLine = lineNumber
                        }

                        currentBlockLines.add(line)
                    }
                }
                
            } catch (e: Exception) {
                // Handle line processing errors
                addError(
                    message = "Error processing line $lineNumber: ${e.message}",
                    location = SourceLocation(lineNumber),
                    severity = ErrorSeverity.ERROR
                )
                
                // Continue with next line (error recovery)
                return@forEachIndexed
            }
        }
        
        // Process any remaining block
        if (currentBlockLines.isNotEmpty()) {
            processBlock(currentBlockLines, currentBlockType, currentBlockStartLine, children, documentAttributes)
        }
        
        // Extract document title from first section if present
        if (children.isNotEmpty() && children.first() is Section) {
            val firstSection = children.first() as Section
            if (firstSection.level == 1) {
                title = firstSection.title
            }
        }
        
        return Document(
            title = title,
            children = children,
            documentAttributes = documentAttributes,
            sourceLocation = SourceLocation(1)
        )
    }
    
    private fun processBlock(
        lines: List<String>,
        blockType: BlockType?,
        startLineNumber: Int,
        children: MutableList<BlockElement>,
        documentAttributes: Map<String, String>
    ) {
        if (lines.isEmpty()) return
        
        try {
            when (blockType) {
                BlockType.SECTION_HEADER -> {
                    val section = blockParser.parseSection(lines.first(), startLineNumber)
                    children.add(section)
                }
                
                BlockType.UNORDERED_LIST -> {
                    val list = blockParser.parseList(lines, startLineNumber, org.markup.poet.asciidoc.ast.ListType.UNORDERED)
                    children.add(list)
                }
                
                BlockType.ORDERED_LIST -> {
                    val list = blockParser.parseList(lines, startLineNumber, org.markup.poet.asciidoc.ast.ListType.ORDERED)
                    children.add(list)
                }
                
                BlockType.CODE_BLOCK_DELIMITER -> {
                    // Extract content between delimiters
                    var language: String? = null
                    val codeContent = if (lines.size >= 2) {
                        if (lines.first().startsWith("[")) {
                            // Extract language from [source,language]
                            val metadata = lines.first().removePrefix("[").removeSuffix("]")
                            if (metadata.startsWith("source,")) {
                                language = metadata.removePrefix("source,").trim()
                            }
                            
                            if (lines.getOrNull(1)?.startsWith("----") == true) {
                                lines.drop(2).dropLast(1)
                            } else {
                                lines.drop(1).dropLast(1)
                            }
                        } else {
                            lines.drop(1).dropLast(1)
                        }
                    } else {
                        // Malformed code block
                        addError(
                            message = "Malformed code block: missing closing delimiter",
                            location = SourceLocation(startLineNumber),
                            severity = ErrorSeverity.ERROR
                        )
                        lines
                    }
                    val codeBlock = blockParser.parseCodeBlock(codeContent, startLineNumber, language)
                    children.add(codeBlock)
                }
                
                BlockType.COMMENT -> {
                    // Comments are typically not added to the AST, but we can process them
                    lines.forEach { line ->
                        blockParser.parseComment(line, startLineNumber)
                    }
                }

                BlockType.INCLUDE_DIRECTIVE -> {
                    val line = lines.first().trim()
                    val pathEnd = line.indexOf('[')
                    val path = line.substring("include::".length, pathEnd)
                    val attributesStr = line.substring(pathEnd + 1, line.length - 1)
                    
                    // Basic parsing of line range if present in attributes
                    var lineRange: IntRange? = null
                    if (attributesStr.startsWith("lines=")) {
                        val rangeStr = attributesStr.substring("lines=".length)
                        val parts = rangeStr.split("..")
                        if (parts.size == 2) {
                            val start = parts[0].toIntOrNull()
                            val end = parts[1].toIntOrNull()
                            if (start != null && end != null) {
                                lineRange = start..end
                            }
                        }
                    }

                    children.add(IncludeDirective(
                        path = path,
                        lineRange = lineRange,
                        sourceLocation = SourceLocation(startLineNumber)
                    ))
                }
                
                BlockType.PARAGRAPH, null -> {
                    val paragraph = blockParser.parseParagraph(lines, startLineNumber)
                    children.add(paragraph)
                }
                
                else -> {
                    // Fallback to paragraph for unknown block types
                    addWarning(
                        message = "Unknown block type $blockType, treating as paragraph",
                        location = SourceLocation(startLineNumber)
                    )
                    val paragraph = blockParser.parseParagraph(lines, startLineNumber)
                    children.add(paragraph)
                }
            }
        } catch (e: Exception) {
            // Error recovery: create a simple paragraph with the content
            addError(
                message = "Error parsing block at line $startLineNumber: ${e.message}",
                location = SourceLocation(startLineNumber),
                severity = ErrorSeverity.ERROR
            )
            
            try {
                val fallbackParagraph = Paragraph(
                    content = listOf(Text(
                        content = lines.joinToString(" "),
                        sourceLocation = SourceLocation(startLineNumber)
                    )),
                    sourceLocation = SourceLocation(startLineNumber)
                )
                children.add(fallbackParagraph)
            } catch (e2: Exception) {
                // Even fallback failed, just log the error
                addError(
                    message = "Failed to create fallback paragraph: ${e2.message}",
                    location = SourceLocation(startLineNumber),
                    severity = ErrorSeverity.ERROR
                )
            }
        }
    }
    
    private fun processAttributeDefinition(
        line: String,
        lineNumber: Int,
        documentAttributes: MutableMap<String, String>
    ) {
        try {
            val attributeDefinition = attributeParser.parseAttributeDefinition(line, lineNumber)
            if (attributeDefinition != null) {
                // Check for duplicate attributes
                if (documentAttributes.containsKey(attributeDefinition.key)) {
                    addWarning(
                        message = "Duplicate attribute '${attributeDefinition.key}', using last value",
                        location = SourceLocation(lineNumber)
                    )
                }
                documentAttributes[attributeDefinition.key] = attributeDefinition.value
            } else {
                addError(
                    message = "Malformed attribute definition",
                    location = SourceLocation(lineNumber),
                    severity = ErrorSeverity.ERROR
                )
            }
        } catch (e: Exception) {
            addError(
                message = "Error parsing attribute definition: ${e.message}",
                location = SourceLocation(lineNumber),
                severity = ErrorSeverity.ERROR
            )
        }
    }
    
    private fun createStateTrigger(lineResult: LineResult): StateTrigger {
        return when (lineResult.blockType) {
            BlockType.EMPTY -> StateTrigger.EmptyLine
            BlockType.SECTION_HEADER -> {
                val level = lineResult.attributes["level"]?.toIntOrNull() ?: 1
                StateTrigger.SectionHeader(level)
            }
            BlockType.UNORDERED_LIST -> {
                StateTrigger.ListMarker(ParserListType.UNORDERED, 1) // Simplified level
            }
            BlockType.ORDERED_LIST -> {
                StateTrigger.ListMarker(ParserListType.ORDERED, 1) // Simplified level
            }
            BlockType.CODE_BLOCK_DELIMITER -> {
                StateTrigger.BlockDelimiter("----")
            }
            BlockType.COMMENT -> StateTrigger.CommentLine
            BlockType.INCLUDE_DIRECTIVE -> StateTrigger.IncludeDirective
            BlockType.ATTRIBUTE_DEFINITION -> StateTrigger.AttributeDefinition
            BlockType.PARAGRAPH -> StateTrigger.TextLine
        }
    }
    
    private fun addError(message: String, location: SourceLocation, severity: ErrorSeverity = ErrorSeverity.ERROR) {
        errors.add(ParseError(message, location, severity))
    }
    
    private fun addWarning(message: String, location: SourceLocation) {
        warnings.add(ParseWarning(message, location))
    }
}