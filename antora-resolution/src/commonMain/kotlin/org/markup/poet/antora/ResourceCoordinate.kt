package org.markup.poet.antora

/**
 * Represents an Antora resource coordinate.
 * Examples:
 *   - partial$filename.adoc
 *   - example$code.java
 *   - page$other-page.adoc
 *   - image$diagram.png
 *   - module:page$file.adoc (cross-module reference)
 *   - component:module:page$file.adoc (cross-component reference)
 */
data class ResourceCoordinate(
    val type: ResourceType,
    val path: String,
    val module: String? = null,
    val component: String? = null
) {
    companion object {
        /**
         * Parse an Antora coordinate string into a ResourceCoordinate.
         * Returns null if the string is not a valid Antora coordinate.
         * 
         * Supported formats:
         * - type$path (e.g., partial$file.adoc)
         * - module:type$path (e.g., admin:page$file.adoc)
         * - component:module:type$path (e.g., mycomp:admin:page$file.adoc)
         * - relative/path (no prefix, returns RELATIVE type)
         */
        fun parse(coordinate: String): ResourceCoordinate? {
            if (coordinate.isBlank()) {
                return null
            }
            
            // Check if it contains a resource type marker ($)
            if (!coordinate.contains('$')) {
                // It's a relative path
                return ResourceCoordinate(
                    type = ResourceType.RELATIVE,
                    path = coordinate,
                    module = null,
                    component = null
                )
            }
            
            // Split by $ to separate the coordinate part from the path
            val parts = coordinate.split('$', limit = 2)
            if (parts.size != 2) {
                return null
            }
            
            val coordinatePart = parts[0]
            val path = parts[1]
            
            if (path.isEmpty()) {
                return null
            }
            
            // Parse the coordinate part (may contain component:module:type or module:type or just type)
            val coordinateSegments = coordinatePart.split(':')
            
            return when (coordinateSegments.size) {
                1 -> {
                    // Just type$path
                    val type = parseResourceType(coordinateSegments[0]) ?: return null
                    ResourceCoordinate(
                        type = type,
                        path = path,
                        module = null,
                        component = null
                    )
                }
                2 -> {
                    // module:type$path
                    val module = coordinateSegments[0]
                    val type = parseResourceType(coordinateSegments[1]) ?: return null
                    if (module.isEmpty()) {
                        return null
                    }
                    ResourceCoordinate(
                        type = type,
                        path = path,
                        module = module,
                        component = null
                    )
                }
                3 -> {
                    // component:module:type$path
                    val component = coordinateSegments[0]
                    val module = coordinateSegments[1]
                    val type = parseResourceType(coordinateSegments[2]) ?: return null
                    if (component.isEmpty() || module.isEmpty()) {
                        return null
                    }
                    ResourceCoordinate(
                        type = type,
                        path = path,
                        module = module,
                        component = component
                    )
                }
                else -> null
            }
        }
        
        private fun parseResourceType(typeStr: String): ResourceType? {
            return when (typeStr.lowercase()) {
                "partial" -> ResourceType.PARTIAL
                "example" -> ResourceType.EXAMPLE
                "page" -> ResourceType.PAGE
                "image" -> ResourceType.IMAGE
                "attachment" -> ResourceType.ATTACHMENT
                else -> null
            }
        }
    }
}

enum class ResourceType {
    PARTIAL,    // partial$
    EXAMPLE,    // example$
    PAGE,       // page$
    IMAGE,      // image$
    ATTACHMENT, // attachment$
    RELATIVE    // No prefix, relative path
}
