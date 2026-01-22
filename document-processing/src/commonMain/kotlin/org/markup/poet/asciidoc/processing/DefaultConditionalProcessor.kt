package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.ast.*

/**
 * Default implementation of ConditionalProcessor.
 * Evaluates conditional directives (ifdef, ifndef, ifeval) and includes/excludes content
 * based on attribute presence and expression evaluation.
 */
class DefaultConditionalProcessor : ConditionalProcessor {
    
    override fun process(document: Document, config: ConditionalConfig): ConditionalResult {
        val errors = mutableListOf<ProcessingError>()
        val warnings = mutableListOf<ProcessingWarning>()
        var evaluatedCount = 0
        
        val processedDocument = processDocument(
            document, 
            config, 
            errors, 
            warnings, 
            0,
            { evaluatedCount++ }
        )
        
        return ConditionalResult(
            document = processedDocument,
            errors = errors,
            warnings = warnings,
            evaluatedConditionals = evaluatedCount
        )
    }
    
    private fun processDocument(
        document: Document,
        config: ConditionalConfig,
        errors: MutableList<ProcessingError>,
        warnings: MutableList<ProcessingWarning>,
        depth: Int,
        onEvaluated: () -> Unit
    ): Document {
        val processedChildren = processBlockElements(
            document.children,
            config,
            errors,
            warnings,
            depth,
            onEvaluated
        )
        
        return document.copy(children = processedChildren)
    }
    
    private fun processBlockElements(
        elements: List<BlockElement>,
        config: ConditionalConfig,
        errors: MutableList<ProcessingError>,
        warnings: MutableList<ProcessingWarning>,
        depth: Int,
        onEvaluated: () -> Unit
    ): List<BlockElement> {
        val result = mutableListOf<BlockElement>()
        
        for (element in elements) {
            when (element) {
                is ConditionalDirective -> {
                    // Check nesting depth
                    if (depth >= config.maxNestingDepth) {
                        errors.add(
                            ProcessingError(
                                message = "Maximum conditional nesting depth (${config.maxNestingDepth}) exceeded",
                                location = element.sourceLocation,
                                errorType = ProcessingErrorType.CONDITIONAL_MAX_DEPTH_EXCEEDED,
                                severity = ErrorSeverity.ERROR
                            )
                        )
                        continue
                    }
                    
                    onEvaluated()
                    
                    // Evaluate the conditional
                    val shouldInclude = evaluateConditional(element, config, errors)
                    
                    // Select content based on evaluation
                    val selectedContent = if (shouldInclude) element.content else element.elseContent
                    
                    // Process the selected content recursively
                    val processedContent = processBlockElements(
                        selectedContent,
                        config,
                        errors,
                        warnings,
                        depth + 1,
                        onEvaluated
                    )
                    
                    result.addAll(processedContent)
                }
                is Section -> {
                    val processedChildren = processBlockElements(
                        element.children,
                        config,
                        errors,
                        warnings,
                        depth,
                        onEvaluated
                    )
                    result.add(element.copy(children = processedChildren))
                }
                is AsciiDocList -> {
                    val processedItems = element.items.map { item ->
                        processListItem(item, config, errors, warnings, depth, onEvaluated)
                    }
                    result.add(element.copy(items = processedItems))
                }
                is Document -> {
                    result.add(processDocument(element, config, errors, warnings, depth, onEvaluated))
                }
                else -> {
                    result.add(element)
                }
            }
        }
        
        return result
    }
    
    private fun processListItem(
        item: ListItem,
        config: ConditionalConfig,
        errors: MutableList<ProcessingError>,
        warnings: MutableList<ProcessingWarning>,
        depth: Int,
        onEvaluated: () -> Unit
    ): ListItem {
        val processedNestedList = item.nestedList?.let { nestedList ->
            val processedItems = nestedList.items.map { nestedItem ->
                processListItem(nestedItem, config, errors, warnings, depth, onEvaluated)
            }
            nestedList.copy(items = processedItems)
        }
        return item.copy(nestedList = processedNestedList)
    }
    
