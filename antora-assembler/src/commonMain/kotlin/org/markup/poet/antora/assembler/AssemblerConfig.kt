package org.markup.poet.antora.assembler

data class AssemblerConfig(
    val indexFile: String,
    val outputFile: String,
    val componentRoot: String,
    val maxDepth: Int = 50,
    val preserveComments: Boolean = true,
    val failOnMissingIncludes: Boolean = true,
    val failOnCircularDependencies: Boolean = true
)
