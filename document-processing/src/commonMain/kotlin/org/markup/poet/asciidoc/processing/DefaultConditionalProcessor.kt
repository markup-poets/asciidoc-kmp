package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.Block
import org.markup.poet.asciidoc.asg.ConditionalBlock
import org.markup.poet.asciidoc.asg.ConditionalVariant
import org.markup.poet.asciidoc.asg.DListBlock
import org.markup.poet.asciidoc.asg.ListBlock
import org.markup.poet.asciidoc.asg.Location
import org.markup.poet.asciidoc.asg.ParentBlock
import org.markup.poet.asciidoc.asg.SectionBlock

/**
 * Default implementation of ConditionalProcessor.
 * Evaluates conditional directives (ifdef, ifndef, ifeval) and includes/excludes content
 * based on attribute presence and expression evaluation.
 */
class DefaultConditionalProcessor : ConditionalProcessor {

    override fun process(document: AsgDocument, config: ConditionalConfig): ConditionalResult {
        val errors = mutableListOf<ProcessingError>()
        val warnings = mutableListOf<ProcessingWarning>()
        var evaluatedCount = 0

        val processedBlocks = processBlocks(
            document.blocks,
            config,
            errors,
            warnings,
            0
        ) { evaluatedCount++ }

        return ConditionalResult(
            document = document.copy(blocks = processedBlocks),
            errors = errors,
            warnings = warnings,
            evaluatedConditionals = evaluatedCount
        )
    }

    private fun processBlocks(
        blocks: List<Block>,
        config: ConditionalConfig,
        errors: MutableList<ProcessingError>,
        warnings: MutableList<ProcessingWarning>,
        depth: Int,
        onEvaluated: () -> Unit
    ): List<Block> {
        val result = mutableListOf<Block>()

        for (block in blocks) {
            when (block) {
                is ConditionalBlock -> {
                    // Check nesting depth
                    if (depth >= config.maxNestingDepth) {
                        errors.add(
                            ProcessingError(
                                message = "Maximum conditional nesting depth (${config.maxNestingDepth}) exceeded",
                                location = block.location,
                                errorType = ProcessingErrorType.CONDITIONAL_MAX_DEPTH_EXCEEDED,
                                severity = ErrorSeverity.ERROR
                            )
                        )
                        continue
                    }

                    onEvaluated()

                    // Evaluate the conditional
                    val shouldInclude = evaluateConditional(block, config, errors)

                    // Select content based on evaluation
                    val selectedContent = if (shouldInclude) block.blocks else block.elseBlocks

                    // Process the selected content recursively
                    result.addAll(
                        processBlocks(selectedContent, config, errors, warnings, depth + 1, onEvaluated)
                    )
                }
                is SectionBlock -> result.add(
                    block.copy(blocks = processBlocks(block.blocks, config, errors, warnings, depth, onEvaluated))
                )
                is ParentBlock -> result.add(
                    block.copy(blocks = processBlocks(block.blocks, config, errors, warnings, depth, onEvaluated))
                )
                is ListBlock -> result.add(
                    block.copy(items = block.items.map { item ->
                        item.copy(blocks = processBlocks(item.blocks, config, errors, warnings, depth, onEvaluated))
                    })
                )
                is DListBlock -> result.add(
                    block.copy(items = block.items.map { item ->
                        item.copy(blocks = processBlocks(item.blocks, config, errors, warnings, depth, onEvaluated))
                    })
                )
                else -> result.add(block)
            }
        }

        return result
    }

    /**
     * Evaluate a conditional directive based on its variant.
     */
    private fun evaluateConditional(
        directive: ConditionalBlock,
        config: ConditionalConfig,
        errors: MutableList<ProcessingError>
    ): Boolean {
        return when (directive.variant) {
            ConditionalVariant.IFDEF -> evaluateIfdef(directive.condition, config)
            ConditionalVariant.IFNDEF -> evaluateIfndef(directive.condition, config)
            ConditionalVariant.IFEVAL -> evaluateIfeval(directive.condition, config, directive.location, errors)
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
        location: Location?,
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