    /**
     * Evaluate a conditional directive based on its type.
     */
    private fun evaluateConditional(
        directive: ConditionalDirective,
        config: ConditionalConfig,
        errors: MutableList<ProcessingError>
    ): Boolean {
        return when (directive.type) {
            ConditionalType.IFDEF -> evaluateIfdef(directive.condition, config)
            ConditionalType.IFNDEF -> evaluateIfndef(directive.condition, config)
            ConditionalType.IFEVAL -> evaluateIfeval(directive.condition, config, directive.sourceLocation, errors)
        }
    }
    
    /**
     * Evaluate an ifdef directive.
     * Returns true if the specified attribute(s) are defined.
     */
    private fun evaluateIfdef(condition: String, config: ConditionalConfig): Boolean {
        return evaluateAttributeCondition(condition, config, true)
    }
    
    /**
     * Evaluate an ifndef directive.
     * Returns true if the specified attribute(s) are NOT defined.
     */
    private fun evaluateIfndef(condition: String, config: ConditionalConfig): Boolean {
        return evaluateAttributeCondition(condition, config, false)
    }
    
    /**
     * Evaluate an attribute condition with support for multiple attributes and logical operators.
     * 
     * @param condition The condition string (e.g., "attr1", "attr1,attr2", "attr1+attr2")
     * @param config The configuration containing defined attributes
     * @param checkDefined If true, check if attributes are defined; if false, check if undefined
     * @return True if the condition is satisfied
     */
    private fun evaluateAttributeCondition(
        condition: String,
        config: ConditionalConfig,
        checkDefined: Boolean
    ): Boolean {
        val trimmed = condition.trim()
        
        // Check for OR operator (comma-separated)
        if (trimmed.contains(',')) {
            val attributes = trimmed.split(',').map { it.trim() }
            return attributes.any { attr ->
                val isDefined = config.definedAttributes.contains(attr)
                if (checkDefined) isDefined else !isDefined
            }
        }
        
        // Check for AND operator (plus-separated)
        if (trimmed.contains('+')) {
            val attributes = trimmed.split('+').map { it.trim() }
            return attributes.all { attr ->
                val isDefined = config.definedAttributes.contains(attr)
                if (checkDefined) isDefined else !isDefined
            }
        }
        
        // Single attribute
        val isDefined = config.definedAttributes.contains(trimmed)
        return if (checkDefined) isDefined else !isDefined
    }
    
    /**
     * Evaluate an ifeval directive.
     * Parses and evaluates conditional expressions with comparison operators.
     */
    private fun evaluateIfeval(
        condition: String,
        config: ConditionalConfig,
        location: SourceLocation,
        errors: MutableList<ProcessingError>
    ): Boolean {
        val trimmed = condition.trim()
        
        // Parse the expression: "{attr}" operator "value"
        val expressionPattern = Regex("""^\s*\{([^}]+)\}\s*(==|!=|<=|>=|<|>)\s*"([^"]*)"\s*$""")
        val match = expressionPattern.matchEntire(trimmed)
        
        if (match == null) {
            errors.add(
                ProcessingError(
                    message = "Invalid ifeval expression: $condition",
                    location = location,
                    errorType = ProcessingErrorType.CONDITIONAL_INVALID_EXPRESSION,
                    severity = ErrorSeverity.ERROR
                )
            )
            return false
        }
        
        val attributeName = match.groupValues[1]
        val operator = match.groupValues[2]
        val compareValue = match.groupValues[3]
        
        // Get the attribute value (empty string if not defined)
        val attributeValue = if (config.definedAttributes.contains(attributeName)) {
            attributeName // For simplicity, we use the attribute name as its value
        } else {
            ""
        }
        
        return evaluateComparison(attributeValue, operator, compareValue)
    }
    
    /**
     * Evaluate a comparison expression.
     */
    private fun evaluateComparison(left: String, operator: String, right: String): Boolean {
        return when (operator) {
            "==" -> left == right
            "!=" -> left != right
            "<" -> left < right
            ">" -> left > right
            "<=" -> left <= right
            ">=" -> left >= right
            else -> false
        }
    }
}
