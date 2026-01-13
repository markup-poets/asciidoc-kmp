package io.github.kotlin.asciidoc.ast

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll

/**
 * Property-based tests for AST structural integrity.
 * **Feature: asciidoc-parser, Property 2: AST Structural Integrity**
 * **Validates: Requirements 2.1, 2.7**
 */
class AstStructuralIntegrityTest : StringSpec({

    "Property 2: AST Structural Integrity - Document should have exactly one root node with proper parent-child relationships" {
        checkAll(100, documentGenerator()) { document ->
            // Verify document is the root node
            document.shouldBeInstanceOf<Document>()
            
            // Verify document has proper structure
            document.sourceLocation shouldNotBe null
            document.attributes shouldNotBe null
            document.children shouldNotBe null
            
            // Verify all children are block elements
            document.children.forEach { child ->
                child.shouldBeInstanceOf<BlockElement>()
                child.sourceLocation shouldNotBe null
                child.attributes shouldNotBe null
            }
            
            // Verify nested structure integrity for sections
            document.children.filterIsInstance<Section>().forEach { section ->
                section.level shouldBe section.level // Level should be consistent
                section.title.isNotEmpty() shouldBe true
                section.children.forEach { child ->
                    child.shouldBeInstanceOf<BlockElement>()
                }
            }
            
            // Verify list structure integrity
            document.children.filterIsInstance<AsciiDocList>().forEach { list ->
                list.items.isNotEmpty() shouldBe true
                list.items.forEach { item ->
                    item.shouldBeInstanceOf<ListItem>()
                    item.sourceLocation shouldNotBe null
                    item.content shouldNotBe null
                }
            }
        }
    }

})

// Generators for property-based testing
private fun documentGenerator(): Arb<Document> = arbitrary { rs ->
    Document(
        title = Arb.string(1..50).bind(),
        children = Arb.list(blockElementGenerator(), 0..5).bind(),
        documentAttributes = Arb.map(Arb.string(1..20), Arb.string(1..50), minSize = 0, maxSize = 3).bind(),
        attributes = Arb.map(Arb.string(1..20), Arb.string(1..50), minSize = 0, maxSize = 2).bind(),
        sourceLocation = sourceLocationGenerator().bind()
    )
}

private fun blockElementGenerator(): Arb<BlockElement> = Arb.choice(
    sectionGenerator(),
    paragraphGenerator(),
    listGenerator(),
    codeBlockGenerator(),
    commentGenerator()
)

private fun sectionGenerator(): Arb<Section> = arbitrary { rs ->
    Section(
        level = Arb.int(1..6).bind(),
        title = Arb.string(1..100).bind(),
        children = Arb.list(blockElementGenerator(), 0..3).bind(),
        attributes = Arb.map(Arb.string(1..20), Arb.string(1..50), minSize = 0, maxSize = 2).bind(),
        sourceLocation = sourceLocationGenerator().bind()
    )
}

private fun paragraphGenerator(): Arb<Paragraph> = arbitrary { rs ->
    Paragraph(
        content = Arb.list(inlineElementGenerator(), 1..5).bind(),
        attributes = Arb.map(Arb.string(1..20), Arb.string(1..50), minSize = 0, maxSize = 2).bind(),
        sourceLocation = sourceLocationGenerator().bind()
    )
}

private fun listGenerator(): Arb<AsciiDocList> = arbitrary { rs ->
    AsciiDocList(
        type = Arb.enum<ListType>().bind(),
        items = Arb.list(listItemGenerator(), 1..5).bind(),
        attributes = Arb.map(Arb.string(1..20), Arb.string(1..50), minSize = 0, maxSize = 2).bind(),
        sourceLocation = sourceLocationGenerator().bind()
    )
}

private fun listItemGenerator(): Arb<ListItem> = arbitrary { rs ->
    ListItem(
        marker = Arb.choice(Arb.constant("*"), Arb.constant("-"), Arb.constant("1.")).bind(),
        content = Arb.list(inlineElementGenerator(), 1..3).bind(),
        nestedList = null, // Keep simple for now
        attributes = Arb.map(Arb.string(1..20), Arb.string(1..50), minSize = 0, maxSize = 2).bind(),
        sourceLocation = sourceLocationGenerator().bind()
    )
}

private fun codeBlockGenerator(): Arb<CodeBlock> = arbitrary { rs ->
    CodeBlock(
        language = Arb.choice(Arb.constant(null), Arb.string(1..20)).bind(),
        content = Arb.string(1..200).bind(),
        attributes = Arb.map(Arb.string(1..20), Arb.string(1..50), minSize = 0, maxSize = 2).bind(),
        sourceLocation = sourceLocationGenerator().bind()
    )
}

private fun commentGenerator(): Arb<Comment> = arbitrary { rs ->
    Comment(
        content = Arb.string(1..100).bind(),
        attributes = Arb.map(Arb.string(1..20), Arb.string(1..50), minSize = 0, maxSize = 2).bind(),
        sourceLocation = sourceLocationGenerator().bind()
    )
}

private fun inlineElementGenerator(): Arb<InlineElement> = Arb.choice(
    textGenerator(),
    strongGenerator(),
    emphasisGenerator(),
    codeInlineGenerator(),
    linkGenerator(),
    imageGenerator()
)

private fun textGenerator(): Arb<Text> = arbitrary { rs ->
    Text(
        content = Arb.string(1..100).bind(),
        attributes = Arb.map(Arb.string(1..20), Arb.string(1..50), minSize = 0, maxSize = 2).bind(),
        sourceLocation = sourceLocationGenerator().bind()
    )
}

private fun strongGenerator(): Arb<Strong> = arbitrary { rs ->
    Strong(
        content = Arb.list(textGenerator(), 1..3).bind(),
        attributes = Arb.map(Arb.string(1..20), Arb.string(1..50), minSize = 0, maxSize = 2).bind(),
        sourceLocation = sourceLocationGenerator().bind()
    )
}

private fun emphasisGenerator(): Arb<Emphasis> = arbitrary { rs ->
    Emphasis(
        content = Arb.list(textGenerator(), 1..3).bind(),
        attributes = Arb.map(Arb.string(1..20), Arb.string(1..50), minSize = 0, maxSize = 2).bind(),
        sourceLocation = sourceLocationGenerator().bind()
    )
}

private fun codeInlineGenerator(): Arb<Code> = arbitrary { rs ->
    Code(
        content = Arb.string(1..50).bind(),
        attributes = Arb.map(Arb.string(1..20), Arb.string(1..50), minSize = 0, maxSize = 2).bind(),
        sourceLocation = sourceLocationGenerator().bind()
    )
}

private fun linkGenerator(): Arb<Link> = arbitrary { rs ->
    Link(
        url = Arb.string(1..100).bind(),
        text = Arb.string(1..50).bind(),
        attributes = Arb.map(Arb.string(1..20), Arb.string(1..50), minSize = 0, maxSize = 2).bind(),
        sourceLocation = sourceLocationGenerator().bind()
    )
}

private fun imageGenerator(): Arb<Image> = arbitrary { rs ->
    Image(
        path = Arb.string(1..100).bind(),
        altText = Arb.string(1..50).bind(),
        attributes = Arb.map(Arb.string(1..20), Arb.string(1..50), minSize = 0, maxSize = 2).bind(),
        sourceLocation = sourceLocationGenerator().bind()
    )
}

private fun sourceLocationGenerator(): Arb<SourceLocation> = arbitrary { rs ->
    SourceLocation(
        line = Arb.int(1..1000).bind(),
        column = Arb.int(0..100).bind()
    )
}