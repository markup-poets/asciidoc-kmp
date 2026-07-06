package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.Block
import org.markup.poet.asciidoc.asg.DListBlock
import org.markup.poet.asciidoc.asg.Inline
import org.markup.poet.asciidoc.asg.InlineAttributeRef
import org.markup.poet.asciidoc.asg.InlineSpan
import org.markup.poet.asciidoc.asg.InlineText
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.LeafBlockName
import org.markup.poet.asciidoc.asg.ListBlock
import org.markup.poet.asciidoc.asg.Location
import org.markup.poet.asciidoc.asg.ParentBlock
import org.markup.poet.asciidoc.asg.SectionBlock
import org.markup.poet.asciidoc.asg.SpanVariant

/**
 * Default implementation of AttributeSubstitutor.
 * Traverses the ASG tree and substitutes attribute references with their values.
 */
class DefaultAttributeSubstitutor : AttributeSubstitutor {

    override fun substitute(document: AsgDocument, config: AttributeConfig): SubstitutionResult {
        val errors = mutableListOf<ProcessingError>()
        val substitutedAttributes = mutableSetOf<String>()

        // Build the attribute map from document attributes and defaults
        val attributeMap = config.defaults.toMutableMap()
        attributeMap.putAll(document.attributes)

        val processedBlocks = document.blocks.map { block ->
            processBlock(block, attributeMap, config, errors, substitutedAttributes)
        }

        return SubstitutionResult(
            document = document.copy(blocks = processedBlocks),
            errors = errors,
            substitutedAttributes = substitutedAttributes
        )
    }

    private fun processBlock(
        block: Block,
        attributeMap: Map<String, String>,
        config: AttributeConfig,
        errors: MutableList<ProcessingError>,
        substitutedAttributes: MutableSet<String>
    ): Block {
        fun recurse(blocks: List<Block>): List<Block> =
            blocks.map { processBlock(it, attributeMap, config, errors, substitutedAttributes) }

        fun mapInlines(inlines: List<Inline>): List<Inline> =
            inlines.map { processInline(it, attributeMap, config, errors, substitutedAttributes) }

        return when (block) {
            is SectionBlock -> block.copy(blocks = recurse(block.blocks))
            // DO NOT substitute attributes in verbatim blocks (listing/literal/pass/stem/verse).
            is LeafBlock -> if (block.name == LeafBlockName.PARAGRAPH) {
                block.copy(inlines = mapInlines(block.inlines))
            } else {
                block
            }
            is ParentBlock -> block.copy(blocks = recurse(block.blocks))
            is ListBlock -> block.copy(items = block.items.map { item ->
                item.copy(principal = mapInlines(item.principal), blocks = recurse(item.blocks))
            })
            is DListBlock -> block.copy(items = block.items.map { item ->
                item.copy(
                    terms = item.terms.map { mapInlines(it) },
                    principal = mapInlines(item.principal),
                    blocks = recurse(item.blocks)
                )
            })
            else -> block
        }
    }

    private fun processInline(
        inline: Inline,
        attributeMap: Map<String, String>,
        config: AttributeConfig,
        errors: MutableList<ProcessingError>,
        substitutedAttributes: MutableSet<String>
    ): Inline {
        return when (inline) {
            is InlineText -> processTextElement(inline, attributeMap, config, errors, substitutedAttributes)
            // Code spans are verbatim; other spans get their content substituted.
            is InlineSpan -> if (inline.variant == SpanVariant.CODE) {
                inline
            } else {
                inline.copy(inlines = inline.inlines.map { nested ->
                    processInline(nested, attributeMap, config, errors, substitutedAttributes)
                })
            }
            is InlineAttributeRef -> processAttributeReference(inline, attributeMap, config, errors, substitutedAttributes)
            else -> inline
        }
    }

    /**
     * Process an InlineText element by finding and replacing {key} patterns.
     */
    private fun processTextElement(
        text: InlineText,
        attributeMap: Map<String, String>,
        config: AttributeConfig,
        errors: MutableList<ProcessingError>,
        substitutedAttributes: MutableSet<String>
    ): Inline {
        val pattern = Regex("""\{([a-zA-Z_][a-zA-Z0-9_-]*)\}""")
        val matches = pattern.findAll(text.value).toList()

        if (matches.isEmpty()) {
            return text
        }

        var result = text.value
        // Process matches in reverse order to maintain correct indices
        for (match in matches.reversed()) {
            val key = match.groupValues[1]
            val replacement = resolveAttribute(key, attributeMap, config, text.location, errors, substitutedAttributes)
            result = result.replaceRange(match.range, replacement)
        }

        return text.copy(value = result)
    }

