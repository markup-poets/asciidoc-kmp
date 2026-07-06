package org.markup.poet.asciidoc.render

import org.markup.poet.asciidoc.asg.AsgDocument
import org.markup.poet.asciidoc.asg.AsgNode

/**
 * Interface for custom renderers that can override default rendering behavior.
 *
 * Custom renderers allow users to provide specialized rendering logic for
 * specific ASG node types. This enables customization of HTML output without
 * modifying the core renderer implementation.
 *
 * Renderers are keyed in [RenderConfig.customRenderers] by either:
 * - the node class simple name (e.g. `"LeafBlock"`, `"SectionBlock"`), or
 * - for [org.markup.poet.asciidoc.asg.LeafBlock] nodes carrying a non-built-in
 *   block style (`metadata.positional.first()`), the style name itself
 *   (e.g. `"gallery"`). The style key is checked first.
 *
 * Example use cases:
 * - Custom styling for specific block types
 * - Integration with third-party libraries (e.g., syntax highlighters)
 * - Special handling for domain-specific block styles
 * - Custom HTML structure for certain elements
 *
 * @see RenderConfig.customRenderers
 */
interface CustomRenderer {
    /**
     * Renders an ASG node to HTML.
     *
     * This method is called instead of the default renderer when a custom
     * renderer is registered for the node's type (or block style).
     *
     * @param node The ASG node to render
     * @param context The rendering context containing configuration and state
     * @return The HTML string representation of the node
     */
    fun render(node: AsgNode, context: RenderContext): String
}

/**
 * Interface for custom attribute handlers that can modify rendering based on node attributes.
 *
 * Attribute handlers allow users to customize rendering behavior based on
 * the metadata present on ASG nodes. This enables conditional styling, special
 * processing, or custom HTML generation based on attribute values.
 *
 * Example use cases:
 * - Adding custom CSS classes based on roles
 * - Generating data attributes for JavaScript integration
 * - Conditional rendering based on option flags
 * - Custom styling for specific attribute combinations
 *
 * @see RenderConfig.attributeHandlers
 */
interface AttributeHandler {
    /**
     * Processes node attributes and returns additional HTML attributes to apply.
     *
     * This method is called during rendering to allow attribute-based customization.
     * The returned map contains HTML attribute names and values that will be
     * added to the rendered element.
     *
     * @param node The ASG node being rendered
     * @param context The rendering context
     * @return Map of HTML attribute names to values to add to the element
     */
    fun processAttributes(node: AsgNode, context: RenderContext): Map<String, String>
}

/**
 * Interface for custom document templates.
 *
 * Templates allow users to customize the overall HTML document structure
 * when rendering in standalone mode. This enables custom layouts, additional
 * sections, or integration with specific frameworks.
 *
 * @see RenderConfig.documentTemplate
 */
interface DocumentTemplate {
    /**
     * Generates the complete HTML document structure.
     *
     * This method is called in standalone mode to wrap the rendered body content
     * in a complete HTML document. The template has full control over the
     * document structure, including head and body sections.
     *
     * @param bodyContent The rendered body HTML content
     * @param document The source document for metadata
     * @param config The rendering configuration
     * @param context The rendering context
     * @return Complete HTML document string
     */
    fun generateDocument(
        bodyContent: String,
        document: AsgDocument,
        config: RenderConfig,
        context: RenderContext
    ): String
}
