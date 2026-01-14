package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.ast.*

/**
 * Default implementation of AttributeSubstitutor.
 * Traverses the document AST and substitutes attribute references with their values.
 */
class DefaultAttributeSubstitutor : AttributeSubstitutor {
    
    override fun substitute(document: Document, config: AttributeConfig): SubstitutionResult {
        val errors = mutableListOf<ProcessingError>()
        val substitutedAttributes = mutableSetOf<String>()
        
        // Build the attribute map from document attributes and defaults
        val attributeMap = config.defaults.toMutableMap()
        attributeMap.putAll(document.documentAttributes)
        
        // Process the document
        val processedDocument = processDocument(document, attributeMap, config, errors, substitutedAttributes)
        
        return SubstitutionResult(
            document = processedDocument,
            errors = errors,
            substitutedAttributes = substitutedAttributes
        )
    }
    
    private fun processDocument(
        document: Document,
        attributeMap: Map<String, String>,
        config: AttributeConfig,
        errors: MutableList<ProcessingError>,
        substitutedAttributes: MutableSet<String>
    ): Document {
        val processedChildren = document.children.map { child ->
            processBlockElement(child, attributeMap, config, errors, substitutedAttributes)
        }
        
        return document.copy(children = processedChildren)
    }
    
    private fun processBlockElement(
        element: BlockElement,
        attributeMap: Map<String, String>,
        config: AttributeConfig,
        errors: MutableList<ProcessingError>,
        substitutedAttributes: MutableSet<String>
    ): BlockElement {
        return when (element) {
            is Section -> element.copy(
                children = element.children.map { child ->
                    processBlockElement(child, attributeMap, config, errors, substitutedAttributes)
                }
            )
            is Paragraph -> element.copy(
                content = element.content.map { inline ->
                    processInlineElement(inline, attributeMap, config, errors, substitutedAttributes)
                }
            )
            is AsciiDocList -> element.copy(
                items = element.items.map { item ->
                    processListItem(item, attributeMap, config, errors, substitutedAttributes)
                }
            )
            is ListItem -> processListItem(element, attributeMap, config, errors, substitutedAttributes)
            is CalloutList -> element.copy(
                items = element.items.map { item ->
                    item.copy(
                        content = item.content.map { inline ->
                            processInlineElement(inline, attributeMap, config, errors, substitutedAttributes)
                        }
                    )
                }
            )
            is CodeBlock -> element // DO NOT substitute attributes in code blocks
            is Document -> processDocument(element, attributeMap, config, errors, substitutedAttributes)
            else -> element
        }
    }
    
    private fun processListItem(
        item: ListItem,
        attributeMap: Map<String, String>,
        config: AttributeConfig,
        errors: MutableList<ProcessingError>,
        substitutedAttributes: MutableSet<String>
    ): ListItem {
        val processedContent = item.content.map { inline ->
            processInlineElement(inline, attributeMap, config, errors, substitutedAttributes)
        }
        val processedNestedList = item.nestedList?.let { nestedList ->
            processBlockElement(nestedList, attributeMap, config, errors, substitutedAttributes) as AsciiDocList
        }
        return item.copy(content = processedContent, nestedList = processedNestedList)
    }
    
    private fun processInlineElement(
        element: InlineElement,
        attributeMap: Map<String, String>,
        config: AttributeConfig,
        errors: MutableList<ProcessingError>,
        substitutedAttributes: MutableSet<String>
    ): InlineElement {
        return when (element) {
            is Text -> processTextElement(element, attributeMap, config, errors, substitutedAttributes)
            is Strong -> element.copy(
                content = element.content.map { inline ->
                    processInlineElement(inline, attributeMap, config, errors, substitutedAttributes)
                }
            )
            is Emphasis -> element.copy(
                content = element.content.map { inline ->
                    processInlineElement(inline, attributeMap, config, errors, substitutedAttributes)
                }
            )
            is AttributeReference -> processAttributeReference(element, attributeMap, config, errors, substitutedAttributes)
            else -> element
        }
    }
    
    /**
     * Process a Text element by finding and replacing {key} patterns.
     */
    private fun processTextElement(
        text: Text,
        attributeMap: Map<String, String>,
        config: AttributeConfig,
        errors: MutableList<ProcessingError>,
        substitutedAttributes: MutableSet<String>
    ): InlineElement {
        val pattern = Regex("""\{([a-zA-Z_][a-zA-Z0-9_-]*)\}""")
        val matches = pattern.findAll(text.content).toList()
        
        if (matches.isEmpty()) {
            return text
        }
        
        var result = text.content
        // Process matches in reverse order to maintain correct indices
        for (match in matches.reversed()) {
            val key = match.groupValues[1]
            val replacement = resolveAttribute(key, attributeMap, config, text.sourceLocation, errors, substitutedAttributes)
            result = result.replaceRange(match.range, replacement)
        }
        
        return text.copy(content = result)
    }
    
    /**
     * Process an AttributeReference element.
     */
    private fun processAttributeReference(
        ref: AttributeReference,
        attributeMap: Map<String, String>,
        config: AttributeConfig,
        errors: MutableList<ProcessingError>,
        substitutedAttributes: MutableSet<String>
    ): InlineElement {
        val value = resolveAttribute(ref.key, attributeMap, config, ref.sourceLocation, errors, substitutedAttributes)
        return Text(content = value, attributes = ref.attributes, sourceLocation = ref.sourceLocation)
    }
    
    /**
     * Resolve an attribute key to its value, handling recursive references.
     */
    private fun resolveAttribute(
        key: String,
        attributeMap: Map<String, String>,
        config: AttributeConfig,
        location: SourceLocation,
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
        location: SourceLocation,
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
        location: SourceLocation,
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