    /**
     * Process an InlineAttributeRef element.
     */
    private fun processAttributeReference(
        ref: InlineAttributeRef,
        attributeMap: Map<String, String>,
        config: AttributeConfig,
        errors: MutableList<ProcessingError>,
        substitutedAttributes: MutableSet<String>
    ): Inline {
        val value = resolveAttribute(ref.name, attributeMap, config, ref.location, errors, substitutedAttributes)
        return InlineText(value = value, location = ref.location)
    }

    /**
     * Resolve an attribute key to its value, handling recursive references.
     */
    private fun resolveAttribute(
        key: String,
        attributeMap: Map<String, String>,
        config: AttributeConfig,
        location: Location?,
        errors: MutableList<ProcessingError>,
        substitutedAttributes: MutableSet<String>
    ): String {
        return resolveAttributeRecursive(key, attributeMap, config, location, errors, substitutedAttributes, emptySet(), 0)
    }

    /**
     * Recursively resolve an attribute, tracking the resolution chain to detect cycles.
     */
    private fun resolveAttributeRecursive(
        key: String,
        attributeMap: Map<String, String>,
        config: AttributeConfig,
        location: Location?,
        errors: MutableList<ProcessingError>,
        substitutedAttributes: MutableSet<String>,
        resolutionChain: Set<String>,
        depth: Int
    ): String {
        // Check for maximum recursion depth
        if (depth >= config.maxRecursionDepth) {
            errors.add(
                ProcessingError(
                    message = "Maximum recursion depth exceeded while resolving attribute: $key",
                    location = location,
                    errorType = ProcessingErrorType.ATTRIBUTE_CIRCULAR_REFERENCE,
                    severity = ErrorSeverity.ERROR
                )
            )
            return "{$key}"
        }

        // Check for circular reference
        if (key in resolutionChain) {
            val cycle = (resolutionChain + key).joinToString(" -> ")
            errors.add(
                ProcessingError(
                    message = "Circular attribute reference detected: $cycle",
                    location = location,
                    errorType = ProcessingErrorType.ATTRIBUTE_CIRCULAR_REFERENCE,
                    severity = ErrorSeverity.ERROR
                )
            )
            return "{$key}"
        }

        val value = attributeMap[key]

        return when {
            value != null -> {
                substitutedAttributes.add(key)
                // Recursively resolve any attribute references in the value
                resolveAttributesInString(value, attributeMap, config, location, errors, substitutedAttributes, resolutionChain + key, depth + 1)
            }
            config.undefinedBehavior == UndefinedAttributeBehavior.PRESERVE -> {
                errors.add(
                    ProcessingError(
                        message = "Undefined attribute: $key",
                        location = location,
                        errorType = ProcessingErrorType.ATTRIBUTE_UNDEFINED,
                        severity = ErrorSeverity.WARNING
                    )
                )
                "{$key}"
            }
            config.undefinedBehavior == UndefinedAttributeBehavior.REMOVE -> {
                errors.add(
                    ProcessingError(
                        message = "Undefined attribute: $key",
                        location = location,
                        errorType = ProcessingErrorType.ATTRIBUTE_UNDEFINED,
                        severity = ErrorSeverity.WARNING
                    )
                )
                ""
            }
            config.undefinedBehavior == UndefinedAttributeBehavior.DEFAULT && config.defaults.containsKey(key) -> {
                substitutedAttributes.add(key)
                val defaultValue = config.defaults[key]!!
                resolveAttributesInString(defaultValue, attributeMap, config, location, errors, substitutedAttributes, resolutionChain + key, depth + 1)
            }
            else -> {
                errors.add(
                    ProcessingError(
                        message = "Undefined attribute: $key",
                        location = location,
                        errorType = ProcessingErrorType.ATTRIBUTE_UNDEFINED,
                        severity = ErrorSeverity.WARNING
                    )
                )
                "{$key}"
            }
        }
    }

    /**
     * Resolve all attribute references in a string.
     */
    private fun resolveAttributesInString(
        text: String,
        attributeMap: Map<String, String>,
        config: AttributeConfig,
        location: Location?,
        errors: MutableList<ProcessingError>,
        substitutedAttributes: MutableSet<String>,
        resolutionChain: Set<String>,
        depth: Int
    ): String {
        val pattern = Regex("""\{([a-zA-Z_][a-zA-Z0-9_-]*)\}""")
        val matches = pattern.findAll(text).toList()

        if (matches.isEmpty()) {
            return text
        }

        var result = text
        // Process matches in reverse order to maintain correct indices
        for (match in matches.reversed()) {
            val key = match.groupValues[1]
            val replacement = resolveAttributeRecursive(key, attributeMap, config, location, errors, substitutedAttributes, resolutionChain, depth)
            result = result.replaceRange(match.range, replacement)
        }

        return result
    }
}
