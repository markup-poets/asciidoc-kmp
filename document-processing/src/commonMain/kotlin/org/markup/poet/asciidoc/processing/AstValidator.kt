package org.markup.poet.asciidoc.processing

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.Block
import org.markup.poet.asciidoc.asg.DListBlock
import org.markup.poet.asciidoc.asg.Inline
import org.markup.poet.asciidoc.asg.InlineRef
import org.markup.poet.asciidoc.asg.InlineSpan
import org.markup.poet.asciidoc.asg.LeafBlock
import org.markup.poet.asciidoc.asg.ListBlock
import org.markup.poet.asciidoc.asg.ParentBlock
import org.markup.poet.asciidoc.asg.RefVariant
import org.markup.poet.asciidoc.asg.SectionBlock
import org.markup.poet.asciidoc.asg.SpanVariant

/**
 * Validates ASG structure for correctness.
 * Used to validate output from custom processors.
 */
object AstValidator {
    /**
     * Validates a document's ASG structure.
     * Returns a list of validation errors if any are found.
     */
    fun validateDocument(document: AsgDocument): List<String> {
        val errors = mutableListOf<String>()

        // Validate document attributes
        for ((key, _) in document.attributes) {
            if (key.isEmpty()) {
                errors.add("Document attribute key cannot be empty")
            }
        }

        // Validate blocks
        for (block in document.blocks) {
            errors.addAll(validateBlock(block))
        }

        return errors
    }

    /**
     * Validates a block.
     */
    private fun validateBlock(block: Block): List<String> {
        val errors = mutableListOf<String>()

        when (block) {
            is SectionBlock -> {
                // Validate section level (ASG levels: '==' is level 1, up to '======' at level 5)
                if (block.level < 0 || block.level > 5) {
                    errors.add("Section level must be between 0 and 5, got ${block.level}")
                }

                // Validate title
                if (block.title.isEmpty()) {
                    errors.add("Section title cannot be empty")
                }

                // Validate child blocks
                for (child in block.blocks) {
                    errors.addAll(validateBlock(child))
                }
            }

            is LeafBlock -> {
                // Validate inline content
                for (inline in block.inlines) {
                    errors.addAll(validateInline(inline))
                }
            }

            is ParentBlock -> {
                for (child in block.blocks) {
                    errors.addAll(validateBlock(child))
                }
            }

            is ListBlock -> {
                for (item in block.items) {
                    for (inline in item.principal) {
                        errors.addAll(validateInline(inline))
                    }
                    for (child in item.blocks) {
                        errors.addAll(validateBlock(child))
                    }
                }
            }

            is DListBlock -> {
                for (item in block.items) {
                    for (term in item.terms) {
                        for (inline in term) {
                            errors.addAll(validateInline(inline))
                        }
                    }
                    for (inline in item.principal) {
                        errors.addAll(validateInline(inline))
                    }
                    for (child in item.blocks) {
                        errors.addAll(validateBlock(child))
                    }
                }
            }

            else -> {
                // Other block types are assumed valid
            }
        }

        return errors
    }

    /**
     * Validates an inline element.
     */
    private fun validateInline(inline: Inline): List<String> {
        val errors = mutableListOf<String>()

        when (inline) {
            is InlineSpan -> {
                if (inline.variant == SpanVariant.CODE && inline.inlines.isEmpty()) {
                    errors.add("Code content cannot be empty")
                }
                for (content in inline.inlines) {
                    errors.addAll(validateInline(content))
                }
            }

            is InlineRef -> {
                if (inline.variant == RefVariant.LINK && inline.target.isEmpty()) {
                    errors.add("Link URL cannot be empty")
                }
                for (content in inline.inlines) {
                    errors.addAll(validateInline(content))
                }
            }

            else -> {
                // Other inline types are assumed valid
            }
        }

        return errors
    }
}
